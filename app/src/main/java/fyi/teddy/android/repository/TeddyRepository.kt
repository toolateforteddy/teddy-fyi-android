package fyi.teddy.android.repository

import android.util.Log
import fyi.teddy.android.network.NetworkClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object TeddyRepository {
    private const val TAG = "TeddyRepository"

    suspend fun checkClusterHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking cluster health...")
                val response = NetworkClient.client.get("https://teddy.fyi/")
                Log.d(TAG, "Cluster health check returned: ${response.status.value}")
                response.status.isSuccess()
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
                val response = NetworkClient.client.get(url)
                val responseBody = response.bodyAsText()
                val json = JSONObject(responseBody)
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
                val response = NetworkClient.client.get("https://api-rust.teddy.fyi/authed/hello") {
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                }
                
                val statusCode = response.status
                Log.d(TAG, "Authed hello returned status code: $statusCode")
                
                val headers = response.headers.entries().joinToString("\n") { (key, values) ->
                    "$key: ${values.joinToString(", ")}"
                }
                
                val body = response.bodyAsText()
                
                "Status: $statusCode\n\nHeaders:\n$headers\n\nBody:\n$body"
            } catch (e: Exception) {
                Log.e(TAG, "Authed hello request failed", e)
                "Request failed: ${e.localizedMessage}"
            }
        }
    }
}
