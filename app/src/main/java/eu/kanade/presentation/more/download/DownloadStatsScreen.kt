package eu.kanade.presentation.more.download

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DensitySmall
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class DownloadStatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { DownloadStatsScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                DownloadStatsAppBar(
                    groupByMode = state.groupByMode,
                    selected = state.selected,
                    onSelectAll = { screenModel.toggleAllSelection(true) },
                    onInvertSelection = { screenModel.invertSelection() },
                    onCancelActionMode = { screenModel.toggleAllSelection(false) },
                    onMultiDeleteClicked = screenModel::deleteMangas,
                    scrollBehavior = scrollBehavior,
                    onClickGroup = screenModel::runGroupBy,
                    onClickSort = screenModel::runSortAction,
                    sortState = state.sortMode,
                    descendingOrder = state.descendingOrder,
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = screenModel::search,
                    navigateUp = navigator::pop,
                )
            },
        ) { contentPadding ->
            when {
                state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))

                state.processedItems.isEmpty() -> EmptyScreen(
                    textResource = R.string.information_no_downloads,
                    modifier = Modifier.padding(contentPadding),
                )

                else -> {
                    when (state.groupByMode) {
                        GroupByMode.NONE -> FastScrollLazyColumn(
                            contentPadding = contentPadding,
                        ) {
                            downloadStatUiItems(
                                items = state.processedItems,
                                selectionMode = state.selectionMode,
                                onClick = { item ->
                                    navigator.push(
                                        MangaScreen(item.libraryManga.manga.id),
                                    )
                                },
                                onSelected = screenModel::toggleSelection,
                                onDeleteManga = screenModel::deleteMangas,
                            )
                        }

                        GroupByMode.BY_SOURCE -> {
                            CategoryList(
                                contentPadding = contentPadding,
                                selectionMode = state.selectionMode,
                                onMangaClick = { item ->
                                    navigator.push(
                                        MangaScreen(item.libraryManga.manga.id),
                                    )
                                },
                                onDeleteManga = screenModel::deleteMangas,
                                onGroupSelected = screenModel::groupSelection,
                                onSelected = screenModel::toggleSelection,
                                categoryMap = screenModel.categoryMap(state.processedItems, GroupByMode.BY_SOURCE, state.sortMode, state.descendingOrder),
                            )
                        }

                        GroupByMode.BY_CATEGORY -> {
                            CategoryList(
                                contentPadding = contentPadding,
                                selectionMode = state.selectionMode,
                                onMangaClick = { item ->
                                    navigator.push(
                                        MangaScreen(item.libraryManga.manga.id),
                                    )
                                },
                                onDeleteManga = screenModel::deleteMangas,
                                onGroupSelected = screenModel::groupSelection,
                                onSelected = screenModel::toggleSelection,
                                categoryMap = screenModel.categoryMap(state.processedItems, GroupByMode.BY_CATEGORY, state.sortMode, state.descendingOrder),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStatsAppBar(
    groupByMode: GroupByMode,
    modifier: Modifier = Modifier,
    selected: List<DownloadStatManga>,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    onClickSort: (SortingMode) -> Unit,
    onClickGroup: (GroupByMode) -> Unit,
    onMultiDeleteClicked: (List<DownloadStatManga>) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    sortState: SortingMode,
    descendingOrder: Boolean? = null,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    navigateUp: (() -> Unit)?,
) {
    if (selected.isNotEmpty()) {
        DownloadStatsActionAppBar(
            modifier = modifier,
            selected = selected,
            onSelectAll = onSelectAll,
            onInvertSelection = onInvertSelection,
            onCancelActionMode = onCancelActionMode,
            scrollBehavior = scrollBehavior,
            navigateUp = navigateUp,
            onMultiDeleteClicked = onMultiDeleteClicked,
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
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            actions = {
                val filterTint = LocalContentColor.current
                var groupExpanded by remember { mutableStateOf(false) }
                val onDownloadDismissRequest = { groupExpanded = false }
                GroupDropdownMenu(
                    expanded = groupExpanded,
                    groupByMode = groupByMode,
                    onDismissRequest = onDownloadDismissRequest,
                    onGroupClicked = onClickGroup,
                )
                var sortExpanded by remember { mutableStateOf(false) }
                val onSortDismissRequest = { sortExpanded = false }
                SortDropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = onSortDismissRequest,
                    onSortClicked = onClickSort,
                    sortState = sortState,
                    descendingOrder = descendingOrder,
                )
                AppBarActions(
                    listOf(
                        AppBar.Action(
                            title = stringResource(R.string.action_sort),
                            icon = Icons.Outlined.Sort,
                            iconTint = filterTint,
                            onClick = { sortExpanded = !sortExpanded },
                        ),
                        AppBar.Action(
                            title = stringResource(R.string.action_group),
                            icon = Icons.Outlined.DensitySmall,
                            onClick = { groupExpanded = !groupExpanded },
                        ),
                    ),
                )
            },
            scrollBehavior = scrollBehavior,
        )
        BackHandler(
            onBack = { onChangeSearchQuery(null) },
        )
    }
}

@Composable
private fun DownloadStatsActionAppBar(
    modifier: Modifier = Modifier,
    selected: List<DownloadStatManga>,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    onMultiDeleteClicked: (List<DownloadStatManga>) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    navigateUp: (() -> Unit)?,
) {
    AppBar(
        modifier = modifier,
        title = stringResource(R.string.label_download_stats),
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(R.string.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(R.string.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onInvertSelection,
                    ),
                    AppBar.Action(
                        title = stringResource(R.string.delete_downloads_for_manga),
                        icon = Icons.Outlined.Delete,
                        onClick = { onMultiDeleteClicked(selected) },
                    ),
                ),
            )
        },
        actionModeCounter = selected.size,
        scrollBehavior = scrollBehavior,
        navigateUp = navigateUp,
    )
}

@Composable
fun GroupDropdownMenu(
    expanded: Boolean,
    groupByMode: GroupByMode,
    onDismissRequest: () -> Unit,
    onGroupClicked: (GroupByMode) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        listOfNotNull(
            if (groupByMode != GroupByMode.NONE) GroupByMode.NONE to stringResource(R.string.action_ungroup) else null,
            if (groupByMode != GroupByMode.BY_CATEGORY) GroupByMode.BY_CATEGORY to stringResource(R.string.action_group_by_category) else null,
            if (groupByMode != GroupByMode.BY_SOURCE) GroupByMode.BY_SOURCE to stringResource(R.string.action_group_by_source) else null,
        ).map { (mode, string) ->
            DropdownMenuItem(
                text = { Text(text = string) },
                onClick = {
                    onGroupClicked(mode)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
fun SortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSortClicked: (SortingMode) -> Unit,
    sortState: SortingMode,
    descendingOrder: Boolean? = null,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        listOfNotNull(
            SortingMode.BY_ALPHABET to stringResource(R.string.action_sort_A_Z),
            SortingMode.BY_SIZE to stringResource(R.string.action_sort_size),
        ).map { (mode, string) ->
            SortItem(
                label = string,
                sortDescending = descendingOrder.takeIf { sortState == mode },
                onClick = { onSortClicked(mode) },
            )
        }
    }
}
