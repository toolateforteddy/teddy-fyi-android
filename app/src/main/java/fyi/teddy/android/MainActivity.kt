package fyi.teddy.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fyi.teddy.android.auth.AuthUtils
import fyi.teddy.android.auth.LoginScreen
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.ui.CategoryManagementScreen
import fyi.teddy.android.grocery.ui.GroceryConfigScreen
import fyi.teddy.android.grocery.ui.GroceryScreen
import fyi.teddy.android.grocery.ui.StoreManagementScreen
import fyi.teddy.android.network.AuthRepository
import fyi.teddy.android.network.NetworkClient
import fyi.teddy.android.network.SyncWorker
import fyi.teddy.android.todo.ui.TodoScreen
import fyi.teddy.android.ui.navigation.Screen
import fyi.teddy.android.ui.screens.AuthedHelloScreen
import fyi.teddy.android.ui.screens.DebugScreen
import fyi.teddy.android.ui.screens.HomeScreen
import fyi.teddy.android.ui.screens.WeatherScreen
import fyi.teddy.android.ui.theme.TeddyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeddyTheme {
                val navController = rememberNavController()
                val session = NetworkClient.session
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                // Global Sync on Resume
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            SyncWorker.enqueue(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(Unit) {
                    session.load(context)
                    val uid = session.userId
                    if (session.idToken != null && uid != null && uid.isNotBlank() && uid != "unknown") {
                        // Ensure local items are claimed if they were somehow missed (e.g. crash after login)
                        val db = AppDatabase.getDatabase(context)
                        db.todoDao().claimUnownedItems(uid)
                        db.groceryDao().claimEverything(uid)

                        if ((session.profilePictureUri == null) || session.profilePictureUri!!.contains(
                                "s2/photos/profile"
                            )
                        ) {
                            session.profilePictureUri =
                                AuthUtils.extractPictureFromToken(session.idToken!!)?.toString()
                        }
                        session.save(context)
                    }
                    Log.d(
                        "MainActivity",
                        "Session loaded: name=${session.userName}, tokenPrefix=${
                            session.idToken?.take(
                                10
                            )
                        }, picUri=${session.profilePictureUri}"
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
                                val success = AuthRepository.login(context, session, result.idToken)
                                if (success) {
                                    // Only claim local items if backend login succeeded and we have a valid ID
                                    val uid = session.userId
                                    if (uid != null && uid.isNotBlank() && uid != "unknown") {
                                        val db = AppDatabase.getDatabase(context)
                                        db.todoDao().claimUnownedItems(uid)
                                        db.groceryDao().claimEverything(uid)
                                    }

                                    // Save the session state (now including backend tokens and claimed UID)
                                    session.save(context)

                                    SyncWorker.enqueue(context)
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
                            onNavigateToTodo = { mode ->
                                navController.navigate(Screen.Todo.createRoute(mode))
                            },
                            onNavigateToGrocery = { navController.navigate(Screen.Grocery.route) },
                            onNavigateToDebug = { navController.navigate(Screen.Debug.route) },
                            onLogout = {
                                scope.launch {
                                    session.clear(context)
                                    NetworkClient.resetClient()
                                    SyncWorker.cancelAllSyncWork(context)
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
                        AuthedHelloScreen(
                            idToken = session.idToken,
                            onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = Screen.Todo.route,
                        arguments = listOf(
                            navArgument("initialMode") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val initialMode = backStackEntry.arguments?.getString("initialMode")
                        TodoScreen(
                            userId = session.userId ?: "unauthed",
                            initialMode = initialMode
                        )
                    }
                    composable(Screen.Grocery.route) {
                        GroceryScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() },
                            onManageConfig = { navController.navigate(Screen.GroceryConfig.route) },
                            onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
                        )
                    }
                    composable(Screen.GroceryConfig.route) {
                        GroceryConfigScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() },
                            onManageStores = { listId ->
                                navController.navigate(
                                    Screen.Stores.createRoute(
                                        listId
                                    )
                                )
                            },
                            onManageCategories = { listId ->
                                navController.navigate(
                                    Screen.Categories.createRoute(
                                        listId
                                    )
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.Stores.route,
                        arguments = listOf(
                            navArgument("listId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val listId = backStackEntry.arguments?.getString("listId")
                        StoreManagementScreen(
                            userId = session.userId ?: "unauthed",
                            listId = listId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Screen.Categories.route,
                        arguments = listOf(
                            navArgument("listId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val listId = backStackEntry.arguments?.getString("listId")
                        CategoryManagementScreen(
                            userId = session.userId ?: "unauthed",
                            listId = listId,
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
