package com.example.lupapj.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomPoint
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.ui.components.BottomActionButtons
import com.example.lupapj.ui.components.BottomNavBar
import com.example.lupapj.ui.components.InventorySheet
import com.example.lupapj.ui.components.RoomViewport
import com.example.lupapj.ui.preview.previewRoomUiState
import com.example.lupapj.ui.theme.LupaPJTheme

@Composable
fun RoomScreen(
    uiState: RoomUiState?,
    placeholderMessage: String?,
    onButtonAClick: () -> Unit,
    onButtonBClick: () -> Unit,
    onInventoryDismiss: () -> Unit,
    onRoomObjectClick: (RoomObjectType) -> Unit,
    onFloorTap: (RoomPoint) -> Unit,
    onBottomNavItemClick: (BottomNavItem) -> Unit,
    onPlaceholderMessageConsumed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(placeholderMessage) {
        placeholderMessage?.let {
            snackbarHostState.showSnackbar(it)
            onPlaceholderMessageConsumed()
        }
    }

    val room = uiState ?: return

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "루파루파",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = room.statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RoomViewport(
                    uiState = room,
                    onRoomObjectClick = onRoomObjectClick,
                    onFloorTap = onFloorTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                AnimatedVisibility(visible = room.navBarVisible) {
                    BottomNavBar(onItemClick = onBottomNavItemClick)
                }

                BottomActionButtons(
                    onButtonAClick = onButtonAClick,
                    onButtonBClick = onButtonBClick,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }

    if (room.inventoryVisible) {
        InventorySheet(onDismiss = onInventoryDismiss)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomScreenPreview() {
    LupaPJTheme {
        RoomScreen(
            uiState = previewRoomUiState,
            placeholderMessage = null,
            onButtonAClick = {},
            onButtonBClick = {},
            onInventoryDismiss = {},
            onRoomObjectClick = {},
            onFloorTap = {},
            onBottomNavItemClick = {},
            onPlaceholderMessageConsumed = {}
        )
    }
}
