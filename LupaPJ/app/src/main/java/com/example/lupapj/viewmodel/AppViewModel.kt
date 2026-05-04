package com.example.lupapj.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.FriendRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.RoomRepository
import android.graphics.Bitmap // [추가됨]
import com.example.lupapj.data.model.label
import com.example.lupapj.data.model.friend.FRIEND_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.scene.FloorAnchor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val FOOD_CONSUME_AFTER_TRAVEL_DELAY_MS = 900L
private const val FOOD_CONSUME_PAUSE_MS = 650L

class AppViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val galleryRepository: GalleryRepository, // [추가됨]
    private val friendRepository: FriendRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var pendingFoodConsumeJob: Job? = null

    init {
        runBootstrap()
        
        // 갤러리 이미지 상태 구독 연동
        viewModelScope.launch {
            galleryRepository.images.collect { images ->
                _uiState.update { it.copy(galleryImages = images) }
            }
        }
        viewModelScope.launch {
            friendRepository.myProfile.collect { profile ->
                _uiState.update { it.copy(myFriendProfile = profile) }
            }
        }
        viewModelScope.launch {
            friendRepository.friends.collect { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
        }
        viewModelScope.launch {
            friendRepository.receivedRequests.collect { requests ->
                _uiState.update { it.copy(receivedFriendRequests = requests) }
            }
        }
        viewModelScope.launch {
            friendRepository.sentRequests.collect { requests ->
                _uiState.update { it.copy(sentFriendRequests = requests) }
            }
        }
        viewModelScope.launch {
            friendRepository.friendMessages.collect { messagesByFriend ->
                _uiState.update { state ->
                    val friendUserId = state.visitingFriendHome?.owner?.userId
                    if (friendUserId == null) {
                        state
                    } else {
                        state.copy(
                            friendRoomMessages = messagesByFriend[friendUserId].orEmpty()
                        )
                    }
                }
            }
        }
    }

    fun onKakaoLoginClick() {
        if (_uiState.value.isProcessingLogin) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingLogin = true) }
            // TODO: Replace mock token with real Kakao SDK access token and POST /auth/kakao.
            authRepository.loginWithKakao(kakaoAccessToken = "mock-kakao-access-token")
            val room = roomRepository.getRoom()
            _uiState.update {
                it.copy(
                    phase = AppPhase.ROOM,
                    authPopupVisible = false,
                    isProcessingLogin = false,
                    room = room
                )
            }
        }
    }

    fun onButtonAClick() {
        updateRoom { room ->
            room.copy(navBarVisible = !room.navBarVisible)
        }
    }

    fun onButtonBClick() {
        updateRoom { room ->
            room.copy(inventoryVisible = true)
        }
    }

    fun onSettingsClick() {
        _uiState.update {
            it.copy(placeholderMessage = "설정 기능은 다음 데모 범위에서 연결할게요.")
        }
    }

    fun onInventoryDismiss() {
        updateRoom { room ->
            room.copy(inventoryVisible = false)
        }
    }

    fun onRoomObjectClick(objectType: RoomObjectType) {
        if (objectType == RoomObjectType.WINDOW) return

        viewModelScope.launch {
            val nextRoom = roomRepository.performObjectAction(objectType)
            applyRepositoryRoom(nextRoom)
        }
    }

    fun onFloorTap(position: FloorAnchor) {
        val room = _uiState.value.room ?: return
        if (!room.feedMode && !room.toyMode) return

        viewModelScope.launch {
            val placingFood = room.feedMode
            val nextRoom = if (placingFood) {
                roomRepository.placeFood(position)
            } else {
                roomRepository.placeToy(position)
            }
            applyRepositoryRoom(nextRoom)

            if (placingFood && nextRoom.droppedFoodAnchor != null) {
                scheduleFoodConsumption()
            }
        }
    }

    fun onBottomNavItemClick(item: BottomNavItem) {
        when (item) {
            BottomNavItem.SCREENSHOT -> {
                // 스크린샷 모드 진입
                updateRoom { it.copy(isCameraMode = true, cameraZoom = 1f) }
            }
            BottomNavItem.GALLERY -> {
                // 갤러리 진입
                _uiState.update { it.copy(phase = AppPhase.GALLERY) }
            }
            BottomNavItem.CONTACTS -> {
                _uiState.update { it.copy(phase = AppPhase.FRIENDS) }
            }
            else -> {
                _uiState.update {
                    it.copy(placeholderMessage = "${item.label} 기능은 이번 주 데모 범위 밖입니다.")
                }
            }
        }
    }

    // [추가됨] 카메라 관련 로직
    fun exitCameraMode() {
        updateRoom { it.copy(isCameraMode = false, cameraZoom = 1f) }
    }

    fun setCameraZoom(zoom: Float) {
        updateRoom { it.copy(cameraZoom = zoom.coerceIn(1f, 3f)) } // 1x ~ 3x 제한
    }

    fun captureScreen(bitmap: Bitmap) {
        viewModelScope.launch {
            galleryRepository.saveImage(bitmap)
        }
    }

    fun toggleFavorite(imageId: String) {
        galleryRepository.toggleFavorite(imageId)
    }

    fun deleteImage(imageId: String) {
        galleryRepository.deleteImage(imageId)
    }

    fun exitGallery() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun exitFriends() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun backToFriendsFromFriendRoom() {
        _uiState.update {
            it.copy(
                phase = AppPhase.FRIENDS,
                isLoadingFriendHome = false
            )
        }
    }

    fun returnHomeFromFriendRoom() {
        _uiState.update {
            it.copy(
                phase = AppPhase.ROOM,
                isLoadingFriendHome = false,
                visitingFriendHome = null,
                friendRoomMessages = emptyList(),
                friendMessageInput = "",
                isSendingFriendMessage = false
            )
        }
    }

    fun visitFriendHome(friendUserId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase = AppPhase.FRIEND_ROOM,
                    isLoadingFriendHome = true,
                    visitingFriendHome = null,
                    friendRoomMessages = emptyList(),
                    friendMessageInput = "",
                    isSendingFriendMessage = false
                )
            }

            val result = friendRepository.getFriendHome(friendUserId)
            val messagesResult = if (result is FriendOperationResult.Success) {
                friendRepository.getFriendMessages(friendUserId)
            } else {
                null
            }
            _uiState.update {
                when (result) {
                    is FriendOperationResult.Success -> it.copy(
                        isLoadingFriendHome = false,
                        visitingFriendHome = result.value,
                        friendRoomMessages = when (messagesResult) {
                            is FriendOperationResult.Success -> messagesResult.value
                            else -> emptyList()
                        }
                    )

                    is FriendOperationResult.Failure -> it.copy(
                        phase = AppPhase.FRIENDS,
                        isLoadingFriendHome = false,
                        visitingFriendHome = null,
                        friendFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onFriendMessageChange(input: String) {
        _uiState.update {
            it.copy(friendMessageInput = input.take(FRIEND_MESSAGE_MAX_LENGTH))
        }
    }

    fun sendFriendMessage() {
        val state = _uiState.value
        val friendUserId = state.visitingFriendHome?.owner?.userId ?: return
        if (state.isSendingFriendMessage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingFriendMessage = true) }
            val result = friendRepository.sendFriendMessage(
                friendUserId = friendUserId,
                message = state.friendMessageInput
            )
            _uiState.update {
                when (result) {
                    is FriendOperationResult.Success -> it.copy(
                        friendMessageInput = "",
                        isSendingFriendMessage = false
                    )

                    is FriendOperationResult.Failure -> it.copy(
                        isSendingFriendMessage = false,
                        friendFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onFriendCodeChange(input: String) {
        _uiState.update { it.copy(friendCodeInput = input) }
    }

    fun sendFriendRequest() {
        if (_uiState.value.isSendingFriendRequest) return

        viewModelScope.launch {
            val input = _uiState.value.friendCodeInput
            _uiState.update { it.copy(isSendingFriendRequest = true) }
            val result = friendRepository.sendFriendRequest(input)
            _uiState.update {
                it.copy(
                    friendCodeInput = if (result is FriendOperationResult.Success) "" else it.friendCodeInput,
                    isSendingFriendRequest = false,
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구 요청을 보냈어요."
                    )
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.acceptFriendRequest(requestId)
            _uiState.update {
                it.copy(
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구 요청을 수락했어요."
                    )
                )
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectFriendRequest(requestId)
            _uiState.update {
                it.copy(
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구 요청을 거절했어요."
                    )
                )
            }
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.cancelFriendRequest(requestId)
            _uiState.update {
                it.copy(
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구 요청을 취소했어요."
                    )
                )
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        viewModelScope.launch {
            val result = friendRepository.removeFriend(friendUserId)
            _uiState.update {
                it.copy(
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구를 삭제했어요."
                    )
                )
            }
        }
    }

    fun onFriendFeedbackConsumed() {
        _uiState.update { it.copy(friendFeedbackMessage = null) }
    }

    fun onPlaceholderMessageConsumed() {
        _uiState.update { it.copy(placeholderMessage = null) }
    }

    private fun runBootstrap() {
        viewModelScope.launch {
            delay(150)
            _uiState.update { it.copy(authPopupVisible = true) }
        }
    }

    private fun updateRoom(transform: (RoomUiState) -> RoomUiState) {
        _uiState.update { state ->
            val room = state.room ?: return@update state
            state.copy(room = transform(room))
        }
    }

    private fun applyRepositoryRoom(repositoryRoom: RoomUiState) {
        val currentRoom = _uiState.value.room
        val mergedRoom = repositoryRoom.copy(
            navBarVisible = currentRoom?.navBarVisible ?: false,
            inventoryVisible = currentRoom?.inventoryVisible ?: false
        )
        _uiState.update { it.copy(room = mergedRoom) }
    }

    private fun scheduleFoodConsumption() {
        pendingFoodConsumeJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            delay(FOOD_CONSUME_AFTER_TRAVEL_DELAY_MS + FOOD_CONSUME_PAUSE_MS)

            val currentRoom = _uiState.value.room ?: return@launch
            if (currentRoom.droppedFoodAnchor == null) return@launch

            val nextRoom = roomRepository.consumeFood()
            applyRepositoryRoom(nextRoom)
        }
    }

    override fun onCleared() {
        pendingFoodConsumeJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val roomRepository: RoomRepository,
        private val galleryRepository: GalleryRepository, // [추가됨]
        private val friendRepository: FriendRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                authRepository = authRepository,
                roomRepository = roomRepository,
                galleryRepository = galleryRepository,
                friendRepository = friendRepository
            ) as T
        }
    }
}

private fun FriendOperationResult<*>.feedbackMessage(
    successMessage: String
): String {
    return when (this) {
        is FriendOperationResult.Success -> successMessage
        is FriendOperationResult.Failure -> reason.message
    }
}

private val FriendOperationFailure.message: String
    get() = when (this) {
        FriendOperationFailure.EMPTY_CODE -> "친구 코드를 입력해주세요."
        FriendOperationFailure.EMPTY_MESSAGE -> "메시지를 입력해주세요."
        FriendOperationFailure.MESSAGE_TOO_LONG -> "메시지는 ${FRIEND_MESSAGE_MAX_LENGTH}자까지 보낼 수 있어요."
        FriendOperationFailure.SELF_CODE -> "내 친구 코드는 입력할 수 없어요."
        FriendOperationFailure.USER_NOT_FOUND -> "해당 친구 코드를 찾을 수 없어요."
        FriendOperationFailure.ALREADY_FRIENDS -> "이미 친구인 유저예요."
        FriendOperationFailure.REQUEST_ALREADY_SENT -> "이미 친구 요청을 보냈어요."
        FriendOperationFailure.REQUEST_ALREADY_RECEIVED -> "이미 받은 요청이 있어요."
        FriendOperationFailure.REQUEST_NOT_FOUND -> "친구 요청을 찾을 수 없어요."
        FriendOperationFailure.REQUEST_NOT_PENDING -> "이미 처리된 요청이에요."
        FriendOperationFailure.FRIEND_NOT_FOUND -> "친구를 찾을 수 없어요."
        FriendOperationFailure.NOT_FRIENDS -> "친구 집은 친구만 방문할 수 있어요."
        FriendOperationFailure.FRIEND_HOME_UNAVAILABLE -> "친구 집을 불러올 수 없어요."
        FriendOperationFailure.BLOCKED -> "친구 요청을 보낼 수 없어요."
    }
