package com.example.lupapj.data.mock

import com.example.lupapj.data.model.PetPersonality
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.plaza.PLAZA_MAX_PARTICIPANTS
import com.example.lupapj.data.model.plaza.PLAZA_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.plaza.PlazaChatMessage
import com.example.lupapj.data.model.plaza.PlazaCode
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaOperationResult
import com.example.lupapj.data.model.plaza.PlazaParticipant
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaRoom
import com.example.lupapj.data.model.plaza.PlazaServerTime
import com.example.lupapj.data.repository.PlazaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockPlazaRepository(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val simulatedDelayMillis: Long = 120L
) : PlazaRepository {
    private val _activePlaza = MutableStateFlow<PlazaRoom?>(null)
    override val activePlaza: StateFlow<PlazaRoom?> = _activePlaza.asStateFlow()

    private val plazaPool = mutableListOf<PlazaRoom>()
    private var nextPlazaSequence = 1
    private var nextMessageSequence = 1

    override suspend fun joinRandomPlaza(
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaOperationResult<PlazaRoom> {
        simulateLatency()
        removeParticipantFromOpenPlazas(currentUserId)

        val room = findAvailableRoomForRandomJoin(currentUserId)
            ?: createRoom(
                plazaCode = createRandomCode(),
                remoteParticipants = nextDemoParticipantsForNewRoom()
            )
        val joinedRoom = room.addParticipant(
            currentUserId = currentUserId,
            nickname = nickname,
            pet = pet
        ) ?: return PlazaOperationResult.Failure(PlazaOperationFailure.PLAZA_FULL)

        upsertRoom(joinedRoom)
        _activePlaza.value = joinedRoom
        return PlazaOperationResult.Success(joinedRoom)
    }

    override suspend fun joinPlazaByCode(
        codeInput: String,
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaOperationResult<PlazaRoom> {
        simulateLatency()

        if (codeInput.isBlank()) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.EMPTY_CODE)
        }

        val code = PlazaCode.fromInput(codeInput)
            ?: return PlazaOperationResult.Failure(PlazaOperationFailure.INVALID_CODE)

        if (code.value.endsWith("4040")) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.PLAZA_NOT_FOUND)
        }

        val room = findRoomByCode(code) ?: createRoom(
            plazaCode = code,
            remoteParticipants = when {
                code.value.endsWith("FULL") -> demoParticipants.take(PLAZA_MAX_PARTICIPANTS)
                code.value.endsWith("SOLO") -> emptyList()
                else -> demoParticipants.take(2)
            }
        )
        val joinedRoom = room.addParticipant(
            currentUserId = currentUserId,
            nickname = nickname,
            pet = pet
        ) ?: return PlazaOperationResult.Failure(PlazaOperationFailure.PLAZA_FULL)

        if (joinedRoom.participants.size > joinedRoom.maxParticipants) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.PLAZA_FULL)
        }

        removeParticipantFromOpenPlazas(currentUserId)
        upsertRoom(joinedRoom)
        _activePlaza.value = joinedRoom
        return PlazaOperationResult.Success(joinedRoom)
    }

    override suspend fun leavePlaza(): PlazaOperationResult<Unit> {
        simulateLatency()
        val activeRoom = _activePlaza.value
        val currentUserId = activeRoom?.participants?.firstOrNull { it.isMe }?.userId
        if (currentUserId != null) {
            removeParticipantFromOpenPlazas(currentUserId)
        }
        _activePlaza.value = null
        return PlazaOperationResult.Success(Unit)
    }

    override suspend fun sendMessage(
        message: String
    ): PlazaOperationResult<PlazaChatMessage> {
        simulateLatency()

        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.EMPTY_MESSAGE)
        }
        if (trimmedMessage.length > PLAZA_MESSAGE_MAX_LENGTH) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.MESSAGE_TOO_LONG)
        }

        val room = _activePlaza.value
            ?: return PlazaOperationResult.Failure(PlazaOperationFailure.NOT_IN_PLAZA)
        val me = room.participants.firstOrNull { it.isMe }
            ?: return PlazaOperationResult.Failure(PlazaOperationFailure.NOT_IN_PLAZA)
        val now = nowProvider()
        val chatMessage = PlazaChatMessage(
            id = "plaza-message-${nextMessageSequence++}",
            plazaId = room.plazaId,
            senderUserId = me.userId,
            senderNickname = me.nickname,
            text = trimmedMessage,
            sentAtMillis = now
        )

        val updatedRoom = room.copy(
            messages = room.messages + chatMessage,
            roomRevision = room.roomRevision + 1L,
            serverTime = serverTimeSnapshot(now)
        )
        upsertRoom(updatedRoom)
        _activePlaza.value = updatedRoom
        return PlazaOperationResult.Success(chatMessage)
    }

    private fun createRoom(
        plazaCode: PlazaCode,
        remoteParticipants: List<PlazaParticipant>
    ): PlazaRoom {
        val plazaId = "plaza-${nextPlazaSequence++}"
        val now = nowProvider()
        val participants = remoteParticipants
            .take(PLAZA_MAX_PARTICIPANTS)
            .mapIndexed { index, participant ->
                participant.copy(
                    joinedAtMillis = now - (index + 1) * 90_000L,
                    isMe = false
                )
            }

        return PlazaRoom(
            plazaId = plazaId,
            plazaCode = plazaCode,
            participants = participants,
            messages = createInitialMessages(plazaId, participants, now),
            joinedAtMillis = now,
            roomRevision = 1L,
            serverTime = serverTimeSnapshot(now)
        )
    }

    private fun PlazaRoom.addParticipant(
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaRoom? {
        val currentParticipants = participants
            .filterNot { it.userId == currentUserId }
            .map { it.copy(isMe = false) }
        if (currentParticipants.size >= maxParticipants) return null

        val now = nowProvider()
        val me = PlazaParticipant(
            userId = currentUserId,
            nickname = nickname,
            pet = pet.copy(ownerUserId = currentUserId),
            joinedAtMillis = now,
            isMe = true
        )
        return copy(
            participants = currentParticipants + me,
            joinedAtMillis = now,
            roomRevision = roomRevision + 1L,
            serverTime = serverTimeSnapshot(now)
        )
    }

    private fun findAvailableRoomForRandomJoin(currentUserId: String): PlazaRoom? {
        return plazaPool.firstOrNull { room ->
            room.participants.size < room.maxParticipants &&
                room.participants.none { it.userId == currentUserId }
        }
    }

    private fun findRoomByCode(code: PlazaCode): PlazaRoom? {
        return plazaPool.firstOrNull { it.plazaCode.value == code.value }
    }

    private fun upsertRoom(room: PlazaRoom) {
        val index = plazaPool.indexOfFirst { it.plazaId == room.plazaId }
        if (index >= 0) {
            plazaPool[index] = room
        } else {
            plazaPool += room
        }
    }

    private fun removeParticipantFromOpenPlazas(userId: String) {
        val updatedRooms = plazaPool.mapNotNull { room ->
            val participants = room.participants
                .filterNot { it.userId == userId }
                .map { it.copy(isMe = false) }

            if (participants.isEmpty()) {
                null
            } else {
                room.copy(
                    participants = participants,
                    roomRevision = room.roomRevision + 1L,
                    serverTime = serverTimeSnapshot()
                )
            }
        }

        plazaPool.clear()
        plazaPool.addAll(updatedRooms)
    }

    private fun createInitialMessages(
        plazaId: String,
        participants: List<PlazaParticipant>,
        joinedAtMillis: Long
    ): List<PlazaChatMessage> {
        return participants
            .filterNot { it.isMe }
            .take(2)
            .mapIndexed { index, participant ->
                PlazaChatMessage(
                    id = "plaza-message-${nextMessageSequence++}",
                    plazaId = plazaId,
                    senderUserId = participant.userId,
                    senderNickname = participant.nickname,
                    text = if (index == 0) {
                        "어서 와! 잠깐 쉬다 가."
                    } else {
                        "광장 코드 공유하면 친구도 들어올 수 있어."
                    },
                    sentAtMillis = joinedAtMillis - (2 - index) * 60_000L
                )
            }
    }

    private fun createRandomCode(): PlazaCode {
        while (true) {
            val suffix = (1000..9999).random()
            val code = PlazaCode.fromInput("PZ$suffix") ?: error("Invalid generated plaza code.")
            if (plazaPool.none { it.plazaCode.value == code.value }) {
                return code
            }
        }
    }

    private fun nextDemoParticipantsForNewRoom(): List<PlazaParticipant> {
        val count = when (nextPlazaSequence % 3) {
            0 -> 3
            1 -> 1
            else -> 2
        }
        return demoParticipants.take(count)
    }

    private suspend fun simulateLatency() {
        if (simulatedDelayMillis > 0L) {
            delay(simulatedDelayMillis)
        }
    }

    private fun serverTimeSnapshot(serverNowMillis: Long = nowProvider()): PlazaServerTime {
        return PlazaServerTime(
            serverNowMillis = serverNowMillis,
            clientReceivedAtMillis = System.currentTimeMillis()
        )
    }

    private companion object {
        val demoParticipants = listOf(
            PlazaParticipant(
                userId = "plaza_user_mina",
                nickname = "미나",
                pet = plazaPet(
                    petId = "plaza_pet_mina",
                    ownerUserId = "plaza_user_mina",
                    name = "몽글",
                    status = PetStatus(satiety = 88, vitality = 74),
                    personality = PetPersonality.CALM
                ),
                joinedAtMillis = 0L
            ),
            PlazaParticipant(
                userId = "plaza_user_haru",
                nickname = "하루",
                pet = plazaPet(
                    petId = "plaza_pet_haru",
                    ownerUserId = "plaza_user_haru",
                    name = "콩이",
                    status = PetStatus(satiety = 71, vitality = 92),
                    personality = PetPersonality.ACTIVE
                ),
                joinedAtMillis = 0L
            ),
            PlazaParticipant(
                userId = "plaza_user_bori",
                nickname = "보리",
                pet = plazaPet(
                    petId = "plaza_pet_bori",
                    ownerUserId = "plaza_user_bori",
                    name = "포포",
                    status = PetStatus(satiety = 64, vitality = 67),
                    personality = PetPersonality.LAZY
                ),
                joinedAtMillis = 0L
            ),
            PlazaParticipant(
                userId = "plaza_user_nari",
                nickname = "나리",
                pet = plazaPet(
                    petId = "plaza_pet_nari",
                    ownerUserId = "plaza_user_nari",
                    name = "라라",
                    status = PetStatus(satiety = 79, vitality = 81),
                    personality = PetPersonality.ACTIVE
                ),
                joinedAtMillis = 0L
            )
        )

        fun plazaPet(
            petId: String,
            ownerUserId: String,
            name: String,
            status: PetStatus,
            personality: PetPersonality
        ): PlazaPetSnapshot {
            return PlazaPetSnapshot(
                petId = petId,
                ownerUserId = ownerUserId,
                name = name,
                status = status,
                personality = personality
            )
        }
    }
}
