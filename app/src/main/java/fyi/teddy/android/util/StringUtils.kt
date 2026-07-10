package fyi.teddy.android.util

import java.util.Locale

object StringUtils {
    /**
     * Formats a title/name by:
     * 1. Trimming whitespace from both ends.
     * 2. Capitalizing each word (Title Case), unless the word is all caps (acronym preservation).
     */
    fun formatTitle(title: String): String {
        return title.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
            if (word.all { !it.isLowerCase() } && word.any { it.isLetter() }) {
                word
            } else {
                word.lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
        }
    }
}
