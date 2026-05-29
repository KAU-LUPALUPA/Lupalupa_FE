package com.example.lupapj.data.remote.contest

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface ContestRetrofitService {
    @POST("contest/join")
    suspend fun join(): ContestJoinResponseDto

    @POST("contest/confirm")
    suspend fun confirm(
        @Body request: ContestConfirmRequestDto
    ): ContestConfirmResponseDto

    @GET("contest/groups")
    suspend fun getGroups(): ContestGroupListResponseDto

    @GET("contest/groups/{groupId}")
    suspend fun getGroupDetail(
        @Path("groupId") groupId: String
    ): ContestGroupDetailResponseDto
}

data class ContestJoinResponseDto(
    val success: Boolean = false,
    val data: ContestJoinDataDto? = null
)

data class ContestJoinDataDto(
    val entryId: Long = 0L,
    val groupId: String? = null,
    val uploadUrl: String? = null,
    val fileKey: String? = null,
    val imageUploadAvailable: Boolean = false,
    val uploadErrorMessage: String? = null
)

data class ContestConfirmRequestDto(
    val entryId: Long,
    val fileKey: String
)

data class ContestConfirmResponseDto(
    val success: Boolean = false
)

data class ContestGroupListResponseDto(
    val success: Boolean = false,
    val data: List<ContestGroupSummaryDto> = emptyList()
)

data class ContestGroupSummaryDto(
    val groupId: String? = null,
    val groupNumber: Long = 0L,
    val status: String = "",
    val memberCount: Long = 0L,
    val myGroup: Boolean = false
)

data class ContestGroupDetailResponseDto(
    val success: Boolean = false,
    val data: ContestGroupDetailDto? = null
)

data class ContestGroupDetailDto(
    val groupId: String? = null,
    val groupNumber: Long = 0L,
    val status: String = "",
    val closeAt: String? = null,
    val entries: List<ContestEntryInfoDto> = emptyList(),
    val myEntryId: Long? = null
)

data class ContestEntryInfoDto(
    val entryId: Long = 0L,
    val userUid: String? = null,
    val imageUrl: String? = null,
    val voteCount: Int = 0,
    val rank: Int? = null,
    val confirmed: Boolean = false
)
