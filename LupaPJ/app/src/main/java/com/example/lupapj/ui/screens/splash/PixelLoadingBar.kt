package com.example.lupapj.ui.screens.splash

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.example.lupapj.R

@Composable
fun PixelLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val strokeWidthDp = dimensionResource(id = R.dimen.splash_loading_bar_stroke_width)
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidthDp.toPx()
        val cornerRadiusPx = size.height / 2f
        
        // Background Track
        drawRoundRect(
            color = Color(0xFF1E0F05),
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        
        // Fill Gradient
        val fillWidth = size.width * progress
        if (fillWidth > cornerRadiusPx * 2) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF0C040), Color(0xFFB8860B)),
                    startX = 0f,
                    endX = size.width 
                ),
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
            
            // Inner Shine (semi-transparent gloss)
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(cornerRadiusPx, strokeWidthPx * 1.5f),
                end = Offset(fillWidth - cornerRadiusPx, strokeWidthPx * 1.5f),
                strokeWidth = strokeWidthPx
            )
        } else if (fillWidth > 0) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF0C040), Color(0xFFB8860B)),
                    startX = 0f,
                    endX = size.width
                ),
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(fillWidth / 2f, fillWidth / 2f) 
            )
        }
        
        // Border Frame
        drawRoundRect(
            color = Color(0xFF3D1C02),
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = strokeWidthPx)
        )
    }
}
