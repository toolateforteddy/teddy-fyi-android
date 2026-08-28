package fyi.teddy.android.network

/**
 * Every URL this app talks to, in one place.
 *
 * Before this existed the backend host was spelled out at each call site, so pointing the
 * app at a staging cluster meant grepping for the hostname and hoping none were missed.
 */
object ApiRoutes {
    const val BACKEND_BASE = "https://api-rust.teddy.fyi"
    const val SITE_BASE = "https://teddy.fyi"

    // Auth
    const val LOGIN = "$BACKEND_BASE/auth/login"
    const val REFRESH = "$BACKEND_BASE/auth/refresh"

    // Sync
    const val SYNC = "$BACKEND_BASE/api/sync"

    // Misc app endpoints
    const val HEALTH_CHECK = "$BACKEND_BASE/api/hc"
    const val ASSIGN_ICON = "$BACKEND_BASE/api/assign-icon"

    // Shared grocery lists
    const val LIST_INVITE = "$BACKEND_BASE/api/lists/invite"
    const val LIST_JOIN = "$BACKEND_BASE/api/lists/join"

    /** Unauthenticated cluster liveness probe. */
    const val CLUSTER_HEALTH = "$SITE_BASE/"

    /**
     * Open-Meteo current weather. Coordinates are fixed (Medford/Somerville, MA) because the
     * app declares no location permission.
     */
    const val WEATHER = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=42.4154&longitude=-71.1565&current_weather=true&temperature_unit=fahrenheit"
}
