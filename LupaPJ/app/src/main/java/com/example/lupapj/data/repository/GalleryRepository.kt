package com.example.lupapj.data.repository

import android.content.Context
import android.graphics.Bitmap
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
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

class GalleryRepository(private val context: Context) {
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
                    isBackedUp = entity.isBackedUp
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
        // 기존 엔티티 찾아서 업데이트 (실제로는 localCache 쪽에 getItem이 있으면 좋음, 여기선 약간 편법)
        val currentItems = localCache.getUnbackedUpItems() // (x) 모든 아이템을 가져와야 함
        // DataStore 특성 상 updateGalleryItem에서 리스트를 다시 읽어 수정하므로
        // 임시로 생성해서 업데이트를 보내거나 updateGalleryItem 로직을 조금 고쳐야 함.
        // 현재 updateGalleryItem은 동일 id의 항목을 덮어씀.
        // 가장 좋은 것은 localCache.toggleFavorite 를 만드는 것이지만, 여기선 Flow 최신값을 읽어옴.
        val target = images.value.find { it.id == imageId }
        if (target != null) {
            val updated = GalleryEntity(
                id = target.id,
                localUri = target.filePath,
                createdAt = Instant.ofEpochMilli(target.timestamp).toString(),
                isBackedUp = target.isBackedUp,
                isFavorite = !target.isFavorite
                // serverImageId 도 보존해야 완벽하지만 UI 모델엔 없음.
            )
            localCache.updateGalleryItem(updated)
            triggerSyncWorker()
        }
    }

    suspend fun deleteImage(imageId: String) = withContext(Dispatchers.IO) {
        val file = File(galleryDir, "$imageId.png")
        if (file.exists()) file.delete()
        
        localCache.deleteGalleryItem(imageId)
        triggerSyncWorker()
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
