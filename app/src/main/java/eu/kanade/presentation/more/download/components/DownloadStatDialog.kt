package eu.kanade.presentation.more.download.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.more.download.GroupByMode
import eu.kanade.presentation.more.download.SortingMode
import eu.kanade.tachiyomi.R
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import kotlin.math.abs

@Composable
fun DeleteAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(text = stringResource(R.string.are_you_sure))
        },
        text = {
            Text(text = stringResource(R.string.confirm_delete_entries))
        },
    )
}

@Composable
fun DownloadStatOperationInfoDialog(
    findManga: (Long?) -> Manga?,
    onMangaClick: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    item: DownloadStatOperation,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        text = {
            val manga = findManga(item.mangaId)
            if (manga != null) {
                MangaOperationsRow(
                    manga = manga,
                    onMangaClick = { onMangaClick(manga.id) },
                    items = listOf(item),
                )
            } else {
                NoMangaOperationsRow(listOf(item))
            }
        },
    )
}

@Composable
fun DownloadStatOperationMultiInfoDialog(
    findManga: (Long?) -> Manga?,
    onMangaClick: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    items: List<DownloadStatOperation>,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        text = {
            LazyColumn {
                items(items = items.groupBy { it.mangaId }.values.toList()) { items ->
                    val manga = findManga(items.first().mangaId)
                    if (manga != null) {
                        MangaOperationsRow(
                            manga = manga,
                            onMangaClick = { onMangaClick(manga.id) },
                            items = items,
                        )
                    } else {
                        NoMangaOperationsRow(items)
                    }
                }
            }
        },
    )
}

@Composable
private fun MangaOperationsRow(
    manga: Manga,
    onMangaClick: () -> Unit,
    items: List<DownloadStatOperation>,
) {
    Row(
        modifier = Modifier.height(120.dp),
    ) {
        MangaCover.Book(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxHeight(),
            data = manga,
            onClick = onMangaClick,
        )
        MangaInfoColumn(items, manga.title)
    }
}

@Composable
private fun MangaInfoColumn(
    items: List<DownloadStatOperation>,
    title: String,
) {
    val downloadItems = items.filter { it.size > 0 }
    val deleteItems = items.filter { it.size < 0 }
    Column(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium),
    ) {
        Text(
            text = title,
            maxLines = 2,
            style = MaterialTheme.typography.titleSmall,
            color = LocalContentColor.current.copy(alpha = 1f),
            overflow = TextOverflow.Ellipsis,
        )
        if (downloadItems.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                Text(
                    text = String.format(
                        stringResource(R.string.download_stat_operation_downloaded),
                        downloadItems.sumOf { it.units },
                        folderSizeText(
                            folderSize = downloadItems.sumOf { it.size },
                        ),
                    ),
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 1f),
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
            }
        }
        if (deleteItems.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                Text(
                    text = String.format(
                        stringResource(R.string.download_stat_operation_deleted),
                        deleteItems.sumOf { it.units },
                        folderSizeText(
                            folderSize = abs(deleteItems.sumOf { it.size }),
                        ),
                    ),
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 1f),
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun NoMangaOperationsRow(
    items: List<DownloadStatOperation>,
) {
    Row {
        MangaInfoColumn(items, stringResource(R.string.entry_was_deleted))
    }
}

@Composable
fun DownloadStatOperationInfoStartDialog(
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        text = {
            Text(stringResource(R.string.start_operation_massage))
        },
    )
}

@Composable
fun DownloadStatSettingsDialog(
    onDismissRequest: () -> Unit,
    sortMode: SortingMode,
    descendingOrder: Boolean,
    groupByMode: GroupByMode,
    showNotDownloaded: Boolean,
    onSort: (SortingMode) -> Unit,
    onGroup: (GroupByMode) -> Unit,
    toggleShowNotDownloaded: () -> Unit,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(R.string.action_sort),
            stringResource(R.string.action_group),
            stringResource(R.string.action_settings),
        ),
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> SortPage(
                    onSort = onSort,
                    descendingOrder = descendingOrder,
                    sortMode = sortMode,

                )
                1 -> GroupPage(
                    onGroup = onGroup,
                    groupByMode = groupByMode,
                )
                2 -> SettingPage(
                    toggleShowNotDownloaded = toggleShowNotDownloaded,
                    showNotDownloaded = showNotDownloaded,
                )
            }
        }
    }
}

@Composable
private fun SortPage(
    onSort: (SortingMode) -> Unit,
    descendingOrder: Boolean,
    sortMode: SortingMode,
) {
    listOfNotNull(
        SortingMode.BY_ALPHABET to stringResource(R.string.action_sort_alpha),
        SortingMode.BY_SIZE to stringResource(R.string.action_sort_size),
        SortingMode.BY_CHAPTERS to stringResource(R.string.chapters),
    ).map { (mode, string) ->
        SortItem(
            label = string,
            sortDescending = descendingOrder.takeIf { sortMode == mode },
            onClick = { onSort(mode) },
        )
    }
}

@Composable
private fun GroupPage(
    onGroup: (GroupByMode) -> Unit,
    groupByMode: GroupByMode,
) {
    listOf(
        GroupByMode.NONE to stringResource(R.string.action_ungroup),
        GroupByMode.BY_CATEGORY to stringResource(R.string.action_group_by_category),
        GroupByMode.BY_SOURCE to stringResource(R.string.action_group_by_source),
    ).map { (mode, string) ->
        RadioItem(
            label = string,
            selected = groupByMode == mode,
            onClick = { onGroup(mode) },
        )
    }
}

@Composable
private fun SettingPage(
    toggleShowNotDownloaded: () -> Unit,
    showNotDownloaded: Boolean,
) {
    CheckboxItem(
        label = stringResource(R.string.show_entries_without_downloaded),
        checked = showNotDownloaded,
        onClick = toggleShowNotDownloaded,
    )
}
