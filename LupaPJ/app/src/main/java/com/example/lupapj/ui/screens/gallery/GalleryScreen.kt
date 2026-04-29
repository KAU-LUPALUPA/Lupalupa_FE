package com.example.lupapj.ui.screens.gallery

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.lupapj.data.model.GalleryImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    onBackClick: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onDeleteClick: (String) -> Unit // [추가됨 - 권순범] 삭제 콜백
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 갤러리") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("◀")
                    }
                }
            )
        }
    ) { paddingValues ->
        // [추가됨 - 권순범] 삭제 확인 모달을 위한 상태 변수
        var imageToDelete by remember { mutableStateOf<String?>(null) }

        if (imageToDelete != null) {
            AlertDialog(
                onDismissRequest = { imageToDelete = null },
                title = { Text("사진 삭제") },
                text = { Text("정말 이 사진을 삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteClick(imageToDelete!!)
                        imageToDelete = null
                    }) {
                        Text("삭제", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { imageToDelete = null }) {
                        Text("취소")
                    }
                }
            )
        }

        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("아직 찍은 사진이 없습니다.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black) // 갤러리 배경은 어둡게 연출
            ) {
                items(images, key = { it.id }) { image ->
                    GalleryItem(
                        image = image,
                        onFavoriteToggle = { onFavoriteToggle(image.id) },
                        onDeleteClick = { imageToDelete = image.id }, // [수정됨 - 권순범] 바로 삭제하지 않고 모달 상태 변경
                        onShareClick = {
                            val file = File(image.filePath)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "image/png"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "사진 공유하기"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryItem(
    image: GalleryImage,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit, // [추가됨 - 권순범]
    onShareClick: () -> Unit
) {
    // 백그라운드 스레드에서 파일 디코딩
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.56f) // 세로형 사진 비율 (대략 16:9)
            .background(Color.DarkGray)
    ) {
        imageBitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = "스크린샷",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 상단 오버레이 바 (공유, 즐겨찾기, 삭제)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .align(Alignment.TopCenter)
                .padding(4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onShareClick) {
                Text("공유", color = Color.White)
            }
            IconButton(onClick = onDeleteClick) { // [추가됨 - 권순범] 삭제 버튼
                Text("🗑️", color = Color.White)
            }
            IconButton(onClick = onFavoriteToggle) {
                Text(
                    text = if (image.isFavorite) "❤️" else "🤍",
                    color = Color.White
                )
            }
        }
    }
}
