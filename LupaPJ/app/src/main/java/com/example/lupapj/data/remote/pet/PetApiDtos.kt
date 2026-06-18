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

data class PetStatusSyncRequestDto(
    val satiety: Int,
    val vitality: Int,
    val cleanliness: Int,
    val offlineSync: Boolean
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
    val traits: PetTraitsDto? = null, // [변경됨(V2)] personality -> traits (fallback 지원)
    val interactions: InteractionEventsDto? = null, // [추가됨(V2)] 누적 횟수 (fallback 지원)
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
    val cleanliness: Int? = null, // [추가됨(V2)] fallback 지원
    val isEgg: Boolean
)

data class PetTraitsDto( // [추가됨(V2)]
    val activity: Float? = null,
    val appetite: Float? = null,
    val attention: Float? = null,
    val curiosity: Float? = null,
    val patience: Float? = null
)

data class InteractionEventsDto( // [추가됨(V2)]
    val feedCount: Int? = null,
    val playCount: Int? = null,
    val cleanCommandCount: Int? = null,
    val sleepCommandCount: Int? = null,
    val daysActive: Int? = null
)

data class PetAnchorDto(
    val u: Float,
    val v: Float
)
