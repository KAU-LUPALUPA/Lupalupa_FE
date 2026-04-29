package com.example.lupapj.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.RoomRepository
import android.graphics.Bitmap // [추가됨]
import com.example.lupapj.data.model.label
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
    private val galleryRepository: GalleryRepository // [추가됨]
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
        private val galleryRepository: GalleryRepository // [추가됨]
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(authRepository, roomRepository, galleryRepository) as T
        }
    }
}
