package com.example.lupapj.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomPoint
import com.example.lupapj.data.model.RoomUiState

@Composable
fun RoomViewport(
    uiState: RoomUiState,
    onRoomObjectClick: (RoomObjectType) -> Unit,
    onFloorTap: (RoomPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val leftWall = Path().apply {
                    moveTo(width * 0.14f, height * 0.18f)
                    lineTo(width * 0.50f, height * 0.06f)
                    lineTo(width * 0.50f, height * 0.46f)
                    lineTo(width * 0.14f, height * 0.62f)
                    close()
                }
                val rightWall = Path().apply {
                    moveTo(width * 0.50f, height * 0.06f)
                    lineTo(width * 0.86f, height * 0.18f)
                    lineTo(width * 0.86f, height * 0.62f)
                    lineTo(width * 0.50f, height * 0.46f)
                    close()
                }
                val floor = Path().apply {
                    moveTo(width * 0.14f, height * 0.62f)
                    lineTo(width * 0.50f, height * 0.46f)
                    lineTo(width * 0.86f, height * 0.62f)
                    lineTo(width * 0.50f, height * 0.92f)
                    close()
                }

                drawPath(leftWall, color = Color(0xFFF1E5D0))
                drawPath(rightWall, color = Color(0xFFE5EAF4))
                drawPath(floor, color = Color(0xFFD6C0A2))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.feedMode) {
                        if (uiState.feedMode) {
                            detectTapGestures { offset ->
                                if (offset.y > size.height * 0.34f) {
                                    onFloorTap(
                                        RoomPoint(
                                            xFraction = offset.x / size.width,
                                            yFraction = offset.y / size.height
                                        )
                                    )
                                }
                            }
                        }
                    }
            )

            uiState.roomObjects.forEach { roomObject ->
                FurnitureView(
                    objectType = roomObject.type,
                    label = roomObject.label,
                    point = roomObject.position,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    onClick = {
                        if (roomObject.type != RoomObjectType.WINDOW) {
                            onRoomObjectClick(roomObject.type)
                        }
                    }
                )
            }

            uiState.foodPosition?.let { point ->
                PositionedBox(
                    point = point,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    width = 18.dp,
                    height = 18.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF8E5B32))
                    )
                }
            }

            CharacterView(
                point = uiState.pet.position,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight
            )
        }
    }
}

@Composable
fun CharacterView(
    point: RoomPoint,
    viewportWidth: Dp,
    viewportHeight: Dp,
    modifier: Modifier = Modifier
) {
    PositionedBox(
        point = point,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        width = 74.dp,
        height = 92.dp,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "루파",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FurnitureView(
    objectType: RoomObjectType,
    label: String,
    point: RoomPoint,
    viewportWidth: Dp,
    viewportHeight: Dp,
    onClick: () -> Unit
) {
    val width = when (objectType) {
        RoomObjectType.BED -> 112.dp
        RoomObjectType.TOY_BOX -> 86.dp
        RoomObjectType.FOOD_BAG -> 58.dp
        RoomObjectType.WINDOW -> 86.dp
    }
    val height = when (objectType) {
        RoomObjectType.BED -> 54.dp
        RoomObjectType.TOY_BOX -> 72.dp
        RoomObjectType.FOOD_BAG -> 76.dp
        RoomObjectType.WINDOW -> 92.dp
    }

    PositionedBox(
        point = point,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        width = width,
        height = height
    ) {
        when (objectType) {
            RoomObjectType.WINDOW -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFEFF6FD),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFA7D6F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            else -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onClick),
                    shape = RoundedCornerShape(20.dp),
                    color = when (objectType) {
                        RoomObjectType.BED -> Color(0xFFF6D2C1)
                        RoomObjectType.TOY_BOX -> Color(0xFFD6C8F0)
                        RoomObjectType.FOOD_BAG -> Color(0xFFE7C18B)
                        RoomObjectType.WINDOW -> Color.Transparent
                    },
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionedBox(
    point: RoomPoint,
    viewportWidth: Dp,
    viewportHeight: Dp,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .offset(
                x = viewportWidth * point.xFraction,
                y = viewportHeight * point.yFraction
            )
            .size(width = width, height = height),
        content = content
    )
}
