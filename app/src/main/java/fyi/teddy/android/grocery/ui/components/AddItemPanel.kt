package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

/**
 * Width at which the add-item entry stops being a modal sheet and becomes a docked pane.
 *
 * Below this the sheet is the only thing that fits. At or above it a full-width modal slab plus
 * a soft keyboard buries the very list you are adding to, and there is room to sit the entry
 * field beside the list instead. The threshold leaves at least [DOCKED_ADD_PANE_WIDTH] for the
 * pane and a comfortable remainder for the list.
 */
const val DOCKED_ADD_PANE_MIN_WIDTH_DP = 720

/** Width of the docked entry pane on wide screens. */
val DOCKED_ADD_PANE_WIDTH = 320.dp

/** Whether the add-item entry has room to dock beside the list instead of opening as a modal sheet. */
fun shouldDockAddItemPane(screenWidthDp: Int): Boolean = screenWidthDp >= DOCKED_ADD_PANE_MIN_WIDTH_DP

/**
 * The add-an-item form: prompt, entry field, name suggestions and the submit button.
 *
 * Shared by the modal sheet (compact widths) and the docked pane (wide widths) so the two
 * entry points stay in step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemForm(
    input: String,
    suggestions: List<String>,
    isAiReady: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val groceryColors = GroceryTheme.colors

    Column(modifier = modifier) {
        Text(
            "What do we need?",
            style = MaterialTheme.typography.titleLarge,
            color = groceryColors.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            placeholder = { Text("e.g. 2 bunches of Bananas", color = groceryColors.onSurfaceMuted) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = groceryColors.onSurface,
                unfocusedTextColor = groceryColors.onSurface,
                focusedContainerColor = groceryColors.well,
                unfocusedContainerColor = groceryColors.well
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        if (suggestions.isNotEmpty()) {
            Text(
                "Suggestions",
                style = MaterialTheme.typography.labelMedium,
                color = groceryColors.onSurfaceMuted,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onInputChange(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }

        if (isAiReady) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = groceryColors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Smart sorting is on",
                    style = MaterialTheme.typography.labelSmall,
                    color = groceryColors.accent
                )
            }
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("Add it")
        }
    }
}

/**
 * Docked entry pane for wide screens. It sits beside the list rather than over it, so the
 * list stays readable while the soft keyboard is up and several items can be added in a row
 * without reopening anything.
 */
@Composable
fun DockedAddItemPane(
    input: String,
    suggestions: List<String>,
    isAiReady: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val groceryColors = GroceryTheme.colors

    Surface(
        modifier = modifier,
        color = groceryColors.card,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AddItemForm(
                input = input,
                suggestions = suggestions,
                isAiReady = isAiReady,
                onInputChange = onInputChange,
                onSubmit = onSubmit,
                focusRequester = focusRequester,
            )
            Text(
                "Stays open, so keep adding.",
                style = MaterialTheme.typography.labelSmall,
                color = groceryColors.onSurfaceMuted,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
