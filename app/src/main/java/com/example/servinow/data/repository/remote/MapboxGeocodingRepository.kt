package com.example.servinow.data.repository.remote

import android.content.Context
import com.example.servinow.R
import com.example.servinow.domain.repository.GeocodingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Temporary geocoding: result is displayed, not saved in posts or persistent storage. */
class MapboxGeocodingRepository @Inject constructor(@ApplicationContext private val context: Context) : GeocodingRepository {
    override suspend fun address(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val token = context.getString(R.string.mapbox_access_token)
        if (!token.startsWith("pk.")) return@withContext null
        val language = context.resources.configuration.locales[0].language
        val url = URL("https://api.mapbox.com/search/geocode/v6/reverse?longitude=$longitude&latitude=$latitude&language=${URLEncoder.encode(language, "UTF-8")}&access_token=${URLEncoder.encode(token, "UTF-8")}")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 7000; connection.readTimeout = 7000
            if (connection.responseCode != 200) return@withContext null
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(json).optJSONArray("features")?.optJSONObject(0)?.optJSONObject("properties")?.optString("full_address")?.takeIf { it.isNotBlank() }
        } finally { connection.disconnect() }
    }
}
