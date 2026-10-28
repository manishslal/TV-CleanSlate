package app.orchard.tv.plex

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import app.orchard.tv.BuildConfig
import app.orchard.tv.R
import app.orchard.tv.wallpaper.ScreensaverVideo
import app.orchard.tv.wallpaper.Wallpapers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Fork: the owner's own videos and photos from a Plex Media Server on the home network.
 *
 * Linking uses Plex's PIN flow: the TV shows a code, the owner enters it at plex.tv/link on a phone,
 * and the TV receives a token. No password passes through the TV. The token stays in this app's
 * private preferences.
 *
 * Videos are played as they are stored (direct play of the part file over HTTP; Media3 handles
 * range requests and buffering), so the server does no transcoding. Photos are fetched resized to
 * the screen by Plex's photo transcoder and kept with the owner's other photos as plex-<key>.jpg;
 * only files of that name are ever removed here.
 */
object Plex {
    private const val TAG = "AerialPlex"
    private const val PLEX_TV = "https://plex.tv/api/v2"
    private const val MAX_PHOTOS = 60
    private const val PHOTO_SYNC_EVERY_MS = 6 * 3600_000L

    data class Section(val key: String, val title: String, val type: String)

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { app.getSharedPreferences("plex", Context.MODE_PRIVATE) }
    private var linkJob: Job? = null

    /** The code to enter at plex.tv/link while linking, else null. */
    var linkCode by mutableStateOf<String?>(null); private set
    var linked by mutableStateOf(false); private set
    var serverName by mutableStateOf<String?>(null); private set
    var sections by mutableStateOf<List<Section>>(emptyList()); private set
    var videoSection by mutableStateOf(""); private set
    var photoSection by mutableStateOf(""); private set
    /** Plex films for the screensaver and the video wallpaper; empty without a video library. */
    @Volatile var videos: List<ScreensaverVideo> = emptyList(); private set
    /** What is going on, for the settings note; null when all is well. */
    var problem by mutableStateOf<Int?>(null); private set

    private val token get() = prefs.getString("token", null)
    private val base get() = prefs.getString("server", null)

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        linked = token != null && base != null
        serverName = prefs.getString("server_name", null)
        videoSection = prefs.getString("video_section", "") ?: ""
        photoSection = prefs.getString("photo_section", "") ?: ""
        sections = parseSections(prefs.getString("sections", null))
        videos = parseVideos(prefs.getString("videos", null))
        if (linked) refresh(photosIfStale = true)
    }

    private val clientId: String
        get() = prefs.getString("client_id", null) ?: UUID.randomUUID().toString().also { prefs.edit { putString("client_id", it) } }

    // ---- linking ----

    fun startLink() {
        linkJob?.cancel()
        linkJob = scope.launch {
            runCatching {
                val pin = JSONObject(http("$PLEX_TV/pins?strong=false", method = "POST"))
                val id = pin.getLong("id")
                linkCode = pin.getString("code")
                problem = null
                val until = System.currentTimeMillis() + 10 * 60_000L
                while (System.currentTimeMillis() < until) {
                    delay(2_000)
                    val got = JSONObject(http("$PLEX_TV/pins/$id")).optString("authToken").takeIf { it.isNotBlank() && it != "null" }
                    if (got != null) {
                        prefs.edit { putString("account_token", got) }
                        linkCode = null
                        discover(got)
                        return@launch
                    }
                }
                linkCode = null
                problem = R.string.fk_plex_expired
            }.onFailure {
                Log.w(TAG, "link failed", it)
                linkCode = null
                problem = R.string.fk_plex_no_plextv
            }
        }
    }

    fun unlink() {
        linkJob?.cancel()
        prefs.edit { clear() }
        linked = false; linkCode = null; serverName = null; sections = emptyList()
        videoSection = ""; photoSection = ""; videos = emptyList(); problem = null
        scope.launch { removePhotos(keep = emptySet()); withContext(Dispatchers.Main) { Wallpapers.mediaChanged() } }
    }

    /** Finds the owner's server, preferring an address on this network over a relay or plex.direct name. */
    private fun discover(accountToken: String) {
        val list = JSONArray(http("$PLEX_TV/resources?includeHttps=1&includeRelay=0", token = accountToken))
        var best: Triple<String, String, String>? = null
        for (i in 0 until list.length()) {
            val r = list.getJSONObject(i)
            if (!r.optString("provides").contains("server")) continue
            val conns = r.optJSONArray("connections") ?: continue
            val serverToken = r.optString("accessToken").ifBlank { accountToken }
            for (j in 0 until conns.length()) {
                val c = conns.getJSONObject(j)
                if (!c.optBoolean("local") || c.optBoolean("relay")) continue
                val uri = "http://${c.optString("address")}:${c.optInt("port", 32400)}"
                if (runCatching { http("$uri/identity", token = serverToken) }.isSuccess) {
                    best = Triple(uri, r.optString("name", "Plex"), serverToken)
                    break
                }
            }
            if (best != null) break
        }
        val (uri, name, serverToken) = best ?: run { problem = R.string.fk_plex_no_server; return }
        prefs.edit { putString("server", uri); putString("server_name", name); putString("token", serverToken) }
        serverName = name
        linked = true
        problem = null
        refresh(photosIfStale = false)
    }

    // ---- libraries ----

    fun chooseVideoSection(key: String) {
        videoSection = key
        prefs.edit { putString("video_section", key) }
        refresh(photosIfStale = true)
    }

    fun choosePhotoSection(key: String) {
        photoSection = key
        prefs.edit { putString("photo_section", key); remove("photos_synced_at") }
        refresh(photosIfStale = true)
    }

    /** Re-reads the libraries and the chosen video library; photos only when they are due. */
    fun refresh(photosIfStale: Boolean) {
        if (!::app.isInitialized) return
        scope.launch {
            val t = token ?: return@launch
            val b = base ?: return@launch
            runCatching {
                val dirs = JSONObject(http("$b/library/sections", token = t)).getJSONObject("MediaContainer").optJSONArray("Directory") ?: JSONArray()
                val found = (0 until dirs.length()).map { dirs.getJSONObject(it) }
                    .map { Section(it.optString("key"), it.optString("title"), it.optString("type")) }
                sections = found
                prefs.edit { putString("sections", JSONArray(found.map { JSONObject().put("key", it.key).put("title", it.title).put("type", it.type) }).toString()) }
                videos = if (videoSection.isBlank()) emptyList() else loadVideos(b, t, videoSection)
                prefs.edit { putString("videos", JSONArray(videos.map { JSONObject().put("id", it.id).put("name", it.name).put("uri", it.uri.toString()) }).toString()) }
                val due = System.currentTimeMillis() - prefs.getLong("photos_synced_at", 0) > PHOTO_SYNC_EVERY_MS
                if (photoSection.isBlank()) removePhotos(keep = emptySet())
                else if (!photosIfStale || due) syncPhotos(b, t, photoSection)
                problem = null
            }.onFailure {
                Log.w(TAG, "refresh failed", it)
                problem = R.string.fk_plex_server_down
            }
            withContext(Dispatchers.Main) { Wallpapers.mediaChanged() }
        }
    }

    private fun loadVideos(b: String, t: String, section: String): List<ScreensaverVideo> {
        val items = metadata(http("$b/library/sections/$section/all?type=1", token = t))
        return items.mapNotNull { m ->
            val part = m.optJSONArray("Media")?.optJSONObject(0)?.optJSONArray("Part")?.optJSONObject(0)?.optString("key") ?: return@mapNotNull null
            if (part.isBlank()) return@mapNotNull null
            val uri = Uri.parse("$b$part?X-Plex-Token=${enc(t)}")
            ScreensaverVideo("plex" + m.optString("ratingKey"), m.optString("title", "Plex"), null, uri)
        }
    }

    private fun syncPhotos(b: String, t: String, section: String) {
        val items = metadata(http("$b/library/sections/$section/all?type=13&sort=addedAt:desc", token = t)).take(MAX_PHOTOS)
        val keep = HashSet<String>()
        val dir = Wallpapers.photosFolder
        for (m in items) {
            val key = m.optString("ratingKey").filter(Char::isLetterOrDigit).takeIf { it.isNotEmpty() } ?: continue
            val part = m.optJSONArray("Media")?.optJSONObject(0)?.optJSONArray("Part")?.optJSONObject(0)?.optString("key")
                ?: m.optString("thumb").takeIf { it.isNotBlank() } ?: continue
            val name = "plex-$key.jpg"
            keep += name
            val file = File(dir, name)
            if (file.exists()) continue
            val url = "$b/photo/:/transcode?width=1920&height=1080&minSize=1&upscale=0&url=${enc(part)}&X-Plex-Token=${enc(t)}"
            runCatching {
                val tmp = File(dir, "$name.part")
                (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8_000; readTimeout = 20_000 }.inputStream.use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                }
                if (!tmp.renameTo(file)) tmp.delete()
            }.onFailure { Log.w(TAG, "photo $key not fetched", it) }
        }
        removePhotos(keep)
        prefs.edit { putLong("photos_synced_at", System.currentTimeMillis()) }
    }

    /** Removes Plex photos not in [keep]; never any other file. */
    private fun removePhotos(keep: Set<String>) {
        Wallpapers.photosFolder.listFiles { f -> f.name.startsWith("plex-") && (f.name.endsWith(".jpg") || f.name.endsWith(".part")) }
            ?.filter { it.name !in keep }?.forEach { it.delete() }
    }

    // ---- plumbing ----

    private fun metadata(body: String): List<JSONObject> {
        val arr = JSONObject(body).getJSONObject("MediaContainer").optJSONArray("Metadata") ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun parseSections(json: String?): List<Section> = runCatching {
        val a = JSONArray(json ?: return emptyList())
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Section(o.getString("key"), o.getString("title"), o.getString("type")) } }
    }.getOrDefault(emptyList())

    private fun parseVideos(json: String?): List<ScreensaverVideo> = runCatching {
        val a = JSONArray(json ?: return emptyList())
        (0 until a.length()).map { a.getJSONObject(it).let { o -> ScreensaverVideo(o.getString("id"), o.getString("name"), null, Uri.parse(o.getString("uri"))) } }
    }.getOrDefault(emptyList())

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun http(url: String, method: String = "GET", token: String? = null): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 6_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Plex-Product", "Home (Aerial fork)")
            setRequestProperty("X-Plex-Version", BuildConfig.VERSION_NAME)
            setRequestProperty("X-Plex-Client-Identifier", clientId)
            setRequestProperty("X-Plex-Platform", "Android")
            setRequestProperty("X-Plex-Device", Build.MODEL)
            setRequestProperty("X-Plex-Device-Name", Build.MODEL)
            if (token != null) setRequestProperty("X-Plex-Token", token)
            if (method == "POST") { doOutput = true; outputStream.close() }
        }
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode} for ${url.substringBefore('?')}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}
