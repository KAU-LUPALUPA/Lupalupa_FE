package com.example.lupapj.data.repository

import com.example.lupapj.data.model.GalleryImage

interface ContestRepository {
    suspend fun uploadEntryImage(image: GalleryImage): ContestUploadResult
    suspend fun getGroups(): Result<List<ContestGroupSummary>>
    suspend fun getGroupDetail(groupId: String): Result<ContestGroupDetail>
}

sealed interface ContestUploadResult {
    data class Success(
        val entryId: Long,
        val groupId: String?,
        val fileKey: String
    ) : ContestUploadResult

    data class MatchedWithoutImage(
        val entryId: Long,
        val groupId: String?,
        val message: String
    ) : ContestUploadResult

    data class Failure(
        val message: String
    ) : ContestUploadResult
}

data class ContestGroupSummary(
    val groupId: String,
    val groupNumber: Long,
    val status: String,
    val memberCount: Long,
    val isMyGroup: Boolean
)

data class ContestGroupDetail(
    val groupId: String,
    val groupNumber: Long,
    val status: String,
    val closeAt: String?,
    val entries: List<ContestEntryInfo>,
    val myEntryId: Long?
)

data class ContestEntryInfo(
    val entryId: Long,
    val userUid: String?,
    val imageUrl: String?,
    val voteCount: Int,
    val rank: Int?,
    val confirmed: Boolean
)
