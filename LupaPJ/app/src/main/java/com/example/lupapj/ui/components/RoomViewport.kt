package com.example.lupapj.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.ui.scene.RoomSceneRenderer

@Composable
fun RoomViewport(
    uiState: RoomUiState,
    onRoomObjectClick: (RoomObjectType) -> Unit,
    onFloorTap: (FloorAnchor) -> Unit,
    modifier: Modifier = Modifier
) {
    // Migration adapter: external callers still depend on RoomViewport while the actual
    // drawing logic now lives in RoomSceneRenderer.
    RoomSceneRenderer(
        sceneDefinition = uiState.sceneDefinition,
        houseSceneState = uiState.houseSceneState,
        feedMode = uiState.feedMode,
        onFloorTap = onFloorTap,
        onSceneObjectClick = { sceneObject ->
            if (sceneObject.clickable) {
                onRoomObjectClick(sceneObject.type)
            }
        },
        modifier = modifier
    )
}
