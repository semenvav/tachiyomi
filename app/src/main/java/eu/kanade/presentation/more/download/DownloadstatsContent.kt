package eu.kanade.presentation.more.download

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.selectedBackground
import kotlin.math.ln
import kotlin.math.pow

fun LazyListScope.downloadStatUiItems(
    items: List<DownloadStatManga>,
    selectionMode: Boolean,
    onSelected: (DownloadStatManga, Boolean, Boolean, Boolean) -> Unit,
    onClick: (DownloadStatManga) -> Unit,
    onDeleteManga: (List<DownloadStatManga>) -> Unit,
) {
    items(
        items = items,
    ) { item ->
        DownloadStatUiItem(
            modifier = Modifier.animateItemPlacement(),
            selected = item.selected,
            onLongClick = {
                onSelected(item, !item.selected, true, true)
            },
            onClick = {
                when {
                    selectionMode -> onSelected(item, !item.selected, true, false)
                    else -> onClick(item)
                }
            },
            manga = item,
            onDeleteManga = { onDeleteManga(listOf(item)) }.takeIf { !selectionMode },
        )
    }
}

@Composable
private fun DownloadStatUiItem(
    modifier: Modifier,
    manga: DownloadStatManga,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteManga: (() -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    val textAlpha = 1f
    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Square(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxHeight(),
            data = manga.libraryManga.manga,
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
                color = LocalContentColor.current.copy(alpha = textAlpha),
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                FolderSizeText(manga.folderSize)
                DotSeparatorText()
                Text(
                    text = String.format("%d %s", manga.downloadChaptersCount, stringResource(R.string.chapters)),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = textAlpha),
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
            }
        }

        DownloadedIndicator(
            modifier = Modifier.padding(start = 4.dp),
            onClick = { onDeleteManga?.invoke() },
        )
    }
}

fun LazyListScope.downloadStatGroupUiItem(
    items: List<DownloadStatManga>,
    selectionMode: Boolean,
    onSelected: (DownloadStatManga, Boolean, Boolean, Boolean) -> Unit,
    onMangaClick: (DownloadStatManga) -> Unit,
    id: String,
    onDeleteManga: (List<DownloadStatManga>) -> Unit,
    onGroupSelected: (List<DownloadStatManga>) -> Unit,
    expanded: MutableMap<String, Boolean>,
) {
    stickyHeader {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectedBackground(!items.fastAny { !it.selected })
                .combinedClickable(
                    onClick = { expanded[id] = if (expanded[id] == null) { false } else { !expanded[id]!! } },
                    onLongClick = { onGroupSelected(items) },
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                id,
                style = typography.h6,
            )
            DotSeparatorText()
            FolderSizeText(items.fold(0L) { acc, downloadStatManga -> acc + downloadStatManga.folderSize })
            Icon(
                imageVector = if (expanded[id] == true) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (expanded[id] == true) {
        downloadStatUiItems(
            items = items,
            onClick = onMangaClick,
            selectionMode = selectionMode,
            onDeleteManga = onDeleteManga,
            onSelected = onSelected,
        )
    }
}

@Composable
fun FolderSizeText(folderSizeBytes: Long) {
    val units = arrayOf(R.string.memory_unit_b, R.string.memory_unit_kb, R.string.memory_unit_mb, R.string.memory_unit_gb)
    val base = 1024.0
    val exponent = (ln(folderSizeBytes.toDouble()) / ln(base)).toInt()
    val size = folderSizeBytes / base.pow(exponent.toDouble())
    Text(
        text = String.format("%.2f %s", size, stringResource(units[exponent])),
    )
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
fun CategoryList(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    onMangaClick: (DownloadStatManga) -> Unit,
    onDeleteManga: (List<DownloadStatManga>) -> Unit,
    onGroupSelected: (List<DownloadStatManga>) -> Unit,
    onSelected: (DownloadStatManga, Boolean, Boolean, Boolean) -> Unit,
    categoryMap: Map<String, List<DownloadStatManga>>,
) {
    val expanded = remember {
        mutableStateMapOf(*categoryMap.keys.toList().map { it to false }.toTypedArray())
    }
    FastScrollLazyColumn(contentPadding = contentPadding) {
        categoryMap.forEach { (category, items) ->
            downloadStatGroupUiItem(
                id = category,
                items = items,
                selectionMode = selectionMode,
                onMangaClick = onMangaClick,
                onSelected = onSelected,
                onDeleteManga = onDeleteManga,
                onGroupSelected = onGroupSelected,
                expanded = expanded,
            )
        }
    }
}
