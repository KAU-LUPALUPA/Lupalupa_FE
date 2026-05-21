package com.example.lupapj.ui.screens.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
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
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            containerColor = Color.Transparent,
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
            // [추가됨(권)] 보유 중인 아이템은 가장 아래로 내리는 정렬 로직 적용
            val sortedShopItems = remember(shopItems, purchasedItemIds) {
                val (purchased, unpurchased) = shopItems.partition { purchasedItemIds.contains(it.id) }
                unpurchased + purchased
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                color = Color(0xFFFFFBF0).copy(alpha = 0.75f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedShopItems) { item ->
                        ShopItemCard(
                            item = item,
                            isPurchased = purchasedItemIds.contains(item.id),
                            onClick = { onItemClick(item) }
                        )
                    }
                }
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
            containerColor = if (isPurchased) Color.LightGray else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // [수정됨(권)] 썸네일 리소스가 등록되어 있는 경우 이미지 렌더링, 없으면 📦 이모지 출력
            if (item.thumbnailResId != null) {
                Image(
                    painter = painterResource(id = item.thumbnailResId),
                    contentDescription = item.name,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Text(
                    text = "📦",
                    style = MaterialTheme.typography.displayMedium
                )
            }
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
