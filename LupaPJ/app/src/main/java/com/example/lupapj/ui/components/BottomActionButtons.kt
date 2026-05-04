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

@Composable
fun BottomActionButtons(
    onMenuClick: () -> Unit,
    onBagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WoodPanel(
        modifier = modifier.widthIn(max = 360.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BottomIconAction(
                iconResId = R.drawable.ic_hud_menu,
                contentDescription = "메뉴",
                onClick = onMenuClick,
                modifier = Modifier.weight(1f)
            )

            BottomIconAction(
                iconResId = R.drawable.ic_hud_bag,
                contentDescription = "가방",
                onClick = onBagClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomIconAction(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
