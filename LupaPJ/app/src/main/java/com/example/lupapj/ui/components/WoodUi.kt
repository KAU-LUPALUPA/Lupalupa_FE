package com.example.lupapj.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val WoodDark = Color(0xFF4E2F1F)
private val WoodBase = Color(0xFF8A5734)
private val WoodWarm = Color(0xFFA46C40)
private val WoodLight = Color(0xFFC38A55)
private val WoodEdge = Color(0xFF2D1B12)
private val WoodHighlight = Color(0x66F8D49B)
private val WoodGrain = Color(0x4D3B2116)

@Composable
fun WoodPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .woodFrame()
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun WoodIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onPrimary,
    contentAlignment: Alignment = Alignment.Center
) {
    Box(
        modifier = modifier
            .woodFrame()
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = contentAlignment
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
    }
}

@Composable
fun WoodScreenFrame(
    modifier: Modifier = Modifier,
    thickness: Dp = 16.dp
) {
    Box(
        modifier = modifier.drawWithCache {
            val frameWidth = thickness.toPx()
            val outerStroke = 2.dp.toPx()
            val innerStroke = 1.2.dp.toPx()
            val grainStroke = 1.15.dp.toPx()
            val pegRadius = 2.8.dp.toPx()

            val horizontalBrush = Brush.horizontalGradient(
                colors = listOf(WoodDark, WoodWarm, WoodLight, WoodWarm, WoodDark)
            )
            val verticalBrush = Brush.verticalGradient(
                colors = listOf(WoodDark, WoodWarm, WoodLight, WoodWarm, WoodDark)
            )

            onDrawBehind {
                if (frameWidth <= 0f) return@onDrawBehind

                drawRect(
                    brush = horizontalBrush,
                    topLeft = Offset.Zero,
                    size = Size(size.width, frameWidth)
                )
                drawRect(
                    brush = horizontalBrush,
                    topLeft = Offset(0f, size.height - frameWidth),
                    size = Size(size.width, frameWidth)
                )
                drawRect(
                    brush = verticalBrush,
                    topLeft = Offset.Zero,
                    size = Size(frameWidth, size.height)
                )
                drawRect(
                    brush = verticalBrush,
                    topLeft = Offset(size.width - frameWidth, 0f),
                    size = Size(frameWidth, size.height)
                )

                val horizontalGrains = listOf(0.28f, 0.62f)
                horizontalGrains.forEach { fraction ->
                    val topY = frameWidth * fraction
                    val bottomY = size.height - frameWidth + topY
                    drawLine(
                        color = WoodGrain,
                        start = Offset(frameWidth * 1.4f, topY),
                        end = Offset(size.width - frameWidth * 1.4f, topY + 1.5f),
                        strokeWidth = grainStroke
                    )
                    drawLine(
                        color = WoodGrain,
                        start = Offset(frameWidth * 1.4f, bottomY),
                        end = Offset(size.width - frameWidth * 1.4f, bottomY - 1.5f),
                        strokeWidth = grainStroke
                    )
                }

                val verticalGrains = listOf(0.26f, 0.56f, 0.82f)
                verticalGrains.forEach { fraction ->
                    val y = size.height * fraction
                    drawLine(
                        color = WoodGrain,
                        start = Offset(frameWidth * 0.5f, y - frameWidth),
                        end = Offset(frameWidth * 0.5f + 1.5f, y + frameWidth),
                        strokeWidth = grainStroke
                    )
                    drawLine(
                        color = WoodGrain,
                        start = Offset(size.width - frameWidth * 0.5f, y - frameWidth),
                        end = Offset(size.width - frameWidth * 0.5f - 1.5f, y + frameWidth),
                        strokeWidth = grainStroke
                    )
                }

                val pegOffsets = listOf(
                    Offset(frameWidth * 0.52f, frameWidth * 0.52f),
                    Offset(size.width - frameWidth * 0.52f, frameWidth * 0.52f),
                    Offset(frameWidth * 0.52f, size.height - frameWidth * 0.52f),
                    Offset(size.width - frameWidth * 0.52f, size.height - frameWidth * 0.52f)
                )
                pegOffsets.forEach { center ->
                    drawCircle(color = WoodEdge, radius = pegRadius, center = center)
                    drawCircle(color = WoodHighlight, radius = pegRadius * 0.42f, center = center - Offset(0.7f, 0.7f))
                }

                drawRect(
                    color = WoodEdge,
                    size = size,
                    style = Stroke(width = outerStroke)
                )
                drawRect(
                    color = WoodHighlight,
                    topLeft = Offset(frameWidth - innerStroke, frameWidth - innerStroke),
                    size = Size(
                        width = (size.width - frameWidth * 2 + innerStroke * 2).coerceAtLeast(0f),
                        height = (size.height - frameWidth * 2 + innerStroke * 2).coerceAtLeast(0f)
                    ),
                    style = Stroke(width = innerStroke)
                )
            }
        }
    )
}

fun Modifier.woodFrame(
    cornerRadius: Dp = 8.dp
): Modifier {
    return clip(RoundedCornerShape(cornerRadius))
        .drawWithCache {
            val radius = cornerRadius.toPx()
            val corner = CornerRadius(radius, radius)
            val outerStroke = 1.5.dp.toPx()
            val innerStroke = 1.dp.toPx()
            val grainStroke = 1.dp.toPx()

            val panelBrush = Brush.verticalGradient(
                colors = listOf(
                    WoodLight,
                    WoodWarm,
                    WoodBase,
                    WoodDark
                )
            )

            onDrawBehind {
                drawRoundRect(
                    brush = panelBrush,
                    cornerRadius = corner
                )

                val grainYs = listOf(0.18f, 0.32f, 0.48f, 0.63f, 0.78f)
                grainYs.forEachIndexed { index, fraction ->
                    val y = size.height * fraction
                    val startX = if (index % 2 == 0) size.width * 0.08f else size.width * 0.16f
                    val endX = size.width - startX
                    drawLine(
                        color = WoodGrain,
                        start = Offset(startX, y),
                        end = Offset(endX, y + ((index % 2) * 1.5f)),
                        strokeWidth = grainStroke
                    )
                }

                drawRoundRect(
                    color = WoodHighlight,
                    topLeft = Offset(outerStroke, outerStroke),
                    size = Size(
                        width = (size.width - outerStroke * 2).coerceAtLeast(0f),
                        height = (size.height * 0.28f).coerceAtLeast(0f)
                    ),
                    cornerRadius = corner
                )

                drawRoundRect(
                    color = WoodEdge,
                    cornerRadius = corner,
                    style = Stroke(width = outerStroke)
                )

                drawRoundRect(
                    color = WoodHighlight,
                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                    size = Size(
                        width = (size.width - 6.dp.toPx()).coerceAtLeast(0f),
                        height = (size.height - 6.dp.toPx()).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(
                        x = (radius - 2.dp.toPx()).coerceAtLeast(0f),
                        y = (radius - 2.dp.toPx()).coerceAtLeast(0f)
                    ),
                    style = Stroke(width = innerStroke)
                )
            }
        }
}
