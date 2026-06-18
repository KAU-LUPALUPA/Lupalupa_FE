package com.example.lupapj.data.remote.pet

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PetRetrofitService {
    @POST("pets/me/validate")
    suspend fun validateMyPet(
        @Body request: ValidatePetRequestDto
    ): Response<PetValidationResponseDto>

    @GET("pets/me")
    suspend fun getMyPet(): Response<PetDto>

    @PUT("pets/me/status")
    suspend fun updatePetStatus(
        @Body request: UpdatePetStatusRequestDto
    ): Response<PetDto>

    @PUT("pets/me/equipment")
    suspend fun updatePetEquipment(
        @Body request: UpdatePetEquipmentRequestDto
    ): Response<UpdatePetEquipmentResponseDto>

    @POST("api/pets/{petId}/feed")
    suspend fun feedPet(@Path("petId") petId: String): Response<PetDto>

    @POST("api/pets/{petId}/sleep")
    suspend fun sleepPet(@Path("petId") petId: String): Response<PetDto>

    @POST("api/pets/{petId}/play")
    suspend fun playPet(@Path("petId") petId: String): Response<PetDto>

    @POST("api/pets/{petId}/clean")
    suspend fun cleanPet(@Path("petId") petId: String): Response<PetDto>

    @retrofit2.http.PATCH("api/pets/{petId}/status")
    suspend fun syncPetStatus(
        @Path("petId") petId: String,
        @Body request: PetStatusSyncRequestDto
    ): Response<PetDto>

    @PUT("api/pets/{petId}/traits/debug")
    suspend fun updateTraitsDebug(
        @Path("petId") petId: String,
        @Body request: PetTraitsDto
    ): Response<PetDto>
}
