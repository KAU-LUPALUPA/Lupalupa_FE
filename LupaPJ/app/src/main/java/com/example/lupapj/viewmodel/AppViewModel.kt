package com.example.lupapj.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.DemoPetConditionPolicy
import com.example.lupapj.data.model.GalleryImage
import com.example.lupapj.data.model.MainMenuAction
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetConditionTickRemainder
import com.example.lupapj.data.model.PetUiState
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.ContestRepository
import com.example.lupapj.data.repository.ContestUploadResult
import com.example.lupapj.data.repository.CurrencyRepository
import com.example.lupapj.data.repository.FriendRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.PetRepository
import com.example.lupapj.data.repository.PlazaRepository
import com.example.lupapj.data.repository.RoomRepository
import com.example.lupapj.data.repository.ShopRepository // [추가됨(권)] 상점 리포지토리 의존성
import android.graphics.Bitmap // [추가됨]
import com.example.lupapj.data.model.ShopItem // [추가됨(권)] 상점 아이템 모델 Import
import com.example.lupapj.data.model.friend.FRIEND_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.plaza.PLAZA_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaOperationResult
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.PET_AUTONOMOUS_MOVE_DURATION_MS
import com.example.lupapj.data.model.scene.PetMovementState
import com.example.lupapj.data.model.advancePetCondition
import com.example.lupapj.data.model.applyFeedRecovery
import com.example.lupapj.data.model.scene.autonomousMovementProfileFor
import com.example.lupapj.data.model.scene.chooseAutonomousPetTarget
import com.example.lupapj.data.model.scene.updatePet
import com.example.lupapj.data.remote.room.RoomLayoutApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.lupapj.ui.screens.main.RearrangeController

private const val FOOD_CONSUME_AFTER_TRAVEL_DELAY_MS = 900L
private const val FOOD_CONSUME_PAUSE_MS = 650L
private const val AUTONOMOUS_MOVEMENT_RETRY_DELAY_MS = 800L
private const val FRIEND_MESSAGE_POLL_INTERVAL_MS = 3_000L
private const val HOME_VISIT_POLL_INTERVAL_MS = 3_000L
private const val PLAZA_SNAPSHOT_POLL_INTERVAL_MS = 2_000L

// [수정됨(권)] 성격별 행동 엔진 밸런스를 위해 2초 틱 유지
private const val PET_CONDITION_TICK_INTERVAL_MS = 2_000L 
private const val DEV_LOGIN_USER_ID = "dev_local_user"
private const val DEV_LOGIN_NICKNAME = "개발자"

class AppViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val galleryRepository: GalleryRepository,
    private val friendRepository: FriendRepository,
    private val plazaRepository: PlazaRepository,
    private val currencyRepository: CurrencyRepository,
    private val shopRepository: ShopRepository,
    private val contestRepository: ContestRepository,
    private val petRepository: PetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var roomBeforeRearrange: RoomUiState? = null
    private var behaviorConsecutiveTicks = 0
    
    // TODO: 아래 상태 변수들과 행동 시퀀스 로직을 PetBehaviorEngine 클래스로 분리 고려 (God Class 방지)
    private var petConditionRemainder = PetConditionTickRemainder()
    private var droppedFoodTicks = 0 // [수정됨(권)] 사료 방치 시간 (M, K 확률 계산용)
    private var droppedToyTicks = 0  // [수정됨(권)] 장난감 방치 시간 (M, K 확률 계산용)
    private var pendingFoodConsumeJob: Job? = null
    private var autonomousPetMovementJob: Job? = null
    private var friendMessagePollingJob: Job? = null
    private var homeVisitPollingJob: Job? = null
    private var petConditionJob: Job? = null
    private var plazaSnapshotPollingJob: Job? = null
    
    // [보존] 팀원 작업 로직용 잡
    private var plazaJoinJob: Job? = null
    private var plazaMessageSendJob: Job? = null
    private var plazaJoinRequestId = 0

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
            friendRepository.receivedHomeInvitations.collect { invitations ->
                _uiState.update { it.copy(receivedHomeInvitations = invitations) }
            }
        }
        viewModelScope.launch {
            friendRepository.friendMessages.collect { messagesByFriend ->
                _uiState.update { state ->
                    val friendUserId = if (state.activeHomeVisitSession == null) {
                        state.visitingFriendHome?.owner?.userId
                    } else {
                        null
                    }
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
        viewModelScope.launch {
            friendRepository.activeHomeVisits.collect { visits ->
                var sessionToPoll: String? = null
                var shouldStopPolling = false
                _uiState.update { state ->
                    val activeSessions = visits.hosting + visits.visiting
                    val currentSession = state.activeHomeVisitSession
                    val refreshedActiveSession = state.activeHomeVisitSession?.let { activeSession ->
                        activeSessions.firstOrNull { it.id == activeSession.id }
                    }
                    val currentSessionEnded = currentSession != null && refreshedActiveSession == null
                    val canSelectHostingSession = state.phase == AppPhase.ROOM ||
                        (currentSessionEnded && state.phase == AppPhase.FRIEND_ROOM)
                    val nextActiveSession = refreshedActiveSession
                        ?: if (canSelectHostingSession) visits.hosting.firstOrNull() else null
                    val activeSessionChanged = nextActiveSession?.id != currentSession?.id

                    if (nextActiveSession != null && activeSessionChanged) {
                        sessionToPoll = nextActiveSession.id
                    }
                    if (currentSessionEnded || nextActiveSession == null) {
                        shouldStopPolling = true
                    }

                    if (currentSessionEnded && state.phase == AppPhase.FRIEND_ROOM) {
                        state.copy(
                            phase = AppPhase.ROOM,
                            hostingHomeVisitSessions = visits.hosting,
                            visitingFriendHome = null,
                            activeHomeVisitSession = nextActiveSession,
                            friendRoomMessages = emptyList(),
                            friendMessageInput = "",
                            isSendingFriendMessage = false,
                            placeholderMessage = "친구 집 방문이 종료됐어요."
                        )
                    } else {
                        state.copy(
                            hostingHomeVisitSessions = visits.hosting,
                            visitingFriendHome = if (
                                state.phase == AppPhase.FRIEND_ROOM &&
                                nextActiveSession != null
                            ) {
                                nextActiveSession.hostHome ?: state.visitingFriendHome
                            } else {
                                state.visitingFriendHome
                            },
                            activeHomeVisitSession = nextActiveSession,
                            friendRoomMessages = if (nextActiveSession == null || activeSessionChanged) {
                                emptyList()
                            } else {
                                state.friendRoomMessages
                            },
                            friendMessageInput = if (nextActiveSession == null || activeSessionChanged) {
                                ""
                            } else {
                                state.friendMessageInput
                            },
                            isSendingFriendMessage = if (nextActiveSession == null || activeSessionChanged) {
                                false
                            } else {
                                state.isSendingFriendMessage
                            },
                            placeholderMessage = if (currentSessionEnded && state.phase == AppPhase.ROOM) {
                                "친구 집 방문이 종료됐어요."
                            } else {
                                state.placeholderMessage
                            }
                        )
                    }
                }
                val visitSessionId = sessionToPoll
                if (visitSessionId != null) {
                    friendRepository.getHomeVisitMessages(visitSessionId)
                    startFriendMessagePolling(visitSessionId)
                } else if (shouldStopPolling) {
                    stopFriendMessagePolling()
                }
            }
        }
        viewModelScope.launch {
            friendRepository.homeVisitMessages.collect { messagesByVisit ->
                _uiState.update { state ->
                    val visitSessionId = state.activeHomeVisitSession?.id
                    if (visitSessionId == null) {
                        state
                    } else {
                        state.copy(
                            friendRoomMessages = messagesByVisit[visitSessionId].orEmpty()
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            plazaRepository.activePlaza.collect { plaza ->
                _uiState.update { state ->
                    val currentPlaza = state.activePlaza
                    if (
                        plaza != null &&
                        currentPlaza?.plazaId == plaza.plazaId &&
                        plaza.roomRevision <= currentPlaza.roomRevision
                    ) {
                        state
                    } else {
                        state.copy(activePlaza = plaza)
                    }
                }
            }
        }
        
        // [추가됨(권)] 재화 및 상점 상태 구독 블록. 리포지토리의 StateFlow 값을 구독하여 UiState에 반영합니다.
        viewModelScope.launch {
            currencyRepository.currencyState.collect { currency ->
                _uiState.update { it.copy(currencyAmount = currency.amount) }
            }
        }
        viewModelScope.launch {
            shopRepository.shopItems.collect { items ->
                _uiState.update { it.copy(shopItems = items) }
            }
        }
        viewModelScope.launch {
            shopRepository.inventory.collect { inventory ->
                _uiState.update { it.copy(purchasedItems = inventory) }
            }
        }
    }

    fun onKakaoLoginClick() {
        if (_uiState.value.isProcessingLogin) return

        _uiState.update {
            it.copy(isProcessingLogin = true)
        }
    }

    fun onKakaoLoginSuccess(
        accessToken: String,
        nickname: String?,
        uid: String? = null
    ) {
        if (accessToken.isBlank()) {
            _uiState.update {
                it.copy(
                    isProcessingLogin = false,
                    placeholderMessage = "로그인 토큰을 받지 못했습니다."
                )
            }
            return
        }

        val resolvedUserId = uid?.takeIf { it.isNotBlank() }
            ?: nickname?.takeIf { it.isNotBlank() }

        // 실제 네트워크 통신을 위해 TokenManager에 저장
        com.example.lupapj.data.local.TokenManager.accessToken = accessToken
        friendRepository.updateCurrentUser(
            userId = resolvedUserId,
            nickname = nickname ?: "사용자"
        )
        plazaRepository.updateCurrentUser(
            userId = resolvedUserId,
            nickname = nickname ?: "사용자"
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessingLogin = true)
            }

            var room = roomRepository.getRoom()
            // [수정됨(권)] 초기 진입 시 이전 세션의 이동 상태가 남아있어 행동 로직이 멈추는 현상을 방지하기 위해 강제 해제
            room = room.copy(
                houseSceneState = room.houseSceneState.updatePet(isMoving = false)
            )
            val displayName = nickname ?: "사용자"

            // [통합 수정] 로그인 성공 시 바로 방에 진입하지 않고, feat/shopv2 기획에 따라 
            // 신규 로딩/스플래시 화면(AppPhase.SPLASH_LOADING)으로 진입합니다.
            // 기존 main의 Ticker 및 Polling 시작 로직은 실제 방 진입 시점인 startRoomPhase()로 이관되었습니다.
            _uiState.update {
                it.copy(
                    phase = AppPhase.SPLASH_LOADING,
                    authPopupVisible = false,
                    isProcessingLogin = false,
                    room = room,
                    placeholderMessage = "${displayName}님 환영합니다!",
                    userId = resolvedUserId // [수정됨(권)] 하트비트 로직을 위한 유저 식별자 저장
                )
            }
        }
    }

    fun onDevLoginClick() {
        if (_uiState.value.isProcessingLogin) return

        com.example.lupapj.data.local.TokenManager.accessToken = null
        friendRepository.updateCurrentUser(
            userId = DEV_LOGIN_USER_ID,
            nickname = DEV_LOGIN_NICKNAME
        )
        plazaRepository.updateCurrentUser(
            userId = DEV_LOGIN_USER_ID,
            nickname = DEV_LOGIN_NICKNAME
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessingLogin = true)
            }

            var room = roomRepository.getRoom()
            room = room.copy(
                houseSceneState = room.houseSceneState.updatePet(isMoving = false)
            )

            // [통합 수정] 개발자 로그인 성공 시에도 동일하게 스플래시 화면(AppPhase.SPLASH_LOADING)으로 진입합니다.
            // 펫 상태 Ticker 및 친구 방문 폴링 시작 로직은 startRoomPhase()로 이관되었습니다.
            _uiState.update {
                it.copy(
                    phase = AppPhase.SPLASH_LOADING,
                    authPopupVisible = false,
                    isProcessingLogin = false,
                    room = room,
                    placeholderMessage = "개발자 모드로 입장했습니다.",
                    userId = null,
                    friendFeedbackMessage = null,
                    plazaFeedbackMessage = null
                )
            }
        }
    }

    fun onSplashComplete() {
        _uiState.update {
            it.copy(
                phase = AppPhase.START_PROMPT,
                authPopupVisible = false
            )
        }
        viewModelScope.launch {
            delay(150)
            _uiState.update { it.copy(authPopupVisible = true) }
        }
    }

    // [통합 추가] 스플래시 연출 및 터치 대기 상태를 완료하고 실제 게임 방에 들어가는 시점입니다.
    fun startRoomPhase() {
        _uiState.update {
            it.copy(phase = AppPhase.ROOM)
        }
        startPetConditionTicker()
        // [통합 추가] main 브랜치의 실시간 친구 집 방문 정보 조회 폴링을 방 입장 시점에 맞춰 시작합니다.
        startHomeVisitPolling()
    }

    fun onButtonAClick() {
        when (_uiState.value.recentMainMenuAction) {
            MainMenuAction.SCREENSHOT -> onBottomNavItemClick(BottomNavItem.SCREENSHOT)
            MainMenuAction.GALLERY -> onBottomNavItemClick(BottomNavItem.GALLERY)
            MainMenuAction.CONTACTS -> onBottomNavItemClick(BottomNavItem.CONTACTS)
            MainMenuAction.SHOP -> onBottomNavItemClick(BottomNavItem.SHOP)
            MainMenuAction.PLAYGROUND -> openPlaza()
            MainMenuAction.CONTEST -> openContest()
            MainMenuAction.MINIGAME -> openMinigame() // [수정됨(권)] 최근 활동 버튼 클릭 시 미니게임 진입 연결
            null -> _uiState.update {
                it.copy(placeholderMessage = "최근 사용한 기능이 없습니다.")
            }
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
        val room = _uiState.value.room ?: return

        if (room.rearrangeMode) {
            updateRoom {
                com.example.lupapj.ui.screens.main.RearrangeController.selectObject(it, objectType)
            }
            return
        }

        if (objectType == RoomObjectType.WINDOW) return

        viewModelScope.launch {
            if (objectType == RoomObjectType.BED) {
                // [수정됨(권)] 침대 클릭 시 즉시 침대로 이동 시퀀스 시작
                val roomState = _uiState.value.room ?: return@launch
                val bedAnchor = roomState.sceneDefinition.objects.find { it.type == RoomObjectType.BED }?.anchor as? FloorAnchor
                if (bedAnchor != null) {
                    startBedRestingSequence(bedAnchor)
                    
                    // 서버 연동 및 이벤트 로깅: 플레이어 행동 (침대 클릭) 즉시 카운트
                    val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
                    val room = _uiState.value.room ?: return@launch
                    try {
                        val serverPetState = petRepository.sleepPet(pet.petId, room.sceneDefinition.id)
                        _uiState.update { state ->
                            val r = state.room ?: return@update state
                            val currentPet = r.houseSceneState.pet
                            val newPetState = currentPet.copy(
                                status = serverPetState.pet.status,
                                traits = serverPetState.pet.traits,
                                interactionEvents = serverPetState.pet.interactionEvents
                            )
                            state.copy(room = r.copy(houseSceneState = r.houseSceneState.copy(pet = newPetState)))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppViewModel", "Failed to sync sleep command to server", e)
                    }
                    _uiState.update { state ->
                        val currentRoom = state.room ?: return@update state
                        val currentPet = currentRoom.houseSceneState.pet
                        val updatedEvents = currentPet.interactionEvents.copy(
                            recentSleepCommandCount = currentPet.interactionEvents.recentSleepCommandCount + 1,
                            totalSleepCommandCount = currentPet.interactionEvents.totalSleepCommandCount + 1
                        )
                        state.copy(
                            room = currentRoom.copy(
                                houseSceneState = currentRoom.houseSceneState.copy(
                                    pet = currentPet.copy(interactionEvents = updatedEvents)
                                )
                            )
                        )
                    }
                }
            } else {
                val nextRoom = roomRepository.performObjectAction(objectType)
                applyRepositoryRoom(nextRoom)
            }
        }
    }

    fun onRearrangeClick() {
        val room = _uiState.value.room ?: return

        if (!room.rearrangeMode) {
            roomBeforeRearrange = room

            updateRoom {
                RearrangeController.toggle(it)
            }

            _uiState.update {
                it.copy(
                    placeholderMessage = "현재 재배치 중입니다. 취소하면 변경 전 배치로 돌아갑니다."
                )
            }
        } else {
            onRearrangeCancel()
        }
    }
    fun onRearrangeCancel() {
        val previousRoom = roomBeforeRearrange

        updateRoom { room ->
            previousRoom?.copy(
                rearrangeMode = false,
                selectedRearrangeObjectType = null
            ) ?: room.copy(
                rearrangeMode = false,
                selectedRearrangeObjectType = null
            )
        }

        roomBeforeRearrange = null

        _uiState.update {
            it.copy(
                placeholderMessage = "재배치를 취소했습니다."
            )
        }
    }
    fun onRearrangeMoveUp() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveUp(room)
        }
    }

    fun onRearrangeMoveDown() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveDown(room)
        }
    }

    fun onRearrangeMoveLeft() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveLeft(room)
        }
    }

    fun onRearrangeMoveRight() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveRight(room)
        }
    }

    fun onRearrangeConfirm() {
        val room = _uiState.value.room ?: return

        viewModelScope.launch {
            val confirmedRoom = com.example.lupapj.ui.screens.main.RearrangeController.confirm(room)

            try {
                val savedRoom = roomRepository.saveRoomLayout(
                    confirmedRoom
                )
                roomBeforeRearrange = null
                applyRepositoryRoom(savedRoom)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: RoomLayoutApiException) {
                if (exception.code == "ROOM_LAYOUT_CONFLICT") {
                    refreshRoomAfterLayoutConflict()
                } else {
                    _uiState.update {
                        it.copy(
                            placeholderMessage = "재배치 저장에 실패했습니다. 위치가 겹치지 않는지 확인해주세요."
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        placeholderMessage = "재배치 저장에 실패했습니다. 위치가 겹치지 않는지 확인해주세요."
                    )
                }
            }
        }
    }

    private suspend fun refreshRoomAfterLayoutConflict() {
        try {
            val latestRoom = roomRepository.refreshRoomLayout()
            roomBeforeRearrange = null
            applyRepositoryRoom(latestRoom)
            _uiState.update {
                it.copy(
                    placeholderMessage = "다른 기기에서 방 배치가 먼저 변경돼 최신 배치로 갱신했습니다."
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    placeholderMessage = "다른 기기에서 방 배치가 먼저 변경됐지만 최신 배치를 불러오지 못했습니다. 네트워크 확인 후 다시 시도해주세요."
                )
            }
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

            if (placingFood && nextRoom.houseSceneState.currentSceneRuntime.droppedFoodAnchor != null) {
                // [수정됨(권)] 직접 먹이 주기 시 즉시 반응하지 않고, 행동 엔진의 다음 틱에서 확률적으로 결정하도록 유도
                _uiState.update { it.copy(placeholderMessage = "바닥에 사료를 놓았습니다.") }
            }
            
            // 서버 연동: 플레이어의 행동 즉시 카운트
            try {
                val petId = nextRoom.houseSceneState.pet.petId
                val sceneId = room.sceneDefinition.id
                val serverPetState = if (placingFood) {
                    petRepository.feedPet(petId, sceneId)
                } else {
                    petRepository.playPet(petId, sceneId)
                }
                _uiState.update { state ->
                    val r = state.room ?: return@update state
                    val currentPet = r.houseSceneState.pet
                    val newPetState = currentPet.copy(
                        status = serverPetState.pet.status,
                        traits = serverPetState.pet.traits,
                        interactionEvents = serverPetState.pet.interactionEvents
                    )
                    state.copy(room = r.copy(houseSceneState = r.houseSceneState.copy(pet = newPetState)))
                }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Failed to sync feed/play action to server", e)
            }
            
            // 로컬 폴백: 서버 실패 시에도 상호작용 카운트 증가 (UI 즉각 반응 및 롤백 방지용)
            _uiState.update { state ->
                val currentRoom = state.room ?: return@update state
                val currentPet = currentRoom.houseSceneState.pet
                val currentEvents = currentPet.interactionEvents
                val updatedEvents = if (placingFood) {
                    currentEvents.copy(
                        recentFeedCount = currentEvents.recentFeedCount + 1,
                        totalFeedCount = currentEvents.totalFeedCount + 1,
                        neglectTicks = 0
                    )
                } else {
                    currentEvents.copy(
                        recentPlayCount = currentEvents.recentPlayCount + 1,
                        totalPlayCount = currentEvents.totalPlayCount + 1,
                        neglectTicks = 0
                    )
                }
                state.copy(
                    room = currentRoom.copy(
                        houseSceneState = currentRoom.houseSceneState.copy(
                            pet = currentPet.copy(interactionEvents = updatedEvents)
                        )
                    )
                )
            }
        }
    }

    fun onDroppedToyClick() {
        val room = _uiState.value.room ?: return
        val runtime = room.houseSceneState.currentSceneRuntime
        val pet = room.houseSceneState.pet
        val droppedToyAnchor = runtime.droppedToyAnchor ?: return
        val toyBoxAnchor = room.sceneDefinition.objects
            .firstOrNull { it.type == RoomObjectType.TOY_BOX }
            ?.anchor as? FloorAnchor ?: return

        if (!runtime.isToyKnockedOver) return
        if (room.feedMode || room.toyMode || room.rearrangeMode || room.isCameraMode) return
        if (pet.action == PetAction.CLEANING) return

        // 서버 연동: 플레이어의 행동 즉시 카운트
        viewModelScope.launch {
            try {
                val serverPetState = petRepository.cleanPet(pet.petId, room.sceneDefinition.id)
                _uiState.update { state ->
                    val r = state.room ?: return@update state
                    val currentPet = r.houseSceneState.pet
                    val newPetState = currentPet.copy(
                        status = serverPetState.pet.status,
                        traits = serverPetState.pet.traits,
                        interactionEvents = serverPetState.pet.interactionEvents
                    )
                    state.copy(room = r.copy(houseSceneState = r.houseSceneState.copy(pet = newPetState)))
                }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Failed to sync clean command to server", e)
            }
        }
        
        _uiState.update { state ->
            val currentRoom = state.room ?: return@update state
            val currentPet = currentRoom.houseSceneState.pet
            val updatedEvents = currentPet.interactionEvents.copy(
                recentCleanCommandCount = currentPet.interactionEvents.recentCleanCommandCount + 1,
                totalCleanCommandCount = currentPet.interactionEvents.totalCleanCommandCount + 1,
                neglectTicks = 0
            )
            state.copy(
                room = currentRoom.copy(
                    houseSceneState = currentRoom.houseSceneState.copy(
                        pet = currentPet.copy(interactionEvents = updatedEvents)
                    )
                )
            )
        }

        startToyCleanupSequence(
            droppedToyAnchor = droppedToyAnchor,
            toyBoxAnchor = toyBoxAnchor
        )
    }

    fun onBottomNavItemClick(item: BottomNavItem) {
        rememberRecentMainMenuAction(item.toMainMenuAction())
        when (item) {
            BottomNavItem.SHOP -> {
                // [추가됨(권)] 상점 탭 클릭 시 상점 아이템을 서버(mock)에서 새로고침 후 진입
                viewModelScope.launch {
                    shopRepository.fetchShopItems()
                }
                _uiState.update { it.copy(phase = AppPhase.SHOP) }
            }
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
                refreshFriendOverview()
            }
        }
    }

    fun exitCameraMode() {
        updateRoom { it.copy(isCameraMode = false, cameraZoom = 1f, cameraOffsetX = 0f, cameraOffsetY = 0f) }
    }

    fun setCameraZoom(zoom: Float) {
        updateRoom { it.copy(cameraZoom = zoom.coerceIn(1f, 3f)) } // 1x ~ 3x 제한
    }

    fun setCameraOffset(dx: Float, dy: Float) {
        val room = _uiState.value.room ?: return
        val zoom = room.cameraZoom
        // zoom이 클수록 이동 범위도 넓어지도록 허용 (최대 절반 화면 이동)
        val maxOffset = 500f * (zoom - 1f) / zoom
        updateRoom {
            it.copy(
                cameraOffsetX = (it.cameraOffsetX + dx).coerceIn(-maxOffset, maxOffset),
                cameraOffsetY = (it.cameraOffsetY + dy).coerceIn(-maxOffset, maxOffset)
            )
        }
    }

    // [추가됨] 카메라 오버레이 내 갤러리 버튼 클릭 시 카메라 종료 후 갤러리 화면 진입
    fun exitCameraAndOpenGallery() {
        exitCameraMode()
        _uiState.update { it.copy(phase = AppPhase.GALLERY) }
    }

    fun captureScreen(bitmap: Bitmap) {
        viewModelScope.launch {
            galleryRepository.saveImage(bitmap)
        }
    }

    fun toggleFavorite(imageId: String) {
        viewModelScope.launch {
            galleryRepository.toggleFavorite(imageId)
        }
    }

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            galleryRepository.deleteImage(imageId)
        }
    }

    fun exitGallery() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun exitFriends() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun openPlaza() {
        _uiState.update {
            it.copy(
                phase = AppPhase.PLAZA,
                recentMainMenuAction = MainMenuAction.PLAYGROUND,
                plazaFeedbackMessage = null
            )
        }
        viewModelScope.launch {
            val result = plazaRepository.refreshActivePlaza()
            when (result) {
                is PlazaOperationResult.Success -> {
                    result.value?.plazaId?.let(::startPlazaSnapshotPolling)
                }

                is PlazaOperationResult.Failure -> {
                    _uiState.update {
                        it.copy(plazaFeedbackMessage = result.reason.message)
                    }
                }
            }
        }
    }

    fun returnHomeFromPlaza() {
        plazaJoinRequestId += 1
        plazaJoinJob?.cancel()
        plazaJoinJob = null
        cancelPlazaMessageSend()
        stopPlazaSnapshotPolling()
        val shouldLeavePlaza = _uiState.value.activePlaza != null

        _uiState.update {
            it.copy(
                phase = AppPhase.ROOM,
                activePlaza = null,
                plazaCodeInput = "",
                plazaMessageInput = "",
                isJoiningPlaza = false,
                isSendingPlazaMessage = false,
                plazaFeedbackMessage = null
            )
        }

        if (!shouldLeavePlaza) return
        viewModelScope.launch {
            plazaRepository.leavePlaza()
        }
    }

    fun onPlazaCodeChange(input: String) {
        _uiState.update {
            it.copy(plazaCodeInput = input.take(12).uppercase())
        }
    }

    fun joinRandomPlaza() {
        if (_uiState.value.isJoiningPlaza) return

        val requestId = ++plazaJoinRequestId
        plazaJoinJob?.cancel()
        cancelPlazaMessageSend()
        plazaJoinJob = viewModelScope.launch {
            val participantProfile = currentPlazaParticipantProfile()
            _uiState.update {
                it.copy(
                    isJoiningPlaza = true,
                    plazaFeedbackMessage = null
                )
            }
            val result = plazaRepository.joinRandomPlaza(
                currentUserId = participantProfile.userId,
                nickname = participantProfile.nickname,
                pet = participantProfile.pet
            )
            if (!isActivePlazaJoinRequest(requestId)) {
                if (
                    result is PlazaOperationResult.Success &&
                    plazaRepository.activePlaza.value?.plazaId == result.value.plazaId
                ) {
                    plazaRepository.leavePlaza()
                }
                return@launch
            }
            _uiState.update {
                when (result) {
                    is PlazaOperationResult.Success -> it.copy(
                        isJoiningPlaza = false,
                        plazaCodeInput = "",
                        plazaFeedbackMessage = null
                    )

                    is PlazaOperationResult.Failure -> it.copy(
                        isJoiningPlaza = false,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
            if (result is PlazaOperationResult.Success) {
                startPlazaSnapshotPolling(result.value.plazaId)
            }
        }
    }

    fun joinPlazaByCode() {
        if (_uiState.value.isJoiningPlaza) return

        val requestId = ++plazaJoinRequestId
        plazaJoinJob?.cancel()
        cancelPlazaMessageSend()
        plazaJoinJob = viewModelScope.launch {
            val state = _uiState.value
            val participantProfile = currentPlazaParticipantProfile()
            _uiState.update {
                it.copy(
                    isJoiningPlaza = true,
                    plazaFeedbackMessage = null
                )
            }
            val result = plazaRepository.joinPlazaByCode(
                codeInput = state.plazaCodeInput,
                currentUserId = participantProfile.userId,
                nickname = participantProfile.nickname,
                pet = participantProfile.pet
            )
            if (!isActivePlazaJoinRequest(requestId)) {
                if (
                    result is PlazaOperationResult.Success &&
                    plazaRepository.activePlaza.value?.plazaId == result.value.plazaId
                ) {
                    plazaRepository.leavePlaza()
                }
                return@launch
            }
            _uiState.update {
                when (result) {
                    is PlazaOperationResult.Success -> it.copy(
                        isJoiningPlaza = false,
                        plazaCodeInput = "",
                        plazaFeedbackMessage = null
                    )

                    is PlazaOperationResult.Failure -> it.copy(
                        isJoiningPlaza = false,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
            if (result is PlazaOperationResult.Success) {
                startPlazaSnapshotPolling(result.value.plazaId)
            }
        }
    }

    fun leaveCurrentPlaza() {
        plazaJoinRequestId += 1
        plazaJoinJob?.cancel()
        plazaJoinJob = null
        cancelPlazaMessageSend()
        stopPlazaSnapshotPolling()
        val leavingPlaza = _uiState.value.activePlaza

        _uiState.update {
            it.copy(
                activePlaza = null,
                plazaMessageInput = "",
                isJoiningPlaza = false,
                isSendingPlazaMessage = false
            )
        }

        viewModelScope.launch {
            val result = plazaRepository.leavePlaza()
            _uiState.update {
                when (result) {
                    is PlazaOperationResult.Success -> it.copy(
                        plazaFeedbackMessage = "광장에서 나왔어요."
                    )

                    is PlazaOperationResult.Failure -> it.copy(
                        activePlaza = leavingPlaza,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onPlazaMessageChange(input: String) {
        _uiState.update {
            it.copy(plazaMessageInput = input.take(PLAZA_MESSAGE_MAX_LENGTH))
        }
    }

    fun sendPlazaMessage() {
        val state = _uiState.value
        if (state.isSendingPlazaMessage) return
        val sendingPlazaId = state.activePlaza?.plazaId
            ?: return

        plazaMessageSendJob = viewModelScope.launch {
            _uiState.update { it.copy(isSendingPlazaMessage = true) }
            val result = plazaRepository.sendMessage(state.plazaMessageInput)

            _uiState.update { latestState ->
                if (
                    latestState.phase != AppPhase.PLAZA ||
                    latestState.activePlaza?.plazaId != sendingPlazaId
                ) {
                    return@update latestState
                }

                when (result) {
                    is PlazaOperationResult.Success -> latestState.copy(
                        plazaMessageInput = "",
                        isSendingPlazaMessage = false,
                        plazaFeedbackMessage = null
                    )

                    is PlazaOperationResult.Failure -> latestState.copy(
                        isSendingPlazaMessage = false,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onPlazaFeedbackConsumed() {
        _uiState.update { it.copy(plazaFeedbackMessage = null) }
    }

    private fun isActivePlazaJoinRequest(requestId: Int): Boolean {
        val state = _uiState.value
        return plazaJoinRequestId == requestId && state.phase == AppPhase.PLAZA
    }

    private fun cancelPlazaMessageSend() {
        plazaMessageSendJob?.cancel()
        plazaMessageSendJob = null
    }

    private fun startPlazaSnapshotPolling(plazaId: String) {
        if (plazaSnapshotPollingJob?.isActive == true) {
            plazaSnapshotPollingJob?.cancel()
        }
        plazaSnapshotPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(PLAZA_SNAPSHOT_POLL_INTERVAL_MS)
                val state = _uiState.value
                if (state.phase != AppPhase.PLAZA || state.activePlaza?.plazaId != plazaId) {
                    break
                }
                when (val result = plazaRepository.refreshPlazaSnapshot(plazaId)) {
                    is PlazaOperationResult.Success -> Unit
                    is PlazaOperationResult.Failure -> {
                        if (result.reason == PlazaOperationFailure.NOT_IN_PLAZA ||
                            result.reason == PlazaOperationFailure.PLAZA_NOT_FOUND
                        ) {
                            _uiState.update {
                                it.copy(
                                    activePlaza = null,
                                    plazaFeedbackMessage = result.reason.message
                                )
                            }
                            break
                        }
                    }
                }
            }
        }
    }

    private fun stopPlazaSnapshotPolling() {
        plazaSnapshotPollingJob?.cancel()
        plazaSnapshotPollingJob = null
    }

    fun openMailbox() {
        refreshFriendOverview()
        _uiState.update { it.copy(mailboxVisible = true) }
    }

    fun closeMailbox() {
        _uiState.update { it.copy(mailboxVisible = false) }
    }

    private fun refreshFriendOverview() {
        viewModelScope.launch {
            val result = friendRepository.refreshFriendOverview()
            if (result is FriendOperationResult.Failure) {
                _uiState.update {
                    it.copy(friendFeedbackMessage = result.reason.message)
                }
            }
        }
    }

    fun backToFriendsFromFriendRoom() {
        stopFriendMessagePolling()
        val visitSessionId = _uiState.value.activeHomeVisitSession?.id
        _uiState.update {
            it.copy(
                phase = AppPhase.FRIENDS,
                isLoadingFriendHome = false,
                visitingFriendHome = null,
                activeHomeVisitSession = null,
                friendRoomMessages = emptyList(),
                friendMessageInput = "",
                isSendingFriendMessage = false
            )
        }
        if (visitSessionId != null) {
            viewModelScope.launch {
                friendRepository.leaveHomeVisit(visitSessionId)
                friendRepository.refreshActiveHomeVisits()
            }
        }
    }

    fun returnHomeFromFriendRoom() {
        stopFriendMessagePolling()
        val visitSessionId = _uiState.value.activeHomeVisitSession?.id
        _uiState.update {
            it.copy(
                phase = AppPhase.ROOM,
                isLoadingFriendHome = false,
                visitingFriendHome = null,
                activeHomeVisitSession = null,
                friendRoomMessages = emptyList(),
                friendMessageInput = "",
                isSendingFriendMessage = false
            )
        }
        if (visitSessionId != null) {
            viewModelScope.launch {
                friendRepository.leaveHomeVisit(visitSessionId)
                friendRepository.refreshActiveHomeVisits()
            }
        }
    }

    fun endActiveHomeVisit() {
        val visitSessionId = _uiState.value.activeHomeVisitSession?.id ?: return
        stopFriendMessagePolling()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    hostingHomeVisitSessions = it.hostingHomeVisitSessions.filterNot { session ->
                        session.id == visitSessionId
                    },
                    activeHomeVisitSession = null,
                    friendRoomMessages = emptyList(),
                    friendMessageInput = "",
                    isSendingFriendMessage = false
                )
            }
            val result = friendRepository.leaveHomeVisit(visitSessionId)
            friendRepository.refreshActiveHomeVisits()
            _uiState.update {
                it.copy(
                    placeholderMessage = result.feedbackMessage(
                        successMessage = "친구 집 방문을 종료했어요."
                    )
                )
            }
        }
    }

    fun acceptHomeInvitation(invitationId: String) {
        val returnPhase = _uiState.value.phase
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase = AppPhase.FRIEND_ROOM,
                    isLoadingFriendHome = true,
                    visitingFriendHome = null,
                    activeHomeVisitSession = null,
                    friendRoomMessages = emptyList(),
                    friendMessageInput = "",
                    isSendingFriendMessage = false
                )
            }

            val result = friendRepository.acceptHomeInvitation(invitationId)
            val messagesResult = if (result is FriendOperationResult.Success) {
                friendRepository.getHomeVisitMessages(result.value.id)
            } else {
                null
            }
            _uiState.update {
                when (result) {
                    is FriendOperationResult.Success -> it.copy(
                        isLoadingFriendHome = false,
                        mailboxVisible = false,
                        visitingFriendHome = result.value.hostHome,
                        activeHomeVisitSession = result.value,
                        friendRoomMessages = when (messagesResult) {
                            is FriendOperationResult.Success -> messagesResult.value
                            else -> emptyList()
                        }
                    )

                    is FriendOperationResult.Failure -> it.copy(
                        phase = returnPhase,
                        isLoadingFriendHome = false,
                        visitingFriendHome = null,
                        friendFeedbackMessage = if (returnPhase == AppPhase.ROOM) {
                            it.friendFeedbackMessage
                        } else {
                            result.reason.message
                        },
                        placeholderMessage = if (returnPhase == AppPhase.ROOM) {
                            result.reason.message
                        } else {
                            it.placeholderMessage
                        }
                    )
                }
            }
            if (result is FriendOperationResult.Success) {
                startFriendMessagePolling(result.value.id)
            }
        }
    }

    fun rejectHomeInvitation(invitationId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectHomeInvitation(invitationId)
            _uiState.update {
                val message = result.feedbackMessage(
                    successMessage = "초대를 거절했어요."
                )
                it.copy(
                    friendFeedbackMessage = if (it.phase == AppPhase.ROOM) {
                        it.friendFeedbackMessage
                    } else {
                        message
                    },
                    placeholderMessage = if (it.phase == AppPhase.ROOM) {
                        message
                    } else {
                        it.placeholderMessage
                    }
                )
            }
        }
    }

    private fun startFriendMessagePolling(visitSessionId: String) {
        friendMessagePollingJob?.cancel()
        friendMessagePollingJob = viewModelScope.launch {
            while (isActive) {
                delay(FRIEND_MESSAGE_POLL_INTERVAL_MS)
                val state = _uiState.value
                val activeVisitSessionId = state.activeHomeVisitSession?.id
                if (
                    (state.phase != AppPhase.FRIEND_ROOM && state.phase != AppPhase.ROOM) ||
                    activeVisitSessionId != visitSessionId
                ) {
                    break
                }
                friendRepository.getHomeVisitMessages(visitSessionId)
            }
        }
    }

    private fun stopFriendMessagePolling() {
        friendMessagePollingJob?.cancel()
        friendMessagePollingJob = null
    }

    private fun startHomeVisitPolling() {
        if (homeVisitPollingJob?.isActive == true) return
        homeVisitPollingJob = viewModelScope.launch {
            while (isActive) {
                val phase = _uiState.value.phase
                if (phase == AppPhase.ROOM || phase == AppPhase.FRIEND_ROOM) {
                    friendRepository.refreshActiveHomeVisits()
                }
                delay(HOME_VISIT_POLL_INTERVAL_MS)
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
        val visitSessionId = state.activeHomeVisitSession?.id ?: return
        if (state.isSendingFriendMessage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingFriendMessage = true) }
            val result = friendRepository.sendHomeVisitMessage(
                visitSessionId = visitSessionId,
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
                val message = result.feedbackMessage(
                    successMessage = "친구 요청을 수락했어요."
                )
                it.copy(
                    friendFeedbackMessage = if (it.phase == AppPhase.ROOM) {
                        it.friendFeedbackMessage
                    } else {
                        message
                    },
                    placeholderMessage = if (it.phase == AppPhase.ROOM) {
                        message
                    } else {
                        it.placeholderMessage
                    }
                )
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectFriendRequest(requestId)
            _uiState.update {
                val message = result.feedbackMessage(
                    successMessage = "친구 요청을 거절했어요."
                )
                it.copy(
                    friendFeedbackMessage = if (it.phase == AppPhase.ROOM) {
                        it.friendFeedbackMessage
                    } else {
                        message
                    },
                    placeholderMessage = if (it.phase == AppPhase.ROOM) {
                        message
                    } else {
                        it.placeholderMessage
                    }
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

    fun sendHomeInvitation(friendUserId: String) {
        if (_uiState.value.pendingHomeInvitationFriendId != null) return

        viewModelScope.launch {
            val friendName = _uiState.value.friends
                .firstOrNull { it.user.userId == friendUserId }
                ?.user
                ?.nickname

            _uiState.update {
                it.copy(pendingHomeInvitationFriendId = friendUserId)
            }

            val result = friendRepository.sendHomeInvitation(
                friendUserId = friendUserId,
                message = "우리 집 구경하러 올래?"
            )
            val successMessage = if (friendName.isNullOrBlank()) {
                "집 초대를 보냈어요."
            } else {
                "${friendName}님에게 집 초대를 보냈어요."
            }

            _uiState.update {
                it.copy(
                    pendingHomeInvitationFriendId = null,
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = successMessage
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
    
    // [추가됨(권)] 미니게임을 완료하여 재화를 획득하는 임시 로직입니다. 
    fun earnCurrencyFromMinigame() {
        viewModelScope.launch {
            val result = currencyRepository.earnCurrency(50L, "MINIGAME") 
            when (result) {
                is com.example.lupapj.data.model.CurrencyUpdateResult.Success -> {
                    _uiState.update { it.copy(shopFeedbackMessage = "미니게임 보상으로 50원을 획득했습니다!") }
                }
                is com.example.lupapj.data.model.CurrencyUpdateResult.ValidationError -> {
                    _uiState.update { it.copy(shopFeedbackMessage = result.message) }
                }
                is com.example.lupapj.data.model.CurrencyUpdateResult.NetworkError -> {
                    _uiState.update { it.copy(shopFeedbackMessage = "네트워크 오류가 발생했습니다.") }
                }
                is com.example.lupapj.data.model.CurrencyUpdateResult.AuthError -> {
                    _uiState.update { it.copy(shopFeedbackMessage = "로그인이 만료되었습니다.") }
                }
            }
        }
    }

    fun selectShopItem(item: ShopItem) {
        _uiState.update { it.copy(phase = AppPhase.SHOP_DETAIL, selectedShopItem = item) }
    }

    fun purchaseItem(itemId: String) {
        if (_uiState.value.isPurchasing) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }
            val result = shopRepository.purchaseItem(itemId)
            _uiState.update { state ->
                state.copy(
                    isPurchasing = false,
                    shopFeedbackMessage = result.exceptionOrNull()?.message ?: "구매에 성공했습니다!"
                )
            }
        }
    }

    fun exitShop() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun exitShopDetail() {
        _uiState.update { it.copy(phase = AppPhase.SHOP, selectedShopItem = null) }
    }

    fun openMinigame() {
        _uiState.update {
            it.copy(
                phase = AppPhase.MINIGAME,
                recentMainMenuAction = MainMenuAction.MINIGAME // [수정됨(권)] PLAYGROUND에서 MINIGAME으로 정정
            )
        }
    }

    fun exitMinigame() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun openContest() {
        _uiState.update {
            it.copy(
                phase = AppPhase.CONTEST,
                recentMainMenuAction = MainMenuAction.CONTEST,
                selectedContestGroup = null,
                contestUploadMessage = null,
                contestGroupMessage = null,
                contestVoteMessage = null
            )
        }
        loadContestGroups()
    }

    fun exitContest() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun openContestGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isContestGroupsLoading = true,
                    contestGroupMessage = null,
                    contestVoteMessage = null
                )
            }

            contestRepository.getGroupDetail(groupId)
                .onSuccess { group ->
                    _uiState.update {
                        it.copy(
                            selectedContestGroup = group,
                            isContestGroupsLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isContestGroupsLoading = false,
                            contestGroupMessage = throwable.message ?: "Could not load contest group."
                        )
                    }
                }
        }
    }

    fun exitContestGroup() {
        _uiState.update {
            it.copy(
                selectedContestGroup = null,
                contestVoteMessage = null
            )
        }
    }

    fun voteForContestEntry(entryId: Long) {
        if (_uiState.value.isContestVoteSubmitting) return

        val groupId = _uiState.value.selectedContestGroup?.groupId ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isContestVoteSubmitting = true,
                    contestVoteMessage = "Submitting vote..."
                )
            }

            contestRepository.vote(entryId)
                .onSuccess {
                    val refreshedGroup = contestRepository.getGroupDetail(groupId).getOrNull()
                    _uiState.update {
                        it.copy(
                            selectedContestGroup = refreshedGroup ?: it.selectedContestGroup,
                            isContestVoteSubmitting = false,
                            contestVoteMessage = "Vote submitted."
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isContestVoteSubmitting = false,
                            contestVoteMessage = throwable.message ?: "Vote failed."
                        )
                    }
                }
        }
    }

    fun uploadContestEntryImage(image: GalleryImage) {
        if (_uiState.value.isContestEntryUploading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isContestEntryUploading = true,
                    contestUploadMessage = "Uploading contest image..."
                )
            }

            when (val result = contestRepository.uploadEntryImage(image)) {
                is ContestUploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isContestEntryUploading = false,
                            contestUploadMessage = "Contest image uploaded."
                        )
                    }
                    loadContestGroups(result.groupId)
                }

                is ContestUploadResult.MatchedWithoutImage -> {
                    _uiState.update {
                        it.copy(
                            isContestEntryUploading = false,
                            contestUploadMessage = "Group matched, but image upload failed. ${result.message}"
                        )
                    }
                    loadContestGroups(result.groupId)
                }

                is ContestUploadResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isContestEntryUploading = false,
                            contestUploadMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadContestGroups(selectedGroupId: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isContestGroupsLoading = true,
                    contestGroupMessage = null
                )
            }

            contestRepository.getGroups()
                .onSuccess { groups ->
                    val myGroupId = groups.firstOrNull { it.isMyGroup }?.groupId
                    _uiState.update {
                        it.copy(
                            contestGroups = groups,
                            isContestGroupsLoading = false,
                            isContestParticipating = myGroupId != null,
                            contestMyEntryImageUrl = if (myGroupId == null) null else it.contestMyEntryImageUrl,
                            contestGroupMessage = if (groups.isEmpty()) "No contest groups yet." else null
                        )
                    }

                    if (myGroupId != null) {
                        loadContestParticipation(myGroupId)
                    }

                    if (selectedGroupId != null) {
                        openContestGroup(selectedGroupId)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isContestGroupsLoading = false,
                            contestGroupMessage = throwable.message ?: "Could not load contest groups."
                        )
                    }
                }
        }
    }

    private fun loadContestParticipation(groupId: String) {
        viewModelScope.launch {
            contestRepository.getGroupDetail(groupId)
                .onSuccess { group ->
                    val myEntryImageUrl = group.entries
                        .firstOrNull { entry -> entry.entryId == group.myEntryId }
                        ?.imageUrl
                    _uiState.update {
                        it.copy(
                            isContestParticipating = true,
                            contestMyEntryImageUrl = myEntryImageUrl
                        )
                    }
                }
        }
    }

    fun consumeShopFeedback() {
        _uiState.update { it.copy(shopFeedbackMessage = null) }
    }

    // [추가됨(권)] 아이템 장착 시 부위별 충돌을 해결해주는 헬퍼 함수
    private fun resolveEquippedCollision(
        currentEquippedIds: List<String>,
        newItemId: String,
        allItems: List<com.example.lupapj.data.model.ShopItem>
    ): List<String> {
        val newItem = allItems.find { it.id == newItemId } ?: return currentEquippedIds
        val newCategory = newItem.category

        val resolved = currentEquippedIds.filter { equippedId ->
            val eqItem = allItems.find { it.id == equippedId } ?: return@filter false
            val eqCategory = eqItem.category
            
            when (newCategory) {
                com.example.lupapj.data.model.ShopCategory.FULL_BODY -> {
                    eqCategory != com.example.lupapj.data.model.ShopCategory.TOP &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.BOTTOM &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.FULL_BODY
                }
                com.example.lupapj.data.model.ShopCategory.TOP -> {
                    eqCategory != com.example.lupapj.data.model.ShopCategory.FULL_BODY &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.TOP
                }
                com.example.lupapj.data.model.ShopCategory.BOTTOM -> {
                    eqCategory != com.example.lupapj.data.model.ShopCategory.FULL_BODY &&
                    eqCategory != com.example.lupapj.data.model.ShopCategory.BOTTOM
                }
                else -> {
                    eqCategory != newCategory
                }
            }
        }.toMutableList()
        
        resolved.add(newItemId)
        return resolved
    }

    // [추가됨(권)] 아이템 장착 함수
    fun equipItem(itemId: String) {
        updateRoom { room ->
            val pet = room.houseSceneState.pet
            val allItems = _uiState.value.shopItems.ifEmpty { com.example.lupapj.data.model.DefaultShopItems }
            val resolved = resolveEquippedCollision(pet.equippedItemIds, itemId, allItems)
            room.copy(
                houseSceneState = room.houseSceneState.copy(
                    pet = pet.copy(equippedItemIds = resolved)
                )
            )
        }
    }

    // [추가됨(권)] 아이템 해제 함수
    fun unequipItem(itemId: String) {
        updateRoom { room ->
            val pet = room.houseSceneState.pet
            room.copy(
                houseSceneState = room.houseSceneState.copy(
                    pet = pet.copy(equippedItemIds = pet.equippedItemIds.filter { it != itemId })
                )
            )
        }
    }

    fun toggleBehaviorDebugWindow() {
        _uiState.update { 
            it.copy(behaviorDebugInfo = it.behaviorDebugInfo.copy(isVisible = !it.behaviorDebugInfo.isVisible)) 
        }
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

    private fun calculateTileDistance(a: com.example.lupapj.data.model.scene.FloorAnchor, b: com.example.lupapj.data.model.scene.FloorAnchor, spec: com.example.lupapj.data.model.scene.IsoRoomProjectionSpec): Float {
        val dx = (a.u - b.u) * spec.roomWidthTiles
        val dy = (a.v - b.v) * spec.roomDepthTiles
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun currentPlazaParticipantProfile(): PlazaParticipantProfile {
        val state = _uiState.value
        val friendProfile = state.myFriendProfile
        val roomPet = state.room?.pet ?: PetUiState()
        val ownerUserId = friendProfile?.userId ?: roomPet.ownerUserId

        return PlazaParticipantProfile(
            userId = ownerUserId,
            nickname = friendProfile?.nickname ?: "나",
            pet = roomPet.toPlazaPetSnapshot(ownerUserId = ownerUserId)
        )
    }

    private fun evaluateBehaviorTransition() {
        val state = _uiState.value
        val room = state.room ?: return
        val pet = room.houseSceneState.pet

        if (
            state.phase != AppPhase.ROOM ||
            room.feedMode ||
            room.toyMode ||
            room.isCameraMode ||
            pet.status.isEgg ||
            pet.movement.isMoving ||
            pet.action == PetAction.CLEANING
        ) {
            return
        }

        // [수정됨(권)] 틱 증가를 로직 최상단으로 이동하여 UI와 실제 판정 시점을 동기화
        behaviorConsecutiveTicks++

        // V2 감정 및 상태 파생
        val derived = com.example.lupapj.data.model.BehaviorEvaluator.calculateDerivedTraits(pet.traits)
        val affect = com.example.lupapj.data.model.BehaviorEvaluator.calculateAffect(pet.traits, derived, pet.status, pet.interactionEvents)

        // 감정 기반 디버그 정보 갱신 (휴식 중 얼리 리턴 전에도 UI 틱이 계속 증가하도록 최상단에 배치)
        _uiState.update { s -> 
            s.copy(behaviorDebugInfo = s.behaviorDebugInfo.copy(
                consecutiveTicks = behaviorConsecutiveTicks,
                currentProbability = 0f,
                isCrisis = pet.status.satiety < 30 || pet.status.vitality < 30,
                mValue = affect.valence,
                kValue = affect.arousal
            ))
        }

        val isRestingState = pet.action == PetAction.RESTING || pet.action == PetAction.BED_RESTING
        
        // [수정됨(권)] 취침/휴식 중에는 10초(5틱) 단위로만 확률 계산 및 행동 전이 판정 수행
        if (isRestingState) {
            if (behaviorConsecutiveTicks % 5 != 0) return
        }

        // 기존 행동 이탈 체크 (Hazard)
        if (pet.action != PetAction.IDLE && !com.example.lupapj.data.model.BehaviorEvaluator.checkHazardExit(behaviorConsecutiveTicks.toFloat(), pet.traits.patience)) {
            return
        }

        if (pet.action == PetAction.EATING || pet.action == PetAction.PLAYING) {
            return
        }

        if (pet.action == PetAction.PLAYING) {
            viewModelScope.launch {
                val nextRoom = roomRepository.updateToyKnockedOver(true)
                applyRepositoryRoom(nextRoom)
            }
        }
        behaviorConsecutiveTicks = 0

        val hasFood = room.houseSceneState.currentSceneRuntime.droppedFoodAnchor != null ||
            room.sceneDefinition.objects.any { it.type == com.example.lupapj.data.model.RoomObjectType.FOOD_BAG }
        val hasDroppedToy = room.houseSceneState.currentSceneRuntime.droppedToyAnchor != null
        val hasToyBox = room.sceneDefinition.objects.any { it.type == com.example.lupapj.data.model.RoomObjectType.TOY_BOX }
        val hasToy = hasDroppedToy || hasToyBox
        val hasBed = room.sceneDefinition.objects.any { it.type == com.example.lupapj.data.model.RoomObjectType.BED }

        // 새 행동 결정 (Softmax)
        val scores = com.example.lupapj.data.model.BehaviorEvaluator.calculateUtilityScores(
            traits = pet.traits,
            derived = derived,
            affect = affect,
            status = pet.status,
            hasFood = hasFood,
            hasToy = hasToy,
            hasBed = hasBed,
            hasDroppedToy = hasDroppedToy,
            hasToyBox = hasToyBox
        )
        val probs = com.example.lupapj.data.model.BehaviorEvaluator.calculateActionProbabilities(scores, pet.traits.patience, derived.volatility)
        
        // [추가됨(권)] 디버그 정보에 확률 및 수치 저장
        _uiState.update { s -> 
            s.copy(behaviorDebugInfo = s.behaviorDebugInfo.copy(
                actionProbabilities = probs,
                traits = pet.traits,
                derived = derived,
                affect = affect
            ))
        }

        val nextAction = com.example.lupapj.data.model.BehaviorEvaluator.sampleAction(probs)
        executeBehaviorAction(nextAction, pet)
    }

    private fun executeBehaviorAction(action: PetAction, pet: com.example.lupapj.data.model.scene.PetSceneState) {
        val room = _uiState.value.room ?: return
        val houseSceneState = room.houseSceneState
        
        when (action) {
            PetAction.WALKING -> {
                val profile = autonomousMovementProfileFor(pet.traits)
                val targetAnchor = chooseAutonomousPetTarget(
                    currentAnchor = pet.anchor,
                    sceneDefinition = room.sceneDefinition,
                    profile = profile,
                    random = Random.Default
                )
                if (targetAnchor != null) {
                    moveToTargetAndExecute(targetAnchor, pet, PetAction.IDLE, profile) 
                } else {
                    setPetAction(PetAction.IDLE)
                }
            }
            PetAction.RESTING -> {
                setPetAction(PetAction.RESTING)
            }
            PetAction.BED_RESTING -> {
                val bedAnchor = room.sceneDefinition.objects.find { it.type == RoomObjectType.BED }?.anchor as? FloorAnchor
                if (bedAnchor != null) {
                    startBedRestingSequence(bedAnchor)
                } else {
                    setPetAction(PetAction.RESTING)
                }
            }
            PetAction.PLAYING -> {
                val targetAnchor = houseSceneState.currentSceneRuntime.droppedToyAnchor 
                    ?: room.sceneDefinition.objects.find { it.type == RoomObjectType.TOY_BOX }?.anchor?.let { it as? FloorAnchor }
                
                if (targetAnchor != null) {
                    val bedAnchor = room.sceneDefinition.objects.find { it.type == RoomObjectType.BED }?.anchor as? FloorAnchor
                    if (bedAnchor != null) {
                        val distToBed = calculateTileDistance(targetAnchor, bedAnchor, room.sceneDefinition.projectionSpec)
                        if (distToBed < 1.0f) { /* 스킵 */ }
                    }

                    if (pet.traits.activity < 0.3f) {
                        val dist = calculateTileDistance(pet.anchor, targetAnchor, room.sceneDefinition.projectionSpec)
                        if (dist > 3f) {
                            setPetAction(PetAction.RESTING)
                            return
                        }
                    }
                    startPlayingSequence(targetAnchor)
                } else {
                    // 장난감이 없을 때 PLAYING 당첨 (허공 섀도우복싱/플레이 연출)
                    // 인위적인 감정 상태(ON/OFF) 주입을 제거하고, 순수하게 행동만 PLAYING으로 둡니다.
                    // (말풍선 등의 연출은 렌더러 단에서 현재 Action과 환경을 참조하여 자연스럽게 그려야 합니다)
                    setPetAction(PetAction.PLAYING)
                    viewModelScope.launch {
                        delay(2500L)
                        // 허공 플레이 후 자연스럽게 다른 행동으로 넘어갈 수 있도록 WALKING 전환
                        if (_uiState.value.room?.houseSceneState?.pet?.action == PetAction.PLAYING) {
                            setPetAction(PetAction.WALKING)
                        }
                    }
                }
            }
            PetAction.EATING -> {
                val targetAnchor = houseSceneState.currentSceneRuntime.droppedFoodAnchor 
                    ?: room.sceneDefinition.objects.find { it.type == RoomObjectType.FOOD_BAG }?.anchor?.let { it as? FloorAnchor }
                if (targetAnchor != null) {
                    if (pet.traits.activity < 0.3f) {
                        val dist = calculateTileDistance(pet.anchor, targetAnchor, room.sceneDefinition.projectionSpec)
                        if (dist > 3f) {
                            setPetAction(PetAction.RESTING)
                            return
                        }
                    }
                    startEatingSequence(targetAnchor, skipMovement = false)
                } else {
                    setPetAction(PetAction.IDLE)
                }
            }
            PetAction.CLEANING -> {
                val droppedToyAnchor = houseSceneState.currentSceneRuntime.droppedToyAnchor
                val toyBoxAnchor = room.sceneDefinition.objects.find { it.type == com.example.lupapj.data.model.RoomObjectType.TOY_BOX }?.anchor as? FloorAnchor
                if (droppedToyAnchor != null && toyBoxAnchor != null) {
                    startToyCleanupSequence(droppedToyAnchor, toyBoxAnchor)
                } else {
                    setPetAction(PetAction.IDLE)
                }
            }
            PetAction.GROOM -> {
                setPetAction(PetAction.GROOM)
                viewModelScope.launch {
                    delay(3000L) // 몸단장 연출 시간
                    if (_uiState.value.room?.houseSceneState?.pet?.action == PetAction.GROOM) {
                        // 청결도 회복 구현
                        updateRoom { roomState ->
                            val currentPet = roomState.houseSceneState.pet
                            roomState.copy(
                                houseSceneState = roomState.houseSceneState.copy(
                                    pet = currentPet.copy(
                                        status = currentPet.status.copy(
                                            cleanliness = (currentPet.status.cleanliness + 40).coerceAtMost(100)
                                        )
                                    )
                                )
                            )
                        }
                        
                        // [추가됨] 자가 청결(그루밍) 후 변경된 수치를 서버와 접속 중 동기화
                        val pet = _uiState.value.room?.houseSceneState?.pet
                        if (pet != null) {
                            viewModelScope.launch {
                                try {
                                    val req = com.example.lupapj.data.remote.pet.PetStatusSyncRequestDto(
                                        satiety = pet.status.satiety,
                                        vitality = pet.status.vitality,
                                        cleanliness = pet.status.cleanliness,
                                        offlineSync = false
                                    )
                                    val sceneId = _uiState.value.room?.sceneDefinition?.id ?: com.example.lupapj.data.model.scene.RoomSceneId("main_room")
                                    petRepository.syncPetStatus(pet.petId, sceneId, req)
                                } catch (e: Exception) {
                                    android.util.Log.e("AppViewModel", "Failed to sync grooming status", e)
                                }
                            }
                        }

                        setPetAction(PetAction.IDLE)
                    }
                }
            }
            else -> {
                setPetAction(PetAction.IDLE)
            }
        }
    }

    private fun moveToTargetAndExecute(
        targetAnchor: FloorAnchor,
        pet: com.example.lupapj.data.model.scene.PetSceneState,
        finalAction: PetAction,
        profile: com.example.lupapj.data.model.scene.PetAutonomousMovementProfile
    ) {
        autonomousPetMovementJob?.cancel()
        autonomousPetMovementJob = viewModelScope.launch {
            // [추가됨] 기상 연출 지연
            if (pet.isLyingSide) {
                setPetAction(PetAction.IDLE)
                delay(600L)
            }
            
            movePetAutonomously(
                targetAnchor = targetAnchor,
                movementState = com.example.lupapj.data.model.scene.PetMovementState(
                    targetAnchor = targetAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = true,
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay((com.example.lupapj.data.model.scene.PET_AUTONOMOUS_MOVE_DURATION_MS / profile.speedMultiplier).toLong())
            finishAutonomousPetMovement(targetAnchor)
            setPetAction(finalAction)
        }
    }

    private fun startPetConditionTicker() {
        if (petConditionJob?.isActive == true) return

        petConditionJob = viewModelScope.launch {
            while (isActive) {
                delay(PET_CONDITION_TICK_INTERVAL_MS)
                advanceCurrentPetCondition(elapsedSeconds = 2L) 
                evaluateBehaviorTransition() 
            }
        }
    }

    private fun advanceCurrentPetCondition(elapsedSeconds: Long) {
        _uiState.update { state ->
            val room = state.room ?: return@update state
            val houseSceneState = room.houseSceneState
            val pet = houseSceneState.pet
            val result = advancePetCondition(
                status = pet.status,
                action = pet.action,
                elapsedSeconds = elapsedSeconds,
                remainder = petConditionRemainder,
                policy = DemoPetConditionPolicy
            )
            petConditionRemainder = result.remainder

            state.copy(
                room = room.copy(
                    houseSceneState = houseSceneState.copy(
                        pet = pet.copy(
                            status = result.status,
                            action = if (result.shouldStopResting) {
                                PetAction.IDLE
                            } else {
                                pet.action
                            }
                        )
                    )
                )
            )
        }
    }

    private fun movePetAutonomously(
        targetAnchor: FloorAnchor,
        movementState: PetMovementState
    ) {
        updateRoom { room ->
            val houseSceneState = room.houseSceneState
            room.copy(
                houseSceneState = houseSceneState.copy(
                    pet = houseSceneState.pet.copy(
                        action = PetAction.WALKING,
                        anchor = targetAnchor,
                        movement = movementState,
                        isLyingSide = false 
                    )
                )
            )
        }
    }

    private fun finishAutonomousPetMovement(targetAnchor: FloorAnchor) {
        updateRoom { room ->
            val houseSceneState = room.houseSceneState
            val pet = houseSceneState.pet
            if (pet.action != PetAction.WALKING || pet.movement.targetAnchor != targetAnchor) {
                return@updateRoom room
            }

            room.copy(
                houseSceneState = houseSceneState.copy(
                    pet = pet.copy(
                        action = PetAction.IDLE,
                        movement = pet.movement.copy(isMoving = false)
                    )
                )
            )
        }
    }

    private fun rememberRecentMainMenuAction(action: MainMenuAction) {
        _uiState.update { it.copy(recentMainMenuAction = action) }
    }

    private fun BottomNavItem.toMainMenuAction(): MainMenuAction {
        return when (this) {
            BottomNavItem.SHOP -> MainMenuAction.SHOP
            BottomNavItem.SCREENSHOT -> MainMenuAction.SCREENSHOT
            BottomNavItem.CONTACTS -> MainMenuAction.CONTACTS
            BottomNavItem.GALLERY -> MainMenuAction.GALLERY
        }
    }

    private fun applyRepositoryRoom(repositoryRoom: RoomUiState) {
        val currentRoom = _uiState.value.room
        val currentPet = currentRoom?.houseSceneState?.pet
        
        // [수정됨(권)] 로컬 변화량 보호 로직
        val repositoryPet = repositoryRoom.houseSceneState.pet
        val mergedPet = if (currentPet != null && repositoryPet.action == PetAction.IDLE && !repositoryPet.isLyingSide) {
            repositoryPet.copy(
                anchor = currentPet.anchor,
                action = currentPet.action,
                movement = currentPet.movement,
                isLyingSide = currentPet.isLyingSide,
                status = currentPet.status, // [수정됨] RoomRepository는 펫 상태를 관리하지 않으므로 항상 로컬 UI 상태를 유지
                traits = currentPet.traits,
                interactionEvents = currentPet.interactionEvents,
                equippedItemIds = currentPet.equippedItemIds
            )
        } else if (currentPet != null) {
            // [수정됨] IDLE 상태가 아닐 때도 장착 아이템과 펫 상태(수치)는 로컬을 유지
            repositoryPet.copy(
                equippedItemIds = currentPet.equippedItemIds,
                status = currentPet.status,
                traits = currentPet.traits,
                interactionEvents = currentPet.interactionEvents
            )
        } else {
            repositoryPet
        }

        val mergedRoom = repositoryRoom.copy(
            houseSceneState = repositoryRoom.houseSceneState.copy(pet = mergedPet),
            navBarVisible = currentRoom?.navBarVisible ?: false,
            inventoryVisible = currentRoom?.inventoryVisible ?: false
        )
        _uiState.update { it.copy(room = mergedRoom) }
    }

    private fun recoverPetSatietyFromFood() {
        petConditionRemainder = petConditionRemainder.copy(
            satietyDecaySeconds = 0L
        )
        updateRoom { room ->
            val houseSceneState = room.houseSceneState
            val pet = houseSceneState.pet
            room.copy(
                houseSceneState = houseSceneState.copy(
                    pet = pet.copy(
                        status = com.example.lupapj.data.model.applyFeedRecovery(
                            status = pet.status,
                            policy = com.example.lupapj.data.model.DemoPetConditionPolicy
                        )
                    )
                )
            )
        }
    }

    private fun calculateMovementDelay(from: FloorAnchor, to: FloorAnchor, profile: com.example.lupapj.data.model.scene.PetAutonomousMovementProfile): Long {
        val room = _uiState.value.room ?: return 1000L
        val dist = calculateTileDistance(from, to, room.sceneDefinition.projectionSpec)
        val duration = (dist * 400L / profile.speedMultiplier).toLong()
        return duration.coerceIn(300L, 3000L)
    }

    private fun startEatingSequence(targetAnchor: FloorAnchor, skipMovement: Boolean = false) {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.traits)

            if (!skipMovement) {
                if (pet.isLyingSide) {
                    setPetAction(PetAction.IDLE)
                    delay(600L)
                }

                val delayMs = calculateMovementDelay(pet.anchor, targetAnchor, profile)
                movePetAutonomously(
                    targetAnchor = targetAnchor,
                    movementState = com.example.lupapj.data.model.scene.PetMovementState(
                        targetAnchor = targetAnchor,
                        isMoving = true,
                        style = profile.style,
                        isAutonomous = true,
                        speedMultiplier = profile.speedMultiplier,
                        bouncePx = profile.bouncePx
                    )
                )
                delay(delayMs)
                finishAutonomousPetMovement(targetAnchor)
            }

            setPetAction(PetAction.EATING)
            delay(6000L) 

            if (_uiState.value.room?.houseSceneState?.pet?.action == PetAction.EATING) {
                val nextRoom = roomRepository.consumeFood()
                applyRepositoryRoom(nextRoom)
                recoverPetSatietyFromFood()
                setPetAction(PetAction.IDLE)
            }
        }
    }

    private fun startPlayingSequence(targetAnchor: FloorAnchor) {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.traits)

            val delayMs = calculateMovementDelay(pet.anchor, targetAnchor, profile)
            movePetAutonomously(
                targetAnchor = targetAnchor,
                movementState = com.example.lupapj.data.model.scene.PetMovementState(
                    targetAnchor = targetAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = true,
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay(delayMs)
            finishAutonomousPetMovement(targetAnchor)

            setPetAction(PetAction.PLAYING)
            delay(6000L) 

            if (_uiState.value.room?.houseSceneState?.pet?.action == PetAction.PLAYING) {
                val nextRoom = roomRepository.updateToyKnockedOver(true)
                applyRepositoryRoom(nextRoom)
                setPetAction(PetAction.IDLE)
            }
        }
    }

    private fun startToyCleanupSequence(
        droppedToyAnchor: FloorAnchor,
        toyBoxAnchor: FloorAnchor
    ) {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            var pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.traits)

            if (pet.isLyingSide) {
                setPetAction(PetAction.IDLE)
                delay(600L)
                pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            }

            _uiState.update { it.copy(placeholderMessage = "장난감을 정리하러 갑니다.") }

            val moveToToyDelayMs = calculateMovementDelay(pet.anchor, droppedToyAnchor, profile)
            movePetAutonomously(
                targetAnchor = droppedToyAnchor,
                movementState = PetMovementState(
                    targetAnchor = droppedToyAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = false,
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay(moveToToyDelayMs)
            finishAutonomousPetMovement(droppedToyAnchor)

            setPetAction(PetAction.CLEANING)
            delay(450L)

            val pickedUpRoom = roomRepository.cleanupToy()
            applyRepositoryRoom(pickedUpRoom)

            val petAtToy = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val moveToBoxDelayMs = calculateMovementDelay(petAtToy.anchor, toyBoxAnchor, profile)
            movePetAutonomously(
                targetAnchor = toyBoxAnchor,
                movementState = PetMovementState(
                    targetAnchor = toyBoxAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = false,
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay(moveToBoxDelayMs)
            finishAutonomousPetMovement(toyBoxAnchor)

            setPetAction(PetAction.CLEANING)
            delay(350L)
            setPetAction(PetAction.IDLE)
            _uiState.update { it.copy(placeholderMessage = "장난감을 장난감 상자에 정리했어요.") }
        }
    }

    private fun startBedRestingSequence(targetAnchor: FloorAnchor) {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.traits)

            val delayMs = calculateMovementDelay(pet.anchor, targetAnchor, profile)
            movePetAutonomously(
                targetAnchor = targetAnchor,
                movementState = com.example.lupapj.data.model.scene.PetMovementState(
                    targetAnchor = targetAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = false, 
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay(delayMs)
            finishAutonomousPetMovement(targetAnchor)

            _uiState.update { state ->
                val room = state.room ?: return@update state
                state.copy(
                    room = room.copy(
                        houseSceneState = room.houseSceneState.copy(
                            pet = room.houseSceneState.pet.copy(
                                action = PetAction.BED_RESTING,
                                isLyingSide = true
                            )
                        )
                    )
                )
            }
        }
    }

    fun applyOfflineProgression(elapsedSeconds: Long): String {
        if (elapsedSeconds <= 0L) return ""
        
        val room = _uiState.value.room ?: return ""
        val pet = room.houseSceneState.pet
        val currentStatus = pet.status
        val traits = pet.traits
        
        // 평균 변화율 산정 (현재 수치 기반 및 캐릭터 특성 반영)
        // - 식욕(appetite)이 높을수록 포만감(satiety)이 더 빨리 떨어짐
        // - 활동성(activity)이 높을수록 활력(vitality)과 청결도(cleanliness)가 더 빨리 떨어짐
        val appetiteMod = 0.5f + traits.appetite // 0.5 ~ 1.5배 보정
        val activityMod = 0.5f + traits.activity // 0.5 ~ 1.5배 보정
        
        val satietyDecay = ((elapsedSeconds / 600L) * appetiteMod).toInt().coerceIn(0, currentStatus.satiety) // 기준: 10분당 1
        val vitalityDecay = ((elapsedSeconds / 900L) * activityMod).toInt().coerceIn(0, currentStatus.vitality) // 기준: 15분당 1
        val cleanlinessDecay = ((elapsedSeconds / 1800L) * activityMod).toInt().coerceIn(0, currentStatus.cleanliness) // 기준: 30분당 1
        
        val nextStatus = currentStatus.copy(
            satiety = currentStatus.satiety - satietyDecay,
            vitality = currentStatus.vitality - vitalityDecay,
            cleanliness = currentStatus.cleanliness - cleanlinessDecay
        )
        
        // 부재중 행동 예측 일지 (Missed You Log)
        val logMessage = buildString {
            if (vitalityDecay > 10) {
                append("방을 뽈뽈거리며 돌아다녔어요! ")
            } else if (vitalityDecay > 3) {
                append("혼자서도 얌전히 놀았어요. ")
            } else {
                append("계속 쿨쿨 잠을 잤나봐요. ")
            }
            
            if (satietyDecay > 15) {
                append("\n배가 많이 고파요! ")
            } else if (satietyDecay > 5) {
                append("\n배가 살짝 출출해요. ")
            }
            
            if (cleanlinessDecay > 5) {
                append("\n조금 꼬질꼬질해졌어요.")
            }
        }.trimEnd()

        val updatedRoom = room.copy(
            houseSceneState = room.houseSceneState.copy(
                pet = pet.copy(status = nextStatus)
            )
        )
        
        _uiState.update { it.copy(room = updatedRoom) }

        // [추가됨] 백엔드 서버에 오프라인 감가 수치 강제 동기화 (오프라인 플래그 켜서)
        viewModelScope.launch {
            try {
                val req = com.example.lupapj.data.remote.pet.PetStatusSyncRequestDto(
                    satiety = nextStatus.satiety,
                    vitality = nextStatus.vitality,
                    cleanliness = nextStatus.cleanliness,
                    offlineSync = true
                )
                petRepository.syncPetStatus(pet.petId, room.sceneDefinition.id, req)
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Failed to sync offline status to server", e)
            }
        }

        return logMessage
    }

    private fun setPetAction(action: PetAction) {
        _uiState.update { state ->
            val room = state.room ?: return@update state
            state.copy(
                room = room.copy(
                    houseSceneState = room.houseSceneState.copy(
                        pet = room.houseSceneState.pet.copy(
                            action = action,
                            isLyingSide = false 
                        )
                    )
                )
            )
        }
    }

    // [추가됨(권)] 랜덤 수치 디버그용
    fun randomizeTraitsDebug() {
        val random = kotlin.random.Random.Default
        val newTraits = com.example.lupapj.data.model.PetTraits(
            activity = random.nextFloat(),
            patience = random.nextFloat(),
            curiosity = random.nextFloat(),
            appetite = random.nextFloat(),
            attention = random.nextFloat()
        )
        
        _uiState.update { state ->
            val room = state.room ?: return@update state
            state.copy(
                room = room.copy(
                    houseSceneState = room.houseSceneState.copy(
                        pet = room.houseSceneState.pet.copy(traits = newTraits)
                    )
                )
            )
        }
        
        // Sync to backend
        viewModelScope.launch {
            try {
                val room = _uiState.value.room ?: return@launch
                val petId = room.houseSceneState.pet.petId
                val req = com.example.lupapj.data.remote.pet.PetTraitsDto(
                    activity = newTraits.activity,
                    patience = newTraits.patience,
                    curiosity = newTraits.curiosity,
                    appetite = newTraits.appetite,
                    attention = newTraits.attention
                )
                petRepository.updateTraitsDebug(petId, room.sceneDefinition.id, req)
                android.util.Log.d("AppViewModel", "Synced random traits to server")
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Failed to sync random traits", e)
            }
        }
    }

    // [추가됨(권)] 수치 기본값 롤백용
    fun resetTraitsDebug() {
        val defaultTraits = com.example.lupapj.data.model.PetTraits(
            activity = 0.5f,
            patience = 0.5f,
            curiosity = 0.5f,
            appetite = 0.5f,
            attention = 0.5f
        )
        
        _uiState.update { state ->
            val room = state.room ?: return@update state
            state.copy(
                room = room.copy(
                    houseSceneState = room.houseSceneState.copy(
                        pet = room.houseSceneState.pet.copy(traits = defaultTraits)
                    )
                )
            )
        }
        
        viewModelScope.launch {
            try {
                val room = _uiState.value.room ?: return@launch
                val petId = room.houseSceneState.pet.petId
                val req = com.example.lupapj.data.remote.pet.PetTraitsDto(
                    activity = defaultTraits.activity,
                    patience = defaultTraits.patience,
                    curiosity = defaultTraits.curiosity,
                    appetite = defaultTraits.appetite,
                    attention = defaultTraits.attention
                )
                petRepository.updateTraitsDebug(petId, room.sceneDefinition.id, req)
                android.util.Log.d("AppViewModel", "Synced default traits to server")
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Failed to sync default traits", e)
            }
        }
    }

    override fun onCleared() {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        friendMessagePollingJob?.cancel()
        homeVisitPollingJob?.cancel()
        petConditionJob?.cancel()
        plazaJoinJob?.cancel()
        plazaMessageSendJob?.cancel()
        plazaSnapshotPollingJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val roomRepository: RoomRepository,
        private val galleryRepository: GalleryRepository, 
        private val friendRepository: FriendRepository,
        private val plazaRepository: PlazaRepository,
        private val currencyRepository: CurrencyRepository, 
        private val shopRepository: ShopRepository ,
        private val contestRepository: ContestRepository,
        private val petRepository: com.example.lupapj.data.repository.PetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                authRepository = authRepository,
                roomRepository = roomRepository,
                galleryRepository = galleryRepository,
                friendRepository = friendRepository,
                plazaRepository = plazaRepository,
                currencyRepository = currencyRepository,
                shopRepository = shopRepository,
                contestRepository = contestRepository,
                petRepository = petRepository
            ) as T
        }
    }
}

// [보존] 팀원 작업 내용 (하단 클래스 및 확장 함수 복원)
private data class PlazaParticipantProfile(
    val userId: String,
    val nickname: String,
    val pet: PlazaPetSnapshot
)

private fun PetUiState.toPlazaPetSnapshot(ownerUserId: String): PlazaPetSnapshot {
    return PlazaPetSnapshot(
        petId = petId,
        ownerUserId = ownerUserId,
        name = name,
        characterAssetKey = characterAssetKey,
        appearance = appearance,
        status = status,
        traits = traits,
        equippedItemIds = equippedItemIds
    )
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
        FriendOperationFailure.ALREADY_FRIENDS -> "이미 친구를 맺었어요."
        FriendOperationFailure.REQUEST_ALREADY_SENT -> "이미 친구 요청을 보냈어요."
        FriendOperationFailure.REQUEST_ALREADY_RECEIVED -> "이미 받은 요청이 있어요."
        FriendOperationFailure.REQUEST_NOT_FOUND -> "친구 요청을 찾을 수 없어요."
        FriendOperationFailure.REQUEST_NOT_PENDING -> "이미 처리된 요청이에요."
        FriendOperationFailure.FRIEND_NOT_FOUND -> "친구를 찾을 수 없어요."
        FriendOperationFailure.NOT_FRIENDS -> "친구 집은 친구만 방문할 수 있어요."
        FriendOperationFailure.HOME_INVITATION_ALREADY_SENT -> "이미 보낸 집 초대가 있어요."
        FriendOperationFailure.HOME_INVITATION_NOT_FOUND -> "집 초대를 찾을 수 없어요."
        FriendOperationFailure.HOME_INVITATION_NOT_PENDING -> "이미 처리된 집 초대예요."
        FriendOperationFailure.NOT_HOME_INVITATION_RECEIVER -> "내가 받은 집 초대만 수락할 수 있어요."
        FriendOperationFailure.NOT_HOME_INVITATION_SENDER -> "내가 보낸 집 초대만 취소할 수 있어요."
        FriendOperationFailure.FRIEND_HOME_UNAVAILABLE -> "친구 집을 불러올 수 없어요."
        FriendOperationFailure.HOME_VISIT_ALREADY_ACTIVE -> "이미 진행 중인 친구 집 방문이 있어요."
        FriendOperationFailure.HOME_VISIT_NOT_FOUND -> "친구 집 방문을 찾을 수 없어요."
        FriendOperationFailure.HOME_VISIT_NOT_ACTIVE -> "이미 종료된 친구 집 방문이에요."
        FriendOperationFailure.NOT_HOME_VISIT_PARTICIPANT -> "해당 친구 집 방문에 참여 중이 아니에요."
        FriendOperationFailure.BLOCKED -> "초대를 수락해야 친구 집에 방문할 수 있어요."
        FriendOperationFailure.UNKNOWN -> "친구 기능을 잠시 사용할 수 없어요."
    }

private val PlazaOperationFailure.message: String
    get() = when (this) {
        PlazaOperationFailure.EMPTY_CODE -> "광장 코드를 입력해주세요."
        PlazaOperationFailure.INVALID_CODE -> "광장 코드는 PZ-0000 형식으로 입력해주세요."
        PlazaOperationFailure.PLAZA_NOT_FOUND -> "해당 광장을 찾을 수 없어요."
        PlazaOperationFailure.PLAZA_FULL -> "광장이 가득 찼어요."
        PlazaOperationFailure.EMPTY_MESSAGE -> "메시지를 입력해주세요."
        PlazaOperationFailure.MESSAGE_TOO_LONG -> "메시지는 ${PLAZA_MESSAGE_MAX_LENGTH}자까지 보낼 수 있어요."
        PlazaOperationFailure.NOT_IN_PLAZA -> "입장한 광장이 없어요."
        PlazaOperationFailure.UNAUTHORIZED -> "로그인이 필요해요."
        PlazaOperationFailure.UNKNOWN -> "광장 기능을 잠시 사용할 수 없어요."
    }
