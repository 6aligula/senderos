package com.example.senderos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.senderos.ui.screens.AppNavigator
import com.example.senderos.ui.theme.SenderosTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SenderosTheme {
                AppNavigator()
            }
        }
    }
}
