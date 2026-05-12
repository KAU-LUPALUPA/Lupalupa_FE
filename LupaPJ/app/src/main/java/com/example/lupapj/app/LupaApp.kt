package com.example.lupapj.app

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun LupaApp(deepLink: Uri? = null) {
    val context = LocalContext.current
    val container = remember { AppContainer(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

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

    var isAppForeground by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var offlineDialogMessage by remember { mutableStateOf("") }
    var hasShownOfflineDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isAppForeground = true
                Lifecycle.Event.ON_STOP -> {
                    isAppForeground = false
                    hasShownOfflineDialog = false
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(deepLink) {
        val token = deepLink?.getQueryParameter("accessToken")
        val nickname = deepLink?.getQueryParameter("nickname")
        val uid = deepLink?.getQueryParameter("uid")

        if (!token.isNullOrBlank()) {
            // [수정됨(권)] 딥링크를 통한 로그인 시 ViewModel에 uid를 함께 전달하여 하트비트 식별자 일치시킴
            appViewModel.onKakaoLoginSuccess(
                accessToken = token,
                nickname = nickname,
                uid = uid
            )
        }
    }

    // [수정됨(권)] 메인 브런치 기준 하트비트 및 오프라인 시간 계산 로직 통합
    LaunchedEffect(uiState.phase, uiState.userId, isAppForeground) {
        val currentUserId = uiState.userId ?: return@LaunchedEffect

        if (uiState.phase == AppPhase.ROOM && isAppForeground) {
            // 초기 접속 시 오프라인 시간 계산
            val offlineSeconds = withContext(Dispatchers.IO) {
                sendHeartbeatToServer(currentUserId)
            }

            if (offlineSeconds != null && !hasShownOfflineDialog) {
                offlineDialogMessage = formatOfflineMessage(offlineSeconds)
                showOfflineDialog = true
                hasShownOfflineDialog = true
            }

            // [수정됨(권)] 30초마다 하트비트를 전송하여 실시간 접속 상태 유지 (메인 브런치 로직)
            while (isActive && isAppForeground && uiState.phase == AppPhase.ROOM) {
                delay(30_000)
                withContext(Dispatchers.IO) {
                    sendHeartbeatToServer(currentUserId)
                }
            }
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
            Box {
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
                onToggleBehaviorDebugClick = appViewModel::toggleBehaviorDebugWindow,
                onMinigameClick = appViewModel::openMinigame // [수정됨(권)] 미니게임 진입 연결
            )

            if (showOfflineDialog) {
                Dialog(
                    onDismissRequest = { showOfflineDialog = false }
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFE6A64A),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = offlineDialogMessage,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showOfflineDialog = false }
                        ) {
                            Text("확인")
                        }
                    }
                }
            }
        }
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
                currencyAmount = uiState.currencyAmount.toInt(),
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
                currencyAmount = uiState.currencyAmount.toInt(),
                feedbackMessage = uiState.shopFeedbackMessage,
                onEarnCurrencyClick = appViewModel::earnCurrencyFromMinigame,
                onBackClick = appViewModel::exitMinigame,
                onFeedbackConsumed = appViewModel::consumeShopFeedback
            )
        }
    }
}

private fun sendHeartbeatToServer(userId: String): Long? {
    val url = URL("http://15.164.49.236:8080/user/heartbeat")
    val connection = url.openConnection() as HttpURLConnection

    return try {
        connection.requestMethod = "POST"
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doOutput = true

        val body = """
            {
                "userId": "$userId"
            }
        """.trimIndent()

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(body)
            writer.flush()
        }

        val responseCode = connection.responseCode
        println("Heartbeat responseCode: $responseCode")

        if (responseCode in 200..299) {
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            println("Heartbeat responseBody: $responseBody")

            val json = JSONObject(responseBody)
            json.optLong("offlineSeconds", 0L)
        } else {
            println("Heartbeat redirectLocation: ${connection.getHeaderField("Location")}")
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        connection.disconnect()
    }
}

private fun formatOfflineMessage(seconds: Long): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}일 ${hours % 24}시간 동안\n자리를 비웠어요!"
        hours > 0 -> "${hours}시간 ${minutes % 60}분 동안\n자리를 비웠어요!"
        minutes > 0 -> "${minutes}분 동안\n자리를 비웠어요!"
        else -> "${seconds}초 동안\n자리를 비웠어요!"
    }
}