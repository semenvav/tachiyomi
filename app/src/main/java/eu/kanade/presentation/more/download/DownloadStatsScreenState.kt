package eu.kanade.presentation.more.download

import androidx.compose.runtime.Immutable

@Immutable
data class DownloadStatsScreenState(
    val isLoading: Boolean = true,
    val items: List<DownloadStatManga> = emptyList(),
    val groupByMode: GroupByMode = GroupByMode.NONE,
    val sortMode: SortingMode = SortingMode.BY_ALPHABET,
    val descendingOrder: Boolean = false,
    val searchQuery: String? = null,
) {
    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()
    val processedItems: List<DownloadStatManga>
        get() = search(items, searchQuery, groupByMode)

    fun search(items: List<DownloadStatManga>, searchQuery: String?, groupByMode: GroupByMode): List<DownloadStatManga> {
        return if (searchQuery != null) {
            items.filter { downloadStatManga ->
                downloadStatManga.libraryManga.manga.title.contains(searchQuery, true) ||
                    if (groupByMode == GroupByMode.BY_SOURCE) { downloadStatManga.source.name.contains(searchQuery, true) } else { false } ||
                    if (groupByMode == GroupByMode.BY_CATEGORY) { downloadStatManga.category.name.contains(searchQuery, true) } else { false }
            }
        } else {
            items
        }
    }
}
