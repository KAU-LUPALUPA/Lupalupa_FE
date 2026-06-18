package com.example.lupapj.data.remote.plaza

class EmptyPlazaRequestDto

data class JoinPlazaByCodeRequestDto(
    val code: String
)

data class SendPlazaMessageRequestDto(
    val text: String
)

data class PlazaRoomEnvelopeDto(
    val plaza: PlazaRoomResponseDto? = null
)

data class PlazaChatMessageEnvelopeDto(
    val message: PlazaChatMessageResponseDto? = null,
    val roomRevision: Long? = null
)

data class PlazaRoomResponseDto(
    val plazaId: String? = null,
    val plazaCode: String? = null,
    val displayPlazaCode: String? = null,
    val serverNowMillis: Long? = null,
    val participants: List<PlazaParticipantResponseDto>? = null,
    val messages: List<PlazaChatMessageResponseDto>? = null,
    val interactions: List<PlazaInteractionResponseDto>? = null,
    val maxParticipants: Int? = null,
    val roomRevision: Long? = null
)

data class PlazaParticipantResponseDto(
    val userId: String? = null,
    val nickname: String? = null,
    val pet: PlazaPetSnapshotResponseDto? = null,
    val position: PlazaPositionResponseDto? = null,
    val movement: PlazaMovementResponseDto? = null,
    val positionUpdatedAtMillis: Long? = null,
    val joinedAtMillis: Long? = null
)

data class PlazaMovementResponseDto(
    val from: PlazaPositionResponseDto? = null,
    val to: PlazaPositionResponseDto? = null,
    val startedAtMillis: Long? = null,
    val durationMillis: Long? = null
)

data class PlazaInteractionResponseDto(
    val id: String? = null,
    val type: String? = null,
    val actorUserId: String? = null,
    val targetUserId: String? = null,
    val textByUserId: Map<String, String>? = null,
    val startedAtMillis: Long? = null,
    val durationMillis: Long? = null,
    val movementTargetByUserId: Map<String, PlazaPositionResponseDto>? = null,
    val facingTargetByUserId: Map<String, PlazaPositionResponseDto>? = null,
    val animationByUserId: Map<String, String>? = null
)

data class PlazaPetSnapshotResponseDto(
    val petId: String? = null,
    val name: String? = null,
    val characterAssetKey: String? = null,
    val appearance: PetAppearanceResponseDto? = null
)

data class PetAppearanceResponseDto(
    val headSizeScale: Float? = null,
    val bodySizeScale: Float? = null,
    val eyeSizeScale: Float? = null,
    val noseSizeScale: Float? = null,
    val mouthSizeScale: Float? = null
)

data class PlazaChatMessageResponseDto(
    val id: String? = null,
    val senderUserId: String? = null,
    val senderNickname: String? = null,
    val text: String? = null,
    val sentAtMillis: Long? = null
)

data class PlazaPositionResponseDto(
    val x: Float? = null,
    val y: Float? = null
)
