package com.cruise.assistant.ui.deckmap

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cruise.assistant.R
import com.cruise.assistant.domain.DeckInfo
import com.cruise.assistant.domain.DeckMapRepository
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckMapScreen(
    state: DeckMapUiState,
    onBack: () -> Unit,
    onSelectDeck: (Int) -> Unit,
    onNavigateToFacility: (Int, String) -> Unit = { _, _ -> },
    onClearHighlight: () -> Unit = {},
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showSearchSheet by remember { mutableStateOf(false) }
    val htmlContent = remember(state.selectedDeck, state.highlightTarget) {
        val svg = runCatching {
            context.assets.open("decks/deck_${state.selectedDeck}.svg").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        DeckMapRepository.inlineHtmlWrapperFor(svg, state.highlightTarget)
    }

    if (showSearchSheet) {
        DeckMapSearchSheet(
            currentDeck = state.selectedDeck,
            onSelectResult = { targetDeck, query ->
                onNavigateToFacility(targetDeck, query)
                showSearchSheet = false
            },
            onDismiss = { showSearchSheet = false },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.deck_map_screen_title_format, state.selectedDeck),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Slate700,
                        )
                    }
                },
                actions = {
                    if (state.hasMap) {
                        IconButton(onClick = {
                            webViewRef?.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.CenterFocusStrong,
                                contentDescription = stringResource(R.string.deck_map_reset_zoom),
                                tint = CruiseMarine,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---- 左侧甲板层级快速切换 Rail ----
            DeckRail(
                decks = state.decks,
                selectedDeck = state.selectedDeck,
                onSelectDeck = onSelectDeck,
                onOpenSearch = { showSearchSheet = true },
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight(),
            )

            // ---- 右侧地图主视图 ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                if (state.hasMap) {
                    // 硬件加速矢量地图容器
                    SvgMapView(
                        htmlContent = htmlContent,
                        onWebViewReady = { webViewRef = it },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // 顶部聚焦提示标签（当由设施指南导航而来时显示）
                    state.highlightTarget?.let { target ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CruiseAzureContainer.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, CruiseAzure.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = CruiseMarine,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = stringResource(R.string.deck_map_focused_format, target),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CruiseMarine,
                                )
                                IconButton(
                                    onClick = onClearHighlight,
                                    modifier = Modifier.size(18.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = Slate500,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 船首/船尾 悬浮方向指示标
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.deck_map_bow),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CruiseMarine,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.deck_map_stern),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                            )
                        }
                    }
                } else {
                    // 暂无图纸占位提示
                    NoMapPlaceholder(
                        deckNumber = state.selectedDeck,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckRail(
    decks: List<DeckInfo>,
    selectedDeck: Int,
    onSelectDeck: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 自动滑动到选中项
    LaunchedEffect(selectedDeck, decks) {
        val index = decks.indexOfFirst { it.deckNumber == selectedDeck }
        if (index >= 0) {
            listState.animateScrollToItem((index - 2).coerceAtLeast(0))
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(decks, key = { it.deckNumber }) { deck ->
                val isSelected = deck.deckNumber == selectedDeck
                val containerColor = when {
                    isSelected -> CruiseAzure
                    deck.hasMap -> CruiseAzureContainer.copy(alpha = 0.5f)
                    else -> Slate100
                }
                val textColor = when {
                    isSelected -> Color.White
                    deck.hasMap -> CruiseMarine
                    else -> Slate500
                }

                Box(
                    modifier = Modifier
                        .size(48.dp, 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(containerColor)
                        .clickable { onSelectDeck(deck.deckNumber) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = deck.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected || deck.hasMap) FontWeight.Bold else FontWeight.Normal,
                            color = textColor,
                        )
                        if (deck.hasMap && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(CruiseAzure),
                            )
                        }
                    }
                }
            }

            // D5 下方的搜索入口按钮
            item {
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .size(48.dp, 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenSearch() },
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = CruiseMarine,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "搜索",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CruiseMarine,
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SvgMapView(
    htmlContent: String,
    onWebViewReady: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                }
                onWebViewReady(this)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = modifier,
    )
}

@Composable
private fun NoMapPlaceholder(
    deckNumber: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Slate100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsBoat,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.deck_map_no_map_title, deckNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.deck_map_no_map_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500,
                textAlign = TextAlign.Center,
            )
        }
    }
}
