package com.example.lupapj.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource // [추가됨(권)] painterResource 임포트
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import com.example.lupapj.data.model.PetAppearance
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Direction names are used by the new 8-way guinea-pig alien sprite set.
// Row0~Row3 remain as compatibility aliases for older saved/server animation strings.
enum class CharacterAnimation {
    East,
    West,
    North,
    South,
    NorthEast,
    NorthWest,
    SouthEast,
    SouthWest,
    Eating,
    Sleeping,
    Playing,
    Grooming,
    Row0,
    Row1,
    Row2,
    Row3
}

private data class CharacterFrameSpec(
    val resId: Int,
    val bottomInsetRatio: Float
)

private const val LUPA_BOTTOM_INSET_RATIO = 26f / 256f
private const val LUPA_EATING_SPRITE_SCALE = 1.16f
private const val LUPA_SLEEPING_SPRITE_SCALE = 1.4f
private const val LUPA_SLEEPING_BED_LIFT_RATIO = 40f / 256f

@Composable
fun AnimatedCharacterSprite(
    modifier: Modifier = Modifier,
    animation: CharacterAnimation = CharacterAnimation.South,
    appearance: PetAppearance = PetAppearance(),
    equippedItemIds: List<String> = emptyList(),
    allShopItems: List<com.example.lupapj.data.model.ShopItem> = com.example.lupapj.data.model.DefaultShopItems, // [수정됨(권)] 데이터 주입을 위한 파라미터 추가
    isEgg: Boolean = false,
    frameDurationMillis: Long = 150L,
    isPlaying: Boolean = true,
    idleBounceEnabled: Boolean = true,
    contentDescription: String? = "character"
) {
    if (isEgg) {
        EggCharacterPlaceholder(
            modifier = modifier,
            contentDescription = contentDescription
        )
        return
    }

    val resources = LocalContext.current.resources
    val frames = remember(animation) { framesFor(animation) }
    var frameIndex by remember(animation) { mutableIntStateOf(0) }
    val safeFrameDuration = frameDurationMillis.coerceAtLeast(16L)
    val idleTransition = rememberInfiniteTransition(label = "LupaIdleBounce")
    val idleBounceYPx by idleTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (!isPlaying && idleBounceEnabled) -4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LupaIdleBounceY"
    )
    val idleScaleY by idleTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!isPlaying && idleBounceEnabled) 1.035f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LupaIdleScaleY"
    )

    LaunchedEffect(animation, safeFrameDuration, isPlaying, frames.size) {
        frameIndex = 0

        if (!isPlaying) {
            return@LaunchedEffect
        }

        while (isActive) {
            delay(safeFrameDuration)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val currentFrame = frames[frameIndex]
    val bitmap = remember(resources, currentFrame.resId) {
        ImageBitmap.imageResource(
            res = resources,
            id = currentFrame.resId
        )
    }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier, // [수정됨(권)] 모자가 컴포넌트 경계를 넘어가도 잘리지 않도록 clipToBounds() 제거
        contentAlignment = Alignment.Center
    ) {
        val boxWidthDp = maxWidth
        val boxHeightDp = maxHeight
        val petAspectRatio = 1f
        val boxAspectRatio = if (boxHeightDp.value > 0f) boxWidthDp.value / boxHeightDp.value else 1f
        
        val spriteWidthDp: androidx.compose.ui.unit.Dp
        val spriteHeightDp: androidx.compose.ui.unit.Dp
        
        if (boxAspectRatio > petAspectRatio) {
            spriteHeightDp = boxHeightDp
            spriteWidthDp = boxHeightDp * petAspectRatio
        } else {
            spriteWidthDp = boxWidthDp
            spriteHeightDp = boxWidthDp / petAspectRatio
        }

        val drawnHeightPx = with(density) { spriteHeightDp.toPx() }
        val translationYPx = drawnHeightPx * currentFrame.bottomInsetRatio
        val animationLiftYPx = if (animation == CharacterAnimation.Sleeping) {
            -drawnHeightPx * LUPA_SLEEPING_BED_LIFT_RATIO
        } else {
            0f
        }
        val appearanceScale = ((appearance.headSizeScale + appearance.bodySizeScale) * 0.5f)
            .coerceIn(0.88f, 1.12f)
        val animationScale = when (animation) {
            CharacterAnimation.Eating -> LUPA_EATING_SPRITE_SCALE
            CharacterAnimation.Sleeping -> LUPA_SLEEPING_SPRITE_SCALE
            else -> 1f
        }
        val spriteScale = appearanceScale * animationScale
        
        val resolvedDescription = when {
            contentDescription == null -> null
            equippedItemIds.isEmpty() -> contentDescription
            else -> "$contentDescription, 치장 아이템 ${equippedItemIds.size}개 착용"
        }

        val equippedItems = remember(equippedItemIds, allShopItems) {
            equippedItemIds.mapNotNull { itemId ->
                allShopItems.find { it.id == itemId }
            }.filter { it.previewOverlayResId != null }
             .sortedBy { item ->
                 when (item.category) {
                     com.example.lupapj.data.model.ShopCategory.SHOES -> 1
                     com.example.lupapj.data.model.ShopCategory.BOTTOM -> 2
                     com.example.lupapj.data.model.ShopCategory.TOP -> 3
                     com.example.lupapj.data.model.ShopCategory.FULL_BODY -> 3
                     com.example.lupapj.data.model.ShopCategory.FACE_DECOR -> 4
                     com.example.lupapj.data.model.ShopCategory.HAT -> 5
                 }
             }
        }

        // [수정됨(권)] 하나의 단일 이너 Box 안에 펫 이미지와 장착 아이템들을 감싸고,
        // 이 Box 전체에 graphicsLayer를 적용하여 펫 캐릭터와 장착 아이템들이 축을 통일하고 완벽한 수학적 동기화로 움직이게 구현.
        Box(
            modifier = Modifier
                .size(spriteWidthDp, spriteHeightDp)
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) // [추가됨(권)] 기준점을 바닥 중앙으로 고정하여 캐릭터와 아이템 축 이탈 방지
                    translationY = translationYPx + animationLiftYPx + idleBounceYPx
                    scaleX = spriteScale
                    scaleY = spriteScale * idleScaleY
                }
        ) {
            // 1. 베이스 캐릭터 펫 스프라이트 그리기 (이너 Box에 맞춤)
            Image(
                bitmap = bitmap,
                contentDescription = resolvedDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None
            )

            // 2. 장착 치장 아이템들을 레이어 순서대로 그리기
            equippedItems.forEach { item ->
                val itemResId = item.previewOverlayResId
                if (itemResId != null) {
                    if (item.overlayAspectRatio != null) {
                        // [수정됨(권)] 하드코딩 제거: 아이템 모델의 데이터(렌더링 파라미터)를 기반으로 동적 스케일 및 기본 좌표 할당
                        val itemWidth = spriteWidthDp * item.overlayScale
                        val itemHeight = itemWidth * item.overlayAspectRatio
                        
                        // [추가됨(권)] 픽셀 프레임 애니메이션에 맞춘 좌우/상하 흔들림(Sway) 미세 보정
                        val swayOffsetX = when (animation) {
                            CharacterAnimation.East, CharacterAnimation.Row0 -> when (frameIndex) {
                                1 -> -1.5f // 왼쪽으로 갸우뚱
                                4 -> 1.5f  // 오른쪽으로 갸우뚱
                                else -> 0f
                            }
                            else -> 0f
                        }
                        
                        val itemLeft = (spriteWidthDp - itemWidth) / 2 + swayOffsetX.dp
                        val itemTop = spriteHeightDp * item.overlayOffsetYRatio - itemHeight

                        Image(
                            painter = painterResource(id = itemResId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(itemWidth, itemHeight)
                                .absoluteOffset(x = itemLeft, y = itemTop),
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        // 프리셋 좌표계 기반의 다른 부위 아이템들은 베이스 이미지에 1:1 매칭
                        Image(
                            painter = painterResource(id = itemResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EggCharacterPlaceholder(
    modifier: Modifier,
    contentDescription: String?
) {
    val semanticModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics {
            this.contentDescription = contentDescription
        }
    }

    Canvas(modifier = semanticModifier) {
        val eggWidth = size.width * 0.58f
        val eggHeight = size.height * 0.76f
        val topLeft = Offset(
            x = (size.width - eggWidth) * 0.5f,
            y = (size.height - eggHeight) * 0.56f
        )

        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF7DB),
                    Color(0xFFFFD98A),
                    Color(0xFFE9A94E)
                )
            ),
            topLeft = topLeft,
            size = Size(eggWidth, eggHeight)
        )
        drawOval(
            color = Color(0x883E2412),
            topLeft = topLeft,
            size = Size(eggWidth, eggHeight),
            style = Stroke(width = size.minDimension * 0.026f)
        )
    }
}

private fun framesFor(animation: CharacterAnimation): List<CharacterFrameSpec> {
    return when (animation) {
        CharacterAnimation.East, CharacterAnimation.Row0 -> walkFrames(
            R.drawable.lupa_walk_east_00,
            R.drawable.lupa_walk_east_01,
            R.drawable.lupa_walk_east_02,
            R.drawable.lupa_walk_east_03,
            R.drawable.lupa_walk_east_04,
            R.drawable.lupa_walk_east_05
        )

        CharacterAnimation.West, CharacterAnimation.Row1 -> walkFrames(
            R.drawable.lupa_walk_west_00,
            R.drawable.lupa_walk_west_01,
            R.drawable.lupa_walk_west_02,
            R.drawable.lupa_walk_west_03,
            R.drawable.lupa_walk_west_04,
            R.drawable.lupa_walk_west_05
        )

        CharacterAnimation.North, CharacterAnimation.Row2 -> walkFrames(
            R.drawable.lupa_walk_north_00,
            R.drawable.lupa_walk_north_01,
            R.drawable.lupa_walk_north_02,
            R.drawable.lupa_walk_north_03,
            R.drawable.lupa_walk_north_04,
            R.drawable.lupa_walk_north_05
        )

        CharacterAnimation.South, CharacterAnimation.Row3 -> walkFrames(
            R.drawable.lupa_walk_south_00,
            R.drawable.lupa_walk_south_01,
            R.drawable.lupa_walk_south_02,
            R.drawable.lupa_walk_south_03,
            R.drawable.lupa_walk_south_04,
            R.drawable.lupa_walk_south_05
        )

        CharacterAnimation.NorthEast -> walkFrames(
            R.drawable.lupa_walk_north_east_00,
            R.drawable.lupa_walk_north_east_01,
            R.drawable.lupa_walk_north_east_02,
            R.drawable.lupa_walk_north_east_03,
            R.drawable.lupa_walk_north_east_04,
            R.drawable.lupa_walk_north_east_05
        )

        CharacterAnimation.NorthWest -> walkFrames(
            R.drawable.lupa_walk_north_west_00,
            R.drawable.lupa_walk_north_west_01,
            R.drawable.lupa_walk_north_west_02,
            R.drawable.lupa_walk_north_west_03,
            R.drawable.lupa_walk_north_west_04,
            R.drawable.lupa_walk_north_west_05
        )

        CharacterAnimation.SouthEast -> walkFrames(
            R.drawable.lupa_walk_south_east_00,
            R.drawable.lupa_walk_south_east_01,
            R.drawable.lupa_walk_south_east_02,
            R.drawable.lupa_walk_south_east_03,
            R.drawable.lupa_walk_south_east_04,
            R.drawable.lupa_walk_south_east_05
        )

        CharacterAnimation.SouthWest -> walkFrames(
            R.drawable.lupa_walk_south_west_00,
            R.drawable.lupa_walk_south_west_01,
            R.drawable.lupa_walk_south_west_02,
            R.drawable.lupa_walk_south_west_03,
            R.drawable.lupa_walk_south_west_04,
            R.drawable.lupa_walk_south_west_05
        )

        CharacterAnimation.Eating -> walkFrames(
            R.drawable.lupa_eat_01,
            R.drawable.lupa_eat_02,
            R.drawable.lupa_eat_03,
            R.drawable.lupa_eat_04
        )

        CharacterAnimation.Sleeping -> walkFrames(
            R.drawable.lupa_sleep_01,
            R.drawable.lupa_sleep_02,
            R.drawable.lupa_sleep_03,
            R.drawable.lupa_sleep_04
        )

        CharacterAnimation.Playing -> walkFrames(
            R.drawable.lupa_play_00,
            R.drawable.lupa_play_01,
            R.drawable.lupa_play_02,
            R.drawable.lupa_play_03,
            R.drawable.lupa_play_04,
            R.drawable.lupa_play_05
        )

        CharacterAnimation.Grooming -> walkFrames(
            R.drawable.lupa_groom_00,
            R.drawable.lupa_groom_01,
            R.drawable.lupa_groom_02,
            R.drawable.lupa_groom_03,
            R.drawable.lupa_groom_04,
            R.drawable.lupa_groom_05
        )
    }
}

private fun walkFrames(vararg resIds: Int): List<CharacterFrameSpec> {
    return resIds.map { resId ->
        CharacterFrameSpec(resId = resId, bottomInsetRatio = LUPA_BOTTOM_INSET_RATIO)
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimatedCharacterSpritePreview() {
    AnimatedCharacterSprite(
        modifier = Modifier.size(120.dp),
        animation = CharacterAnimation.South,
        frameDurationMillis = 150L
    )
}
