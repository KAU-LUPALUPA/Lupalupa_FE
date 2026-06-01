package com.example.lupapj.data.model

import com.google.gson.annotations.SerializedName

// 로컬 DB (DataStore) 용 모델
data class GalleryEntity(
    val id: String,               // 로컬 고유 ID (UUID)
    val localUri: String,         // 스크린샷 로컬 파일 경로
    val createdAt: String,        // 촬영 시각 (ISO-8601 등)
    val isBackedUp: Boolean = false, // 서버 백업 완료 여부
    val serverImageId: String? = null, // 서버에 백업된 후 발급받은 ID
    val isFavorite: Boolean = false // 즐겨찾기 여부
)

// 백엔드 API (GalleryController) 연동용 Request/Response DTO
data class GalleryItemsResponse(
    val success: Boolean,
    val items: List<GalleryItemDto>
)

data class GalleryItemDto(
    val imageId: String,
    val uid: String,
    val url: String,
    val createdAt: String,
    val isFavorite: Boolean
)

data class UploadUrlRequest(
    val fileName: String,
    val contentType: String
)

data class UploadUrlResponse(
    val success: Boolean,
    val uploadUrl: String,
    val imageUrl: String
)

data class SaveMetadataRequest(
    val imageUrl: String
)

data class SaveMetadataResponse(
    val success: Boolean,
    val imageId: String,
    val message: String
)

data class DeleteResponse(
    val success: Boolean,
    val message: String
)

data class FavoriteRequest(
    val isFavorite: Boolean
)

data class FavoriteResponse(
    val success: Boolean,
    val message: String
)
