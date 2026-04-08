package com.example.lupapj.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "루파루파",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = loadingMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
