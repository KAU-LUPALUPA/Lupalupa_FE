package com.example.lupapj.data.remote.friend

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetPersonality
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.friend.FriendCode
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendHomeInvitationStatus
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendMessageSender
import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendRequestStatus
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.data.model.friend.FriendshipStatus
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.initialHouseSceneState
import java.time.OffsetDateTime

internal fun FriendUserDto.toDomain(): FriendUser {
    return FriendUser(
        userId = userId,
        nickname = nickname,
        friendCode = FriendCode.fromInput(friendCode) ?: FriendCode(friendCode),
        avatarAssetKey = avatarAssetKey
    )
}

internal fun FriendCodeResponseDto.applyTo(currentUser: FriendUser): FriendUser {
    return currentUser.copy(
        friendCode = FriendCode.fromInput(friendCode) ?: currentUser.friendCode
    )
}

internal fun FriendRequestDto.toDomain(): FriendRequest {
    return FriendRequest(
        id = id,
        fromUser = fromUser.toDomain(),
        toUser = toUser.toDomain(),
        status = enumValueOrDefault(status, FriendRequestStatus.PENDING),
        createdAtMillis = createdAt.toEpochMillis(),
        respondedAtMillis = respondedAt?.toEpochMillis()
    )
}

internal fun FriendshipDto.toDomain(): FriendSummary {
    return FriendSummary(
        user = friend.toDomain(),
        status = enumValueOrDefault(status, FriendshipStatus.ACCEPTED),
        friendsSinceMillis = friendsSince.toEpochMillis()
    )
}

internal fun FriendHomeInvitationDto.toDomain(): FriendHomeInvitation {
    return FriendHomeInvitation(
        id = id,
        fromUser = fromUser.toDomain(),
        toUser = toUser.toDomain(),
        status = enumValueOrDefault(status, FriendHomeInvitationStatus.PENDING),
        message = message,
        createdAtMillis = createdAt.toEpochMillis(),
        respondedAtMillis = respondedAt?.toEpochMillis(),
        expiresAtMillis = expiresAt?.toEpochMillis()
    )
}

internal fun FriendMessageDto.toDomain(currentUserId: String): FriendMessage {
    return FriendMessage(
        id = id,
        friendUserId = friendUserId,
        senderUserId = senderUserId,
        sender = if (senderUserId == currentUserId) {
            FriendMessageSender.ME
        } else {
            FriendMessageSender.FRIEND
        },
        text = text,
        sentAtMillis = sentAt.toEpochMillis()
    )
}

internal fun FriendHomeResponseDto.toDomain(
    sceneResolver: (String) -> RoomSceneDefinition
): FriendHome {
    val sceneDefinition = sceneResolver(room.sceneId)
    return FriendHome(
        owner = owner.toDomain(),
        room = room.toDomainRoomState(
            sceneDefinition = sceneDefinition,
            pet = pet
        ),
        visitedAtMillis = visitedAt.toEpochMillis()
    )
}

internal fun AcceptHomeInvitationResponseDto.toDomain(
    sceneResolver: (String) -> RoomSceneDefinition
): FriendHome {
    return homeSnapshot.toDomain(sceneResolver = sceneResolver)
}

private fun FriendHomeSnapshotDto.toDomain(
    sceneResolver: (String) -> RoomSceneDefinition
): FriendHome {
    val sceneDefinition = sceneResolver(room.sceneId)
    return FriendHome(
        owner = owner.toDomain(),
        room = room.toDomainRoomState(
            sceneDefinition = sceneDefinition,
            petSnapshot = petSnapshot
        ),
        visitedAtMillis = visitedAt.toEpochMillis(),
        snapshotAtMillis = snapshotAt?.toEpochMillis()
    )
}

internal fun FriendApiException.toFriendOperationFailure(): FriendOperationFailure {
    return when (code) {
        "EMPTY_CODE" -> FriendOperationFailure.EMPTY_CODE
        "EMPTY_MESSAGE" -> FriendOperationFailure.EMPTY_MESSAGE
        "MESSAGE_TOO_LONG" -> FriendOperationFailure.MESSAGE_TOO_LONG
        "SELF_CODE" -> FriendOperationFailure.SELF_CODE
        "USER_NOT_FOUND" -> FriendOperationFailure.USER_NOT_FOUND
        "ALREADY_FRIENDS" -> FriendOperationFailure.ALREADY_FRIENDS
        "REQUEST_ALREADY_SENT" -> FriendOperationFailure.REQUEST_ALREADY_SENT
        "REQUEST_ALREADY_RECEIVED" -> FriendOperationFailure.REQUEST_ALREADY_RECEIVED
        "REQUEST_NOT_FOUND" -> FriendOperationFailure.REQUEST_NOT_FOUND
        "REQUEST_NOT_PENDING" -> FriendOperationFailure.REQUEST_NOT_PENDING
        "FRIEND_NOT_FOUND" -> FriendOperationFailure.FRIEND_NOT_FOUND
        "NOT_FRIENDS" -> FriendOperationFailure.NOT_FRIENDS
        "HOME_INVITATION_ALREADY_SENT" -> FriendOperationFailure.HOME_INVITATION_ALREADY_SENT
        "HOME_INVITATION_NOT_FOUND" -> FriendOperationFailure.HOME_INVITATION_NOT_FOUND
        "HOME_INVITATION_NOT_PENDING" -> FriendOperationFailure.HOME_INVITATION_NOT_PENDING
        "NOT_HOME_INVITATION_RECEIVER" -> FriendOperationFailure.NOT_HOME_INVITATION_RECEIVER
        "NOT_HOME_INVITATION_SENDER" -> FriendOperationFailure.NOT_HOME_INVITATION_SENDER
        "FRIEND_HOME_UNAVAILABLE" -> FriendOperationFailure.FRIEND_HOME_UNAVAILABLE
        "BLOCKED" -> FriendOperationFailure.BLOCKED
        else -> FriendOperationFailure.UNKNOWN
    }
}

private fun FriendRoomDto.toDomainRoomState(
    sceneDefinition: RoomSceneDefinition,
    pet: FriendPetDto?
): RoomUiState {
    return initialRoomUiState(
        sceneDefinition = sceneDefinition,
        houseSceneState = initialHouseSceneState(
            sceneId = sceneDefinition.id,
            petId = pet?.petId ?: "friend_pet",
            ownerUserId = pet?.ownerUserId ?: "friend_user",
            petName = pet?.name ?: "루파",
            characterAssetKey = pet?.characterAssetKey ?: "room/characters/lupa_default",
            petAppearance = pet?.appearance?.toDomain() ?: PetAppearance(),
            petStatus = pet?.status?.toDomain() ?: PetStatus(),
            petPersonality = pet?.personality?.let {
                enumValueOrDefault(it, PetPersonality.ACTIVE)
            } ?: PetPersonality.ACTIVE,
            equippedItemIds = pet?.equippedItemIds.orEmpty(),
            petAnchor = pet?.anchor?.toFloorAnchor() ?: FloorAnchor(u = 0.44f, v = 0.64f),
            petAction = PetAction.IDLE
        )
    )
}

private fun FriendRoomDto.toDomainRoomState(
    sceneDefinition: RoomSceneDefinition,
    petSnapshot: FriendPetSnapshotDto?
): RoomUiState {
    return initialRoomUiState(
        sceneDefinition = sceneDefinition,
        houseSceneState = initialHouseSceneState(
            sceneId = sceneDefinition.id,
            petId = petSnapshot?.petId ?: "friend_pet",
            ownerUserId = petSnapshot?.ownerUserId ?: "friend_user",
            petName = petSnapshot?.name ?: "루파",
            characterAssetKey = petSnapshot?.characterAssetKey ?: "room/characters/lupa_default",
            petAppearance = petSnapshot?.appearance?.toDomain() ?: PetAppearance(),
            petStatus = petSnapshot?.condition?.toDomain() ?: PetStatus(),
            petPersonality = petSnapshot?.personality?.let {
                enumValueOrDefault(it, PetPersonality.ACTIVE)
            } ?: PetPersonality.ACTIVE,
            equippedItemIds = petSnapshot?.equippedItemIds.orEmpty(),
            petAnchor = petSnapshot?.sceneState?.anchor?.toFloorAnchor()
                ?: FloorAnchor(u = 0.44f, v = 0.64f),
            petAction = petSnapshot?.sceneState?.action?.let {
                enumValueOrDefault(it, PetAction.IDLE)
            } ?: PetAction.IDLE
        )
    )
}

private fun FriendPetAppearanceDto.toDomain(): PetAppearance {
    return PetAppearance(
        headSizeScale = headSizeScale,
        bodySizeScale = bodySizeScale,
        eyeSizeScale = eyeSizeScale,
        noseSizeScale = noseSizeScale,
        mouthSizeScale = mouthSizeScale
    )
}

private fun FriendPetStatusDto.toDomain(): PetStatus {
    return PetStatus(
        satiety = satiety.coerceIn(0, 100),
        vitality = vitality.coerceIn(0, 100),
        isEgg = isEgg
    )
}

private fun FriendPetConditionDto.toDomain(): PetStatus {
    return PetStatus(
        satiety = satiety.coerceIn(0, 100),
        vitality = vitality.coerceIn(0, 100),
        isEgg = isEgg
    )
}

private fun FriendAnchorDto.toFloorAnchor(): FloorAnchor {
    return FloorAnchor(
        u = u.coerceIn(0f, 1f),
        v = v.coerceIn(0f, 1f)
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T
): T {
    return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
}

private fun String.toEpochMillis(): Long {
    return runCatching {
        OffsetDateTime.parse(this).toInstant().toEpochMilli()
    }.getOrDefault(0L)
}
