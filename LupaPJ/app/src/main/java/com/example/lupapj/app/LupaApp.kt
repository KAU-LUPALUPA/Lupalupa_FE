package com.example.lupapj.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext // [추가됨]
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.ui.screens.main.MainLoadingScreen
import com.example.lupapj.ui.screens.main.RoomScreen
import com.example.lupapj.ui.screens.gallery.GalleryScreen // [추가됨]
import com.example.lupapj.ui.screens.friends.FriendRoomScreen
import com.example.lupapj.ui.screens.friends.FriendScreen
import com.example.lupapj.ui.screens.minigame.MinigameScreen // [추가됨(권)] 미니게임 화면 임포트
import com.example.lupapj.ui.screens.shop.ShopScreen // [추가됨(권)] 상점 메인 화면 UI
import com.example.lupapj.ui.screens.shop.ShopDetailScreen // [추가됨(권)] 상점 상세 화면 UI
import com.example.lupapj.viewmodel.AppViewModel

@Composable
fun LupaApp() {
    val context = LocalContext.current // [추가됨]
    val container = remember { AppContainer(context) } // [수정됨] context 주입
    val appViewModel: AppViewModel = viewModel(
        factory = AppViewModel.Factory(
            authRepository = container.authRepository,
            roomRepository = container.roomRepository,
            galleryRepository = container.galleryRepository, // [추가됨] 갤러리 리포지토리 주입
            friendRepository = container.friendRepository,
            currencyRepository = container.currencyRepository, // [추가됨(권)] 재화 리포지토리 주입
            shopRepository = container.shopRepository // [추가됨(권)] 상점 리포지토리 주입
        )
    )
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.phase) {
        AppPhase.MAIN_LOADING -> {
            MainLoadingScreen(
                loadingMessage = uiState.loadingMessage,
                authPopupVisible = uiState.authPopupVisible,
                isProcessingLogin = uiState.isProcessingLogin,
                galleryImages = uiState.galleryImages, // [추가됨] 로딩 화면 전시용 이미지 전달
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
                onFloorTap = appViewModel::onFloorTap,
                onBottomNavItemClick = appViewModel::onBottomNavItemClick,
                onPlaceholderMessageConsumed = appViewModel::onPlaceholderMessageConsumed,
                onSetCameraZoom = appViewModel::setCameraZoom, // [추가됨]
                onCaptureClick = appViewModel::captureScreen, // [추가됨]
                onExitCameraMode = appViewModel::exitCameraMode, // [추가됨]
                currencyAmount = uiState.currencyAmount, // [추가됨(권)]
                purchasedShopItems = uiState.shopItems.filter { uiState.purchasedItemIds.contains(it.id) }, // [추가됨(권)] 인벤토리용 아이템 목록
                onMinigameClick = appViewModel::openMinigame // [추가됨(권)] 미니게임 화면 진입으로 변경
            )
        }
        
        AppPhase.GALLERY -> { // [추가됨] 갤러리 화면 라우팅
            GalleryScreen(
                images = uiState.galleryImages,
                onBackClick = appViewModel::exitGallery,
                onFavoriteToggle = appViewModel::toggleFavorite,
                onDeleteClick = appViewModel::deleteImage // [추가됨]
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
        
        AppPhase.SHOP -> { // [추가됨(권)] 상점 목록 화면 페이즈 라우팅
            ShopScreen(
                currencyAmount = uiState.currencyAmount,
                shopItems = uiState.shopItems,
                purchasedItemIds = uiState.purchasedItemIds,
                onItemClick = appViewModel::selectShopItem,
                onBackClick = appViewModel::exitShop
            )
        }
        
        AppPhase.SHOP_DETAIL -> { // [추가됨(권)] 상점 상세 및 치장 미리보기 페이즈 라우팅
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
        
        AppPhase.MINIGAME -> { // [추가됨(권)] 미니게임 화면 라우팅 분기
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
