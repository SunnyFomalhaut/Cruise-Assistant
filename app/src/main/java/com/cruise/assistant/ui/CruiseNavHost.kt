package com.cruise.assistant.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import android.widget.Toast
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cruise.assistant.AppContainer
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.domain.fullRoom
import com.cruise.assistant.domain.presetShowsSummary
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.ui.category.CategorySheet
import com.cruise.assistant.ui.category.CategoryViewModel
import com.cruise.assistant.ui.common.viewModelFactory
import com.cruise.assistant.ui.deckmap.DeckMapScreen
import com.cruise.assistant.ui.deckmap.DeckMapViewModel
import com.cruise.assistant.ui.event.EventSheet
import com.cruise.assistant.ui.event.EventSheetViewModel
import com.cruise.assistant.ui.facilities.FacilityGuideScreen
import com.cruise.assistant.ui.facilities.FacilityGuideViewModel
import com.cruise.assistant.ui.onboarding.OnboardingScreen
import com.cruise.assistant.ui.onboarding.OnboardingViewModel
import com.cruise.assistant.ui.pixiedust.GivenList
import com.cruise.assistant.ui.pixiedust.LinkageViewDialog
import com.cruise.assistant.ui.pixiedust.PixieDustEditDialog
import com.cruise.assistant.ui.pixiedust.PixieDustKeypad
import com.cruise.assistant.ui.pixiedust.PixieDustScreen
import com.cruise.assistant.ui.pixiedust.PixieDustViewModel
import com.cruise.assistant.ui.pixiedust.PixieLevelSheet
import com.cruise.assistant.ui.pixiedust.PixieTab
import com.cruise.assistant.ui.pixiedust.ReceivedList
import com.cruise.assistant.ui.pixiedust.ReplyLevelDialog
import com.cruise.assistant.ui.settings.SettingsScreen
import com.cruise.assistant.ui.settings.SettingsViewModel
import com.cruise.assistant.ui.timeline.ConflictDialog
import com.cruise.assistant.ui.timeline.TimelineLoading
import com.cruise.assistant.ui.timeline.TimelineScreen
import com.cruise.assistant.ui.timeline.TimelineViewModel
import com.cruise.assistant.ui.wishlist.WishlistScreen
import com.cruise.assistant.ui.wishlist.WishlistViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

object Routes {
    const val ONBOARDING = "onboarding"
    const val TIMELINE = "timeline"
    const val SETTINGS = "settings"
    const val PIXIE_DUST = "pixiedust"
    const val WISHLIST = "wishlist"
    const val FACILITIES = "facilities"
    const val DECK_MAP = "deck_map"
    const val DECK_MAP_PATTERN = "deck_map?deck={deck}&highlight={highlight}"

    fun deckMap(deck: Int? = null, highlight: String? = null): String {
        return buildString {
            append("deck_map")
            val params = mutableListOf<String>()
            if (deck != null) params.add("deck=$deck")
            if (!highlight.isNullOrBlank()) params.add("highlight=${Uri.encode(highlight)}")
            if (params.isNotEmpty()) {
                append("?").append(params.joinToString("&"))
            }
        }
    }
}

/** 显示范围最小跨度，和 domain 层（TimelineLayout.kt 的 MIN_RANGE_MINUTES）保持一致。 */
private const val MIN_DISPLAY_RANGE_MINUTES = 120

@Composable
fun CruiseNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val settings by container.settingsStore.settings.collectAsState(initial = null)

    // 选完内置船（探险号）进入后的一次性演出提示
    // rememberSaveable：旋转/配置变更不丢一次性弹窗（引导提交后丢失则永不重现）
    var showPresetShowsDialog by rememberSaveable { mutableStateOf(false) }

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
                        container.reminderScheduler,
                        LocalDate.now(),
                    )
                },
            )
            val state by vm.state.collectAsState()
            OnboardingScreen(
                state = state,
                onShipNameChange = vm::onShipNameChange,
                onShipSelected = vm::onShipSelected,
                onScheduleSelected = vm::onScheduleSelected,
                onBoardDateChange = vm::onBoardDateChange,
                onBoardTimeChange = vm::onBoardTimeChange,
                onDisembarkDateChange = vm::onDisembarkDateChange,
                onMaxDeckChange = vm::onMaxDeckChange,
                onDinnerTimeChange = vm::onDinnerTimeChange,
                onSubmit = {
                    if (state.isBuiltInShip) showPresetShowsDialog = true
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
                        container.reminderScheduler,
                    )
                },
            )
            val timelineState by vm.state.collectAsState()
            val current = timelineState

            var sheetRequest by remember { mutableStateOf<EventSheetRequest?>(null) }
            // saveable：旋转后 sheetSeq 从 0 重新递增会撞上 ViewModelStore 里旋转前的
            // 旧 VM（key "event-sheet-1" 复用）→ 编辑状态残留，可能覆盖错误的事件。
            var sheetSeq by rememberSaveable { mutableStateOf(0L) }
            val coroutineScope = rememberCoroutineScope()

            // ---- 需求8：定稿冲突检查。conflictCheck 控制弹窗显隐，highlightIds 在
            // "去修改"关闭弹窗后仍要留着（这才是去修改的意义），只在定稿成功或切换日期时清空。----
            var conflictCheck by remember {
                mutableStateOf<List<Pair<CruiseEvent, CruiseEvent>>?>(null)
            }
            var highlightIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
            var conflictCheckDate by remember { mutableStateOf<LocalDate?>(null) }

            if (current == null) {
                TimelineLoading()
            } else {
                TimelineScreen(
                    state = current,
                    onSelectDate = { date ->
                        highlightIds = emptySet()
                        conflictCheck = null
                        vm.selectDate(date)
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenPixieDust = { navController.navigate(Routes.PIXIE_DUST) },
                    onOpenWishlist = { navController.navigate(Routes.WISHLIST) },
                    onOpenFacilities = { navController.navigate(Routes.FACILITIES) },
                    onOpenDeckMap = { navController.navigate(Routes.DECK_MAP) },
                    onNavigateToDeckMap = { deck, venueName ->
                        navController.navigate(Routes.deckMap(deck = deck, highlight = venueName))
                    },
                    onAddEvent = { startMinute ->
                        sheetSeq++
                        sheetRequest = EventSheetRequest(current.selectedDate, startMinute, 0L, sheetSeq)
                    },
                    onEditEvent = { event ->
                        sheetSeq++
                        sheetRequest = EventSheetRequest(event.date, 0, event.id, sheetSeq)
                    },
                    onMoveToLane = vm::moveToLane,
                    onToggleActive = vm::toggleActive,
                    onKeepOnlyThisShow = vm::keepOnlyThisShow,
                    onDeleteEvent = vm::deleteEvent,
                    onGeneratePlan = {
                        val date = current.selectedDate
                        coroutineScope.launch {
                            val found = vm.conflictsOn(date)
                            // 期间用户切了日期 → 丢弃结果：dialog 描述会指错天、定稿会定错天。
                            // 不能比较 current（lambda 创建时的旧快照），必须读 VM 当前选中日期。
                            if (vm.state.value?.selectedDate != date) return@launch
                            highlightIds = found.flatMap { listOf(it.first.id, it.second.id) }.toSet()
                            conflictCheckDate = date
                            conflictCheck = found
                        }
                    },
                    onUnlockDay = { vm.unlockDay(current.selectedDate) },
                    onJumpToNow = { vm.jumpToToday() },
                    onToggleFullDay = vm::toggleFullDay,
                    conflictIds = highlightIds,
                )

                if (showPresetShowsDialog) {
                    AlertDialog(
                        onDismissRequest = { showPresetShowsDialog = false },
                        title = { Text(stringResource(R.string.preset_shows_dialog_title)) },
                        text = {
                            Text(
                                stringResource(R.string.preset_shows_dialog_header) + "\n\n" +
                                    presetShowsSummary() + "\n\n" +
                                    stringResource(R.string.preset_shows_dialog_footer),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showPresetShowsDialog = false }) {
                                Text(stringResource(R.string.preset_shows_dialog_ok))
                            }
                        },
                    )
                }
            }

            conflictCheck?.let { conflicts ->
                ConflictDialog(
                    conflicts = conflicts,
                    onGoFix = { conflictCheck = null }, // 弹窗关，highlightIds 保留 → 角标不消失
                    onFinalize = {
                        conflictCheckDate?.let { vm.finalizeDay(it) }
                        highlightIds = emptySet()
                        conflictCheck = null
                    },
                    onDismiss = { conflictCheck = null },
                )
            }

            // ---- 添加/编辑事件弹窗（Task 8）----
            sheetRequest?.let { request ->
                // key 必须能区分弹窗类型（否则会撞上其它同作用域的 viewModel key，
                // ViewModelFactory 盲转型会在运行时抛 ClassCastException——见 Task 6 遗留问题）。
                // key 只依赖 requestId（每次打开递增），保证新开弹窗不复用旧 ViewModel、不带旧草稿。
                val sheetVm: EventSheetViewModel = viewModel(
                    key = "event-sheet-${request.requestId}",
                    factory = viewModelFactory {
                        EventSheetViewModel(
                            container.eventRepository,
                            container.categoryRepository,
                            container.locationPresetRepository,
                            container.voyageRepository,
                            container.reminderScheduler,
                            request.date,
                            request.startMinute,
                            request.editingId,
                        )
                    },
                )
                val sheetState by sheetVm.state.collectAsState()
                var showCategorySheet by remember { mutableStateOf(false) }
                var pendingScopeChoice by remember { mutableStateOf(false) }
                // 营业时间校验弹窗"仍要保存"时重发的 scope：记住 scope 弹窗里用户的选择，
                // 否则循环成员事件在 WHOLE_GROUP 下被校验拦截后，重发会静默退化成仅本日。
                var hoursCheckScope by remember { mutableStateOf(RecurrenceScope.THIS_DAY_ONLY) }

                EventSheet(
                    state = sheetState,
                    onCategorySelect = sheetVm::onCategorySelect,
                    onManageCategories = { showCategorySheet = true },
                    onTitleChange = sheetVm::onTitleChange,
                    onDeckChange = sheetVm::onDeckChange,
                    onZoneChange = sheetVm::onZoneChange,
                    onPlaceNameChange = sheetVm::onPlaceNameChange,
                    onNoLocationToggle = sheetVm::onNoLocationToggle,
                    onSelectPresetLocation = sheetVm::onSelectPresetLocation,
                    onCustomLocationSelected = sheetVm::onCustomLocationSelected,
                    onShowPresetSelect = sheetVm::onShowPresetSelect,
                    onCustomShowSelected = sheetVm::onCustomShowSelected,
                    onMealPeriodSelect = sheetVm::onMealPeriodSelect,
                    onStartChange = sheetVm::onStartChange,
                    onEndChange = sheetVm::onEndChange,
                    onRecurringToggle = sheetVm::onRecurringToggle,
                    onSave = {
                        if (sheetState.isRecurrenceMember) {
                            pendingScopeChoice = true
                        } else {
                            hoursCheckScope = RecurrenceScope.THIS_DAY_ONLY
                            sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null }
                        }
                    },
                    // 先置 override 标志再重发 save：save() 内 hoursCheckPending 已清、
                    // hoursCheckOverride 短路校验，不会重复弹窗。scope 用 hoursCheckScope
                    // （scope 弹窗里用户的选择），保持循环成员事件的 scope 语义一致。
                    onHoursCheckOverride = {
                        sheetVm.onHoursCheckOverride()
                        sheetVm.save(hoursCheckScope) { sheetRequest = null }
                    },
                    onHoursCheckDismiss = sheetVm::onHoursCheckDismiss,
                    onDismiss = { sheetRequest = null },
                )

                if (pendingScopeChoice) {
                    AlertDialog(
                        onDismissRequest = { pendingScopeChoice = false },
                        title = { Text(stringResource(R.string.recur_scope_title)) },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingScopeChoice = false
                                hoursCheckScope = RecurrenceScope.WHOLE_GROUP
                                sheetVm.save(RecurrenceScope.WHOLE_GROUP) { sheetRequest = null }
                            }) { Text(stringResource(R.string.recur_scope_whole_group)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                pendingScopeChoice = false
                                hoursCheckScope = RecurrenceScope.THIS_DAY_ONLY
                                sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null }
                            }) { Text(stringResource(R.string.recur_scope_this_day)) }
                        },
                    )
                }
                if (showCategorySheet) {
                    val catVm: CategoryViewModel = viewModel(
                        key = "category-sheet",
                        factory = viewModelFactory {
                            CategoryViewModel(container.categoryRepository, container.reminderScheduler)
                        },
                    )
                    val catState by catVm.state.collectAsState()
                    CategorySheet(
                        state = catState,
                        onNameChange = catVm::onNameChange,
                        onColorSelect = catVm::onColorSelect,
                        onIconSelect = catVm::onIconSelect,
                        onSubCategoryToggle = catVm::onSubCategoryToggle,
                        onParentSelect = catVm::onParentSelect,
                        onArchiveToggle = catVm::onArchiveToggle,
                        onEditExisting = catVm::startEdit,
                        onNewCategory = catVm::startNew,
                        onSave = { catVm.save { showCategorySheet = false } },
                        onDelete = { catVm.delete { showCategorySheet = false } },
                        onDismiss = { showCategorySheet = false },
                    )
                }
            }
        }
        composable(Routes.PIXIE_DUST) {
            val vm: PixieDustViewModel = viewModel(
                factory = viewModelFactory {
                    PixieDustViewModel(
                        container.pixieDustRepository,
                        container.pixieLevelRepository,
                        container.voyageRepository,
                        container.settingsStore,
                    )
                },
            )
            val pixieState by vm.state.collectAsState()
            // 编辑弹窗目标（Task 8 消费）；当前无消费者，先留存以待编辑弹窗接入
            var editTarget by remember { mutableStateOf<PixieDustRecord?>(null) }
            var showLevelSheet by remember { mutableStateOf(false) }
            var showLinkage by remember { mutableStateOf(false) }
            val context = LocalContext.current
            LaunchedEffect(pixieState?.savedEvent) {
                pixieState?.savedEvent?.let { rec ->
                    val levelName = pixieState?.levelById?.get(rec.levelId)?.name
                        ?: rec.levelNameSnapshot
                    Toast.makeText(
                        context,
                        context.getString(R.string.pixie_saved_format, rec.fullRoom, levelName),
                        Toast.LENGTH_SHORT,
                    ).show()
                    vm.onToastShown()
                }
            }
            PixieDustScreen(
                state = pixieState,
                onBack = { navController.popBackStack() },
                onTabChange = vm::onTabChange,
                onOpenLevelManager = { showLevelSheet = true },
                onOpenLinkage = { showLinkage = true },
                content = { s ->
                    Column(Modifier.fillMaxSize()) {
                        if (!s.keypadCollapsed) {
                            PixieDustKeypad(
                                state = s,
                                onDeckChange = vm::onDeckChange,
                                onDigit = vm::onDigit,
                                onBackspace = vm::onBackspace,
                                onClearRoom = vm::onClearRoom,
                                onLevelSelect = vm::onLevelSelect,
                                onSave = vm::saveRecord,
                            )
                        }
                        TextButton(
                            onClick = vm::onToggleKeypad,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                stringResource(
                                    if (s.keypadCollapsed) R.string.pixie_expand_keypad
                                    else R.string.pixie_collapse_keypad
                                )
                            )
                        }
                        if (s.tab == PixieTab.GIVEN) {
                            GivenList(
                                state = s,
                                onToggleViewMode = vm::onToggleViewMode,
                                onEditRecord = { editTarget = it },
                            )
                        } else {
                            ReceivedList(
                                state = s,
                                onEditRecord = { editTarget = it },
                                onRequestReply = { vm.onRequestReply(it) },
                                onCancelReply = { vm.onToggleReplied(it, false) },
                            )
                        }
                    }
                },
            )

            // ---- 编辑弹窗（Task 8）：列表点击置位 editTarget，这里消费 ----
            editTarget?.let { target ->
                PixieDustEditDialog(
                    initial = target,
                    isReceived = target.direction == PixieDustDirection.RECEIVED,
                    deckRange = pixieState?.deckRange ?: 1..20,
                    levels = pixieState?.levels ?: emptyList(),
                    onRequestReply = { edited ->
                        editTarget = null    // 先关编辑弹窗
                        vm.onRequestReply(edited)
                    },
                    onSave = {
                        vm.saveEdited(it)
                        editTarget = null
                    },
                    onDelete = {
                        vm.deleteRecord(it)
                        editTarget = null
                    },
                    onDismiss = { editTarget = null },
                )
            }

            // ---- 回礼等级弹窗（联动方向二）----
            pixieState?.pendingReply?.let { pending ->
                ReplyLevelDialog(
                    record = pending,
                    levels = pixieState?.levels ?: emptyList(),
                    onConfirm = { vm.confirmReply(it) },
                    onDismiss = { vm.cancelReply() },
                )
            }

            // ---- 联动自动勾选 toast ----
            LaunchedEffect(pixieState?.autoRepliedRoom) {
                pixieState?.autoRepliedRoom?.let { roomLabel ->
                    Toast.makeText(context, context.getString(R.string.pixie_auto_replied_format, roomLabel), Toast.LENGTH_SHORT).show()
                    vm.onAutoReplyToastShown()
                }
            }

            // ---- 等级管理弹层（Task 9）：齿轮触发 ----
            if (showLevelSheet) {
                PixieLevelSheet(
                    levels = pixieState?.levels ?: emptyList(),
                    onSave = vm::saveLevel,
                    onDelete = vm::deleteLevel,
                    onMove = vm::onMoveLevel,
                    countByLevel = vm::countByLevel,
                    onDismiss = { showLevelSheet = false },
                )
            }

            // ---- 联动检视（统计条末尾链接图标触发）：只读展示，pixieState 为空不渲染 ----
            if (showLinkage) {
                pixieState?.let { s ->
                    LinkageViewDialog(state = s, onDismiss = { showLinkage = false })
                }
            }
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    SettingsViewModel(
                        container.settingsStore,
                        container.voyageRepository,
                        container.eventRepository,
                        container.reminderScheduler,
                    )
                },
            )
            val settingsState by vm.state.collectAsState()
            val current = settingsState
            val context = LocalContext.current
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
            ) { uri ->
                if (uri != null) {
                    vm.saveCustomWallpaper(context, uri)
                }
            }
            var showCategorySheet by remember { mutableStateOf(false) }

            if (current != null) {
                SettingsScreen(
                    state = current,
                    onRangeChange = vm::setRange,
                    onHourHeightChange = vm::setHourHeight,
                    onCollapseToggle = vm::setCollapse,
                    onNotificationsToggle = vm::setNotifications,
                    onRemindBefore15Change = vm::setRemindBefore15,
                    onRemindBefore5Change = vm::setRemindBefore5,
                    onRemindAtStartChange = vm::setRemindAtStart,
                    onDinnerTimeChange = vm::setDinnerTime,
                    onResetVoyage = {
                        vm.resetVoyage {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onManageCategories = { showCategorySheet = true },
                    exactAlarmAvailable = container.reminderScheduler.canScheduleExact(),
                    onNavigateToFacilities = { navController.navigate(Routes.FACILITIES) },
                    onNavigateToDeckMap = { navController.navigate(Routes.DECK_MAP) },
                    onColorThemeChange = vm::setColorTheme,
                    onWallpaperEnabledToggle = vm::setWallpaperEnabled,
                    onWallpaperKeyChange = vm::setWallpaperKey,
                    onWallpaperAlphaChange = vm::setWallpaperAlpha,
                    onPickCustomWallpaper = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
            }

            // ---- 事项管理：复用 Task 9 的 CategorySheet，不重建第二套类别编辑器 ----
            if (showCategorySheet) {
                val catVm: CategoryViewModel = viewModel(
                    key = "settings-category-sheet",
                    factory = viewModelFactory {
                        CategoryViewModel(container.categoryRepository, container.reminderScheduler)
                    },
                )
                val catState by catVm.state.collectAsState()
                CategorySheet(
                    state = catState,
                    onNameChange = catVm::onNameChange,
                    onColorSelect = catVm::onColorSelect,
                    onIconSelect = catVm::onIconSelect,
                    onSubCategoryToggle = catVm::onSubCategoryToggle,
                    onParentSelect = catVm::onParentSelect,
                    onArchiveToggle = catVm::onArchiveToggle,
                    onEditExisting = catVm::startEdit,
                    onNewCategory = catVm::startNew,
                    onSave = { catVm.save { showCategorySheet = false } },
                    onDelete = { catVm.delete { showCategorySheet = false } },
                    onDismiss = { showCategorySheet = false },
                )
            }
        }
        composable(Routes.WISHLIST) {
            val vm: WishlistViewModel = viewModel(
                factory = viewModelFactory {
                    WishlistViewModel(container.wishlistRepository, container.categoryRepository)
                },
            )
            val state by vm.state.collectAsState()
            WishlistScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onNavigateToDeckMap = { deck, venueName ->
                    navController.navigate(Routes.deckMap(deck = deck, highlight = venueName))
                },
                onToggleCompleted = vm::toggleCompleted,
                onSaveItem = vm::saveItem,
                onDeleteItem = vm::deleteItem,
                onFilterCategory = vm::onCategoryFilterSelect,
            )
        }
        composable(Routes.FACILITIES) {
            val vm: FacilityGuideViewModel = viewModel()
            val state by vm.state.collectAsState()
            FacilityGuideScreen(
                state = state,
                categories = vm.categories,
                onBack = { navController.popBackStack() },
                onOpenDeckMap = { navController.navigate(Routes.deckMap()) },
                onNavigateToDeckMap = { deck, venueName ->
                    navController.navigate(Routes.deckMap(deck = deck, highlight = venueName))
                },
                onSearchChange = vm::onSearchQueryChange,
                onCategorySelect = vm::onCategorySelect,
                onToggleOnlyOpenNow = vm::onToggleOnlyOpenNow,
            )
        }
        composable(
            route = Routes.DECK_MAP_PATTERN,
            arguments = listOf(
                navArgument("deck") {
                    type = NavType.IntType
                    defaultValue = 5
                },
                navArgument("highlight") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val initialDeck = backStackEntry.arguments?.getInt("deck") ?: 5
            val highlightTarget = backStackEntry.arguments?.getString("highlight")
            val vm: DeckMapViewModel = viewModel(
                key = "deck_map_${initialDeck}_${highlightTarget}",
                factory = viewModelFactory {
                    DeckMapViewModel(initialDeck = initialDeck, initialHighlight = highlightTarget)
                },
            )
            val state by vm.state.collectAsState()
            DeckMapScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSelectDeck = vm::onSelectDeck,
                onNavigateToFacility = vm::navigateToFacility,
                onClearHighlight = vm::clearHighlight,
            )
        }
    }
}

/** FAB/编辑入口 → 事件弹窗 的请求参数。editingId==0L 表示新建。requestId 每次打开递增，保证 ViewModel 不复用。 */
data class EventSheetRequest(
    val date: LocalDate,
    val startMinute: Int,
    val editingId: Long,
    val requestId: Long,
)
