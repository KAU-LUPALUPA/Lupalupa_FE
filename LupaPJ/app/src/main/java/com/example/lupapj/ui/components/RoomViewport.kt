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

    RoomSceneRenderer(
        sceneDefinition = uiState.sceneDefinition,
        houseSceneState = uiState.houseSceneState,
        feedMode = uiState.feedMode || uiState.toyMode,

        onFloorTap = onFloorTap,

        onSceneObjectClick = { sceneObject ->

            if (sceneObject.clickable || uiState.rearrangeMode) {
                onRoomObjectClick(sceneObject.type)
            }
        },

        modifier = modifier
    )
}