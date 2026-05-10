package com.example.lupapj.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lupapj.BuildConfig
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.ui.screens.friends.FriendRoomScreen
import com.example.lupapj.ui.screens.friends.FriendScreen
import com.example.lupapj.ui.screens.gallery.GalleryScreen
import com.example.lupapj.ui.screens.main.MainLoadingScreen
import com.example.lupapj.ui.screens.main.RoomScreen
import com.example.lupapj.ui.screens.minigame.MinigameScreen
import com.example.lupapj.ui.screens.plaza.PlazaScreen
import com.example.lupapj.ui.screens.shop.ShopDetailScreen
import com.example.lupapj.ui.screens.shop.ShopScreen
import com.example.lupapj.viewmodel.AppViewModel

@Composable
fun LupaApp(deepLink: Uri? = null) {
    val context = LocalContext.current
    val container = remember { AppContainer(context) }

    val appViewModel: AppViewModel = viewModel(
        factory = AppViewModel.Factory(
            authRepository = container.authRepository,
            roomRepository = container.roomRepository,
            galleryRepository = container.galleryRepository,
            friendRepository = container.friendRepository,
            plazaRepository = container.plazaRepository,
            currencyRepository = container.currencyRepository,
            shopRepository = container.shopRepository
        )
    )

    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    // [추가됨] 딥링크를 통한 카카오 로그인 처리 로직
    LaunchedEffect(deepLink) {
        val token = deepLink?.getQueryParameter("accessToken")
        val nickname = deepLink?.getQueryParameter("nickname")

        if (!token.isNullOrBlank()) {
            appViewModel.onKakaoLoginSuccess(
                accessToken = token,
                nickname = nickname
            )
        }
    }

    when (uiState.phase) {
        AppPhase.MAIN_LOADING -> {
            MainLoadingScreen(
                loadingMessage = uiState.loadingMessage,
                authPopupVisible = uiState.authPopupVisible,
                isProcessingLogin = uiState.isProcessingLogin,
                galleryImages = uiState.galleryImages,
                isDevLoginEnabled = BuildConfig.DEBUG,
                onKakaoLoginClick = appViewModel::onKakaoLoginClick,
                onDevLoginClick = appViewModel::onDevLoginClick
            )
        }

        AppPhase.ROOM -> {
            RoomScreen(
                uiState = uiState.room,
                placeholderMessage = uiState.placeholderMessage,
                onButtonAClick = appViewModel::onButtonAClick,
                onButtonBClick = appViewModel::onButtonBClick,
                onInventoryDismiss = appViewModel::onInventoryDismiss,
                onSettingsClick = appViewModel::onSettingsClick,
                onRoomObjectClick = appViewModel::onRoomObjectClick,

                // [sub_branch에서 가져온 가구 재배치 로직]
                onRearrangeClick = appViewModel::onRearrangeClick,
                onRearrangeMoveUp = appViewModel::onRearrangeMoveUp,
                onRearrangeMoveDown = appViewModel::onRearrangeMoveDown,
                onRearrangeMoveLeft = appViewModel::onRearrangeMoveLeft,
                onRearrangeMoveRight = appViewModel::onRearrangeMoveRight,
                onRearrangeConfirm = appViewModel::onRearrangeConfirm,
                onRearrangeCancel = appViewModel::onRearrangeCancel,

                onFloorTap = appViewModel::onFloorTap,
                onBottomNavItemClick = appViewModel::onBottomNavItemClick,
                recentMainMenuAction = uiState.recentMainMenuAction,
                onPlaceholderMessageConsumed = appViewModel::onPlaceholderMessageConsumed,
                onSetCameraZoom = appViewModel::setCameraZoom,
                onCaptureClick = appViewModel::captureScreen,
                onExitCameraMode = appViewModel::exitCameraMode,
                currencyAmount = uiState.currencyAmount.toInt(), // Long -> Int 변환
                purchasedShopItems = uiState.shopItems.filter { item ->
                    uiState.purchasedItems.any { it.masterId == item.id }
                },
                onPlaygroundClick = appViewModel::openPlaza,
                mailboxVisible = uiState.mailboxVisible,
                friendRequests = uiState.receivedFriendRequests,
                homeInvitations = uiState.receivedHomeInvitations,
                onMailboxClick = appViewModel::openMailbox,
                onMailboxDismiss = appViewModel::closeMailbox,
                onAcceptFriendRequest = appViewModel::acceptFriendRequest,
                onRejectFriendRequest = appViewModel::rejectFriendRequest,
                onAcceptHomeInvitation = appViewModel::acceptHomeInvitation,
                onRejectHomeInvitation = appViewModel::rejectHomeInvitation,
                behaviorDebugInfo = uiState.behaviorDebugInfo,
                onToggleBehaviorDebugClick = appViewModel::toggleBehaviorDebugWindow
            )
        }

        AppPhase.GALLERY -> {
            GalleryScreen(
                images = uiState.galleryImages,
                onBackClick = appViewModel::exitGallery,
                onFavoriteToggle = appViewModel::toggleFavorite,
                onDeleteClick = appViewModel::deleteImage
            )
        }

        AppPhase.FRIENDS -> {
            FriendScreen(
                myProfile = uiState.myFriendProfile,
                friends = uiState.friends,
                receivedRequests = uiState.receivedFriendRequests,
                sentRequests = uiState.sentFriendRequests,
                receivedHomeInvitations = uiState.receivedHomeInvitations,
                friendCodeInput = uiState.friendCodeInput,
                isSendingFriendRequest = uiState.isSendingFriendRequest,
                feedbackMessage = uiState.friendFeedbackMessage,
                onFriendCodeChange = appViewModel::onFriendCodeChange,
                onSendFriendRequest = appViewModel::sendFriendRequest,
                onAcceptRequest = appViewModel::acceptFriendRequest,
                onRejectRequest = appViewModel::rejectFriendRequest,
                onCancelRequest = appViewModel::cancelFriendRequest,
                onAcceptHomeInvitation = appViewModel::acceptHomeInvitation,
                onRejectHomeInvitation = appViewModel::rejectHomeInvitation,
                onRemoveFriend = appViewModel::removeFriend,
                onBackClick = appViewModel::exitFriends,
                onFeedbackConsumed = appViewModel::onFriendFeedbackConsumed
            )
        }

        AppPhase.FRIEND_ROOM -> {
            FriendRoomScreen(
                friendHome = uiState.visitingFriendHome,
                isLoading = uiState.isLoadingFriendHome,
                messages = uiState.friendRoomMessages,
                messageInput = uiState.friendMessageInput,
                isSendingMessage = uiState.isSendingFriendMessage,
                onMessageInputChange = appViewModel::onFriendMessageChange,
                onSendMessage = appViewModel::sendFriendMessage,
                onBackToFriendsClick = appViewModel::backToFriendsFromFriendRoom,
                onReturnHomeClick = appViewModel::returnHomeFromFriendRoom
            )
        }

        AppPhase.PLAZA -> {
            PlazaScreen(
                activePlaza = uiState.activePlaza,
                codeInput = uiState.plazaCodeInput,
                messageInput = uiState.plazaMessageInput,
                isJoining = uiState.isJoiningPlaza,
                isSendingMessage = uiState.isSendingPlazaMessage,
                feedbackMessage = uiState.plazaFeedbackMessage,
                onRandomJoin = appViewModel::joinRandomPlaza,
                onCodeInputChange = appViewModel::onPlazaCodeChange,
                onCodeJoin = appViewModel::joinPlazaByCode,
                onMessageInputChange = appViewModel::onPlazaMessageChange,
                onSendMessage = appViewModel::sendPlazaMessage,
                onLeavePlaza = appViewModel::leaveCurrentPlaza,
                onBackHome = appViewModel::returnHomeFromPlaza,
                onFeedbackConsumed = appViewModel::onPlazaFeedbackConsumed
            )
        }

        AppPhase.SHOP -> {
            ShopScreen(
                currencyAmount = uiState.currencyAmount.toInt(), // Long -> Int 변환
                shopItems = uiState.shopItems,
                purchasedItemIds = uiState.purchasedItems.map { it.masterId },
                onItemClick = appViewModel::selectShopItem,
                onBackClick = appViewModel::exitShop
            )
        }

        AppPhase.SHOP_DETAIL -> {
            val selectedItem = uiState.selectedShopItem

            if (selectedItem != null) {
                ShopDetailScreen(
                    item = selectedItem,
                    currencyAmount = uiState.currencyAmount.toInt(), // Long -> Int 변환
                    isPurchased = uiState.purchasedItems.any { it.masterId == selectedItem.id },
                    isPurchasing = uiState.isPurchasing,
                    feedbackMessage = uiState.shopFeedbackMessage,
                    onPurchaseClick = { appViewModel.purchaseItem(selectedItem.id) },
                    onBackClick = appViewModel::exitShopDetail,
                    onFeedbackConsumed = appViewModel::consumeShopFeedback
                )
            } else {
                appViewModel.exitShopDetail()
            }
        }

        AppPhase.MINIGAME -> {
            MinigameScreen(
                currencyAmount = uiState.currencyAmount.toInt(), // Long -> Int 변환
                feedbackMessage = uiState.shopFeedbackMessage,
                onEarnCurrencyClick = appViewModel::earnCurrencyFromMinigame,
                onBackClick = appViewModel::exitMinigame,
                onFeedbackConsumed = appViewModel::consumeShopFeedback
            )
        }
    }
}
