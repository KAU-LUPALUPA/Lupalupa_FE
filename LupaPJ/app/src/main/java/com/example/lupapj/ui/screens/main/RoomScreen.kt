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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.example.lupapj.data.model.iconRes
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.ui.components.FloatingMailboxButton
import com.example.lupapj.ui.components.InventorySheet
import com.example.lupapj.ui.components.MailboxSheet
import com.example.lupapj.ui.components.RoomViewport
import com.example.lupapj.ui.preview.previewRoomUiState
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
    onBottomNavItemClick: (BottomNavItem) -> Unit,
    recentMainMenuAction: MainMenuAction?,
    onPlaceholderMessageConsumed: () -> Unit,
    onSetCameraZoom: (Float) -> Unit,
    onCaptureClick: (Bitmap) -> Unit,
    onExitCameraMode: () -> Unit,
    currencyAmount: Int,
    purchasedShopItems: List<com.example.lupapj.data.model.ShopItem>,
    onPlaygroundClick: () -> Unit,
    mailboxVisible: Boolean,
    friendRequests: List<FriendRequest>,
    homeInvitations: List<FriendHomeInvitation>,
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
                RoomViewport(
                    uiState = room,
                    onRoomObjectClick = onRoomObjectClick,
                    onFloorTap = onFloorTap,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = room.cameraZoom
                            scaleY = room.cameraZoom
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
                            onRoomObjectClick = onRoomObjectClick,
                            onFloorTap = onFloorTap,
                            modifier = Modifier.fillMaxSize()
                        )
                        
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

            if (!room.rearrangeMode) {
                Surface(
                    onClick = onRearrangeClick,
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF7F5539),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 90.dp, end = 16.dp)
                ) {
                    Text(
                        text = "재배치",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            } else {
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

            if (!room.isCameraMode && !room.rearrangeMode && mailboxItemCount > 0) {
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

                    RearrangeCircleButton(
                        onClick = onRearrangeMoveDown
                    ) {
                        Text(
                            text = "↙",
                            color = Color.White
                        )
                    }
                }
            }

            if (room.isCameraMode) {
                CameraOverlay(
                    zoom = room.cameraZoom,
                    onZoomChange = onSetCameraZoom,
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
            onBottomNavItemClick = {},
            recentMainMenuAction = MainMenuAction.SHOP,
            onPlaceholderMessageConsumed = {},
            onSetCameraZoom = {},
            onCaptureClick = {},
            onExitCameraMode = {},
            currencyAmount = 100,
            purchasedShopItems = emptyList(),
            onPlaygroundClick = {},
            mailboxVisible = false,
            friendRequests = emptyList(),
            homeInvitations = emptyList(),
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
