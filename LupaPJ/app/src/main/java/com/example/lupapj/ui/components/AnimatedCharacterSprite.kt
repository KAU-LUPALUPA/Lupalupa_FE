package com.example.lupapj.ui.components

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
import kotlin.math.min

// 스프라이트 행의 의미는 임시 매핑이다. 추후 캐릭터 방향/상태에 맞게 Idle, WalkDown,
// WalkUp, WalkSide 등으로 이름을 바꿀 수 있다.
enum class CharacterAnimation {
    Row0,
    Row1,
    Row2,
    Row3
}

private data class CharacterFrameSpec(
    val resId: Int,
    val bottomInsetRatio: Float
)

@Composable
fun AnimatedCharacterSprite(
    modifier: Modifier = Modifier,
    animation: CharacterAnimation = CharacterAnimation.Row0,
    appearance: PetAppearance = PetAppearance(),
    equippedItemIds: List<String> = emptyList(),
    allShopItems: List<com.example.lupapj.data.model.ShopItem> = com.example.lupapj.data.model.DefaultShopItems, // [수정됨(권)] 데이터 주입을 위한 파라미터 추가
    isEgg: Boolean = false,
    frameDurationMillis: Long = 150L,
    isPlaying: Boolean = true,
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
        val petAspectRatio = 170f / 258f
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
        val spriteScale = ((appearance.headSizeScale + appearance.bodySizeScale) * 0.5f)
            .coerceIn(0.88f, 1.12f)
        
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
                    translationY = translationYPx
                    scaleX = spriteScale
                    scaleY = spriteScale
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
                        
                        // [추가됨(권)] 픽셀 프레임 애니메이션(Row0)에 맞춘 좌우/상하 흔들림(Sway) 미세 보정
                        val swayOffsetX = when (animation) {
                            CharacterAnimation.Row0 -> when (frameIndex) {
                                1 -> -1.5f // 왼쪽으로 갸우뚱
                                3 -> 1.5f  // 오른쪽으로 갸우뚱
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
        CharacterAnimation.Row0 -> listOf(
            CharacterFrameSpec(R.drawable.sprite_0_0, bottomInsetRatio = 66f / 258f),
            CharacterFrameSpec(R.drawable.sprite_0_1, bottomInsetRatio = 71f / 258f),
            CharacterFrameSpec(R.drawable.sprite_0_2, bottomInsetRatio = 66f / 258f),
            CharacterFrameSpec(R.drawable.sprite_0_3, bottomInsetRatio = 66f / 258f)
        )

        // 왼쪽 이동 자산은 4장을 모두 돌리면 몸이 회전하는 느낌이 강해서,
        // 중심이 가장 비슷한 두 프레임만 사용해 좌향 보행으로 보이게 고정한다.
        CharacterAnimation.Row1 -> listOf(
            CharacterFrameSpec(R.drawable.sprite_1_1, bottomInsetRatio = 47f / 258f),
            CharacterFrameSpec(R.drawable.sprite_1_2, bottomInsetRatio = 49f / 258f)
        )

        // 상/하 방향 자산은 현재 4프레임 전체를 순회하면 회전하는 느낌이 강해서,
        // 일관된 방향으로 보이는 프레임만 골라 우선 2프레임 루프로 사용한다.
        CharacterAnimation.Row2 -> listOf(
            CharacterFrameSpec(R.drawable.sprite_2_2, bottomInsetRatio = 15f / 258f),
            CharacterFrameSpec(R.drawable.sprite_2_3, bottomInsetRatio = 15f / 258f)
        )

        CharacterAnimation.Row3 -> listOf(
            CharacterFrameSpec(R.drawable.sprite_3_1, bottomInsetRatio = 0f),
            CharacterFrameSpec(R.drawable.sprite_3_2, bottomInsetRatio = 0f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimatedCharacterSpritePreview() {
    AnimatedCharacterSprite(
        modifier = Modifier.size(120.dp),
        animation = CharacterAnimation.Row0,
        frameDurationMillis = 150L
    )
}
