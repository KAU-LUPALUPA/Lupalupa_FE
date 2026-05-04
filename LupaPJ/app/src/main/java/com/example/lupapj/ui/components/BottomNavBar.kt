package com.example.lupapj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // [추가됨(권)] Text 컴포저블 임포트
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.label

@Composable
fun BottomNavBar(
    onItemClick: (BottomNavItem) -> Unit,
    onMinigameClick: () -> Unit, // [추가됨(권)] 미니게임 진입 액션
    modifier: Modifier = Modifier
) {
    WoodPanel(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem.entries.forEach { item ->
                    IconButton(onClick = { onItemClick(item) }) {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // [추가됨(권)] 두 번째 행: 미니게임 진입 버튼
            androidx.compose.material3.Button(
                onClick = onMinigameClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                androidx.compose.material3.Text("🎮 미니게임장 가기")
            }
        }
    }
}

private val BottomNavItem.iconResId: Int
    get() = when (this) {
        BottomNavItem.SHOP -> R.drawable.ic_nav_shop
        BottomNavItem.SCREENSHOT -> R.drawable.ic_nav_camera
        BottomNavItem.CONTACTS -> R.drawable.ic_nav_friends
        BottomNavItem.GALLERY -> R.drawable.ic_nav_gallery
    }
