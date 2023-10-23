package tachiyomi.data.stat

import tachiyomi.domain.stat.model.DownloadStatOperation

val DownloadStatActionMapper: (Long, Long?, Long, Long, Long) ->
DownloadStatOperation = { id, mangaId, date, size, units ->
    DownloadStatOperation(
        id = id,
        mangaId = mangaId,
        date = date * 1000,
        size = size,
        units = units,
    )
}
