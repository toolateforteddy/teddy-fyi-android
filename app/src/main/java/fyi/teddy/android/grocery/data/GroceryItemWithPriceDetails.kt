package fyi.teddy.android.grocery.data

data class GroceryItemWithPriceDetails(
    val item: GroceryItem,
    val cheaperStoreName: String?,
    val cheaperStorePrice: Double?,
    val isMoreExpensiveAtCurrentStore: Boolean
) {
    companion object {
        fun from(
            item: GroceryItem,
            shoppingStoreId: Int?,
            itemStoreInfos: List<GroceryItemStoreInfo>,
            stores: List<Store>
        ): GroceryItemWithPriceDetails {
            val minPriceInfo = itemStoreInfos.filter { it.price != null }.minByOrNull { it.price!! }
            val shoppingStoreInfo = itemStoreInfos.find { it.storeId == shoppingStoreId }
            
            val isMoreExpensive = shoppingStoreId != null && 
                                  shoppingStoreInfo?.price != null && 
                                  minPriceInfo?.price != null && 
                                  shoppingStoreInfo.price > minPriceInfo.price

            val cheaperStoreName = if (isMoreExpensive) {
                stores.find { it.id == minPriceInfo.storeId }?.name ?: "another store"
            } else {
                null
            }

            return GroceryItemWithPriceDetails(
                item = item,
                cheaperStoreName = cheaperStoreName,
                cheaperStorePrice = minPriceInfo?.price,
                isMoreExpensiveAtCurrentStore = isMoreExpensive
            )
        }
    }
}
