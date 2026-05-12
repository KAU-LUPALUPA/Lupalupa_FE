package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [추가됨(권)] 상점 목록 검증 응답 DTO
 */
data class ShopItemsValidateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("mismatchedItems") val mismatchedItems: List<ShopItemFailDto>? = null // [수정됨(권)] 서버 필드명 일치
)

data class ShopItemFailDto(
    @SerializedName("itemId") val itemId: Int, // [수정됨(권)] 서버 ItemResponse 필드명 일치
    @SerializedName("itemName") val itemName: String,
    @SerializedName("serverPrice") val serverPrice: Int
)

/**
 * [추가됨(권)] 아이템 구매 응답 DTO
 */
data class ShopPurchaseResponse(
    @SerializedName("status") val status: String,
    @SerializedName("purchasedItemId") val purchasedItemId: Int? = null, // [수정됨(권)] String -> Int
    @SerializedName("balance") val balance: Long? = null,
    @SerializedName("inventory") val inventory: List<InventoryItemDto>? = null
)

data class InventoryItemDto(
    @SerializedName("instanceId") val instanceId: String,
    @SerializedName("id") val id: Int, // [수정됨(권)] 서버 id(int)와 일치
    @SerializedName("itemName") val itemName: String, // [수정됨(권)] 서버 필드 추가
    @SerializedName("count") val count: Int
)
