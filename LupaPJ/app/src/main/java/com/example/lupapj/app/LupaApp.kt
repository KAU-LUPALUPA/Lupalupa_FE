package com.example.lupapj.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.ui.screens.main.MainLoadingScreen
import com.example.lupapj.ui.screens.main.RoomScreen
import com.example.lupapj.viewmodel.AppViewModel

@Composable
fun LupaApp() {
    val container = remember { AppContainer() }
    val appViewModel: AppViewModel = viewModel(
        factory = AppViewModel.Factory(
            authRepository = container.authRepository,
            roomRepository = container.roomRepository
        )
    )
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.phase) {
        AppPhase.MAIN_LOADING -> {
            MainLoadingScreen(
                loadingMessage = uiState.loadingMessage,
                authPopupVisible = uiState.authPopupVisible,
                isProcessingLogin = uiState.isProcessingLogin,
                onKakaoLoginClick = appViewModel::onKakaoLoginClick
            )
        }

        AppPhase.ROOM -> {
            RoomScreen(
                uiState = uiState.room,
                placeholderMessage = uiState.placeholderMessage,
                onButtonAClick = appViewModel::onButtonAClick,
                onButtonBClick = appViewModel::onButtonBClick,
                onInventoryDismiss = appViewModel::onInventoryDismiss,
                onRoomObjectClick = appViewModel::onRoomObjectClick,
                onFloorTap = appViewModel::onFloorTap,
                onBottomNavItemClick = appViewModel::onBottomNavItemClick,
                onPlaceholderMessageConsumed = appViewModel::onPlaceholderMessageConsumed
            )
        }
    }
}
