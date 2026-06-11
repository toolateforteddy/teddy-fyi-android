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

