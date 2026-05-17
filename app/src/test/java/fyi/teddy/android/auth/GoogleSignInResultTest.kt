package fyi.teddy.android.auth

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleSignInResultTest {

    @Test
    fun dataClass_holdsCorrectValues() {
        val uri = Uri.parse("https://example.com/pic.jpg")
        val result = GoogleSignInResult(
            displayName = "Test User",
            idToken = "token_abc",
            profilePictureUri = uri
        )
        
        assertEquals("Test User", result.displayName)
        assertEquals("token_abc", result.idToken)
        assertEquals(uri, result.profilePictureUri)
    }
}
