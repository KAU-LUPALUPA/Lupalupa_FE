package com.example.lupapj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lupapj.app.LupaApp
import com.example.lupapj.ui.theme.LupaPJTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LupaPJTheme {
                LupaApp()
            }
        }
    }
}
