package com.example.lupapj.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AuthPopup(
    isProcessingLogin: Boolean,
    onKakaoLoginClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "로그인 / 회원가입",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "현재 데모에서는 카카오 로그인만 동작합니다.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onKakaoLoginClick,
                enabled = !isProcessingLogin
            ) {
                Text(if (isProcessingLogin) "로그인 중..." else "카카오 로그인")
            }
        }
    )
}
