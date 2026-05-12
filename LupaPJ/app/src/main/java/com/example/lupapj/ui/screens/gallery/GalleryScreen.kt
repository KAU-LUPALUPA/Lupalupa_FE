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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.lupapj.data.model.GalleryImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    onBackClick: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val context = LocalContext.current
    // [수정] ID로 관리하여 실시간 상태(즐겨찾기 등) 동기화
    var fullscreenImageId by remember { mutableStateOf<String?>(null) }
    val fullscreenImage = remember(fullscreenImageId, images) {
        images.find { it.id == fullscreenImageId }
    }
    // [수정] 스코프 확장을 위해 Scaffold 외부로 이동
    var imageToDelete by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    items(images, key = { it.id }) { image ->
                        GalleryItem(
                            image = image,
                            onItemClick = { fullscreenImageId = image.id },
                            onFavoriteToggle = { onFavoriteToggle(image.id) },
                            onDeleteClick = { imageToDelete = image.id },
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
        } // Scaffold 닫기

        // 3. 전체 화면 이미지 오버레이 (Scaffold 밖에 두어 상단바까지 모두 덮도록 수정)
        AnimatedVisibility(
            visible = fullscreenImageId != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            fullscreenImage?.let { image ->
                FullscreenImageOverlay(
                    image = image,
                    onDismiss = { fullscreenImageId = null },
                    onFavoriteToggle = { onFavoriteToggle(image.id) },
                    onDeleteClick = { 
                        // 전체 화면 모드에서도 삭제 확인 모달은 GalleryScreen의 상태를 이용
                        // imageToDelete를 설정하면 Scaffold 내부에 선언된 AlertDialog가 표시됨
                        // 하지만 Scaffold 외부에서 오버레이가 덮고 있으므로, 
                        // 우선 오버레이를 닫고 모달을 띄우거나 모달을 최상단으로 옮겨야 함.
                        // 여기서는 오버레이를 닫고 모달을 띄우는 방식으로 처리.
                        fullscreenImageId = null 
                        imageToDelete = image.id
                    },
                    onShareClick = {
                        val file = File(image.filePath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, uri)
                                type = "image/png"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "사진 공유하기"))
                        }
                    }
                )
            }
        }
    }

    // 통합 뒤로가기 처리
    BackHandler(enabled = true) {
        if (fullscreenImageId != null) {
            fullscreenImageId = null 
        } else {
            onBackClick()
        }
    }
}

@Composable
fun FullscreenImageOverlay(
    image: GalleryImage,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
        }
    }

    // UI 컨트롤 노출 여부 상태
    var showControls by remember { mutableStateOf(false) }

    // 상세 날짜 포맷팅
    val fullDateFormatter = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss", Locale.KOREA) }
    val fullDateString = remember(image.timestamp) { fullDateFormatter.format(Date(image.timestamp)) }

    Surface(
        color = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls } // 화면 터치 시 UI 토글
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 메인 이미지
            imageBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = "전체 화면 사진",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. 상단 바 (왼쪽 뒤로가기 버튼)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.Start // 왼쪽 배치
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Text("◀", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. 하단 상세 정보 및 액션 바
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(bottom = 48.dp, top = 20.dp, start = 24.dp, end = 24.dp)
                ) {
                    // 촬영 정보
                    Text(text = "촬영 정보", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = fullDateString, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 액션 버튼들
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 공유
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onShareClick() }
                        ) {
                            Text("📤", fontSize = 24.sp)
                            Text("공유", color = Color.White, fontSize = 12.sp)
                        }
                        
                        // 삭제
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onDeleteClick() }
                        ) {
                            Text("🗑️", fontSize = 24.sp)
                            Text("삭제", color = Color.White, fontSize = 12.sp)
                        }
                        
                        // 즐겨찾기 (실시간 하트 상태 반영)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onFavoriteToggle() }
                        ) {
                            Text(
                                text = if (image.isFavorite) "❤️" else "🤍",
                                fontSize = 24.sp
                            )
                            Text("즐겨찾기", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryItem(
    image: GalleryImage,
    onItemClick: () -> Unit, // [추가됨]
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
        }
    }

    // 날짜 포맷팅 (예: 2024.05.12)
    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val dateString = remember(image.timestamp) { dateFormatter.format(Date(image.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .clickable { onItemClick() }, // [추가됨] 카드 클릭 시 전체 화면 보기
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column {
            // 1. 이미지 영역 (카드 상단)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.LightGray)
            ) {
                imageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = "스크린샷",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // 즐겨찾기 표시 (상점의 '보유중' 태그 느낌)
                if (image.isFavorite) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Favorite",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 2. 하단 정보 및 액션 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "📸 $dateString",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 공유 및 삭제
                    Row {
                        IconButton(onClick = onShareClick, modifier = Modifier.size(24.dp)) {
                            Text("📤", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                    
                    // 하트 토글
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(24.dp)) {
                        Text(
                            text = if (image.isFavorite) "❤️" else "🤍",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
