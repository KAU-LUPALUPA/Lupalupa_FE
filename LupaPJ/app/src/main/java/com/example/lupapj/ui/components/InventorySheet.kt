package com.example.lupapj.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.ShopItem // [추가됨(권)]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySheet(
    purchasedShopItems: List<ShopItem>, // [추가됨(권)] 보유 중인 치장 아이템 목록을 넘겨받음
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .heightIn(min = 300.dp), // [추가됨(권)] 최소 높이 지정
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "내 인벤토리", // [수정됨(권)]
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // [추가됨(권)] 치장 아이템이 없을 때의 안내 문구
            if (purchasedShopItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 보유한 치장 아이템이 없습니다.\n상점에서 아이템을 구매해 보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // [추가됨(권)] 치장 아이템을 그리드 형태로 렌더링
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // 한 줄에 3개씩 배치
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(purchasedShopItems) { item ->
                        InventoryItemCard(item)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// [추가됨(권)] 인벤토리 내부 개별 아이템 카드
@Composable
private fun InventoryItemCard(item: ShopItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.aspectRatio(1f) // 정사각형 비율
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📦", // 임시 아이콘
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
