package eu.kanade.presentation.more.download

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.download.data.DownloadStatManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.stat.interactor.GetDownloadStatOperations
import tachiyomi.domain.stat.model.DownloadStatOperation
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class DownloadStatsScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val getDownloadStatOperations: GetDownloadStatOperations = Injekt.get(),
) : StateScreenModel<DownloadStatsScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            mutableState.update { state ->
                state.copy(
                    items = getLibraryManga.await().map { libraryManga ->
                        val source = sourceManager.getOrStub(libraryManga.manga.source)
                        val path = downloadProvider.findMangaDir(
                            libraryManga.manga.title,
                            source,
                        )?.filePath
                        DownloadStatManga(
                            libraryManga = libraryManga,
                            folderSize = if (path != null) DiskUtil.getDirectorySize(File(path)) else 0,
                            downloadChaptersCount = downloadManager.getDownloadCount(libraryManga.manga),
                        )
                    },
                    downloadStatOperations = getDownloadStatOperations.await(),
                    isLoading = false,
                )
            }
        }

        screenModelScope.launchIO {
            getDownloadStatOperations.subscribe()
                .distinctUntilChanged()
                .collectLatest { operations ->
                    mutableState.update { state ->
                        val oldOperationsId = state.downloadStatOperations.map { it.id }.toHashSet()
                        val newOperations = operations
                            .mapNotNull { if (!oldOperationsId.contains(it.id)) it else null }
                            .groupBy { it.mangaId }
                        val newItems = state.items.map { item ->
                            if (newOperations.containsKey(item.libraryManga.id)) {
                                item.copy(
                                    folderSize = item.folderSize +
                                        newOperations[item.libraryManga.id]!!
                                            .sumOf { it.size },
                                    downloadChaptersCount = item.downloadChaptersCount +
                                        newOperations[item.libraryManga.id]!!.sumOf { it.units }.toInt(),
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

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<DownloadStatManga> = emptyList(),
        val downloadStatOperations: List<DownloadStatOperation> = emptyList(),
    ) {

        val uniqueItems: List<DownloadStatManga> by lazy {
            val uniqueIds = HashSet<Long>()
            val uniqueMangas = mutableListOf<DownloadStatManga>()
            for (manga in items) {
                if (uniqueIds.add(manga.libraryManga.manga.id)) {
                    uniqueMangas.add(manga)
                }
            }
            uniqueMangas
        }
    }
}
