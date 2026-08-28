package fyi.teddy.android.repository

import android.util.Log
import fyi.teddy.android.network.ApiRoutes
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
                val response = NetworkClient.client.get(ApiRoutes.CLUSTER_HEALTH)
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
                val response = NetworkClient.client.get(ApiRoutes.WEATHER)
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
                Log.d(TAG, "Calling authed hello...")
                val response = NetworkClient.client.get(ApiRoutes.HEALTH_CHECK) {
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                }
                
                // If unauthorized (401), we don't have automatic refresh logic here yet, 
                // but this is where it would go if we supported refresh tokens.
                if (response.status == HttpStatusCode.Unauthorized) {
                    Log.w(TAG, "Got 401 Unauthorized for authed hello")
                }
                
                val statusCode = response.status
                val body = response.bodyAsText()
                
                "Status: $statusCode\n\nBody:\n$body"
            } catch (e: Exception) {
                Log.e(TAG, "Authed hello request failed", e)
                "Request failed: ${e.localizedMessage}"
            }
        }
    }

    suspend fun fetchAuthedHelloBody(idToken: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching authed hello body...")
                val response = NetworkClient.client.get(ApiRoutes.HEALTH_CHECK) {
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                }
                response.bodyAsText()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch authed hello body: ${e.message}")
                "Error: ${e.localizedMessage}"
            }
        }
    }
}
