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
import com.example.lupapj.data.model.plaza.PlazaInteractionEvent
import com.example.lupapj.data.model.plaza.PlazaInteractionType
import com.example.lupapj.data.model.plaza.PlazaMovementCommand
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaParticipant
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaPosition
import com.example.lupapj.data.model.plaza.PlazaRoom
import com.example.lupapj.data.model.plaza.PlazaServerTime

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
        interactions = interactions.orEmpty().mapNotNull {
            it.toDomain(plazaId = roomId, nowMillis = nowMillis)
        },
        maxParticipants = maxParticipants ?: PLAZA_MAX_PARTICIPANTS,
        joinedAtMillis = participantDomains.firstOrNull { it.isMe }?.joinedAtMillis ?: nowMillis,
        roomRevision = roomRevision ?: 0L,
        serverTime = serverNowMillis?.let {
            PlazaServerTime(
                serverNowMillis = it,
                clientReceivedAtMillis = nowMillis
            )
        },
        isServerAuthoritative = serverNowMillis != null
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
        movement = movement?.toDomain(),
        positionUpdatedAtMillis = positionUpdatedAtMillis
    )
}

private fun PlazaMovementResponseDto.toDomain(): PlazaMovementCommand? {
    val fromPosition = from?.toDomain() ?: return null
    val toPosition = to?.toDomain() ?: return null
    val startedAt = startedAtMillis ?: return null
    val duration = durationMillis?.takeIf { it > 0L } ?: return null

    return PlazaMovementCommand(
        from = fromPosition,
        to = toPosition,
        startedAtMillis = startedAt,
        durationMillis = duration
    )
}

private fun PlazaInteractionResponseDto.toDomain(
    plazaId: String,
    nowMillis: Long
): PlazaInteractionEvent? {
    val actorId = actorUserId?.takeIf { it.isNotBlank() } ?: return null
    val interactionType = type
        ?.takeIf { it.isNotBlank() }
        ?.let { rawType ->
            runCatching { PlazaInteractionType.valueOf(rawType.uppercase()) }.getOrNull()
        }
        ?: return null
    val startedAt = startedAtMillis ?: nowMillis
    val duration = durationMillis?.takeIf { it > 0L } ?: return null

    return PlazaInteractionEvent(
        id = id?.takeIf { it.isNotBlank() } ?: "plaza_interaction_${plazaId}_$startedAt",
        plazaId = plazaId,
        type = interactionType,
        actorUserId = actorId,
        targetUserId = targetUserId?.takeIf { it.isNotBlank() },
        textByUserId = textByUserId.orEmpty().filterKeys { it.isNotBlank() },
        startedAtMillis = startedAt,
        durationMillis = duration,
        movementTargetByUserId = movementTargetByUserId.toPositionMap(),
        facingTargetByUserId = facingTargetByUserId.toPositionMap(),
        animationByUserId = animationByUserId.orEmpty()
            .filterKeys { it.isNotBlank() }
            .filterValues { it.isNotBlank() }
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

private fun Map<String, PlazaPositionResponseDto>?.toPositionMap(): Map<String, PlazaPosition> {
    return orEmpty()
        .mapNotNull { (userId, position) ->
            userId.takeIf { it.isNotBlank() }?.let { it to position.toDomain() }
        }
        .toMap()
}
