package com.example.lupapj.data.remote.pet

interface PetApiClient {
    suspend fun validateMyPet(
        request: ValidatePetRequestDto
    ): PetValidationResponseDto

    suspend fun getMyPet(): PetDto

    suspend fun updatePetStatus(
        request: UpdatePetStatusRequestDto
    ): PetDto

    suspend fun updatePetEquipment(
        request: UpdatePetEquipmentRequestDto
    ): UpdatePetEquipmentResponseDto

    suspend fun feedPet(petId: String): PetDto
    suspend fun sleepPet(petId: String): PetDto
    suspend fun playPet(petId: String): PetDto
    suspend fun cleanPet(petId: String): PetDto
    suspend fun syncPetStatus(petId: String, request: PetStatusSyncRequestDto): PetDto
    suspend fun updateTraitsDebug(petId: String, request: PetTraitsDto): PetDto
}

class PetApiException(
    val code: String,
    val httpStatus: Int? = null,
    override val message: String? = null
) : Exception(message)
