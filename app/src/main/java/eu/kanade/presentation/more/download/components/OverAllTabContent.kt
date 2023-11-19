package eu.kanade.presentation.more.download.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.components.StatsSection
import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

@Composable
fun DownloadStatOverviewSection(
    items: List<DownloadStatManga>,
) {
    StatsSection(MR.strings.label_overview_section) {
        Row {
            StatsOverviewItem(
                title = folderSizeText(items.sumOf { it.folderSize }),
                subtitle = stringResource(MR.strings.label_size),
                icon = Icons.Outlined.Storage,
            )
            StatsOverviewItem(
                title = items.sumOf { it.downloadChaptersCount }.toString(),
                subtitle = stringResource(MR.strings.chapters),
                icon = Icons.Outlined.Book,
            )
            StatsOverviewItem(
                title = items.filter { it.downloadChaptersCount > 0 }.size.toString(),
                subtitle = stringResource(MR.strings.manga),
                icon = Icons.Outlined.CollectionsBookmark,
            )
        }
    }
}

@Composable
fun DownloadStatsRow(
    data: List<DownloadStatOperation>,
) {
    StatsSection(MR.strings.downloaded_chapters) {
        Row {
            StatsItem(
                data.size.toString(),
                stringResource(MR.strings.label_total_chapters),
            )
            StatsItem(
                folderSizeText(data.sumOf { abs(it.size) }),
                stringResource(MR.strings.label_size),
            )
        }
    }
}

@Composable
fun DeletedStatsRow(
    data: List<DownloadStatOperation>,
) {
    StatsSection(MR.strings.deleted_chapters) {
        Row {
            StatsItem(
                abs(data.sumOf { it.units }).toString(),
                stringResource(MR.strings.label_total_chapters),
            )
            StatsItem(
                folderSizeText(abs(data.sumOf { it.size })),
                stringResource(MR.strings.label_size),
            )
        }
    }
}

@Composable
private fun folderSizeText(folderSize: Long): String {
    val context = LocalContext.current
    return Formatter.formatFileSize(context, folderSize)
}
