package tachiyomi.domain.stat.repository

import tachiyomi.domain.stat.model.DownloadStatOperation

interface DownloadStatRepository {

    suspend fun getStatOperations(): List<DownloadStatOperation>

    suspend fun insert(operation: DownloadStatOperation)
}
