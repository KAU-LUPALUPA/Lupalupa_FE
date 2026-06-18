package com.example.lupapj.ui.screens.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import com.example.lupapj.data.mock.DemoFriendUsers
import com.example.lupapj.data.model.friend.*
import com.example.lupapj.ui.theme.LupaPJTheme
import kotlinx.coroutines.launch

private enum class FriendTab(val title: String) {
    Friends("친구"),
    Received("받은 요청"),
    Sent("보낸 요청")
}

@Composable
fun FriendCodePanel(
    myProfile: FriendUser?,
    friendCodeInput: String,
    isSendingFriendRequest: Boolean,
    onFriendCodeChange: (String) -> Unit,
    onSendFriendRequest: () -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = myProfile?.nickname ?: "내 프로필",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = myProfile?.displayFriendCode ?: "코드 준비 중",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        myProfile?.displayFriendCode?.let(onCopyCode)
                    },
                    enabled = myProfile != null
                ) {
                    Text("복사")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = friendCodeInput,
                    onValueChange = onFriendCodeChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("친구 코드") },
                    placeholder = { Text("LUPA-5B0RI") }
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onSendFriendRequest,
                    enabled = !isSendingFriendRequest,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(if (isSendingFriendRequest) "전송 중" else "신청")
                }
            }
        }
    }
}

@Composable
fun UserRow(
    user: FriendUser,
    modifier: Modifier = Modifier,
    supportingText: String = user.displayFriendCode,
    trailingContent: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarInitial(nickname = user.nickname)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
            trailingContent()
        }
    }
}

@Composable
fun AvatarInitial(
    nickname: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nickname.firstOrNull()?.toString() ?: "?",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        FilterChip(
            selected = false,
            onClick = {},
            label = { Text(text) }
        )
    }
}

@Composable
fun FriendList(
    friends: List<FriendSummary>,
    pendingHomeInvitationFriendId: String?,
    onSendHomeInvitationClick: (FriendUser) -> Unit,
    onRemoveFriendClick: (FriendUser) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyState(text = "친구가 없습니다.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(friends, key = { it.user.userId }) { friend ->
            UserRow(
                user = friend.user,
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSendHomeInvitationClick(friend.user) },
                            enabled = pendingHomeInvitationFriendId == null,
                            modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 64.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(
                                text = if (pendingHomeInvitationFriendId == friend.user.userId) {
                                    "전송 중"
                                } else {
                                    "초대"
                                }
                            )
                        }
                        TextButton(onClick = { onRemoveFriendClick(friend.user) }) {
                            Text("삭제")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ReceivedRequestList(
    requests: List<FriendRequest>,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(text = "받은 요청이 없습니다.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            UserRow(
                user = request.fromUser,
                supportingText = "친구 요청",
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRejectRequest(request.id) }) {
                            Text("거절")
                        }
                        Button(onClick = { onAcceptRequest(request.id) }) {
                            Text("수락")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SentRequestList(
    requests: List<FriendRequest>,
    onCancelRequest: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(text = "보낸 요청이 없습니다.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            UserRow(
                user = request.toUser,
                supportingText = request.status.displayText,
                trailingContent = {
                    OutlinedButton(onClick = { onCancelRequest(request.id) }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    myProfile: FriendUser?,
    friends: List<FriendSummary>,
    receivedRequests: List<FriendRequest>,
    sentRequests: List<FriendRequest>,
    friendCodeInput: String,
    isSendingFriendRequest: Boolean,
    pendingHomeInvitationFriendId: String?,
    feedbackMessage: String?,
    onFriendCodeChange: (String) -> Unit,
    onSendFriendRequest: () -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
    onSendHomeInvitation: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onBackClick: () -> Unit,
    onFeedbackConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var friendPendingRemoval by remember { mutableStateOf<FriendUser?>(null) }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFeedbackConsumed()
        }
    }

    friendPendingRemoval?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendPendingRemoval = null },
            title = { Text("친구 삭제") },
            text = { Text("${friend.nickname}님을 친구 목록에서 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveFriend(friend.userId)
                        friendPendingRemoval = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { friendPendingRemoval = null }) {
                    Text("취소")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("친구") },
                    navigationIcon = {
                        TextButton(onClick = onBackClick) {
                            Text("뒤로")
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                color = Color(0xFFFFFBF0).copy(alpha = 0.75f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    FriendCodePanel(
                        myProfile = myProfile,
                        friendCodeInput = friendCodeInput,
                        isSendingFriendRequest = isSendingFriendRequest,
                        onFriendCodeChange = onFriendCodeChange,
                        onSendFriendRequest = onSendFriendRequest,
                        onCopyCode = { code ->
                            copyToClipboard(context, code)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("친구 코드를 복사했어요.")
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                        FriendTab.entries.forEachIndexed { index, tab ->
                            val count = when (tab) {
                                FriendTab.Friends -> friends.size
                                FriendTab.Received -> receivedRequests.size
                                FriendTab.Sent -> sentRequests.size
                            }
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text("${tab.title} $count") }
                            )
                        }
                    }

                    when (FriendTab.entries[selectedTabIndex]) {
                        FriendTab.Friends -> FriendList(
                            friends = friends,
                            pendingHomeInvitationFriendId = pendingHomeInvitationFriendId,
                            onSendHomeInvitationClick = { onSendHomeInvitation(it.userId) },
                            onRemoveFriendClick = { friendPendingRemoval = it }
                        )

                        FriendTab.Received -> ReceivedRequestList(
                            requests = receivedRequests,
                            onAcceptRequest = { requestId ->
                                selectedTabIndex = FriendTab.Friends.ordinal
                                onAcceptRequest(requestId)
                            },
                            onRejectRequest = onRejectRequest
                        )

                        FriendTab.Sent -> SentRequestList(
                            requests = sentRequests,
                            onCancelRequest = onCancelRequest
                        )
                    }
                }
            }
        }
    }
}

private val FriendRequestStatus.displayText: String
    get() = when (this) {
        FriendRequestStatus.PENDING -> "대기 중"
        FriendRequestStatus.ACCEPTED -> "수락됨"
        FriendRequestStatus.REJECTED -> "거절됨"
        FriendRequestStatus.CANCELED -> "취소됨"
    }

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(
        ClipData.newPlainText("friend_code", text)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun FriendScreenPreview() {
    val receivedRequest = FriendRequest(
        id = "preview-request",
        fromUser = DemoFriendUsers.incomingRequester,
        toUser = DemoFriendUsers.me,
        createdAtMillis = 1_000L
    )

    LupaPJTheme {
        FriendScreen(
            myProfile = DemoFriendUsers.me,
            friends = listOf(
                FriendSummary(
                    user = DemoFriendUsers.alreadyFriend,
                    friendsSinceMillis = 1_000L
                )
            ),
            receivedRequests = listOf(receivedRequest),
            sentRequests = emptyList(),
            friendCodeInput = FriendCode.display("LUPA5B0RI"),
            isSendingFriendRequest = false,
            pendingHomeInvitationFriendId = null,
            feedbackMessage = null,
            onFriendCodeChange = {},
            onSendFriendRequest = {},
            onAcceptRequest = {},
            onRejectRequest = {},
            onCancelRequest = {},
            onSendHomeInvitation = {},
            onRemoveFriend = {},
            onBackClick = {},
            onFeedbackConsumed = {}
        )
    }
}
