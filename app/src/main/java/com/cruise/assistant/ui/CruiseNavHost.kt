package com.cruise.assistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cruise.assistant.AppContainer
import com.cruise.assistant.ui.common.viewModelFactory
import com.cruise.assistant.ui.onboarding.OnboardingScreen
import com.cruise.assistant.ui.onboarding.OnboardingViewModel
import java.time.LocalDate

object Routes {
    const val ONBOARDING = "onboarding"
    const val TIMELINE = "timeline"
    const val SETTINGS = "settings"
}

@Composable
fun CruiseNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val settings by container.settingsStore.settings.collectAsState(initial = null)

    // 设置还没读出来，先转圈，避免闪一下引导页
    val loaded = settings
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (loaded.onboardingCompleted) Routes.TIMELINE else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = viewModel(
                factory = viewModelFactory {
                    OnboardingViewModel(
                        container.voyageRepository,
                        container.settingsStore,
                        LocalDate.now(),
                    )
                },
            )
            val state by vm.state.collectAsState()
            OnboardingScreen(
                state = state,
                onShipNameChange = vm::onShipNameChange,
                onBoardDateChange = vm::onBoardDateChange,
                onBoardTimeChange = vm::onBoardTimeChange,
                onDisembarkDateChange = vm::onDisembarkDateChange,
                onMaxDeckChange = vm::onMaxDeckChange,
                onSubmit = {
                    vm.submit {
                        navController.navigate(Routes.TIMELINE) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.TIMELINE) {
            // Task 7 填入 TimelineScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("时间轴（Task 7）")
            }
        }
        composable(Routes.SETTINGS) {
            // Task 10 填入 SettingsScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("设置（Task 10）")
            }
        }
    }
}
