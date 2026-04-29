package com.example.lupapj.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Picture
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.ui.components.BottomActionButtons
import com.example.lupapj.ui.components.BottomNavBar
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
    onRoomObjectClick: (RoomObjectType) -> Unit,
    onFloorTap: (FloorAnchor) -> Unit,
    onBottomNavItemClick: (BottomNavItem) -> Unit,
    onPlaceholderMessageConsumed: () -> Unit,
    onSetCameraZoom: (Float) -> Unit, // [추가됨]
    onCaptureClick: (Bitmap) -> Unit, // [추가됨]
    onExitCameraMode: () -> Unit // [추가됨]
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val picture = remember { Picture() } // [추가됨] 화면 캡처용 Picture

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
            RoomViewport(
                uiState = room,
                onRoomObjectClick = onRoomObjectClick,
                onFloorTap = onFloorTap,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val width = this.size.width.toInt()
                        val height = this.size.height.toInt()
                        onDrawWithContent {
                            val pictureCanvas = androidx.compose.ui.graphics.Canvas(picture.beginRecording(width, height))
                            draw(this, this.layoutDirection, pictureCanvas, this.size) {
                                this@onDrawWithContent.drawContent()
                            }
                            picture.endRecording()
                            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
            ) {
                if (!room.isCameraMode) {
                    AnimatedVisibility(
                        visible = room.navBarVisible,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 108.dp)
                    ) {
                        BottomNavBar(onItemClick = onBottomNavItemClick)
                    }

                    BottomActionButtons(
                        onButtonAClick = onButtonAClick,
                        onButtonBClick = onButtonBClick,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                    )
                }
            }
            
            // [추가됨] 스크린샷 카메라 오버레이
            if (room.isCameraMode) {
                CameraOverlay(
                    zoom = room.cameraZoom,
                    onZoomChange = onSetCameraZoom,
                    onCapture = {
                        val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        picture.draw(canvas)
                        
                        // [수정됨] CameraOverlay의 프레임 비율(85% x 65%)에 맞게 크롭
                        val frameWidth = (bitmap.width * 0.85f).toInt()
                        val frameHeight = (bitmap.height * 0.65f).toInt()
                        val x = (bitmap.width - frameWidth) / 2
                        val y = (bitmap.height - frameHeight) / 2
                        
                        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, frameWidth, frameHeight)
                        onCaptureClick(croppedBitmap)
                    },
                    onCancel = onExitCameraMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (room.inventoryVisible) {
        InventorySheet(onDismiss = onInventoryDismiss)
    }
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
            onRoomObjectClick = {},
            onFloorTap = {},
            onBottomNavItemClick = {},
            onPlaceholderMessageConsumed = {},
            onSetCameraZoom = {},
            onCaptureClick = {},
            onExitCameraMode = {}
        )
    }
}
