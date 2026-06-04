package com.example.lupapj.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lupapj.data.local.GalleryLocalCache
import com.example.lupapj.data.remote.AuthInterceptor
import com.example.lupapj.data.remote.ServerConfig
import com.example.lupapj.data.remote.gallery.GalleryRetrofitService
import com.example.lupapj.data.repository.RemoteGalleryRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File

class GallerySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("GallerySyncWorker", "Starting sync...")
        val localCache = GalleryLocalCache(applicationContext)

        // 1. Retrofit 및 Repository 초기화
        val authInterceptor = AuthInterceptor {
            com.example.lupapj.data.local.TokenManager.accessToken
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl(ServerConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        val apiService = retrofit.create(GalleryRetrofitService::class.java)
        val remoteRepo = RemoteGalleryRepository(apiService)

        // 2. 백업되지 않은 로컬 항목 조회
        val unbackedUpItems = localCache.getUnbackedUpItems()
        Log.d("GallerySyncWorker", "Unbacked up items: ${unbackedUpItems.size}")
        
        var allSuccess = true

        for (item in unbackedUpItems) {
            val file = File(item.localUri)
            if (!file.exists()) {
                Log.w("GallerySyncWorker", "File not found: ${item.localUri}")
                // 로컬 파일이 없으면 백업 시도 안함 (필요 시 DB 정리)
                localCache.deleteGalleryItem(item.id)
                continue
            }

            try {
                // S3 업로드 및 메타데이터 저장
                val result = remoteRepo.uploadScreenshot(file)
                if (result.isSuccess) {
                    val serverId = result.getOrThrow()
                    // 로컬 DB에 서버 ID 및 백업 완료 플래그 업데이트
                    localCache.markAsBackedUp(localId = item.id, serverImageId = serverId)
                    Log.d("GallerySyncWorker", "Successfully backed up: ${item.id} as $serverId")
                } else {
                    Log.e("GallerySyncWorker", "Failed to backup: ${item.id}", result.exceptionOrNull())
                    allSuccess = false
                }
            } catch (e: Exception) {
                Log.e("GallerySyncWorker", "Exception during backup: ${item.id}", e)
                allSuccess = false
            }
        }

        // 3. 즐겨찾기 오프라인 변경분 동기화
        val dirtyFavorites = localCache.getDirtyFavoriteItems()
        Log.d("GallerySyncWorker", "Dirty favorite items: ${dirtyFavorites.size}")
        for (item in dirtyFavorites) {
            item.serverImageId?.let { serverId ->
                try {
                    val result = remoteRepo.toggleFavorite(serverId, item.isFavorite)
                    if (result.isSuccess) {
                        localCache.clearFavoriteDirtyFlag(item.id)
                        Log.d("GallerySyncWorker", "Successfully synced favorite for: ${item.id}")
                    } else {
                        Log.e("GallerySyncWorker", "Failed to sync favorite for: ${item.id}", result.exceptionOrNull())
                        allSuccess = false
                    }
                } catch (e: Exception) {
                    Log.e("GallerySyncWorker", "Exception during favorite sync: ${item.id}", e)
                    allSuccess = false
                }
            }
        }

        return if (allSuccess) {
            Log.d("GallerySyncWorker", "Sync finished successfully.")
            Result.success()
        } else {
            Log.d("GallerySyncWorker", "Sync finished with some failures, will retry.")
            Result.retry()
        }
    }
}
