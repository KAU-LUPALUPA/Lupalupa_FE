package com.example.lupapj.ui.screens.main

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.ui.components.InventorySheet
import com.example.lupapj.ui.components.RoomViewport
import com.example.lupapj.ui.preview.previewRoomUiState
import com.example.lupapj.ui.theme.LupaPJTheme

@Composable
fun RoomScreen(
    uiState: RoomUiState?,
    placeholderMessage: String?,
    onButtonAClick: () -> Unit,
    onButtonBClick: () -> Unit,
    onInventoryDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onRoomObjectClick: (RoomObjectType) -> Unit,
    onFloorTap: (FloorAnchor) -> Unit,
    onBottomNavItemClick: (BottomNavItem) -> Unit,
    recentMainMenuAction: MainMenuAction?,
    onPlaceholderMessageConsumed: () -> Unit, // [복구됨(권)] 실수로 삭제된 파라미터 복구
    onSetCameraZoom: (Float) -> Unit, // [추가됨]
    onCaptureClick: (Bitmap) -> Unit, // [추가됨]
    onExitCameraMode: () -> Unit, // [추가됨]
    currencyAmount: Int, // [추가됨(권)] 보유 중인 재화
    purchasedShopItems: List<com.example.lupapj.data.model.ShopItem>, // [추가됨(권)] 보유 중인 치장 아이템 정보
    onMinigameClick: () -> Unit // [추가됨(권)] 미니게임 진입 액션
) {
    val snackbarHostState = remember { SnackbarHostState() }
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
                    recentIconRes = recentMainMenuAction?.iconRes,
                    onRecentActionClick = onButtonAClick,
                    onInventoryClick = onButtonBClick,
                    onSettingClick = onSettingsClick,
                    onPopupMenuItemClick = onBottomNavItemClick,
                    onPlaygroundClick = onMinigameClick,
                    roomContent = {
                        RoomViewport(
                            uiState = room,
                            onRoomObjectClick = onRoomObjectClick,
                            onFloorTap = onFloorTap,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
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
    }

    // [추가됨(권)] 인벤토리 바텀시트. ModalBottomSheet가 자체 애니메이션을 갖고 있으므로 if 분기만 사용.
    if (room.inventoryVisible) {
        InventorySheet(
            purchasedShopItems = purchasedShopItems, // [추가됨(권)]
            onDismiss = onInventoryDismiss
        )
    }
}

private val MainMenuAction.iconRes: Int
    get() = when (this) {
        MainMenuAction.SCREENSHOT -> R.drawable.camera_trimmed
        MainMenuAction.GALLERY -> R.drawable.gallery_trimmed
        MainMenuAction.CONTACTS -> R.drawable.friends_trimmed
        MainMenuAction.SHOP -> R.drawable.shop_trimmed
        MainMenuAction.PLAYGROUND -> R.drawable.playground_trimmed
    }

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
            onFloorTap = {},
            onBottomNavItemClick = {},
            recentMainMenuAction = MainMenuAction.SHOP,
            onPlaceholderMessageConsumed = {},
            onSetCameraZoom = {},
            onCaptureClick = {},
            onExitCameraMode = {},
            currencyAmount = 100,
            purchasedShopItems = emptyList(),
            onMinigameClick = {}
        )
    }
}
