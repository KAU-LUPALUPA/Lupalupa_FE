package com.example.lupapj.data.model.friend

import com.example.lupapj.data.model.RoomUiState

const val FRIEND_MESSAGE_MAX_LENGTH = 120

@JvmInline
value class FriendCode(val value: String) {
    init {
        require(value.isNotBlank()) { "Friend code must not be blank." }
    }

    companion object {
        private const val PREFIX = "LUPA"
        private val ignoredSeparators = Regex("[\\s-]+")

        fun fromInput(input: String): FriendCode? {
            val compact = input
                .trim()
                .uppercase()
                .replace(ignoredSeparators, "")

            if (compact.isBlank()) return null

            val normalized = if (compact.startsWith(PREFIX)) {
                compact
            } else {
                "$PREFIX$compact"
            }

            return FriendCode(normalized)
        }

        fun display(value: String): String {
            val code = fromInput(value)?.value ?: return value
            return if (code.length <= PREFIX.length) {
                code
            } else {
                "$PREFIX-${code.drop(PREFIX.length)}"
            }
        }
    }
}

data class FriendUser(
    val userId: String,
    val nickname: String,
    val friendCode: FriendCode,
    val avatarAssetKey: String? = null
) {
    val displayFriendCode: String
        get() = FriendCode.display(friendCode.value)
}

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED
}

enum class FriendHomeInvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED,
    EXPIRED
}

enum class FriendshipStatus {
    NONE,
    PENDING_SENT,
    PENDING_RECEIVED,
    ACCEPTED,
    REJECTED,
    CANCELED,
    BLOCKED
}

data class FriendRequest(
    val id: String,
    val fromUser: FriendUser,
    val toUser: FriendUser,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAtMillis: Long,
    val respondedAtMillis: Long? = null
)

data class FriendSummary(
    val user: FriendUser,
    val status: FriendshipStatus = FriendshipStatus.ACCEPTED,
    val friendsSinceMillis: Long
)

data class FriendHomeInvitation(
    val id: String,
    val fromUser: FriendUser,
    val toUser: FriendUser,
    val status: FriendHomeInvitationStatus = FriendHomeInvitationStatus.PENDING,
    val message: String? = null,
    val createdAtMillis: Long,
    val respondedAtMillis: Long? = null,
    val expiresAtMillis: Long? = null
)

data class FriendHome(
    val owner: FriendUser,
    val room: RoomUiState,
    val visitedAtMillis: Long,
    val snapshotAtMillis: Long? = null
)

enum class FriendMessageSender {
    ME,
    FRIEND
}

data class FriendMessage(
    val id: String,
    val friendUserId: String,
    val senderUserId: String,
    val sender: FriendMessageSender,
    val text: String,
    val sentAtMillis: Long
)

enum class FriendOperationFailure {
    EMPTY_CODE,
    EMPTY_MESSAGE,
    MESSAGE_TOO_LONG,
    SELF_CODE,
    USER_NOT_FOUND,
    ALREADY_FRIENDS,
    REQUEST_ALREADY_SENT,
    REQUEST_ALREADY_RECEIVED,
    REQUEST_NOT_FOUND,
    REQUEST_NOT_PENDING,
    FRIEND_NOT_FOUND,
    NOT_FRIENDS,
    HOME_INVITATION_ALREADY_SENT,
    HOME_INVITATION_NOT_FOUND,
    HOME_INVITATION_NOT_PENDING,
    NOT_HOME_INVITATION_RECEIVER,
    NOT_HOME_INVITATION_SENDER,
    FRIEND_HOME_UNAVAILABLE,
    BLOCKED,
    UNKNOWN
}

sealed interface FriendOperationResult<out T> {
    data class Success<T>(val value: T) : FriendOperationResult<T>
    data class Failure(val reason: FriendOperationFailure) : FriendOperationResult<Nothing>
}
