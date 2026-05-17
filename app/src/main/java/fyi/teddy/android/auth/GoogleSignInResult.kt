package fyi.teddy.android.auth

import android.net.Uri

data class GoogleSignInResult(
    val displayName: String?,
    val idToken: String,
    val profilePictureUri: Uri?
)
