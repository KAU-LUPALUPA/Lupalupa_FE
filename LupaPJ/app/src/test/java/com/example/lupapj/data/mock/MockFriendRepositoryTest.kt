package com.example.lupapj.data.mock

import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.friend.FriendMessageSender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockFriendRepositoryTest {
    @Test
    fun sendFriendRequest_normalizesCodeAndAddsSentRequest() = runBlocking {
        val repository = createRepository(
            initialFriends = emptyList(),
            initialReceivedRequestUsers = emptyList()
        )

        val result = repository.sendFriendRequest("  5b0ri  ")

        assertTrue(result is FriendOperationResult.Success)
        val request = (result as FriendOperationResult.Success).value
        assertEquals(DemoFriendUsers.me.userId, request.fromUser.userId)
        assertEquals(DemoFriendUsers.requestableUser.userId, request.toUser.userId)
        assertEquals(listOf(request), repository.sentRequests.value)
    }

    @Test
    fun sendFriendRequest_rejectsInvalidRelationshipCases() = runBlocking {
        val repository = createRepository()

        assertFailure(
            expected = FriendOperationFailure.EMPTY_CODE,
            result = repository.sendFriendRequest("")
        )
        assertFailure(
            expected = FriendOperationFailure.SELF_CODE,
            result = repository.sendFriendRequest(DemoFriendUsers.me.displayFriendCode)
        )
        assertFailure(
            expected = FriendOperationFailure.ALREADY_FRIENDS,
            result = repository.sendFriendRequest(DemoFriendUsers.alreadyFriend.displayFriendCode)
        )
        assertFailure(
            expected = FriendOperationFailure.REQUEST_ALREADY_RECEIVED,
            result = repository.sendFriendRequest(DemoFriendUsers.incomingRequester.displayFriendCode)
        )
    }

    @Test
    fun acceptFriendRequest_movesRequesterToFriendsAndClearsPendingRequest() = runBlocking {
        val repository = createRepository()
        val request = repository.receivedRequests.value.single()

        val result = repository.acceptFriendRequest(request.id)

        assertTrue(result is FriendOperationResult.Success)
        val acceptedFriend = (result as FriendOperationResult.Success).value
        assertEquals(DemoFriendUsers.incomingRequester.userId, acceptedFriend.user.userId)
        assertTrue(repository.receivedRequests.value.isEmpty())
        assertTrue(
            repository.friends.value.any {
                it.user.userId == DemoFriendUsers.incomingRequester.userId
            }
        )
    }

    @Test
    fun getFriendHome_rejectsDirectVisitsEvenForAcceptedFriends() = runBlocking {
        val repository = createRepository()

        val result = repository.getFriendHome(DemoFriendUsers.alreadyFriend.userId)

        assertFailure(
            expected = FriendOperationFailure.BLOCKED,
            result = result
        )
    }

    @Test
    fun acceptHomeInvitation_returnsFriendRoomAndClearsInvitation() = runBlocking {
        val repository = createRepository()
        val invitation = repository.receivedHomeInvitations.value.single()

        val result = repository.acceptHomeInvitation(invitation.id)

        assertTrue(result is FriendOperationResult.Success)
        val session = (result as FriendOperationResult.Success).value
        assertEquals(DemoFriendUsers.alreadyFriend.userId, session.hostUser.userId)
        assertEquals(DemoScenes.mainRoom.id, session.hostHome?.room?.sceneDefinition?.id)
        assertTrue(repository.receivedHomeInvitations.value.isEmpty())
    }

    @Test
    fun rejectHomeInvitation_clearsPendingInvitation() = runBlocking {
        val repository = createRepository()
        val invitation = repository.receivedHomeInvitations.value.single()

        val result = repository.rejectHomeInvitation(invitation.id)

        assertTrue(result is FriendOperationResult.Success)
        assertTrue(repository.receivedHomeInvitations.value.isEmpty())
    }

    @Test
    fun getFriendHome_rejectsNonFriends() = runBlocking {
        val repository = createRepository()

        assertFailure(
            expected = FriendOperationFailure.NOT_FRIENDS,
            result = repository.getFriendHome(DemoFriendUsers.requestableUser.userId)
        )
    }

    @Test
    fun sendFriendMessage_appendsMessageAndMockReplyForAcceptedFriend() = runBlocking {
        val repository = createRepository()
        val initialCount = repository.friendMessages.value[DemoFriendUsers.alreadyFriend.userId]
            .orEmpty()
            .size

        val result = repository.sendFriendMessage(
            friendUserId = DemoFriendUsers.alreadyFriend.userId,
            message = " 다음에 또 올게 "
        )

        assertTrue(result is FriendOperationResult.Success)
        val sentMessage = (result as FriendOperationResult.Success).value
        assertEquals(FriendMessageSender.ME, sentMessage.sender)
        assertEquals(DemoFriendUsers.me.userId, sentMessage.senderUserId)
        assertEquals("다음에 또 올게", sentMessage.text)
        val messages = repository.friendMessages.value[DemoFriendUsers.alreadyFriend.userId].orEmpty()
        assertEquals(initialCount + 2, messages.size)
        assertEquals(FriendMessageSender.FRIEND, messages.last().sender)
        assertEquals(DemoFriendUsers.alreadyFriend.userId, messages.last().senderUserId)
    }

    @Test
    fun sendFriendMessage_rejectsInvalidMessagesAndNonFriends() = runBlocking {
        val repository = createRepository()

        assertFailure(
            expected = FriendOperationFailure.EMPTY_MESSAGE,
            result = repository.sendFriendMessage(
                friendUserId = DemoFriendUsers.alreadyFriend.userId,
                message = "   "
            )
        )
        assertFailure(
            expected = FriendOperationFailure.MESSAGE_TOO_LONG,
            result = repository.sendFriendMessage(
                friendUserId = DemoFriendUsers.alreadyFriend.userId,
                message = "a".repeat(121)
            )
        )
        assertFailure(
            expected = FriendOperationFailure.NOT_FRIENDS,
            result = repository.sendFriendMessage(
                friendUserId = DemoFriendUsers.requestableUser.userId,
                message = "안녕"
            )
        )
    }

    private fun createRepository(
        initialFriends: List<com.example.lupapj.data.model.friend.FriendUser> =
            listOf(DemoFriendUsers.alreadyFriend),
        initialReceivedRequestUsers: List<com.example.lupapj.data.model.friend.FriendUser> =
            listOf(DemoFriendUsers.incomingRequester)
    ): MockFriendRepository {
        return MockFriendRepository(
            initialFriends = initialFriends,
            initialReceivedRequestUsers = initialReceivedRequestUsers,
            initialHomeInvitationUsers = initialFriends,
            nowProvider = { 1_000L },
            simulatedDelayMillis = 0L
        )
    }

    private fun assertFailure(
        expected: FriendOperationFailure,
        result: FriendOperationResult<*>
    ) {
        assertTrue(result is FriendOperationResult.Failure)
        assertEquals(expected, (result as FriendOperationResult.Failure).reason)
    }
}
