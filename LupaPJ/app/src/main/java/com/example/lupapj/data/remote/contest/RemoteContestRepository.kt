package com.example.lupapj.data.remote.contest

import com.example.lupapj.data.model.GalleryImage
import com.example.lupapj.data.repository.ContestEntryInfo
import com.example.lupapj.data.repository.ContestGroupDetail
import com.example.lupapj.data.repository.ContestGroupSummary
import com.example.lupapj.data.repository.ContestRepository
import com.example.lupapj.data.repository.ContestUploadResult
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.HttpException

class RemoteContestRepository(
    private val service: ContestRetrofitService,
    private val s3Client: OkHttpClient = OkHttpClient()
) : ContestRepository {
    override suspend fun uploadEntryImage(image: GalleryImage): ContestUploadResult {
        return try {
            val joinResponse = service.join()
            val joinData = joinResponse.data
                ?: return ContestUploadResult.Failure("콘테스트 참가 응답이 비어 있습니다.")

            if (!joinData.imageUploadAvailable) {
                return ContestUploadResult.MatchedWithoutImage(
                    entryId = joinData.entryId,
                    groupId = joinData.groupId,
                    message = joinData.uploadErrorMessage
                        ?: "조 매칭은 완료됐지만 이미지 업로드 주소를 받지 못했습니다."
                )
            }

            val uploadUrl = joinData.uploadUrl
                ?: return ContestUploadResult.MatchedWithoutImage(
                    entryId = joinData.entryId,
                    groupId = joinData.groupId,
                    message = "조 매칭은 완료됐지만 S3 업로드 주소를 받지 못했습니다."
                )
            val fileKey = joinData.fileKey
                ?: return ContestUploadResult.MatchedWithoutImage(
                    entryId = joinData.entryId,
                    groupId = joinData.groupId,
                    message = "조 매칭은 완료됐지만 S3 파일 키를 받지 못했습니다."
                )

            try {
                uploadFileToS3(
                    uploadUrl = uploadUrl,
                    file = File(image.filePath)
                )

                val confirmResponse = service.confirm(
                    ContestConfirmRequestDto(
                        entryId = joinData.entryId,
                        fileKey = fileKey
                    )
                )

                if (confirmResponse.success) {
                    ContestUploadResult.Success(
                        entryId = joinData.entryId,
                        groupId = joinData.groupId,
                        fileKey = fileKey
                    )
                } else {
                    ContestUploadResult.MatchedWithoutImage(
                        entryId = joinData.entryId,
                        groupId = joinData.groupId,
                        message = "조 매칭은 완료됐지만 콘테스트 참가 확인에 실패했습니다."
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ContestUploadResult.MatchedWithoutImage(
                    entryId = joinData.entryId,
                    groupId = joinData.groupId,
                    message = "조 매칭은 완료됐지만 이미지 업로드에 실패했습니다. (${exception.toContestUploadMessage()})"
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ContestUploadResult.Failure(exception.toContestUploadMessage())
        }
    }

    override suspend fun getGroups(): Result<List<ContestGroupSummary>> {
        return runCatching {
            service.getGroups().data.mapNotNull { dto ->
                val groupId = dto.groupId ?: return@mapNotNull null
                ContestGroupSummary(
                    groupId = groupId,
                    groupNumber = dto.groupNumber,
                    status = dto.status,
                    memberCount = dto.memberCount,
                    isMyGroup = dto.myGroup
                )
            }
        }
    }

    override suspend fun getGroupDetail(groupId: String): Result<ContestGroupDetail> {
        return runCatching {
            service.getGroupDetail(groupId).data?.toDomain()
                ?: error("콘테스트 조 정보를 불러오지 못했습니다.")
        }
    }

    private suspend fun uploadFileToS3(
        uploadUrl: String,
        file: File
    ) = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalArgumentException("선택한 이미지 파일을 찾을 수 없습니다.")
        }

        val request = Request.Builder()
            .url(uploadUrl)
            .put(file.asRequestBody("image/png".toMediaType()))
            .header("Content-Type", "image/png")
            .build()

        s3Client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("S3 업로드 실패: HTTP ${response.code}")
            }
        }
    }

    private fun ContestGroupDetailDto.toDomain(): ContestGroupDetail {
        val resolvedGroupId = groupId ?: error("콘테스트 조 ID가 비어 있습니다.")
        return ContestGroupDetail(
            groupId = resolvedGroupId,
            groupNumber = groupNumber,
            status = status,
            closeAt = closeAt,
            entries = entries.map { entry ->
                ContestEntryInfo(
                    entryId = entry.entryId,
                    userUid = entry.userUid,
                    imageUrl = entry.imageUrl,
                    voteCount = entry.voteCount,
                    rank = entry.rank,
                    confirmed = entry.confirmed
                )
            },
            myEntryId = myEntryId
        )
    }

    private fun Exception.toContestUploadMessage(): String {
        if (this is HttpException) {
            val errorBody = response()?.errorBody()?.string()
            val apiMessage = errorBody?.toApiErrorMessage()
            if (!apiMessage.isNullOrBlank()) return apiMessage

            return "서버 요청 실패: HTTP ${code()}"
        }

        return message ?: "콘테스트 이미지 업로드에 실패했습니다."
    }

    private fun String.toApiErrorMessage(): String? {
        return runCatching {
            val json = JSONObject(this)
            val code = json.optString("code").takeIf { it.isNotBlank() }
            val message = json.optString("message").takeIf { it.isNotBlank() }

            when {
                code != null && message != null -> "$code: $message"
                message != null -> message
                code != null -> code
                else -> null
            }
        }.getOrNull()
    }
}
