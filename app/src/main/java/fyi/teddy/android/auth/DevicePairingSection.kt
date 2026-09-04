package fyi.teddy.android.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.ui.theme.TeddyTheme

/**
 * The pairing half of the login screen: a code to read off the tablet, and where to type it.
 *
 * Drawn from [DevicePairingState] alone, so the screen has one place to look and this file has
 * no idea what a network is. The code is the largest thing on screen on purpose -- it is read
 * off a propped-up tablet, often from further away than a phone is ever held.
 */
@Composable
fun DevicePairingSection(
    state: DevicePairingState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            DevicePairingState.Idle -> {
                Button(onClick = onStart) {
                    Text("Sign in with a code")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Google sign-in on this device. Sign in from a phone or a computer " +
                        "instead, and this device picks up the rest.",
                    color = TeddyTheme.colors.onSurfaceMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            DevicePairingState.Starting -> {
                CircularProgressIndicator(color = TeddyTheme.colors.accent)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Getting a code…",
                    color = TeddyTheme.colors.onSurfaceMuted,
                    fontSize = 13.sp,
                )
            }

            is DevicePairingState.AwaitingRedemption -> {
                Text(
                    text = "On a phone or a computer, open",
                    color = TeddyTheme.colors.onSurfaceMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.verificationUri.removePrefix("https://"),
                    color = TeddyTheme.colors.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "sign in with Google, and type this code:",
                    color = TeddyTheme.colors.onSurfaceMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.displayCode,
                    color = TeddyTheme.colors.onSurface,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TeddyTheme.colors.panelSunken, RoundedCornerShape(16.dp))
                        .border(1.dp, TeddyTheme.colors.outline, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "The code lasts ten minutes. This screen signs itself in a few " +
                        "seconds after the code is accepted — nothing else to do here.",
                    color = TeddyTheme.colors.onSurfaceMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = TeddyTheme.colors.onSurfaceMuted)
                }
            }

            DevicePairingState.Expired -> {
                Text(
                    text = "That code ran out before it was used.",
                    color = TeddyTheme.colors.onSurface,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onStart) {
                    Text("Get a new code")
                }
            }

            is DevicePairingState.Failure -> {
                Text(
                    text = state.message,
                    color = TeddyTheme.colors.danger,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onStart) {
                    Text("Try again")
                }
            }
        }
    }
}
