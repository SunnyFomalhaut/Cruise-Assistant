package com.cruise.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.cruise.assistant.ui.CruiseNavHost
import com.cruise.assistant.ui.theme.CruiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 应用固定浅色主题（见 CruiseTheme 注释），状态栏图标需保持深色才能在浅色背景上可见
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        val container = (application as CruiseApp).container
        setContent {
            CruiseTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .consumeWindowInsets(WindowInsets.safeDrawing),
                ) {
                    CruiseNavHost(container)
                }
            }
        }
    }
}
