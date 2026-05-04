package com.example.lupapj.data.repository

import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.friend.FriendHome
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
    val friendMessages: StateFlow<Map<String, List<FriendMessage>>>

    suspend fun findUserByFriendCode(friendCodeInput: String): FriendUser?

    suspend fun sendFriendRequest(friendCodeInput: String): FriendOperationResult<FriendRequest>

    suspend fun acceptFriendRequest(requestId: String): FriendOperationResult<FriendSummary>

    suspend fun rejectFriendRequest(requestId: String): FriendOperationResult<FriendRequest>

    suspend fun cancelFriendRequest(requestId: String): FriendOperationResult<FriendRequest>

    suspend fun removeFriend(friendUserId: String): FriendOperationResult<FriendSummary>

    suspend fun getFriendHome(friendUserId: String): FriendOperationResult<FriendHome>

    suspend fun getFriendMessages(friendUserId: String): FriendOperationResult<List<FriendMessage>>

    suspend fun sendFriendMessage(
        friendUserId: String,
        message: String
    ): FriendOperationResult<FriendMessage>
}
