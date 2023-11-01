package eu.kanade.presentation.more.download.components

import android.text.format.Formatter
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.more.download.components.graphic.GraphGroupByMode
import eu.kanade.presentation.more.download.components.graphic.GraphicPoint
import eu.kanade.presentation.more.download.components.graphic.PointGraph
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.launch
import okhttp3.internal.format
import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.selectedBackground
import kotlin.math.abs

@Composable
fun CategoryList(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    onCoverClick: (DownloadStatManga) -> Unit,
    onDeleteManga: (List<DownloadStatManga>) -> Unit,
    onGroupSelected: (List<DownloadStatManga>) -> Unit,
    onSelected: (DownloadStatManga) -> Unit,
    onMassSelected: (DownloadStatManga) -> Unit,
    categoryMap: Map<String, List<DownloadStatManga>>,
    toggleExpanded: (DownloadStatManga) -> Unit,
    operations: List<DownloadStatOperation>,
    getGraphPoints: (List<DownloadStatOperation>, Long, GraphGroupByMode) -> List<GraphicPoint>,
    snackbarHostState: SnackbarHostState,
) {
    val categoryExpandedMapSaver: Saver<MutableMap<String, Boolean>, *> = Saver(
        save = { map -> map.toMap() },
        restore = { map -> mutableStateMapOf(*map.toList().toTypedArray()) },
    )

    val expanded = rememberSaveable(
        saver = categoryExpandedMapSaver,
        init = { mutableStateMapOf(*categoryMap.keys.toList().map { it to false }.toTypedArray()) },
    )

    FastScrollLazyColumn(contentPadding = contentPadding) {
        categoryMap.forEach { (category, items) ->
            downloadStatGroupUiItem(
                title = category,
                items = items,
                selectionMode = selectionMode,
                onCoverClick = onCoverClick,
                onSelected = onSelected,
                onMassSelected = onMassSelected,
                onDeleteManga = onDeleteManga,
                onGroupSelected = onGroupSelected,
                expanded = expanded,
                operations = operations,
                getGraphPoints = getGraphPoints,
                snackbarHostState = snackbarHostState,
                toggleExpanded = toggleExpanded,
            )
        }
    }
}

fun LazyListScope.downloadStatGroupUiItem(
    items: List<DownloadStatManga>,
    selectionMode: Boolean,
    onSelected: (DownloadStatManga) -> Unit,
    onMassSelected: (DownloadStatManga) -> Unit,
    onCoverClick: (DownloadStatManga) -> Unit,
    title: String,
    onDeleteManga: (List<DownloadStatManga>) -> Unit,
    onGroupSelected: (List<DownloadStatManga>) -> Unit,
    toggleExpanded: (DownloadStatManga) -> Unit,
    expanded: MutableMap<String, Boolean>,
    operations: List<DownloadStatOperation>,
    getGraphPoints: (List<DownloadStatOperation>, Long, GraphGroupByMode) -> List<GraphicPoint>,
    snackbarHostState: SnackbarHostState,
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectedBackground(!items.fastAny { !it.selected })
                .combinedClickable(
                    onClick = {
                        expanded[title] = if (expanded[title] == null) {
                            false
                        } else {
                            !expanded[title]!!
                        }
                    },
                    onLongClick = { onGroupSelected(items) },
                )
                .padding(horizontal = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                    color = LocalContentColor.current.copy(alpha = 1f),
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var textHeight by remember { mutableIntStateOf(0) }
                    Text(
                        text = folderSizeText(items.sumOf { downloadStatManga -> downloadStatManga.folderSize }),
                    )
                    DotSeparatorText()
                    Text(
                        text = format(
                            stringResource(R.string.group_info),
                            items.sumOf { it.downloadChaptersCount },
                            items.size,
                        ),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 1f),
                        onTextLayout = { textHeight = it.size.height },
                        modifier = Modifier
                            .weight(weight = 1f, fill = false),
                    )
                }
            }
            Icon(
                imageVector = if (expanded[title] == true) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (expanded[title] == true) {
        downloadStatUiItems(
            items = items,
            onCoverClick = onCoverClick,
            selectionMode = selectionMode,
            onDeleteManga = onDeleteManga,
            onSelected = onSelected,
            onMassSelected = onMassSelected,
            toggleExpanded = toggleExpanded,
            operations = operations,
            getGraphPoints = getGraphPoints,
            snackbarHostState = snackbarHostState,
        )
    }
}

fun LazyListScope.downloadStatUiItems(
    items: List<DownloadStatManga>,
    selectionMode: Boolean,
    onSelected: (DownloadStatManga) -> Unit,
    onMassSelected: (DownloadStatManga) -> Unit,
    onCoverClick: (DownloadStatManga) -> Unit,
    onDeleteManga: (List<DownloadStatManga>) -> Unit,
    toggleExpanded: (DownloadStatManga) -> Unit,
    operations: List<DownloadStatOperation>,
    getGraphPoints: (List<DownloadStatOperation>, Long, GraphGroupByMode) -> List<GraphicPoint>,
    snackbarHostState: SnackbarHostState,
) {
    items(
        items = items,
    ) { item ->

        val scope = rememberCoroutineScope()
        val noDataString = stringResource(R.string.no_stat_data)

        val mangaOperations = operations.filter { it.mangaId == item.libraryManga.id }

        DownloadStatUiItem(
            modifier = Modifier.animateItemPlacement(),
            onLongClick = {
                if (selectionMode) {
                    onMassSelected(item)
                } else {
                    onSelected(item)
                }
            },
            onClick = {
                if (selectionMode) {
                    onSelected(item)
                } else if (mangaOperations.isNotEmpty()) {
                    toggleExpanded(item)
                } else {
                    scope.launch { snackbarHostState.showSnackbar(noDataString) }
                }
            },
            onCoverClick = { onCoverClick(item) },
            manga = item,
            onDeleteManga = { onDeleteManga(listOf(item)) }.takeIf { item.downloadChaptersCount > 0 },
            operations = mangaOperations,
            getGraphPoints = getGraphPoints,
        )
    }
}

@Composable
private fun DownloadStatUiItem(
    modifier: Modifier,
    manga: DownloadStatManga,
    onCoverClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteManga: (() -> Unit)?,
    operations: List<DownloadStatOperation>,
    getGraphPoints: (List<DownloadStatOperation>, Long, GraphGroupByMode) -> List<GraphicPoint>,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .selectedBackground(manga.selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        MangaBaseInfoRow(modifier, manga, onCoverClick, onDeleteManga)
        if (manga.expanded) {
            MangaDownloadStatSection(operations)
            PointGraph(
                getPoints = { groupByMode ->
                    getGraphPoints(
                        operations,
                        manga.folderSize,
                        groupByMode,
                    )
                },
                canvasHeight = 100f,
                supportLinesCount = 3,
                onColumnClick = null,
                showControls = false,
                showSubLine = false,
                columnWidth = 50f,
            )
        }
    }
}

@Composable
private fun MangaBaseInfoRow(
    modifier: Modifier,
    manga: DownloadStatManga,
    onCoverClick: () -> Unit,
    onDeleteManga: (() -> Unit)?,
) {
    Row(
        modifier = modifier
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Square(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxHeight(),
            data = manga.libraryManga.manga,
            onClick = onCoverClick,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
        ) {
            Text(
                text = manga.libraryManga.manga.title,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 1f),
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                Text(
                    text = folderSizeText(manga.folderSize),
                )
                DotSeparatorText()
                Text(
                    text = String.format(
                        "%d %s",
                        manga.downloadChaptersCount,
                        stringResource(R.string.chapters),
                    ),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 1f),
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
            }
        }
        if (onDeleteManga != null) {
            DownloadedIndicator(
                modifier = Modifier.padding(start = 4.dp),
                onClick = { onDeleteManga.invoke() },
            )
        }
    }
}

@Composable
fun MangaDownloadStatSection(
    items: List<DownloadStatOperation>,
) {
    Column {
        TitleRow(
            titles = listOf(
                stringResource(R.string.downloaded_chapters),
                items.filter { it.size > 0 }.sumOf { it.units }.toString(),
                folderSizeText(items.filter { it.size > 0 }.sumOf { it.size }),
            ),
            titleStyle = MaterialTheme.typography.bodyMedium,
        )
        TitleRow(
            titles = listOf(
                stringResource(R.string.deleted_chapters),
                items.filter { it.size < 0 }.sumOf { it.units }.toString(),
                folderSizeText(items.filter { it.size < 0 }.sumOf { abs(it.size) }),
            ),
            titleStyle = MaterialTheme.typography.bodyMedium,
        )
        SubTitleRow(
            subtitles = listOf(
                null,
                stringResource(R.string.label_total_chapters),
                stringResource(R.string.action_sort_size),
            ),
            subtitleStyle = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun DownloadedIndicator(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.action_delete),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun folderSizeText(folderSize: Long): String {
    val context = LocalContext.current
    return Formatter.formatFileSize(context, folderSize)
}

@Composable
fun TitleRow(
    titles: List<String?> = emptyList(),
    titleStyle: TextStyle,
) {
    Row {
        for (title in titles) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = titleStyle
                            .copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun SubTitleRow(
    subtitles: List<String?> = emptyList(),
    subtitleStyle: TextStyle,
) {
    Row {
        for (subtitle in subtitles) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = subtitleStyle
                            .copy(
                                color = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = SecondaryItemAlpha),
                            ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
