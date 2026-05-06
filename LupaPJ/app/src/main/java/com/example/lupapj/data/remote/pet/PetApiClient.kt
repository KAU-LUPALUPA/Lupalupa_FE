package com.example.lupapj.data.remote.pet

interface PetApiClient {
    suspend fun validateMyPet(
        request: ValidatePetRequestDto
    ): PetValidationResponseDto

    suspend fun getMyPet(): PetResponseDto

    suspend fun updatePetStatus(
        request: UpdatePetStatusRequestDto
    ): PetResponseDto

    suspend fun updatePetEquipment(
        request: UpdatePetEquipmentRequestDto
    ): UpdatePetEquipmentResponseDto
}

class PetApiException(
    val code: String,
    val httpStatus: Int? = null,
    override val message: String? = null
) : Exception(message)
