package eu.kanade.presentation.more.download.data

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga

@Immutable
data class DownloadStatManga(
    val libraryManga: LibraryManga,
    val folderSize: Long = 0,
    val selected: Boolean = false,
    val expanded: Boolean = false,
    val source: Source,
    val category: Category,
    val downloadChaptersCount: Int = 0,
)
