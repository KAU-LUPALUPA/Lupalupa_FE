package com.example.lupapj.ui.screens.contest

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lupapj.R

private val ContestPanelColor = Color(0xFFFFFBF0).copy(alpha = 0.82f)
private val ContestSlotColor = Color(0xFFFFF3D7)
private val ContestBorderColor = Color(0xFFB88952)
private val ContestPrimaryColor = Color(0xFFE69A42)
private val ContestSecondaryColor = Color(0xFF7BBE84)

private val PreviewCharacterResIds = listOf(
    R.drawable.sprite_0_0,
    R.drawable.sprite_1_0,
    R.drawable.sprite_2_0,
    R.drawable.sprite_3_0,
    R.drawable.sprite_0_1,
    R.drawable.sprite_1_1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestScreen(
    onBackClick: () -> Unit,
    onParticipateClick: () -> Unit = {},
    onVoteClick: () -> Unit = {}
) {
    var isVoteMode by remember { mutableStateOf(false) }
    var selectedCharacterNumber by remember { mutableStateOf<Int?>(null) }

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
                            onClick = onParticipateClick,
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
    isVoteMode: Boolean,
    selectedCharacterNumber: Int?,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        characterResIds.chunked(3).take(2).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEachIndexed { columnIndex, characterResId ->
                    ContestCharacterSlot(
                        slotNumber = rowIndex * 3 + columnIndex + 1,
                        characterResId = characterResId,
                        isVoteMode = isVoteMode,
                        isSelected = selectedCharacterNumber == rowIndex * 3 + columnIndex + 1,
                        onClick = { onCharacterClick(rowIndex * 3 + columnIndex + 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContestCharacterSlot(
    slotNumber: Int,
    characterResId: Int,
    isVoteMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> ContestSecondaryColor
        isVoteMode -> ContestPrimaryColor.copy(alpha = 0.7f)
        else -> ContestBorderColor.copy(alpha = 0.55f)
    }
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Column(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .background(ContestSlotColor)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = isVoteMode, onClick = onClick)
            .padding(10.dp),
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

        Image(
            painter = painterResource(id = characterResId),
            contentDescription = "콘테스트 캐릭터 $slotNumber",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
        )

        Surface(
            color = Color.White.copy(alpha = 0.72f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(width = 68.dp, height = 24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isSelected) "선택됨" else "캐릭터",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B4423),
                    maxLines = 1
                )
            }
        }
    }
}
