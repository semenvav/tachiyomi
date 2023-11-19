package tachiyomi.domain.stat.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.domain.stat.repository.DownloadStatRepository

class GetDownloadStatOperations(
    private val repository: DownloadStatRepository,
) {

    suspend fun await(): List<DownloadStatOperation> {
        return repository.getStatOperations()
    }

    suspend fun subscribe(): Flow<List<DownloadStatOperation>> {
        return repository.getStatOperationsAsFlow()
    }
}
