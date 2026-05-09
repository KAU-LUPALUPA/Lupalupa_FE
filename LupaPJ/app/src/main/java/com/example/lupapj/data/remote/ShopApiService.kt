package com.example.lupapj.data.remote

import com.example.lupapj.data.remote.model.ShopItemsValidateRequest
import com.example.lupapj.data.remote.model.ShopItemsValidateResponse
import com.example.lupapj.data.remote.model.ShopPurchaseRequest
import com.example.lupapj.data.remote.model.ShopPurchaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * [추가됨(권)] 상점 관련 서버 API 정의
 */
interface ShopApiService {
    
    @POST("/shop/items")
    suspend fun validateShopItems(
        @Body request: ShopItemsValidateRequest
    ): Response<ShopItemsValidateResponse>

    @POST("/shop/purchase")
    suspend fun purchaseItem(
        @Body request: ShopPurchaseRequest
    ): Response<ShopPurchaseResponse>
}
