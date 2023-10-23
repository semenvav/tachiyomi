package tachiyomi.data.stat

import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.domain.stat.repository.DownloadStatRepository

class DownloadStatRepositoryImpl(
    private val handler: DatabaseHandler,

) : DownloadStatRepository {
    override suspend fun getStatOperations(): List<DownloadStatOperation> {
        return handler.awaitList { download_statQueries.getStatOperations(DownloadStatActionMapper) }
    }

    override suspend fun insert(operation: DownloadStatOperation) {
        handler.await {
            download_statQueries.insert(
                mangaId = operation.mangaId,
                date = operation.date / 1000,
                size = operation.size,
                units = operation.units,
            )
        }
    }
}
