package tachiyomi.domain.stat.interactor

import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.domain.stat.repository.DownloadStatRepository

class GetDownloadStatOperations(
    private val repository: DownloadStatRepository,
) {

    suspend fun await(): List<DownloadStatOperation> {
        return repository.getStatOperations()
    }
}
