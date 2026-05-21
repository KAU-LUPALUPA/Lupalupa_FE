package com.example.lupapj.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.model.label // [추가됨(권)] ShopCategory.label 확장 프로퍼티 임포트

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.lupapj.R

// [수정됨(권)] 장착/해제 콜백 및 장착 상태를 전달받도록 매개변수 추가
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySheet(
    purchasedShopItems: List<ShopItem>,
    equippedItemIds: List<String>,
    onEquipClick: (String) -> Unit,
    onUnequipClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    // [추가됨(권)] 인벤토리 시트 내에서 선택된 아이템을 추적하는 상태
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }

    val filteredShopItems = remember(purchasedShopItems, searchQuery) {
        val keyword = searchQuery.trim()

        if (keyword.isBlank()) {
            purchasedShopItems
        } else {
            purchasedShopItems.filter { item ->
                item.name.contains(keyword, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp)) {
            Image(
                painter = painterResource(id = R.drawable.background_1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color(0xFFFFFBF0).copy(alpha = 0.75f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "내 인벤토리",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(text = "구매한 아이템 검색")
                        },
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (purchasedShopItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "아직 보유한 치장 아이템이 없습니다.\n상점에서 아이템을 구매해 보세요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (filteredShopItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "검색 결과가 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp) // [수정됨(권)] 하단 정보창 공간 확보를 위해 240dp로 축소
                        ) {
                            items(filteredShopItems) { item ->
                                val isEquipped = equippedItemIds.contains(item.id)
                                val isSelected = selectedItem?.id == item.id
                                InventoryItemCard(
                                    item = item,
                                    isEquipped = isEquipped,
                                    isSelected = isSelected,
                                    onClick = { selectedItem = item }
                                )
                            }
                        }
                    }

                    // [추가됨(권)] 선택된 아이템의 상세 내용 및 장착/해제 버튼 영역
                    selectedItem?.let { item ->
                        val isEquipped = equippedItemIds.contains(item.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (item.thumbnailResId != null) {
                                    Image(
                                        painter = painterResource(id = item.thumbnailResId),
                                        contentDescription = item.name,
                                        modifier = Modifier.size(48.dp)
                                    )
                                } else {
                                    Text("📦", style = MaterialTheme.typography.headlineSmall)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "분류: ${item.category.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                if (isEquipped) {
                                    Button(
                                        onClick = { onUnequipClick(item.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("해제")
                                    }
                                } else {
                                    Button(
                                        onClick = { onEquipClick(item.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("장착")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// [수정됨(권)] 카드 외관 장식 및 선택/장착 상태를 표시하는 개별 아이템 카드 컴포넌트
@Composable
private fun InventoryItemCard(
    item: ShopItem,
    isEquipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else if (isEquipped) {
                Color(0xFF4CAF50)
            } else {
                Color.LightGray.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (item.thumbnailResId != null) {
                    Image(
                        painter = painterResource(id = item.thumbnailResId),
                        contentDescription = item.name,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Text(
                        text = "📦",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            if (isEquipped) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 0.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "장착됨",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}