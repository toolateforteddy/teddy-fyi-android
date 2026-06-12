package fyi.teddy.android.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

private val ICON_CATEGORIES = mapOf(
    Icons.Default.Code to setOf(
        "code", "coding", "deploy", "prod", "production", "program", "programming",
        "develop", "developing", "developer", "software", "fix", "bug", "api",
        "db", "database", "git", "github"
    ),
    Icons.Default.DirectionsCar to setOf(
        "car", "cars", "maintain", "maintenance", "vehicle", "drive", "driving",
        "tire", "tires", "oil", "wash", "mechanic"
    ),
    Icons.Default.DirectionsBike to setOf(
        "bike", "bicycle"
    ),
    Icons.Default.Shower to setOf(
        "shower"
    ),
    Icons.Default.Kitchen to setOf(
        "dishwasher", "dishes", "wash"
    ),
    Icons.Default.Forum to setOf(
        "interview", "respond", "vitally", "call", "calls", "forum", "email",
        "emails", "follow", "message", "messages", "chat", "slack", "zoom",
        "meeting", "talk"
    ),
    Icons.Default.ShoppingBasket to setOf(
        "pot", "kitchen", "cook", "cooking", "stock", "acquire", "store",
        "shop", "shopping", "buy", "purchase", "mall"
    ),
    Icons.Default.Restaurant to setOf(
        "osso", "ingredient", "ingredients", "food", "dinner", "eat", "eating",
        "buco", "meal", "meals", "groc", "grocery", "recipe", "lunch",
        "breakfast", "restaurant"
    )
)

private val MATERIAL_ICON_MAP = mapOf(
    "Build" to Icons.Default.Build,
    "Home" to Icons.Default.Home,
    "Plumbing" to Icons.Default.Plumbing,
    "ElectricalServices" to Icons.Default.ElectricalServices,
    "CleaningServices" to Icons.Default.CleaningServices,
    "Brush" to Icons.Default.Brush,
    "Yard" to Icons.Default.Yard,
    "Work" to Icons.Default.Work,
    "AttachMoney" to Icons.Default.AttachMoney,
    "CreditCard" to Icons.Default.CreditCard,
    "ReceiptLong" to Icons.Default.ReceiptLong,
    "Email" to Icons.Default.Email,
    "Phone" to Icons.Default.Phone,
    "Analytics" to Icons.Default.Analytics,
    "ShoppingCart" to Icons.Default.ShoppingCart,
    "LocalShipping" to Icons.Default.LocalShipping,
    "DirectionsCar" to Icons.Default.DirectionsCar,
    "Storefront" to Icons.Default.Storefront,
    "LocalPharmacy" to Icons.Default.LocalPharmacy,
    "FitnessCenter" to Icons.Default.FitnessCenter,
    "DirectionsBike" to Icons.Default.DirectionsBike,
    "DirectionsRun" to Icons.Default.DirectionsRun,
    "MedicalInformation" to Icons.Default.MedicalInformation,
    "Restaurant" to Icons.Default.Restaurant,
    "Bed" to Icons.Default.Bed,
    "Event" to Icons.Default.Event,
    "Schedule" to Icons.Default.Schedule,
    "List" to Icons.Default.List,
    "Group" to Icons.Default.Group,
    "Person" to Icons.Default.Person,
    "Settings" to Icons.Default.Settings,
    "Computer" to Icons.Default.Computer,
    "MenuBook" to Icons.Default.MenuBook,
    "Movie" to Icons.Default.Movie,
    "Palette" to Icons.Default.Palette,
    "MusicNote" to Icons.Default.MusicNote,
    "Pets" to Icons.Default.Pets,
    "Flight" to Icons.Default.Flight,
    "Eco" to Icons.Default.Eco,
    "Lock" to Icons.Default.Lock
)

fun getIconForTask(title: String, defaultIcon: ImageVector): ImageVector {
    val taskWords = title.lowercase().split(Regex("[\\s,.:;?!'\"()_-]+")).toSet()

    for ((icon, keywords) in ICON_CATEGORIES) {
        // Check if there is any intersection between task words and category keywords
        if (taskWords.any { word ->
                keywords.any { keyword ->
                    word == keyword || word.startsWith(keyword)
                }
            }) {
            return icon
        }
    }

    return defaultIcon
}

fun getIconByName(name: String?): ImageVector? {
    if (name == null) return null
    return MATERIAL_ICON_MAP[name] ?: MATERIAL_ICON_MAP[name.replaceFirstChar { it.uppercase() }]
}
