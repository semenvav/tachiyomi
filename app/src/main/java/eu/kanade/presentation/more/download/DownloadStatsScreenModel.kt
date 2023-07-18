package eu.kanade.presentation.more.download

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.util.addOrRemove
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import kotlinx.coroutines.flow.update
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.TreeMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class DownloadStatsScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
) : StateScreenModel<DownloadStatsScreenState>(DownloadStatsScreenState()) {

    private val downloadCache: DownloadCache by injectLazy()

    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedMangaIds: HashSet<Long> = HashSet()

    init {
        coroutineScope.launchIO {
            val sortMode = preferenceStore.getEnum("sort_mode", SortingMode.BY_ALPHABET).get()
            mutableState.update {
                val categories = getCategories.await().associateBy { group -> group.id }
                it.copy(
                    items = getLibraryManga.await().filter { libraryManga ->
                        downloadCache.getDownloadCount(libraryManga.manga) > 0
                    }.map { libraryManga ->
                        val source = sourceManager.get(libraryManga.manga.source)!!
                        DownloadStatManga(
                            libraryManga = libraryManga,
                            selected = libraryManga.id in selectedMangaIds,
                            source = source,
                            folderSize = getFolderSize(
                                downloadProvider.findMangaDir(
                                    libraryManga.manga.title,
                                    source,
                                )?.filePath!!,
                            ),
                            downloadChaptersCount = downloadCache.getDownloadCount(libraryManga.manga),
                            category = categories[libraryManga.category]!!,
                        )
                    },
                    groupByMode = preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).get(),
                    sortMode = sortMode,
                    descendingOrder = preferenceStore.getBoolean("descending_order", false).get(),
                    searchQuery = preferenceStore.getString("search_query", "").get().takeIf { string -> string != "" },
                    isLoading = false,
                )
            }
            runSortAction(sortMode)
        }
    }

    private fun getFolderSize(path: String): Long {
        val file = File(path)
        var size: Long = 0

        if (file.exists()) {
            if (file.isDirectory) {
                val files = file.listFiles()
                if (files != null) {
                    for (childFile in files) {
                        size += if (childFile.isDirectory) {
                            getFolderSize(childFile.path)
                        } else {
                            getFileSize(childFile)
                        }
                    }
                }
            } else {
                size = getFileSize(file)
            }
        }

        return size
    }

    private fun getFileSize(file: File): Long {
        val archiveFormats = setOf(".zip", ".cbz", ".rar", ".cbr")
        return if (file.isDirectory) {
            getFolderSize(file.path)
        } else if (file.isFile) {
            file.length()
        } else if (file.extension.lowercase() in archiveFormats) {
            getZipFileSize(file)
        } else {
            0
        }
    }

    private fun getZipFileSize(file: File): Long {
        var size: Long = 0

        val zipFile = ZipFile(file)
        val entries = zipFile.entries()

        while (entries.hasMoreElements()) {
            val entry: ZipEntry = entries.nextElement()
            size += entry.size
        }

        zipFile.close()

        return size
    }

    fun runSortAction(mode: SortingMode) {
        when (mode) {
            SortingMode.BY_ALPHABET -> sortByAlphabet()
            SortingMode.BY_SIZE -> sortBySize()
        }
    }

    fun runGroupBy(mode: GroupByMode) {
        when (mode) {
            GroupByMode.NONE -> unGroup()
            GroupByMode.BY_CATEGORY -> groupByCategory()
            GroupByMode.BY_SOURCE -> groupBySource()
        }
    }

    private fun sortByAlphabet() {
        mutableState.update { state ->
            val descendingOrder = if (state.sortMode == SortingMode.BY_ALPHABET) !state.descendingOrder else false
            preferenceStore.getBoolean("descending_order", false).set(descendingOrder)
            state.copy(
                items = if (descendingOrder) state.items.sortedByDescending { it.libraryManga.manga.title } else state.items.sortedBy { it.libraryManga.manga.title },
                descendingOrder = descendingOrder,
                sortMode = SortingMode.BY_ALPHABET,
            )
        }
        preferenceStore.getEnum("sort_mode", SortingMode.BY_ALPHABET).set(SortingMode.BY_ALPHABET)
    }

    @Composable
    fun categoryMap(items: List<DownloadStatManga>, groupMode: GroupByMode, sortMode: SortingMode, descendingOrder: Boolean): Map<String, List<DownloadStatManga>> {
        val unsortedMap = when (groupMode) {
            GroupByMode.BY_CATEGORY -> items.groupBy { if (it.category.isSystemCategory) { stringResource(R.string.label_default) } else { it.category.name } }
            GroupByMode.BY_SOURCE -> items.groupBy { it.source.name }
            GroupByMode.NONE -> emptyMap()
        }
        return when (sortMode) {
            SortingMode.BY_ALPHABET -> {
                val sortedMap = TreeMap<String, List<DownloadStatManga>>(if (descendingOrder) { compareByDescending { it } } else { compareBy { it } })
                sortedMap.putAll(unsortedMap)
                sortedMap
            }
            SortingMode.BY_SIZE -> {
                val compareFun: (String) -> Comparable<*> = { it: String -> unsortedMap[it]?.sumOf { manga -> manga.folderSize } ?: 0 }
                val sortedMap = TreeMap<String, List<DownloadStatManga>>(if (descendingOrder) { compareByDescending { compareFun(it) } } else { compareBy { compareFun(it) } })
                sortedMap.putAll(unsortedMap)
                sortedMap
            }
        }
    }

    private fun sortBySize() {
        mutableState.update { state ->
            val descendingOrder = if (state.sortMode == SortingMode.BY_SIZE) !state.descendingOrder else false
            preferenceStore.getBoolean("descending_order", false).set(descendingOrder)
            state.copy(
                items = if (descendingOrder) state.items.sortedByDescending { it.folderSize } else state.items.sortedBy { it.folderSize },
                descendingOrder = descendingOrder,
                sortMode = SortingMode.BY_SIZE,
            )
        }
        preferenceStore.getEnum("sort_mode", SortingMode.BY_SIZE).set(SortingMode.BY_SIZE)
    }

    private fun groupBySource() {
        mutableState.update {
            it.copy(
                groupByMode = GroupByMode.BY_SOURCE,
            )
        }
        preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).set(GroupByMode.BY_SOURCE)
    }

    private fun groupByCategory() {
        mutableState.update {
            it.copy(
                groupByMode = GroupByMode.BY_CATEGORY,
            )
        }
        preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).set(GroupByMode.BY_CATEGORY)
    }

    private fun unGroup() {
        mutableState.update {
            it.copy(
                groupByMode = GroupByMode.NONE,
            )
        }
        preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).set(GroupByMode.NONE)
    }

    fun toggleSelection(
        item: DownloadStatManga,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val selectedIndex = indexOfFirst { it.libraryManga.manga.id == item.libraryManga.manga.id }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if (selectedItem.selected == selected) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedMangaIds.addOrRemove(item.libraryManga.manga.id, selected)

                if (selected && userSelected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Try to select the items in-between when possible
                        val range: IntRange
                        if (selectedIndex < selectedPositions[0]) {
                            range = selectedIndex + 1 until selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            range = (selectedPositions[1] + 1) until selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Just select itself
                            range = IntRange.EMPTY
                        }

                        range.forEach {
                            val inbetweenItem = get(it)
                            if (!inbetweenItem.selected) {
                                selectedMangaIds.add(inbetweenItem.libraryManga.manga.id)
                                set(it, inbetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (userSelected && !fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) {
                            selectedPositions[0] = indexOfFirst { it.selected }
                        } else if (selectedIndex == selectedPositions[1]) {
                            selectedPositions[1] = indexOfLast { it.selected }
                        }
                    } else {
                        if (selectedIndex < selectedPositions[0]) {
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            selectedPositions[1] = selectedIndex
                        }
                    }
                }
            }
            state.copy(items = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedMangaIds.addOrRemove(it.libraryManga.manga.id, selected)
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }

        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
        if (query != null) {
            preferenceStore.getString("search_query", "").set(query)
        }
    }

    fun deleteMangas(libraryMangas: List<DownloadStatManga>) {
        coroutineScope.launchNonCancellable {
            libraryMangas.forEach { manga ->
                val source = sourceManager.get(manga.libraryManga.manga.source) ?: return@forEach
                downloadManager.deleteManga(manga.libraryManga.manga, source)
            }
        }
        val set = libraryMangas.map { it.libraryManga.id }.toHashSet()
        toggleAllSelection(false)
        mutableState.update { state ->
            state.copy(
                items = state.items.filterNot { it.libraryManga.id in set },
            )
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedMangaIds.addOrRemove(it.libraryManga.manga.id, !it.selected)
                it.copy(selected = !it.selected)
            }
            state.copy(items = newItems)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun groupSelection(items: List<DownloadStatManga>) {
        val newSelected = items.map { manga -> manga.libraryManga.id }.toHashSet()
        selectedMangaIds.addAll(newSelected)
        mutableState.update { state ->
            val newItems = state.items.map {
                it.copy(selected = if (it.libraryManga.id in newSelected) !it.selected else it.selected)
            }
            state.copy(items = newItems)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }
}

enum class GroupByMode {
    NONE,
    BY_CATEGORY,
    BY_SOURCE,
}

enum class SortingMode {
    BY_ALPHABET,
    BY_SIZE,
}
