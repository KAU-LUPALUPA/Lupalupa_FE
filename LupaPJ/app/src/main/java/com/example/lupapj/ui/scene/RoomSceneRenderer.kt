package com.example.lupapj.ui.scene

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.scene.DefaultFloorPivot
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.IsoRoomProjectionSpec
import com.example.lupapj.data.model.scene.PET_AUTONOMOUS_MOVE_DURATION_MS
import com.example.lupapj.data.model.scene.PetMovementStyle
import com.example.lupapj.data.model.scene.PetSceneState
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.ScenePivot
import com.example.lupapj.data.model.scene.SceneSpriteSpec
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.WallFace
import com.example.lupapj.data.model.scene.defaultPivotFor
import com.example.lupapj.data.model.scene.toFloorAnchor
import com.example.lupapj.ui.components.AnimatedCharacterSprite
import com.example.lupapj.ui.components.BedImage
import com.example.lupapj.ui.components.CharacterAnimation
import com.example.lupapj.ui.components.FeedBagImage
import com.example.lupapj.ui.components.FeedPelletImage
import com.example.lupapj.ui.components.ToyBoxImage
import com.example.lupapj.ui.components.ToyDuckImage
import kotlin.math.abs
import kotlin.math.roundToInt

private val PET_MOVE_DURATION_MS = PET_AUTONOMOUS_MOVE_DURATION_MS.toInt()
private const val PET_DIRECTION_EPSILON = 0.01f
private const val PET_AXIS_DOMINANCE_RATIO = 1.75f
private const val PET_RENDER_DEPTH_BIAS = 0.18f

private val PetSprite = SceneSpriteSpec(
    assetKey = "room/characters/lupa_default",
    fallbackLabel = "루파",
    widthRatio = 0.16f,
    heightRatio = 1.0f,
    minWidthDp = 64f,
    maxWidthDp = 104f,
    isoTileFillRatio = 1.05f,
    pivot = DefaultFloorPivot
)

private val FoodSprite = SceneSpriteSpec(
    assetKey = "room/objects/food_drop",
    fallbackLabel = "사료",
    widthRatio = 0.055f,
    heightRatio = 1.0f,
    minWidthDp = 18f,
    maxWidthDp = 24f,
    pivot = DefaultFloorPivot
)

private val ToySprite = SceneSpriteSpec(
    assetKey = "room/objects/toy_drop",
    fallbackLabel = "장난감",
    widthRatio = 0.075f,
    heightRatio = 1.0f,
    minWidthDp = 24f,
    maxWidthDp = 36f,
    pivot = DefaultFloorPivot
)

private data class ContactShadowStyle(
    val widthMultiplier: Float,
    val heightMultiplier: Float,
    val baseAlpha: Float,
    val yLiftPx: Float = 0f
)

@Composable
fun RoomSceneRenderer(
    sceneDefinition: RoomSceneDefinition,
    houseSceneState: HouseSceneState,
    feedMode: Boolean,
    companionPets: List<PetSceneState> = emptyList(),
    onFloorTap: (FloorAnchor) -> Unit,
    onSceneObjectClick: (SceneObjectDefinition) -> Unit,
    onDroppedToyClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    uiOverlay: @Composable BoxScope.() -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }

        val animatedPets = mutableListOf<AnimatedPetRenderState>()
        (listOf(houseSceneState.pet) + companionPets).forEachIndexed { index, pet ->
            key("${pet.petId}-$index") {
                val petMovementDuration = (
                    PET_MOVE_DURATION_MS / pet.movement.speedMultiplier.coerceAtLeast(0.1f)
                    ).toInt()
                val animatedPetU by animateFloatAsState(
                    targetValue = pet.anchor.u,
                    animationSpec = tween(
                        durationMillis = petMovementDuration,
                        easing = FastOutSlowInEasing
                    ),
                    label = "PetAnchorU-${pet.petId}-$index"
                )
                val animatedPetV by animateFloatAsState(
                    targetValue = pet.anchor.v,
                    animationSpec = tween(
                        durationMillis = petMovementDuration,
                        easing = FastOutSlowInEasing
                    ),
                    label = "PetAnchorV-${pet.petId}-$index"
                )
                val animatedPetAnchor = FloorAnchor(
                    u = animatedPetU,
                    v = animatedPetV
                )
                val isPetMoving =
                    abs(animatedPetU - pet.anchor.u) > 0.0005f ||
                        abs(animatedPetV - pet.anchor.v) > 0.0005f
                var lockedPetAnimation by remember(pet.petId, index) {
                    mutableStateOf(CharacterAnimation.South)
                }
                LaunchedEffect(pet.anchor) {
                    lockedPetAnimation = resolveCharacterAnimationForMovement(
                        projectionSpec = sceneDefinition.projectionSpec,
                        currentAnchor = animatedPetAnchor,
                        targetAnchor = pet.anchor,
                        fallbackAnimation = lockedPetAnimation
                    )
                }
                animatedPets += AnimatedPetRenderState(
                    key = "pet-${pet.petId}-$index",
                    pet = pet,
                    anchor = animatedPetAnchor,
                    isMoving = isPetMoving,
                    animation = lockedPetAnimation,
                    movementDurationMillis = petMovementDuration
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            RoomBackground(
                projectionSpec = sceneDefinition.projectionSpec,
                spec = RoomBackgroundSpec(
                    roomColumns = 3,
                    wallMiddleRows = 1,
                    floorRows = 1
                ),
                wallDecor = sceneDefinition.fixedDecor.filter { it.anchor is WallAnchor },
                sideWallFace = sceneDefinition.sideWallFace ?: WallFace.RIGHT,
                highlightFloor = feedMode
            )

            FloorInteractionLayer(
                enabled = feedMode,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                projectionSpec = sceneDefinition.projectionSpec,
                onFloorTap = onFloorTap
            )

            // Future furniture and character layers continue to stack above the tiled background.
            sceneDefinition.fixedDecor
                .filterNot { it.anchor is WallAnchor }
                .forEach { decor ->
                key(decor.id) {
                    val node = projectSceneObject(
                        sceneObject = decor,
                        projectionSpec = sceneDefinition.projectionSpec,
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx,
                        density = density
                    )
                    ProjectedNodeBox(
                        node = node,
                        density = density
                    ) {
                        SceneObjectPlaceholder(
                            objectType = decor.type,
                            label = decor.sprite.fallbackLabel,
                            clickable = false
                        )
                    }
                }
            }

            val floorRenderables = buildFloorRenderables(
                sceneObjects = sceneDefinition.objects,
                projectionSpec = sceneDefinition.projectionSpec,
                houseSceneState = houseSceneState,
                pets = animatedPets,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                density = density
            )

            sortFloorRenderables(floorRenderables).forEach { entry ->
                key(entry.key) {
                    val renderable = entry.value
                    shadowStyleFor(renderable)?.let { style ->
                        ContactShadow(
                            node = renderable.node,
                            style = style,
                            density = density
                        )
                    }

                    ProjectedNodeBox(
                        node = renderable.node,
                        density = density
                    ) {
                        when (renderable) {
                            is FloorRenderableModel.SceneObjectRenderable -> {
                                SceneObjectPlaceholder(
                                    objectType = renderable.sceneObject.type,
                                    label = renderable.sceneObject.sprite.fallbackLabel,
                                    clickable = renderable.sceneObject.clickable,
                                    onClick = {
                                        if (renderable.sceneObject.clickable) {
                                            onSceneObjectClick(renderable.sceneObject)
                                        }
                                    }
                                )
                            }

                            is FloorRenderableModel.PetRenderable -> {
                                val spriteAnimation = when {
                                    renderable.pet.action == PetAction.EATING &&
                                        !renderable.isMoving -> CharacterAnimation.Eating
                                    renderable.pet.action == PetAction.BED_RESTING -> CharacterAnimation.Sleeping
                                    else -> renderable.animation
                                }
                                val isSpritePlaying = renderable.isMoving ||
                                    renderable.pet.action == PetAction.EATING ||
                                    renderable.pet.action == PetAction.BED_RESTING
                                val applyMovementStyle = renderable.isMoving &&
                                    renderable.pet.action != PetAction.BED_RESTING
                                val frameDurationMillis = when (spriteAnimation) {
                                    CharacterAnimation.Eating -> 180L
                                    CharacterAnimation.Sleeping -> 520L
                                    else -> 150L
                                }

                                AnimatedCharacterSprite(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .petMovementStyle(
                                            style = renderable.pet.movement.style,
                                            isMoving = applyMovementStyle,
                                            bouncePx = renderable.pet.movement.bouncePx,
                                            durationMillis = renderable.movementDurationMillis
                                        )
                                        // [수정됨(권)] 옆으로 눕기 플래그가 활성화된 경우에만 회전 애니메이션 적용
                                        .petRestingStyle(
                                            isResting = renderable.pet.isLyingSide &&
                                                renderable.pet.action != PetAction.BED_RESTING &&
                                                spriteAnimation != CharacterAnimation.Sleeping
                                        ),
                                    animation = spriteAnimation,
                                    appearance = renderable.pet.appearance,
                                    equippedItemIds = renderable.pet.equippedItemIds,
                                    isEgg = renderable.pet.status.isEgg,
                                    frameDurationMillis = frameDurationMillis,
                                    isPlaying = isSpritePlaying,
                                    idleBounceEnabled = renderable.pet.action == PetAction.IDLE,
                                    contentDescription = renderable.pet.name.ifBlank {
                                        PetSprite.fallbackLabel
                                    }
                                )
                            }

                            is FloorRenderableModel.FoodRenderable -> {
                                FoodPlaceholder()
                            }

                            is FloorRenderableModel.ToyRenderable -> {
                                val isPlaying = houseSceneState.pet.action == PetAction.PLAYING
                                // ToyRenderable은 항상 droppedToyAnchor를 기반으로 생성되므로 isTargetToy는 참으로 간주
                                val isKnocked = houseSceneState.currentSceneRuntime.isToyKnockedOver

                                val rotation by animateFloatAsState(
                                    targetValue = if (isKnocked) 90f else 0f,
                                    animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
                                    label = "ToyRotation"
                                )

                                val shakeTransition = rememberInfiniteTransition(label = "ToyShake")
                                val shakeValue by shakeTransition.animateFloat(
                                    initialValue = -1.5f,
                                    targetValue = 1.5f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "Shake"
                                )

                                val finalRotation = rotation + if (isPlaying || isKnocked) shakeValue else 0f

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationZ = finalRotation
                                        }
                                        .clickable(
                                            enabled = isKnocked && !feedMode,
                                            onClick = onDroppedToyClick
                                        )
                                ) {
                                    ToyPlaceholder()
                                }
                            }
                        }
                    }
                }
            }

            sceneDefinition.frontOccluders.forEach { occluder ->
                key(occluder.id) {
                    val node = projectSceneObject(
                        sceneObject = occluder,
                        projectionSpec = sceneDefinition.projectionSpec,
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx,
                        density = density
                    )
                    ProjectedNodeBox(
                        node = node,
                        density = density
                    ) {
                        FrontOccluderPlaceholder()
                    }
                }
            }

            uiOverlay()
        }
    }
}

private data class AnimatedPetRenderState(
    val key: String,
    val pet: PetSceneState,
    val anchor: FloorAnchor,
    val isMoving: Boolean,
    val animation: CharacterAnimation,
    val movementDurationMillis: Int
)

private sealed interface FloorRenderableModel {
    val key: String
    val node: ProjectedNode

    data class SceneObjectRenderable(
        val sceneObject: SceneObjectDefinition,
        override val node: ProjectedNode
    ) : FloorRenderableModel {
        override val key: String = sceneObject.id
    }

    data class PetRenderable(
        val pet: PetSceneState,
        val isMoving: Boolean,
        val animation: CharacterAnimation,
        val movementDurationMillis: Int,
        override val node: ProjectedNode
    ) : FloorRenderableModel {
        override val key: String = "pet-${pet.petId}"
    }

    data class FoodRenderable(
        override val node: ProjectedNode
    ) : FloorRenderableModel {
        override val key: String = "dropped_food"
    }

    data class ToyRenderable(
        override val node: ProjectedNode
    ) : FloorRenderableModel {
        override val key: String = "dropped_toy"
    }
}

private fun buildFloorRenderables(
    sceneObjects: List<SceneObjectDefinition>,
    projectionSpec: IsoRoomProjectionSpec,
    houseSceneState: HouseSceneState,
    pets: List<AnimatedPetRenderState>,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    density: Density
): List<DepthSortable<FloorRenderableModel>> {
    val renderables = mutableListOf<DepthSortable<FloorRenderableModel>>()
    val metrics = resolveIsoRoomMetrics(
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        projectionSpec = projectionSpec
    )

    sceneObjects.forEach { sceneObject ->
        val node = projectSceneObject(
            sceneObject = sceneObject,
            projectionSpec = projectionSpec,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            density = density
        )
        renderables += DepthSortable(
            key = sceneObject.id,
            sortDepth = node.sortDepth,
            value = FloorRenderableModel.SceneObjectRenderable(
                sceneObject = sceneObject,
                node = node
            )
        )
    }

    houseSceneState.currentSceneRuntime.droppedFoodAnchor?.let { droppedFood ->
        val node = projectFloorAnchor(
            anchor = droppedFood,
            projectionSpec = projectionSpec,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            spriteSizePx = resolveSpriteSizePx(
                viewportWidthPx = viewportWidthPx,
                sprite = FoodSprite,
                minWidthPx = with(density) { FoodSprite.minWidthDp.dp.toPx() },
                maxWidthPx = with(density) { FoodSprite.maxWidthDp.dp.toPx() }
            ),
            pivot = FoodSprite.pivot ?: DefaultFloorPivot,
            depthBias = -0.01f
        )
        renderables += DepthSortable(
            key = "dropped_food",
            sortDepth = node.sortDepth,
            value = FloorRenderableModel.FoodRenderable(node = node)
        )
    }

    houseSceneState.currentSceneRuntime.droppedToyAnchor?.let { droppedToy ->
        val node = projectFloorAnchor(
            anchor = droppedToy,
            projectionSpec = projectionSpec,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            spriteSizePx = resolveSpriteSizePx(
                viewportWidthPx = viewportWidthPx,
                sprite = ToySprite,
                minWidthPx = with(density) { ToySprite.minWidthDp.dp.toPx() },
                maxWidthPx = with(density) { ToySprite.maxWidthDp.dp.toPx() }
            ),
            pivot = ToySprite.pivot ?: DefaultFloorPivot,
            depthBias = -0.012f
        )
        renderables += DepthSortable(
            key = "dropped_toy",
            sortDepth = node.sortDepth,
            value = FloorRenderableModel.ToyRenderable(node = node)
        )
    }

    pets.forEach { petState ->
        val petNode = projectFloorAnchor(
            anchor = petState.anchor,
            projectionSpec = projectionSpec,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            spriteSizePx = resolvePetSpriteSizePx(
                sprite = PetSprite,
                minWidthPx = with(density) { PetSprite.minWidthDp.dp.toPx() },
                maxWidthPx = with(density) { PetSprite.maxWidthDp.dp.toPx() },
                metrics = metrics
            ),
            pivot = PetSprite.pivot ?: DefaultFloorPivot,
            depthBias = PET_RENDER_DEPTH_BIAS
        )
        renderables += DepthSortable(
            key = petState.key,
            sortDepth = petNode.sortDepth,
            value = FloorRenderableModel.PetRenderable(
                pet = petState.pet,
                isMoving = petState.isMoving,
                animation = petState.animation,
                movementDurationMillis = petState.movementDurationMillis,
                node = petNode
            )
        )
    }

    return renderables
}

private fun projectSceneObject(
    sceneObject: SceneObjectDefinition,
    projectionSpec: IsoRoomProjectionSpec,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    density: Density
): ProjectedNode {
    val sprite = sceneObject.sprite
    val pivot = sprite.pivot ?: defaultPivotFor(sceneObject.anchor)

    return when (val anchor = sceneObject.anchor) {
        is FloorAnchor -> {
            val metrics = resolveIsoRoomMetrics(
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                projectionSpec = projectionSpec
            )
            val resolvedAnchor = sceneObject.tilePlacement?.toFloorAnchor(projectionSpec) ?: anchor
            val spriteSizePx = resolveFloorSpriteSizePx(
                viewportWidthPx = viewportWidthPx,
                sprite = sprite,
                minWidthPx = with(density) { sprite.minWidthDp.dp.toPx() },
                maxWidthPx = with(density) { sprite.maxWidthDp.dp.toPx() },
                metrics = metrics,
                tilePlacement = sceneObject.tilePlacement
            )
            projectFloorAnchor(
                anchor = resolvedAnchor,
                projectionSpec = projectionSpec,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                spriteSizePx = spriteSizePx,
                pivot = pivot,
                depthBias = sceneObject.depthBias
            )
        }

        is WallAnchor -> {
            val spriteSizePx = resolveSpriteSizePx(
                viewportWidthPx = viewportWidthPx,
                sprite = sprite,
                minWidthPx = with(density) { sprite.minWidthDp.dp.toPx() },
                maxWidthPx = with(density) { sprite.maxWidthDp.dp.toPx() }
            )
            projectWallAnchor(
                projectionSpec = projectionSpec,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                spriteSizePx = spriteSizePx,
                pivot = pivot,
                anchor = anchor,
                depthBias = sceneObject.depthBias
            )
        }
    }
}

private fun Modifier.petMovementStyle(
    style: PetMovementStyle,
    isMoving: Boolean,
    bouncePx: Float = 0f,
    durationMillis: Int = 1000
): Modifier = composed {
    if (!isMoving || bouncePx <= 0f) return@composed this

    when (style) {
        PetMovementStyle.SMOOTH -> this
        PetMovementStyle.BOUNCY -> {
            val infiniteTransition = rememberInfiniteTransition(label = "BounceTransition")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -bouncePx,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BounceOffset"
            )
            this.graphicsLayer { translationY = yOffset }
        }
    }
}

private fun Modifier.petRestingStyle(
    isResting: Boolean
): Modifier = composed {
    if (!isResting) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "RestingBreathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    this.graphicsLayer {
        rotationZ = 90f // 옆으로 누운 자세
        scaleX = scale
        scaleY = scale * 0.85f // 약간 납작해진 느낌
        translationX = 15f // 회전으로 인한 중심축 이탈 보정
    }
}

@Composable
private fun BoxScope.FloorInteractionLayer(
    enabled: Boolean,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    projectionSpec: IsoRoomProjectionSpec,
    onFloorTap: (FloorAnchor) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled, projectionSpec) {
                if (enabled) {
                    detectTapGestures { offset ->
                        resolveFloorAnchorFromViewport(
                            tapXPx = offset.x,
                            tapYPx = offset.y,
                            viewportWidthPx = viewportWidthPx,
                            viewportHeightPx = viewportHeightPx,
                            projectionSpec = projectionSpec
                        )?.let(onFloorTap)
                    }
                }
            }
    )
}

@Composable
private fun BoxScope.ContactShadow(
    node: ProjectedNode,
    style: ContactShadowStyle,
    density: Density
) {
    val shadowWidthPx = node.widthPx * style.widthMultiplier
    val shadowHeightPx = node.heightPx * style.heightMultiplier
    val alpha = (style.baseAlpha * node.perspectiveScale.coerceIn(0.82f, 1.18f)).coerceIn(0.08f, 0.32f)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (node.footXpx - shadowWidthPx / 2f).roundToInt(),
                    y = (node.footYpx - shadowHeightPx * 0.40f + style.yLiftPx).roundToInt()
                )
            }
            .size(
                width = with(density) { shadowWidthPx.toDp() },
                height = with(density) { shadowHeightPx.toDp() }
            )
            .drawBehind {
                drawOval(
                    color = Color.Black.copy(alpha = alpha),
                    size = size
                )
            }
    )
}

@Composable
private fun ProjectedNodeBox(
    node: ProjectedNode,
    density: Density,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = node.xPx.roundToInt(),
                    y = node.yPx.roundToInt()
                )
            }
            .size(
                width = with(density) { node.widthPx.toDp() },
                height = with(density) { node.heightPx.toDp() }
            ),
        content = content
    )
}

@Composable
private fun SceneObjectPlaceholder(
    objectType: RoomObjectType,
    label: String,
    clickable: Boolean,
    onClick: () -> Unit = {}
) {
    when (objectType) {
        RoomObjectType.WINDOW -> {
            Image(
                painter = painterResource(id = R.drawable.window_object),
                contentDescription = label,
                modifier = Modifier.fillMaxSize()
            )
        }

        RoomObjectType.BED -> Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = clickable, onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BedImage(
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = label
                )
            }
        }

        RoomObjectType.TOY_BOX -> Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = clickable, onClick = onClick),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ToyBoxImage(
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = label
                )
            }
        }

        RoomObjectType.FOOD_BAG -> Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = clickable, onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                FeedBagImage(
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = label
                )
            }
        }
    }
}

@Composable
private fun CharacterPlaceholder(label: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FoodPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        FeedPelletImage(
            modifier = Modifier.fillMaxSize(),
            contentDescription = "사료 알갱이"
        )
    }
}

@Composable
private fun ToyPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyDuckImage(
            modifier = Modifier.fillMaxSize(),
            contentDescription = "오리 장난감"
        )
    }
}

@Composable
private fun FrontOccluderPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = Color(0xFFD79C85),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        color = Color(0x24FFFFFF),
                        size = Size(size.width, size.height * 0.22f)
                    )
                    drawRect(
                        color = Color(0x12000000),
                        topLeft = Offset(0f, size.height * 0.82f),
                        size = Size(size.width, size.height * 0.18f)
                    )
                }
        )
    }
}

private fun shadowStyleFor(renderable: FloorRenderableModel): ContactShadowStyle? {
    return when (renderable) {
        is FloorRenderableModel.PetRenderable -> ContactShadowStyle(
            widthMultiplier = 0.48f,
            heightMultiplier = 0.16f,
            baseAlpha = 0.24f
        )

        is FloorRenderableModel.FoodRenderable -> ContactShadowStyle(
            widthMultiplier = 0.42f,
            heightMultiplier = 0.18f,
            baseAlpha = 0.10f
        )

        is FloorRenderableModel.ToyRenderable -> null

        is FloorRenderableModel.SceneObjectRenderable -> when (renderable.sceneObject.type) {
            RoomObjectType.BED -> ContactShadowStyle(
                widthMultiplier = 0.72f,
                heightMultiplier = 0.16f,
                baseAlpha = 0.14f,
                yLiftPx = -11f
            )

            RoomObjectType.TOY_BOX -> ContactShadowStyle(
                widthMultiplier = 0.54f,
                heightMultiplier = 0.13f,
                baseAlpha = 0.12f,
                yLiftPx = -4f
            )

            RoomObjectType.FOOD_BAG -> ContactShadowStyle(
                widthMultiplier = 0.44f,
                heightMultiplier = 0.14f,
                baseAlpha = 0.12f,
                yLiftPx = -9f
            )

            RoomObjectType.WINDOW -> null
        }
    }
}

private fun resolveCharacterAnimationForMovement(
    projectionSpec: IsoRoomProjectionSpec,
    currentAnchor: FloorAnchor,
    targetAnchor: FloorAnchor,
    fallbackAnimation: CharacterAnimation
): CharacterAnimation {
    val currentScreenPoint = projectAnchorToIsoScreen(
        projectionSpec = projectionSpec,
        anchor = currentAnchor
    )
    val targetScreenPoint = projectAnchorToIsoScreen(
        projectionSpec = projectionSpec,
        anchor = targetAnchor
    )
    val deltaX = targetScreenPoint.xPx - currentScreenPoint.xPx
    val deltaY = targetScreenPoint.yPx - currentScreenPoint.yPx
    val absDeltaX = abs(deltaX)
    val absDeltaY = abs(deltaY)
    val dominantDelta = maxOf(abs(deltaX), abs(deltaY))

    if (dominantDelta < PET_DIRECTION_EPSILON) {
        return fallbackAnimation
    }

    return when {
        absDeltaX >= absDeltaY * PET_AXIS_DOMINANCE_RATIO -> {
            if (deltaX >= 0f) CharacterAnimation.East else CharacterAnimation.West
        }

        absDeltaY >= absDeltaX * PET_AXIS_DOMINANCE_RATIO -> {
            if (deltaY < 0f) CharacterAnimation.North else CharacterAnimation.South
        }

        deltaX >= 0f && deltaY < 0f -> CharacterAnimation.NorthEast
        deltaX < 0f && deltaY < 0f -> CharacterAnimation.NorthWest
        deltaX >= 0f && deltaY >= 0f -> CharacterAnimation.SouthEast
        deltaX < 0f && deltaY >= 0f -> CharacterAnimation.SouthWest
        else -> fallbackAnimation
    }
}

private fun projectAnchorToIsoScreen(
    projectionSpec: IsoRoomProjectionSpec,
    anchor: FloorAnchor
): ScreenPointPx {
    val xTiles = anchor.u * projectionSpec.roomWidthTiles
    val yTiles = anchor.v * projectionSpec.roomDepthTiles
    return ScreenPointPx(
        xPx = (xTiles - yTiles) * 0.5f,
        yPx = (xTiles + yTiles) * projectionSpec.tileHeightRatio * 0.5f
    )
}
