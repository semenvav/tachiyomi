package eu.kanade.presentation.more.download

import eu.kanade.presentation.more.download.data.DownloadStatManga

fun getDownloadStatMangaSort(
    sortingMode: SortingMode,
    sortDescending: Boolean,
): (
    DownloadStatManga,
    DownloadStatManga,
) -> Int {
    return when (sortingMode) {
        SortingMode.BY_ALPHABET -> when (sortDescending) {
            true -> { m1, m2 -> m1.libraryManga.manga.title.compareTo(m2.libraryManga.manga.title) }
            false -> { m1, m2 -> m2.libraryManga.manga.title.compareTo(m1.libraryManga.manga.title) }
        }
        SortingMode.BY_SIZE -> when (sortDescending) {
            true -> { m1, m2 -> m2.folderSize.compareTo(m1.folderSize) }
            false -> { m1, m2 -> m1.folderSize.compareTo(m2.folderSize) }
        }
        SortingMode.BY_CHAPTERS -> when (sortDescending) {
            true -> { m1, m2 -> m2.downloadChaptersCount.compareTo(m1.downloadChaptersCount) }
            false -> { m1, m2 -> m1.downloadChaptersCount.compareTo(m2.downloadChaptersCount) }
        }
    }
}
