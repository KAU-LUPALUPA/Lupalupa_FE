package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [추가됨(권)] 상점 목록 검증 요청 DTO
 */
data class ShopItemsValidateRequest(
    @SerializedName("items") val items: List<ShopItemDto>
)

data class ShopItemDto(
    @SerializedName("itemId") val itemId: Int, // [수정됨(권)] String -> Int
    @SerializedName("itemName") val itemName: String,
    @SerializedName("price") val price: Int
)

/**
 * [추가됨(권)] 아이템 구매 요청 DTO
 */
data class ShopPurchaseRequest(
    @SerializedName("itemId") val itemId: Int, // [수정됨(권)] String -> Int
    @SerializedName("amount") val amount: Int,
    @SerializedName("price") val price: Int,
    @SerializedName("balance") val balance: Long
)
