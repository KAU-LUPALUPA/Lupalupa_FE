package com.example.lupapj.data.remote.friend

import com.example.lupapj.data.model.friend.FRIEND_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.friend.FriendCode
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.repository.FriendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RemoteFriendRepository(
    private val apiClient: FriendApiClient,
    initialCurrentUser: FriendUser,
    private val sceneResolver: (String) -> RoomSceneDefinition
) : FriendRepository {
    private val _myProfile = MutableStateFlow(initialCurrentUser)
    override val myProfile: StateFlow<FriendUser> = _myProfile.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendSummary>>(emptyList())
    override val friends: StateFlow<List<FriendSummary>> = _friends.asStateFlow()

    private val _receivedRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    override val receivedRequests: StateFlow<List<FriendRequest>> = _receivedRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    override val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests.asStateFlow()

    private val _receivedHomeInvitations = MutableStateFlow<List<FriendHomeInvitation>>(emptyList())
    override val receivedHomeInvitations: StateFlow<List<FriendHomeInvitation>> =
        _receivedHomeInvitations.asStateFlow()

    private val _friendMessages = MutableStateFlow<Map<String, List<FriendMessage>>>(emptyMap())
    override val friendMessages: StateFlow<Map<String, List<FriendMessage>>> =
        _friendMessages.asStateFlow()

    override fun updateCurrentUser(userId: String?, nickname: String?) {
        _myProfile.update { current ->
            current.copy(
                userId = userId?.takeIf { it.isNotBlank() } ?: current.userId,
                nickname = nickname?.takeIf { it.isNotBlank() } ?: current.nickname
            )
        }
    }

    override suspend fun refreshFriendOverview(): FriendOperationResult<Unit> {
        return apiCall {
            val currentProfile = apiClient.getMyFriendCode().applyTo(_myProfile.value)
            val friends = apiClient.getFriends().friends.map { it.toDomain() }
            val receivedRequests = apiClient.getReceivedFriendRequests().requests.map { it.toDomain() }
            val sentRequests = apiClient.getSentFriendRequests().requests.map { it.toDomain() }
            val receivedHomeInvitations = loadReceivedHomeInvitationsOrNull()

            _myProfile.value = currentProfile
            _friends.value = friends
            _receivedRequests.value = receivedRequests
            _sentRequests.value = sentRequests
            receivedHomeInvitations?.let { _receivedHomeInvitations.value = it }
        }
    }

    override suspend fun findUserByFriendCode(friendCodeInput: String): FriendUser? {
        val friendCode = FriendCode.fromInput(friendCodeInput)?.value ?: return null
        return runCatching {
            apiClient.findUserByFriendCode(friendCode).user?.toDomain()
        }.getOrNull()
    }

    override suspend fun sendFriendRequest(
        friendCodeInput: String
    ): FriendOperationResult<FriendRequest> {
        val friendCode = FriendCode.fromInput(friendCodeInput)
            ?: return FriendOperationResult.Failure(FriendOperationFailure.EMPTY_CODE)

        return apiCall {
            val request = apiClient
                .sendFriendRequest(SendFriendRequestDto(friendCode = friendCode.value))
                .request
                .toDomain()
            _sentRequests.update { listOf(request) + it.filterNot { existing -> existing.id == request.id } }
            request
        }
    }

    override suspend fun acceptFriendRequest(
        requestId: String
    ): FriendOperationResult<FriendSummary> {
        return apiCall {
            val response = apiClient.acceptFriendRequest(requestId)
            val friendship = response.friendship.toDomain()
            _receivedRequests.update { requests -> requests.filterNot { it.id == requestId } }
            _friends.update { listOf(friendship) + it.filterNot { friend ->
                friend.user.userId == friendship.user.userId
            } }
            friendship
        }
    }

    override suspend fun rejectFriendRequest(
        requestId: String
    ): FriendOperationResult<FriendRequest> {
        return apiCall {
            val request = apiClient.rejectFriendRequest(requestId).request.toDomain()
            _receivedRequests.update { requests -> requests.filterNot { it.id == requestId } }
            request
        }
    }

    override suspend fun cancelFriendRequest(
        requestId: String
    ): FriendOperationResult<FriendRequest> {
        return apiCall {
            val request = apiClient.cancelFriendRequest(requestId).request.toDomain()
            _sentRequests.update { requests -> requests.filterNot { it.id == requestId } }
            request
        }
    }

    override suspend fun removeFriend(
        friendUserId: String
    ): FriendOperationResult<FriendSummary> {
        return apiCall {
            val removedFriend = _friends.value.firstOrNull { it.user.userId == friendUserId }
                ?: throw FriendApiException(code = "FRIEND_NOT_FOUND", httpStatus = 404)
            apiClient.deleteFriend(friendUserId)
            _friends.update { friends -> friends.filterNot { it.user.userId == friendUserId } }
            removedFriend
        }
    }

    override suspend fun getFriendHome(
        friendUserId: String
    ): FriendOperationResult<FriendHome> {
        return FriendOperationResult.Failure(FriendOperationFailure.BLOCKED)
    }

    override suspend fun acceptHomeInvitation(
        invitationId: String
    ): FriendOperationResult<FriendHome> {
        return apiCall {
            val home = apiClient
                .acceptHomeInvitation(invitationId)
                .toDomain(sceneResolver = sceneResolver)
            _receivedHomeInvitations.update { invitations ->
                invitations.filterNot { it.id == invitationId }
            }
            home
        }
    }

    override suspend fun rejectHomeInvitation(
        invitationId: String
    ): FriendOperationResult<FriendHomeInvitation> {
        return apiCall {
            val invitation = apiClient.rejectHomeInvitation(invitationId).invitation.toDomain()
            _receivedHomeInvitations.update { invitations ->
                invitations.filterNot { it.id == invitationId }
            }
            invitation
        }
    }

    override suspend fun getFriendMessages(
        friendUserId: String
    ): FriendOperationResult<List<FriendMessage>> {
        return apiCall {
            val messages = apiClient
                .getFriendMessages(friendUserId = friendUserId)
                .messages
                .map { it.toDomain(currentUserId = _myProfile.value.userId) }
            _friendMessages.update { it + (friendUserId to messages) }
            messages
        }
    }

    override suspend fun sendFriendMessage(
        friendUserId: String,
        message: String
    ): FriendOperationResult<FriendMessage> {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) {
            return FriendOperationResult.Failure(FriendOperationFailure.EMPTY_MESSAGE)
        }
        if (trimmedMessage.length > FRIEND_MESSAGE_MAX_LENGTH) {
            return FriendOperationResult.Failure(FriendOperationFailure.MESSAGE_TOO_LONG)
        }

        return apiCall {
            val sentMessage = apiClient
                .sendFriendMessage(
                    friendUserId = friendUserId,
                    request = SendFriendMessageRequestDto(text = trimmedMessage)
                )
                .message
                .toDomain(currentUserId = _myProfile.value.userId)
            _friendMessages.update { messagesByFriend ->
                val currentMessages = messagesByFriend[friendUserId].orEmpty()
                messagesByFriend + (friendUserId to (currentMessages + sentMessage))
            }
            sentMessage
        }
    }

    private suspend fun <T> apiCall(block: suspend () -> T): FriendOperationResult<T> {
        return try {
            FriendOperationResult.Success(block())
        } catch (exception: FriendApiException) {
            FriendOperationResult.Failure(exception.toFriendOperationFailure())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            FriendOperationResult.Failure(FriendOperationFailure.UNKNOWN)
        }
    }

    private suspend fun loadReceivedHomeInvitationsOrNull(): List<FriendHomeInvitation>? {
        return try {
            apiClient
                .getReceivedHomeInvitations()
                .invitations
                .map { it.toDomain() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }
}
