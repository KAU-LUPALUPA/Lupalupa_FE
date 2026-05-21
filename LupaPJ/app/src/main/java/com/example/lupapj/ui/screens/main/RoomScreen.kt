package com.example.lupapj.ui.screens.main

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import com.example.lupapj.R
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.MainMenuAction
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.iconRes
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendHomeVisitSession
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.ui.components.FloatingMailboxButton
import com.example.lupapj.ui.components.InventorySheet
import com.example.lupapj.ui.components.MailboxSheet
import com.example.lupapj.ui.components.RoomViewport
import com.example.lupapj.ui.preview.previewRoomUiState
import com.example.lupapj.ui.screens.friends.FriendRoomChatPanel
import com.example.lupapj.ui.theme.LupaPJTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
@Composable
fun RoomScreen(
    uiState: RoomUiState?,
    placeholderMessage: String?,
    onButtonAClick: () -> Unit,
    onButtonBClick: () -> Unit,
    onInventoryDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onRoomObjectClick: (RoomObjectType) -> Unit,
    onRearrangeClick: () -> Unit,
    onRearrangeMoveUp: () -> Unit,
    onRearrangeMoveDown: () -> Unit,
    onRearrangeMoveLeft: () -> Unit,
    onRearrangeMoveRight: () -> Unit,
    onRearrangeConfirm: () -> Unit,
    onRearrangeCancel: () -> Unit,
    onFloorTap: (FloorAnchor) -> Unit,
    onDroppedToyClick: () -> Unit,
    onBottomNavItemClick: (BottomNavItem) -> Unit,
    recentMainMenuAction: MainMenuAction?,
    onPlaceholderMessageConsumed: () -> Unit,
    onSetCameraZoom: (Float) -> Unit,
    onSetCameraOffset: (Float, Float) -> Unit, // [추가됨] 한 손가락 드래그 Panning 콜백
    onCaptureClick: (Bitmap) -> Unit,
    onExitCameraMode: () -> Unit,
    onGalleryClick: () -> Unit, // [추가됨] 카메라 오버레이 내 갤러리 버튼 콜백
    currencyAmount: Int,
    purchasedShopItems: List<com.example.lupapj.data.model.ShopItem>,
    onEquipClick: (String) -> Unit, // [추가됨(권)] 아이템 장착 콜백 추가
    onUnequipClick: (String) -> Unit, // [추가됨(권)] 아이템 해제 콜백 추가
    onPlaygroundClick: () -> Unit,
    mailboxVisible: Boolean,
    friendRequests: List<FriendRequest>,
    homeInvitations: List<FriendHomeInvitation>,
    hostingHomeVisitSessions: List<FriendHomeVisitSession>,
    hostingVisitMessages: List<FriendMessage>,
    hostingVisitMessageInput: String,
    isSendingHostingVisitMessage: Boolean,
    onHostingVisitMessageInputChange: (String) -> Unit,
    onSendHostingVisitMessage: () -> Unit,
    onEndHostingVisit: () -> Unit,
    onMailboxClick: () -> Unit,
    onMailboxDismiss: () -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onRejectFriendRequest: (String) -> Unit,
    onAcceptHomeInvitation: (String) -> Unit, // [추가됨(권)] 복구됨
    onRejectHomeInvitation: (String) -> Unit,
    behaviorDebugInfo: com.example.lupapj.data.model.BehaviorDebugInfo,
    onToggleBehaviorDebugClick: () -> Unit,
    onMinigameClick: () -> Unit // [수정됨(권)] 미니게임 클릭 콜백 추가
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // [추가됨(권)]
    val rootView = LocalView.current
    val density = LocalDensity.current
    var cameraOverlaySize by remember { mutableStateOf(IntSize.Zero) }
    var cameraOverlayTopLeft by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(placeholderMessage) {
        placeholderMessage?.let {
            snackbarHostState.showSnackbar(it)
            onPlaceholderMessageConsumed()
        }
    }

    val room = uiState ?: return
    val mailboxItemCount = friendRequests.size + homeInvitations.size
    val activeHostingVisitSession = hostingHomeVisitSessions.firstOrNull()
    val visitingCompanionPets = hostingHomeVisitSessions.mapNotNull { session ->
        session.visitorPet?.copy(
            petId = "${session.id}_${session.visitorPet.petId}",
            action = PetAction.IDLE
        )
    }

    // [추가됨] 카메라 모드 진입 시 캐릭터를 중앙에 두도록 자동 확대
    LaunchedEffect(room.isCameraMode) {
        if (room.isCameraMode) {
            // 캐릭터 위치를 기준으로 자동 2x 줌
            onSetCameraZoom(2f)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (room.isCameraMode) {
                // 1. 배경 레이어: 블러 처리된 비확대 뷰포트 (프레임 바깥 영역을 담당)
                RoomViewport(
                    uiState = room,
                    companionPets = visitingCompanionPets,
                    onRoomObjectClick = {},
                    onFloorTap = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radiusX = 18.dp, radiusY = 18.dp)
                )

                // 2. 전경 레이어: 확대된 선명한 뷰포트 (중앙 프레임 내부만 보이도록 클리핑)
                val density = androidx.compose.ui.platform.LocalDensity.current
                val cornerRadiusPx = with(density) { 16.dp.toPx() }
                
                val frameShape = remember {
                    androidx.compose.foundation.shape.GenericShape { size, _ ->
                        val fw = size.width * CAMERA_FRAME_WIDTH_FRACTION
                        val fh = size.height * CAMERA_FRAME_HEIGHT_FRACTION
                        val left = (size.width - fw) / 2
                        val top = (size.height - fh) / 2
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(left, top, left + fw, top + fh),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                            )
                        )
                    }
                }

                RoomViewport(
                    uiState = room,
                    companionPets = visitingCompanionPets,
                    onRoomObjectClick = onRoomObjectClick,
                    onFloorTap = onFloorTap,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(frameShape) // 중앙 프레임 영역만 남기고 나머지는 아래의 블러 배경이 보이게 함
                        .graphicsLayer {
                            scaleX = room.cameraZoom
                            scaleY = room.cameraZoom
                            translationX = room.cameraOffsetX
                            translationY = room.cameraOffsetY
                            transformOrigin = TransformOrigin.Center
                        }
                )
            } else {
                LupaMainScreen(
                    petSatiety = room.pet.status.satiety.coerceIn(0, 100),
                    petVitality = room.pet.status.vitality.coerceIn(0, 100),
                    petPersonality = room.pet.personality, // [추가됨(권)]
                    recentIconRes = recentMainMenuAction?.iconRes,
                    onConditionTabClick = { // [추가됨(권)] 터치 시 성격 스낵바 출력
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("현재 성격: ${room.pet.personality.name}")
                        }
                    },
                    onRecentActionClick = onButtonAClick,
                    onInventoryClick = onButtonBClick,
                    onSettingClick = onSettingsClick,
                    onPopupMenuItemClick = onBottomNavItemClick,
                    onPlaygroundClick = onPlaygroundClick,
                    onMinigameClick = onMinigameClick, // [수정됨(권)] 전달
                    roomContent = {
                        RoomViewport(
                            uiState = room,
                            companionPets = visitingCompanionPets,
                            onRoomObjectClick = onRoomObjectClick,
                            onFloorTap = onFloorTap,
                            onDroppedToyClick = onDroppedToyClick,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (hostingHomeVisitSessions.isNotEmpty()) {
                            val visitorNames = hostingHomeVisitSessions
                                .map { it.visitorUser.nickname }
                                .distinct()
                                .joinToString(", ")
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFEADFD3).copy(alpha = 0.94f),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 112.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${visitorNames}님이 놀러왔어요.",
                                        color = Color(0xFF5C4033),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onEndHostingVisit) {
                                        Text("방문 종료", color = Color(0xFF7F5539))
                                    }
                                }
                            }
                        }

                        if (activeHostingVisitSession != null) {
                            FriendRoomChatPanel(
                                ownerName = activeHostingVisitSession.visitorUser.nickname,
                                messages = hostingVisitMessages,
                                messageInput = hostingVisitMessageInput,
                                isSendingMessage = isSendingHostingVisitMessage,
                                onMessageInputChange = onHostingVisitMessageInputChange,
                                onSendMessage = onSendHostingVisitMessage,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                                    .imePadding()
                            )
                        }
                        
                        // [추가됨(권)] 행동 디버깅 토글 버튼
                        androidx.compose.material3.TextButton(
                            onClick = onToggleBehaviorDebugClick,
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 120.dp)
                        ) {
                            Text("디버그", color = Color.White)
                        }

                        // [추가됨(권)] 행동 디버깅 윈도우 (구석 배치)
                        if (behaviorDebugInfo.isVisible) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                contentColor = Color.Green,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 180.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Action: ${room.pet.currentAction.name}")
                                    Text("Crisis: ${behaviorDebugInfo.isCrisis}")
                                    Text("Ticks: ${behaviorDebugInfo.consecutiveTicks}")
                                    Text("Prob: ${"%.2f".format(behaviorDebugInfo.currentProbability)}")
                                    Text("M: ${behaviorDebugInfo.mValue}, k: ${behaviorDebugInfo.kValue}")
                                }
                            }
                        }
                    }
                )
            }

            if (!room.rearrangeMode && !room.isCameraMode) {
                Surface(
                    onClick = onRearrangeClick,
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF7F5539),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 112.dp, end = 16.dp)
                ) {
                    Text(
                        text = "재배치",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            } else if (room.rearrangeMode) { // [수정됨] 카메라 모드일 땐 이 팝업 표시 안 함
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFEADFD3),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 120.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "현재 재배치 중입니다.",
                            color = Color(0xFF5C4033)
                        )

                        Surface(
                            onClick = onRearrangeCancel,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF7F5539)
                        ) {
                            Text(
                                text = "취소",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (!room.isCameraMode && !room.rearrangeMode) {
                FloatingMailboxButton(
                    itemCount = mailboxItemCount,
                    onClick = onMailboxClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 198.dp, end = 14.dp)
                )
            }
            if (room.rearrangeMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color.Transparent)
                        .clickable(enabled = true) {
                            // 재배치 중 하단 메뉴 클릭만 막기
                        }
                )
            }
            if (room.rearrangeMode && room.selectedRearrangeObjectType != null) {

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 85.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(80.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RearrangeCircleButton(
                            onClick = onRearrangeMoveLeft
                        ) {
                            Text(
                                text = "↖",
                                color = Color.White
                            )
                        }

                        RearrangeCircleButton(
                            onClick = onRearrangeMoveUp
                        ) {
                            Text(
                                text = "↗",
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = onRearrangeConfirm,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFB08968),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {

                        Text(
                            text = "✓",
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(80.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RearrangeCircleButton(
                            onClick = onRearrangeMoveDown
                        ) {
                            Text(
                                text = "↙",
                                color = Color.White
                            )
                        }

                        RearrangeCircleButton(
                            onClick = onRearrangeMoveRight
                        ) {
                            Text(
                                text = "↘",
                                color = Color.White
                            )
                        }
                    }
                }
                }
            }

            if (room.isCameraMode) {
                CameraOverlay(
                    zoom = room.cameraZoom,
                    onZoomChange = onSetCameraZoom,
                    onPan = onSetCameraOffset, // [추가됨] 한 손가락 드래그 Panning
                    onCapture = {
                        if (cameraOverlaySize.width > 0 && cameraOverlaySize.height > 0) {
                            val screenBitmap = rootView.drawToBitmap(Bitmap.Config.ARGB_8888)
                            val frameWidth = (cameraOverlaySize.width * CAMERA_FRAME_WIDTH_FRACTION).toInt()
                            val frameHeight = (cameraOverlaySize.height * CAMERA_FRAME_HEIGHT_FRACTION).toInt()
                            val frameLeft = cameraOverlayTopLeft.x + ((cameraOverlaySize.width - frameWidth) / 2f)
                            val frameTop = cameraOverlayTopLeft.y + ((cameraOverlaySize.height - frameHeight) / 2f)
                            val frameInsetPx = with(density) { 6.dp.roundToPx() }

                            val cropLeft = (frameLeft.toInt() + frameInsetPx).coerceIn(0, screenBitmap.width - 1)
                            val cropTop = (frameTop.toInt() + frameInsetPx).coerceIn(0, screenBitmap.height - 1)
                            val cropWidth = (frameWidth - (frameInsetPx * 2))
                                .coerceAtLeast(1)
                                .coerceAtMost(screenBitmap.width - cropLeft)
                            val cropHeight = (frameHeight - (frameInsetPx * 2))
                                .coerceAtLeast(1)
                                .coerceAtMost(screenBitmap.height - cropTop)

                            val croppedBitmap = Bitmap.createBitmap(
                                screenBitmap,
                                cropLeft,
                                cropTop,
                                cropWidth,
                                cropHeight
                            )
                            onCaptureClick(croppedBitmap)
                        }
                    },
                    onCancel = onExitCameraMode,
                    onGalleryClick = onGalleryClick, // [추가됨] 갤러리 연결
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            cameraOverlaySize = coordinates.size
                            cameraOverlayTopLeft = coordinates.positionInRoot()
                        }
                )
            }
        }


    if (room.inventoryVisible) {
        InventorySheet(
            purchasedShopItems = purchasedShopItems,
            equippedItemIds = room.pet.equippedItemIds, // [추가됨(권)] 장착 상태 전달
            onEquipClick = onEquipClick,               // [추가됨(권)] 장착 콜백 연결
            onUnequipClick = onUnequipClick,           // [추가됨(권)] 해제 콜백 연결
            onDismiss = onInventoryDismiss
        )
    }

    if (mailboxVisible) {
        MailboxSheet(
            friendRequests = friendRequests,
            homeInvitations = homeInvitations,
            onDismiss = onMailboxDismiss,
            onAcceptFriendRequest = onAcceptFriendRequest,
            onRejectFriendRequest = onRejectFriendRequest,
            onAcceptHomeInvitation = onAcceptHomeInvitation,
            onRejectHomeInvitation = onRejectHomeInvitation
        )
    }
}

// [수정됨(권)] 로컬 iconRes 제거하고 AppModels의 전역 확장 프로퍼티 사용

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomScreenPreview() {
    LupaPJTheme {
        RoomScreen(
            uiState = previewRoomUiState,
            placeholderMessage = null,
            onButtonAClick = {},
            onButtonBClick = {},
            onInventoryDismiss = {},
            onSettingsClick = {},
            onRoomObjectClick = {},
            onRearrangeClick = {},
            onRearrangeMoveUp = {},
            onRearrangeMoveDown = {},
            onRearrangeMoveLeft = {},
            onRearrangeMoveRight = {},
            onRearrangeConfirm = {},
            onRearrangeCancel = {},
            onFloorTap = {},
            onDroppedToyClick = {},
            onBottomNavItemClick = {},
            recentMainMenuAction = MainMenuAction.SHOP,
            onPlaceholderMessageConsumed = {},
            onSetCameraZoom = {},
            onSetCameraOffset = { _, _ -> },
            onCaptureClick = {},
            onExitCameraMode = {},
            onGalleryClick = {},
            currencyAmount = 100,
            purchasedShopItems = emptyList(),
            onEquipClick = {},
            onUnequipClick = {},
            onPlaygroundClick = {},
            mailboxVisible = false,
            friendRequests = emptyList(),
            homeInvitations = emptyList(),
            hostingHomeVisitSessions = emptyList(),
            hostingVisitMessages = emptyList(),
            hostingVisitMessageInput = "",
            isSendingHostingVisitMessage = false,
            onHostingVisitMessageInputChange = {},
            onSendHostingVisitMessage = {},
            onEndHostingVisit = {},
            onMailboxClick = {},
            onMailboxDismiss = {},
            onAcceptFriendRequest = {},
            onRejectFriendRequest = {},
            onAcceptHomeInvitation = {},
            onRejectHomeInvitation = {},
            behaviorDebugInfo = com.example.lupapj.data.model.BehaviorDebugInfo(),
            onToggleBehaviorDebugClick = {},
            onMinigameClick = {} // [수정됨(권)] 프리뷰 파라미터 추가
        )
    }
}
@Composable
private fun RearrangeCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFF7F5539),
        shadowElevation = 6.dp,
        modifier = Modifier.padding(3.dp)
    ) {

        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
