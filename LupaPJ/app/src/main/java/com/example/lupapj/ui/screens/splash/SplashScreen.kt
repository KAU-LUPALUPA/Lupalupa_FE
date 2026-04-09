package com.example.lupapj.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lupapj.R
import com.example.lupapj.loading.LoadingController
import com.example.lupapj.loading.SimulatedLoadingManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var progress by remember { mutableIntStateOf(0) }
    var logoAlpha by remember { mutableFloatStateOf(0f) }
    var loadingFinished by remember { mutableStateOf(false) }

    val logoAlphaAnim by animateFloatAsState(
        targetValue = logoAlpha,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "LogoAlpha"
    )

    var splashVisible by remember { mutableStateOf(true) }
    val splashAlpha by animateFloatAsState(
        targetValue = if (splashVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        finishedListener = { onSplashComplete() },
        label = "SplashFade"
    )

    val controller = remember {
        object : LoadingController {
            override fun onProgressUpdate(p: Int) {
                progress = p
            }
            override fun onLoadingComplete() {
                loadingFinished = true
            }
        }
    }

    var loadingJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        logoAlpha = 1f
        delay(800) // Wait for logo to fade in
        
        loadingJob = launch {
            // TODO: Replace with RealLoadingManager when backend is ready
            val manager = SimulatedLoadingManager(controller)
            manager.startLoading()
        }
    }

    LaunchedEffect(loadingFinished) {
        if (loadingFinished) {
            splashVisible = false
        }
    }

    val progressAnim by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(durationMillis = 300),
        label = "ProgressBarState"
    )

    val message = when {
        progress < 21 -> stringResource(id = R.string.splash_loading_1, progress)
        progress < 46 -> stringResource(id = R.string.splash_loading_2, progress)
        progress < 66 -> stringResource(id = R.string.splash_loading_3, progress)
        progress < 86 -> stringResource(id = R.string.splash_loading_4, progress)
        progress < 100 -> stringResource(id = R.string.splash_loading_5, progress)
        else -> stringResource(id = R.string.splash_loading_complete)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (logoAlphaAnim > 0f && !loadingFinished) {
                    loadingJob?.cancel()
                    progress = 100
                    loadingFinished = true
                }
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_loading),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(splashAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            Image(
                painter = painterResource(id = R.drawable.ic_logo_loopaloopa),
                contentDescription = "Logo",
                modifier = Modifier
                    .alpha(logoAlphaAnim)
                    .wrapContentSize()
            )

            if (logoAlphaAnim > 0.5f) {
                Text(
                    text = stringResource(id = R.string.splash_subtitle),
                    color = Color(0xFFF5E6C8),
                    fontSize = dimensionResource(id = R.dimen.splash_subtitle_text_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFF3D1C02), // Dark brown, sharp shadow
                            offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                            blurRadius = 0f // 0 blur for pixel art style
                        )
                    ),
                    modifier = Modifier
                        .padding(top = dimensionResource(id = R.dimen.splash_subtitle_margin_top))
                )
            }

            Spacer(modifier = Modifier.weight(0.4f))

            if (logoAlphaAnim >= 1f) {
                PixelLoadingBar(
                    progress = progressAnim,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(dimensionResource(id = R.dimen.splash_loading_bar_height))
                )

                Text(
                    text = message,
                    color = Color(0xFFF5E6C8),
                    fontSize = dimensionResource(id = R.dimen.splash_loading_text_size).value.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.splash_loading_text_margin_top))
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}
