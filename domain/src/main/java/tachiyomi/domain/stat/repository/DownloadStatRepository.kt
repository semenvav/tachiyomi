package tachiyomi.domain.stat.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.stat.model.DownloadStatOperation

interface DownloadStatRepository {

    suspend fun getStatOperations(): List<DownloadStatOperation>

    suspend fun getStatOperationsAsFlow(): Flow<List<DownloadStatOperation>>

    suspend fun insert(operation: DownloadStatOperation)
}
