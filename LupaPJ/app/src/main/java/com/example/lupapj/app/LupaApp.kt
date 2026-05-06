package com.example.lupapj.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.ui.screens.friends.FriendRoomScreen
import com.example.lupapj.ui.screens.friends.FriendScreen
import com.example.lupapj.ui.screens.gallery.GalleryScreen
import com.example.lupapj.ui.screens.main.MainLoadingScreen
import com.example.lupapj.ui.screens.main.RoomScreen
import com.example.lupapj.ui.screens.minigame.MinigameScreen
import com.example.lupapj.ui.screens.shop.ShopDetailScreen
import com.example.lupapj.ui.screens.shop.ShopScreen
import com.example.lupapj.viewmodel.AppViewModel
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect

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
            currencyRepository = container.currencyRepository,
            shopRepository = container.shopRepository
        )
    )

    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

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
                onSettingsClick = appViewModel::onSettingsClick,
                onRoomObjectClick = appViewModel::onRoomObjectClick,

                onRearrangeClick = appViewModel::onRearrangeClick,
                onRearrangeMoveUp = appViewModel::onRearrangeMoveUp,
                onRearrangeMoveDown = appViewModel::onRearrangeMoveDown,
                onRearrangeMoveLeft = appViewModel::onRearrangeMoveLeft,
                onRearrangeMoveRight = appViewModel::onRearrangeMoveRight,
                onRearrangeConfirm = appViewModel::onRearrangeConfirm,

                onFloorTap = appViewModel::onFloorTap,
                onBottomNavItemClick = appViewModel::onBottomNavItemClick,
                recentMainMenuAction = uiState.recentMainMenuAction,
                onPlaceholderMessageConsumed = appViewModel::onPlaceholderMessageConsumed,
                onSetCameraZoom = appViewModel::setCameraZoom,
                onCaptureClick = appViewModel::captureScreen,
                onExitCameraMode = appViewModel::exitCameraMode,
                currencyAmount = uiState.currencyAmount,
                purchasedShopItems = uiState.shopItems.filter {
                    uiState.purchasedItemIds.contains(it.id)
                },
                onMinigameClick = appViewModel::openMinigame
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
                friendCodeInput = uiState.friendCodeInput,
                isSendingFriendRequest = uiState.isSendingFriendRequest,
                feedbackMessage = uiState.friendFeedbackMessage,
                onFriendCodeChange = appViewModel::onFriendCodeChange,
                onSendFriendRequest = appViewModel::sendFriendRequest,
                onAcceptRequest = appViewModel::acceptFriendRequest,
                onRejectRequest = appViewModel::rejectFriendRequest,
                onCancelRequest = appViewModel::cancelFriendRequest,
                onVisitFriend = appViewModel::visitFriendHome,
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

        AppPhase.SHOP -> {
            ShopScreen(
                currencyAmount = uiState.currencyAmount,
                shopItems = uiState.shopItems,
                purchasedItemIds = uiState.purchasedItemIds,
                onItemClick = appViewModel::selectShopItem,
                onBackClick = appViewModel::exitShop
            )
        }

        AppPhase.SHOP_DETAIL -> {
            val selectedItem = uiState.selectedShopItem

            if (selectedItem != null) {
                ShopDetailScreen(
                    item = selectedItem,
                    currencyAmount = uiState.currencyAmount,
                    isPurchased = uiState.purchasedItemIds.contains(selectedItem.id),
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
                currencyAmount = uiState.currencyAmount,
                feedbackMessage = uiState.shopFeedbackMessage,
                onEarnCurrencyClick = appViewModel::earnCurrencyFromMinigame,
                onBackClick = appViewModel::exitMinigame,
                onFeedbackConsumed = appViewModel::consumeShopFeedback
            )
        }
    }
}