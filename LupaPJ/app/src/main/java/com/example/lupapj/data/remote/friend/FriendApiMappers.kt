package com.example.lupapj.data.remote.friend

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetTraits
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.friend.ActiveFriendHomeVisits
import com.example.lupapj.data.remote.pet.toDomain
import com.example.lupapj.data.model.friend.FriendCode
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendHomeInvitationStatus
import com.example.lupapj.data.model.friend.FriendHomeVisitSession
import com.example.lupapj.data.model.friend.FriendHomeVisitStatus
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
import com.example.lupapj.data.model.scene.PetSceneState
import com.example.lupapj.data.model.scene.FloorTilePlacement
import com.example.lupapj.data.model.scene.IsoRoomProjectionSpec
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.TileAnchorMode
import com.example.lupapj.data.model.scene.TileCoord
import com.example.lupapj.data.model.scene.TileFootprint
import com.example.lupapj.data.model.scene.initialHouseSceneState
import com.example.lupapj.data.model.scene.toFloorAnchor
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

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
    val friendUser = friend ?: user
        ?: throw IllegalArgumentException("Friendship response is missing friend user.")

    return FriendSummary(
        user = friendUser.toDomain(),
        status = enumValueOrDefault(status, FriendshipStatus.ACCEPTED),
        friendsSinceMillis = friendsSince.toEpochMillis()
    )
}

internal fun FriendHomeInvitationDto.toDomain(): FriendHomeInvitation {
    return FriendHomeInvitation(
        id = id,
        fromUser = fromUserOrFallback().toDomain(),
        toUser = toUserOrFallback().toDomain(),
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
    val sceneDefinition = room.toSceneDefinition(sceneResolver)
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
    return homeSnapshot.toDomain(
        sceneResolver = sceneResolver,
        fallbackOwner = invitation.fromUserOrFallback(),
        fallbackVisitedAt = invitation.respondedAt ?: invitation.createdAt
    )
}

internal fun AcceptHomeInvitationResponseDto.toHomeVisitSession(
    sceneResolver: (String) -> RoomSceneDefinition
): FriendHomeVisitSession {
    return requireNotNull(visitSession) {
        "Accept home invitation response is missing visitSession."
    }.toDomain(sceneResolver)
}

internal fun ActiveHomeVisitsResponseDto.toDomain(
    sceneResolver: (String) -> RoomSceneDefinition
): ActiveFriendHomeVisits {
    return ActiveFriendHomeVisits(
        hosting = hosting.map { it.toDomain(sceneResolver) },
        visiting = visiting.map { it.toDomain(sceneResolver) }
    )
}

internal fun FriendHomeVisitSessionDto.toDomain(
    sceneResolver: (String) -> RoomSceneDefinition
): FriendHomeVisitSession {
    return FriendHomeVisitSession(
        id = id,
        hostUser = hostUser.toDomain(),
        visitorUser = visitorUser.toDomain(),
        status = enumValueOrDefault(status, FriendHomeVisitStatus.ACTIVE),
        startedAtMillis = startedAt.toEpochMillis(),
        endedAtMillis = endedAt?.toEpochMillis(),
        expiresAtMillis = expiresAt?.toEpochMillis(),
        hostHome = hostHomeSnapshot?.toDomain(
            sceneResolver = sceneResolver,
            fallbackOwner = hostUser,
            fallbackVisitedAt = startedAt
        ),
        visitorPet = visitorPetSnapshot?.toPetSceneState()
    )
}

internal fun HomeVisitMessageDto.toDomain(
    currentUserId: String,
    friendUserId: String
): FriendMessage {
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

private fun FriendHomeSnapshotDto.toDomain(
    sceneResolver: (String) -> RoomSceneDefinition,
    fallbackOwner: FriendUserDto? = null,
    fallbackVisitedAt: String? = null
): FriendHome {
    val sceneDefinition = room.toSceneDefinition(sceneResolver)
    val ownerDto = owner ?: fallbackOwner ?: fallbackFriendUserDto()
    return FriendHome(
        owner = ownerDto.toDomain(),
        room = room.toDomainRoomState(
            sceneDefinition = sceneDefinition,
            petSnapshot = petSnapshot
        ),
        visitedAtMillis = visitedAt?.toEpochMillis()
            ?: fallbackVisitedAt?.toEpochMillis()
            ?: System.currentTimeMillis(),
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
        "HOME_VISIT_ALREADY_ACTIVE" -> FriendOperationFailure.HOME_VISIT_ALREADY_ACTIVE
        "HOME_VISIT_NOT_FOUND" -> FriendOperationFailure.HOME_VISIT_NOT_FOUND
        "HOME_VISIT_NOT_ACTIVE" -> FriendOperationFailure.HOME_VISIT_NOT_ACTIVE
        "NOT_HOME_VISIT_PARTICIPANT" -> FriendOperationFailure.NOT_HOME_VISIT_PARTICIPANT
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
            petTraits = pet?.traits?.toDomain() ?: PetTraits(),
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
            petTraits = petSnapshot?.traits?.toDomain() ?: PetTraits(),
            equippedItemIds = petSnapshot?.equippedItemIds.orEmpty(),
            petAnchor = petSnapshot?.sceneState?.anchor?.toFloorAnchor()
                ?: FloorAnchor(u = 0.44f, v = 0.64f),
            petAction = petSnapshot?.sceneState?.action?.let {
                enumValueOrDefault(it, PetAction.IDLE)
            } ?: PetAction.IDLE
        )
    )
}

private fun FriendRoomDto.toSceneDefinition(
    sceneResolver: (String) -> RoomSceneDefinition
): RoomSceneDefinition {
    val baseScene = sceneResolver(sceneId)
    return baseScene.copy(
        wallAssetKey = wallAssetKey ?: baseScene.wallAssetKey,
        floorAssetKey = floorAssetKey ?: baseScene.floorAssetKey,
        objects = placedItems.toSceneObjects(baseScene)
    )
}

private fun List<FriendPlacedItemDto>.toSceneObjects(
    baseScene: RoomSceneDefinition
): List<SceneObjectDefinition> {
    return mapNotNull { placedItem ->
        val type = placedItem.objectType.toRoomObjectTypeOrNull()
            ?: placedItem.itemId.toRoomObjectTypeOrNull()
            ?: return@mapNotNull null
        val template = baseScene.objects.firstOrNull { it.type == type }
            ?: return@mapNotNull null
        val tilePlacement = placedItem.tile?.toFloorTilePlacement(
            projectionSpec = baseScene.projectionSpec,
            type = type
        )
        val anchor = tilePlacement?.toFloorAnchor(baseScene.projectionSpec)
            ?: FloorAnchor(
                u = placedItem.anchor.u.coerceIn(0f, 1f),
                v = placedItem.anchor.v.coerceIn(0f, 1f)
            )

        template.copy(
            id = placedItem.placedItemId,
            anchor = anchor,
            tilePlacement = tilePlacement,
            clickable = false
        )
    }
}

private fun FriendTileDto.toFloorTilePlacement(
    projectionSpec: IsoRoomProjectionSpec,
    type: RoomObjectType
): FloorTilePlacement {
    val roomWidthTiles = projectionSpec.roomWidthTiles.toInt().coerceAtLeast(1)
    val roomDepthTiles = projectionSpec.roomDepthTiles.toInt().coerceAtLeast(1)
    val safeWidthTiles = widthTiles.coerceIn(1, roomWidthTiles)
    val safeDepthTiles = depthTiles.coerceIn(1, roomDepthTiles)
    val maxX = (roomWidthTiles - safeWidthTiles).coerceAtLeast(0)
    val maxY = (roomDepthTiles - safeDepthTiles).coerceAtLeast(0)

    return FloorTilePlacement(
        tile = TileCoord(
            x = x.coerceIn(0, maxX),
            y = y.coerceIn(0, maxY)
        ),
        footprint = TileFootprint(
            widthTiles = safeWidthTiles,
            depthTiles = safeDepthTiles
        ),
        anchorMode = resolvedAnchorModeFor(type, anchorMode)
    )
}

private fun resolvedAnchorModeFor(type: RoomObjectType, rawAnchorMode: String?): TileAnchorMode {
    if (type == RoomObjectType.BED) {
        return TileAnchorMode.FRONT_CENTER
    }
    return enumValueOrDefault(rawAnchorMode ?: TileAnchorMode.CENTER.name, TileAnchorMode.CENTER)
}

private fun String?.toRoomObjectTypeOrNull(): RoomObjectType? {
    val normalized = this
        ?.uppercase()
        ?.replace(Regex("[^A-Z0-9]+"), "_")
        ?: return null

    return when {
        "BED" in normalized -> RoomObjectType.BED
        "TOY" in normalized -> RoomObjectType.TOY_BOX
        "FOOD" in normalized || "FEED" in normalized -> RoomObjectType.FOOD_BAG
        "WINDOW" in normalized -> RoomObjectType.WINDOW
        else -> null
    }
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
        cleanliness = cleanliness.coerceIn(0, 100),
        isEgg = isEgg
    )
}

private fun FriendPetConditionDto.toDomain(): PetStatus {
    return PetStatus(
        satiety = satiety.coerceIn(0, 100),
        vitality = vitality.coerceIn(0, 100),
        cleanliness = cleanliness.coerceIn(0, 100),
        isEgg = isEgg
    )
}

private fun FriendPetSnapshotDto.toPetSceneState(): PetSceneState {
    return PetSceneState(
        petId = petId,
        ownerUserId = ownerUserId,
        name = name,
        characterAssetKey = characterAssetKey,
        appearance = appearance.toDomain(),
        status = condition.toDomain(),
        traits = traits.toDomain(),
        equippedItemIds = equippedItemIds,
        action = enumValueOrDefault(sceneState.action, PetAction.IDLE),
        anchor = sceneState.anchor.toFloorAnchor()
    )
}

private fun FriendAnchorDto.toFloorAnchor(): FloorAnchor {
    return FloorAnchor(
        u = u.coerceIn(0f, 1f),
        v = v.coerceIn(0f, 1f)
    )
}

private fun FriendHomeInvitationDto.fromUserOrFallback(): FriendUserDto {
    return fromUser ?: senderId?.toFallbackFriendUserDto(nickname = "친구")
        ?: fallbackFriendUserDto()
}

private fun FriendHomeInvitationDto.toUserOrFallback(): FriendUserDto {
    return toUser ?: receiverId?.toFallbackFriendUserDto(nickname = "나")
        ?: fallbackFriendUserDto(userId = "me", nickname = "나")
}

private fun Long.toFallbackFriendUserDto(nickname: String): FriendUserDto {
    return fallbackFriendUserDto(
        userId = "user_$this",
        nickname = nickname
    )
}

private fun fallbackFriendUserDto(
    userId: String = "friend_user",
    nickname: String = "친구"
): FriendUserDto {
    return FriendUserDto(
        userId = userId,
        nickname = nickname,
        friendCode = "LUPA00000"
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
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(this)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(0L)
    }
}
