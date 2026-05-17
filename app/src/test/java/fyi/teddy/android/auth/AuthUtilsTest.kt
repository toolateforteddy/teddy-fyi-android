package fyi.teddy.android.auth

import android.util.Base64
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthUtilsTest {

    private fun createFakeToken(payload: Map<String, Any>): String {
        val header = Base64.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val payloadJson = JSONObject(payload).toString()
        val payloadEncoded = Base64.encodeToString(payloadJson.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$header.$payloadEncoded.signature"
    }

    @Test
    fun md5_isCorrect() {
        // MD5 of "test" is 098f6bcd4621d373cade4e832627b4f6
        assertEquals("098f6bcd4621d373cade4e832627b4f6", AuthUtils.md5("test"))
        
        // Test with uppercase and spaces (should be lowercase and trimmed)
        assertEquals("098f6bcd4621d373cade4e832627b4f6", AuthUtils.md5(" TEST "))
    }

    @Test
    fun extractPicture_withPictureClaim() {
        val payload = mapOf("picture" to "https://example.com/pic.jpg")
        val token = createFakeToken(payload)
        
        val uri = AuthUtils.extractPictureFromToken(token)
        assertEquals("https://example.com/pic.jpg", uri.toString())
    }

    @Test
    fun extractPicture_withEmailFallback() {
        val email = "test@example.com"
        val payload = mapOf("email" to email)
        val token = createFakeToken(payload)
        
        val uri = AuthUtils.extractPictureFromToken(token)
        val hash = AuthUtils.md5(email)
        assertEquals("https://www.gravatar.com/avatar/$hash?d=identicon&s=200", uri.toString())
    }

    @Test
    fun extractPicture_withSubFallback() {
        val sub = "123456789"
        val payload = mapOf("sub" to sub)
        val token = createFakeToken(payload)
        
        val uri = AuthUtils.extractPictureFromToken(token)
        assertEquals("https://www.google.com/s2/photos/profile/$sub", uri.toString())
    }

    @Test
    fun extractPicture_prefersPictureOverEmail() {
        val payload = mapOf(
            "picture" to "https://example.com/pic.jpg",
            "email" to "test@example.com"
        )
        val token = createFakeToken(payload)
        
        val uri = AuthUtils.extractPictureFromToken(token)
        assertEquals("https://example.com/pic.jpg", uri.toString())
    }

    @Test
    fun extractPicture_invalidToken() {
        assertNull(AuthUtils.extractPictureFromToken("invalid-token"))
    }
}
