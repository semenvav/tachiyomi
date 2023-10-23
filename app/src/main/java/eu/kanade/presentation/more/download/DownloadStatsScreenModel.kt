package eu.kanade.presentation.more.download

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.preference.asState
import eu.kanade.presentation.more.download.components.graphic.GraphGroupByMode
import eu.kanade.presentation.more.download.components.graphic.GraphicPoint
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.preference.toggle
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.stat.interactor.GetDownloadStatOperations
import tachiyomi.domain.stat.model.DownloadStatOperation
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap

class DownloadStatsScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
    private val getDownloadStatOperations: GetDownloadStatOperations = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<DownloadStatsScreenState>(DownloadStatsScreenState()) {

    var activeCategoryIndex: Int by preferenceStore.getInt("downloadStatSelectedTab", 0).asState(coroutineScope)

    private lateinit var lastSelectedManga: LibraryManga

    init {
        coroutineScope.launchIO {
            val sortMode = preferenceStore.getEnum("sort_mode", SortingMode.BY_ALPHABET).get()
            mutableState.update { state ->
                val categories = getCategories.await().associateBy { group -> group.id }
                val operations = getDownloadStatOperations.await()
                state.copy(
                    items = getLibraryManga.await().filter { libraryManga ->
                        (downloadManager.getDownloadCount(libraryManga.manga) > 0) || operations.any { it.mangaId == libraryManga.id }
                    }.mapNotNull { libraryManga ->
                        val source = sourceManager.getOrStub(libraryManga.manga.source)
                        val path = downloadProvider.findMangaDir(
                            libraryManga.manga.title,
                            source,
                        )?.filePath
                        val downloadChaptersCount = downloadManager.getDownloadCount(libraryManga.manga)
                        if (downloadChaptersCount == 0) {
                            DownloadStatManga(
                                libraryManga = libraryManga,
                                source = source,
                                category = categories[libraryManga.category]!!,
                            )
                        } else if (path != null) {
                            DownloadStatManga(
                                libraryManga = libraryManga,
                                source = source,
                                folderSize = downloadProvider.getFolderSize(path),
                                downloadChaptersCount = downloadChaptersCount,
                                category = categories[libraryManga.category]!!,
                            )
                        } else {
                            null
                        }
                    },
                    groupByMode = preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).get(),
                    sortMode = sortMode,
                    descendingOrder = preferenceStore.getBoolean("descending_order", false).get(),
                    searchQuery = preferenceStore.getString("search_query", "").get().takeIf { string -> string != "" },
                    downloadStatOperations = operations,
                    showNotDownloaded = preferenceStore.getBoolean("show_no_downloaded", false).get(),
                    isLoading = false,
                )
            }
            runSort(sortMode, true)
        }
    }

    fun runSort(
        mode: SortingMode,
        initSort: Boolean = false,
    ) {
        when (mode) {
            SortingMode.BY_ALPHABET -> sortByAlphabet(initSort)
            SortingMode.BY_SIZE -> sortBySize(initSort)
            SortingMode.BY_CHAPTERS -> sortByChapters(initSort)
        }
        preferenceStore.getEnum("sort_mode", SortingMode.BY_ALPHABET).set(mode)
    }

    fun runGroupBy(mode: GroupByMode) {
        when (mode) {
            GroupByMode.NONE -> unGroup()
            GroupByMode.BY_CATEGORY -> groupByCategory()
            GroupByMode.BY_SOURCE -> groupBySource()
        }
        preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).set(mode)
    }

    fun toggleShowNoDownload() {
        val showNotDownloaded = preferenceStore.getBoolean("show_no_downloaded").toggle()
        mutableState.update { state ->
            state.copy(
                showNotDownloaded = showNotDownloaded,
            )
        }
    }

    private fun sortByAlphabet(initSort: Boolean) {
        mutableState.update { state ->
            val descendingOrder = if (initSort) state.descendingOrder else if (state.sortMode == SortingMode.BY_ALPHABET) !state.descendingOrder else false
            preferenceStore.getBoolean("descending_order", false).set(descendingOrder)
            state.copy(
                items = if (descendingOrder) state.items.sortedByDescending { it.libraryManga.manga.title } else state.items.sortedBy { it.libraryManga.manga.title },
                descendingOrder = descendingOrder,
                sortMode = SortingMode.BY_ALPHABET,
            )
        }
    }

    private fun sortBySize(initSort: Boolean) {
        mutableState.update { state ->
            val descendingOrder = if (initSort) state.descendingOrder else if (state.sortMode == SortingMode.BY_SIZE) !state.descendingOrder else false
            preferenceStore.getBoolean("descending_order", false).set(descendingOrder)
            state.copy(
                items = if (descendingOrder) state.items.sortedByDescending { it.folderSize } else state.items.sortedBy { it.folderSize },
                descendingOrder = descendingOrder,
                sortMode = SortingMode.BY_SIZE,
            )
        }
    }

    private fun sortByChapters(initSort: Boolean) {
        mutableState.update { state ->
            val descendingOrder = if (initSort) state.descendingOrder else if (state.sortMode == SortingMode.BY_CHAPTERS) !state.descendingOrder else false
            preferenceStore.getBoolean("descending_order", false).set(descendingOrder)
            state.copy(
                items = if (descendingOrder) state.items.sortedByDescending { it.downloadChaptersCount } else state.items.sortedBy { it.downloadChaptersCount },
                descendingOrder = descendingOrder,
                sortMode = SortingMode.BY_CHAPTERS,
            )
        }
    }

    fun categoryMap(
        items: List<DownloadStatManga>,
        groupMode: GroupByMode,
        sortMode: SortingMode,
        descendingOrder: Boolean,
        defaultCategoryName: String?,
    ): Map<String, List<DownloadStatManga>> {
        val unsortedMap = when (groupMode) {
            GroupByMode.BY_CATEGORY -> items.groupBy {
                if (it.category.isSystemCategory && defaultCategoryName != null) { defaultCategoryName } else { it.category.name }
            }
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
            SortingMode.BY_CHAPTERS -> {
                val compareFun: (String) -> Comparable<*> = { it: String -> unsortedMap[it]?.sumOf { manga -> manga.downloadChaptersCount } ?: 0 }
                val sortedMap = TreeMap<String, List<DownloadStatManga>>(if (descendingOrder) { compareByDescending { compareFun(it) } } else { compareBy { compareFun(it) } })
                sortedMap.putAll(unsortedMap)
                sortedMap
            }
        }
    }

    private fun groupBySource() {
        mutableState.update {
            it.copy(
                groupByMode = GroupByMode.BY_SOURCE,
            )
        }
    }

    private fun groupByCategory() {
        mutableState.update {
            it.copy(
                groupByMode = GroupByMode.BY_CATEGORY,
            )
        }
    }

    private fun unGroup() {
        mutableState.update {
            it.copy(
                groupByMode = GroupByMode.NONE,
            )
        }
    }

    fun toggleSelection(
        item: DownloadStatManga,
    ) {
        lastSelectedManga = item.libraryManga
        mutableState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.libraryManga.id == item.libraryManga.id) {
                        it.copy(selected = !it.selected)
                    } else {
                        it
                    }
                },
            )
        }
    }

    fun toggleMassSelection(item: DownloadStatManga) {
        mutableState.update { state ->
            val processedItems = if (state.groupByMode == GroupByMode.NONE) {
                state.processedItems(false)
            } else {
                val temp = mutableListOf<DownloadStatManga>()
                categoryMap(
                    items = state.processedItems(false),
                    groupMode = state.groupByMode,
                    sortMode = state.sortMode,
                    descendingOrder = state.descendingOrder,
                    defaultCategoryName = null,
                ).map {
                    temp.addAll(it.value)
                }
                temp
            }
            val lastSelectedIndex =
                processedItems.indexOfFirst { it.libraryManga.id == lastSelectedManga.id && it.libraryManga.category == lastSelectedManga.category }
            val selectedIndex =
                processedItems.indexOfFirst { it.libraryManga.id == item.libraryManga.id && it.libraryManga.category == item.libraryManga.category }
            val itemsToChange = mutableSetOf(processedItems[lastSelectedIndex].libraryManga.id)
            val range = if (selectedIndex < lastSelectedIndex) {
                selectedIndex until lastSelectedIndex
            } else if (selectedIndex > lastSelectedIndex) {
                (lastSelectedIndex + 1) until (selectedIndex + 1)
            } else {
                IntRange.EMPTY
            }
            range.forEach {
                val betweenItem = processedItems[it]
                if (!betweenItem.selected) {
                    lastSelectedManga = betweenItem.libraryManga
                    itemsToChange.add(betweenItem.libraryManga.id)
                }
            }
            val newItems = state.items.map {
                if (it.libraryManga.id in itemsToChange) {
                    it.copy(selected = true)
                } else {
                    it
                }
            }
            state.copy(items = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newSelected = state.processedItems(false).map { manga -> manga.libraryManga.id }.toHashSet()
            val newItems = state.items.map {
                if (it.libraryManga.id in newSelected) {
                    it.copy(selected = selected)
                } else {
                    it
                }
            }
            state.copy(items = newItems)
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newSelected = state.processedItems(false).map { manga -> manga.libraryManga.id }.toHashSet()
            val newItems = state.items.map {
                if (it.libraryManga.id in newSelected) {
                    it.copy(selected = !it.selected)
                } else {
                    it
                }
            }
            state.copy(items = newItems)
        }
    }

    fun groupSelection(items: List<DownloadStatManga>) {
        val newSelected = items.map { manga -> manga.libraryManga.manga.id }.toHashSet()
        lastSelectedManga = items.last().libraryManga
        mutableState.update { state ->
            val newItems = state.items.map {
                if (it.libraryManga.manga.id in newSelected) {
                    it.copy(selected = !it.selected)
                } else {
                    it
                }
            }
            state.copy(items = newItems)
        }
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
        if (query != null) {
            preferenceStore.getString("search_query", "").set(query)
        } else {
            preferenceStore.getString("search_query", "").delete()
        }
    }
    fun findManga(id: Long?): Manga? {
        return runBlocking { if (id != null) getManga.await(id) else null }
    }

    fun deleteMangas(manga: List<DownloadStatManga>) {
        coroutineScope.launchNonCancellable {
            manga.forEach { manga ->
                downloadManager.deleteManga(manga.libraryManga.manga, manga.source)
            }
        }
        val toDeleteIds = manga.map { it.libraryManga.manga.id }.toHashSet()
        toggleAllSelection(false)
        mutableState.update { state ->
            state.copy(
                items = state.items.map { if (it.libraryManga.manga.id in toDeleteIds) it.copy(downloadChaptersCount = 0, folderSize = 0) else it },
            )
        }
    }

    fun showDeleteAlert(items: List<DownloadStatManga>) {
        mutableState.update { it.copy(dialog = Dialog.DeleteManga(items)) }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun openSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.SettingsSheet) }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun formGraphData(
        downloadStatOperations: List<DownloadStatOperation>,
        currentWeight: Long,
        graphGroupByMode: GraphGroupByMode,
        context: Context,
    ): List<GraphicPoint> {
        var weight = currentWeight.toFloat()
        val pointsList = mutableListOf<GraphicPoint>()
        for (i in downloadStatOperations.indices.reversed()) {
            weight -= downloadStatOperations[i].size
        }
        pointsList.add(
            GraphicPoint(
                coordinate = weight,
                subLine = context.getString(R.string.graph_start_point_subLine),
                dialog = Dialog.DownloadStatOperationStart,
            ),
        )
        when (graphGroupByMode) {
            GraphGroupByMode.NONE -> {
                for (i in downloadStatOperations) {
                    weight += i.size
                    pointsList.add(
                        GraphicPoint(
                            coordinate = weight,
                            subLine = Date(i.date).toRelativeString(
                                context = context,
                                range = 0,
                            ),
                            dialog = Dialog.DownloadStatOperationInfo(i),
                        ),
                    )
                }
            }
            GraphGroupByMode.BY_DAY -> {
                val dateMap = mutableMapOf<String, MutableList<DownloadStatOperation>>()
                for (i in downloadStatOperations) {
                    weight += i.size
                    val key = Date(i.date).toRelativeString(
                        context = context,
                        range = 0,
                    )
                    if (dateMap.containsKey(key)) {
                        dateMap[key]?.add(i)
                    } else {
                        dateMap[key] = mutableListOf(i)
                    }
                }
                for (i in dateMap) {
                    weight += i.value.sumOf { it.size }
                    pointsList.add(
                        GraphicPoint(
                            coordinate = weight,
                            subLine = i.key,
                            dialog = Dialog.MultiMangaDownloadStatOperationInfo(
                                i.value,
                            ),
                        ),
                    )
                }
            }
            GraphGroupByMode.BY_MONTH -> {
                val dateMap = mutableMapOf<String, MutableList<DownloadStatOperation>>()
                val dateFormat = SimpleDateFormat("MM.yyyy", Locale.US)
                for (i in downloadStatOperations) {
                    weight += i.size
                    val key = dateFormat.format(Date(i.date))
                    if (dateMap.containsKey(key)) {
                        dateMap[key]?.add(i)
                    } else {
                        dateMap[key] = mutableListOf(i)
                    }
                }
                for (i in dateMap) {
                    weight += i.value.sumOf { it.size }
                    pointsList.add(
                        GraphicPoint(
                            coordinate = weight,
                            subLine = i.key,
                            dialog = Dialog.MultiMangaDownloadStatOperationInfo(
                                i.value,
                            ),
                        ),
                    )
                }
            }
        }
        return pointsList
    }

    fun toggleExpanded(manga: DownloadStatManga) {
        mutableState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.libraryManga.id == manga.libraryManga.id && it.libraryManga.category == manga.libraryManga.category) {
                        it.copy(expanded = !it.expanded)
                    } else {
                        it
                    }
                },
            )
        }
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
    BY_CHAPTERS,
}

sealed class Dialog {
    data class DeleteManga(val items: List<DownloadStatManga>) : Dialog()
    data class DownloadStatOperationInfo(val downloadStatOperation: DownloadStatOperation) : Dialog()
    data class MultiMangaDownloadStatOperationInfo(val downloadStatOperation: List<DownloadStatOperation>) : Dialog()
    object DownloadStatOperationStart : Dialog()
    object SettingsSheet : Dialog()
}
