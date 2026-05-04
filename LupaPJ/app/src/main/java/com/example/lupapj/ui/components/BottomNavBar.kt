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
    modifier: Modifier = Modifier
) {
    WoodPanel(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
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
    }
}

private val BottomNavItem.iconResId: Int
    get() = when (this) {
        BottomNavItem.SHOP -> R.drawable.ic_nav_shop
        BottomNavItem.SCREENSHOT -> R.drawable.ic_nav_camera
        BottomNavItem.CONTACTS -> R.drawable.ic_nav_friends
        BottomNavItem.GALLERY -> R.drawable.ic_nav_gallery
    }
