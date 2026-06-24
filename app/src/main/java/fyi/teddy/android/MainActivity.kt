package fyi.teddy.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import fyi.teddy.android.network.SyncWorker
import fyi.teddy.android.network.NetworkClient
import fyi.teddy.android.todo.ui.TodoScreen
import fyi.teddy.android.ui.navigation.Screen
import fyi.teddy.android.ui.screens.AuthedHelloScreen
import fyi.teddy.android.ui.screens.HomeScreen
import fyi.teddy.android.ui.screens.WeatherScreen
import fyi.teddy.android.ui.screens.DebugScreen
import fyi.teddy.android.ui.theme.TeddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeddyTheme {
                val navController = rememberNavController()
                val session = NetworkClient.session
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    session.load(context)
                    if (session.idToken != null) {
                        if (session.userId == null) {
                            session.userId = AuthUtils.extractUserIdFromToken(session.idToken!!)
                        }
                        if ((session.profilePictureUri == null) || session.profilePictureUri!!.contains("s2/photos/profile")) {
                            session.profilePictureUri = AuthUtils.extractPictureFromToken(session.idToken!!)?.toString()
                        }
                        session.save(context)
                    }
                    Log.d(
                        "MainActivity", 
                        "Session loaded: name=${session.userName}, tokenPrefix=${session.idToken?.take(10)}, picUri=${session.profilePictureUri}"
                    )
                    if (session.idToken != null) {
                        SyncWorker.enqueueIfNecessary(context)
                        SyncWorker.schedulePeriodicSync(context)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = Screen.Login.route) {
                    composable(Screen.Login.route) {
                        LoginScreen { result ->
                            session.userName = result.displayName
                            session.idToken = result.idToken
                            session.userId = AuthUtils.extractUserIdFromToken(result.idToken)
                            session.profilePictureUri = result.profilePictureUri?.toString()
                            
                            scope.launch { 
                                val success = fyi.teddy.android.network.AuthRepository.login(context, session, result.idToken)
                                if (success) {
                                    SyncWorker.enqueueIfNecessary(context)
                                    SyncWorker.schedulePeriodicSync(context)
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    Log.e("MainActivity", "Backend login failed")
                                }
                            }
                        }
                    }
                    composable(Screen.Home.route) {
                        HomeScreen(
                            userId = session.userId,
                            userName = session.userName,
                            profilePic = session.profilePictureUri,
                            onNavigateToWeather = { navController.navigate(Screen.Weather.route) },
                            onNavigateToAuthed = { navController.navigate(Screen.Authed.route) },
                            onNavigateToTodo = { mode -> 
                                navController.navigate(Screen.Todo.createRoute(mode)) 
                            },
                            onNavigateToGrocery = { navController.navigate(Screen.Grocery.route) },
                            onNavigateToDebug = { navController.navigate(Screen.Debug.route) },
                            onLogout = {
                                scope.launch { 
                                    session.clear(context)
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable(Screen.Weather.route) {
                        WeatherScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Authed.route) {
                        AuthedHelloScreen(idToken = session.idToken, onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = Screen.Todo.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("initialMode") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val initialMode = backStackEntry.arguments?.getString("initialMode")
                        TodoScreen(
                            userId = session.userId ?: "unauthed",
                            initialMode = initialMode,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Grocery.route) {
                        GroceryScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() },
                        ) {
                            navController.navigate(Screen.GroceryConfig.route)
                        }
                    }
                    composable(Screen.GroceryConfig.route) {
                        GroceryConfigScreen(
                            onBack = { navController.popBackStack() },
                            onManageStores = { navController.navigate(Screen.Stores.route) },
                            onManageCategories = { navController.navigate(Screen.Categories.route) }
                        )
                    }
                    composable(Screen.Stores.route) {
                        StoreManagementScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Categories.route) {
                        CategoryManagementScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Debug.route) {
                        DebugScreen(
                            idToken = session.idToken,
                            onNavigateToAuthed = { navController.navigate(Screen.Authed.route) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
