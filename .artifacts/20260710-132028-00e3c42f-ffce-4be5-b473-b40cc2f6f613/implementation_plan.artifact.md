# Change Todo Item Colors based on List Color

Match the color of the checkbox and icons in `TodoItemRow` to the color of the `TodoList` the item belongs to.

## Proposed Changes

### UI Components

#### [HexUI.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/todo/ui/components/HexUI.kt)

- Update `HexCheckbox` to accept a `color: Color` parameter.
- Use the provided `color` for background and border when checked.

```kotlin
@Composable
fun HexCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonTeal
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(HexagonShape())
            .background(if (checked) color else Color.Transparent)
            .border(
                BorderStroke(2.dp, if (checked) color else MutedGrey),
                HexagonShape()
            ),
        contentAlignment = Alignment.Center
    ) {
        // No tick needed, just solid fill as requested
    }
}
```

#### [TodoItemRow.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/todo/ui/components/TodoItemRow.kt)

- Calculate `itemColor` based on the item's `listId` and the provided `allLists`.
- Pass `itemColor` to `HexCheckbox`.
- Update other icons (explicit icon, daily icon) to use `itemColor` instead of hardcoded `NeonTeal`.

```kotlin
val itemColor = remember(item.listId, allLists) {
    allLists.find { it.id == item.listId }?.let { list ->
        try {
            Color(android.graphics.Color.parseColor(list.colorHex))
        } catch (_: Exception) {
            NeonTeal
        }
    } ?: NeonTeal
}
```

## Verification Plan

### Manual Verification
1.  **Open Todo List**: Navigate to the Todo section.
2.  **Assign Item to List**: Create or edit a todo item and assign it to a list with a specific color (e.g., Red).
3.  **Check Color**: Verify that the checkbox and icons for that item are now Red.
4.  **Change List Color**: Edit the list's color and verify the item's color updates accordingly.
5.  **Unassigned Items**: Verify that items not in a list still use the default `NeonTeal`.
