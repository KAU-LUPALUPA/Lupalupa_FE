package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [수정됨(권)] 재화 획득 요청을 위한 DTO
 * 서버(CurrencyController.java)의 GoldRequest 스펙에 맞춰 수정됨.
 */
data class CurrencyEarnRequest(
    @SerializedName("amount")
    val amount: Int, // [수정됨(권)] Long -> Int
    @SerializedName("total")
    val total: Long
)
