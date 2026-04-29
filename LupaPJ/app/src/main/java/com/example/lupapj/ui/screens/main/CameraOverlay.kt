package com.example.lupapj.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CameraOverlay(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onCapture: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberTransformableState { zoomChange, _, _ ->
        onZoomChange(zoom * zoomChange)
    }

    var showFlash by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // BlendMode.Clear를 위한 Offscreen 버퍼 설정
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawBehind {
                val frameWidth = size.width * 0.85f
                val frameHeight = size.height * 0.65f
                val topLeftX = (size.width - frameWidth) / 2
                val topLeftY = (size.height - frameHeight) / 2
                
                // 1. 어두운 외부 영역 그리기
                drawRect(Color.Black.copy(alpha = 0.6f))
                
                // 2. 투명한 액자 구멍 뚫기 (화면 캡처 영역)
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(frameWidth, frameHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                
                // 3. 흰색 프레임 외곽선 그리기
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(frameWidth, frameHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            .transformable(state = state)
    ) {
        // 상단 안내 문구
        Text(
            text = "두 손가락으로 화면을 확대/축소하세요.",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )

        // 하단 컨트롤 버튼 영역
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            // 촬영 버튼
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
                Box(modifier = Modifier.size(56.dp).background(Color.White, CircleShape).border(2.dp, Color.Black, CircleShape))
            }

            // 취소 버튼
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(start = 140.dp) // 촬영 버튼과 간격
                    .background(Color.DarkGray.copy(alpha = 0.7f), CircleShape)
            ) {
                Text("X", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
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
