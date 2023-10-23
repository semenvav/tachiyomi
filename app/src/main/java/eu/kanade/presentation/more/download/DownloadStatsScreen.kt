package eu.kanade.presentation.more.download

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.more.download.components.DeleteAlertDialog
import eu.kanade.presentation.more.download.components.DownloadStatOperationInfoDialog
import eu.kanade.presentation.more.download.components.DownloadStatOperationInfoStartDialog
import eu.kanade.presentation.more.download.components.DownloadStatOperationMultiInfoDialog
import eu.kanade.presentation.more.download.components.DownloadStatSettingsDialog
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.presentation.more.download.tabs.downloadStatsItemsTab
import eu.kanade.presentation.more.download.tabs.overAllStatsTab
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.HorizontalPager
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabIndicator
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.screens.LoadingScreen

class DownloadStatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { DownloadStatsScreenModel() }
        val state by screenModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        val tabs = listOf(
            overAllStatsTab(
                state = state,
                screenModel = screenModel,
            ),
            downloadStatsItemsTab(
                state = state,
                screenModel = screenModel,
            ),
        )

        val scope = rememberCoroutineScope()

        val pagerState = rememberPagerState(initialPage = screenModel.activeCategoryIndex) { tabs.size }

        val tab = tabs[pagerState.currentPage]

        when (val dialog = state.dialog) {
            is Dialog.SettingsSheet -> run {
                DownloadStatSettingsDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    descendingOrder = state.descendingOrder,
                    sortMode = state.sortMode,
                    groupByMode = state.groupByMode,
                    showNotDownloaded = state.showNotDownloaded,
                    onSort = screenModel::runSort,
                    onGroup = screenModel::runGroupBy,
                    toggleShowNotDownloaded = screenModel::toggleShowNoDownload,
                )
            }
            is Dialog.DeleteManga -> {
                DeleteAlertDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onConfirm = { screenModel.deleteMangas(dialog.items) },
                )
            }
            is Dialog.DownloadStatOperationInfo -> {
                DownloadStatOperationInfoDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onMangaClick = { mangaId -> navigator.push(MangaScreen(mangaId)) },
                    item = dialog.downloadStatOperation,
                    findManga = screenModel::findManga,
                )
            }

            is Dialog.MultiMangaDownloadStatOperationInfo -> {
                DownloadStatOperationMultiInfoDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onMangaClick = { mangaId -> navigator.push(MangaScreen(mangaId)) },
                    items = dialog.downloadStatOperation,
                    findManga = screenModel::findManga,
                )
            }

            is Dialog.DownloadStatOperationStart -> {
                DownloadStatOperationInfoStartDialog(
                    onDismissRequest = screenModel::dismissDialog,
                )
            }

            else -> {}
        }

        Scaffold(
            topBar = { scrollBehavior ->
                DownloadStatsAppBar(
                    selected = state.selected,
                    onCancelActionMode = { screenModel.toggleAllSelection(false) },
                    scrollBehavior = scrollBehavior,
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = screenModel::search,
                    navigateUp = navigator::pop,
                    defaultActions = tab.actions,
                    actionModeActions = tab.actionModeActions,
                    searchEnabled = tab.searchEnabled,
                    actionModeEnabled = tab.actionModeActions.isNotEmpty(),
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            if (state.isLoading) {
                LoadingScreen(modifier = Modifier.padding(contentPadding))
            } else {
                Column(
                    modifier = Modifier.padding(
                        top = contentPadding.calculateTopPadding(),
                        start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    ),
                ) {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = {
                            TabIndicator(
                                it[pagerState.currentPage],
                                pagerState.currentPageOffsetFraction,
                            )
                        },
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { TabText(text = stringResource(tab.titleRes)) },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        tabs[page].content(
                            PaddingValues(
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                            snackbarHostState,
                        )
                    }
                    LaunchedEffect(pagerState.currentPage) {
                        screenModel.activeCategoryIndex = pagerState.currentPage
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStatsAppBar(
    modifier: Modifier = Modifier,
    selected: List<DownloadStatManga>,
    onCancelActionMode: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    navigateUp: (() -> Unit)?,
    defaultActions: List<AppBar.Action>,
    actionModeActions: List<AppBar.Action>,
    searchEnabled: Boolean,
    actionModeEnabled: Boolean,
) {
    if (actionModeEnabled && selected.isNotEmpty()) {
        AppBar(
            modifier = modifier,
            title = stringResource(R.string.label_download_stats),
            onCancelActionMode = onCancelActionMode,
            actionModeActions = { AppBarActions(actionModeActions) },
            actionModeCounter = selected.size,
            scrollBehavior = scrollBehavior,
            navigateUp = navigateUp,
        )
        BackHandler(
            onBack = onCancelActionMode,
        )
    } else {
        SearchToolbar(
            navigateUp = navigateUp,
            titleContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_download_stats),
                        maxLines = 1,
                        modifier = Modifier.weight(1f, false),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            searchEnabled = searchEnabled,
            searchQuery = searchQuery.takeIf { searchEnabled },
            onChangeSearchQuery = onChangeSearchQuery,
            actions = { AppBarActions(defaultActions) },
            scrollBehavior = scrollBehavior,
        )
        if (searchQuery != null && searchEnabled) {
            BackHandler(
                onBack = { onChangeSearchQuery(null) },
            )
        }
    }
}
