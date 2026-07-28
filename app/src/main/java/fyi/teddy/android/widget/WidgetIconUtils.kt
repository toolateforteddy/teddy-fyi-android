package fyi.teddy.android.widget

import fyi.teddy.android.R

object WidgetIconUtils {

    /**
     * Resolves an explicit Android framework vector drawable resource ID (R.drawable.ic_*)
     * for a task based on its assigned icon string or title keywords, matching the app's IconUtils mapping.
     */
    fun getWidgetIconRes(iconName: String?, title: String): Int {
        if (!iconName.isNullOrBlank()) {
            val key = iconName.trim().lowercase()
            when {
                key.contains("kitchen") || key.contains("clean") || key.contains("dish") -> return R.drawable.ic_kitchen
                key.contains("palette") || key.contains("brush") || key.contains("paint") || key.contains("art") -> return R.drawable.ic_palette
                key.contains("email") || key.contains("mail") || key.contains("message") || key.contains("forum") -> return R.drawable.ic_email
                key.contains("eco") || key.contains("plant") || key.contains("yard") || key.contains("compost") || key.contains("leaf") -> return R.drawable.ic_eco
                key.contains("work") -> return R.drawable.ic_work
                key.contains("list") -> return R.drawable.ic_list
                key.contains("code") || key.contains("dev") -> return R.drawable.ic_code
                key.contains("build") || key.contains("tool") -> return R.drawable.ic_build
                key.contains("shop") || key.contains("cart") -> return R.drawable.ic_shopping
                key.contains("car") || key.contains("auto") -> return R.drawable.ic_car
            }
        }

        val words = title.lowercase().split(Regex("[\\s,.:;?!'\"()_-]+")).toSet()
        return when {
            // Dishwasher / Cleaning / Household
            words.any { it in setOf("dishwasher", "dishes", "dish", "wash", "clean", "cleaning", "shower") } -> R.drawable.ic_kitchen
            
            // Games / Art / Palette
            words.any { it in setOf("game", "strands", "palette", "paint", "color", "colors", "art", "draw") } -> R.drawable.ic_palette

            // Letters / Email / Matching / Communication
            words.any { it in setOf("letter", "letters", "email", "emails", "mail", "message", "messages", "matching", "respond", "call", "chat") } -> R.drawable.ic_email

            // Compost / Eco / Plant / Yard
            words.any { it in setOf("compost", "plant", "plants", "eco", "leaf", "yard", "bag", "bags", "garden") } -> R.drawable.ic_eco

            // Code / Software / Algo / Bug
            words.any { it in setOf("code", "coding", "deploy", "prod", "bug", "fix", "api", "git", "github", "db", "algo", "algorithm") } -> R.drawable.ic_code

            // Work / Career / Office
            words.any { it in setOf("work", "job", "career", "office", "meeting") } -> R.drawable.ic_work

            // Car / Driving
            words.any { it in setOf("car", "cars", "drive", "oil", "wash", "mechanic", "tire") } -> R.drawable.ic_car

            // Shopping / Purchases
            words.any { it in setOf("shop", "shopping", "buy", "purchase", "store", "groc", "grocery", "order") } -> R.drawable.ic_shopping

            // Tools / Maintenance
            words.any { it in setOf("build", "repair", "plumb", "tool") } -> R.drawable.ic_build

            else -> R.drawable.ic_list
        }
    }

    /**
     * Helper to check if a string is a single emoji character (e.g. "🛒", "⚡").
     */
    fun isEmoji(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = text.trim()
        if (trimmed.any { it in 'a'..'z' || it in 'A'..'Z' }) return false
        val codePoint = trimmed.codePointAt(0)
        return codePoint in 0x1F600..0x1F64F ||
                codePoint in 0x1F300..0x1F5FF ||
                codePoint in 0x1F680..0x1F6FF ||
                codePoint in 0x2600..0x26FF ||
                codePoint in 0x2700..0x27BF ||
                codePoint in 0x1F900..0x1F9FF ||
                codePoint in 0x1FA70..0x1FAFF
    }
}
