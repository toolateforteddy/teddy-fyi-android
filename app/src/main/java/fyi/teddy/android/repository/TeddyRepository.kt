package fyi.teddy.android.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TeddyRepository {
    private const val TAG = "TeddyRepository"

    suspend fun checkClusterHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking cluster health...")
                val url = URL("https://teddy.fyi/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val code = connection.responseCode
                Log.d(TAG, "Cluster health check returned: $code")
                code == 200
            } catch (e: Exception) {
                Log.e(TAG, "Cluster health check failed: ${e.message}")
                false
            }
        }
    }

    suspend fun fetchTemperature(): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching temperature...")
                val url = "https://api.open-meteo.com/v1/forecast?latitude=42.4154&longitude=-71.1565&current_weather=true&temperature_unit=fahrenheit"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val currentWeather = json.getJSONObject("current_weather")
                val temp = currentWeather.getDouble("temperature")
                Log.d(TAG, "Fetched temperature: $temp")
                "$temp°F"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch temperature: ${e.message}")
                "Error: ${e.localizedMessage}"
            }
        }
    }

    suspend fun callAuthedHello(idToken: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling authed hello with token length: ${idToken.length}")
                val url = URL("https://api-rust.teddy.fyi/authed/hello")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $idToken")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val statusCode = connection.responseCode
                Log.d(TAG, "Authed hello returned status code: $statusCode")
                
                val headers = connection.headerFields.entries.joinToString("\n") { (key, values) ->
                    "${key ?: "Status"}: ${values.joinToString(", ")}"
                }
                
                val body = try {
                    if (statusCode in 200..299) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read response body", e)
                    "Error reading body: ${e.localizedMessage}"
                }
                
                "Status: $statusCode\n\nHeaders:\n$headers\n\nBody:\n$body"
            } catch (e: Exception) {
                Log.e(TAG, "Authed hello request failed", e)
                "Request failed: ${e.localizedMessage}"
            }
        }
    }
}
