package app.orchard.tv.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.LocalDate

/**
 * Fork: free picture collections from well-known sources, each with a new picture every day, so
 * the wallpaper and the photo screensaver change over time. Beside Bing's photo of the day:
 *
 * - NASA Earth Observatory's Image of the Day (its RSS feed; NASA's pictures are public domain).
 *   NASA's own Picture of the Day API answered every day of 2026 with a placeholder logo, so it is
 *   not used. Earth Observatory's resizer gives a 1920-wide copy of a picture that may be 6 MB.
 * - Wikimedia Commons' Picture of the Day (api.wikimedia.org), credited to its author and licence.
 * - Google Earth View: Google's 1,520 satellite landscapes (assets/earthview.json, their numbers
 *   and places), one a day in a fixed shuffled order, so every TV in a home shows the same.
 *
 * Each source keeps its last [KEEP] pictures in files/collections/<source>/, with an index.json of
 * their titles and credits. Called off the main thread.
 */
enum class PictureSource(val key: String, val en: String, val zh: String) {
    Nasa("nasa", "NASA Earth Observatory", "NASA 地球观测"),
    Wiki("wiki", "Wikimedia Picture of the Day", "维基共享资源每日图片"),
    Earth("earth", "Google Earth View", "Google 地球景观");

    /** The wallpaper id that follows this source's newest picture. */
    val latestId: String get() = "$key:latest"

    companion object {
        fun of(id: String): PictureSource? = entries.firstOrNull { id.startsWith(it.key + ":") }
    }
}

internal object Collections {
    private const val TAG = "AerialCollections"
    private const val KEEP = 6

    /** A plain name for Home, as Wikimedia asks every client to send; nothing about the owner. */
    private const val AGENT = "HomeTVLauncher/1.67 (personal TV launcher)"

    data class Entry(val key: String, val title: String, val caption: String)

    fun dir(root: File, s: PictureSource) = File(root, "collections/${s.key}").apply { mkdirs() }

    fun file(root: File, s: PictureSource, key: String) = File(dir(root, s), "$key.jpg")

    /** What is on hand, newest first, only those whose picture is here. */
    fun index(root: File, s: PictureSource): List<Entry> = runCatching {
        val arr = JSONArray(File(dir(root, s), "index.json").readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Entry(o.getString("key"), o.getString("title"), o.getString("caption"))
        }.filter { file(root, s, it.key).exists() }
    }.getOrDefault(emptyList())

    /** Fetches what is new for [s]; true if the list changed. */
    fun refresh(context: Context, root: File, s: PictureSource): Boolean = runCatching {
        val wanted = when (s) {
            PictureSource.Nasa -> nasa()
            PictureSource.Wiki -> wiki()
            PictureSource.Earth -> earth(context)
        }
        val before = index(root, s).map { it.key }
        val kept = ArrayList<Entry>()
        for ((entry, url) in wanted) {
            if (kept.size >= KEEP) break
            val f = file(root, s, entry.key)
            if (!f.exists()) {
                val bytes = runCatching { get(url) }.onFailure { Log.w(TAG, "${s.key}: ${entry.key} unavailable", it) }.getOrNull() ?: continue
                // Wide enough to fill a TV: a portrait picture cropped to 16:9 is a sliver of it.
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                if (o.outWidth < 1200 || o.outWidth < o.outHeight) continue
                f.writeBytes(bytes)
            }
            kept += entry
        }
        app.orchard.tv.system.DiagLog.i("collection ${s.key}: ${wanted.size} listed, ${kept.size} kept")
        if (kept.isEmpty()) return@runCatching false
        val arr = JSONArray()
        kept.forEach { arr.put(JSONObject().put("key", it.key).put("title", it.title).put("caption", it.caption)) }
        File(dir(root, s), "index.json").writeText(arr.toString())
        val keep = kept.map { it.key + ".jpg" }.toSet()
        dir(root, s).listFiles { f -> f.extension == "jpg" && f.name !in keep }?.forEach { it.delete() }
        kept.map { it.key } != before
    }.onFailure { Log.w(TAG, "${s.key} unavailable", it); app.orchard.tv.system.DiagLog.i("collection ${s.key} failed: ${it.javaClass.simpleName} ${it.message}") }.getOrDefault(false)

    // ---- the sources ----

    /** NASA Earth Observatory: each item's first picture of Earth Observatory's own, maps left out. */
    private fun nasa(): List<Pair<Entry, String>> {
        val rss = get("https://earthobservatory.nasa.gov/feeds/image-of-the-day.rss").decodeToString()
        val picture = Regex("""https://assets\.science\.nasa\.gov/(?:content/dam|dynamicimage/assets)/science/esd/eo/[^"'<>\s]+?\.jpe?g""")
        return Regex("""<item>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL).findAll(rss).mapNotNull { m ->
            val item = m.groupValues[1]
            val title = unescape(Regex("""<title>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL).find(item)?.groupValues?.get(1).orEmpty()).trim()
            if (title.isEmpty() || Regex("""(?i)\b(map|maps|chart|graph|diagram|slice)\b""").containsMatchIn(title)) return@mapNotNull null
            val src = picture.find(unescape(item))?.value ?: return@mapNotNull null
            val sized = src.replace("/content/dam/", "/dynamicimage/assets/") + "?w=1920"
            val key = "n" + Integer.toHexString(src.hashCode())
            Entry(key, title, "$title (NASA Earth Observatory)") to sized
        }.toList()
    }

    /** Wikimedia Commons: the last eight days' Pictures of the Day, wide ones only. */
    private fun wiki(): List<Pair<Entry, String>> {
        val today = LocalDate.now()
        return (0 until 8).mapNotNull { back ->
            val d = today.minusDays(back.toLong())
            val json = runCatching {
                JSONObject(get("https://api.wikimedia.org/feed/v1/wikipedia/en/featured/%04d/%02d/%02d".format(d.year, d.monthValue, d.dayOfMonth)).decodeToString())
            }.getOrNull() ?: return@mapNotNull null
            val img = json.optJSONObject("image") ?: return@mapNotNull null
            val full = img.optJSONObject("image") ?: return@mapNotNull null
            if (full.optInt("width") < full.optInt("height") * 13 / 10) return@mapNotNull null
            val thumb = img.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val url = thumb.substringBefore('?').replace(Regex("""/\d+px-"""), "/1920px-")
            val description = img.optJSONObject("description")?.optString("text").orEmpty().let(::plain)
            val name = img.optString("title").removePrefix("File:").substringBeforeLast('.')
            val title = description.substringBefore(". ").takeIf { it.isNotEmpty() && it.length <= 70 } ?: name.take(70)
            val artist = img.optJSONObject("artist")?.optString("text").orEmpty().let(::plain)
            val license = img.optJSONObject("license")?.optString("type").orEmpty()
            val credit = listOf(artist.takeIf { it.isNotEmpty() }?.let { "© $it" }, license.takeIf { it.isNotEmpty() }).filterNotNull().joinToString(", ")
            Entry("w%04d%02d%02d".format(d.year, d.monthValue, d.dayOfMonth), title, "$title ($credit / Wikimedia Commons)".replace("( / ", "(")) to url
        }
    }

    /** Google Earth View: one of its pictures a day, in a fixed shuffled order. */
    private fun earth(context: Context): List<Pair<Entry, String>> {
        val items = JSONObject(context.assets.open("earthview.json").use { it.readBytes().decodeToString() }).getJSONArray("items")
        val n = items.length()
        if (n == 0) return emptyList()
        val today = LocalDate.now().toEpochDay()
        return (0 until KEEP).map { back ->
            val day = today - back
            // A step coprime with n walks every picture before one comes round again.
            val i = Math.floorMod(day * 619L, n.toLong()).toInt()
            val e = items.getJSONArray(i)
            val id = e.getInt(0)
            val place = e.getString(1).ifEmpty { "Earth" }
            Entry("e$id", place, "$place (Google Earth View)") to "https://www.gstatic.com/prettyearth/assets/full/$id.jpg"
        }
    }

    private fun plain(s: String) = s.replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()

    private fun unescape(s: String) = s
        .replace("&#039;", "'").replace("&#8217;", "’").replace("&#8216;", "‘").replace("&#8220;", "“").replace("&#8221;", "”")
        .replace("&#034;", "\"").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        .replace("<![CDATA[", "").replace("]]>", "")

    private fun get(url: String): ByteArray {
        val c = URL(URI(url).toASCIIString()).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", AGENT)
        return try { c.inputStream.use { it.readBytes() } } finally { c.disconnect() }
    }
}
