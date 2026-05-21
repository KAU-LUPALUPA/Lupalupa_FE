package com.example.lupapj.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember // [추가됨(권)] remember 임포트
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.model.label

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.lupapj.R
import com.example.lupapj.ui.components.AnimatedCharacterSprite // [추가됨(권)] 펫 스프라이트 컴포넌트 임포트

// [수정됨(권)] 펫 상태 정보 및 장착/해제 기능 연결을 위한 매개변수 추가
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailScreen(
    item: ShopItem,
    currencyAmount: Int,
    isPurchased: Boolean,
    isPurchasing: Boolean,
    feedbackMessage: String?,
    equippedItemIds: List<String>,
    petAppearance: com.example.lupapj.data.model.PetAppearance,
    isEgg: Boolean,
    allShopItems: List<ShopItem>,
    onPurchaseClick: () -> Unit,
    onEquipClick: (String) -> Unit,
    onUnequipClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onFeedbackConsumed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            snackbarHostState.showSnackbar(feedbackMessage)
            onFeedbackConsumed()
        }
    }

    // [추가됨(권)] 미리보기 목록 연산 (현재 고른 아이템을 착용한 것으로 가정하되, 중복되는 카테고리 충돌 처리)
    val previewItemIds = remember(equippedItemIds, item, allShopItems) {
        val otherCategoryItems = equippedItemIds.filter { equippedId ->
            val eqItem = allShopItems.find { it.id == equippedId } ?: return@filter false
            val eqCategory = eqItem.category
            
            when (item.category) {
                com.example.lupapj.data.model.ShopCategory.FULL_BODY -> {
                    eqCategory != com.example.lupapj.data.model.ShopCategory.TOP &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.BOTTOM &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.FULL_BODY
                }
                com.example.lupapj.data.model.ShopCategory.TOP -> {
                    eqCategory != com.example.lupapj.data.model.ShopCategory.FULL_BODY &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.TOP
                }
                com.example.lupapj.data.model.ShopCategory.BOTTOM -> {
                    eqCategory != com.example.lupapj.data.model.ShopCategory.FULL_BODY &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.BOTTOM
                }
                else -> {
                    eqCategory != item.category
                }
            }
        }
        otherCategoryItems + item.id
    }

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
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                color = Color(0xFFFFFBF0).copy(alpha = 0.75f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // [수정됨(권)] 펫 프리뷰 영역에 AnimatedCharacterSprite 배치 및 미리보기 장착 상태 적용
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp) // [수정됨(권)] 정보카드 노출을 위해 영역 소폭 축소
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE0E0E0).copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedCharacterSprite(
                            modifier = Modifier.size(150.dp),
                            appearance = petAppearance,
                            equippedItemIds = previewItemIds,
                            allShopItems = allShopItems, // [수정됨(권)] 전역 Mock 데이터 대신 상위에서 내려받은 실시간 상점 데이터 주입
                            isEgg = isEgg,
                            isPlaying = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // [추가됨(권)] 미리보기 장착 정보 카드 (Preview Equip Summary Card)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.8f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✨ 미리보기 장착 정보 카드",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            val categories = com.example.lupapj.data.model.ShopCategory.values()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEach { cat ->
                                    val activePreviewItem = allShopItems.find { 
                                        previewItemIds.contains(it.id) && it.category == cat 
                                    }
                                    val isCurrentSelected = activePreviewItem?.id == item.id
                                    
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrentSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else if (activePreviewItem != null) {
                                            Color(0xFFE8F5E9)
                                        } else {
                                            Color.LightGray.copy(alpha = 0.2f)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = cat.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                                            )
                                            Text(
                                                text = activePreviewItem?.name?.take(4) ?: "없음",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else if (activePreviewItem != null) Color(0xFF2E7D32) else Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = item.name, style = MaterialTheme.typography.headlineMedium)
                    Text(text = "분류: ${item.category.label}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // [수정됨(권)] 보유 중인 경우 "장착하기/해제하기"로 뷰모델과 연결하여 실시간 변경 가능하게 처리
                    if (isPurchased) {
                        val isCurrentlyEquipped = equippedItemIds.contains(item.id)
                        if (isCurrentlyEquipped) {
                            Button(
                                onClick = { onUnequipClick(item.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("해제하기")
                            }
                        } else {
                            Button(
                                onClick = { onEquipClick(item.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("장착하기")
                            }
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
    }
}
