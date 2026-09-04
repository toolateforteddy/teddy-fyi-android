package fyi.teddy.android.grocery.ui.components

import fyi.teddy.android.grocery.data.GroceryList

/** A switchable grocery space. [id] is null for the implicit default list. */
data class GrocerySpaceOption(val id: String?, val name: String)

/**
 * The spaces a person can switch to right now: the named lists, plus the implicit
 * default list while anything still lives in it (or while it is what is on screen).
 */
fun grocerySpaceOptions(
    lists: List<GroceryList>,
    hasItemsInDefaultList: Boolean,
    selectedListId: String?,
): List<GrocerySpaceOption> = buildList {
    if (hasItemsInDefaultList || selectedListId == null) {
        add(GrocerySpaceOption(id = null, name = "Default List"))
    }
    lists.forEach { add(GrocerySpaceOption(id = it.id, name = it.name)) }
}

fun List<GrocerySpaceOption>.nameFor(selectedListId: String?): String =
    firstOrNull { it.id == selectedListId }?.name ?: "Default List"
