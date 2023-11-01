package eu.kanade.presentation.more.download.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.download.DownloadStatsScreenModel
import eu.kanade.presentation.more.download.DownloadStatsScreenState
import eu.kanade.presentation.more.download.SortingMode
import eu.kanade.presentation.more.download.components.DeletedStatsRow
import eu.kanade.presentation.more.download.components.DownloadStatOverviewSection
import eu.kanade.presentation.more.download.components.DownloadStatsRow
import eu.kanade.presentation.more.download.components.graphic.PieChartWithLegend
import eu.kanade.presentation.more.download.components.graphic.PointGraph
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.material.padding

@Composable
fun overAllStatsTab(
    state: DownloadStatsScreenState,
    screenModel: DownloadStatsScreenModel,
): TabContent {
    return TabContent(
        titleRes = R.string.label_download_stats_overall_tab,
        searchEnabled = false,
        content = { paddingValues, _ ->
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                item {
                    DownloadStatOverviewSection(state.uniqueItems())
                }
                item {
                    DownloadStatsRow(state.downloadStatOperations.filter { it.size > 0 })
                }
                item {
                    DeletedStatsRow(state.downloadStatOperations.filter { it.size < 0 })
                }
                item {
                    val defaultCategoryName = stringResource(R.string.label_default)

                    PieChartWithLegend(
                        getMap = {
                                unique, groupByMode ->
                            screenModel.categoryMap(
                                items = if (unique) state.uniqueItems() else state.items,
                                groupMode = groupByMode,
                                sortMode = SortingMode.BY_SIZE,
                                descendingOrder = true,
                                defaultCategoryName = defaultCategoryName,
                            )
                        },
                        contentPadding = paddingValues,
                    )
                }
                item {
                    val context = LocalContext.current
                    PointGraph(
                        getPoints = { groupByMode ->
                            screenModel.formGraphData(
                                state.downloadStatOperations,
                                state.items.sumOf { it.folderSize },
                                groupByMode,
                                context,
                            )
                        },
                        canvasHeight = 200f,
                        supportLinesCount = 5,
                        onColumnClick = screenModel::setDialog,
                        showSubLine = true,
                        showControls = true,
                    )
                }
            }
        },
    )
}
