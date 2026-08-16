package com.cruise.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

    // Task 12：API 33+ 需要在运行时申请通知权限，否则闹钟响了也弹不出通知。
    // 用 Activity Result API 而不是过时的 requestPermissions() override。
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 结果不影响 UI，settings 里会用 canScheduleExact 之类的真实状态展示 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureHighRefreshRate()

        // 应用固定浅色主题（见 CruiseTheme 注释），状态栏图标需保持深色才能在浅色背景上可见
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

    /**
     * 主动配置 120Hz 高刷新率：
     * 1. API 23+ (Android 6.0+) 设置 preferredRefreshRate = 120f；
     * 2. 遍历 Display.supportedModes 优先选取 >= 120Hz 的显示模式（preferredDisplayModeId）。
     */
    private fun configureHighRefreshRate() {
        val win = window ?: return
        val lp = win.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lp.preferredRefreshRate = 120f
            val disp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val bestMode = disp?.supportedModes
                ?.filter { it.refreshRate >= 119f }
                ?.maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                lp.preferredDisplayModeId = bestMode.modeId
            }
        }
        win.attributes = lp
    }
}

