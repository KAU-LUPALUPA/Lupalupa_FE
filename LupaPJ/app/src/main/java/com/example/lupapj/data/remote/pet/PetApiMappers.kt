package com.example.lupapj.data.remote.pet

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetPersonality
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneId
import com.example.lupapj.data.model.scene.initialHouseSceneState

internal fun PetDto.toHouseSceneState(
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
        petPersonality = enumValueOrDefault(personality, PetPersonality.ACTIVE),
        equippedItemIds = equippedItemIds,
        petAnchor = anchor.toFloorAnchor(),
        petAction = enumValueOrDefault(action, PetAction.IDLE)
    )
}

internal fun PetAppearanceDto.toDomain(): PetAppearance {
    return PetAppearance(
        headSizeScale = headSizeScale,
        bodySizeScale = bodySizeScale,
        eyeSizeScale = eyeSizeScale,
        noseSizeScale = noseSizeScale,
        mouthSizeScale = mouthSizeScale
    )
}

internal fun PetStatusDto.toDomain(): PetStatus {
    return PetStatus(
        hunger = hunger.coerceIn(0, 100),
        fatigue = fatigue.coerceIn(0, 100),
        isEgg = isEgg
    )
}

internal fun PetAnchorDto.toFloorAnchor(): FloorAnchor {
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
