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
- [x] **Auth Integration**: Associate grocery lists with a user account.
- [x] **Shopping Mode Done State**: Added a new visual only category called "In Cart" on the shopping mode where checked items are moved after a 2-second delay.
- [x] **Trip Complete Button**: On the shopping tab, add a Trip Complete button. Maybe it looks like a check icon in the top. After a confirmation modal to make sure you don't execute on purpose, all items in the "in cart" category are marked as "done". Done items do not show up in any screen until they are readded as new need items.

## Requested Features

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
- [ ] **Auto Categorize by LLM**: Add button to send all uncategorized items to an LLM api to generate categories for them. The prompt should include all current categories and explain that for each item it should return one of the existing categories that the item best fits in.
- [ ] **Create an Edit Menu**: The edit menu should have options for "edit name", "edit category", "edit quantity".
- [ ] **Long Press to Edit**: When long pressing on an item, it should open the edit menu.
- [ ] **Adding item skip quantity**: When adding an item, the enter button on the keyboard should be "submit" and not tab. The quantity will almost always be 1, so don't make the user tab through that field every time.  

## Future Features Requiring DB Schema Changes (Pre-Sync)
The following features are ranked by **User Desirability Score (1-10)**. Implementing them before finalizing the cloud backend and making it the source of truth is highly recommended, as introducing new tables, structural relationships, or changes to core data types becomes exponentially more complex once bidirectional client-server synchronization is live.

1. **Shared Lists / Multi-List Collaboration**
   * **Score**: `9.5/10`
   * **Required Schema Changes**:
     * Create a `lists` table (`id` [UUID], `name`, `owner_id`, `created_at`, `sync_state`, `version`, `is_deleted`).
     * Create a `list_members` table (`id`, `list_id` [FK], `user_id`, `role`, `joined_at`).
     * Update `grocery_items` table to include a `list_id` column (FK linking to `lists`).
   * **Why it's harder after Sync**: Migrating from a single implicit flat list to a multi-list system requires migrating all existing items to a "Default" list on both client and server, updating every API sync payload to be list-scoped, and resolving complex access control conflicts (e.g., an item modified in a list the user was just removed from).

2. **Robust Unit Support & Custom Units**
   * **Score**: `9.0/10`
   * **Required Schema Changes**:
     * Alter the `quantity` column in `grocery_items` from an integer (`Int`) to a decimal (`Double`/`Float`) to allow partial measurements (e.g., `1.5` lbs).
     * Add a `unit` column (e.g., "lbs", "oz", "bunch", "can") or link to a new `units` table (`id`, `name`, `is_custom`).
   * **Why it's harder after Sync**: Changing primary data types (like integer to float) across distributed clients requires absolute synchronization of data-type migration scripts. If older app clients send integer quantities to a backend that expects rich units (or vice versa), it will break JSON serialization or database integrity constraints.

3. **Detailed Price History & Store-Specific Pricing**
   * **Score**: `8.5/10`
   * **Required Schema Changes**:
     * Create a `price_history` table (`id` [UUID], `item_id` / `item_name`, `store_id` [FK], `price` [Double], `recorded_at` [Timestamp], `sync_state`, `version`, `is_deleted`).
   * **Why it's harder after Sync**: It introduces a brand new, heavy-write table to the synchronization engine. The sync manager will need to track batch deltas, handle conflicts for price records uploaded out of order, and manage cascading deletes of stores/items linked to these historical price logs.

4. **Subtasks, Notes, and Item Descriptions**
   * **Score**: `8.2/10`
   * **Required Schema Changes**:
     * Add a `notes` column (`TEXT`) to the `grocery_items` table, or create an `item_notes` table to support multiple formatted notes.
   * **Why it's harder after Sync**: Text fields are prone to concurrent editing conflicts (e.g., one user writes "Buy organic" while another writes "Buy 1 gallon" offline). Resolving text conflicts requires sophisticated string merging or last-write-wins rules that are difficult to retrofit into an active sync payload.

5. **Aisle / Custom Store Layout Mapping**
   * **Score**: `8.0/10`
   * **Required Schema Changes**:
     * Create a `store_category_layouts` table (`id`, `store_id` [FK], `category_id` [FK], `aisle_number` [String], `sort_order` [Int], `sync_state`, `version`, `is_deleted`) to map category sequences specifically per store.
   * **Why it's harder after Sync**: Managing many-to-many relationships (Stores <-> Categories) with sequence indexes over sync is notoriously difficult. If a category is renamed, merged, or deleted on one client, syncing the re-ordered indices across other clients without introducing circular dependencies or index gaps is highly complex.

6. **Item Image Attachments & Barcode Storage**
   * **Score**: `7.8/10`
   * **Required Schema Changes**:
     * Add `barcode` (`String`) and `image_url` (`String`) columns to `grocery_items`, or create an `item_media` table.
   * **Why it's harder after Sync**: Barcodes are relatively simple, but image syncing requires uploading binary files to cloud storage (S3/GCS), keeping local paths in sync with remote CDN URLs, and handling local-offline photo caching states.

7. **Recurring / Automated Grocery Schedules**
   * **Score**: `7.5/10`
   * **Required Schema Changes**:
     * Create a `recurring_schedules` table (`id` [UUID], `item_name`, `recurrence_interval_days` [Int], `last_triggered_at`, `is_active` [Boolean], `sync_state`, `version`, `is_deleted`).
   * **Why it's harder after Sync**: Triggering automated additions across multiple devices is a classic distributed systems problem. If a client goes offline, triggers a recurring item add, and then syncs, the backend must detect and deduplicate recurring items added by other active devices to prevent duplicate "Need" items.

8. **Pantry Inventory & Expiration Date Tracker**
   * **Score**: `7.2/10`
   * **Required Schema Changes**:
     * Create a `pantry_items` table or add columns to `grocery_items`: `in_pantry` [Boolean], `expiration_date` [Date], `pantry_quantity` [Float].
   * **Why it's harder after Sync**: Pantry status involves rapid state changes (e.g., moving from "Needs" to "Shopping" to "Pantry" to "Consumed"). Ensuring these state-machine transitions resolve in the correct sequence across devices requires complex, multi-state conflict resolution rules.

9. **Nutritional / Dietary Tagging**
   * **Score**: `6.8/10`
   * **Required Schema Changes**:
     * Create a `dietary_tags` table (`id`, `tag_name`, e.g., "Keto", "Vegan", "Gluten-Free").
     * Create a join table `item_dietary_tags` (`item_id` [FK], `tag_id` [FK]).
   * **Why it's harder after Sync**: Any many-to-many join table doubles the synchronization surface area. Deleting a tag requires a cascading update across sync entities to prevent dangling foreign key references on either the server or other clients.

10. **Store-Specific Loyalty Cards & Digital Coupons**
    * **Score**: `6.5/10`
    * **Required Schema Changes**:
      * Add `loyalty_card_number` (`String`) and `loyalty_card_barcode` (`String`) to the `stores` table.
      * Create a `store_coupons` table (`id` [UUID], `store_id` [FK], `coupon_code`, `discount_details`, `expiry_date`, `is_redeemed`, `sync_state`, `version`, `is_deleted`).
    * **Why it's harder after Sync**: Redemptions must be real-time or highly synchronized. If a coupon is marked as redeemed offline on one device, the schema must prevent double-redemption on another device during sync reconciliation.

11. **User Profile Settings & Application Preferences**
    * **Score**: `6.2/10`
    * **Required Schema Changes**:
      * Create a `user_preferences` table (`user_id` [PK], `preference_key` [String], `preference_value` [String], `updated_at`).
    * **Why it's harder after Sync**: Device-specific settings (like screen brightness or local cache size) must be distinguished from account-wide preferences (like default currency or primary store) within the database schema to avoid conflicting preferences across devices.

12. **Store Geofencing & Opening Hours Metadata**
    * **Score**: `5.8/10`
    * **Required Schema Changes**:
      * Add `latitude` (`Double`), `longitude` (`Double`), and a structured/JSON `opening_hours` column to the `stores` table.
    * **Why it's harder after Sync**: Location coordinates require strict accuracy validation on the server. If coordinates are modified offline, the schema needs rules to prevent sync loops or overwrite collisions of critical store physical addresses.

13. **Trip Budgeting, Estimates & Receipts**
    * **Score**: `5.5/10`
    * **Required Schema Changes**:
      * Create a `shopping_trips` table (`id` [UUID], `store_id` [FK], `trip_date` [Date], `budget_limit` [Double], `actual_total` [Double], `completed_at` [Timestamp]).
    * **Why it's harder after Sync**: Summing total purchases and reconciling them against historical records is highly sensitive to schema structural integrity. An item price update on one device must not retroactively alter the frozen total of a completed, synchronized trip.

14. **Custom Sort Ordering per Category / Store**
    * **Score**: `5.2/10`
    * **Required Schema Changes**:
      * Create a `custom_sort_indices` table (`id`, `entity_type` [e.g., "STORE", "CATEGORY"], `entity_id` [UUID], `sort_index` [Float], `sync_state`, `version`).
    * **Why it's harder after Sync**: Drag-and-drop sort order relies on precise floating-point or fractional index allocations. Syncing these indices across concurrent offline edits is incredibly difficult and frequently results in duplicate sorting indexes or interleaving issues.

15. **Item Substitution Preferences**
    * **Score**: `4.5/10`
    * **Required Schema Changes**:
      * Create an `item_substitutions` table (`id` [UUID], `primary_item_id` [FK], `substitute_item_id` [FK], `preference_rank` [Int]).
    * **Why it's harder after Sync**: Self-referential and circular relationships (e.g., Item A substitutes Item B, which substitutes Item A) are difficult to validate. Batch synchronization runs the risk of committing invalid circular links if child entities are processed before parent entities.

## Bugs
- [x] **Entering Shopping Mode Crash**: Entering Shopping Mode without any items crashes the app.
- [x] **Manage Stores Submit on Enter**: Tapping enter on the keyboard should be the same as tapping plus after adding a store.
- [x] **Capitalized Phases**: Please don't yell at me with all caps in the title of each screen.
- [x] **Cursor Position After Add**: Cursor now correctly returns to the item name field after adding an item.
- [x] **No Checkboxes in Needs or Planning Mode**: Checkboxes are now only visible in Shopping Mode.
- [x] **Planning page Pill Wrap**: On planning screen, the pills now wrap to a second line correctly.
- [x] **First Letter Cap**: All text boxes in this section of the app now default to sentence capitalization.
- [x] **In Cart at bottom**: The In Cart Category should always be at the bottom of the list of categories, on the shopping screen, not at the top, or alphabetically sorted.
- [x] **Checkbox delay**: The delay on ticking an item should only be a delay on moving it to the In Cart section, not a delay on marking the item as checked. That change should happen immediately.
- [x] **Trip Complete Button Changes Nothing**: Fixed by migrating presentation state to a background-thread Combined StateFlow in a custom ViewModel, ensuring direct reactivity to database transactions.
