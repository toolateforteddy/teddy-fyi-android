# Grocery List Manager Features

This document tracks the features of the Teddy FYI Grocery List Manager.
The Developer will add feature requests to the Requested Features section, and then, upon Prompting,
will look at the Requested Features section, implement each feature, then move that item to the
Current Features List.


## Current Features
- [x] **Local Persistence**: Uses Room database to store grocery items on the device.
- [x] **Add Items**: Input fields to add items with a name and optional quantity.
- [x] **Quantity Support**: Displays quantity for each item if specified (default is 1).
- [x] **Mark as Bought**: Checkbox to toggle "bought" status, which applies a strikethrough to the item name.
- [x] **Delete Items**: Ability to remove individual items from the list.
- [x] **Reactive UI**: The list updates automatically when the database changes using Kotlin Flow.
- [x] **Common Store Management**: Configuration screen to manage a list of stores (Trader Joe's, Whole Foods, etc.).
- [x] **Store tagging**: Items can be tagged as available only in a subset of stores. By default, they are available in all.
- [x] **Optimal Purchase Store**: Reminds the user if an item is cheaper at a different store during planning/shopping. Allows recording the price paid.
- [x] **Phased usage**: 
    - [x] **Need Mode**: Generic item gathering.
    - [x] **Planning Mode**: Select which store or stores to plan for. Shows only available items.
    - [x] **Shopping Mode**: Single store view. Tap to add price and see history.
- [x] **Item Autocomplete**: When adding a new item, if a similar item is in the database, offer it as a suggestion.
- [x] **Ordering of list items**: Items can be reordered using up/down arrows in Edit Mode.
- [x] **Ordering of Store Lists**: Stores can be reordered using up/down arrows in the Manage Stores screen.
- [x] **Store Default Status**: Stores can be toggled to be "OFF" by default for new items.
- [x] **Edit Quantity**: Tapping the quantity label on an item allows editing it.
- [x] **Back Arrow Visual Noise**: Removed the back arrow from the navigation bar.
- [x] **Categories**: Group items by grocery store section (Produce, Dairy, etc.).
- [x] **Config Management**: Consolidated store and category management into a single Settings view.
- [x] **Recurring Groceries**: Added a "Recommended" button in planning mode to quickly re-add frequently bought items.

## Requested Features
- [ ] **Auth Integration**: Associate grocery lists with a user account.

## Planned Features
- [ ] **Cloud Sync**: Sync grocery data with the backend.
- [ ] **Clear Bought Items**: Quickly remove all items marked as bought.
- [ ] **Receipt Mode**: Submit pictures or screenshots of a receipt to populate the list with known items and prices.
- [ ] **Shared Lists**: Allow real-time collaboration so multiple users (e.g., family members) can edit and check off items on the same list simultaneously.
- [ ] **Price Analytics & History**: A stats dashboard showing spending trends over time, price comparisons for the same item across different stores, and monthly grocery spend by category.
- [ ] **Smart Sorting by Aisle**: Automatically reorder the shopping list based on the typical layout of a selected store to minimize walking back and forth.
- [ ] **Voice Entry**: A hands-free way to add items to the "Need" list using speech-to-text.
- [ ] **Barcode Scanner**: Use the phone's camera to scan product barcodes for instant addition to the list with accurate brand and unit info.
- [ ] **Inventory / Pantry Tracker**: A mode to track what you currently have in stock at home, with "low stock" alerts that automatically move items to the shopping list.
- [ ] **Recipe Ingredient Import**: Integration to parse ingredient lists from URLs or photos and add all required items to the grocery list in one tap.
- [ ] **Unit Support & Conversion**: Robust handling of units (oz, lbs, kg, count) with automatic conversion and price-per-unit calculations to find the best value.
- [ ] **Store Maps**: Visual aisle mapping for supported major retailers to show exactly where an item is located on a store floor plan.
- [ ] **Trip Budgeting**: Set a spending limit before a shopping trip and see a running total estimate as you check off items and enter prices.

## Bugs
- [x] **Entering Shopping Mode Crash**: Entering Shopping Mode without any items crashes the app.
- [x] **Manage Stores Submit on Enter**: Tapping enter on the keyboard should be the same as tapping plus after adding a store.
- [x] **Capitalized Phases**: Please don't yell at me with all caps in the title of each screen.
- [x] **Cursor Position After Add**: Cursor now correctly returns to the item name field after adding an item.
- [x] **No Checkboxes in Needs or Planning Mode**: Checkboxes are now only visible in Shopping Mode.
- [x] **Planning page Pill Wrap**: On planning screen, the pills now wrap to a second line correctly.
- [x] **First Letter Cap**: All text boxes in this section of the app now default to sentence capitalization.
