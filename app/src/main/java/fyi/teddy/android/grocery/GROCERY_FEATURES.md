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

## Requested Features

## Planned Features
- [ ] **Auth Integration**: Associate grocery lists with a user account.
- [ ] **Cloud Sync**: Sync grocery data with the backend.
- [ ] **Categories**: Group items by grocery store section (Produce, Dairy, etc.).
- [ ] **Clear Bought Items**: Quickly remove all items marked as bought.
- [ ] **Store tagging**: Some items are only available at some stores and not others. At first, we should assume a new item is available at all the supported stores. But, it should be easy to tag an item as being only available in a subset of stores.
- [ ] **Optimal Purchase Store**: Some items, such as Fage Yogurt, is a full dollar less expensive at Whole Foods compared to at Stop & Shop. There should be a way to remind the user during both planning and shopping mode if they are considering buying something at a more expensive location. And in turn, there should be a way to add the price paid for an item.
- [ ] **Common Store Management**: There should be a configuration screen to build out a list of stores we want to be able to leverage commonly. For example, Trader Joe's, Whole Foods, and Stop & Shop.
- [ ] **Phased usage**: There are at least three phases in the life of a grocery list. First is Need Mode. This is the mode you are in as you go about your day. You notice things that are running out, and you add things to your grocery list without caring which store you need to go to get that item. Second is planning mode. This comes after you decide "I am going to go to the grocery store, and I plan to go to this one in particular." During planning mode, items from the core list that are available at the selected store should be shown, but other items should be hidden for later. During this phase, you can still add new items to the list. If the item you add  
  - [ ] **Need Mode**: Generic item gathering.
  - [ ] **Planning Mode**: In planning mode, I should be able to select which store or stores I intend to go to. 
  - [ ] **Shopping Mode**: Limited to showing a single store. Tapping on an item on the list should show a history of prices paid for that item, and at which store.
- [ ] **Receipt Mode**: I just thought of a 4th phase. This is after the shopping trip data collection. There should be a way to submit pictures or screenshots of a receipt to populate the list if known items and the prices of those items at a given store.

## Bugs
