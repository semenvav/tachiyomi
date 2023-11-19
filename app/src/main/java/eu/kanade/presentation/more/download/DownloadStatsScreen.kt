package eu.kanade.presentation.more.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.download.components.DeletedStatsRow
import eu.kanade.presentation.more.download.components.DownloadStatOverviewSection
import eu.kanade.presentation.more.download.components.DownloadStatsRow
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class DownloadStatsScreen : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { DownloadStatsScreenModel() }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_download_stats),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when {
                state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
                else -> {
                    OverallStats(
                        state = state,
                        contentPadding = contentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallStats(
    state: DownloadStatsScreenModel.State,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            DownloadStatOverviewSection(state.uniqueItems)
        }
        item {
            DownloadStatsRow(state.downloadStatOperations.filter { it.size > 0 })
        }
        item {
            DeletedStatsRow(state.downloadStatOperations.filter { it.size < 0 })
        }
    }
}
