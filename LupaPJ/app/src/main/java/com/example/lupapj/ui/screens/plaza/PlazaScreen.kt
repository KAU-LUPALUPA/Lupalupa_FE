package com.example.lupapj.ui.screens.plaza

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import com.example.lupapj.data.model.plaza.PLAZA_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.plaza.PlazaChatMessage
import com.example.lupapj.data.model.plaza.PlazaCode
import com.example.lupapj.data.model.plaza.PlazaInteractionEvent
import com.example.lupapj.data.model.plaza.PlazaInteractionType
import com.example.lupapj.data.model.plaza.PlazaMovementCommand
import com.example.lupapj.data.model.plaza.PlazaParticipant
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaPosition
import com.example.lupapj.data.model.plaza.PlazaRoom
import com.example.lupapj.data.model.plaza.PlazaServerTime
import com.example.lupapj.ui.components.AnimatedCharacterSprite
import com.example.lupapj.ui.components.CharacterAnimation
import com.example.lupapj.ui.theme.LupaPJTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private const val PLAZA_PET_MOVE_DURATION_MS = 1_450
private const val PLAZA_PET_DIRECTION_EPSILON = 0.006f
private const val PLAZA_PET_AXIS_DOMINANCE_RATIO = 1.75f
private const val PLAZA_WANDER_MIN_DELAY_MS = 1_100L
private const val PLAZA_WANDER_MAX_DELAY_MS = 2_700L
private const val PLAZA_INTERACTION_SCAN_INTERVAL_MS = 700L
private const val PLAZA_INTERACTION_COOLDOWN_MS = 3_200L
private const val PLAZA_INTERACTION_DISTANCE = 0.31f
private const val PLAZA_INTERACTION_FALLBACK_SCAN_COUNT = 7
private const val PLAZA_CHAT_BUBBLE_DURATION_MS = 2_600L
private const val PLAZA_CHAT_BUBBLE_MAX_LENGTH = 28
private const val PLAZA_PET_MIN_X = 0.14f
private const val PLAZA_PET_MAX_X = 0.86f
private const val PLAZA_PET_MIN_Y = 0.47f
private const val PLAZA_PET_MAX_Y = 0.88f
private const val PLAZA_GREET_OFFSET_X = 0.065f
private const val PLAZA_GREET_OFFSET_Y = 0.035f
private const val PLAZA_FOLLOW_LEADER_STEP_X = 0.18f
private const val PLAZA_FOLLOW_LEADER_STEP_Y = 0.12f
private const val PLAZA_FOLLOW_TRAIL_STEP_X = 0.10f
private const val PLAZA_FOLLOW_TRAIL_STEP_Y = 0.06f
private const val PLAZA_FOLLOW_MIN_VECTOR = 0.03f
private const val PLAZA_PLAY_OFFSET_X = 0.08f
private const val PLAZA_PLAY_OFFSET_Y = 0.045f
private const val PLAZA_REST_OFFSET_X = 0.065f
private const val PLAZA_REST_OFFSET_Y = 0.02f
private const val PLAZA_FOUNTAIN_SIZE_DP = 136f
private const val PLAZA_FOUNTAIN_ANCHOR_X = 0.50f
private const val PLAZA_FOUNTAIN_ANCHOR_Y = 0.58f
private const val PLAZA_FOUNTAIN_ANCHOR_TO_TOP_RATIO = 0.72f

private val PlazaIntroBackground = Color(0xFFFFF7EA)
private val PlazaIntroCardBackground = Color(0xFFFFFCF7)
private val PlazaIntroMainText = Color(0xFF6B5548)
private val PlazaIntroSubText = Color(0xFF9A7B68)
private val PlazaIntroPrimaryGreen = Color(0xFF4F765C)
private val PlazaIntroInactiveGreen = Color(0xFF9EB89E)
private val PlazaIntroDisabledButton = Color(0xFFEFECE7)
private val PlazaIntroDisabledText = Color(0xFFC9BDB4)
private val PlazaIntroInputBorder = Color(0xFFD7C7B8)

private data class PlazaChatBubble(
    val text: String,
    val expiresAtMillis: Long
)

private data class PlazaInteractionPresentation(
    val event: PlazaInteractionEvent,
    val movementTargetByUserId: Map<String, PlazaPosition> = emptyMap(),
    val facingTargetByUserId: Map<String, PlazaPosition> = emptyMap(),
    val lockedWanderingUserIds: Set<String> = emptySet(),
    val animatedSpriteUserIds: Set<String> = emptySet(),
    val idleAnimationByUserId: Map<String, CharacterAnimation> = emptyMap(),
    val bubbleColor: Color,
    val bubbleTextColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlazaScreen(
    activePlaza: PlazaRoom?,
    codeInput: String,
    messageInput: String,
    isJoining: Boolean,
    isSendingMessage: Boolean,
    feedbackMessage: String?,
    onRandomJoin: () -> Unit,
    onCodeInputChange: (String) -> Unit,
    onCodeJoin: () -> Unit,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onLeavePlaza: () -> Unit,
    onBackHome: () -> Unit,
    onFeedbackConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFeedbackConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = if (activePlaza == null) {
            PlazaIntroBackground
        } else {
            MaterialTheme.colorScheme.background
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (activePlaza == null) {
                PlazaEntryTopTabs(onBackHome = onBackHome)
            } else {
                TopAppBar(
                    title = {
                        Text(text = "광장 ${activePlaza.displayCode}")
                    },
                    navigationIcon = {
                        TextButton(onClick = onBackHome) {
                            Text("내 방")
                        }
                    },
                    actions = {
                        TextButton(onClick = onLeavePlaza) {
                            Text("나가기")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activePlaza == null) {
                PlazaEntryContent(
                    codeInput = codeInput,
                    isJoining = isJoining,
                    onRandomJoin = onRandomJoin,
                    onCodeInputChange = onCodeInputChange,
                    onCodeJoin = onCodeJoin,
                    modifier = Modifier
                        .fillMaxSize()
                )
            } else {
                PlazaRoomContent(
                    plaza = activePlaza,
                    messageInput = messageInput,
                    isSendingMessage = isSendingMessage,
                    onMessageInputChange = onMessageInputChange,
                    onSendMessage = onSendMessage,
                    onFeedbackMessage = { message ->
                        snackbarHostState.showSnackbar(message)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .imePadding()
                )
            }
        }
    }
}

@Composable
private fun PlazaEntryTopTabs(
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PlazaIntroBackground
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlazaEntryTab(
                text = "내 방",
                selected = false,
                onClick = onBackHome
            )
            Spacer(modifier = Modifier.width(18.dp))
            PlazaEntryTab(
                text = "광장",
                selected = true,
                onClick = {}
            )
        }
    }
}

@Composable
private fun PlazaEntryTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !selected, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) PlazaIntroMainText else PlazaIntroInactiveGreen
        )
        Spacer(modifier = Modifier.height(5.dp))
        Surface(
            modifier = Modifier
                .width(30.dp)
                .height(4.dp),
            shape = RoundedCornerShape(999.dp),
            color = if (selected) PlazaIntroPrimaryGreen else Color.Transparent
        ) {}
    }
}

@Composable
private fun PlazaEntryContent(
    codeInput: String,
    isJoining: Boolean,
    onRandomJoin: () -> Unit,
    onCodeInputChange: (String) -> Unit,
    onCodeJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCodeJoinEnabled = !isJoining && PlazaCode.fromInput(codeInput) != null

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.plaza_intro_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        PlazaIntroCard(
            iconResId = R.drawable.icon_friends,
            title = "오늘은 어떤 광장으로 갈까요?",
            description = "랜덤으로 열린 작은 광장에서\n최대 4명의 루파루파들과 함께 머물러요.",
            iconSize = 64.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = onRandomJoin,
                enabled = !isJoining,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlazaIntroPrimaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor = PlazaIntroDisabledButton,
                    disabledContentColor = PlazaIntroDisabledText
                )
            ) {
                Text(
                    text = if (isJoining) "입장 중" else "광장으로 가기",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        PlazaIntroCard(
            iconResId = R.drawable.icon_card,
            title = "친구 광장에 입장",
            description = "초대 코드를 입력해\n친구와 같은 광장으로 이동해요.",
            iconSize = 60.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = codeInput,
                onValueChange = onCodeInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(
                        text = "PZ-0000",
                        color = PlazaIntroDisabledText
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = PlazaIntroMainText,
                    fontWeight = FontWeight.SemiBold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PlazaIntroMainText,
                    unfocusedTextColor = PlazaIntroMainText,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = PlazaIntroPrimaryGreen,
                    unfocusedBorderColor = PlazaIntroInputBorder,
                    cursorColor = PlazaIntroPrimaryGreen
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCodeJoin,
                enabled = isCodeJoinEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlazaIntroPrimaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor = PlazaIntroDisabledButton,
                    disabledContentColor = PlazaIntroDisabledText
                )
            ) {
                Text(
                    text = if (isJoining) "입장 중" else "코드로 입장",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Image(
            painter = painterResource(id = R.drawable.plaza_intro_bottom),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
        )
    }
}

@Composable
private fun PlazaIntroCard(
    iconResId: Int,
    title: String,
    description: String,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = PlazaIntroCardBackground
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(iconSize)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlazaIntroMainText
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlazaIntroSubText
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun PlazaRoomContent(
    plaza: PlazaRoom,
    messageInput: String,
    isSendingMessage: Boolean,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFeedbackMessage: suspend (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlazaCodeHeader(
            plaza = plaza,
            onFeedbackMessage = onFeedbackMessage
        )
        PlazaScene(
            plazaId = plaza.plazaId,
            participants = plaza.participants,
            messages = plaza.messages,
            remoteInteractions = plaza.interactions,
            serverTime = plaza.serverTime,
            isServerAuthoritative = plaza.isServerAuthoritative,
            modifier = Modifier.fillMaxWidth()
        )
        PlazaChatPanel(
            messages = plaza.messages,
            currentUserId = plaza.participants.firstOrNull { it.isMe }?.userId,
            messageInput = messageInput,
            isSendingMessage = isSendingMessage,
            onMessageInputChange = onMessageInputChange,
            onSendMessage = onSendMessage,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlazaCodeHeader(
    plaza: PlazaRoom,
    onFeedbackMessage: suspend (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "광장 코드",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = plaza.displayCode,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = plaza.participantCountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(end = 8.dp)
            )
            TextButton(
                onClick = {
                    clipboardManager.setPrimaryClip(
                        ClipData.newPlainText("plaza code", plaza.displayCode)
                    )
                    coroutineScope.launch {
                        onFeedbackMessage("광장 코드를 복사했어요.")
                    }
                }
            ) {
                Text("복사")
            }
        }
    }
}

@Composable
private fun PlazaScene(
    plazaId: String,
    participants: List<PlazaParticipant>,
    messages: List<PlazaChatMessage>,
    remoteInteractions: List<PlazaInteractionEvent>,
    serverTime: PlazaServerTime?,
    isServerAuthoritative: Boolean,
    modifier: Modifier = Modifier
) {
    val currentServerTime by rememberUpdatedState(serverTime)
    val sceneKey = remember(plazaId, participants) {
        val stableParticipantIds = participants
            .map { it.userId }
            .sorted()
            .joinToString(separator = "|")
        "$plazaId|$stableParticipantIds"
    }
    var petPositions by remember(sceneKey) {
        mutableStateOf(
            participants
                .mapIndexed { index, participant ->
                    participant.userId to (
                        participant.position ?: initialPlazaPetPosition(index, participant.userId)
                    )
                }
                .toMap()
        )
    }
    var localInteraction by remember(sceneKey) {
        mutableStateOf<PlazaInteractionEvent?>(null)
    }
    var activeRemoteInteraction by remember(sceneKey) {
        mutableStateOf<PlazaInteractionEvent?>(null)
    }
    var activeChatBubbles by remember(sceneKey) {
        mutableStateOf<Map<String, PlazaChatBubble>>(emptyMap())
    }
    var seenChatMessageIds by remember(sceneKey) {
        mutableStateOf(messages.map { it.id }.toSet())
    }
    var frameClientNowMillis by remember(sceneKey) {
        mutableStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(sceneKey, isServerAuthoritative, serverTime) {
        if (!isServerAuthoritative) return@LaunchedEffect

        while (isActive) {
            frameClientNowMillis = System.currentTimeMillis()
            delay(50L)
        }
    }

    LaunchedEffect(participants, sceneKey) {
        val now = currentServerTime.serverNowMillis()
        val serverPositions = participants
            .mapNotNull { participant ->
                participant.authoritativePositionAt(now)?.let { position ->
                    participant.userId to position
                }
            }
            .toMap()
        if (serverPositions.isNotEmpty()) {
            petPositions = petPositions + serverPositions
        }
    }

    LaunchedEffect(messages, sceneKey) {
        val participantIds = participants.map { it.userId }.toSet()
        val now = currentServerTime.serverNowMillis()
        val newMessages = messages.filter { message ->
            message.id !in seenChatMessageIds &&
                message.senderUserId in participantIds
        }

        seenChatMessageIds = seenChatMessageIds + messages.map { it.id }
        if (newMessages.isEmpty()) return@LaunchedEffect

        val nextBubbles = newMessages
            .takeLast(participants.size.coerceAtLeast(1))
            .mapNotNull { message ->
                message.senderUserId to PlazaChatBubble(
                    text = message.text.toPlazaChatBubbleText(),
                    expiresAtMillis = now + PLAZA_CHAT_BUBBLE_DURATION_MS
                )
            }
            .toMap()

        activeChatBubbles = activeChatBubbles + nextBubbles
    }

    LaunchedEffect(remoteInteractions, sceneKey) {
        if (remoteInteractions.isEmpty()) {
            activeRemoteInteraction = null
            return@LaunchedEffect
        }

        while (isActive) {
            val now = currentServerTime.serverNowMillis()
            activeRemoteInteraction = remoteInteractions.lastOrNull { interaction ->
                interaction.isActiveAt(now)
            }

            val hasPendingOrActiveInteraction = remoteInteractions.any { interaction ->
                interaction.startedAtMillis + interaction.durationMillis > now
            }
            if (!hasPendingOrActiveInteraction) break

            delay(250L)
        }
    }

    LaunchedEffect(activeChatBubbles) {
        if (activeChatBubbles.isEmpty()) return@LaunchedEffect

        while (isActive && activeChatBubbles.isNotEmpty()) {
            delay(250L)
            val now = currentServerTime.serverNowMillis()
            activeChatBubbles = activeChatBubbles.filterValues { it.expiresAtMillis > now }
        }
    }

    LaunchedEffect(sceneKey, remoteInteractions, isServerAuthoritative) {
        if (isServerAuthoritative) {
            localInteraction = null
            return@LaunchedEffect
        }

        var scansWithoutNearby = 0
        while (isActive) {
            delay(PLAZA_INTERACTION_SCAN_INTERVAL_MS)

            if (participants.size < 2 || localInteraction != null || activeRemoteInteraction != null) {
                continue
            }

            val nearbyPair = closestPlazaParticipantPair(
                participants = participants,
                positions = petPositions,
                maxDistance = PLAZA_INTERACTION_DISTANCE
            )
            val interactionPair = nearbyPair ?: if (
                scansWithoutNearby >= PLAZA_INTERACTION_FALLBACK_SCAN_COUNT
            ) {
                closestPlazaParticipantPair(
                    participants = participants,
                    positions = petPositions,
                    maxDistance = null
                )
            } else {
                null
            }

            if (interactionPair == null) {
                scansWithoutNearby += 1
                continue
            }

            scansWithoutNearby = 0
            localInteraction = createPlazaInteraction(
                plazaId = plazaId,
                first = interactionPair.first,
                second = interactionPair.second,
                positions = petPositions,
                startedAtMillis = currentServerTime.serverNowMillis()
            )
            delay(localInteraction?.durationMillis ?: PLAZA_INTERACTION_COOLDOWN_MS)
            localInteraction = null
            delay(PLAZA_INTERACTION_COOLDOWN_MS)
        }
    }

    Surface(
        modifier = modifier.height(318.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEAF6DE),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        ) {
            PlazaSceneBackground(modifier = Modifier.fillMaxSize())
            PlazaFountain(modifier = Modifier.fillMaxSize())

            val frameServerNowMillis = currentServerTime.serverNowMillis(frameClientNowMillis)
            val serverDrivenPetPositions = if (isServerAuthoritative) {
                participants
                    .mapIndexed { index, participant ->
                        participant.userId to (
                            participant.authoritativePositionAt(frameServerNowMillis)
                                ?: participant.position
                                ?: initialPlazaPetPosition(index, participant.userId)
                        )
                    }
                    .toMap()
            } else {
                emptyMap()
            }
            val displayPetPositions = if (isServerAuthoritative) {
                serverDrivenPetPositions
            } else {
                petPositions
            }
            val activeInteraction = activeRemoteInteraction ?: if (isServerAuthoritative) {
                null
            } else {
                localInteraction
            }
            val interactionPresentation = remember(
                activeInteraction?.id,
                activeInteraction?.movementTargetByUserId,
                activeInteraction?.facingTargetByUserId,
                activeInteraction?.animationByUserId,
                displayPetPositions
            ) {
                activeInteraction?.toPresentation(displayPetPositions)
            }

            participants
                .mapIndexed { index, participant -> index to participant }
                .sortedBy {
                    displayPetPositions[it.second.userId]?.y
                        ?: initialPlazaPetPosition(it.first, it.second.userId).y
                }
                .forEach { (index, participant) ->
                    val interaction = interactionPresentation?.event
                    val interactionPartnerId = interaction?.partnerOf(participant.userId)
                    val chatBubble = activeChatBubbles[participant.userId]
                    val serverNowMillis = frameServerNowMillis
                    val authoritativePosition = participant.authoritativePositionAt(
                        serverNowMillis
                    )
                    key(participant.userId) {
                        PlazaPetActor(
                            participant = participant,
                            index = index,
                            authoritativePosition = authoritativePosition,
                            authoritativeMovement = participant.movement,
                            serverNowMillis = serverNowMillis,
                            canWander = !isServerAuthoritative,
                            isInteractionLocked = interactionPresentation
                                ?.lockedWanderingUserIds
                                ?.contains(participant.userId) == true,
                            isInteractionAnimating = interactionPresentation
                                ?.animatedSpriteUserIds
                                ?.contains(participant.userId) == true,
                            interactionIdleAnimation = interactionPresentation
                                ?.idleAnimationByUserId
                                ?.get(participant.userId),
                            interactionText = interaction?.textFor(participant.userId),
                            interactionBubbleColor = interactionPresentation?.bubbleColor,
                            interactionBubbleTextColor = interactionPresentation?.bubbleTextColor,
                            chatBubbleText = chatBubble?.text,
                            chatBubbleColor = null,
                            chatBubbleTextColor = null,
                            movementTargetPosition = interactionPresentation
                                ?.movementTargetByUserId
                                ?.get(participant.userId),
                            facingTargetPosition = interactionPresentation
                                ?.facingTargetByUserId
                                ?.get(participant.userId)
                                ?: interactionPresentation
                                    ?.movementTargetByUserId
                                    ?.get(participant.userId)
                                ?: interactionPartnerId?.let { displayPetPositions[it] },
                            onPositionChange = { position ->
                                if (!isServerAuthoritative) {
                                    petPositions = petPositions + (participant.userId to position)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.86f),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "${participants.size}/4",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF31412A)
                )
            }
        }
    }
}

@Composable
private fun PlazaSceneBackground(
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val plazaSky = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.plaza_sky)
    }
    val plazaTile = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.plaza_tile)
    }
    val grassBase = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.grass_base)
    }
    val plazaTilePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawBitmapFitWidthClippedRect(
            bitmap = plazaSky,
            paint = plazaTilePaint,
            topLeft = Offset.Zero,
            size = Size(w, h * 0.34f)
        )
        drawBitmapClippedTileRect(
            bitmap = grassBase,
            paint = plazaTilePaint,
            topLeft = Offset(0f, h * 0.34f),
            size = Size(w, h * 0.66f)
        )
        drawRect(
            color = Color(0x337BA461),
            topLeft = Offset(0f, h * 0.34f),
            size = Size(w, h * 0.08f)
        )

        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.44f)
            lineTo(w * 0.82f, h * 0.44f)
            lineTo(w * 0.96f, h * 0.96f)
            lineTo(w * 0.04f, h * 0.96f)
            close()
        }
        drawPath(
            path = path,
            color = Color(0xFFE9D5A8)
        )
        drawProjectedPlazaFloorTile(
            floorPath = path,
            bitmap = plazaTile,
            paint = plazaTilePaint,
            topLeft = Offset(w * 0.18f, h * 0.44f),
            topRight = Offset(w * 0.82f, h * 0.44f),
            bottomRight = Offset(w * 0.96f, h * 0.96f),
            bottomLeft = Offset(w * 0.04f, h * 0.96f)
        )
        drawPath(
            path = path,
            color = Color(0x553A2B1F),
            style = Stroke(width = 2.dp.toPx())
        )

        drawLine(
            color = Color(0xFF5D7A4B),
            start = Offset(w * 0.08f, h * 0.37f),
            end = Offset(w * 0.92f, h * 0.37f),
            strokeWidth = 4.dp.toPx()
        )
        drawLine(
            color = Color(0xFF5D7A4B),
            start = Offset(w * 0.12f, h * 0.34f),
            end = Offset(w * 0.12f, h * 0.43f),
            strokeWidth = 5.dp.toPx()
        )
        drawLine(
            color = Color(0xFF5D7A4B),
            start = Offset(w * 0.88f, h * 0.34f),
            end = Offset(w * 0.88f, h * 0.43f),
            strokeWidth = 5.dp.toPx()
        )
    }
}

private fun DrawScope.drawBitmapFitWidthClippedRect(
    bitmap: Bitmap,
    paint: Paint,
    topLeft: Offset,
    size: Size
) {
    val scaledHeight = size.width * bitmap.height / bitmap.width
    val drawTop = topLeft.y + size.height - scaledHeight
    val destinationRect = RectF(
        topLeft.x,
        drawTop,
        topLeft.x + size.width,
        drawTop + scaledHeight
    )

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        val checkpoint = nativeCanvas.save()
        nativeCanvas.clipRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height
        )
        nativeCanvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            destinationRect,
            paint
        )
        nativeCanvas.restoreToCount(checkpoint)
    }
}

private fun DrawScope.drawBitmapClippedTileRect(
    bitmap: Bitmap,
    paint: Paint,
    topLeft: Offset,
    size: Size
) {
    val tileSize = size.height
    val sourceRect = Rect(0, 0, bitmap.width, bitmap.height)

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        val checkpoint = nativeCanvas.save()
        nativeCanvas.clipRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height
        )

        var tileLeft = topLeft.x
        while (tileLeft < topLeft.x + size.width) {
            nativeCanvas.drawBitmap(
                bitmap,
                sourceRect,
                RectF(
                    tileLeft,
                    topLeft.y,
                    tileLeft + tileSize,
                    topLeft.y + tileSize
                ),
                paint
            )
            tileLeft += tileSize
        }
        nativeCanvas.restoreToCount(checkpoint)
    }
}

private fun DrawScope.drawProjectedPlazaFloorTile(
    floorPath: Path,
    bitmap: Bitmap,
    paint: Paint,
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset
) {
    val src = floatArrayOf(
        0f,
        0f,
        bitmap.width.toFloat(),
        0f,
        bitmap.width.toFloat(),
        bitmap.height.toFloat(),
        0f,
        bitmap.height.toFloat()
    )
    val dst = floatArrayOf(
        topLeft.x,
        topLeft.y,
        topRight.x,
        topRight.y,
        bottomRight.x,
        bottomRight.y,
        bottomLeft.x,
        bottomLeft.y
    )
    val matrix = Matrix().apply {
        setPolyToPoly(src, 0, dst, 0, 4)
    }

    clipPath(floorPath) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val checkpoint = nativeCanvas.save()
            nativeCanvas.concat(matrix)
            nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)
            nativeCanvas.restoreToCount(checkpoint)
        }
    }
}

@Composable
private fun PlazaFountain(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val fountainSize = PLAZA_FOUNTAIN_SIZE_DP.dp
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val fountainSizePx = with(density) { fountainSize.toPx() }
        val offsetX = (viewportWidthPx * PLAZA_FOUNTAIN_ANCHOR_X - fountainSizePx * 0.5f)
            .roundToInt()
            .coerceIn(0, maxOf(0, (viewportWidthPx - fountainSizePx).roundToInt()))
        val offsetY = (
            viewportHeightPx * PLAZA_FOUNTAIN_ANCHOR_Y -
                fountainSizePx * PLAZA_FOUNTAIN_ANCHOR_TO_TOP_RATIO
            )
            .roundToInt()
            .coerceIn(0, maxOf(0, (viewportHeightPx - fountainSizePx).roundToInt()))

        Image(
            painter = painterResource(id = R.drawable.fountain),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .size(fountainSize)
        )
    }
}

@Composable
private fun PlazaPetActor(
    participant: PlazaParticipant,
    index: Int,
    authoritativePosition: PlazaPosition?,
    authoritativeMovement: PlazaMovementCommand?,
    serverNowMillis: Long,
    canWander: Boolean,
    isInteractionLocked: Boolean,
    isInteractionAnimating: Boolean,
    interactionIdleAnimation: CharacterAnimation?,
    interactionText: String?,
    interactionBubbleColor: Color?,
    interactionBubbleTextColor: Color?,
    chatBubbleText: String?,
    chatBubbleColor: Color?,
    chatBubbleTextColor: Color?,
    movementTargetPosition: PlazaPosition?,
    facingTargetPosition: PlazaPosition?,
    onPositionChange: (PlazaPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val actorWidth = 104.dp
        val actorHeight = 150.dp
        val actorWidthPx = with(density) { actorWidth.toPx() }
        val actorHeightPx = with(density) { actorHeight.toPx() }
        var targetPosition by remember(participant.userId) {
            mutableStateOf(participant.position ?: initialPlazaPetPosition(index, participant.userId))
        }
        val moveDurationMillis = authoritativeMovement
            ?.remainingDurationMillis(serverNowMillis)
            ?: PLAZA_PET_MOVE_DURATION_MS
        val animatedX by animateFloatAsState(
            targetValue = targetPosition.x,
            animationSpec = tween(
                durationMillis = moveDurationMillis,
                easing = FastOutSlowInEasing
            ),
            label = "PlazaPetX"
        )
        val animatedY by animateFloatAsState(
            targetValue = targetPosition.y,
            animationSpec = tween(
                durationMillis = moveDurationMillis,
                easing = FastOutSlowInEasing
            ),
            label = "PlazaPetY"
        )
        var lockedAnimation by remember(participant.userId) {
            mutableStateOf(CharacterAnimation.South)
        }
        val animatedPosition = PlazaPosition(animatedX, animatedY)
        val serverDrivenPosition = authoritativePosition?.takeIf { !canWander }
        val renderedPosition = serverDrivenPosition ?: animatedPosition
        val serverDrivenAnimation = authoritativeMovement
            ?.takeIf { !canWander }
            ?.let { movement ->
                resolvePlazaCharacterAnimation(
                    currentPosition = movement.from,
                    targetPosition = movement.to,
                    fallbackAnimation = lockedAnimation
                )
            }
        val isMoving = if (!canWander) {
            authoritativeMovement?.isActiveAt(serverNowMillis) == true || isInteractionAnimating
        } else {
            abs(animatedX - targetPosition.x) > PLAZA_PET_DIRECTION_EPSILON ||
                abs(animatedY - targetPosition.y) > PLAZA_PET_DIRECTION_EPSILON
        }

        LaunchedEffect(targetPosition) {
            onPositionChange(targetPosition)
            lockedAnimation = resolvePlazaCharacterAnimation(
                currentPosition = animatedPosition,
                targetPosition = targetPosition,
                fallbackAnimation = lockedAnimation
            )
        }

        LaunchedEffect(isInteractionLocked, facingTargetPosition) {
            if (isInteractionLocked && facingTargetPosition != null) {
                lockedAnimation = resolvePlazaCharacterAnimation(
                    currentPosition = animatedPosition,
                    targetPosition = facingTargetPosition,
                    fallbackAnimation = lockedAnimation
                )
            }
        }

        LaunchedEffect(isInteractionLocked, movementTargetPosition) {
            if (isInteractionLocked && movementTargetPosition != null) {
                targetPosition = movementTargetPosition
            }
        }

        LaunchedEffect(authoritativePosition, authoritativeMovement, isInteractionLocked) {
            if (!isInteractionLocked) {
                val movement = authoritativeMovement
                if (movement != null) {
                    val startDelayMillis = movement.delayUntilStartMillis(serverNowMillis)
                    if (startDelayMillis > 0L) {
                        delay(startDelayMillis)
                    }
                    targetPosition = movement.to
                } else {
                    targetPosition = authoritativePosition ?: targetPosition
                }
            }
        }

        LaunchedEffect(participant.userId, canWander, isInteractionLocked) {
            if (!canWander || isInteractionLocked) return@LaunchedEffect

            val random = Random(participant.userId.hashCode())
            var currentPosition = targetPosition
            delay(index * 280L)
            while (isActive) {
                val idleDelay = random.nextLong(
                    from = PLAZA_WANDER_MIN_DELAY_MS,
                    until = PLAZA_WANDER_MAX_DELAY_MS + 1L
                )
                delay(idleDelay)
                currentPosition = nextPlazaPetPosition(currentPosition, random)
                targetPosition = currentPosition
            }
        }

        val offsetX = (renderedPosition.x * viewportWidthPx - actorWidthPx * 0.5f)
            .roundToInt()
            .coerceIn(0, maxOf(0, (viewportWidthPx - actorWidthPx).roundToInt()))
        val offsetY = (renderedPosition.y * viewportHeightPx - actorHeightPx * 0.66f)
            .roundToInt()
            .coerceIn(0, maxOf(0, (viewportHeightPx - actorHeightPx).roundToInt()))
        val spriteAnimation = if (!isMoving && interactionIdleAnimation != null) {
            interactionIdleAnimation
        } else {
            serverDrivenAnimation ?: lockedAnimation
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .size(width = actorWidth, height = actorHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 56.dp, height = 16.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 92.dp)
            ) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.18f),
                    size = size
                )
            }
            AnimatedCharacterSprite(
                animation = spriteAnimation,
                appearance = participant.pet.appearance,
                equippedItemIds = participant.pet.equippedItemIds,
                isEgg = participant.pet.status.isEgg,
                isPlaying = isMoving || isInteractionAnimating,
                contentDescription = participant.pet.name,
                modifier = Modifier
                    .offset(y = 24.dp)
                    .size(88.dp)
            )
            (chatBubbleText ?: interactionText)?.let { text ->
                PlazaPetSpeechBubble(
                    text = text,
                    containerColor = if (chatBubbleText != null) {
                        chatBubbleColor ?: Color.White.copy(alpha = 0.95f)
                    } else {
                        interactionBubbleColor ?: Color.White.copy(alpha = 0.95f)
                    },
                    contentColor = if (chatBubbleText != null) {
                        chatBubbleTextColor ?: Color(0xFF3A3328)
                    } else {
                        interactionBubbleTextColor ?: Color(0xFF3A3328)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            PlazaPetNameBadge(
                participant = participant,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PlazaPetSpeechBubble(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        shadowElevation = 3.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlazaPetNameBadge(
    participant: PlazaParticipant,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (participant.isMe) {
            Color(0xFFFFF1B8)
        } else {
            Color.White.copy(alpha = 0.9f)
        },
        shadowElevation = 2.dp
    ) {
        Text(
            text = if (participant.isMe) "${participant.nickname} · 나" else participant.nickname,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2F352C),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun initialPlazaPetPosition(
    index: Int,
    userId: String
): PlazaPosition {
    val basePositions = listOf(
        PlazaPosition(0.30f, 0.64f),
        PlazaPosition(0.70f, 0.64f),
        PlazaPosition(0.44f, 0.78f),
        PlazaPosition(0.58f, 0.78f)
    )
    val random = Random(userId.hashCode() + index * 17)
    val base = basePositions[index % basePositions.size]
    return PlazaPosition(
        x = (base.x + random.nextSignedFloat(0.045f)).coerceIn(0.16f, 0.84f),
        y = (base.y + random.nextSignedFloat(0.04f)).coerceIn(0.48f, 0.86f)
    )
}

private fun nextPlazaPetPosition(
    currentPosition: PlazaPosition,
    random: Random
): PlazaPosition {
    repeat(6) {
        val next = PlazaPosition(
            x = (currentPosition.x + random.nextSignedFloat(0.20f))
                .coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
            y = (currentPosition.y + random.nextSignedFloat(0.14f))
                .coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
        )
        if (abs(next.x - currentPosition.x) + abs(next.y - currentPosition.y) >= 0.08f) {
            return next
        }
    }
    return currentPosition
}

private fun Random.nextSignedFloat(radius: Float): Float {
    return (nextFloat() * 2f - 1f) * radius
}

private fun PlazaServerTime?.serverNowMillis(
    clientNowMillis: Long = System.currentTimeMillis()
): Long {
    return this?.currentServerNowMillis(clientNowMillis) ?: clientNowMillis
}

private fun PlazaParticipant.authoritativePositionAt(nowMillis: Long): PlazaPosition? {
    return movement?.positionAt(nowMillis) ?: position
}

private fun PlazaMovementCommand.positionAt(nowMillis: Long): PlazaPosition {
    val progress = if (durationMillis <= 0L) {
        1f
    } else {
        ((nowMillis - startedAtMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
    }
    return PlazaPosition(
        x = from.x + (to.x - from.x) * progress,
        y = from.y + (to.y - from.y) * progress
    )
}

private fun PlazaMovementCommand.remainingDurationMillis(nowMillis: Long): Int {
    val elapsedMillis = (nowMillis - startedAtMillis).coerceAtLeast(0L)
    val remainingMillis = (durationMillis - elapsedMillis).coerceAtLeast(0L)
    return remainingMillis.coerceIn(250L, 6_000L).toInt()
}

private fun PlazaMovementCommand.delayUntilStartMillis(nowMillis: Long): Long {
    return (startedAtMillis - nowMillis).coerceAtLeast(0L)
}

private fun PlazaMovementCommand.isActiveAt(nowMillis: Long): Boolean {
    return startedAtMillis <= nowMillis && nowMillis < startedAtMillis + durationMillis
}

private fun PlazaInteractionEvent.isActiveAt(nowMillis: Long): Boolean {
    return startedAtMillis <= nowMillis && nowMillis <= startedAtMillis + durationMillis
}

private fun String.toPlazaChatBubbleText(): String {
    val compactText = trim().replace(Regex("\\s+"), " ")
    if (compactText.length <= PLAZA_CHAT_BUBBLE_MAX_LENGTH) return compactText
    return compactText.take(PLAZA_CHAT_BUBBLE_MAX_LENGTH - 3) + "..."
}

private fun closestPlazaParticipantPair(
    participants: List<PlazaParticipant>,
    positions: Map<String, PlazaPosition>,
    maxDistance: Float?
): Pair<PlazaParticipant, PlazaParticipant>? {
    var closestPair: Pair<PlazaParticipant, PlazaParticipant>? = null
    var closestDistanceSquared = Float.MAX_VALUE
    val maxDistanceSquared = maxDistance?.let { it * it }

    for (firstIndex in participants.indices) {
        for (secondIndex in firstIndex + 1 until participants.size) {
            val first = participants[firstIndex]
            val second = participants[secondIndex]
            val firstPosition = positions[first.userId] ?: continue
            val secondPosition = positions[second.userId] ?: continue
            val distanceSquared = firstPosition.distanceSquaredTo(secondPosition)

            if (maxDistanceSquared != null && distanceSquared > maxDistanceSquared) {
                continue
            }
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared
                closestPair = first to second
            }
        }
    }

    return closestPair
}

private fun createPlazaInteraction(
    plazaId: String,
    first: PlazaParticipant,
    second: PlazaParticipant,
    positions: Map<String, PlazaPosition>,
    startedAtMillis: Long
): PlazaInteractionEvent {
    val type = chooseLocalInteractionType(
        first = first,
        second = second,
        positions = positions,
        now = startedAtMillis
    )
    val phrase = phraseForInteractionType(type, startedAtMillis)
    val interaction = PlazaInteractionEvent(
        id = "local-interaction-${type.name.lowercase()}-${first.userId}-${second.userId}-$startedAtMillis",
        plazaId = plazaId,
        type = type,
        actorUserId = first.userId,
        targetUserId = second.userId,
        textByUserId = mapOf(
            first.userId to phrase.first,
            second.userId to phrase.second
        ),
        startedAtMillis = startedAtMillis,
        durationMillis = durationForInteractionType(type)
    )
    return interaction.copy(
        movementTargetByUserId = interaction.movementTargetsForInteraction(positions)
    )
}

private fun chooseLocalInteractionType(
    first: PlazaParticipant,
    second: PlazaParticipant,
    positions: Map<String, PlazaPosition>,
    now: Long
): PlazaInteractionType {
    val firstPosition = positions[first.userId]
    val secondPosition = positions[second.userId]
    val isVeryClose = firstPosition != null &&
        secondPosition != null &&
        firstPosition.distanceSquaredTo(secondPosition) <= 0.055f
    val options = if (isVeryClose) {
        listOf(
            PlazaInteractionType.GREET,
            PlazaInteractionType.PLAY,
            PlazaInteractionType.REST,
            PlazaInteractionType.FOLLOW
        )
    } else {
        listOf(
            PlazaInteractionType.GREET,
            PlazaInteractionType.FOLLOW
        )
    }
    val seed = now + first.userId.hashCode() * 31L + second.userId.hashCode()
    return options[seed.mod(options.size)]
}

private fun phraseForInteractionType(
    type: PlazaInteractionType,
    now: Long
): Pair<String, String> {
    val phrases = when (type) {
        PlazaInteractionType.GREET -> listOf(
            "안녕!" to "반가워!",
            "여기 있었구나!" to "응!",
            "좋은 하루야!" to "너도!"
        )

        PlazaInteractionType.PLAY -> listOf(
            "같이 놀자!" to "좋아!",
            "뛰어볼까?" to "가자!",
            "장난칠래?" to "응!"
        )

        PlazaInteractionType.REST -> listOf(
            "잠깐 쉬자" to "그래!",
            "여기 편하다" to "좋다",
            "숨 좀 돌리자" to "응"
        )

        PlazaInteractionType.FOLLOW -> listOf(
            "따라와!" to "갈게!",
            "이쪽이야!" to "알겠어!",
            "같이 가자!" to "좋아!"
        )
    }
    return phrases[now.mod(phrases.size)]
}

private fun durationForInteractionType(type: PlazaInteractionType): Long {
    return when (type) {
        PlazaInteractionType.GREET -> 2_200L
        PlazaInteractionType.PLAY -> 2_900L
        PlazaInteractionType.REST -> 2_800L
        PlazaInteractionType.FOLLOW -> 3_100L
    }
}

private fun PlazaInteractionEvent.toPresentation(
    positions: Map<String, PlazaPosition>
): PlazaInteractionPresentation {
    val movementTargets = movementTargetsForInteraction(positions)
    val serverAnimations = animationByUserId.toCharacterAnimations()
    return PlazaInteractionPresentation(
        event = this,
        movementTargetByUserId = movementTargets,
        facingTargetByUserId = facingTargetByUserId.ifEmpty {
            facingTargetsForInteraction(
                positions = positions,
                movementTargets = movementTargets
            )
        },
        lockedWanderingUserIds = lockedWanderingUsersForInteraction(),
        animatedSpriteUserIds = animatedSpriteUsersForInteraction() + serverAnimations.keys,
        idleAnimationByUserId = idleAnimationsForInteraction() + serverAnimations,
        bubbleColor = bubbleColorForInteractionType(type),
        bubbleTextColor = bubbleTextColorForInteractionType(type)
    )
}

private fun PlazaInteractionEvent.movementTargetsForInteraction(
    positions: Map<String, PlazaPosition>
): Map<String, PlazaPosition> {
    if (movementTargetByUserId.isNotEmpty()) return movementTargetByUserId

    return when (type) {
        PlazaInteractionType.GREET -> greetMovementTargets(positions)
        PlazaInteractionType.PLAY -> playMovementTargets(positions)
        PlazaInteractionType.REST -> restMovementTargets(positions)
        PlazaInteractionType.FOLLOW -> followMovementTargets(positions)
    }
}

private fun Map<String, String>.toCharacterAnimations(): Map<String, CharacterAnimation> {
    return mapNotNull { (userId, animationName) ->
        CharacterAnimation.values()
            .firstOrNull { it.name.equals(animationName, ignoreCase = true) }
            ?.let { animation -> userId to animation }
    }.toMap()
}

private fun PlazaInteractionEvent.greetMovementTargets(
    positions: Map<String, PlazaPosition>
): Map<String, PlazaPosition> {
    val firstPosition = positions[actorUserId] ?: return emptyMap()
    val secondUserId = targetUserId ?: return emptyMap()
    val secondPosition = positions[secondUserId] ?: return emptyMap()
    val center = firstPosition.midpointTo(secondPosition)
    val direction = greetDirectionFrom(
        firstPosition = firstPosition,
        secondPosition = secondPosition,
        seed = id.hashCode()
    )
    val firstTarget = center.greetTarget(
        direction = direction,
        side = -1f
    )
    val secondTarget = center.greetTarget(
        direction = direction,
        side = 1f
    )

    return mapOf(
        actorUserId to firstTarget,
        secondUserId to secondTarget
    )
}

private fun PlazaInteractionEvent.playMovementTargets(
    positions: Map<String, PlazaPosition>
): Map<String, PlazaPosition> {
    val firstPosition = positions[actorUserId] ?: return emptyMap()
    val secondUserId = targetUserId ?: return emptyMap()
    val secondPosition = positions[secondUserId] ?: return emptyMap()
    val center = firstPosition.midpointTo(secondPosition)
    val direction = playSideDirection(seed = id.hashCode())
    val firstTarget = center.playTarget(
        direction = direction,
        side = 1f
    )
    val secondTarget = center.playTarget(
        direction = direction,
        side = -1f
    )

    return mapOf(
        actorUserId to firstTarget,
        secondUserId to secondTarget
    )
}

private fun PlazaInteractionEvent.restMovementTargets(
    positions: Map<String, PlazaPosition>
): Map<String, PlazaPosition> {
    val firstPosition = positions[actorUserId] ?: return emptyMap()
    val secondUserId = targetUserId ?: return emptyMap()
    val secondPosition = positions[secondUserId] ?: return emptyMap()
    val center = firstPosition
        .midpointTo(secondPosition)
        .restAnchor()
    val direction = restSideDirection(seed = id.hashCode())
    val firstTarget = center.restTarget(
        direction = direction,
        side = 1f
    )
    val secondTarget = center.restTarget(
        direction = direction,
        side = -1f
    )

    return mapOf(
        actorUserId to firstTarget,
        secondUserId to secondTarget
    )
}

private fun PlazaInteractionEvent.followMovementTargets(
    positions: Map<String, PlazaPosition>
): Map<String, PlazaPosition> {
    val leaderPosition = positions[actorUserId] ?: return emptyMap()
    val followerUserId = targetUserId ?: return emptyMap()
    val followerPosition = positions[followerUserId] ?: return emptyMap()
    val direction = followDirectionFrom(
        leaderPosition = leaderPosition,
        followerPosition = followerPosition,
        seed = id.hashCode()
    )
    val leaderTarget = leaderPosition.followLeaderTarget(direction)
    val followerTarget = leaderTarget.followTrailTarget(
        direction = direction,
        seed = id.hashCode()
    )

    return mapOf(
        actorUserId to leaderTarget,
        followerUserId to followerTarget
    )
}

private fun PlazaInteractionEvent.facingTargetsForInteraction(
    positions: Map<String, PlazaPosition>,
    movementTargets: Map<String, PlazaPosition>
): Map<String, PlazaPosition> {
    val secondUserId = targetUserId ?: return emptyMap()
    return when (type) {
        PlazaInteractionType.GREET -> {
            val firstTarget = movementTargets[actorUserId]
                ?: positions[actorUserId]
                ?: return emptyMap()
            val secondTarget = movementTargets[secondUserId]
                ?: positions[secondUserId]
                ?: return emptyMap()
            mapOf(
                actorUserId to secondTarget,
                secondUserId to firstTarget
            )
        }

        else -> emptyMap()
    }
}

private fun PlazaInteractionEvent.animatedSpriteUsersForInteraction(): Set<String> {
    val userIds = setOfNotNull(actorUserId, targetUserId)
    return when (type) {
        PlazaInteractionType.GREET,
        PlazaInteractionType.PLAY,
        PlazaInteractionType.FOLLOW -> userIds

        PlazaInteractionType.REST -> emptySet()
    }
}

private fun PlazaInteractionEvent.idleAnimationsForInteraction(): Map<String, CharacterAnimation> {
    val userIds = setOfNotNull(actorUserId, targetUserId)
    return when (type) {
        PlazaInteractionType.REST -> userIds.associateWith { CharacterAnimation.South }
        else -> emptyMap()
    }
}

private fun PlazaInteractionEvent.lockedWanderingUsersForInteraction(): Set<String> {
    val userIds = setOfNotNull(actorUserId, targetUserId)
    return when (type) {
        PlazaInteractionType.GREET,
        PlazaInteractionType.PLAY,
        PlazaInteractionType.REST,
        PlazaInteractionType.FOLLOW -> userIds
    }
}

private fun bubbleColorForInteractionType(type: PlazaInteractionType): Color {
    return when (type) {
        PlazaInteractionType.GREET -> Color.White.copy(alpha = 0.95f)
        PlazaInteractionType.PLAY -> Color(0xFFFFE58F)
        PlazaInteractionType.REST -> Color(0xFFDDF4C7)
        PlazaInteractionType.FOLLOW -> Color(0xFFD7ECFF)
    }
}

private fun bubbleTextColorForInteractionType(type: PlazaInteractionType): Color {
    return when (type) {
        PlazaInteractionType.GREET -> Color(0xFF3A3328)

        PlazaInteractionType.PLAY -> Color(0xFF5D4200)
        PlazaInteractionType.REST -> Color(0xFF264B21)
        PlazaInteractionType.FOLLOW -> Color(0xFF1E4360)
    }
}

private fun greetDirectionFrom(
    firstPosition: PlazaPosition,
    secondPosition: PlazaPosition,
    seed: Int
): Pair<Float, Float> {
    val deltaX = secondPosition.x - firstPosition.x
    val deltaY = secondPosition.y - firstPosition.y
    val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

    if (distance < PLAZA_FOLLOW_MIN_VECTOR) {
        return seededGreetDirection(seed)
    }

    return deltaX / distance to deltaY / distance
}

private fun seededGreetDirection(seed: Int): Pair<Float, Float> {
    val direction = when (seed.mod(4)) {
        0 -> 1f to 0.15f
        1 -> -1f to 0.15f
        2 -> 0.55f to 1f
        else -> -0.55f to 1f
    }
    val distance = sqrt(direction.first * direction.first + direction.second * direction.second)
    return direction.first / distance to direction.second / distance
}

private fun PlazaPosition.greetTarget(
    direction: Pair<Float, Float>,
    side: Float
): PlazaPosition {
    return PlazaPosition(
        x = (x + direction.first * PLAZA_GREET_OFFSET_X * side)
            .coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = (y + direction.second * PLAZA_GREET_OFFSET_Y * side)
            .coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun playSideDirection(seed: Int): Pair<Float, Float> {
    val direction = when (seed.mod(4)) {
        0 -> 1f to 0.2f
        1 -> -1f to 0.2f
        2 -> 0.35f to 1f
        else -> -0.35f to 1f
    }
    val distance = sqrt(direction.first * direction.first + direction.second * direction.second)
    return direction.first / distance to direction.second / distance
}

private fun PlazaPosition.midpointTo(other: PlazaPosition): PlazaPosition {
    return PlazaPosition(
        x = ((x + other.x) * 0.5f).coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = ((y + other.y) * 0.5f).coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun PlazaPosition.playTarget(
    direction: Pair<Float, Float>,
    side: Float
): PlazaPosition {
    val offsetX = direction.first * PLAZA_PLAY_OFFSET_X * side
    val offsetY = direction.second * PLAZA_PLAY_OFFSET_Y * side
    return PlazaPosition(
        x = (x + offsetX).coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = (y + offsetY).coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun restSideDirection(seed: Int): Pair<Float, Float> {
    val direction = when (seed.mod(4)) {
        0 -> 1f to 0f
        1 -> -1f to 0f
        2 -> 0.72f to 0.18f
        else -> -0.72f to 0.18f
    }
    val distance = sqrt(direction.first * direction.first + direction.second * direction.second)
    return direction.first / distance to direction.second / distance
}

private fun PlazaPosition.restAnchor(): PlazaPosition {
    return PlazaPosition(
        x = x.coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = (y + 0.025f).coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun PlazaPosition.restTarget(
    direction: Pair<Float, Float>,
    side: Float
): PlazaPosition {
    val offsetX = direction.first * PLAZA_REST_OFFSET_X * side
    val offsetY = direction.second * PLAZA_REST_OFFSET_Y * side
    return PlazaPosition(
        x = (x + offsetX).coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = (y + offsetY).coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun followDirectionFrom(
    leaderPosition: PlazaPosition,
    followerPosition: PlazaPosition,
    seed: Int
): Pair<Float, Float> {
    val deltaX = leaderPosition.x - followerPosition.x
    val deltaY = leaderPosition.y - followerPosition.y
    val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

    if (distance < PLAZA_FOLLOW_MIN_VECTOR) {
        return seededFollowDirection(seed)
    }

    return deltaX / distance to deltaY / distance
}

private fun seededFollowDirection(seed: Int): Pair<Float, Float> {
    val direction = when (seed.mod(4)) {
        0 -> 1f to 0.35f
        1 -> -1f to 0.35f
        2 -> 0.7f to -1f
        else -> -0.7f to -1f
    }
    val distance = sqrt(direction.first * direction.first + direction.second * direction.second)
    return direction.first / distance to direction.second / distance
}

private fun PlazaPosition.followLeaderTarget(direction: Pair<Float, Float>): PlazaPosition {
    return PlazaPosition(
        x = (x + direction.first * PLAZA_FOLLOW_LEADER_STEP_X)
            .coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = (y + direction.second * PLAZA_FOLLOW_LEADER_STEP_Y)
            .coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun PlazaPosition.followTrailTarget(
    direction: Pair<Float, Float>,
    seed: Int
): PlazaPosition {
    val side = if (seed.mod(2) == 0) 1f else -1f
    val sideOffsetX = -direction.second * side * 0.025f
    val sideOffsetY = direction.first * side * 0.018f

    return PlazaPosition(
        x = (x - direction.first * PLAZA_FOLLOW_TRAIL_STEP_X + sideOffsetX)
            .coerceIn(PLAZA_PET_MIN_X, PLAZA_PET_MAX_X),
        y = (y - direction.second * PLAZA_FOLLOW_TRAIL_STEP_Y + sideOffsetY)
            .coerceIn(PLAZA_PET_MIN_Y, PLAZA_PET_MAX_Y)
    )
}

private fun PlazaPosition.distanceSquaredTo(other: PlazaPosition): Float {
    val deltaX = x - other.x
    val deltaY = y - other.y
    return deltaX * deltaX + deltaY * deltaY
}

private fun PlazaInteractionEvent.involves(userId: String): Boolean {
    return actorUserId == userId || targetUserId == userId || textByUserId.containsKey(userId)
}

private fun PlazaInteractionEvent.partnerOf(userId: String): String? {
    return when (userId) {
        actorUserId -> targetUserId
        targetUserId -> actorUserId
        else -> null
    }
}

private fun PlazaInteractionEvent.textFor(userId: String): String? {
    return textByUserId[userId]
}

private fun resolvePlazaCharacterAnimation(
    currentPosition: PlazaPosition,
    targetPosition: PlazaPosition,
    fallbackAnimation: CharacterAnimation
): CharacterAnimation {
    val deltaX = targetPosition.x - currentPosition.x
    val deltaY = targetPosition.y - currentPosition.y
    val absDeltaX = abs(deltaX)
    val absDeltaY = abs(deltaY)
    val dominantDelta = maxOf(absDeltaX, absDeltaY)

    if (dominantDelta < PLAZA_PET_DIRECTION_EPSILON) {
        return fallbackAnimation
    }

    return when {
        absDeltaX >= absDeltaY * PLAZA_PET_AXIS_DOMINANCE_RATIO -> {
            if (deltaX >= 0f) CharacterAnimation.East else CharacterAnimation.West
        }

        absDeltaY >= absDeltaX * PLAZA_PET_AXIS_DOMINANCE_RATIO -> {
            if (deltaY < 0f) CharacterAnimation.North else CharacterAnimation.South
        }

        deltaX >= 0f && deltaY < 0f -> CharacterAnimation.NorthEast
        deltaX < 0f && deltaY < 0f -> CharacterAnimation.NorthWest
        deltaX >= 0f && deltaY >= 0f -> CharacterAnimation.SouthEast
        deltaX < 0f && deltaY >= 0f -> CharacterAnimation.SouthWest
        else -> fallbackAnimation
    }
}

@Composable
private fun PlazaChatPanel(
    messages: List<PlazaChatMessage>,
    currentUserId: String?,
    messageInput: String,
    isSendingMessage: Boolean,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleMessages = remember(messages) { messages.takeLast(40) }
    val listState = rememberLazyListState()

    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "채팅",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "첫 메시지를 남겨보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleMessages, key = { it.id }) { message ->
                        PlazaMessageBubble(
                            message = message,
                            isMine = message.senderUserId == currentUserId
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = onMessageInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isSendingMessage,
                    singleLine = true,
                    placeholder = { Text("메시지") },
                    supportingText = {
                        Text(
                            text = "${messageInput.length}/$PLAZA_MESSAGE_MAX_LENGTH",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSendMessage,
                    enabled = !isSendingMessage && messageInput.isNotBlank()
                ) {
                    Text(if (isSendingMessage) "전송 중" else "보내기")
                }
            }
        }
    }
}

@Composable
private fun PlazaMessageBubble(
    message: PlazaChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.senderNickname,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = formatPlazaMessageTime(message.sentAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val plazaMessageTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm", Locale.KOREA)

private fun formatPlazaMessageTime(sentAtMillis: Long): String {
    return Instant
        .ofEpochMilli(sentAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(plazaMessageTimeFormatter)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlazaEntryPreview() {
    LupaPJTheme {
        PlazaScreen(
            activePlaza = null,
            codeInput = "",
            messageInput = "",
            isJoining = false,
            isSendingMessage = false,
            feedbackMessage = null,
            onRandomJoin = {},
            onCodeInputChange = {},
            onCodeJoin = {},
            onMessageInputChange = {},
            onSendMessage = {},
            onLeavePlaza = {},
            onBackHome = {},
            onFeedbackConsumed = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlazaRoomPreview() {
    val now = 1_000_000L
    val me = PlazaParticipant(
        userId = "user_me",
        nickname = "루파",
        pet = PlazaPetSnapshot(name = "루파"),
        joinedAtMillis = now,
        isMe = true
    )
    val friend = PlazaParticipant(
        userId = "user_mina",
        nickname = "미나",
        pet = PlazaPetSnapshot(name = "몽글"),
        joinedAtMillis = now
    )

    LupaPJTheme {
        PlazaScreen(
            activePlaza = PlazaRoom(
                plazaId = "preview-plaza",
                plazaCode = PlazaCode.fromInput("PZ-4821") ?: error("Invalid preview code"),
                participants = listOf(friend, me),
                messages = listOf(
                    PlazaChatMessage(
                        id = "preview-message-1",
                        plazaId = "preview-plaza",
                        senderUserId = friend.userId,
                        senderNickname = friend.nickname,
                        text = "어서 와!",
                        sentAtMillis = now
                    )
                ),
                joinedAtMillis = now
            ),
            codeInput = "",
            messageInput = "",
            isJoining = false,
            isSendingMessage = false,
            feedbackMessage = null,
            onRandomJoin = {},
            onCodeInputChange = {},
            onCodeJoin = {},
            onMessageInputChange = {},
            onSendMessage = {},
            onLeavePlaza = {},
            onBackHome = {},
            onFeedbackConsumed = {}
        )
    }
}
