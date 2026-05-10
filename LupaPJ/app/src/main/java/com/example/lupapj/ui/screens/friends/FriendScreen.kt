package com.example.lupapj.ui.screens.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.mock.DemoFriendUsers
import com.example.lupapj.data.model.friend.FriendCode
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendHomeInvitationStatus
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendRequestStatus
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.ui.theme.LupaPJTheme
import kotlinx.coroutines.launch

private enum class FriendTab(val title: String) {
    Friends("친구"),
    HomeInvites("집 초대"),
    Received("받은 요청"),
    Sent("보낸 요청")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    myProfile: FriendUser?,
    friends: List<FriendSummary>,
    receivedRequests: List<FriendRequest>,
    sentRequests: List<FriendRequest>,
    receivedHomeInvitations: List<FriendHomeInvitation>,
    friendCodeInput: String,
    isSendingFriendRequest: Boolean,
    feedbackMessage: String?,
    onFriendCodeChange: (String) -> Unit,
    onSendFriendRequest: () -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
    onAcceptHomeInvitation: (String) -> Unit,
    onRejectHomeInvitation: (String) -> Unit,
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

    Scaffold(
        modifier = modifier,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
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
                        FriendTab.HomeInvites -> receivedHomeInvitations.size
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
                    onRemoveFriendClick = { friendPendingRemoval = it }
                )

                FriendTab.HomeInvites -> HomeInvitationList(
                    invitations = receivedHomeInvitations,
                    onAcceptHomeInvitation = onAcceptHomeInvitation,
                    onRejectHomeInvitation = onRejectHomeInvitation
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

@Composable
private fun FriendCodePanel(
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
private fun FriendList(
    friends: List<FriendSummary>,
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
                    TextButton(onClick = { onRemoveFriendClick(friend.user) }) {
                        Text("삭제")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeInvitationList(
    invitations: List<FriendHomeInvitation>,
    onAcceptHomeInvitation: (String) -> Unit,
    onRejectHomeInvitation: (String) -> Unit
) {
    if (invitations.isEmpty()) {
        EmptyState(text = "받은 집 초대가 없습니다.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(invitations, key = { it.id }) { invitation ->
            UserRow(
                user = invitation.fromUser,
                supportingText = invitation.message ?: invitation.status.displayText,
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRejectHomeInvitation(invitation.id) }) {
                            Text("거절")
                        }
                        Button(onClick = { onAcceptHomeInvitation(invitation.id) }) {
                            Text("수락")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ReceivedRequestList(
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
private fun SentRequestList(
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

@Composable
private fun UserRow(
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
private fun AvatarInitial(
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
private fun EmptyState(text: String) {
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

private val FriendRequestStatus.displayText: String
    get() = when (this) {
        FriendRequestStatus.PENDING -> "대기 중"
        FriendRequestStatus.ACCEPTED -> "수락됨"
        FriendRequestStatus.REJECTED -> "거절됨"
        FriendRequestStatus.CANCELED -> "취소됨"
    }

private val FriendHomeInvitationStatus.displayText: String
    get() = when (this) {
        FriendHomeInvitationStatus.PENDING -> "집 초대 대기 중"
        FriendHomeInvitationStatus.ACCEPTED -> "초대 수락됨"
        FriendHomeInvitationStatus.REJECTED -> "초대 거절됨"
        FriendHomeInvitationStatus.CANCELED -> "초대 취소됨"
        FriendHomeInvitationStatus.EXPIRED -> "만료됨"
    }

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(
        ClipData.newPlainText("friend_code", text)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FriendScreenPreview() {
    val receivedRequest = FriendRequest(
        id = "preview-request",
        fromUser = DemoFriendUsers.incomingRequester,
        toUser = DemoFriendUsers.me,
        createdAtMillis = 1_000L
    )
    val homeInvitation = FriendHomeInvitation(
        id = "preview-home-invitation",
        fromUser = DemoFriendUsers.alreadyFriend,
        toUser = DemoFriendUsers.me,
        message = "미나님의 집에 초대받았어요.",
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
            receivedHomeInvitations = listOf(homeInvitation),
            friendCodeInput = FriendCode.display("LUPA5B0RI"),
            isSendingFriendRequest = false,
            feedbackMessage = null,
            onFriendCodeChange = {},
            onSendFriendRequest = {},
            onAcceptRequest = {},
            onRejectRequest = {},
            onCancelRequest = {},
            onAcceptHomeInvitation = {},
            onRejectHomeInvitation = {},
            onRemoveFriend = {},
            onBackClick = {},
            onFeedbackConsumed = {}
        )
    }
}
