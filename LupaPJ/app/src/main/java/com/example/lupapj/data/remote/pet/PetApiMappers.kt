package com.example.lupapj.data.remote.pet

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetTraits
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneId
import com.example.lupapj.data.model.scene.initialHouseSceneState

fun PetDto.toHouseSceneState(
    sceneId: RoomSceneId
): HouseSceneState {
    return initialHouseSceneState(
        sceneId = sceneId,
        petId = petId,
        ownerUserId = ownerUserId,
        petName = name,
        characterAssetKey = characterAssetKey,
        petAppearance = appearance.toDomain(),
        petStatus = status.toDomain(),
        petTraits = traits?.toDomain() ?: PetTraits(), // [수정됨(V2)] fallback 지원
        petAnchor = anchor.toFloorAnchor(),
        petAction = enumValueOrDefault(action, PetAction.IDLE)
    ).let { baseState ->
        baseState.copy(
            pet = baseState.pet.copy(
                interactionEvents = interactions?.toDomain() ?: baseState.pet.interactionEvents,
                equippedItemIds = equippedItemIds
            )
        )
    }
}

fun PetAppearanceDto.toDomain(): PetAppearance {
    return PetAppearance(
        headSizeScale = headSizeScale,
        bodySizeScale = bodySizeScale,
        eyeSizeScale = eyeSizeScale,
        noseSizeScale = noseSizeScale,
        mouthSizeScale = mouthSizeScale
    )
}

fun PetStatusDto.toDomain(): PetStatus {
    return PetStatus(
        satiety = satiety.coerceIn(0, 100),
        vitality = vitality.coerceIn(0, 100),
        cleanliness = cleanliness?.coerceIn(0, 100) ?: 100, // [수정됨(V2)] fallback 지원
        isEgg = isEgg
    )
}

fun PetTraitsDto.toDomain(): PetTraits { // [수정됨(V2)]
    return PetTraits(
        activity = activity?.coerceIn(0f, 1f) ?: 0.5f,
        appetite = appetite?.coerceIn(0f, 1f) ?: 0.5f,
        attention = attention?.coerceIn(0f, 1f) ?: 0.5f,
        curiosity = curiosity?.coerceIn(0f, 1f) ?: 0.5f,
        patience = patience?.coerceIn(0f, 1f) ?: 0.5f
    )
}

fun InteractionEventsDto.toDomain(): com.example.lupapj.data.model.InteractionEvents { // [추가됨(V2)]
    return com.example.lupapj.data.model.InteractionEvents(
        totalFeedCount = feedCount ?: 0,
        totalPlayCount = playCount ?: 0,
        totalCleanCommandCount = cleanCommandCount ?: 0,
        totalSleepCommandCount = sleepCommandCount ?: 0,
        daysActive = daysActive ?: 1
    )
}

fun PetAnchorDto.toFloorAnchor(): FloorAnchor {
    return FloorAnchor(
        u = u.coerceIn(0f, 1f),
        v = v.coerceIn(0f, 1f)
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T
): T {
    return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
}
