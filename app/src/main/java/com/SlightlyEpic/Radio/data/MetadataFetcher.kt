package com.SlightlyEpic.Radio.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NowPlaying(
    val title: String = "",
    val artist: String = ""
) {
    val displayText: String
        get() = when {
            artist.isNotBlank() && title.isNotBlank() -> "$artist - $title"
            title.isNotBlank() -> title
            else -> ""
        }
}

class MetadataFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(station: Station): NowPlaying = withContext(Dispatchers.IO) {
        try {
            when (station.metaType) {
                MetaType.ICECAST -> fetchIcecast(station.metaUrl!!)
                MetaType.SHOUTCAST -> fetchShoutcast(station.metaUrl!!)
                MetaType.NONE -> NowPlaying()
            }
        } catch (_: Exception) {
            NowPlaying()
        }
    }

    private fun fetchIcecast(url: String): NowPlaying {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return NowPlaying()

        val json = JSONObject(body)
        val icestats = json.getJSONObject("icestats")

        val source = when (val s = icestats.opt("source")) {
            is org.json.JSONArray -> s.getJSONObject(0)
            is JSONObject -> s
            else -> return NowPlaying()
        }

        // Prefer yp_currently_playing, then title
        val currentlyPlaying = source.optString("yp_currently_playing", "")
        if (currentlyPlaying.isNotBlank()) {
            return parseTrackString(currentlyPlaying)
        }

        val title = source.optString("title", "")
        if (title.isNotBlank()) {
            return parseTrackString(title)
        }

        val displayTitle = source.optString("server_description", "")
        return NowPlaying(title = displayTitle)
    }

    private fun fetchShoutcast(url: String): NowPlaying {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return NowPlaying()

        // Shoutcast 7.html format: extract content between <body> tags
        val bodyMatch = Regex("<body>(.*?)</body>", RegexOption.DOT_MATCHES_ALL).find(body)
        val content = bodyMatch?.groupValues?.get(1) ?: return NowPlaying()

        // Format: listeners,status,peak,max,unique,bitrate,song_title
        val parts = content.split(",")
        if (parts.size >= 7) {
            val songTitle = parts.drop(6).joinToString(",").trim()
            return parseTrackString(songTitle)
        }

        return NowPlaying()
    }

    private fun stripHtml(text: String): String {
        return text.replace(Regex("</?br\\s*/?>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }

    private fun parseTrackString(text: String): NowPlaying {
        val cleaned = stripHtml(text)
        // Try "Artist - Title" format
        val dashIndex = cleaned.indexOf(" - ")
        return if (dashIndex > 0) {
            NowPlaying(
                artist = cleaned.substring(0, dashIndex).trim(),
                title = cleaned.substring(dashIndex + 3).trim()
            )
        } else {
            NowPlaying(title = cleaned.trim())
        }
    }
}
