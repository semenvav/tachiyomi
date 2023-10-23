package eu.kanade.presentation.more.download.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.download.DownloadStatsScreenModel
import eu.kanade.presentation.more.download.DownloadStatsScreenState
import eu.kanade.presentation.more.download.GroupByMode
import eu.kanade.presentation.more.download.components.CategoryList
import eu.kanade.presentation.more.download.components.downloadStatUiItems
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun downloadStatsItemsTab(
    state: DownloadStatsScreenState,
    screenModel: DownloadStatsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current

    return TabContent(
        titleRes = R.string.label_download_enties_tab,
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(R.string.action_sort),
                icon = Icons.Outlined.Sort,
                onClick = { screenModel.openSettingsDialog() },
            ),
        ),
        actionModeActions = listOf(
            AppBar.Action(
                title = stringResource(R.string.action_select_all),
                icon = Icons.Outlined.SelectAll,
                onClick = { screenModel.toggleAllSelection(true) },
            ),
            AppBar.Action(
                title = stringResource(R.string.action_select_inverse),
                icon = Icons.Outlined.FlipToBack,
                onClick = { screenModel.invertSelection() },
            ),
            AppBar.Action(
                title = stringResource(R.string.delete_downloads_for_manga),
                icon = Icons.Outlined.Delete,
                onClick = { screenModel.showDeleteAlert(state.selected) },
            ),
        ),
        content = { contentPadding, snackBarHost ->
            if (state.items.isEmpty()) {
                EmptyScreen(
                    textResource = R.string.information_no_downloads,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                when (state.groupByMode) {
                    GroupByMode.NONE -> FastScrollLazyColumn(
                        contentPadding = contentPadding,
                    ) {
                        downloadStatUiItems(
                            items = state.processedItems(true),
                            selectionMode = state.selectionMode,
                            onCoverClick = { manga ->
                                navigator.push(
                                    MangaScreen(manga.libraryManga.manga.id),
                                )
                            },
                            onSelected = screenModel::toggleSelection,
                            onMassSelected = screenModel::toggleMassSelection,
                            onDeleteManga = screenModel::showDeleteAlert,
                            operations = state.downloadStatOperations,
                            getGraphPoints = { ops, weight, groupByMode ->
                                screenModel.formGraphData(
                                    ops,
                                    weight,
                                    groupByMode,
                                    context,
                                )
                            },
                            snackbarHostState = snackBarHost,
                            toggleExpanded = screenModel::toggleExpanded,
                        )
                    }

                    GroupByMode.BY_SOURCE -> {
                        CategoryList(
                            contentPadding = contentPadding,
                            selectionMode = state.selectionMode,
                            onCoverClick = { item ->
                                navigator.push(
                                    MangaScreen(item.libraryManga.manga.id),
                                )
                            },
                            onDeleteManga = screenModel::showDeleteAlert,
                            onGroupSelected = screenModel::groupSelection,
                            onSelected = screenModel::toggleSelection,
                            onMassSelected = screenModel::toggleMassSelection,
                            categoryMap = screenModel.categoryMap(
                                state.processedItems(true),
                                GroupByMode.BY_SOURCE,
                                state.sortMode,
                                state.descendingOrder,
                                defaultCategoryName = null,
                            ),
                            operations = state.downloadStatOperations,
                            getGraphPoints = { ops, weight, groupByMode ->
                                screenModel.formGraphData(
                                    ops,
                                    weight,
                                    groupByMode,
                                    context,
                                )
                            },
                            snackbarHostState = snackBarHost,
                            toggleExpanded = screenModel::toggleExpanded,
                        )
                    }

                    GroupByMode.BY_CATEGORY -> {
                        CategoryList(
                            contentPadding = contentPadding,
                            selectionMode = state.selectionMode,
                            onCoverClick = { item ->
                                navigator.push(
                                    MangaScreen(item.libraryManga.manga.id),
                                )
                            },
                            onDeleteManga = screenModel::showDeleteAlert,
                            onGroupSelected = screenModel::groupSelection,
                            onSelected = screenModel::toggleSelection,
                            onMassSelected = screenModel::toggleMassSelection,
                            categoryMap = screenModel.categoryMap(
                                items = state.processedItems(false),
                                groupMode = GroupByMode.BY_CATEGORY,
                                sortMode = state.sortMode,
                                descendingOrder = state.descendingOrder,
                                defaultCategoryName = stringResource(R.string.label_default),
                            ),
                            operations = state.downloadStatOperations,
                            getGraphPoints = { ops, weight, groupByMode ->
                                screenModel.formGraphData(
                                    ops,
                                    weight,
                                    groupByMode,
                                    context,
                                )
                            },
                            snackbarHostState = snackBarHost,
                            toggleExpanded = screenModel::toggleExpanded,
                        )
                    }
                }
            }
        },
    )
}
