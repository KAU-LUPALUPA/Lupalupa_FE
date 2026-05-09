package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [추가됨(권)] 재화 관련 API 응답 DTO
 */
data class CurrencyResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("currentBalance") val currentBalance: Long?
)
