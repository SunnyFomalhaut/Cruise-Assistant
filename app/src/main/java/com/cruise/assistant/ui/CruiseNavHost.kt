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
import com.cruise.assistant.ui.timeline.TimelineLoading
import com.cruise.assistant.ui.timeline.TimelineScreen
import com.cruise.assistant.ui.timeline.TimelineViewModel
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
            val vm: TimelineViewModel = viewModel(
                factory = viewModelFactory {
                    TimelineViewModel(
                        container.voyageRepository,
                        container.eventRepository,
                        container.categoryRepository,
                        container.settingsStore,
                    )
                },
            )
            val timelineState by vm.state.collectAsState()
            val current = timelineState
            if (current == null) {
                TimelineLoading()
            } else {
                TimelineScreen(
                    state = current,
                    onSelectDate = vm::selectDate,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenDisplayRange = { /* Task 10：显示范围面板 */ },
                    onAddEvent = { /* Task 8：事件弹窗 */ },
                    onEditEvent = { /* Task 8：事件弹窗 */ },
                    onMoveToLane = vm::moveToLane,
                    onDeleteEvent = vm::deleteEvent,
                    onGeneratePlan = { /* Task 11：定稿流程与冲突弹窗 */ },
                    onUnlockDay = { vm.unlockDay(current.selectedDate) },
                    onToggleFullDay = { /* Task 10：把临时展开范围传给 ViewModel */ },
                    conflictIds = emptySet(),
                )
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
