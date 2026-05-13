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
    val participants: List<PlazaParticipantResponseDto>? = null,
    val messages: List<PlazaChatMessageResponseDto>? = null,
    val maxParticipants: Int? = null,
    val roomRevision: Long? = null
)

data class PlazaParticipantResponseDto(
    val userId: String? = null,
    val nickname: String? = null,
    val pet: PlazaPetSnapshotResponseDto? = null,
    val position: PlazaPositionResponseDto? = null,
    val joinedAtMillis: Long? = null
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
