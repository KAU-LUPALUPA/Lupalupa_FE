package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [추가됨(권)] 상점 목록 검증 응답 DTO
 */
data class ShopItemsValidateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("fail_list") val failList: List<ShopItemFailDto>? = null
)

data class ShopItemFailDto(
    @SerializedName("id") val id: String,
    @SerializedName("price") val price: Int
)

/**
 * [추가됨(권)] 아이템 구매 응답 DTO
 */
data class ShopPurchaseResponse(
    @SerializedName("status") val status: String,
    @SerializedName("purchasedItemId") val purchasedItemId: String? = null,
    @SerializedName("balance") val balance: Long? = null,
    @SerializedName("inventory") val inventory: List<InventoryItemDto>? = null
)

data class InventoryItemDto(
    @SerializedName("instanceId") val instanceId: String,
    @SerializedName("id") val masterId: String, // API 상에서는 "id"로 내려옴
    @SerializedName("count") val count: Int
)
