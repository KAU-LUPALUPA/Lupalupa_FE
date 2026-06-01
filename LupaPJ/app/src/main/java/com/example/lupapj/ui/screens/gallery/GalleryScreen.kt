package com.example.lupapj.ui.screens.gallery

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.lupapj.R
import com.example.lupapj.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryItem(
    image: GalleryImage,
    onItemClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val dateString = remember(image.timestamp) { dateFormatter.format(Date(image.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .clickable { onItemClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
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
                
                if (image.isFavorite) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "📸 $dateString${if (image.isBackedUp) " ☁️" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(onClick = onShareClick, modifier = Modifier.size(24.dp)) {
                            Text("📤", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                    
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

    var showControls by remember { mutableStateOf(false) }
    val fullDateFormatter = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss", Locale.KOREA) }
    val fullDateString = remember(image.timestamp) { fullDateFormatter.format(Date(image.timestamp)) }

    Surface(
        color = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            imageBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = "전체 화면 사진",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Text("◀", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

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
                    Text(text = "촬영 정보", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = fullDateString, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onShareClick() }
                        ) {
                            Text("📤", fontSize = 24.sp)
                            Text("공유", color = Color.White, fontSize = 12.sp)
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onDeleteClick() }
                        ) {
                            Text("🗑️", fontSize = 24.sp)
                            Text("삭제", color = Color.White, fontSize = 12.sp)
                        }
                        
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    onBackClick: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val context = LocalContext.current
    var fullscreenImageId by remember { mutableStateOf<String?>(null) }
    val fullscreenImage = remember(fullscreenImageId, images) {
        images.find { it.id == fullscreenImageId }
    }
    var imageToDelete by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            containerColor = Color.Transparent,
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

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                color = Color(0xFFFFFBF0).copy(alpha = 0.75f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxSize()
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
            }
        }

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

    BackHandler(enabled = true) {
        if (fullscreenImageId != null) {
            fullscreenImageId = null 
        } else {
            onBackClick()
        }
    }
}
