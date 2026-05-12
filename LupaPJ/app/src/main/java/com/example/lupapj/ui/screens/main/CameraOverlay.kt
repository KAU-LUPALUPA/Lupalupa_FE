package com.example.lupapj.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lupapj.R
import kotlinx.coroutines.delay

internal const val CAMERA_FRAME_WIDTH_FRACTION = 0.85f
internal const val CAMERA_FRAME_HEIGHT_FRACTION = 0.65f

@Composable
fun CameraOverlay(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onPan: (dx: Float, dy: Float) -> Unit,
    onCapture: () -> Unit,
    onCancel: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 핀치-투-줌 + 두 손가락 팬 제스처 상태
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        onZoomChange(zoom * zoomChange)
        // 두 손가락 이동 시 화면 패닝
        if (zoomChange != 1f || panChange.x != 0f || panChange.y != 0f) {
            onPan(panChange.x, panChange.y)
        }
    }

    var showFlash by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 제스처 레이어 (배경에서 줌/팬 처리)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        onPan(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // 2. 외곽 프레임 및 가이드라인 레이어
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // BlendMode.Clear를 작동시키기 위해 오프스크린 레이어 필요
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawBehind {
                    val frameWidth = size.width * CAMERA_FRAME_WIDTH_FRACTION
                    val frameHeight = size.height * CAMERA_FRAME_HEIGHT_FRACTION
                    val topLeftX = (size.width - frameWidth) / 2
                    val topLeftY = (size.height - frameHeight) / 2

                    // [복구] 어두운 외부 영역 (아래 레이어의 블러와 합쳐져 어두운 블러 효과가 됨)
                    drawRect(Color.Black.copy(alpha = 0.5f))

                    // [복구] 프레임 구멍 뚫기
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(topLeftX, topLeftY),
                        size = Size(frameWidth, frameHeight),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )

                    // 흰색 프레임 외곽선
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(topLeftX, topLeftY),
                        size = Size(frameWidth, frameHeight),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 3×3 그리드 격자
                    val gridLineColor = Color.White.copy(alpha = 0.4f)
                    val gridStroke = Stroke(width = 1.dp.toPx())
                    for (i in 1..2) {
                        val x = topLeftX + frameWidth * i / 3f
                        drawLine(gridLineColor, Offset(x, topLeftY), Offset(x, topLeftY + frameHeight), gridStroke.width)
                    }
                    for (i in 1..2) {
                        val y = topLeftY + frameHeight * i / 3f
                        drawLine(gridLineColor, Offset(topLeftX, y), Offset(topLeftX + frameWidth, y), gridStroke.width)
                    }
                }
        )

        // 3. UI 요소 (로고, 안내문, 버튼) - 제스처 방해받지 않도록 최상단 배치
        // 로고 및 안내문
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val hPad = maxWidth * (1f - CAMERA_FRAME_WIDTH_FRACTION) / 2f + 12.dp
            val vPad = maxHeight * (1f - CAMERA_FRAME_HEIGHT_FRACTION) / 2f + 10.dp

            // 안내 문구
            Text(
                text = "두 손가락: 확대/축소 • 한 손가락: 화면 이동",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_logo_loopaloopa),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = hPad, bottom = vPad)
                    .size(64.dp)
                    .graphicsLayer { alpha = 0.8f }
            )
        }

        // 하단 컨트롤 버튼 영역 — 갤러리(좌) / 촬영(중앙) / 취소(우)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 52.dp, start = 40.dp, end = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 갤러리 버튼 (좌)
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.DarkGray.copy(alpha = 0.7f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nav_gallery),
                    contentDescription = "갤러리",
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.White), // [수정됨] 검정 아이콘 → 흰색 표시
                    modifier = Modifier.size(28.dp)
                )
            }

            // 촬영 버튼 (중앙)
            IconButton(
                onClick = {
                    showFlash = true
                    onCapture()
                },
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
                    .border(4.dp, Color.LightGray, CircleShape)
            ) {
                // 내부에 작은 원을 하나 더 그려서 카메라 버튼처럼 연출
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, Color.Black, CircleShape)
                )
            }

            // 취소 버튼 (우)
            IconButton(
                onClick = {
                    if (!showFlash) onCancel() 
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.DarkGray.copy(alpha = 0.7f), CircleShape)
            ) {
                Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 시스템 뒤로가기 버튼 클릭 시에도 카메라 모드 종료
        androidx.activity.compose.BackHandler(enabled = true) {
            onCancel()
        }

        // 찰칵! 플래시 및 페이드아웃 효과
        if (showFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White) // 순간적으로 화면 하얗게
            )

            LaunchedEffect(Unit) {
                delay(150)
                showFlash = false
                // onCancel() 호출 제거: 촬영 후 스크린샷 모드 유지 [수정됨 - 권순범]
            }
        }
    }
}
