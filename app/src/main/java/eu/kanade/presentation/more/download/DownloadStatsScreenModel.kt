package eu.kanade.presentation.more.download

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.presentation.more.download.components.graphic.GraphGroupByMode
import eu.kanade.presentation.more.download.components.graphic.GraphicPoint
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.core.preference.toggle
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
import java.io.File
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

    var activeCategoryIndex: Int by preferenceStore.getInt("downloadStatSelectedTab", 0).asState(screenModelScope)

    private lateinit var lastSelectedManga: LibraryManga

    init {
        screenModelScope.launchIO {
            val sortMode = preferenceStore.getEnum("sort_mode", SortingMode.BY_ALPHABET).get()
            mutableState.update { state ->
                val categories = getCategories.await().associateBy { group -> group.id }
                state.copy(
                    items = getLibraryManga.await().map { libraryManga ->
                        val source = sourceManager.getOrStub(libraryManga.manga.source)
                        val path = downloadProvider.findMangaDir(
                            libraryManga.manga.title,
                            source,
                        )?.filePath
                        DownloadStatManga(
                            libraryManga = libraryManga,
                            source = source,
                            folderSize = if (path != null) DiskUtil.getDirectorySize(File(path)) else 0,
                            downloadChaptersCount = downloadManager.getDownloadCount(libraryManga.manga),
                            category = categories[libraryManga.category]!!,
                        )
                    },
                    groupByMode = preferenceStore.getEnum("group_by_mode", GroupByMode.NONE).get(),
                    sortMode = sortMode,
                    descendingOrder = preferenceStore.getBoolean("descending_order", false).get(),
                    searchQuery = preferenceStore.getString("search_query", "").get().takeIf { string -> string != "" },
                    downloadStatOperations = getDownloadStatOperations.await(),
                    showNotDownloaded = preferenceStore.getBoolean("show_no_downloaded", false).get(),
                    isLoading = false,
                )
            }
        }

        screenModelScope.launchIO {
            getDownloadStatOperations.subscribe().distinctUntilChanged().collectLatest { operations ->
                mutableState.update { state ->
                    val oldOperationsId = state.downloadStatOperations.map { it.id }.toHashSet()
                    val newOperations = operations.mapNotNull { if (!oldOperationsId.contains(it.id)) it else null }.groupBy { it.mangaId }
                    val newItems = state.items.map { item ->
                        if (newOperations.containsKey(item.libraryManga.id)) {
                            item.copy(
                                folderSize = item.folderSize + newOperations[item.libraryManga.id]!!.sumOf { it.size },
                                downloadChaptersCount = item.downloadChaptersCount + newOperations[item.libraryManga.id]!!.sumOf { it.units }.toInt(),
                            )
                        } else {
                            item
                        }
                    }
                    state.copy(
                        items = newItems,
                        downloadStatOperations = operations,
                    )
                }
            }
        }
    }

    fun changeSortMode(
        mode: SortingMode,
    ) {
        mutableState.update { state ->
            val descendingOrder = if (state.sortMode == mode) !state.descendingOrder else false
            preferenceStore.getBoolean("descending_order", false).set(descendingOrder)
            state.copy(
                descendingOrder = descendingOrder,
                sortMode = mode,
            )
        }
        preferenceStore.getEnum("sort_mode", SortingMode.BY_ALPHABET).set(mode)
    }

    fun changeGroupByMode(mode: GroupByMode) {
        mutableState.update {
            it.copy(
                groupByMode = mode,
            )
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
                val compareFun: (String) -> Comparable<*> = { unsortedMap[it]?.sumOf { manga -> manga.folderSize } ?: 0 }
                val sortedMap = TreeMap<String, List<DownloadStatManga>>(if (descendingOrder) { compareByDescending { compareFun(it) } } else { compareBy { compareFun(it) } })
                sortedMap.putAll(unsortedMap)
                sortedMap
            }
            SortingMode.BY_CHAPTERS -> {
                val compareFun: (String) -> Comparable<*> = { unsortedMap[it]?.sumOf { manga -> manga.downloadChaptersCount } ?: 0 }
                val sortedMap = TreeMap<String, List<DownloadStatManga>>(if (descendingOrder) { compareByDescending { compareFun(it) } } else { compareBy { compareFun(it) } })
                sortedMap.putAll(unsortedMap)
                sortedMap
            }
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
                categoryMap(
                    items = state.processedItems(false),
                    groupMode = state.groupByMode,
                    sortMode = state.sortMode,
                    descendingOrder = state.descendingOrder,
                    defaultCategoryName = null,
                ).flatMap { it.value }
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
        screenModelScope.launchNonCancellable {
            manga.forEach { manga ->
                downloadManager.deleteManga(manga.libraryManga.manga, manga.source)
            }
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
                                relative = false,
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
                        relative = false,
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
    data object DownloadStatOperationStart : Dialog()
    data object SettingsSheet : Dialog()
}
