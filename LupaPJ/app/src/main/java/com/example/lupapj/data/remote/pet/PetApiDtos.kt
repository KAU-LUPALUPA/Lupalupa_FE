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
    val satiety: Int? = null,
    val vitality: Int? = null,
    val cleanliness: Int? = null, // [추가됨(V2)]
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
    val traits: PetTraitsDto, // [변경됨(V2)] personality -> traits
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
    val satiety: Int,
    val vitality: Int,
    val cleanliness: Int, // [추가됨(V2)]
    val isEgg: Boolean
)

data class PetTraitsDto( // [추가됨(V2)]
    val activity: Float,
    val appetite: Float,
    val attention: Float,
    val curiosity: Float,
    val patience: Float
)

data class PetAnchorDto(
    val u: Float,
    val v: Float
)
