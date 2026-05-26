package com.example.lupapj.ui.screens.contest

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lupapj.R
import com.example.lupapj.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ContestPanelColor = Color(0xFFFFFBF0).copy(alpha = 0.82f)
private val ContestSlotColor = Color(0xFFFFF3D7)
private val ContestBorderColor = Color(0xFFB88952)
private val ContestPrimaryColor = Color(0xFFE69A42)
private val ContestSecondaryColor = Color(0xFF7BBE84)

private val PreviewCharacterResIds = listOf(
    R.drawable.sprite_1_0,
    R.drawable.sprite_2_0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestScreen(
    galleryImages: List<GalleryImage>,
    onBackClick: () -> Unit,
    onParticipateClick: () -> Unit = {},
    onVoteClick: () -> Unit = {}
) {
    var isVoteMode by remember { mutableStateOf(false) }
    var selectedCharacterNumber by remember { mutableStateOf<Int?>(null) }
    var isGalleryPickerVisible by remember { mutableStateOf(false) }
    var selectedEntryImage by remember { mutableStateOf<GalleryImage?>(null) }

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
                    title = { Text("콘테스트") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Text("<")
                        }
                    }
                )
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                color = ContestPanelColor,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "콘테스트",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C371D)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ContestActionButton(
                            text = "참가하기",
                            containerColor = ContestPrimaryColor,
                            onClick = {
                                isVoteMode = false
                                selectedCharacterNumber = null
                                isGalleryPickerVisible = true
                                onParticipateClick()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ContestActionButton(
                            text = "투표하기",
                            containerColor = ContestSecondaryColor,
                            onClick = {
                                isVoteMode = true
                                selectedCharacterNumber = null
                                onVoteClick()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            selectedEntryImage != null && !isVoteMode -> "선택한 사진이 내 참가 칸에 등록되었어요."
                            !isVoteMode -> "투표하기를 누르면 캐릭터를 선택할 수 있어요."
                            selectedCharacterNumber == null -> "투표할 캐릭터를 선택해주세요."
                            else -> "${selectedCharacterNumber}번 캐릭터를 선택했어요."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B4423),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ContestCharacterGrid(
                        characterResIds = PreviewCharacterResIds,
                        selectedEntryImage = selectedEntryImage,
                        isVoteMode = isVoteMode,
                        selectedCharacterNumber = selectedCharacterNumber,
                        onCharacterClick = { characterNumber ->
                            if (isVoteMode) {
                                selectedCharacterNumber = characterNumber
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (isGalleryPickerVisible) {
            ContestGalleryPicker(
                images = galleryImages,
                onImageSelected = { image ->
                    selectedEntryImage = image
                    isGalleryPickerVisible = false
                },
                onDismiss = { isGalleryPickerVisible = false }
            )
        }
    }
}

@Composable
private fun ContestActionButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun ContestCharacterGrid(
    characterResIds: List<Int>,
    selectedEntryImage: GalleryImage?,
    isVoteMode: Boolean,
    selectedCharacterNumber: Int?,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ContestCharacterSlot(
            slotNumber = 1,
            characterResId = null,
            galleryImage = selectedEntryImage,
            emptyLabel = "내 참가 칸",
            isVoteMode = isVoteMode,
            isSelected = selectedCharacterNumber == 1,
            onClick = { onCharacterClick(1) },
            modifier = Modifier.weight(1f)
        )

        characterResIds.take(2).forEachIndexed { index, characterResId ->
            val slotNumber = index + 2
            ContestCharacterSlot(
                slotNumber = slotNumber,
                characterResId = characterResId,
                galleryImage = null,
                emptyLabel = "캐릭터",
                isVoteMode = isVoteMode,
                isSelected = selectedCharacterNumber == slotNumber,
                onClick = { onCharacterClick(slotNumber) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ContestCharacterSlot(
    slotNumber: Int,
    characterResId: Int?,
    galleryImage: GalleryImage?,
    emptyLabel: String,
    isVoteMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBitmap = galleryImage?.let { image ->
        produceState<ImageBitmap?>(initialValue = null, image.filePath) {
            value = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
            }
        }.value
    }
    val borderColor = when {
        isSelected -> ContestSecondaryColor
        isVoteMode -> ContestPrimaryColor.copy(alpha = 0.7f)
        else -> ContestBorderColor.copy(alpha = 0.55f)
    }
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Column(
        modifier = modifier
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(18.dp))
            .background(ContestSlotColor)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = isVoteMode, onClick = onClick)
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${slotNumber}번",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B4423),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                selectedBitmap != null -> Image(
                    bitmap = selectedBitmap,
                    contentDescription = "내 콘테스트 참가 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
                characterResId != null -> Image(
                    painter = painterResource(id = characterResId),
                    contentDescription = "콘테스트 캐릭터 $slotNumber",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Text(
                    text = "사진 없음",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9A8065)
                )
            }
        }

        Surface(
            color = Color.White.copy(alpha = 0.72f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        isSelected -> "선택됨"
                        galleryImage != null -> "내 참가작"
                        else -> emptyLabel
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B4423),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ContestGalleryPicker(
    images: List<GalleryImage>,
    onImageSelected: (GalleryImage) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFFFFFBF0),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "갤러리에서 사진 한 장을 선택해주세요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C371D),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "갤러리에 저장된 사진이 없습니다.",
                            color = Color(0xFF80684F)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    ) {
                        items(images, key = { it.id }) { image ->
                            ContestGalleryThumbnail(
                                image = image,
                                onClick = { onImageSelected(image) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ContestPrimaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("취소")
                }
            }
        }
    }
}

@Composable
private fun ContestGalleryThumbnail(
    image: GalleryImage,
    onClick: () -> Unit
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE7D7BF))
            .clickable(onClick = onClick)
    ) {
        imageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "선택할 갤러리 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
