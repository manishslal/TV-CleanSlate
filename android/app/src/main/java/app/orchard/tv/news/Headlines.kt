package app.orchard.tv.news

import android.content.Context
import android.util.Log
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fork: technology headlines for the screensaver, from Google News' Technology feed (RSS, no key).
 * Read every 30 minutes at most; the last good list is kept for when the network is down. Only
 * stories from the last day are shown, newest first.
 */
object Headlines {
    private const val TAG = "AerialNews"
    private const val FEED = "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en"
    private const val EVERY_MS = 30 * 60_000L
    private const val MAX_AGE_MS = 24 * 3600_000L
    private const val KEEP = 8

    data class Headline(val title: String, val source: String, val published: Long)

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { app.getSharedPreferences("headlines", Context.MODE_PRIVATE) }
    @Volatile private var lastAttempt = 0L

    var items by mutableStateOf<List<Headline>>(emptyList()); private set

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        items = fresh(parseCache(prefs.getString("items", null)))
    }

    /** Reads the feed when the last read is older than 30 minutes. Safe to call often. */
    fun refreshIfStale() {
        if (!::app.isInitialized) return
        val now = System.currentTimeMillis()
        if (now - lastAttempt < 5 * 60_000L || now - prefs.getLong("read_at", 0) < EVERY_MS) {
            items = fresh(items)
            return
        }
        lastAttempt = now
        scope.launch {
            runCatching {
                val read = parse(fetch())
                if (read.isNotEmpty()) {
                    prefs.edit {
                        putLong("read_at", System.currentTimeMillis())
                        putString("items", JSONArray(read.map { JSONObject().put("t", it.title).put("s", it.source).put("p", it.published) }).toString())
                    }
                    items = fresh(read)
                }
            }.onFailure { Log.w(TAG, "headlines not read", it) }
        }
    }

    private fun fresh(list: List<Headline>): List<Headline> {
        val now = System.currentTimeMillis()
        return list.filter { now - it.published in 0..MAX_AGE_MS }.sortedByDescending { it.published }.take(KEEP)
    }

    private fun fetch(): String {
        val c = (URL(FEED).openConnection() as HttpURLConnection).apply { connectTimeout = 6_000; readTimeout = 10_000 }
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    private fun parse(xml: String): List<Headline> {
        val out = ArrayList<Headline>()
        val dates = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
        val p = Xml.newPullParser().apply { setInput(xml.reader()) }
        var inItem = false
        var tag = ""
        var title = ""; var source = ""; var date = ""
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> { tag = p.name; if (tag == "item") { inItem = true; title = ""; source = ""; date = "" } }
                XmlPullParser.TEXT -> if (inItem) when (tag) {
                    "title" -> title += p.text
                    "source" -> source += p.text
                    "pubDate" -> date += p.text
                }
                XmlPullParser.END_TAG -> {
                    if (p.name == "item" && inItem) {
                        inItem = false
                        val published = runCatching { dates.parse(date.trim())?.time }.getOrNull() ?: continue
                        // Google News titles end in " - Source"; the source is shown on its own line.
                        val clean = title.trim().let { t -> if (source.isNotBlank() && t.endsWith(" - ${source.trim()}")) t.removeSuffix(" - ${source.trim()}") else t }
                        if (clean.isNotBlank()) out += Headline(clean, source.trim(), published)
                    }
                    tag = ""
                }
            }
        }
        return out
    }

    private fun parseCache(json: String?): List<Headline> = runCatching {
        val a = JSONArray(json ?: return emptyList())
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Headline(o.getString("t"), o.getString("s"), o.getLong("p")) } }
    }.getOrDefault(emptyList())
}
