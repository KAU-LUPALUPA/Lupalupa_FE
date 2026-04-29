package com.example.lupapj.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.lupapj.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GalleryRepository(private val context: Context) {
    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images.asStateFlow()

    private val galleryDir = File(context.filesDir, "gallery").apply {
        if (!exists()) mkdirs()
    }

    // 즐겨찾기 상태를 로컬에 영구 저장하기 위한 SharedPreferences
    private val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

    init {
        loadImages()
    }

    private fun loadImages() {
        val favoriteIds = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        val files = galleryDir.listFiles()?.filter { it.extension == "png" } ?: emptyList()
        val loadedImages = files.map { file ->
            val id = file.nameWithoutExtension
            GalleryImage(
                id = id,
                filePath = file.absolutePath,
                isFavorite = favoriteIds.contains(id),
                timestamp = file.lastModified()
            )
        }.sortedByDescending { it.timestamp }
        _images.update { loadedImages }
    }

    suspend fun saveImage(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        // 1. 최대 100장 제한 처리
        val currentImages = _images.value
        if (currentImages.size >= 100) {
            val oldest = currentImages.last()
            deleteImage(oldest.id) // 삭제 로직 통합
        }

        // 2. 새로운 이미지 파일로 저장
        val id = UUID.randomUUID().toString()
        val file = File(galleryDir, "$id.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val newImage = GalleryImage(
            id = id,
            filePath = file.absolutePath,
            isFavorite = false,
            timestamp = System.currentTimeMillis()
        )

        _images.update { listOf(newImage) + it }

        // 3. 서버 동기화 (Mock)
        syncToServerMock(newImage)
    }

    fun toggleFavorite(imageId: String) {
        val currentFavorites = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentFavorites.contains(imageId)) {
            currentFavorites.remove(imageId)
        } else {
            currentFavorites.add(imageId)
        }
        prefs.edit().putStringSet("favorites", currentFavorites).apply()

        _images.update { list ->
            list.map {
                if (it.id == imageId) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    // [추가됨] 이미지 삭제 기능
    fun deleteImage(imageId: String) {
        val file = File(galleryDir, "$imageId.png")
        if (file.exists()) {
            file.delete()
        }

        // SharedPreferences에서 즐겨찾기 상태 제거
        val currentFavorites = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentFavorites.contains(imageId)) {
            currentFavorites.remove(imageId)
            prefs.edit().putStringSet("favorites", currentFavorites).apply()
        }

        // 상태 업데이트
        _images.update { list -> list.filter { it.id != imageId } }
    }

    // [Mock] 실제 서버 연동 로직
    private suspend fun syncToServerMock(image: GalleryImage) {
        // 서버 업로드를 시뮬레이션하는 딜레이
        delay(500)
    }
}
