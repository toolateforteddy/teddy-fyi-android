package fyi.teddy.android.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("hello")
    object Weather : Screen("weather")
    object Authed : Screen("authed")
    object Todo : Screen("todo?initialMode={initialMode}") {
        fun createRoute(initialMode: String? = null): String {
            return if (initialMode != null) "todo?initialMode=$initialMode" else "todo"
        }
    }
    object Grocery : Screen("grocery")
    object GroceryConfig : Screen("grocery_config")
    object Stores : Screen("stores")
    object Categories : Screen("categories")
    object Debug : Screen("debug")
}
