package tachiyomi.data.stat

import tachiyomi.domain.stat.model.DownloadStatOperation

object DownloadStatMapper {
    fun map(
        id: Long,
        mangaId: Long?,
        date: Long,
        size: Long,
        units: Long,
    ): DownloadStatOperation = DownloadStatOperation(
        id = id,
        mangaId = mangaId,
        date = date * 1000,
        size = size,
        units = units,
    )
}
