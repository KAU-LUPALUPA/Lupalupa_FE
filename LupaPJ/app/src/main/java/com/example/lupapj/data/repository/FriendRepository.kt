package com.example.lupapj.data.repository

import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.friend.ActiveFriendHomeVisits
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendHomeVisitSession
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import kotlinx.coroutines.flow.StateFlow

interface FriendRepository {
    val myProfile: StateFlow<FriendUser>
    val friends: StateFlow<List<FriendSummary>>
    val receivedRequests: StateFlow<List<FriendRequest>>
    val sentRequests: StateFlow<List<FriendRequest>>
    val receivedHomeInvitations: StateFlow<List<FriendHomeInvitation>>
    val activeHomeVisits: StateFlow<ActiveFriendHomeVisits>
    val friendMessages: StateFlow<Map<String, List<FriendMessage>>>
    val homeVisitMessages: StateFlow<Map<String, List<FriendMessage>>>

    fun updateCurrentUser(userId: String?, nickname: String?) = Unit

    suspend fun refreshFriendOverview(): FriendOperationResult<Unit>

    suspend fun findUserByFriendCode(friendCodeInput: String): FriendUser?

    suspend fun sendFriendRequest(friendCodeInput: String): FriendOperationResult<FriendRequest>

    suspend fun acceptFriendRequest(requestId: String): FriendOperationResult<FriendSummary>

    suspend fun rejectFriendRequest(requestId: String): FriendOperationResult<FriendRequest>

    suspend fun cancelFriendRequest(requestId: String): FriendOperationResult<FriendRequest>

    suspend fun removeFriend(friendUserId: String): FriendOperationResult<FriendSummary>

    suspend fun sendHomeInvitation(
        friendUserId: String,
        message: String? = null
    ): FriendOperationResult<FriendHomeInvitation>

    suspend fun getFriendHome(friendUserId: String): FriendOperationResult<FriendHome>

    suspend fun acceptHomeInvitation(invitationId: String): FriendOperationResult<FriendHomeVisitSession>

    suspend fun rejectHomeInvitation(
        invitationId: String
    ): FriendOperationResult<FriendHomeInvitation>

    suspend fun refreshActiveHomeVisits(): FriendOperationResult<ActiveFriendHomeVisits>

    suspend fun leaveHomeVisit(visitSessionId: String): FriendOperationResult<FriendHomeVisitSession>

    suspend fun getHomeVisitMessages(visitSessionId: String): FriendOperationResult<List<FriendMessage>>

    suspend fun sendHomeVisitMessage(
        visitSessionId: String,
        message: String
    ): FriendOperationResult<FriendMessage>

    suspend fun getFriendMessages(friendUserId: String): FriendOperationResult<List<FriendMessage>>

    suspend fun sendFriendMessage(
        friendUserId: String,
        message: String
    ): FriendOperationResult<FriendMessage>
}
