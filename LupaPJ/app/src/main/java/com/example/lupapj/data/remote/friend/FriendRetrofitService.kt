package com.example.lupapj.data.remote.friend

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FriendRetrofitService {
    @GET("friends/users/me/friend-code")
    suspend fun getMyFriendCode(): Response<FriendCodeResponseDto>

    @GET("friends/users/by-code")
    suspend fun findUserByFriendCode(
        @Query("friendCode") friendCode: String
    ): Response<FriendUserLookupResponseDto>

    @POST("friends/requests")
    suspend fun sendFriendRequest(
        @Body request: SendFriendRequestDto
    ): Response<FriendRequestResponseDto>

    @GET("friends/requests/received")
    suspend fun getReceivedFriendRequests(): Response<FriendRequestsResponseDto>

    @GET("friends/requests/sent")
    suspend fun getSentFriendRequests(): Response<FriendRequestsResponseDto>

    @POST("friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(
        @Path("requestId") requestId: String
    ): Response<AcceptFriendRequestResponseDto>

    @POST("friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(
        @Path("requestId") requestId: String
    ): Response<FriendRequestResponseDto>

    @POST("friends/requests/{requestId}/cancel")
    suspend fun cancelFriendRequest(
        @Path("requestId") requestId: String
    ): Response<FriendRequestResponseDto>

    @GET("friends")
    suspend fun getFriends(): Response<FriendsResponseDto>

    @DELETE("friends/{friendUserId}")
    suspend fun deleteFriend(
        @Path("friendUserId") friendUserId: String
    ): Response<Unit>

    @GET("friends/home-invitations/received")
    suspend fun getReceivedHomeInvitations(): Response<FriendHomeInvitationsResponseDto>

    @POST("friends/home-invitations/{invitationId}/accept")
    suspend fun acceptHomeInvitation(
        @Path("invitationId") invitationId: String
    ): Response<AcceptHomeInvitationResponseDto>

    @POST("friends/home-invitations/{invitationId}/reject")
    suspend fun rejectHomeInvitation(
        @Path("invitationId") invitationId: String
    ): Response<FriendHomeInvitationResponseDto>

    @GET("friends/{friendUserId}/messages")
    suspend fun getFriendMessages(
        @Path("friendUserId") friendUserId: String,
        @Query("limit") limit: Int,
        @Query("before") before: String?
    ): Response<FriendMessagesResponseDto>

    @POST("friends/{friendUserId}/messages")
    suspend fun sendFriendMessage(
        @Path("friendUserId") friendUserId: String,
        @Body request: SendFriendMessageRequestDto
    ): Response<FriendMessageResponseDto>
}
