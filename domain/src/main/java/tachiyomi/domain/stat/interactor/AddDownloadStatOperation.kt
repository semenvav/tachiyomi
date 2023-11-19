package tachiyomi.domain.stat.interactor

import tachiyomi.domain.stat.model.DownloadStatOperation
import tachiyomi.domain.stat.repository.DownloadStatRepository

class AddDownloadStatOperation(
    private val repository: DownloadStatRepository,
) {

    suspend fun await(actions: DownloadStatOperation) {
        repository.insert(actions)
    }
}
