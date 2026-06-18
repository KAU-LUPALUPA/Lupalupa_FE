package com.example.lupapj.ui.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lupapj.R
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.label
import com.example.lupapj.ui.theme.LupaPJTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val MAIN_BOARD_ASPECT_RATIO = 1287f / 318f
private const val MAIN_BUTTON_ASPECT_RATIO = 620f / 552f
private const val MAIN_TIME_ASPECT_RATIO = 628f / 273f
private const val MAIN_SETTING_ASPECT_RATIO = 378f / 325f
private const val POPUP_ASPECT_RATIO = 705f / 509f

private val TimeTextColor = Color(0xFF5C371D)
private val ConditionPanelBackground = Color(0xF8FFF4DF)
private val ConditionPanelBorder = Color(0xB8875E32)
private val ConditionTextColor = Color(0xFF5C371D)
private val ConditionTrackColor = Color(0xFFE7D3B7)
private val SatietyFillColor = Color(0xFFFFA24A)
private val VitalityFillColor = Color(0xFF63BA76)
private val ConditionLowFillColor = Color(0xFFE15A4F)
private val ConditionMidFillColor = Color(0xFFE3B348)
private val PopupButtonBorderColor = Color(0xB88B5B2E)
private val EmptyRoomBackground = Color(0xFFFFF2D8)
private data class PopupMenuItem(
    val label: String,
    val iconRes: Int,
    val navItem: BottomNavItem? = null,
    val opensPlayground: Boolean = false,
    val opensContest: Boolean = false,
    val opensMinigame: Boolean = false // [수정됨(권)] 미니게임 진입 필드 추가
)

private val ContestPopupMenuItem = PopupMenuItem(
    label = "콘테스트",
    iconRes = R.drawable.contest_trophy_icon,
    opensContest = true
)

private val PopupMenuItems = listOf(
    PopupMenuItem(
        label = BottomNavItem.SCREENSHOT.label,
        iconRes = R.drawable.camera_trimmed,
        navItem = BottomNavItem.SCREENSHOT
    ),
    PopupMenuItem(
        label = BottomNavItem.GALLERY.label,
        iconRes = R.drawable.gallery_trimmed,
        navItem = BottomNavItem.GALLERY
    ),
    PopupMenuItem(
        label = BottomNavItem.CONTACTS.label,
        iconRes = R.drawable.friends_trimmed,
        navItem = BottomNavItem.CONTACTS
    ),
    PopupMenuItem(
        label = BottomNavItem.SHOP.label,
        iconRes = R.drawable.shop_trimmed,
        navItem = BottomNavItem.SHOP
    ),
    PopupMenuItem(
        label = "광장",
        iconRes = R.drawable.playground_trimmed,
        opensPlayground = true
    ),
    // [수정됨(권)] 미니게임 버튼 복구 (6번째 버튼으로 추가)
    PopupMenuItem(
        label = "미니게임",
        iconRes = R.drawable.minigame_icon, // [수정됨(권)] 리소스명 규칙(소문자)에 맞춰 수정
        opensMinigame = true
    )
)

@Composable
fun LupaMainScreen(
    modifier: Modifier = Modifier,
    currentTimeText: String? = null,
    isDayTime: Boolean? = null,
    petSatiety: Int = 80,
    petVitality: Int = 80,
    petCleanliness: Int = 100,
    petTraits: com.example.lupapj.data.model.PetTraits = com.example.lupapj.data.model.PetTraits(), // [추가됨(권)] 디버깅용 성격
    recentIconRes: Int? = null,
    onConditionTabClick: () -> Unit = {}, // [추가됨(권)] 디버깅용 클릭 이벤트
    onRecentActionClick: () -> Unit = {},
    onMainMenuClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onKakaoLoginClick: () -> Unit = {},
    onSettingClick: () -> Unit = {},
    onPopupMenuItemClick: (BottomNavItem) -> Unit = {},
    onPlaygroundClick: () -> Unit = {},
    onContestClick: () -> Unit = {},
    onMinigameClick: () -> Unit = {}, // [수정됨(권)] 미니게임 클릭 콜백 추가
    roomContent: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EmptyRoomBackground)
        )
    }
) {
    val currentTime = rememberCurrentTime()
    val resolvedTimeText = currentTimeText ?: currentTime.format(mainTimeFormatter)
    val resolvedIsDayTime = isDayTime ?: currentTime.isDayTime()
    var isPopupVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val boardWidth = resolveBoardWidth(maxWidth)
        val boardHeight = boardWidth / MAIN_BOARD_ASPECT_RATIO
        val popupWidth = resolvePopupWidth(maxWidth)
        val outsidePopupInteraction = remember { MutableInteractionSource() }

        roomContent()

        if (isPopupVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.07f))
                    .clickable(
                        interactionSource = outsidePopupInteraction,
                        indication = null
                    ) {
                        isPopupVisible = false
                    }
            )

            MainMenuPopup(
                onMenuItemClick = { item ->
                    isPopupVisible = false
                    when {
                        item.opensPlayground -> onPlaygroundClick()
                        item.opensContest -> onContestClick()
                        item.opensMinigame -> onMinigameClick() // [수정됨(권)] 미니게임 진입 처리
                        item.navItem != null -> onPopupMenuItemClick(item.navItem)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(bottom = boardHeight + 18.dp),
                width = popupWidth
            )
        }

        TopStatusLayer(
            currentTimeText = resolvedTimeText,
            isDayTime = resolvedIsDayTime,
            petSatiety = petSatiety,
            petVitality = petVitality,
            petCleanliness = petCleanliness,
            petTraits = petTraits, // [추가됨(권)]
            onTabClick = onConditionTabClick, // [추가됨(권)]
            onSettingClick = onSettingClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        BottomMenuLayer(
            boardWidth = boardWidth,
            recentIconRes = recentIconRes,
            onRecentActionClick = onRecentActionClick,
            onMainMenuClick = {
                isPopupVisible = !isPopupVisible
                onMainMenuClick()
            },
            onInventoryClick = onInventoryClick,
            onKakaoLoginClick = onKakaoLoginClick,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun TopStatusLayer(
    currentTimeText: String,
    isDayTime: Boolean,
    petSatiety: Int,
    petVitality: Int,
    petCleanliness: Int,
    petTraits: com.example.lupapj.data.model.PetTraits,
    onTabClick: () -> Unit,
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        val timePanelWidth = resolveTimePanelWidth(maxWidth)
        val settingWidth = resolveSettingWidth(maxWidth)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(timePanelWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimePanel(
                    currentTimeText = currentTimeText,
                    isDayTime = isDayTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(MAIN_TIME_ASPECT_RATIO)
                )

                PetConditionFloatingTab(
                    satiety = petSatiety,
                    vitality = petVitality,
                    cleanliness = petCleanliness,
                    onTabClick = onTabClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Image(
                painter = painterResource(id = R.drawable.main_setting_trimmed),
                contentDescription = "설정",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(settingWidth)
                    .aspectRatio(MAIN_SETTING_ASPECT_RATIO)
                    .clickable(onClick = onSettingClick)
            )
        }
    }
}

@Composable
private fun PetConditionFloatingTab(
    satiety: Int,
    vitality: Int,
    cleanliness: Int,
    onTabClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(ConditionPanelBackground)
            .border(1.dp, ConditionPanelBorder, shape)
            .clickable(onClick = onTabClick) // [추가됨(권)] 디버깅 클릭 이벤트
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PetConditionBar(
            label = "포만감",
            value = satiety,
            fillColor = SatietyFillColor
        )
        PetConditionBar(
            label = "활력",
            value = vitality,
            fillColor = VitalityFillColor
        )
        PetConditionBar(
            label = "청결",
            value = cleanliness,
            fillColor = androidx.compose.ui.graphics.Color(0xFF81D4FA)
        )
    }
}

@Composable
private fun PetConditionBar(
    label: String,
    value: Int,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val safeValue = value.coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = safeValue / 100f,
        animationSpec = tween(durationMillis = 280),
        label = "$label progress"
    )
    val resolvedFillColor = conditionFillColor(
        value = safeValue,
        normalColor = fillColor
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ConditionTextColor,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.width(38.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(ConditionTrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(resolvedFillColor)
            )
        }

        Text(
            text = safeValue.toString(),
            color = ConditionTextColor,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
fun TimePanel(
    currentTimeText: String,
    isDayTime: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val iconSize = minOf(maxHeight * 0.46f, 32.dp)

        Image(
            painter = painterResource(id = R.drawable.main_time_trimmed),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )

        Row(
            modifier = Modifier
                .matchParentSize()
                .offset(y = -maxHeight * 0.07f)
                .padding(start = maxWidth * 0.13f, end = maxWidth * 0.12f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    id = if (isDayTime) {
                        R.drawable.time_sun_trimmed
                    } else {
                        R.drawable.time_moon_trimmed
                    }
                ),
                contentDescription = if (isDayTime) "낮" else "밤",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentTimeText,
                color = TimeTextColor,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BottomMenuLayer(
    boardWidth: Dp,
    recentIconRes: Int?,
    onRecentActionClick: () -> Unit,
    onMainMenuClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boardHeight = boardWidth / MAIN_BOARD_ASPECT_RATIO

        Box(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
                .offset(y = boardHeight * 0.055f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_board_trimmed),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize()
            )

            BoxWithConstraints(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = boardWidth * 0.105f),
                contentAlignment = Alignment.Center
            ) {
                val sideButtonWidth = (maxWidth * 0.28f).coerceIn(72.dp, 96.dp)
                val mainButtonWidth = (sideButtonWidth * 1.18f).coerceIn(84.dp, 116.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = -boardHeight * 0.05f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomMenuButton(
                        buttonWidth = sideButtonWidth,
                        iconRes = recentIconRes,
                        iconWidthFraction = 0.54f,
                        contentDescription = "최근 사용 기능",
                        onClick = onRecentActionClick
                    )
                    BottomMenuButton(
                        buttonWidth = mainButtonWidth,
                        iconRes = R.drawable.main_action_trimmed,
                        iconWidthFraction = 0.58f,
                        contentDescription = "메인 메뉴",
                        onClick = onMainMenuClick

                    )
                    BottomMenuButton(
                        buttonWidth = sideButtonWidth,
                        iconRes = R.drawable.main_inv_trimmed,
                        iconWidthFraction = 0.52f,
                        contentDescription = "인벤토리",
                        onClick = onInventoryClick
                    )
                }
            }
        }
    }
}

@Composable
fun BottomMenuButton(
    buttonWidth: Dp,
    iconRes: Int?,
    iconWidthFraction: Float,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .width(buttonWidth)
            .aspectRatio(MAIN_BUTTON_ASPECT_RATIO)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_button_trimmed),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )

        iconRes?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(maxWidth * iconWidthFraction)
                    .offset(y = -maxHeight * 0.05f)
            )
        }
    }
}

@Composable
private fun MainMenuPopup(
    width: Dp,
    onMenuItemClick: (PopupMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .width(width)
            .aspectRatio(POPUP_ASPECT_RATIO),
        contentAlignment = Alignment.Center
    ) {
        val popupWidth = maxWidth
        val popupHeight = maxHeight

        Image(
            painter = painterResource(id = R.drawable.popup_trimmed),
            contentDescription = "메인 메뉴 팝업",
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    start = popupWidth * 0.10f,
                    end = popupWidth * 0.10f,
                    top = popupHeight * 0.12f,
                    bottom = popupHeight * 0.14f
                ),
            verticalArrangement = Arrangement.spacedBy(popupHeight * 0.045f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PopupMenuButton(
                    iconRes = ContestPopupMenuItem.iconRes,
                    contentDescription = ContestPopupMenuItem.label,
                    onClick = { onMenuItemClick(ContestPopupMenuItem) },
                    modifier = Modifier.width(popupWidth * 0.30f),
                    buttonHeight = popupHeight * 0.20f
                )
            }

            PopupMenuItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(popupWidth * 0.035f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (rowItems.size) {
                        1 -> Spacer(modifier = Modifier.weight(1f))
                        2 -> Spacer(modifier = Modifier.weight(0.5f))
                    }

                    rowItems.forEach { item ->
                        PopupMenuButton(
                            iconRes = item.iconRes,
                            contentDescription = item.label,
                            onClick = { onMenuItemClick(item) },
                            modifier = Modifier.weight(1f),
                            buttonHeight = popupHeight * 0.24f
                        )
                    }

                    when (rowItems.size) {
                        1 -> Spacer(modifier = Modifier.weight(1f))
                        2 -> Spacer(modifier = Modifier.weight(0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupMenuButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 44.dp
) {
    BoxWithConstraints(
        modifier = modifier
            .height(buttonHeight.coerceIn(48.dp, 64.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(maxWidth * 0.74f)
                .height(maxHeight * 0.68f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xF7FFF5DB))
                .border(
                    width = 1.dp,
                    color = PopupButtonBorderColor,
                    shape = RoundedCornerShape(8.dp)
                )
        )

        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun rememberCurrentTime(): LocalTime {
    var time by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            time = LocalTime.now()
            delay(1_000L)
        }
    }

    return time
}

private val mainTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun LocalTime.isDayTime(): Boolean {
    return hour in 6..17
}

private fun resolveBoardWidth(screenWidth: Dp): Dp {
    return minOf(screenWidth * 0.93f, 430.dp)
}

private fun resolvePopupWidth(screenWidth: Dp): Dp {
    return (screenWidth * 0.92f).coerceAtMost(430.dp)
}

private fun resolveTimePanelWidth(screenWidth: Dp): Dp {
    return (screenWidth * 0.43f).coerceIn(148.dp, 190.dp)
}

private fun resolveSettingWidth(screenWidth: Dp): Dp {
    return (screenWidth * 0.155f).coerceIn(56.dp, 74.dp)
}

private fun conditionFillColor(
    value: Int,
    normalColor: Color
): Color {
    return when {
        value <= 20 -> ConditionLowFillColor
        value <= 45 -> ConditionMidFillColor
        else -> normalColor
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LupaMainScreenCompactPreview() {
    LupaPJTheme {
        LupaMainScreen(
            currentTimeText = "12:30",
            isDayTime = true
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LupaMainScreenTallPreview() {
    LupaPJTheme {
        LupaMainScreen(
            currentTimeText = "21:45",
            isDayTime = false
        )
    }
}
