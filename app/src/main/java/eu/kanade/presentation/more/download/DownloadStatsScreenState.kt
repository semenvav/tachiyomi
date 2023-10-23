package eu.kanade.presentation.more.download

import androidx.compose.runtime.Immutable
import eu.kanade.presentation.more.download.data.DownloadStatManga
import tachiyomi.domain.stat.model.DownloadStatOperation

@Immutable
data class DownloadStatsScreenState(
    val isLoading: Boolean = true,
    val items: List<DownloadStatManga> = emptyList(),
    val groupByMode: GroupByMode = GroupByMode.NONE,
    val sortMode: SortingMode = SortingMode.BY_ALPHABET,
    val descendingOrder: Boolean = false,
    val searchQuery: String? = null,
    val showNotDownloaded: Boolean = false,
    val dialog: Dialog? = null,
    val downloadStatOperations: List<DownloadStatOperation> = emptyList(),
) {
    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()

    fun uniqueItems(): List<DownloadStatManga> {
        val uniqueIds = HashSet<Long>()
        val uniqueMangas = mutableListOf<DownloadStatManga>()
        for (manga in items) {
            if (uniqueIds.add(manga.libraryManga.manga.id)) {
                uniqueMangas.add(manga)
            }
        }
        return uniqueMangas
    }

    fun processedItems(unique: Boolean): List<DownloadStatManga> {
        return (if (unique) uniqueItems() else items).filter {
            if (!showNotDownloaded) it.downloadChaptersCount > 0 else true
        }.filter {
            if (searchQuery != null) {
                it.libraryManga.manga.title.contains(searchQuery, true) ||
                    if (groupByMode == GroupByMode.BY_SOURCE) { it.source.name.contains(searchQuery, true) } else { false } ||
                    if (groupByMode == GroupByMode.BY_CATEGORY) { it.category.name.contains(searchQuery, true) } else { false }
            } else {
                true
            }
        }
    }
}
