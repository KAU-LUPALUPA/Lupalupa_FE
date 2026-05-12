package com.example.lupapj.data.remote.friend

data class FriendCodeResponseDto(
    val friendCode: String,
    val displayFriendCode: String? = null
)

data class FriendUserLookupResponseDto(
    val user: FriendUserDto?,
    val relationshipStatus: String? = null
)

data class SendFriendRequestDto(
    val friendCode: String
)

data class FriendRequestResponseDto(
    val request: FriendRequestDto
)

data class FriendRequestsResponseDto(
    val requests: List<FriendRequestDto>
)

data class AcceptFriendRequestResponseDto(
    val request: FriendRequestDto,
    val friendship: FriendshipDto
)

data class FriendsResponseDto(
    val friends: List<FriendshipDto>
)

data class FriendHomeInvitationsResponseDto(
    val invitations: List<FriendHomeInvitationDto>
)

data class FriendHomeInvitationResponseDto(
    val invitation: FriendHomeInvitationDto
)

data class AcceptHomeInvitationResponseDto(
    val invitation: FriendHomeInvitationDto,
    val homeSnapshot: FriendHomeSnapshotDto
)

data class FriendHomeResponseDto(
    val owner: FriendUserDto,
    val room: FriendRoomDto,
    val pet: FriendPetDto? = null,
    val visitedAt: String
)

data class FriendMessagesResponseDto(
    val messages: List<FriendMessageDto>,
    val nextCursor: String? = null
)

data class SendFriendMessageRequestDto(
    val text: String
)

data class FriendMessageResponseDto(
    val message: FriendMessageDto
)

data class FriendUserDto(
    val userId: String,
    val nickname: String,
    val friendCode: String,
    val displayFriendCode: String? = null,
    val avatarAssetKey: String? = null
)

data class FriendRequestDto(
    val id: String,
    val fromUser: FriendUserDto,
    val toUser: FriendUserDto,
    val status: String,
    val createdAt: String,
    val respondedAt: String? = null
)

data class FriendshipDto(
    val friendshipId: String,
    val friend: FriendUserDto? = null,
    val user: FriendUserDto? = null,
    val status: String,
    val friendsSince: String
)

data class FriendHomeInvitationDto(
    val id: String,
    val fromUser: FriendUserDto,
    val toUser: FriendUserDto,
    val status: String,
    val message: String? = null,
    val createdAt: String,
    val respondedAt: String? = null,
    val expiresAt: String? = null
)

data class FriendMessageDto(
    val id: String,
    val friendUserId: String,
    val senderUserId: String,
    val text: String,
    val sentAt: String
)

data class FriendRoomDto(
    val sceneId: String,
    val wallAssetKey: String? = null,
    val floorAssetKey: String? = null,
    val placedItems: List<FriendPlacedItemDto> = emptyList(),
    val layoutRevision: Int? = null,
    val updatedAt: String? = null
)

data class FriendPlacedItemDto(
    val placedItemId: String,
    val itemId: String,
    val objectType: String,
    val anchorType: String,
    val anchor: FriendAnchorDto,
    val tile: FriendTileDto? = null
)

data class FriendAnchorDto(
    val u: Float,
    val v: Float
)

data class FriendTileDto(
    val x: Int,
    val y: Int,
    val widthTiles: Int,
    val depthTiles: Int,
    val anchorMode: String
)

data class FriendPetDto(
    val petId: String,
    val ownerUserId: String,
    val name: String,
    val characterAssetKey: String,
    val appearance: FriendPetAppearanceDto,
    val status: FriendPetStatusDto,
    val personality: String,
    val equippedItemIds: List<String> = emptyList(),
    val anchor: FriendAnchorDto
)

data class FriendHomeSnapshotDto(
    val owner: FriendUserDto,
    val room: FriendRoomDto,
    val petSnapshot: FriendPetSnapshotDto? = null,
    val snapshotAt: String? = null,
    val visitedAt: String
)

data class FriendPetSnapshotDto(
    val petId: String,
    val ownerUserId: String,
    val name: String,
    val characterAssetKey: String,
    val appearance: FriendPetAppearanceDto,
    val condition: FriendPetConditionDto,
    val sceneState: FriendPetSceneStateDto,
    val personality: String,
    val equippedItemIds: List<String> = emptyList()
)

data class FriendPetAppearanceDto(
    val headSizeScale: Float,
    val bodySizeScale: Float,
    val eyeSizeScale: Float,
    val noseSizeScale: Float,
    val mouthSizeScale: Float
)

data class FriendPetStatusDto(
    val satiety: Int,
    val vitality: Int,
    val isEgg: Boolean
)

data class FriendPetConditionDto(
    val satiety: Int,
    val vitality: Int,
    val isEgg: Boolean
)

data class FriendPetSceneStateDto(
    val action: String,
    val anchor: FriendAnchorDto
)
