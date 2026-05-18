package fyi.teddy.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fyi.teddy.android.auth.AuthUtils
import fyi.teddy.android.auth.LoginScreen
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.grocery.ui.CategoryManagementScreen
import fyi.teddy.android.grocery.ui.GroceryConfigScreen
import fyi.teddy.android.grocery.ui.GroceryScreen
import fyi.teddy.android.grocery.ui.StoreManagementScreen
import fyi.teddy.android.todo.ui.TodoScreen
import fyi.teddy.android.ui.screens.AuthedHelloScreen
import fyi.teddy.android.ui.screens.HomeScreen
import fyi.teddy.android.ui.screens.WeatherScreen
import fyi.teddy.android.ui.theme.TeddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeddyTheme {
                val navController = rememberNavController()
                val session = remember { UserSession() }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    session.load(context)
                    if (session.idToken != null) {
                        if (session.userId == null) {
                            session.userId = if (session.idToken == "fake_emulator_token") {
                                "emulator_guest_id"
                            } else {
                                AuthUtils.extractUserIdFromToken(session.idToken!!)
                            }
                        }
                        // If we have a known-bad fallback URL, or it's null, try re-extracting with current logic
                        if (session.profilePictureUri == null || session.profilePictureUri!!.contains("s2/photos/profile")) {
                            Log.d("MainActivity", "Pic URI is missing or bad, attempting re-extraction...")
                            session.profilePictureUri = AuthUtils.extractPictureFromToken(session.idToken!!)?.toString()
                        }
                        session.save(context)
                    }
                    Log.d("MainActivity", "Session loaded: name=${session.userName}, tokenPrefix=${session.idToken?.take(10)}, picUri=${session.profilePictureUri}")
                    if (session.idToken != null) {
                        navController.navigate("hello") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(onLoginSuccess = { result ->
                            Log.d("MainActivity", "Login success callback: name=${result.displayName}, pic=${result.profilePictureUri}")
                            session.userName = result.displayName
                            session.idToken = result.idToken
                            session.userId = if (result.idToken == "fake_emulator_token") {
                                "emulator_guest_id"
                            } else {
                                AuthUtils.extractUserIdFromToken(result.idToken)
                            }
                            session.profilePictureUri = result.profilePictureUri?.toString()
                            session.save(context)
                            navController.navigate("hello") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("hello") {
                        HomeScreen(
                            userName = session.userName,
                            profilePic = session.profilePictureUri,
                            onNavigateToWeather = { navController.navigate("weather") },
                            onNavigateToAuthed = { navController.navigate("authed") },
                            onNavigateToTodo = { navController.navigate("todo") },
                            onNavigateToGrocery = { navController.navigate("grocery") },
                            onLogout = {
                                session.clear(context)
                                navController.navigate("login") {
                                    popUpTo("hello") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("weather") {
                        WeatherScreen(onBack = { navController.popBackStack() })
                    }
                    composable("authed") {
                        AuthedHelloScreen(idToken = session.idToken, onBack = { navController.popBackStack() })
                    }
                    composable("todo") {
                        TodoScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("grocery") {
                        GroceryScreen(
                            onBack = { navController.popBackStack() },
                            onManageConfig = { navController.navigate("grocery_config") }
                        )
                    }
                    composable("grocery_config") {
                        GroceryConfigScreen(
                            onBack = { navController.popBackStack() },
                            onManageStores = { navController.navigate("stores") },
                            onManageCategories = { navController.navigate("categories") }
                        )
                    }
                    composable("stores") {
                        StoreManagementScreen(onBack = { navController.popBackStack() })
                    }
                    composable("categories") {
                        CategoryManagementScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
