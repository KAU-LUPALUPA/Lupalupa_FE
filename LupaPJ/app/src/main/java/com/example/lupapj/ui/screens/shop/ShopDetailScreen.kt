package com.example.lupapj.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.model.label

// [추가됨(권)] 상점 아이템의 세부 스펙을 보고 구매할 수 있는 상세 화면 컴포저블
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailScreen(
    item: ShopItem,
    currencyAmount: Int,
    isPurchased: Boolean,
    isPurchasing: Boolean,
    feedbackMessage: String?,
    onPurchaseClick: () -> Unit,
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
                title = { Text(item.name) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // [추가됨(권)] 치장 미리보기 영역. 현재는 실제 펫/아이템 에셋이 없으므로 빈 공간(회색 박스)로 대체.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🐶", style = MaterialTheme.typography.displayLarge)
                    Text(text = "[미리보기 화면]", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "장착: ${item.name} (${item.category.label})", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = item.name, style = MaterialTheme.typography.headlineMedium)
            Text(text = "분류: ${item.category.label}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isPurchased) {
                Button(
                    onClick = { /* 추후 장착 기능 연결 */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("장착하기")
                }
            } else {
                Button(
                    onClick = onPurchaseClick,
                    enabled = !isPurchasing && currencyAmount >= item.price,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("${item.price}원 구매하기")
                    }
                }
                if (currencyAmount < item.price) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("재화가 부족합니다.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
