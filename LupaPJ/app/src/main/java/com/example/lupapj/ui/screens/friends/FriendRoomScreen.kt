package com.example.lupapj.ui.screens.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.friend.FRIEND_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.mock.DemoFriendUsers
import com.example.lupapj.data.mock.DemoScenes
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendMessageSender
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.ui.components.RoomViewport
import com.example.lupapj.ui.theme.LupaPJTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRoomScreen(
    friendHome: FriendHome?,
    isLoading: Boolean,
    messages: List<FriendMessage>,
    messageInput: String,
    isSendingMessage: Boolean,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onBackToFriendsClick: () -> Unit,
    onReturnHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ownerName = friendHome?.owner?.nickname

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (ownerName == null) "친구 집" else "${ownerName}의 집") },
                navigationIcon = {
                    TextButton(onClick = onBackToFriendsClick) {
                        Text("친구")
                    }
                },
                actions = {
                    TextButton(onClick = onReturnHomeClick) {
                        Text("내 방")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val room = friendHome?.room
            if (room == null) {
                Text(
                    text = if (isLoading) "불러오는 중..." else "방문할 수 없습니다.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                RoomViewport(
                    uiState = room.copy(
                        feedMode = false,
                        toyMode = false,
                        navBarVisible = false,
                        inventoryVisible = false,
                        isCameraMode = false,
                        cameraZoom = 1f
                    ),
                    onRoomObjectClick = {},
                    onFloorTap = {},
                    modifier = Modifier.fillMaxSize()
                )

                FriendRoomChatPanel(
                    ownerName = friendHome.owner.nickname,
                    messages = messages,
                    messageInput = messageInput,
                    isSendingMessage = isSendingMessage,
                    onMessageInputChange = onMessageInputChange,
                    onSendMessage = onSendMessage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .imePadding()
                )
            }
        }
    }
}

@Composable
private fun FriendRoomChatPanel(
    ownerName: String,
    messages: List<FriendMessage>,
    messageInput: String,
    isSendingMessage: Boolean,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = if (isExpanded) 284.dp else 96.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$ownerName 채팅",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isExpanded) {
                        Text(
                            text = messages.lastOrNull()?.text ?: "첫 메시지를 남겨보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "접기" else "펼치기")
                }
            }

            if (!isExpanded) {
                return@Column
            }

            val recentMessages = messages.takeLast(4)
            if (recentMessages.isEmpty()) {
                Text(
                    text = "첫 메시지를 남겨보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentMessages.forEach { message ->
                        MessageBubble(message = message)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = onMessageInputChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("메시지") },
                    supportingText = {
                        Text(
                            text = "${messageInput.length}/$FRIEND_MESSAGE_MAX_LENGTH",
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
private fun MessageBubble(
    message: FriendMessage,
    modifier: Modifier = Modifier
) {
    val isMine = message.sender == FriendMessageSender.ME
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
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
                text = formatMessageTime(message.sentAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val messageTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm", Locale.KOREA)

private fun formatMessageTime(sentAtMillis: Long): String {
    return Instant
        .ofEpochMilli(sentAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(messageTimeFormatter)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FriendRoomScreenPreview() {
    LupaPJTheme {
        FriendRoomScreen(
            friendHome = FriendHome(
                owner = DemoFriendUsers.alreadyFriend,
                room = initialRoomUiState(DemoScenes.mainRoom),
                visitedAtMillis = 1_000L
            ),
            isLoading = false,
            messages = listOf(
                FriendMessage(
                    id = "preview-1",
                    friendUserId = DemoFriendUsers.alreadyFriend.userId,
                    senderUserId = DemoFriendUsers.alreadyFriend.userId,
                    sender = FriendMessageSender.FRIEND,
                    text = "어서 와! 우리 집 구경해.",
                    sentAtMillis = 1_000L
                ),
                FriendMessage(
                    id = "preview-2",
                    friendUserId = DemoFriendUsers.alreadyFriend.userId,
                    senderUserId = DemoFriendUsers.me.userId,
                    sender = FriendMessageSender.ME,
                    text = "인테리어 귀엽다.",
                    sentAtMillis = 1_100L
                )
            ),
            messageInput = "",
            isSendingMessage = false,
            onMessageInputChange = {},
            onSendMessage = {},
            onBackToFriendsClick = {},
            onReturnHomeClick = {}
        )
    }
}
