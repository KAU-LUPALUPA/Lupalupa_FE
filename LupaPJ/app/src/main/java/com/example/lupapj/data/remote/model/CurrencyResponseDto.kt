package com.example.lupapj.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [수정됨(권)] 재화 관련 API 응답 DTO
 * 서버(CurrencyController.java)의 응답 필드명(status, total)에 맞춰 수정됨.
 */
data class CurrencyResponseDto(
    @SerializedName("status") val status: String?, // [수정됨(권)] success(Boolean)에서 status(String)로 변경
    @SerializedName("total") val total: Long?      // [수정됨(권)] currentBalance에서 total로 변경
)
