package com.example.lupapj.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.lupapj.R
import com.example.lupapj.data.model.GalleryImage // [추가됨]
import com.example.lupapj.ui.components.AuthPopup
import com.example.lupapj.ui.preview.previewLoadingUiState
import com.example.lupapj.ui.theme.LupaPJTheme

@Composable
fun MainLoadingScreen(
    promptText: String,
    showGalleryFrame: Boolean,
    isPromptReady: Boolean,
    isLoginMode: Boolean,
    isProcessingLogin: Boolean = false,
    galleryImages: List<GalleryImage> = emptyList(), // [추가됨] 로딩 화면용 이미지 목록
    isDevLoginEnabled: Boolean = false,
    onKakaoLoginClick: () -> Unit,
    onDevLoginClick: () -> Unit = {},
    onStartClick: () -> Unit = {}
) {
    // 팝업 노출 상태는 최상위 레벨에서 remember 선언
    var showLoginPopup by remember { mutableStateOf(false) }

    // 1. 랜덤 이미지 선정 (즐겨찾기 우선 -> 전체 중 랜덤 -> 없으면 null)
    val displayImage = remember(galleryImages) {
        val favorites = galleryImages.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            favorites.random()
        } else if (galleryImages.isNotEmpty()) {
            galleryImages.random()
        } else {
            null
        }
    }

    // 2. 선택된 이미지가 있다면 백그라운드에서 디코딩
    val displayBitmap by produceState<ImageBitmap?>(initialValue = null, displayImage) {
        if (displayImage != null) {
            value = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(displayImage.filePath)?.asImageBitmap()
            }
        } else {
            value = null
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 항상 기본 로딩 배경을 가장 아래에 띄움
        Image(
            painter = painterResource(id = R.drawable.bg_loading),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 귀여운 액자 프레임 UI는 사진이 있든 없든 항상 노출
        if (showGalleryFrame) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f) // 화면 가로의 60% 크기로 살짝 줄임
                    .aspectRatio(0.75f) 
                    .align(Alignment.Center)
                    .offset(y = (-80).dp) // 위로 올림
                    .rotate(-10f) // 왼쪽으로 10도 기울임
                    // TODO: 나중에 여기에 귀여운 액자 테두리 이미지를 추가하세요. (예: R.drawable.cute_frame)
                    // .paint(painterResource(id = R.drawable.cute_frame), contentScale = ContentScale.FillBounds)
                    .background(Color.White, shape = RoundedCornerShape(12.dp)) 
                    .padding(8.dp) 
            ) {
                if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap!!,
                    contentDescription = "Captured Screenshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                // 갤러리에 사진이 없을 때(첫 다운로드 등) 기본으로 들어갈 이미지
                // 나중에 R.drawable.xxx 와 같이 진짜 이미지 리소스로 교체 시 Image() 컴포저블을 사용하세요.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                )
            }
        }
    }

        // 로딩바가 끝나고 팝업이 뜰 타이밍에 팝업 대신 화면 전체 터치 대기 상태로 전환
        if (isPromptReady) {
            if (!showLoginPopup) {
                val infiniteTransition = rememberInfiniteTransition(label = "blink")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // 클릭 이펙트 제거
                            onClick = { 
                                if (isLoginMode) {
                                    showLoginPopup = true 
                                } else {
                                    onStartClick()
                                }
                            } 
                        )
                ) {
                    Text(
                        text = promptText,
                        color = Color.White.copy(alpha = alpha),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp) // 기존 로딩바 위치 부근
                    )
                }
            } else if (isLoginMode) {
                // 기존 로그인 팝업 렌더링
                AuthPopup(
                    isProcessingLogin = isProcessingLogin,
                    isDevLoginEnabled = isDevLoginEnabled,
                    onKakaoLoginClick = onKakaoLoginClick,
                    onDevLoginClick = onDevLoginClick
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainLoadingScreenPreview() {
    LupaPJTheme {
        MainLoadingScreen(
            promptText = "로그인하시려면 화면을 터치하세요",
            showGalleryFrame = false,
            isPromptReady = previewLoadingUiState.authPopupVisible,
            isLoginMode = true,
            isProcessingLogin = false,
            galleryImages = emptyList(), // [추가됨]
            isDevLoginEnabled = true,
            onKakaoLoginClick = {},
            onDevLoginClick = {}
        )
    }
}
