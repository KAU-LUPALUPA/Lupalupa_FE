package com.example.lupapj.data.remote.friend

import com.google.gson.Gson
import retrofit2.Response

class RetrofitFriendApiClient(
    private val service: FriendRetrofitService,
    private val gson: Gson = Gson()
) : FriendApiClient {
    override suspend fun getMyFriendCode(): FriendCodeResponseDto {
        return service.getMyFriendCode().bodyOrThrow()
    }

    override suspend fun findUserByFriendCode(
        friendCode: String
    ): FriendUserLookupResponseDto {
        return service.findUserByFriendCode(friendCode).bodyOrThrow()
    }

    override suspend fun sendFriendRequest(
        request: SendFriendRequestDto
    ): FriendRequestResponseDto {
        return service.sendFriendRequest(request).bodyOrThrow()
    }

    override suspend fun getReceivedFriendRequests(): FriendRequestsResponseDto {
        return service.getReceivedFriendRequests().bodyOrThrow()
    }

    override suspend fun getSentFriendRequests(): FriendRequestsResponseDto {
        return service.getSentFriendRequests().bodyOrThrow()
    }

    override suspend fun acceptFriendRequest(
        requestId: String
    ): AcceptFriendRequestResponseDto {
        return service
            .acceptFriendRequest(requestId)
            .bodyOrThrow()
    }

    override suspend fun rejectFriendRequest(requestId: String): FriendRequestResponseDto {
        return service
            .rejectFriendRequest(requestId)
            .bodyOrThrow()
    }

    override suspend fun cancelFriendRequest(requestId: String): FriendRequestResponseDto {
        return service
            .cancelFriendRequest(requestId)
            .bodyOrThrow()
    }

    override suspend fun getFriends(): FriendsResponseDto {
        return service.getFriends().bodyOrThrow()
    }

    override suspend fun deleteFriend(friendUserId: String) {
        service.deleteFriend(friendUserId).throwIfFailed()
    }

    override suspend fun sendHomeInvitation(
        request: SendHomeInvitationRequestDto
    ): FriendHomeInvitationResponseDto {
        return service.sendHomeInvitation(request).bodyOrThrow()
    }

    override suspend fun getReceivedHomeInvitations(): FriendHomeInvitationsResponseDto {
        return service.getReceivedHomeInvitations().bodyOrThrow()
    }

    override suspend fun acceptHomeInvitation(
        invitationId: String
    ): AcceptHomeInvitationResponseDto {
        return service
            .acceptHomeInvitation(invitationId)
            .bodyOrThrow()
    }

    override suspend fun rejectHomeInvitation(
        invitationId: String
    ): FriendHomeInvitationResponseDto {
        return service
            .rejectHomeInvitation(invitationId)
            .bodyOrThrow()
    }

    override suspend fun getActiveHomeVisits(): ActiveHomeVisitsResponseDto {
        return service.getActiveHomeVisits().bodyOrThrow()
    }

    override suspend fun leaveHomeVisit(
        visitSessionId: String
    ): FriendHomeVisitSessionResponseDto {
        return service.leaveHomeVisit(visitSessionId).bodyOrThrow()
    }

    override suspend fun getHomeVisitMessages(
        visitSessionId: String
    ): HomeVisitMessagesResponseDto {
        return service.getHomeVisitMessages(visitSessionId).bodyOrThrow()
    }

    override suspend fun sendHomeVisitMessage(
        visitSessionId: String,
        request: SendHomeVisitMessageRequestDto
    ): HomeVisitMessageResponseDto {
        return service.sendHomeVisitMessage(visitSessionId, request).bodyOrThrow()
    }

    @Deprecated(
        message = "Direct friend home visits are blocked by policy. Use home invitation accept flow.",
        replaceWith = ReplaceWith("acceptHomeInvitation(invitationId)")
    )
    override suspend fun getFriendHome(friendUserId: String): FriendHomeResponseDto {
        throw FriendApiException(code = "BLOCKED", httpStatus = null)
    }

    override suspend fun getFriendMessages(
        friendUserId: String,
        limit: Int,
        before: String?
    ): FriendMessagesResponseDto {
        return service.getFriendMessages(friendUserId, limit, before).bodyOrThrow()
    }

    override suspend fun sendFriendMessage(
        friendUserId: String,
        request: SendFriendMessageRequestDto
    ): FriendMessageResponseDto {
        return service.sendFriendMessage(friendUserId, request).bodyOrThrow()
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) throw toFriendApiException()
        return body() ?: throw FriendApiException(
            code = "EMPTY_RESPONSE",
            httpStatus = code(),
            message = "Response body is empty."
        )
    }

    private fun Response<*>.throwIfFailed() {
        if (!isSuccessful) throw toFriendApiException()
    }

    private fun Response<*>.toFriendApiException(): FriendApiException {
        val parsedError = errorBody()?.string()?.let { raw ->
            runCatching {
                gson.fromJson(raw, ErrorResponseDto::class.java)
            }.getOrNull()
        }
        return FriendApiException(
            code = parsedError?.code ?: "HTTP_${code()}",
            httpStatus = code(),
            message = parsedError?.message ?: message()
        )
    }

    private data class ErrorResponseDto(
        val code: String? = null,
        val message: String? = null
    )
}
