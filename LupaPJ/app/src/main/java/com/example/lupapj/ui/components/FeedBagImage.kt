package com.example.lupapj.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.R

@Composable
fun FeedBagImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = "사료 봉투"
) {
    val resources = LocalContext.current.resources
    val bitmap = remember(resources) {
        ImageBitmap.imageResource(
            res = resources,
            id = R.drawable.feed_bag_brown
        )
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}

@Composable
fun FeedPelletImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = "사료 알갱이"
) {
    val resources = LocalContext.current.resources
    val bitmap = remember(resources) {
        ImageBitmap.imageResource(
            res = resources,
            id = R.drawable.feed_food_pellet
        )
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedBagImagePreview() {
    FeedBagImage(
        modifier = Modifier.size(120.dp),
        contentDescription = "사료 봉투"
    )
}
