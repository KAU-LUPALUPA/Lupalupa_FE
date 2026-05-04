package com.example.lupapj.ui.screens.minigame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// [추가됨(권)] 미니게임 플레이 화면 컴포저블. 재화를 획득할 수 있는 수단 제공.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinigameScreen(
    currencyAmount: Int,
    feedbackMessage: String?,
    onEarnCurrencyClick: () -> Unit,
    onBackClick: () -> Unit,
    onFeedbackConsumed: () -> Unit
) {
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            snackbarHostState.showSnackbar(feedbackMessage)
            onFeedbackConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("미니게임") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                },
                actions = {
                    Text(
                        text = "💰 $currencyAmount",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎮",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "미니게임을 플레이하세요!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "실제 게임 로직이 들어갈 자리입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // [추가됨(권)] 게임 완료(성공) 보상 획득을 모사하는 버튼
            Button(
                onClick = onEarnCurrencyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("게임 완료 보상 얻기 (+50원)")
            }
        }
    }
}
