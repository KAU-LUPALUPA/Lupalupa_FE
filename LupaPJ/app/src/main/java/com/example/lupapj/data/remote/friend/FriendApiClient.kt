package com.example.lupapj.data.remote.friend

/**
 * Contract-facing friend API boundary.
 *
 * A Retrofit, Ktor, or backend-provided implementation can implement this interface without
 * changing the UI/ViewModel layer.
 */
interface FriendApiClient {
    suspend fun getMyFriendCode(): FriendCodeResponseDto

    suspend fun findUserByFriendCode(friendCode: String): FriendUserLookupResponseDto

    suspend fun sendFriendRequest(request: SendFriendRequestDto): FriendRequestResponseDto

    suspend fun getReceivedFriendRequests(): FriendRequestsResponseDto

    suspend fun getSentFriendRequests(): FriendRequestsResponseDto

    suspend fun acceptFriendRequest(requestId: String): AcceptFriendRequestResponseDto

    suspend fun rejectFriendRequest(requestId: String): FriendRequestResponseDto

    suspend fun cancelFriendRequest(requestId: String): FriendRequestResponseDto

    suspend fun getFriends(): FriendsResponseDto

    suspend fun deleteFriend(friendUserId: String)

    suspend fun getReceivedHomeInvitations(): FriendHomeInvitationsResponseDto

    suspend fun acceptHomeInvitation(invitationId: String): AcceptHomeInvitationResponseDto

    suspend fun rejectHomeInvitation(invitationId: String): FriendHomeInvitationResponseDto

    @Deprecated(
        message = "Direct friend home visits are blocked by policy. Use home invitation accept flow.",
        replaceWith = ReplaceWith("acceptHomeInvitation(invitationId)")
    )
    suspend fun getFriendHome(friendUserId: String): FriendHomeResponseDto

    suspend fun getFriendMessages(
        friendUserId: String,
        limit: Int = 30,
        before: String? = null
    ): FriendMessagesResponseDto

    suspend fun sendFriendMessage(
        friendUserId: String,
        request: SendFriendMessageRequestDto
    ): FriendMessageResponseDto
}

class FriendApiException(
    val code: String,
    val httpStatus: Int? = null,
    override val message: String? = null
) : Exception(message)
