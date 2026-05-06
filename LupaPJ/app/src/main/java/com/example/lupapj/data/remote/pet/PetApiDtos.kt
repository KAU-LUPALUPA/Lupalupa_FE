package com.example.lupapj.data.remote.pet

data class ValidatePetRequestDto(
    val localPetRevision: Int?,
    val localPetHash: String?,
    val localUpdatedAt: String?
)

data class PetValidationResponseDto(
    val syncStatus: String,
    val serverPetRevision: Int?,
    val serverPetHash: String?,
    val serverUpdatedAt: String?,
    val pet: PetDto? = null
)

data class PetResponseDto(
    val pet: PetDto
)

data class UpdatePetStatusRequestDto(
    val hunger: Int? = null,
    val fatigue: Int? = null,
    val isEgg: Boolean? = null,
    val action: String? = null,
    val anchor: PetAnchorDto? = null
)

data class UpdatePetEquipmentRequestDto(
    val equippedItemIds: List<String>
)

data class UpdatePetEquipmentResponseDto(
    val equippedItemIds: List<String>,
    val petRevision: Int,
    val petHash: String,
    val updatedAt: String
)

data class PetDto(
    val petId: String,
    val ownerUserId: String,
    val name: String,
    val characterAssetKey: String,
    val appearance: PetAppearanceDto,
    val status: PetStatusDto,
    val personality: String,
    val equippedItemIds: List<String> = emptyList(),
    val action: String,
    val anchor: PetAnchorDto,
    val petRevision: Int,
    val petHash: String,
    val updatedAt: String
)

data class PetAppearanceDto(
    val headSizeScale: Float,
    val bodySizeScale: Float,
    val eyeSizeScale: Float,
    val noseSizeScale: Float,
    val mouthSizeScale: Float
)

data class PetStatusDto(
    val hunger: Int,
    val fatigue: Int,
    val isEgg: Boolean
)

data class PetAnchorDto(
    val u: Float,
    val v: Float
)
