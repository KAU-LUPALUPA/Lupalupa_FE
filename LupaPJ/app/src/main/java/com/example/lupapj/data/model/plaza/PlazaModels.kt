package com.example.lupapj.data.model.plaza

import com.example.lupapj.data.model.DEFAULT_PET_CHARACTER_ASSET_KEY
import com.example.lupapj.data.model.DEFAULT_PET_ID
import com.example.lupapj.data.model.DEFAULT_PET_NAME
import com.example.lupapj.data.model.DEFAULT_PET_OWNER_USER_ID
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetTraits
import com.example.lupapj.data.model.PetStatus

const val PLAZA_MAX_PARTICIPANTS = 4
const val PLAZA_MESSAGE_MAX_LENGTH = 120

@JvmInline
value class PlazaCode(val value: String) {
    init {
        require(value.isNotBlank()) { "Plaza code must not be blank." }
    }

    companion object {
        private const val PREFIX = "PZ"
        private val ignoredSeparators = Regex("[\\s-]+")
        private val validCode = Regex("^$PREFIX[A-Z0-9]{4,6}$")

        fun fromInput(input: String): PlazaCode? {
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

            return normalized
                .takeIf { validCode.matches(it) }
                ?.let(::PlazaCode)
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

data class PlazaPetSnapshot(
    val petId: String = DEFAULT_PET_ID,
    val ownerUserId: String = DEFAULT_PET_OWNER_USER_ID,
    val name: String = DEFAULT_PET_NAME,
    val characterAssetKey: String = DEFAULT_PET_CHARACTER_ASSET_KEY,
    val appearance: PetAppearance = PetAppearance(),
    val status: PetStatus = PetStatus(),
    val traits: PetTraits = PetTraits(),
    val equippedItemIds: List<String> = emptyList()
)

data class PlazaPosition(
    val x: Float,
    val y: Float
)

data class PlazaMovementCommand(
    val from: PlazaPosition,
    val to: PlazaPosition,
    val startedAtMillis: Long,
    val durationMillis: Long
)

data class PlazaServerTime(
    val serverNowMillis: Long,
    val clientReceivedAtMillis: Long
) {
    fun currentServerNowMillis(clientNowMillis: Long): Long {
        return serverNowMillis + (clientNowMillis - clientReceivedAtMillis)
    }
}

data class PlazaParticipant(
    val userId: String,
    val nickname: String,
    val pet: PlazaPetSnapshot,
    val joinedAtMillis: Long,
    val isMe: Boolean = false,
    val position: PlazaPosition? = null,
    val movement: PlazaMovementCommand? = null,
    val positionUpdatedAtMillis: Long? = null
)

data class PlazaChatMessage(
    val id: String,
    val plazaId: String,
    val senderUserId: String,
    val senderNickname: String,
    val text: String,
    val sentAtMillis: Long
)

enum class PlazaInteractionType {
    GREET,
    PLAY,
    REST,
    FOLLOW
}

data class PlazaInteractionEvent(
    val id: String,
    val plazaId: String,
    val type: PlazaInteractionType,
    val actorUserId: String,
    val targetUserId: String? = null,
    val textByUserId: Map<String, String> = emptyMap(),
    val startedAtMillis: Long,
    val durationMillis: Long,
    val movementTargetByUserId: Map<String, PlazaPosition> = emptyMap(),
    val facingTargetByUserId: Map<String, PlazaPosition> = emptyMap(),
    val animationByUserId: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap()
)

data class PlazaRoom(
    val plazaId: String,
    val plazaCode: PlazaCode,
    val participants: List<PlazaParticipant>,
    val messages: List<PlazaChatMessage> = emptyList(),
    val interactions: List<PlazaInteractionEvent> = emptyList(),
    val maxParticipants: Int = PLAZA_MAX_PARTICIPANTS,
    val joinedAtMillis: Long,
    val roomRevision: Long = 0L,
    val serverTime: PlazaServerTime? = null,
    val isServerAuthoritative: Boolean = false
) {
    val displayCode: String
        get() = PlazaCode.display(plazaCode.value)

    val participantCountText: String
        get() = "${participants.size}/$maxParticipants"
}

enum class PlazaOperationFailure {
    EMPTY_CODE,
    INVALID_CODE,
    PLAZA_NOT_FOUND,
    PLAZA_FULL,
    EMPTY_MESSAGE,
    MESSAGE_TOO_LONG,
    NOT_IN_PLAZA,
    UNAUTHORIZED,
    API_NOT_FOUND,
    SERVER_ERROR,
    NETWORK_ERROR,
    RESPONSE_ERROR,
    UNKNOWN
}

sealed interface PlazaOperationResult<out T> {
    data class Success<T>(val value: T) : PlazaOperationResult<T>
    data class Failure(val reason: PlazaOperationFailure) : PlazaOperationResult<Nothing>
}
