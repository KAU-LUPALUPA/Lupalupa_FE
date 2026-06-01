package com.example.lupapj.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lupapj.data.local.GalleryLocalCache
import com.example.lupapj.data.model.GalleryEntity
import com.example.lupapj.data.model.GalleryImage
import com.example.lupapj.worker.GallerySyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

class GalleryRepository(
    private val context: Context,
    private val remoteGalleryRepository: RemoteGalleryRepository? = null
) {
    private val localCache = GalleryLocalCache(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

    // 로컬 무제한, DataStore Flow 매핑
    val images: StateFlow<List<GalleryImage>> = localCache.getGalleryItems()
        .map { entities ->
            entities.map { entity ->
                GalleryImage(
                    id = entity.id,
                    filePath = entity.localUri,
                    isFavorite = entity.isFavorite,
                    timestamp = Instant.parse(entity.createdAt).toEpochMilli(),
                    isBackedUp = entity.isBackedUp,
                    serverImageId = entity.serverImageId
                )
            }.sortedByDescending { it.timestamp }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val galleryDir = File(context.filesDir, "gallery").apply {
        if (!exists()) mkdirs()
    }

    suspend fun saveImage(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val file = File(galleryDir, "$id.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val entity = GalleryEntity(
            id = id,
            localUri = file.absolutePath,
            createdAt = Instant.now().toString(),
            isBackedUp = false,
            isFavorite = false
        )
        localCache.saveGalleryItem(entity)

        // 즉시 동기화 백그라운드 워커 트리거
        triggerSyncWorker()
    }

    suspend fun toggleFavorite(imageId: String) = withContext(Dispatchers.IO) {
        val target = images.value.find { it.id == imageId }
        if (target != null) {
            val newFavoriteStatus = !target.isFavorite
            val updated = GalleryEntity(
                id = target.id,
                localUri = target.filePath,
                createdAt = Instant.ofEpochMilli(target.timestamp).toString(),
                isBackedUp = target.isBackedUp,
                isFavorite = newFavoriteStatus,
                serverImageId = target.serverImageId
            )
            localCache.updateGalleryItem(updated)
            
            // 백엔드 동기화 (Fire-and-forget)
            target.serverImageId?.let { serverId ->
                scope.launch {
                    try {
                        remoteGalleryRepository?.toggleFavorite(serverId, newFavoriteStatus)
                    } catch (e: Exception) {
                        Log.e("GalleryRepo", "Failed to sync favorite", e)
                    }
                }
            }
        }
    }

    suspend fun deleteImage(imageId: String) = withContext(Dispatchers.IO) {
        val target = images.value.find { it.id == imageId }
        val serverId = target?.serverImageId
        
        val file = File(galleryDir, "$imageId.png")
        if (file.exists()) file.delete()
        
        localCache.deleteGalleryItem(imageId)
        
        // 백엔드 동기화 (Fire-and-forget)
        serverId?.let { sId ->
            scope.launch {
                try {
                    remoteGalleryRepository?.deleteItems(listOf(sId))
                } catch (e: Exception) {
                    Log.e("GalleryRepo", "Failed to sync delete", e)
                }
            }
        }
    }

    private fun triggerSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val request = OneTimeWorkRequestBuilder<GallerySyncWorker>()
            .setConstraints(constraints)
            .build()
            
        workManager.enqueue(request)
    }
}
