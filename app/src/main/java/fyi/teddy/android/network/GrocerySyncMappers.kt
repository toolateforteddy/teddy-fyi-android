@file:Suppress("TooManyFunctions")
package fyi.teddy.android.network

import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.Store

fun GroceryItem.toDto(): GroceryItemDto {
    return GroceryItemDto(
        id = id,
        name = name,
        quantity = quantity,
        isBought = isBought,
        createdAt = createdAt,
        position = position,
        categoryId = categoryId,
        timesBought = timesBought,
        userId = userId,
        isActive = isActive,
        listId = listId,
        unit = unit,
        notes = notes,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryItemDto.toEntity(): GroceryItem {
    return GroceryItem(
        id = id,
        name = name,
        quantity = quantity,
        isBought = isBought,
        createdAt = createdAt,
        position = position,
        categoryId = categoryId,
        timesBought = timesBought,
        userId = userId,
        isActive = isActive,
        listId = listId,
        unit = unit,
        notes = notes,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryList.toDto(): GroceryListDto {
    return GroceryListDto(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        position = position,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryListDto.toEntity(fallbackPosition: Int = 0): GroceryList {
    return GroceryList(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        position = position ?: fallbackPosition,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun Store.toDto(): StoreDto {
    return StoreDto(
        id = id,
        name = name,
        position = position,
        isDefaultSupported = isDefaultSupported,
        userId = userId,
        listId = listId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun StoreDto.toEntity(): Store {
    return Store(
        id = id,
        name = name,
        position = position,
        isDefaultSupported = isDefaultSupported,
        userId = userId,
        listId = listId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun Category.toDto(): CategoryDto {
    return CategoryDto(
        id = id,
        name = name,
        position = position,
        userId = userId,
        listId = listId,
        icon = icon,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun CategoryDto.toEntity(): Category {
    return Category(
        id = id,
        name = name,
        position = position,
        userId = userId,
        listId = listId,
        icon = icon,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryListMember.toDto(): GroceryListMemberDto {
    return GroceryListMemberDto(
        id = id,
        listId = listId,
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryListMemberDto.toEntity(): GroceryListMember {
    return GroceryListMember(
        id = id,
        listId = listId,
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryItemStoreInfo.toDto(): GroceryItemStoreInfoDto {
    return GroceryItemStoreInfoDto(
        id = id,
        groceryItemId = groceryItemId,
        storeId = storeId,
        price = price,
        isAvailable = isAvailable,
        userId = userId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryItemStoreInfoDto.toEntity(): GroceryItemStoreInfo {
    return GroceryItemStoreInfo(
        id = id,
        groceryItemId = groceryItemId,
        storeId = storeId,
        price = price,
        isAvailable = isAvailable,
        userId = userId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}
