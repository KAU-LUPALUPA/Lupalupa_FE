package com.example.lupapj.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxSheet(
    friendRequests: List<FriendRequest>,
    homeInvitations: List<FriendHomeInvitation>,
    onDismiss: () -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onRejectFriendRequest: (String) -> Unit,
    onAcceptHomeInvitation: (String) -> Unit,
    onRejectHomeInvitation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "우편함",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "친구 신청과 집 초대를 확인할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (friendRequests.isEmpty() && homeInvitations.isEmpty()) {
                EmptyMailboxContent()
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (friendRequests.isNotEmpty()) {
                        item(key = "friend_requests_header") {
                            MailboxSectionTitle("친구 신청")
                        }
                        items(friendRequests, key = { "friend-request-${it.id}" }) { request ->
                            MailboxItemRow(
                                user = request.fromUser,
                                label = "친구가 되고 싶어해요.",
                                actions = {
                                    OutlinedButton(onClick = { onRejectFriendRequest(request.id) }) {
                                        Text("거절")
                                    }
                                    Button(onClick = { onAcceptFriendRequest(request.id) }) {
                                        Text("수락")
                                    }
                                }
                            )
                        }
                    }

                    if (homeInvitations.isNotEmpty()) {
                        item(key = "home_invitations_header") {
                            MailboxSectionTitle("집 초대")
                        }
                        items(homeInvitations, key = { "home-invitation-${it.id}" }) { invitation ->
                            MailboxItemRow(
                                user = invitation.fromUser,
                                label = invitation.message ?: "친구의 집에 초대받았어요.",
                                actions = {
                                    TextButton(onClick = onDismiss) {
                                        Text("나중에")
                                    }
                                    OutlinedButton(onClick = { onRejectHomeInvitation(invitation.id) }) {
                                        Text("거절")
                                    }
                                    Button(onClick = { onAcceptHomeInvitation(invitation.id) }) {
                                        Text("방문")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingMailboxButton(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(68.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.Center)
                .size(58.dp),
            shape = CircleShape,
            color = Color(0xFFFFF7E8),
            shadowElevation = 7.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                MailEnvelopeIcon(
                    modifier = Modifier.size(33.dp),
                    color = Color(0xFF6B4A2E)
                )
            }
        }

        if (itemCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = itemCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMailboxContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "새 우편이 없습니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MailboxSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MailboxItemRow(
    user: FriendUser,
    label: String,
    actions: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.nickname.firstOrNull()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.nickname,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.End
                )
            ) {
                actions()
            }
        }
    }
}

@Composable
private fun MailEnvelopeIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.075f
        val envelopeTop = size.height * 0.22f
        val envelopeLeft = size.width * 0.08f
        val envelopeSize = Size(
            width = size.width * 0.84f,
            height = size.height * 0.58f
        )
        val topLeft = Offset(envelopeLeft, envelopeTop)
        val topRight = Offset(envelopeLeft + envelopeSize.width, envelopeTop)
        val bottomLeft = Offset(envelopeLeft, envelopeTop + envelopeSize.height)
        val bottomRight = Offset(
            envelopeLeft + envelopeSize.width,
            envelopeTop + envelopeSize.height
        )
        val center = Offset(size.width * 0.5f, size.height * 0.56f)

        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = envelopeSize,
            cornerRadius = CornerRadius(size.minDimension * 0.08f),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = color,
            start = topLeft,
            end = center,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = topRight,
            end = center,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = bottomLeft,
            end = center,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = bottomRight,
            end = center,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
