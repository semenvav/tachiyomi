package eu.kanade.presentation.more.download.components.graphic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.download.GroupByMode
import eu.kanade.presentation.more.download.data.DownloadStatManga

@Composable
fun LazyItemScope.PieChartWithLegend(
    contentPadding: PaddingValues,
    getMap: (Boolean, GroupByMode) -> Map<String, List<DownloadStatManga>>,
) {
    var groupByMode by remember { mutableStateOf(GroupByMode.BY_CATEGORY) }

    val data = getMap(groupByMode == GroupByMode.BY_SOURCE, groupByMode)

    val total = data.values.sumOf { list -> list.sumOf { it.folderSize } }

    if (total > 0) {
        Column(
            modifier = Modifier
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
                .padding(
                    horizontal = 10.dp,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
            ) {
                IconButton(
                    onClick = {
                        when (groupByMode) {
                            GroupByMode.BY_SOURCE -> groupByMode = GroupByMode.BY_CATEGORY
                            GroupByMode.BY_CATEGORY -> groupByMode = GroupByMode.BY_SOURCE
                            else -> {}
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Swipe,
                        contentDescription = null,
                    )
                }
            }
            val colors = generateColors(data.keys.toList())
            Row(
                modifier = Modifier.fillParentMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f),
                ) {
                    PieChart(data = data, total = total, colors = colors)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    items(items = colors.entries.toList()) { entry ->
                        LegendItem(
                            name = entry.key,
                            color = entry.value,
                        )
                    }
                }
            }
        }
    }
}

fun generateColors(strings: List<String>): Map<String, Color> {
    val colorMap = mutableMapOf<String, Color>()
    val step = 360f / strings.size

    for ((index, str) in strings.withIndex()) {
        val hue = (step * index) % 360
        val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        colorMap[str] = color
    }

    return colorMap
}

@Composable
fun PieChart(data: Map<String, List<DownloadStatManga>>, total: Long, colors: Map<String, Color>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(100.dp, 500.dp)
            .aspectRatio(1f),
        onDraw = {
            val sorted = data.toList().sortedBy { pair -> pair.second.sumOf { it.folderSize } }
            var startAngle = 0f
            sorted.forEach { (name, value) ->
                val sweepAngle = (value.sumOf { it.folderSize }.toFloat() / total.toFloat()) * 360f
                drawArc(
                    color = colors[name]!!,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = size,
                )
                startAngle += sweepAngle
            }
        },
    )
}

@Composable
fun LegendItem(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color = color),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
