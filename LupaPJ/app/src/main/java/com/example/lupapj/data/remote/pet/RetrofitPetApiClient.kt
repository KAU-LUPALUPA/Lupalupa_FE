package com.example.lupapj.data.remote.pet

import retrofit2.Response

class RetrofitPetApiClient(
    private val service: PetRetrofitService
) : PetApiClient {
    override suspend fun validateMyPet(request: ValidatePetRequestDto): PetValidationResponseDto =
        service.validateMyPet(request).bodyOrThrow()

    override suspend fun getMyPet(): PetDto =
        service.getMyPet().bodyOrThrow()

    override suspend fun updatePetStatus(request: UpdatePetStatusRequestDto): PetDto =
        service.updatePetStatus(request).bodyOrThrow()

    override suspend fun updatePetEquipment(request: UpdatePetEquipmentRequestDto): UpdatePetEquipmentResponseDto =
        service.updatePetEquipment(request).bodyOrThrow()

    override suspend fun feedPet(petId: String): PetDto =
        service.feedPet(petId).bodyOrThrow()

    override suspend fun sleepPet(petId: String): PetDto =
        service.sleepPet(petId).bodyOrThrow()

    override suspend fun playPet(petId: String): PetDto =
        service.playPet(petId).bodyOrThrow()

    override suspend fun cleanPet(petId: String): PetDto =
        service.cleanPet(petId).bodyOrThrow()

    override suspend fun syncPetStatus(petId: String, request: PetStatusSyncRequestDto): PetDto =
        service.syncPetStatus(petId, request).bodyOrThrow()

    override suspend fun updateTraitsDebug(petId: String, request: PetTraitsDto): PetDto =
        service.updateTraitsDebug(petId, request).bodyOrThrow()

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) throw PetApiException("HTTP_${code()}", code(), message())
        return body() ?: throw PetApiException("EMPTY_BODY", code(), "Empty body")
    }
}
