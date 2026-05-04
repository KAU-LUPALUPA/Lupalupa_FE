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
            friendRepository = container.friendRepository
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
                onExitCameraMode = appViewModel::exitCameraMode // [추가됨]
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
    }
}
