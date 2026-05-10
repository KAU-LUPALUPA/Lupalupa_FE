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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    onAcceptHomeInvitation: (String) -> Unit,
    onRejectHomeInvitation: (String) -> Unit
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
                    recentIconRes = recentMainMenuAction?.iconRes,
                    onRecentActionClick = onButtonAClick,
                    onInventoryClick = onButtonBClick,
                    onSettingClick = onSettingsClick,
                    onPopupMenuItemClick = onBottomNavItemClick,
                    onPlaygroundClick = onPlaygroundClick,
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

            if (!room.rearrangeMode) {

                Surface(
                    onClick = onRearrangeClick,
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFEADFD3),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 130.dp, end = 16.dp)
                ) {

                    Text(
                        text = "재배치",
                        color = Color(0xFF5C4033),
                        modifier = Modifier.padding(
                            horizontal = 18.dp,
                            vertical = 10.dp
                        )
                    )
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

            if (room.rearrangeMode && room.selectedRearrangeObjectType != null) {

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 85.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    RearrangeCircleButton(
                        onClick = onRearrangeMoveUp
                    ) {
                        Text(
                            text = "↑",
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RearrangeCircleButton(
                            onClick = onRearrangeMoveLeft
                        ) {
                            Text(
                                text = "←",
                                color = Color.White
                            )
                        }

                        Surface(
                            onClick = onRearrangeConfirm,
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFB08968),
                            shadowElevation = 6.dp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {

                            Text(
                                text = "✓",
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        RearrangeCircleButton(
                            onClick = onRearrangeMoveRight
                        ) {
                            Text(
                                text = "→",
                                color = Color.White
                            )
                        }
                    }

                    RearrangeCircleButton(
                        onClick = onRearrangeMoveDown
                    ) {
                        Text(
                            text = "↓",
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
            onRearrangeClick = {},
            onRearrangeMoveUp = {},
            onRearrangeMoveDown = {},
            onRearrangeMoveLeft = {},
            onRearrangeMoveRight = {},
            onRearrangeConfirm = {},
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
            onRejectHomeInvitation = {}
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
