package com.example.lupapj.data.remote.plaza

import com.example.lupapj.data.model.DEFAULT_PET_CHARACTER_ASSET_KEY
import com.example.lupapj.data.model.DEFAULT_PET_ID
import com.example.lupapj.data.model.DEFAULT_PET_NAME
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetTraits
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.plaza.PLAZA_MAX_PARTICIPANTS
import com.example.lupapj.data.model.plaza.PlazaChatMessage
import com.example.lupapj.data.model.plaza.PlazaCode
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaParticipant
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaPosition
import com.example.lupapj.data.model.plaza.PlazaRoom

internal fun PlazaRoomResponseDto.toDomain(
    currentUserId: String?,
    currentNickname: String?,
    nowMillis: Long = System.currentTimeMillis()
): PlazaRoom {
    val normalizedCode = plazaCode
        ?: displayPlazaCode
        ?: "PZ0000"
    val domainCode = PlazaCode.fromInput(normalizedCode) ?: PlazaCode(normalizedCode)
    val roomId = plazaId?.takeIf { it.isNotBlank() } ?: "plaza_unknown"
    val participantDomains = participants.orEmpty().map { participant ->
        participant.toDomain(
            currentUserId = currentUserId,
            currentNickname = currentNickname,
            nowMillis = nowMillis
        )
    }

    return PlazaRoom(
        plazaId = roomId,
        plazaCode = domainCode,
        participants = participantDomains,
        messages = messages.orEmpty().map { it.toDomain(plazaId = roomId, nowMillis = nowMillis) },
        interactions = emptyList(),
        maxParticipants = maxParticipants ?: PLAZA_MAX_PARTICIPANTS,
        joinedAtMillis = participantDomains.firstOrNull { it.isMe }?.joinedAtMillis ?: nowMillis,
        roomRevision = roomRevision ?: 0L,
        serverTime = null,
        isServerAuthoritative = false
    )
}

internal fun PlazaChatMessageResponseDto.toDomain(
    plazaId: String,
    nowMillis: Long = System.currentTimeMillis()
): PlazaChatMessage {
    return PlazaChatMessage(
        id = id?.takeIf { it.isNotBlank() } ?: "plaza_message_$nowMillis",
        plazaId = plazaId,
        senderUserId = senderUserId.orEmpty(),
        senderNickname = senderNickname?.takeIf { it.isNotBlank() } ?: "사용자",
        text = text.orEmpty(),
        sentAtMillis = sentAtMillis ?: nowMillis
    )
}

internal fun PlazaApiException.toPlazaOperationFailure(): PlazaOperationFailure {
    return when (code) {
        "EMPTY_CODE" -> PlazaOperationFailure.EMPTY_CODE
        "INVALID_CODE",
        "INVALID_PLAZA_CODE" -> PlazaOperationFailure.INVALID_CODE
        "PLAZA_NOT_FOUND" -> PlazaOperationFailure.PLAZA_NOT_FOUND
        "PLAZA_FULL" -> PlazaOperationFailure.PLAZA_FULL
        "EMPTY_MESSAGE" -> PlazaOperationFailure.EMPTY_MESSAGE
        "MESSAGE_TOO_LONG" -> PlazaOperationFailure.MESSAGE_TOO_LONG
        "NOT_IN_PLAZA" -> PlazaOperationFailure.NOT_IN_PLAZA
        "UNAUTHORIZED",
        "INVALID_TOKEN",
        "HTTP_401" -> PlazaOperationFailure.UNAUTHORIZED
        else -> PlazaOperationFailure.UNKNOWN
    }
}

private fun PlazaParticipantResponseDto.toDomain(
    currentUserId: String?,
    currentNickname: String?,
    nowMillis: Long
): PlazaParticipant {
    val participantUserId = userId?.takeIf { it.isNotBlank() } ?: "plaza_user_unknown"
    val participantNickname = nickname?.takeIf { it.isNotBlank() } ?: "사용자"
    val isMe = participantUserId == currentUserId ||
        (currentNickname != null && participantNickname == currentNickname)

    return PlazaParticipant(
        userId = participantUserId,
        nickname = participantNickname,
        pet = pet.toDomain(ownerUserId = participantUserId),
        joinedAtMillis = joinedAtMillis ?: nowMillis,
        isMe = isMe,
        position = position?.toDomain(),
        movement = null,
        positionUpdatedAtMillis = null
    )
}

private fun PlazaPetSnapshotResponseDto?.toDomain(ownerUserId: String): PlazaPetSnapshot {
    return PlazaPetSnapshot(
        petId = this?.petId?.takeIf { it.isNotBlank() } ?: DEFAULT_PET_ID,
        ownerUserId = ownerUserId,
        name = this?.name?.takeIf { it.isNotBlank() } ?: DEFAULT_PET_NAME,
        characterAssetKey = this?.characterAssetKey?.takeIf { it.isNotBlank() }
            ?: DEFAULT_PET_CHARACTER_ASSET_KEY,
        appearance = this?.appearance?.toDomain() ?: PetAppearance(),
        status = PetStatus(),
        traits = PetTraits(),
        equippedItemIds = emptyList()
    )
}

private fun PetAppearanceResponseDto.toDomain(): PetAppearance {
    return PetAppearance(
        headSizeScale = headSizeScale ?: 1f,
        bodySizeScale = bodySizeScale ?: 1f,
        eyeSizeScale = eyeSizeScale ?: 1f,
        noseSizeScale = noseSizeScale ?: 1f,
        mouthSizeScale = mouthSizeScale ?: 1f
    )
}

private fun PlazaPositionResponseDto.toDomain(): PlazaPosition {
    return PlazaPosition(
        x = (x ?: 0.5f).coerceIn(0f, 1f),
        y = (y ?: 0.7f).coerceIn(0f, 1f)
    )
}
