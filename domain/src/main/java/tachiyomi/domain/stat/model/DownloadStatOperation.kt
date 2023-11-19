package tachiyomi.domain.stat.model

import java.util.Date

data class DownloadStatOperation(
    val id: Long,
    val mangaId: Long?,
    val date: Long,
    val size: Long,
    val units: Long,
) {
    companion object {
        fun create() = DownloadStatOperation(
            id = -1,
            mangaId = -1,
            date = Date().time,
            size = -1,
            units = 1,
        )
    }
}
