package com.example.lupapj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun RoomTopHud(
    petStatus: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeText = rememberCurrentTimeText()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoomHudChip(
                iconResId = R.drawable.ic_hud_paw,
                text = "루파",
                modifier = Modifier.weight(1f)
            )

            RoomTimeChip(
                iconResId = R.drawable.ic_hud_clock,
                text = timeText,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            RoomHudButton(
                iconResId = R.drawable.ic_hud_settings,
                contentDescription = "설정",
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
        }

        RoomStatusChip(
            text = petStatus,
            modifier = Modifier.widthIn(max = 360.dp)
        )
    }
}

@Composable
private fun RoomHudChip(
    iconResId: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    WoodPanel(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun RoomTimeChip(
    iconResId: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    WoodPanel(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun RoomHudButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WoodIconButton(
        iconResId = iconResId,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        iconTint = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
private fun RoomStatusChip(
    text: String,
    modifier: Modifier = Modifier
) {
    WoodPanel(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun rememberCurrentTimeText(): String {
    var timeText by remember { mutableStateOf(currentTimeText()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            timeText = currentTimeText()
            delay(1_000L)
        }
    }

    return timeText
}

private val roomTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun currentTimeText(): String {
    return LocalTime.now().format(roomTimeFormatter)
}
