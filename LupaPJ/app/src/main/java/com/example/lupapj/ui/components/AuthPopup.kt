package com.example.lupapj.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.lupapj.data.remote.ServerConfig

private val KakaoYellow = Color(0xFFFEE500)
private val KakaoBlack = Color(0xFF191919)

@Composable
fun AuthPopup(
    isProcessingLogin: Boolean,
    isDevLoginEnabled: Boolean = false,
    onKakaoLoginClick: () -> Unit,
    onDevLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val devLoginModifier = if (isDevLoginEnabled) {
        Modifier.pointerInput(isProcessingLogin) {
            detectTapGestures(
                onLongPress = {
                    if (!isProcessingLogin) {
                        onDevLoginClick()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 26.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "로그인",
                    modifier = devLoginModifier,
                    color = KakaoBlack,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "카카오 계정으로 간편하게 로그인해보세요.",
                    color = Color(0xFF4A4A4A),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(46.dp))

                ElevatedButton(
                    onClick = {
                        onKakaoLoginClick()
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(ServerConfig.KAKAO_AUTH_URL)
                        )
                        context.startActivity(intent)
                    },
                    enabled = !isProcessingLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = KakaoYellow,
                        contentColor = KakaoBlack,
                        disabledContainerColor = Color(0xFFE8E0A8),
                        disabledContentColor = Color(0xFF5F5A44)
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.width(48.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            KakaoBubbleIcon(
                                modifier = Modifier.size(26.dp),
                                color = KakaoBlack
                            )
                        }

                        Text(
                            text = if (isProcessingLogin) "로그인 중..." else "카카오 로그인",
                            color = KakaoBlack,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                if (isDevLoginEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onDevLoginClick,
                        enabled = !isProcessingLogin
                    ) {
                        Text(
                            text = "개발자 모드로 입장",
                            color = KakaoBlack,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(28.dp))
                }

                Text(
                    text = "카카오 로그인 시, 이용약관 및 개인정보 처리방침에 동의하게 됩니다.",
                    color = Color(0xFF8A8A8A),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun KakaoBubbleIcon(
    modifier: Modifier = Modifier,
    color: Color = KakaoBlack
) {
    Canvas(modifier = modifier) {
        drawKakaoBubble(color)
    }
}

private fun DrawScope.drawKakaoBubble(color: Color) {
    drawOval(
        color = color,
        topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
        size = Size(size.width * 0.84f, size.height * 0.65f)
    )

    val tail = Path().apply {
        moveTo(size.width * 0.33f, size.height * 0.65f)
        lineTo(size.width * 0.25f, size.height * 0.92f)
        lineTo(size.width * 0.52f, size.height * 0.70f)
        close()
    }

    drawPath(path = tail, color = color)
}
