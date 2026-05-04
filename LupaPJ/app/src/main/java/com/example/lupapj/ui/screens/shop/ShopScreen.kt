package com.example.lupapj.ui.screens.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.model.label

// [추가됨(권)] 상점 목록을 보여주는 전체 화면 컴포저블
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    currencyAmount: Int,
    shopItems: List<ShopItem>,
    purchasedItemIds: List<String>,
    onItemClick: (ShopItem) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("상점") },
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
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shopItems) { item ->
                ShopItemCard(
                    item = item,
                    isPurchased = purchasedItemIds.contains(item.id),
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

// [추가됨(권)] 상점 목록의 개별 아이템을 표시하는 카드 컴포넌트
@Composable
fun ShopItemCard(
    item: ShopItem,
    isPurchased: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPurchased) Color.LightGray else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // [추가됨(권)] 임시 아이콘 (차후 thumbnailResId가 있다면 실제 이미지로 대체 가능)
            Text(
                text = "📦",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
            Text(text = item.category.label, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.weight(1f))
            if (isPurchased) {
                Text("보유중", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else {
                Text("💰 ${item.price}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
