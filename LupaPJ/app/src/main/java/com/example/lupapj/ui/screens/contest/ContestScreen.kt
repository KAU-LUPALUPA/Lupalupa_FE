package com.example.lupapj.ui.screens.contest

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
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
import com.example.lupapj.data.repository.ContestEntryInfo
import com.example.lupapj.data.repository.ContestGroupDetail
import com.example.lupapj.data.repository.ContestGroupSummary
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ContestPanelColor = Color(0xFFFFFBF0).copy(alpha = 0.82f)
private val ContestSlotColor = Color(0xFFFFF3D7)
private val ContestBorderColor = Color(0xFFB88952)
private val ContestPrimaryColor = Color(0xFFE69A42)
private val ContestSecondaryColor = Color(0xFF7BBE84)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestScreen(
    galleryImages: List<GalleryImage>,
    isUploadingEntry: Boolean,
    uploadMessage: String?,
    groups: List<ContestGroupSummary>,
    selectedGroup: ContestGroupDetail?,
    isLoadingGroups: Boolean,
    groupMessage: String?,
    isParticipating: Boolean,
    myEntryImageUrl: String?,
    isSubmittingVote: Boolean,
    voteMessage: String?,
    onEntryImageSelected: (GalleryImage) -> Unit,
    onGroupClick: (String) -> Unit,
    onGroupBackClick: () -> Unit,
    onVoteEntryClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onParticipateClick: () -> Unit = {}
) {
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
                        .verticalScroll(rememberScrollState())
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

                    if (selectedGroup == null) {
                        ContestActionButton(
                            text = if (isParticipating) "사진 바꾸기" else "참가하기",
                            containerColor = ContestPrimaryColor,
                            onClick = {
                                isGalleryPickerVisible = true
                                onParticipateClick()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when {
                                isUploadingEntry -> "선택한 이미지를 S3에 업로드 중입니다."
                                uploadMessage != null -> uploadMessage
                                isParticipating -> "조에 참가중입니다."
                                selectedEntryImage != null -> "선택한 사진이 내 참가 칸에 등록되었어요."
                                else -> "참가할 캐릭터 사진을 선택해주세요."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B4423),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MyContestEntrySlot(
                            selectedEntryImage = selectedEntryImage,
                            serverImageUrl = myEntryImageUrl,
                            isParticipating = isParticipating,
                            modifier = Modifier.fillMaxWidth(0.52f)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        ContestGroupList(
                            groups = groups,
                            isLoading = isLoadingGroups,
                            message = groupMessage,
                            onGroupClick = onGroupClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ContestGroupDetailPanel(
                            group = selectedGroup,
                            isLoading = isLoadingGroups,
                            message = groupMessage,
                            isSubmittingVote = isSubmittingVote,
                            voteMessage = voteMessage,
                            onBackClick = onGroupBackClick,
                            onVoteEntryClick = onVoteEntryClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (isGalleryPickerVisible) {
            ContestGalleryPicker(
                images = galleryImages,
                initialSelectedImageId = selectedEntryImage?.id,
                onImageConfirmed = { image ->
                    selectedEntryImage = image
                    isGalleryPickerVisible = false
                    onEntryImageSelected(image)
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
private fun MyContestEntrySlot(
    selectedEntryImage: GalleryImage?,
    serverImageUrl: String?,
    isParticipating: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedBitmap = selectedEntryImage?.let { image ->
        produceState<ImageBitmap?>(initialValue = null, image.filePath) {
            value = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
            }
        }.value
    }

    Column(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(18.dp))
            .background(ContestSlotColor)
            .border(1.dp, ContestBorderColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "내 캐릭터 사진",
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
                serverImageUrl != null -> ContestRemoteImage(
                    imageUrl = serverImageUrl,
                    contentDescription = "서버에 등록된 내 콘테스트 참가 사진",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
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
                        selectedEntryImage != null || serverImageUrl != null -> "내 참가작"
                        isParticipating -> "이미지 대기"
                        else -> "선택 대기"
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
private fun ContestGroupList(
    groups: List<ContestGroupSummary>,
    isLoading: Boolean,
    message: String?,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "매칭된 조",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C371D)
        )

        when {
            isLoading && groups.isEmpty() -> Text(
                text = "조 목록을 불러오는 중입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F)
            )
            groups.isEmpty() -> Text(
                text = message ?: "아직 생성된 콘테스트 조가 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F)
            )
            else -> groups.forEach { group ->
                ContestGroupRow(
                    group = group,
                    onClick = { onGroupClick(group.groupId) }
                )
            }
        }

        if (!message.isNullOrBlank() && groups.isNotEmpty()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F)
            )
        }
    }
}

@Composable
private fun ContestGroupRow(
    group: ContestGroupSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, ContestBorderColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${group.groupNumber.coerceAtLeast(1L)}조",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C371D)
            )
            Text(
                text = when (group.status) {
                    "ACTIVE" -> "진행 중"
                    "OPEN" -> "${group.memberCount.coerceAtMost(3L)}/3명 매칭 중"
                    else -> group.status
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F)
            )
        }

        Text(
            text = if (group.isMyGroup) "내 조" else "보기",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (group.isMyGroup) ContestPrimaryColor else ContestSecondaryColor
        )
    }
}

@Composable
private fun ContestGroupDetailPanel(
    group: ContestGroupDetail,
    isLoading: Boolean,
    message: String?,
    isSubmittingVote: Boolean,
    voteMessage: String?,
    onBackClick: () -> Unit,
    onVoteEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVoteMode by remember(group.groupId) { mutableStateOf(false) }
    var selectedVoteEntryId by remember(group.groupId) { mutableStateOf<Long?>(null) }
    val canVote = group.status != "CLOSED" &&
        group.entries.any { entry -> entry.confirmed }
    val standings = remember(group.entries) { buildContestStandings(group.entries) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${group.groupNumber.coerceAtLeast(1L)}조",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C371D)
                )
                Text(
                    text = when (group.status) {
                        "ACTIVE" -> "진행 중"
                        "OPEN" -> "${group.entries.size.coerceAtMost(3)}/3명 매칭 중"
                        else -> group.status
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF80684F)
                )
            }

            Button(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ContestSecondaryColor)
            ) {
                Text("목록")
            }
        }

        if (isLoading) {
            Text(
                text = "조 정보를 불러오는 중입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F)
            )
        } else if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = {
                isVoteMode = !isVoteMode
                selectedVoteEntryId = null
            },
            enabled = canVote && !isSubmittingVote,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ContestSecondaryColor,
                disabledContainerColor = Color(0xFFD8C7AD),
                disabledContentColor = Color.White.copy(alpha = 0.72f)
            )
        ) {
            Text(if (isVoteMode) "투표 취소" else "투표하기")
        }

        Text(
            text = when {
                !voteMessage.isNullOrBlank() -> voteMessage
                group.status == "CLOSED" -> "종료된 조에는 투표할 수 없습니다."
                !canVote -> "투표할 수 있는 참가 사진이 없습니다."
                selectedVoteEntryId != null -> "선택한 참가 사진에 투표하려면 확인을 눌러주세요."
                isVoteMode -> "투표할 참가 사진을 선택해주세요."
                else -> "투표하기를 누르면 참가 사진을 선택할 수 있습니다."
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF80684F),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        ContestGroupEntryGrid(
            entries = group.entries,
            myEntryId = group.myEntryId,
            isVoteMode = isVoteMode,
            isSubmittingVote = isSubmittingVote,
            selectedVoteEntryId = selectedVoteEntryId,
            onVoteEntryClick = { entryId -> selectedVoteEntryId = entryId },
            modifier = Modifier.fillMaxWidth()
        )

        ContestVoteStatusPanel(
            standings = standings,
            myEntryId = group.myEntryId,
            modifier = Modifier.fillMaxWidth()
        )

        if (isVoteMode) {
            Button(
                onClick = {
                    selectedVoteEntryId?.let { entryId ->
                        isVoteMode = false
                        selectedVoteEntryId = null
                        onVoteEntryClick(entryId)
                    }
                },
                enabled = selectedVoteEntryId != null && !isSubmittingVote,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ContestPrimaryColor,
                    disabledContainerColor = Color(0xFFD8C7AD),
                    disabledContentColor = Color.White.copy(alpha = 0.72f)
                )
            ) {
                Text("확인")
            }
        }
    }
}

private data class ContestEntryStanding(
    val entry: ContestEntryInfo,
    val slotNumber: Int,
    val rank: Int
)

private fun buildContestStandings(entries: List<ContestEntryInfo>): List<ContestEntryStanding> {
    val orderedEntries = entries
        .mapIndexed { index, entry -> index + 1 to entry }
        .sortedWith(
            compareByDescending<Pair<Int, ContestEntryInfo>> { it.second.voteCount }
                .thenBy { it.first }
        )

    var previousVoteCount: Int? = null
    var currentRank = 0
    return orderedEntries.mapIndexed { index, pair ->
        val voteCount = pair.second.voteCount
        if (previousVoteCount != voteCount) {
            currentRank = index + 1
            previousVoteCount = voteCount
        }

        ContestEntryStanding(
            entry = pair.second,
            slotNumber = pair.first,
            rank = currentRank
        )
    }
}

@Composable
private fun ContestVoteStatusPanel(
    standings: List<ContestEntryStanding>,
    myEntryId: Long?,
    modifier: Modifier = Modifier
) {
    val maxVoteCount = standings.maxOfOrNull { it.entry.voteCount } ?: 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "투표 현황",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C371D)
        )

        if (standings.isEmpty()) {
            Text(
                text = "아직 조에 참가자가 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF80684F)
            )
        } else {
            standings.forEach { standing ->
                ContestStandingRow(
                    standing = standing,
                    isMine = standing.entry.entryId == myEntryId,
                    maxVoteCount = maxVoteCount
                )
            }
        }
    }
}

@Composable
private fun ContestStandingRow(
    standing: ContestEntryStanding,
    isMine: Boolean,
    maxVoteCount: Int
) {
    val voteFraction = when {
        maxVoteCount <= 0 -> 0f
        else -> standing.entry.voteCount.toFloat() / maxVoteCount.toFloat()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${standing.rank}위",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C371D)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.58f))
                .border(1.dp, ContestBorderColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${standing.slotNumber}번",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C371D)
                    )
                    Text(
                        text = if (standing.entry.confirmed) "사진 등록 완료" else "이미지 없음",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF80684F)
                    )
                }

                Text(
                    text = "${standing.entry.voteCount}표",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ContestPrimaryColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEADCC7))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(voteFraction.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isMine) ContestPrimaryColor else ContestSecondaryColor)
                )
                if (standing.entry.voteCount == 0) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFBFA280).copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ContestGroupEntryGrid(
    entries: List<ContestEntryInfo>,
    myEntryId: Long?,
    isVoteMode: Boolean,
    isSubmittingVote: Boolean,
    selectedVoteEntryId: Long?,
    onVoteEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) { index ->
            val entry = entries.getOrNull(index)
            ContestGroupEntrySlot(
                slotNumber = index + 1,
                entry = entry,
                isMine = entry?.entryId == myEntryId,
                isVoteMode = isVoteMode,
                isSubmittingVote = isSubmittingVote,
                isVoteSelected = entry?.entryId == selectedVoteEntryId,
                onVoteEntryClick = onVoteEntryClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ContestGroupEntrySlot(
    slotNumber: Int,
    entry: ContestEntryInfo?,
    isMine: Boolean,
    isVoteMode: Boolean,
    isSubmittingVote: Boolean,
    isVoteSelected: Boolean,
    onVoteEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val canSelectForVote = isVoteMode && !isSubmittingVote && entry?.confirmed == true

    Column(
        modifier = modifier
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(18.dp))
            .background(ContestSlotColor)
            .border(
                width = if (isMine || isVoteSelected) 3.dp else 1.dp,
                color = when {
                    isVoteSelected -> ContestSecondaryColor
                    isMine -> ContestPrimaryColor
                    else -> ContestBorderColor.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                enabled = canSelectForVote,
                onClick = { entry?.let { onVoteEntryClick(it.entryId) } }
            )
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
                entry?.imageUrl != null -> ContestRemoteImage(
                    imageUrl = entry.imageUrl,
                    contentDescription = "콘테스트 ${slotNumber}번 참가 사진",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
                entry != null -> Text(
                    text = "이미지 없음",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9A8065),
                    textAlign = TextAlign.Center
                )
                else -> Text(
                    text = "빈 자리",
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
                        entry == null -> "대기 중"
                        isVoteSelected -> "선택됨"
                        isMine -> "내 참가"
                        entry.confirmed -> "${entry.voteCount}표"
                        else -> "이미지 없음"
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
private fun ContestRemoteImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val imageState by produceState<Pair<Boolean, ImageBitmap?>>(initialValue = false to null, imageUrl) {
        value = true to withContext(Dispatchers.IO) {
            runCatching {
                URL(imageUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    if (imageState.second != null) {
        Image(
            bitmap = imageState.second!!,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFE7D7BF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (imageState.first) "불러오기 실패" else "로딩",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF80684F),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ContestGalleryPicker(
    images: List<GalleryImage>,
    initialSelectedImageId: String?,
    onImageConfirmed: (GalleryImage) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingImage by remember(images, initialSelectedImageId) {
        mutableStateOf(images.firstOrNull { it.id == initialSelectedImageId })
    }

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
                                isSelected = pendingImage?.id == image.id,
                                onClick = { pendingImage = image }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ContestPrimaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }

                    Button(
                        onClick = { pendingImage?.let(onImageConfirmed) },
                        enabled = pendingImage != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ContestSecondaryColor,
                            disabledContainerColor = Color(0xFFD8C7AD),
                            disabledContentColor = Color.White.copy(alpha = 0.72f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("확인")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContestGalleryThumbnail(
    image: GalleryImage,
    isSelected: Boolean,
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
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) ContestSecondaryColor else Color.White.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
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
