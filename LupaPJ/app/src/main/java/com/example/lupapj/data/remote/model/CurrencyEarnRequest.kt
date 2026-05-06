package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [추가됨(권)] 재화 획득 요청을 위한 DTO
 * 서버 스펙: { "amount": Long, "total": Long }
 */
data class CurrencyEarnRequest(
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("total")
    val total: Long,
    @SerializedName("source") // [추가됨(권)] 추후 검증을 위한 획득 경로
    val source: String
)
