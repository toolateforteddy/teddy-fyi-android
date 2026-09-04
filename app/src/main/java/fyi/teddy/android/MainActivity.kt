package fyi.teddy.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fyi.teddy.android.auth.AuthUtils
import fyi.teddy.android.auth.GoogleSignInResult
import fyi.teddy.android.auth.LoginScreen
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.ui.CategoryManagementScreen
import fyi.teddy.android.grocery.ui.GroceryConfigScreen
import fyi.teddy.android.grocery.ui.GroceryScreen
import fyi.teddy.android.grocery.ui.StoreManagementScreen
import fyi.teddy.android.network.AuthRepository
import fyi.teddy.android.network.DevicePairingRepository
import fyi.teddy.android.network.NetworkClient
import fyi.teddy.android.network.SyncWorker
import fyi.teddy.android.todo.ui.TodoScreen
import fyi.teddy.android.ui.navigation.Screen
import fyi.teddy.android.ui.screens.AuthedHelloScreen
import fyi.teddy.android.ui.screens.DebugScreen
import fyi.teddy.android.ui.screens.HomeScreen
import fyi.teddy.android.ui.screens.WeatherScreen
import fyi.teddy.android.ui.theme.TeddyTheme
import fyi.teddy.android.widget.GroceryWidget
import fyi.teddy.android.widget.TodoTacticalWidget
import fyi.teddy.android.widget.WidgetUpdateHelper
import kotlinx.coroutines.launch

/**
 * Where sign-in lands, and where the widgets and the session bootstrap send the app.
 *
 * The full build opens on the dashboard, which is a todo surface. The grocery build has no
 * dashboard and no todo surfaces at all: it opens on the grocery list, which starts on the
 * Needs phase, so the first thing on screen is the thing that build exists for.
 */
private val HOME_DESTINATION: String
    get() = if (BuildConfig.INCLUDE_TODO) Screen.Home.route else Screen.Grocery.route

/**
 * What both ways in have to do once there is a session, whichever route minted it.
 *
 * Rows made before anybody signed in are unowned; they are adopted here so a first sign-in
 * keeps the list that is already on the device rather than syncing an empty one over it. The
 * client is then rebuilt, because the one that made the login call is primed with the null
 * token it had before there was a session.
 */
private suspend fun adoptSessionAndSync(context: android.content.Context, session: UserSession) {
    val uid = session.userId
    if (!uid.isNullOrBlank() && uid != "unknown") {
        val db = AppDatabase.getDatabase(context)
        db.todoDao().claimUnownedItems(uid)
        db.groceryDao().claimEverything(uid)
    }
    session.save(context)
    NetworkClient.resetClient()
    SyncWorker.enqueue(context)
    SyncWorker.schedulePeriodicSync(context)
}

class MainActivity : ComponentActivity() {
    private val webAuthResult = mutableStateOf<GoogleSignInResult?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthRedirect(intent)
    }

    override fun onStop() {
        super.onStop()
        WidgetUpdateHelper.updateAllTodoWidgets(this)
        WidgetUpdateHelper.updateAllGroceryWidgets(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            WidgetUpdateHelper.updateAllTodoWidgets(this)
            WidgetUpdateHelper.updateAllGroceryWidgets(this)
        }
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "com.googleusercontent.apps.34718544535-a8csa0c9ihbe5543dcl21h4ruvilpjav") {
            // fragment (after #) usually contains the id_token in response_type=id_token
            val fragment = data.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate {
                    val parts = it.split("=")
                    parts[0] to parts.getOrNull(1)
                }
                val idToken = params["id_token"]
                if (idToken != null) {
                    Log.d("MainActivity", "Extracted ID Token from redirect")
                    val displayName = "Web User" // We'd need to decode more or use another scope for name
                    val pic = AuthUtils.extractPictureFromToken(idToken)
                    webAuthResult.value = GoogleSignInResult(displayName, idToken, pic)
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "ComplexCondition", "LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkClient.initialize(this)
        handleAuthRedirect(intent)

        setContent {
            TeddyTheme {
                val navController = rememberNavController()
                val session = NetworkClient.session
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(intent) {
                    val targetAction = intent?.getStringExtra("widget_action") ?: intent?.action
                    when {
                        targetAction == TodoTacticalWidget.ACTION_OPEN_TODO &&
                            BuildConfig.INCLUDE_TODO -> {
                            navController.navigate(Screen.Todo.createRoute("TODAY"))
                        }
                        targetAction == GroceryWidget.ACTION_OPEN_GROCERY -> {
                            navController.navigate(Screen.Grocery.route)
                        }
                    }
                }

                // Handle web auth result
                LaunchedEffect(webAuthResult.value) {
                    val result = webAuthResult.value
                    if (result != null) {
                        webAuthResult.value = null // Consume it
                        
                        session.userName = result.displayName
                        session.idToken = result.idToken
                        session.userId = AuthUtils.extractUserIdFromToken(result.idToken)
                        session.profilePictureUri = result.profilePictureUri?.toString()

                        scope.launch {
                            val success = AuthRepository.login(context, session, result.idToken)
                            if (success) {
                                adoptSessionAndSync(context, session)
                                navController.navigate(HOME_DESTINATION) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                Log.e("MainActivity", "Backend login failed from web fallback")
                            }
                        }
                    }
                }

                // Global Sync on Resume & Widget Update on Loss of Focus
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                SyncWorker.enqueue(context)
                            }
                            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                                WidgetUpdateHelper.updateAllTodoWidgets(context)
                                WidgetUpdateHelper.updateAllGroceryWidgets(context)
                            }
                            else -> {}
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
                    val isValidUser = session.isSignedIn && uid != null && uid.isNotBlank() && uid != "unknown"
                    if (isValidUser) {
                        // Ensure local items are claimed if they were somehow missed (e.g. crash after login)
                        val db = AppDatabase.getDatabase(context)
                        db.todoDao().claimUnownedItems(uid)
                        db.groceryDao().claimEverything(uid)

                        // A paired session has no Google ID token to read a picture out of;
                        // there is simply no avatar for it, which every avatar site already
                        // handles as a null.
                        val idToken = session.idToken
                        if (idToken != null &&
                            ((session.profilePictureUri == null) ||
                                session.profilePictureUri!!.contains("s2/photos/profile"))
                        ) {
                            session.profilePictureUri =
                                AuthUtils.extractPictureFromToken(idToken)?.toString()
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
                    if (session.isSignedIn) {
                        SyncWorker.enqueueIfNecessary(context)
                        SyncWorker.schedulePeriodicSync(context)
                        val targetAction = intent?.getStringExtra("widget_action") ?: intent?.action
                        val destination = when {
                            targetAction == TodoTacticalWidget.ACTION_OPEN_TODO &&
                                BuildConfig.INCLUDE_TODO -> Screen.Todo.createRoute("TODAY")
                            targetAction == GroceryWidget.ACTION_OPEN_GROCERY -> Screen.Grocery.route
                            else -> HOME_DESTINATION
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = Screen.Login.route) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginSuccess = { result ->
                                session.userName = result.displayName
                                session.idToken = result.idToken
                                session.userId = AuthUtils.extractUserIdFromToken(result.idToken)
                                session.profilePictureUri = result.profilePictureUri?.toString()

                                scope.launch {
                                    val success =
                                        AuthRepository.login(context, session, result.idToken)
                                    if (success) {
                                        adoptSessionAndSync(context, session)
                                        navController.navigate(HOME_DESTINATION) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        Log.e("MainActivity", "Backend login failed")
                                    }
                                }
                            },
                            // Pairing has already been through the API: the tokens in hand are
                            // the ones /auth/login would have returned, so there is nothing to
                            // exchange and no Google ID token on this device to exchange it with.
                            onPaired = { paired ->
                                session.userId = paired.userId
                                session.updateTokens(paired.accessToken, paired.refreshToken)

                                scope.launch {
                                    adoptSessionAndSync(context, session)
                                    navController.navigate(HOME_DESTINATION) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }
                    if (BuildConfig.INCLUDE_TODO) {
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
                    }
                    composable(Screen.Weather.route) {
                        WeatherScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Authed.route) {
                        AuthedHelloScreen(
                            idToken = session.idToken,
                            onBack = { navController.popBackStack() })
                    }
                    if (BuildConfig.INCLUDE_TODO) {
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
                        // The full build signs out from the dashboard. The grocery build has no
                        // dashboard, so grocery settings is where its only way out lives.
                        val signOut: (() -> Unit)? = if (BuildConfig.INCLUDE_TODO) {
                            null
                        } else {
                            {
                                scope.launch {
                                    session.clear(context)
                                    NetworkClient.resetClient()
                                    SyncWorker.cancelAllSyncWork(context)
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Grocery.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                        GroceryConfigScreen(
                            userId = session.userId ?: "unauthed",
                            onBack = { navController.popBackStack() },
                            onSignOut = signOut,
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
