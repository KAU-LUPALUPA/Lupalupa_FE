package com.example.lupapj.data.remote.room

import com.example.lupapj.data.local.RoomCache
import com.example.lupapj.data.local.RoomLayoutSnapshot
import com.example.lupapj.data.mock.DemoScenes
import com.example.lupapj.data.model.scene.RoomSceneId
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteRoomRepositoryTest {
    private val gson = Gson()

    @Test
    fun getRoom_withoutCachedSnapshotFetchesServerLayoutAndCachesIt() = runBlocking {
        val serverLayout = layout(revision = 7, hash = "sha256:server")
        val apiClient = FakeRoomLayoutApiClient(
            getResponse = RoomLayoutResponseDto(serverLayout)
        )
        val cache = FakeRoomCache()
        val repository = createRepository(apiClient, cache)

        val room = repository.getRoom()

        assertEquals(0, apiClient.validateCallCount)
        assertEquals(1, apiClient.getCallCount)
        assertEquals(7, room.layoutRevision)
        assertEquals("sha256:server", room.layoutHash)
        assertEquals("sha256:server", cache.snapshot?.layoutHash)
    }

    @Test
    fun getRoom_withMatchingCachedSnapshotUsesCacheWithoutFetchingLayout() = runBlocking {
        val cachedLayout = layout(revision = 7, hash = "sha256:cached", bedX = 3, bedY = 2)
        val apiClient = FakeRoomLayoutApiClient(
            validateResponse = RoomLayoutValidationResponseDto(
                syncStatus = "MATCH",
                serverLayoutRevision = cachedLayout.layoutRevision,
                serverLayoutHash = cachedLayout.layoutHash,
                serverUpdatedAt = cachedLayout.updatedAt,
                roomLayout = null
            )
        )
        val cache = FakeRoomCache(snapshot = snapshotOf(cachedLayout))
        val repository = createRepository(apiClient, cache)

        val room = repository.getRoom()

        assertEquals(1, apiClient.validateCallCount)
        assertEquals(0, apiClient.getCallCount)
        assertEquals("sha256:cached", room.layoutHash)
        assertEquals(3, room.sceneDefinition.objects.first { it.type.name == "BED" }.tilePlacement?.tile?.x)
    }

    @Test
    fun getRoom_withServerUpdatedValidationUsesReturnedServerLayout() = runBlocking {
        val cachedLayout = layout(revision = 7, hash = "sha256:cached")
        val serverLayout = layout(revision = 8, hash = "sha256:server", bedX = 2, bedY = 1)
        val apiClient = FakeRoomLayoutApiClient(
            validateResponse = RoomLayoutValidationResponseDto(
                syncStatus = "SERVER_UPDATED",
                serverLayoutRevision = serverLayout.layoutRevision,
                serverLayoutHash = serverLayout.layoutHash,
                serverUpdatedAt = serverLayout.updatedAt,
                roomLayout = serverLayout
            )
        )
        val cache = FakeRoomCache(snapshot = snapshotOf(cachedLayout))
        val repository = createRepository(apiClient, cache)

        val room = repository.getRoom()

        assertEquals(1, apiClient.validateCallCount)
        assertEquals(0, apiClient.getCallCount)
        assertEquals(8, room.layoutRevision)
        assertEquals("sha256:server", room.layoutHash)
        assertEquals("sha256:server", cache.snapshot?.layoutHash)
    }

    @Test
    fun getRoom_whenValidationFailsFallsBackToCachedSnapshot() = runBlocking {
        val cachedLayout = layout(revision = 7, hash = "sha256:cached")
        val apiClient = FakeRoomLayoutApiClient(
            validateException = IllegalStateException("network down")
        )
        val cache = FakeRoomCache(snapshot = snapshotOf(cachedLayout))
        val repository = createRepository(apiClient, cache)

        val room = repository.getRoom()

        assertEquals(1, apiClient.validateCallCount)
        assertEquals(0, apiClient.getCallCount)
        assertEquals(7, room.layoutRevision)
        assertEquals("sha256:cached", room.layoutHash)
    }

    @Test
    fun saveRoomLayout_whenSuccessfulCachesSavedLayout() = runBlocking {
        val before = layout(revision = 7, hash = "sha256:before").toDomainRoomState(::sceneFor)
        val saved = layout(revision = 8, hash = "sha256:saved")
        val apiClient = FakeRoomLayoutApiClient(
            saveResponse = RoomLayoutResponseDto(saved)
        )
        val cache = FakeRoomCache()
        val repository = createRepository(apiClient, cache)

        val room = repository.saveRoomLayout(before)

        assertEquals(1, apiClient.saveCallCount)
        assertEquals(8, room.layoutRevision)
        assertEquals("sha256:saved", cache.snapshot?.layoutHash)
    }

    @Test
    fun saveRoomLayout_whenApiFailsDoesNotUpdateCachedSnapshot() = runBlocking {
        val before = layout(revision = 7, hash = "sha256:before").toDomainRoomState(::sceneFor)
        val originalSnapshot = snapshotOf(layout(revision = 7, hash = "sha256:cached"))
        val apiClient = FakeRoomLayoutApiClient(
            saveException = RoomLayoutApiException(code = "ROOM_LAYOUT_CONFLICT", httpStatus = 409)
        )
        val cache = FakeRoomCache(snapshot = originalSnapshot)
        val repository = createRepository(apiClient, cache)

        runCatching { repository.saveRoomLayout(before) }

        assertEquals(1, apiClient.saveCallCount)
        assertEquals("sha256:cached", cache.snapshot?.layoutHash)
    }

    private fun createRepository(
        apiClient: FakeRoomLayoutApiClient,
        cache: FakeRoomCache
    ): RemoteRoomRepository {
        return RemoteRoomRepository(
            apiClient = apiClient,
            localCache = cache,
            sceneResolver = ::sceneFor,
            gson = gson
        )
    }

    private fun sceneFor(sceneId: String) = DemoScenes.sceneFor(RoomSceneId(sceneId))

    private fun snapshotOf(layout: RoomLayoutDto): RoomLayoutSnapshot {
        return RoomLayoutSnapshot(
            layoutRevision = layout.layoutRevision,
            layoutHash = layout.layoutHash,
            updatedAt = layout.updatedAt,
            layoutJson = gson.toJson(layout)
        )
    }

    private fun layout(
        revision: Int,
        hash: String,
        bedX: Int = 0,
        bedY: Int = 0
    ): RoomLayoutDto {
        return RoomLayoutDto(
            roomId = "room-1",
            ownerUserId = "owner-1",
            sceneId = "main_room",
            layoutRevision = revision,
            layoutHash = hash,
            wallAssetKey = "room/walls/main_wall",
            floorAssetKey = "room/floors/main_floor",
            placedItems = listOf(
                placedItem("BED", bedX, bedY, 2, 2),
                placedItem("TOY_BOX", 0, 4, 1, 1),
                placedItem("FOOD_BAG", 1, 3, 1, 1)
            ),
            updatedAt = "2026-05-19T10:00:00"
        )
    }

    private fun placedItem(
        type: String,
        x: Int,
        y: Int,
        widthTiles: Int,
        depthTiles: Int
    ): PlacedRoomItemDto {
        return PlacedRoomItemDto(
            placementId = "placement_${type.lowercase()}",
            shopItemId = type,
            assetKey = when (type) {
                "BED" -> "room/objects/bed_basic"
                "TOY_BOX" -> "room/objects/toy_box_basic"
                else -> "room/objects/food_bag_basic"
            },
            type = type,
            anchorType = "FLOOR",
            tilePlacement = TilePlacementDto(
                tile = TileCoordDto(x = x, y = y),
                footprint = TileFootprintDto(widthTiles = widthTiles, depthTiles = depthTiles),
                anchorMode = "CENTER"
            )
        )
    }

    private class FakeRoomLayoutApiClient(
        private val validateResponse: RoomLayoutValidationResponseDto? = null,
        private val getResponse: RoomLayoutResponseDto? = null,
        private val saveResponse: RoomLayoutResponseDto? = null,
        private val validateException: Exception? = null,
        private val saveException: Exception? = null
    ) : RoomLayoutApiClient {
        var validateCallCount = 0
        var getCallCount = 0
        var saveCallCount = 0

        override suspend fun validateRoomLayout(
            request: ValidateRoomLayoutRequestDto
        ): RoomLayoutValidationResponseDto {
            validateCallCount += 1
            validateException?.let { throw it }
            return requireNotNull(validateResponse)
        }

        override suspend fun getRoomLayout(): RoomLayoutResponseDto {
            getCallCount += 1
            return requireNotNull(getResponse)
        }

        override suspend fun saveRoomLayout(
            request: SaveRoomLayoutRequestDto
        ): RoomLayoutResponseDto {
            saveCallCount += 1
            saveException?.let { throw it }
            return requireNotNull(saveResponse)
        }
    }

    private class FakeRoomCache(
        var snapshot: RoomLayoutSnapshot? = null
    ) : RoomCache {
        override suspend fun saveDroppedFood(u: Float?, v: Float?) = Unit

        override suspend fun saveDroppedToy(u: Float?, v: Float?, isKnockedOver: Boolean) = Unit

        override suspend fun saveToyKnockedOver(isKnockedOver: Boolean) = Unit

        override suspend fun getDroppedFood(): Pair<Float, Float>? = null

        override suspend fun getDroppedToy(): Triple<Float, Float, Boolean>? = null

        override suspend fun saveRoomLayoutSnapshot(
            layoutRevision: Int,
            layoutHash: String,
            updatedAt: String,
            layoutJson: String
        ) {
            snapshot = RoomLayoutSnapshot(
                layoutRevision = layoutRevision,
                layoutHash = layoutHash,
                updatedAt = updatedAt,
                layoutJson = layoutJson
            )
        }

        override suspend fun getRoomLayoutSnapshot(): RoomLayoutSnapshot? = snapshot
    }
}
