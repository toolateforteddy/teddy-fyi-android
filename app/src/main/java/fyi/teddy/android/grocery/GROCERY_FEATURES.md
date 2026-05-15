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

## Requested Features

## Planned Features
- [ ] **Auth Integration**: Associate grocery lists with a user account.
- [ ] **Cloud Sync**: Sync grocery data with the backend.
- [ ] **Categories**: Group items by grocery store section (Produce, Dairy, etc.).
- [ ] **Clear Bought Items**: Quickly remove all items marked as bought.
- [ ] **Receipt Mode**: Submit pictures or screenshots of a receipt to populate the list with known items and prices.

## Bugs
- [x] **Entering Shopping Mode Crash**: Entering Shopping Mode without any items crashes the app.
- [x] **Manage Stores Submit on Enter**: Tapping enter on the keyboard should be the same as tapping plus after adding a store.
- [x] **Capitalized Phases**: Please don't yell at me with all caps in the title of each screen.
