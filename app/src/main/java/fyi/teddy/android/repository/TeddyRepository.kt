package fyi.teddy.android.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TeddyRepository {

    suspend fun checkClusterHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://teddy.fyi/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.responseCode == 200
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun fetchTemperature(): String {
        return withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=42.4154&longitude=-71.1565&current_weather=true&temperature_unit=fahrenheit"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val currentWeather = json.getJSONObject("current_weather")
            val temp = currentWeather.getDouble("temperature")
            "$temp°F"
        }
    }

    suspend fun callAuthedHello(idToken: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api-rust.teddy.fyi/authed/hello")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $idToken")
            
            val statusCode = connection.responseCode
            val headers = connection.headerFields.entries.joinToString("\n") { (key, values) ->
                "${key ?: "Status"}: ${values.joinToString(", ")}"
            }
            val body = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: e.localizedMessage
            }
            
            "Status: $statusCode\n\nHeaders:\n$headers\n\nBody:\n$body"
        }
    }
}
