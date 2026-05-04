package com.example.lupapj.data.mock

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.friend.FRIEND_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.friend.FriendCode
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendMessageSender
import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendRequestStatus
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.data.model.friend.FriendshipStatus
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.initialHouseSceneState
import com.example.lupapj.data.repository.FriendRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DemoFriendUsers {
    val me = FriendUser(
        userId = "user_me",
        nickname = "루파",
        friendCode = FriendCode.fromInput("LUPA-ME01") ?: error("Invalid demo friend code")
    )

    val alreadyFriend = FriendUser(
        userId = "user_mina",
        nickname = "미나",
        friendCode = FriendCode.fromInput("LUPA-8K2FQ") ?: error("Invalid demo friend code")
    )

    val incomingRequester = FriendUser(
        userId = "user_haru",
        nickname = "하루",
        friendCode = FriendCode.fromInput("LUPA-2H4RU") ?: error("Invalid demo friend code")
    )

    val requestableUser = FriendUser(
        userId = "user_bori",
        nickname = "보리",
        friendCode = FriendCode.fromInput("LUPA-5B0RI") ?: error("Invalid demo friend code")
    )

    val allRemoteUsers = listOf(
        alreadyFriend,
        incomingRequester,
        requestableUser
    )
}

class MockFriendRepository(
    currentUser: FriendUser = DemoFriendUsers.me,
    remoteUsers: List<FriendUser> = DemoFriendUsers.allRemoteUsers,
    initialFriends: List<FriendUser> = listOf(DemoFriendUsers.alreadyFriend),
    initialReceivedRequestUsers: List<FriendUser> = listOf(DemoFriendUsers.incomingRequester),
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val simulatedDelayMillis: Long = 120L
) : FriendRepository {
    private val _myProfile = MutableStateFlow(currentUser)
    override val myProfile: StateFlow<FriendUser> = _myProfile.asStateFlow()

    private val usersByCode = (remoteUsers + currentUser).associateBy { it.friendCode.value }
    private val friendRoomsByUserId = remoteUsers.associate { user ->
        user.userId to createFriendRoom(user)
    }
    private var nextRequestSequence = 1
    private var nextMessageSequence = 1

    private val _friends = MutableStateFlow(
        initialFriends.map { friend ->
            FriendSummary(
                user = friend,
                friendsSinceMillis = nowProvider()
            )
        }
    )
    override val friends: StateFlow<List<FriendSummary>> = _friends.asStateFlow()

    private val _receivedRequests = MutableStateFlow(
        initialReceivedRequestUsers.map { requester ->
            createRequest(
                fromUser = requester,
                toUser = currentUser
            )
        }
    )
    override val receivedRequests: StateFlow<List<FriendRequest>> = _receivedRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    override val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests.asStateFlow()

    private val _friendMessages = MutableStateFlow(createInitialMessages(remoteUsers))
    override val friendMessages: StateFlow<Map<String, List<FriendMessage>>> =
        _friendMessages.asStateFlow()

    override suspend fun findUserByFriendCode(friendCodeInput: String): FriendUser? {
        simulateLatency()
        val code = FriendCode.fromInput(friendCodeInput) ?: return null
        return usersByCode[code.value]?.takeUnless { it.userId == myProfile.value.userId }
    }

    override suspend fun sendFriendRequest(
        friendCodeInput: String
    ): FriendOperationResult<FriendRequest> {
        simulateLatency()

        val code = FriendCode.fromInput(friendCodeInput)
            ?: return FriendOperationResult.Failure(FriendOperationFailure.EMPTY_CODE)
        val currentUser = myProfile.value

        if (code.value == currentUser.friendCode.value) {
            return FriendOperationResult.Failure(FriendOperationFailure.SELF_CODE)
        }

        val targetUser = usersByCode[code.value]
            ?: return FriendOperationResult.Failure(FriendOperationFailure.USER_NOT_FOUND)

        if (_friends.value.any { it.user.userId == targetUser.userId }) {
            return FriendOperationResult.Failure(FriendOperationFailure.ALREADY_FRIENDS)
        }

        if (_sentRequests.value.anyPendingWith(targetUser.userId)) {
            return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_ALREADY_SENT)
        }

        if (_receivedRequests.value.anyPendingFrom(targetUser.userId)) {
            return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_ALREADY_RECEIVED)
        }

        val request = createRequest(
            fromUser = currentUser,
            toUser = targetUser
        )
        _sentRequests.update { listOf(request) + it }
        return FriendOperationResult.Success(request)
    }

    override suspend fun acceptFriendRequest(
        requestId: String
    ): FriendOperationResult<FriendSummary> {
        simulateLatency()

        val request = _receivedRequests.value.firstOrNull { it.id == requestId }
            ?: return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_NOT_FOUND)

        if (request.status != FriendRequestStatus.PENDING) {
            return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_NOT_PENDING)
        }

        if (_friends.value.any { it.user.userId == request.fromUser.userId }) {
            _receivedRequests.update { requests -> requests.filterNot { it.id == requestId } }
            return FriendOperationResult.Failure(FriendOperationFailure.ALREADY_FRIENDS)
        }

        val friend = FriendSummary(
            user = request.fromUser,
            status = FriendshipStatus.ACCEPTED,
            friendsSinceMillis = nowProvider()
        )
        _receivedRequests.update { requests -> requests.filterNot { it.id == requestId } }
        _friends.update { listOf(friend) + it }
        return FriendOperationResult.Success(friend)
    }

    override suspend fun rejectFriendRequest(
        requestId: String
    ): FriendOperationResult<FriendRequest> {
        simulateLatency()

        val request = _receivedRequests.value.firstOrNull { it.id == requestId }
            ?: return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_NOT_FOUND)

        if (request.status != FriendRequestStatus.PENDING) {
            return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_NOT_PENDING)
        }

        _receivedRequests.update { requests -> requests.filterNot { it.id == requestId } }
        return FriendOperationResult.Success(
            request.copy(
                status = FriendRequestStatus.REJECTED,
                respondedAtMillis = nowProvider()
            )
        )
    }

    override suspend fun cancelFriendRequest(
        requestId: String
    ): FriendOperationResult<FriendRequest> {
        simulateLatency()

        val request = _sentRequests.value.firstOrNull { it.id == requestId }
            ?: return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_NOT_FOUND)

        if (request.status != FriendRequestStatus.PENDING) {
            return FriendOperationResult.Failure(FriendOperationFailure.REQUEST_NOT_PENDING)
        }

        _sentRequests.update { requests -> requests.filterNot { it.id == requestId } }
        return FriendOperationResult.Success(
            request.copy(
                status = FriendRequestStatus.CANCELED,
                respondedAtMillis = nowProvider()
            )
        )
    }

    override suspend fun removeFriend(
        friendUserId: String
    ): FriendOperationResult<FriendSummary> {
        simulateLatency()

        val friend = _friends.value.firstOrNull { it.user.userId == friendUserId }
            ?: return FriendOperationResult.Failure(FriendOperationFailure.FRIEND_NOT_FOUND)

        _friends.update { friends -> friends.filterNot { it.user.userId == friendUserId } }
        return FriendOperationResult.Success(friend)
    }

    override suspend fun getFriendHome(
        friendUserId: String
    ): FriendOperationResult<FriendHome> {
        simulateLatency()

        val friend = _friends.value.firstOrNull { it.user.userId == friendUserId }
            ?: return FriendOperationResult.Failure(FriendOperationFailure.NOT_FRIENDS)
        val room = friendRoomsByUserId[friendUserId]
            ?: return FriendOperationResult.Failure(FriendOperationFailure.FRIEND_HOME_UNAVAILABLE)

        return FriendOperationResult.Success(
            FriendHome(
                owner = friend.user,
                room = room,
                visitedAtMillis = nowProvider()
            )
        )
    }

    override suspend fun getFriendMessages(
        friendUserId: String
    ): FriendOperationResult<List<FriendMessage>> {
        simulateLatency()

        if (!_friends.value.any { it.user.userId == friendUserId }) {
            return FriendOperationResult.Failure(FriendOperationFailure.NOT_FRIENDS)
        }

        return FriendOperationResult.Success(_friendMessages.value[friendUserId].orEmpty())
    }

    override suspend fun sendFriendMessage(
        friendUserId: String,
        message: String
    ): FriendOperationResult<FriendMessage> {
        simulateLatency()

        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) {
            return FriendOperationResult.Failure(FriendOperationFailure.EMPTY_MESSAGE)
        }
        if (trimmedMessage.length > FRIEND_MESSAGE_MAX_LENGTH) {
            return FriendOperationResult.Failure(FriendOperationFailure.MESSAGE_TOO_LONG)
        }
        if (!_friends.value.any { it.user.userId == friendUserId }) {
            return FriendOperationResult.Failure(FriendOperationFailure.NOT_FRIENDS)
        }

        val sentMessage = createMessage(
            friendUserId = friendUserId,
            sender = FriendMessageSender.ME,
            text = trimmedMessage
        )
        val replyMessage = createMessage(
            friendUserId = friendUserId,
            sender = FriendMessageSender.FRIEND,
            text = "좋아! 다음에 또 놀러 와."
        )

        _friendMessages.update { messagesByFriend ->
            val currentMessages = messagesByFriend[friendUserId].orEmpty()
            messagesByFriend + (friendUserId to (currentMessages + sentMessage + replyMessage))
        }

        return FriendOperationResult.Success(sentMessage)
    }

    private fun createRequest(
        fromUser: FriendUser,
        toUser: FriendUser
    ): FriendRequest {
        return FriendRequest(
            id = "friend-request-${nextRequestSequence++}",
            fromUser = fromUser,
            toUser = toUser,
            createdAtMillis = nowProvider()
        )
    }

    private fun createInitialMessages(
        remoteUsers: List<FriendUser>
    ): Map<String, List<FriendMessage>> {
        return remoteUsers.associate { user ->
            val messages = when (user.userId) {
                DemoFriendUsers.alreadyFriend.userId -> listOf(
                    createMessage(
                        friendUserId = user.userId,
                        sender = FriendMessageSender.FRIEND,
                        text = "어서 와! 우리 집 구경해."
                    ),
                    createMessage(
                        friendUserId = user.userId,
                        sender = FriendMessageSender.ME,
                        text = "인테리어 귀엽다."
                    )
                )

                else -> emptyList()
            }
            user.userId to messages
        }
    }

    private fun createMessage(
        friendUserId: String,
        sender: FriendMessageSender,
        text: String
    ): FriendMessage {
        return FriendMessage(
            id = "friend-message-${nextMessageSequence++}",
            friendUserId = friendUserId,
            sender = sender,
            text = text,
            sentAtMillis = nowProvider()
        )
    }

    private suspend fun simulateLatency() {
        if (simulatedDelayMillis > 0) {
            delay(simulatedDelayMillis)
        }
    }

    private fun createFriendRoom(user: FriendUser) = when (user.userId) {
        DemoFriendUsers.alreadyFriend.userId -> initialRoomUiState(
            sceneDefinition = DemoScenes.mainRoom,
            houseSceneState = initialHouseSceneState(
                sceneId = DemoScenes.mainRoom.id,
                petAnchor = FloorAnchor(u = 0.58f, v = 0.52f),
                petAction = PetAction.RESTING
            )
        )

        DemoFriendUsers.incomingRequester.userId -> initialRoomUiState(
            sceneDefinition = DemoScenes.sideRoom,
            houseSceneState = initialHouseSceneState(
                sceneId = DemoScenes.sideRoom.id,
                petAnchor = FloorAnchor(u = 0.42f, v = 0.68f),
                petAction = PetAction.PLAYING
            )
        )

        else -> initialRoomUiState(
            sceneDefinition = DemoScenes.mainRoom,
            houseSceneState = initialHouseSceneState(
                sceneId = DemoScenes.mainRoom.id,
                petAnchor = FloorAnchor(u = 0.36f, v = 0.72f),
                petAction = PetAction.IDLE
            )
        )
    }
}

private fun List<FriendRequest>.anyPendingWith(userId: String): Boolean {
    return any { request ->
        request.status == FriendRequestStatus.PENDING && request.toUser.userId == userId
    }
}

private fun List<FriendRequest>.anyPendingFrom(userId: String): Boolean {
    return any { request ->
        request.status == FriendRequestStatus.PENDING && request.fromUser.userId == userId
    }
}
