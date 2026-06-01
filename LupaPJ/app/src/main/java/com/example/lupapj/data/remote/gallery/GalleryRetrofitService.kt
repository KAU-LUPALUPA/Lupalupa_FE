package com.example.lupapj.data.remote.gallery

import com.example.lupapj.data.model.DeleteResponse
import com.example.lupapj.data.model.FavoriteRequest
import com.example.lupapj.data.model.FavoriteResponse
import com.example.lupapj.data.model.GalleryItemsResponse
import com.example.lupapj.data.model.SaveMetadataRequest
import com.example.lupapj.data.model.SaveMetadataResponse
import com.example.lupapj.data.model.UploadUrlRequest
import com.example.lupapj.data.model.UploadUrlResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface GalleryRetrofitService {
    @GET("gallery/items")
    suspend fun getItems(): Response<GalleryItemsResponse>

    @POST("gallery/items/upload-url")
    suspend fun getUploadUrl(
        @Body request: UploadUrlRequest
    ): Response<UploadUrlResponse>

    // S3에 직접 PUT 요청을 보내기 위한 인터페이스
    @PUT
    suspend fun uploadImageToS3(
        @Url url: String,
        @Body requestBody: RequestBody
    ): Response<Unit>

    @POST("gallery/items")
    suspend fun saveMetadata(
        @Body request: SaveMetadataRequest
    ): Response<SaveMetadataResponse>

    @DELETE("gallery/items")
    suspend fun deleteItems(
        @Query("imageIds") imageIds: List<String>
    ): Response<DeleteResponse>

    @PATCH("gallery/items/{imageId}/favorite")
    suspend fun toggleFavorite(
        @Path("imageId") imageId: String,
        @Body request: FavoriteRequest
    ): Response<FavoriteResponse>
}
