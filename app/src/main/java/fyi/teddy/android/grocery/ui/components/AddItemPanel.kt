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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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

/** Long enough for the bottom sheet's entry animation to place the field before it is focused. */
private val FOCUS_SETTLE_DELAY = 150.milliseconds

/** Whether the add-item entry has room to dock beside the list instead of opening as a modal sheet. */
fun shouldDockAddItemPane(screenWidthDp: Int): Boolean = screenWidthDp >= DOCKED_ADD_PANE_MIN_WIDTH_DP

/**
 * The add-an-item form: entry field, a receipt of what has been filed so far, name suggestions
 * and the submit button.
 *
 * Shared by the modal sheet (compact widths) and the docked pane (wide widths) so rapid entry
 * behaves the same either way.
 *
 * @param addedThisSession names filed since entry began, newest first, shown as the receipt.
 * @param onClose renders a dismiss button when given; the docked pane has nothing to dismiss.
 * @param autoFocusOnAppear claims focus once the container has settled, so the keyboard is up
 *   without a tap. The sheet wants this; the always-present pane would be rude to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemForm(
    input: String,
    suggestions: List<String>,
    isAiReady: Boolean,
    addedThisSession: List<String>,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    autoFocusOnAppear: Boolean = false,
) {
    val groceryColors = GroceryTheme.colors

    Column(modifier = modifier) {
        if (autoFocusOnAppear) {
            LaunchedEffect(Unit) {
                delay(FOCUS_SETTLE_DELAY)
                focusRequester.requestFocus()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "What do we need?",
                style = MaterialTheme.typography.titleLarge,
                color = groceryColors.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (addedThisSession.isNotEmpty()) {
                Text(
                    "${addedThisSession.size} added",
                    style = MaterialTheme.typography.labelLarge,
                    color = groceryColors.accent
                )
            }
        }

        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            placeholder = { Text("e.g. 2 bunches of Bananas", color = groceryColors.onSurfaceMuted) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = groceryColors.onSurface,
                unfocusedTextColor = groceryColors.onSurface,
                focusedContainerColor = groceryColors.well,
                unfocusedContainerColor = groceryColors.well
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            // Both actions do the same thing: a hardware Enter reports Done on some keyboards
            // even when the field asks for Next.
            keyboardActions = KeyboardActions(
                onNext = { onSubmit() },
                onDone = { onSubmit() }
            )
        )

        if (addedThisSession.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(addedThisSession) { added ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = groceryColors.success,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            added,
                            style = MaterialTheme.typography.labelMedium,
                            color = groceryColors.onSurfaceMuted
                        )
                    }
                }
            }
        }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Add it")
            }
            if (onClose != null) {
                OutlinedButton(onClick = onClose) {
                    Text(if (addedThisSession.isEmpty()) "Close" else "Done")
                }
            }
        }
    }
}

/**
 * Docked entry pane for wide screens. It sits beside the list rather than over it, so the list
 * stays readable while the soft keyboard is up and a whole week's worth of items can go in
 * without anything opening or closing.
 */
@Composable
fun DockedAddItemPane(
    input: String,
    suggestions: List<String>,
    isAiReady: Boolean,
    addedThisSession: List<String>,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
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
                addedThisSession = addedThisSession,
                focusRequester = focusRequester,
                onInputChange = onInputChange,
                onSubmit = onSubmit,
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
