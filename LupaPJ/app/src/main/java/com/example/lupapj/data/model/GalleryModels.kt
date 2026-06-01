package com.example.lupapj.data.model

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
    val data: GalleryItemsData?
)

data class GalleryItemsData(
    val items: List<GalleryItemDto>
)

data class GalleryItemDto(
    val imageId: String,
    val imageUrl: String,
    val isFavorite: Boolean,
    val timestamp: Long
)

data class UploadUrlRequest(
    val fileName: String,
    val fileType: String
)

data class UploadUrlResponse(
    val success: Boolean,
    val data: UploadUrlData?
)

data class UploadUrlData(
    val uploadUrl: String,
    val fileKey: String
)

data class SaveMetadataRequest(
    val fileKey: String,
    val size: Long,
    val isFavorite: Boolean
)

data class SaveMetadataResponse(
    val success: Boolean,
    val data: SaveMetadataData?
)

data class SaveMetadataData(
    val imageId: String
)

data class DeleteResponse(
    val success: Boolean
)

data class FavoriteRequest(
    val isFavorite: Boolean
)

data class FavoriteResponse(
    val success: Boolean,
    val data: FavoriteData?
)

data class FavoriteData(
    val imageId: String,
    val isFavorite: Boolean
)
