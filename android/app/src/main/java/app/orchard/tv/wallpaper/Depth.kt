package app.orchard.tv.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.system.DiagLog
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Fork: the wallpaper's subject drawn over the clock, as a phone's lock screen lets a mountain or a
 * tower stand in front of the time. The cut-out comes from the home's cut-out service
 * (server/cutout, on a home server; AerialSettings.depthServer), which answers a picture with its
 * subject's mask, and is kept on the TV by the picture it was made for.
 *
 * Only a firm cut-out of one subject is used ([MIN_SOLID]): the model also marks haze over a river
 * or a sky, which drawn over the clock would read as a smudge. Home then decides from where the
 * clock stands whether the subject reaches it, and not so far that the time cannot be read
 * (ui/home/DepthOverClock).
 */
object Depth {
    /** The mask, 960×540, one byte a pixel (0 background, 255 subject), the picture cropped as Home shows it. */
    var mask by mutableStateOf<ByteArray?>(null); private set

    /** The top of Home's backdrop where the subject is, transparent elsewhere: what is drawn over the clock. */
    var front by mutableStateOf<ImageBitmap?>(null); private set

    /** How firmly the subject was cut out (0 to 1), and how much of the frame it covers. */
    var solid = 0f; private set
    var share = 0f; private set

    const val MASK_W = 960
    const val MASK_H = 540

    /** Below this the cut-out is haze, not a subject: none is drawn. */
    private const val MIN_SOLID = 0.7f

    /** How much of the screen, from the top, the front covers: the clock stands in the top half. */
    const val FRONT_SHARE = 0.55f

    private fun dir(context: Context) = File(context.filesDir, "depth").apply { mkdirs() }

    fun clear() {
        mask = null
        front = null
    }

    /**
     * The cut-out for the picture behind [key]: from the TV's own copy, else asked of the service
     * with [jpeg] (the picture as Home shows it, before shading). [shaded] is Home's backdrop, which
     * the front is cut from so it matches the picture under it pixel for pixel. Off the main thread.
     */
    fun prepare(context: Context, key: String, jpeg: ByteArray?, shaded: Bitmap): Pair<ByteArray, Bitmap>? {
        val server = AerialSettings.depthServer.trim().trimEnd('/')
        if (server.isEmpty() || !AerialSettings.depthEffect) return null
        val name = Integer.toHexString(key.hashCode())
        val png = File(dir(context), "$name.png")
        val meta = File(dir(context), "$name.txt")
        if (!png.exists() || !meta.exists()) {
            if (jpeg == null) return null
            val (bytes, info) = ask(server, jpeg) ?: return null
            // Kept only if it is a picture: a cut short answer kept would never be asked for again.
            val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, probe)
            if (probe.outWidth <= 0 || probe.outHeight <= 0) {
                DiagLog.i("depth: the cut-out service answered something that is not a picture")
                return null
            }
            png.writeBytes(bytes)
            meta.writeText(info)
            // Keep the last few: a picture comes back as the days turn (Every Source, the rotating photos).
            dir(context).listFiles()?.sortedByDescending { it.lastModified() }?.drop(16)?.forEach { it.delete() }
        }
        val (s, sh) = meta.readText().split(' ').let { (it.getOrNull(0)?.toFloatOrNull() ?: 0f) to (it.getOrNull(1)?.toFloatOrNull() ?: 0f) }
        solid = s
        share = sh
        DiagLog.i("depth for $key: subject ${"%.3f".format(sh)}, solid ${"%.2f".format(s)}")
        if (s < MIN_SOLID || sh < 0.01f || sh > 0.6f) return null
        val gray = BitmapFactory.decodeFile(png.path) ?: return null
        val bytes = ByteArray(MASK_W * MASK_H)
        val small = if (gray.width == MASK_W && gray.height == MASK_H) gray else Bitmap.createScaledBitmap(gray, MASK_W, MASK_H, true)
        val row = IntArray(MASK_W)
        for (y in 0 until MASK_H) {
            small.getPixels(row, 0, MASK_W, 0, y, MASK_W, 1)
            for (x in 0 until MASK_W) bytes[y * MASK_W + x] = (row[x] and 0xFF).toByte()
        }
        // The front: the top of the backdrop, kept only where the mask is (its grey turned to alpha).
        val w = shaded.width
        val h = (shaded.height * FRONT_SHARE).toInt()
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(shaded, Rect(0, 0, w, h), Rect(0, 0, w, h), null)
        val toAlpha = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            1f, 0f, 0f, 0f, 0f,
        )))
        val maskH = (MASK_H * FRONT_SHARE).toInt()
        c.drawBitmap(small, Rect(0, 0, MASK_W, maskH), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = toAlpha
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        if (small !== gray) small.recycle()
        gray.recycle()
        return bytes to out
    }

    /** Puts a prepared cut-out up; main thread. */
    fun show(prepared: Pair<ByteArray, Bitmap>?) {
        mask = prepared?.first
        front = prepared?.second?.asImageBitmap()
    }

    /**
     * Asks the service, over plain HTTP on the home network. Android 9 and later refuse cleartext
     * HTTP to any host the network security config does not name, and every home's server address
     * is its own; rather than allow cleartext everywhere, the request is written on a socket, and
     * only to a private address ([isHome]): the service never leaves the home network.
     */
    private fun ask(server: String, jpeg: ByteArray): Pair<ByteArray, String>? = runCatching {
        val u = java.net.URI(server)
        val host = u.host ?: return@runCatching null
        val port = if (u.port > 0) u.port else 80
        // The address the name stands for, checked and then used: a name that looks local
        // but points elsewhere (a private-looking label under a public domain) is not asked.
        val address = if (u.scheme == "http") java.net.InetAddress.getByName(host) else null
        if (address == null || !isHome(address)) {
            DiagLog.i("depth: $host is not on the home network; not asked")
            return@runCatching null
        }
        java.net.Socket().use { sock ->
            sock.connect(java.net.InetSocketAddress(address, port), 4_000)
            // The service makes a cut-out in a few seconds; one in the queue may wait for another.
            sock.soTimeout = 60_000
            val out = sock.getOutputStream()
            out.write("POST /cutout HTTP/1.1\r\nHost: $host:$port\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(jpeg)
            out.flush()
            val all = readCapped(sock.getInputStream()) ?: return@runCatching null
            val split = (0 until all.size - 3).firstOrNull { all[it] == '\r'.code.toByte() && all[it + 1] == '\n'.code.toByte() && all[it + 2] == '\r'.code.toByte() && all[it + 3] == '\n'.code.toByte() }
                ?: return@runCatching null
            val head = String(all, 0, split, Charsets.ISO_8859_1).split("\r\n")
            if (head.firstOrNull()?.split(' ')?.getOrNull(1) != "200") return@runCatching null
            fun header(name: String) = head.firstOrNull { it.startsWith("$name:", ignoreCase = true) }?.substringAfter(':')?.trim()
            // Only the plain framing the service speaks: a stated length, all of it there.
            if (header("Transfer-Encoding") != null) return@runCatching null
            val length = header("Content-Length")?.toIntOrNull() ?: return@runCatching null
            if (all.size - (split + 4) < length) return@runCatching null
            val body = all.copyOfRange(split + 4, split + 4 + length)
            body to "${header("X-Solid") ?: "0"} ${header("X-Subject") ?: "0"}"
        }
    }.onFailure { DiagLog.i("depth: the cut-out service did not answer (${it.javaClass.simpleName})") }.getOrNull()

    /** An address on the home network: a private range (10, 172.16-31, 192.168), link-local or this TV. */
    private fun isHome(a: java.net.InetAddress): Boolean = a.isSiteLocalAddress || a.isLinkLocalAddress || a.isLoopbackAddress

    /** The whole answer, up to [MAX_ANSWER]; null if it is longer. */
    private fun readCapped(input: java.io.InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) return out.toByteArray()
            out.write(buf, 0, n)
            if (out.size() > MAX_ANSWER) return null
        }
    }

    /** A 960×540 mask is some tens of KB; anything past this is not one. */
    private const val MAX_ANSWER = 4 shl 20

    /** The picture as JPEG for the service: small enough to send in a moment, the service works at 1024. */
    fun jpegOf(picture: Bitmap): ByteArray {
        val scaled = if (picture.width > 1280) Bitmap.createScaledBitmap(picture, 1280, picture.height * 1280 / picture.width, true) else picture
        return ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it); if (scaled !== picture) scaled.recycle() }.toByteArray()
    }
}
