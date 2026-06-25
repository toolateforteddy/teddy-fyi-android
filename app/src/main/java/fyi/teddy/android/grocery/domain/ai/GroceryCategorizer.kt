package fyi.teddy.android.grocery.domain.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class GroceryCategorizer(private val context: Context) {
    private var llmInference: LlmInference? = null
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    /**
     * Initializes the SLM. In a production app, the model file would be
     * downloaded from a remote server. For this implementation, we look
     * for a model file named 'model.bin' in the app's files directory.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (_isReady.value) return@withContext

        val modelFile = File(context.filesDir, "llm/model.bin")
        if (!modelFile.exists()) {
            return@withContext
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            _isReady.value = true
        } catch (e: Exception) {
            // Silently fail if AI initialization fails, falling back to manual entry
            android.util.Log.e("GroceryCategorizer", "Failed to init AI", e)
        }
    }

    suspend fun categorize(itemName: String, categories: List<String>): String? = withContext(Dispatchers.Default) {
        val inference = llmInference ?: return@withContext null
        if (categories.isEmpty()) return@withContext null

        val prompt = """
            You are a grocery assistant. Categorize the item into exactly ONE of these categories:
            ${categories.joinToString(", ")}
            
            Item: $itemName
            Category:
        """.trimIndent()
        
        try {
            val result = inference.generateResponse(prompt)
            val cleaned = result.trim().removeSuffix(".")
            // Verify the SLM's output against the allowed list to handle hallucinations
            categories.find { it.equals(cleaned, ignoreCase = true) }
        } catch (e: Exception) {
            null
        }
    }
}
