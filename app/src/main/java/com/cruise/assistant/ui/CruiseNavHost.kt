package com.cruise.assistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cruise.assistant.AppContainer
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.ui.common.viewModelFactory
import com.cruise.assistant.ui.event.EventSheet
import com.cruise.assistant.ui.event.EventSheetViewModel
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

            var sheetRequest by remember { mutableStateOf<EventSheetRequest?>(null) }

            if (current == null) {
                TimelineLoading()
            } else {
                TimelineScreen(
                    state = current,
                    onSelectDate = vm::selectDate,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenDisplayRange = { /* Task 10：显示范围面板 */ },
                    onAddEvent = { startMinute ->
                        sheetRequest = EventSheetRequest(current.selectedDate, startMinute, 0L)
                    },
                    onEditEvent = { event ->
                        sheetRequest = EventSheetRequest(event.date, 0, event.id)
                    },
                    onMoveToLane = vm::moveToLane,
                    onDeleteEvent = vm::deleteEvent,
                    onGeneratePlan = { /* Task 11：定稿流程与冲突弹窗 */ },
                    onUnlockDay = { vm.unlockDay(current.selectedDate) },
                    onToggleFullDay = { /* Task 10：把临时展开范围传给 ViewModel */ },
                    conflictIds = emptySet(),
                )
            }

            // ---- 添加/编辑事件弹窗（Task 8）----
            sheetRequest?.let { request ->
                // key 必须能区分弹窗类型（否则会撞上其它同作用域的 viewModel key，
                // ViewModelFactory 盲转型会在运行时抛 ClassCastException——见 Task 6 遗留问题）。
                val sheetVm: EventSheetViewModel = viewModel(
                    key = "event-sheet-${request.date}-${request.editingId}-${request.startMinute}",
                    factory = viewModelFactory {
                        EventSheetViewModel(
                            container.eventRepository,
                            container.categoryRepository,
                            container.locationPresetRepository,
                            container.voyageRepository,
                            request.date,
                            request.startMinute,
                            request.editingId,
                        )
                    },
                )
                val sheetState by sheetVm.state.collectAsState()
                var showCategorySheet by remember { mutableStateOf(false) }
                var pendingScopeChoice by remember { mutableStateOf(false) }

                EventSheet(
                    state = sheetState,
                    onCategorySelect = sheetVm::onCategorySelect,
                    onManageCategories = { showCategorySheet = true },
                    onTitleChange = sheetVm::onTitleChange,
                    onDeckChange = sheetVm::onDeckChange,
                    onZoneChange = sheetVm::onZoneChange,
                    onPlaceNameChange = sheetVm::onPlaceNameChange,
                    onNoLocationToggle = sheetVm::onNoLocationToggle,
                    onStartChange = sheetVm::onStartChange,
                    onEndChange = sheetVm::onEndChange,
                    onRecurringToggle = sheetVm::onRecurringToggle,
                    onSave = {
                        if (sheetState.isRecurrenceMember) {
                            pendingScopeChoice = true
                        } else {
                            sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null }
                        }
                    },
                    onDismiss = { sheetRequest = null },
                )

                if (pendingScopeChoice) {
                    AlertDialog(
                        onDismissRequest = { pendingScopeChoice = false },
                        title = { Text(stringResource(R.string.recur_scope_title)) },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingScopeChoice = false
                                sheetVm.save(RecurrenceScope.WHOLE_GROUP) { sheetRequest = null }
                            }) { Text(stringResource(R.string.recur_scope_whole_group)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                pendingScopeChoice = false
                                sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null }
                            }) { Text(stringResource(R.string.recur_scope_this_day)) }
                        },
                    )
                }
                // Task 9 在这里挂 CategorySheet（showCategorySheet）
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

/** FAB/编辑入口 → 事件弹窗 的请求参数。editingId==0L 表示新建。 */
data class EventSheetRequest(
    val date: LocalDate,
    val startMinute: Int,
    val editingId: Long,
)
