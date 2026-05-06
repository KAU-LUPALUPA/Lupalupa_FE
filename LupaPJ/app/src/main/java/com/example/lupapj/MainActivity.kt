package com.example.lupapj

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.lupapj.app.LupaApp
import com.example.lupapj.ui.screens.splash.SplashScreen
import com.example.lupapj.ui.theme.LupaPJTheme

class MainActivity : ComponentActivity() {

    private var deepLinkState by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deepLinkState = intent?.data
        Log.d("KAKAO", "onCreate deepLink = $deepLinkState")

        enableEdgeToEdge()

        setContent {
            LupaPJTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashComplete = { showSplash = false })
                } else {
                    LupaApp(deepLink = deepLinkState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        deepLinkState = intent.data
        Log.d("KAKAO", "onNewIntent deepLink = $deepLinkState")
    }
}