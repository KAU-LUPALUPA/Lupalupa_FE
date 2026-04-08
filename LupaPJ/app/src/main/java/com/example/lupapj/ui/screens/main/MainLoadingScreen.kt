package com.example.lupapj.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.lupapj.R
import com.example.lupapj.ui.components.AuthPopup
import com.example.lupapj.ui.preview.previewLoadingUiState
import com.example.lupapj.ui.theme.LupaPJTheme

@Composable
fun MainLoadingScreen(
    loadingMessage: String,
    authPopupVisible: Boolean,
    isProcessingLogin: Boolean,
    onKakaoLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 이전에 그렸던 빈 그라데이션 배경과 "루파루파" 텍스트를 완전히 제거하고
        // 스플래시 화면에서 쓰이던 배경(bg_loading)만 이어서 그려주어 자연스럽게 이어지게 합니다.
        Image(
            painter = painterResource(id = R.drawable.bg_loading),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (authPopupVisible) {
            AuthPopup(
                isProcessingLogin = isProcessingLogin,
                onKakaoLoginClick = onKakaoLoginClick
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainLoadingScreenPreview() {
    LupaPJTheme {
        MainLoadingScreen(
            loadingMessage = previewLoadingUiState.loadingMessage,
            authPopupVisible = previewLoadingUiState.authPopupVisible,
            isProcessingLogin = false,
            onKakaoLoginClick = {}
        )
    }
}
