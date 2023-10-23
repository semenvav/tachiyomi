package eu.kanade.presentation.more.download.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.components.StatsSection
import eu.kanade.tachiyomi.R
import tachiyomi.domain.stat.model.DownloadStatOperation
import kotlin.math.abs

@Composable
fun DownloadStatOverviewSection(
    items: List<DownloadStatManga>,
) {
    StatsSection(R.string.label_overview_section) {
        Row {
            StatsOverviewItem(
                title = folderSizeText(items.sumOf { it.folderSize }),
                subtitle = stringResource(R.string.action_sort_size),
                icon = Icons.Outlined.Storage,
            )
            StatsOverviewItem(
                title = items.sumOf { it.downloadChaptersCount }.toString(),
                subtitle = stringResource(R.string.chapters),
                icon = Icons.Outlined.Book,
            )
            StatsOverviewItem(
                title = items.size.toString(),
                subtitle = stringResource(R.string.manga),
                icon = Icons.Outlined.CollectionsBookmark,
            )
        }
    }
}

@Composable
fun DownloadStatsRow(
    data: List<DownloadStatOperation>,
) {
    StatsSection(R.string.downloaded_chapters) {
        Row {
            StatsItem(
                data.size.toString(),
                stringResource(R.string.label_total_chapters),
            )
            StatsItem(
                folderSizeText(data.sumOf { abs(it.size) }),
                stringResource(R.string.action_sort_size),
            )
        }
    }
}

@Composable
fun DeletedStatsRow(
    data: List<DownloadStatOperation>,
) {
    StatsSection(R.string.deleted_chapters) {
        Row {
            StatsItem(
                data.size.toString(),
                stringResource(R.string.label_total_chapters),
            )
            StatsItem(
                folderSizeText(abs(data.sumOf { it.size })),
                stringResource(R.string.action_sort_size),
            )
        }
    }
}
