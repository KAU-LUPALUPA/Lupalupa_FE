package com.example.lupapj.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import kotlin.math.roundToInt

private const val TOY_BOX_BOTTOM_TRIM_PX = 54

@Composable
fun ToyBoxImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = "장난감 박스"
) {
    val resources = LocalContext.current.resources
    val bitmap = remember(resources) {
        ImageBitmap.imageResource(
            res = resources,
            id = R.drawable.toy_box
        )
    }

    val trimmedHeight = (bitmap.height - TOY_BOX_BOTTOM_TRIM_PX).coerceAtLeast(1)

    Canvas(
        modifier = if (contentDescription != null) {
            modifier.semantics {
                this.contentDescription = contentDescription
            }
        } else {
            modifier
        }
    ) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(
                width = bitmap.width,
                height = trimmedHeight
            ),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                width = size.width.roundToInt(),
                height = size.height.roundToInt()
            ),
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
fun ToyDuckImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = "오리 장난감"
) {
    val resources = LocalContext.current.resources
    val bitmap = remember(resources) {
        ImageBitmap.imageResource(
            res = resources,
            id = R.drawable.toy_duck
        )
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = Alignment.BottomCenter,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}

@Preview(showBackground = true)
@Composable
private fun ToyBoxImagePreview() {
    ToyBoxImage(
        modifier = Modifier.size(120.dp),
        contentDescription = "장난감 박스"
    )
}
