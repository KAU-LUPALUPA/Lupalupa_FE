package com.example.lupapj.data.repository

import com.example.lupapj.data.model.*
import com.example.lupapj.data.remote.gallery.GalleryRetrofitService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class RemoteGalleryRepository(
    private val apiService: GalleryRetrofitService
) {
    suspend fun getItems(): Result<GalleryItemsResponse> = runCatching {
        val response = apiService.getItems()
        if (response.isSuccessful) {
            response.body() ?: throw Exception("Empty body")
        } else {
            throw Exception("Failed to get items: ${response.code()}")
        }
    }

    suspend fun uploadScreenshot(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Get Presigned URL
            val uploadReq = UploadUrlRequest(
                fileName = file.name,
                fileType = "image/png"
            )
            val urlResponse = apiService.getUploadUrl(uploadReq)
            if (!urlResponse.isSuccessful || urlResponse.body()?.data == null) {
                throw Exception("Failed to get upload URL: ${urlResponse.code()}")
            }
            val presignedUrl = urlResponse.body()!!.data!!.uploadUrl
            val fileKey = urlResponse.body()!!.data!!.fileKey

            // 2. Upload to S3
            val requestBody = file.asRequestBody("image/png".toMediaTypeOrNull())
            val s3Response = apiService.uploadImageToS3(presignedUrl, requestBody)
            if (!s3Response.isSuccessful) {
                throw Exception("Failed to upload to S3: ${s3Response.code()}")
            }

            // 3. Save Metadata
            val metaReq = SaveMetadataRequest(
                fileKey = fileKey,
                size = file.length(),
                isFavorite = false
            )
            val metaResponse = apiService.saveMetadata(metaReq)
            if (!metaResponse.isSuccessful || metaResponse.body()?.data == null) {
                throw Exception("Failed to save metadata: ${metaResponse.code()}")
            }
            
            metaResponse.body()!!.data!!.imageId
        }
    }

    suspend fun deleteItems(imageIds: List<String>): Result<Boolean> = runCatching {
        val response = apiService.deleteItems(imageIds)
        response.isSuccessful && response.body()?.success == true
    }

    suspend fun toggleFavorite(imageId: String, isFavorite: Boolean): Result<Boolean> = runCatching {
        val response = apiService.toggleFavorite(imageId, FavoriteRequest(isFavorite))
        response.isSuccessful && response.body()?.success == true
    }
}
