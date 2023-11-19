package eu.kanade.presentation.more.download.data

import androidx.compose.runtime.Immutable
import tachiyomi.domain.library.model.LibraryManga

@Immutable
data class DownloadStatManga(
    val libraryManga: LibraryManga,
    val folderSize: Long = 0,
    val downloadChaptersCount: Int = 0,
)
