package app.orchard.tv.news

import android.content.Context
import android.util.Log
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import app.orchard.tv.R
import app.orchard.tv.settings.AerialSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fork: what the screensaver's headlines are about, each one a Google News RSS feed (no key).
 * Tech and AI are searches, not Google's Technology section, which is mostly games and gadgets:
 * Tech asks for AI, open source, models, chips, startups and IPOs, with games left out.
 */
enum class NewsTopic(val key: String, val label: Int, private val section: String? = null, private val query: String? = null) {
    Tech("tech", R.string.fk_news_tech, query = "(AI OR \"artificial intelligence\" OR \"open source\" OR \"AI model\" OR LLM OR OpenAI OR Anthropic OR Nvidia OR semiconductor OR startup OR IPO) -game -gaming -PlayStation -Xbox -Nintendo"),
    Ai("ai", R.string.fk_news_ai, query = "(\"AI model\" OR LLM OR OpenAI OR Anthropic OR \"Google DeepMind\" OR \"open-source model\" OR \"generative AI\")"),
    Us("us", R.string.fk_news_us, section = "NATION"),
    World("world", R.string.fk_news_world, section = "WORLD"),
    Business("business", R.string.fk_news_business, section = "BUSINESS"),
    Science("science", R.string.fk_news_science, section = "SCIENCE"),
    Sports("sports", R.string.fk_news_sports, section = "SPORTS"),
    Gaming("gaming", R.string.fk_news_gaming, query = "(\"video game\" OR \"video games\" OR PlayStation OR Xbox OR Nintendo OR Steam)"),
    Entertainment("entertainment", R.string.fk_news_entertainment, section = "ENTERTAINMENT"),
    Health("health", R.string.fk_news_health, section = "HEALTH");

    val feed: String get() = if (query != null) {
        "https://news.google.com/rss/search?q=${URLEncoder.encode("$query when:1d", "UTF-8")}&hl=en-US&gl=US&ceid=US:en"
    } else "https://news.google.com/rss/headlines/section/topic/$section?hl=en-US&gl=US&ceid=US:en"

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

/**
 * Fork: headlines for the screensaver, from the topics chosen in Personalize (AerialSettings.newsTopics).
 * Each topic is read every 30 minutes at most; the last good list is kept for when the network is
 * down. Only stories from the last day are shown: the topics take turns, newest first, or in a
 * random order with Shuffle on.
 */
object Headlines {
    private const val TAG = "AerialNews"
    private const val EVERY_MS = 30 * 60_000L
    private const val MAX_AGE_MS = 24 * 3600_000L
    private const val KEEP = 8

    data class Headline(val title: String, val source: String, val published: Long, val topic: NewsTopic)

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { app.getSharedPreferences("headlines", Context.MODE_PRIVATE) }
    @Volatile private var lastAttempt = 0L

    /** Every topic's last good read, newest first within each. */
    @Volatile private var read: Map<NewsTopic, List<Headline>> = emptyMap()

    var items by mutableStateOf<List<Headline>>(emptyList()); private set

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        read = NewsTopic.entries.associateWith { parseCache(prefs.getString("items_${it.key}", null), it) }
        rebuild()
    }

    /** The topics or the order changed in Personalize: shown again at once, and read if not read lately. */
    fun topicsChanged() {
        if (!::app.isInitialized) return
        rebuild()
        lastAttempt = 0L
        refreshIfStale()
    }

    /** Reads each chosen topic not read in the last 30 minutes. Safe to call often. */
    fun refreshIfStale() {
        if (!::app.isInitialized) return
        val now = System.currentTimeMillis()
        val due = AerialSettings.newsTopics.filter { now - prefs.getLong("read_at_${it.key}", 0) >= EVERY_MS }
        if (due.isEmpty() || now - lastAttempt < 5 * 60_000L) {
            rebuild()
            return
        }
        lastAttempt = now
        scope.launch {
            for (topic in due) runCatching {
                val list = parse(fetch(topic.feed), topic)
                if (list.isNotEmpty()) {
                    prefs.edit {
                        putLong("read_at_${topic.key}", System.currentTimeMillis())
                        putString("items_${topic.key}", JSONArray(list.map { JSONObject().put("t", it.title).put("s", it.source).put("p", it.published) }).toString())
                    }
                    read = read + (topic to list)
                }
            }.onFailure { Log.w(TAG, "${topic.key} headlines not read", it) }
            rebuild()
        }
    }

    /** The list shown: the chosen topics' fresh stories, taking turns newest first, or shuffled. */
    private fun rebuild() {
        val now = System.currentTimeMillis()
        val lists = AerialSettings.newsTopics.map { topic ->
            read[topic].orEmpty().filter { now - it.published in 0..MAX_AGE_MS }.sortedByDescending { it.published }.take(KEEP)
        }
        val out = ArrayList<Headline>()
        for (i in 0 until (lists.maxOfOrNull { it.size } ?: 0)) lists.forEach { l -> l.getOrNull(i)?.let(out::add) }
        // Rebuilt only when what is shown would change, so a shuffled list keeps its order between reads.
        val next = if (AerialSettings.newsShuffle) out.shuffled() else out
        if (next.toSet() != items.toSet() || (!AerialSettings.newsShuffle && next != items)) items = next
    }

    private fun fetch(feed: String): String {
        val c = (URL(feed).openConnection() as HttpURLConnection).apply { connectTimeout = 6_000; readTimeout = 10_000 }
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    private fun parse(xml: String, topic: NewsTopic): List<Headline> {
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
                        if (clean.isNotBlank()) out += Headline(clean, source.trim(), published, topic)
                    }
                    tag = ""
                }
            }
        }
        return out.sortedByDescending { it.published }.take(KEEP * 2)
    }

    private fun parseCache(json: String?, topic: NewsTopic): List<Headline> = runCatching {
        val a = JSONArray(json ?: return emptyList())
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Headline(o.getString("t"), o.getString("s"), o.getLong("p"), topic) } }
    }.getOrDefault(emptyList())
}
