package eu.kanade.presentation.more.download.components.graphic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DensitySmall
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.download.Dialog
import eu.kanade.presentation.more.download.components.folderSizeText
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import java.util.stream.IntStream.range

@Composable
fun PointGraph(
    showSubLine: Boolean,
    getPoints: (GraphGroupByMode) -> List<GraphicPoint>,
    canvasHeight: Float,
    showControls: Boolean,
    supportLinesCount: Int,
    onColumnClick: ((Dialog?) -> Unit)?,
    columnWidth: Float = 80f,
) {
    var groupByMode by remember { mutableStateOf(GraphGroupByMode.NONE) }
    val points = getPoints(groupByMode)
    val minValue = points.minOf { it.coordinate }
    val maxValue = points.maxOf { it.coordinate }
    val normalizedPoints = scaleData(
        data = points,
        targetMax = canvasHeight,
    )
    val scope = rememberCoroutineScope()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    if (normalizedPoints.size * columnWidth >= screenWidthDp.value) {
        Column {
            val graphScrollState = rememberLazyListState()
            if (showControls) {
                var groupByModeMenuExpanded by remember { mutableStateOf(false) }

                GraphGroupDropdownMenu(
                    expanded = groupByModeMenuExpanded,
                    groupByMode = groupByMode,
                    onGroupClicked = { graphGroupByMode -> groupByMode = graphGroupByMode },
                    onDismissRequest = { groupByModeMenuExpanded = false },
                )

                Row(
                    modifier = Modifier.height(30.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        enabled = graphScrollState.canScrollBackward,
                        onClick = {
                            scope.launch {
                                graphScrollState.scrollToItem(0)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowRightAlt,
                            contentDescription = stringResource(R.string.scroll_to_start),
                            modifier = Modifier
                                .scale(scaleX = -1f, scaleY = 1f),
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                graphScrollState.scrollToItem(normalizedPoints.size - 1)
                            }
                        },
                        enabled = graphScrollState.canScrollForward,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowRightAlt,
                            contentDescription = stringResource(R.string.scroll_to_end),
                        )
                    }
                    IconButton(onClick = { groupByModeMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DensitySmall,
                            contentDescription = stringResource(R.string.action_group),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(canvasHeight.dp),
                    ) {
                        val spacing = 2f / (supportLinesCount - 1)
                        val textUnderLineDashBase = 20
                        val underLineDash = textUnderLineDashBase * 0.1f * (textUnderLineDashBase / canvasHeight)
                        for (i in range(1, supportLinesCount - 1)) {
                            Text(
                                modifier = Modifier.align(
                                    BiasAlignment(
                                        1f,
                                        i * spacing - 1 - underLineDash * ((supportLinesCount - i).toFloat() / supportLinesCount),
                                    ),
                                ),
                                text = folderSizeText((minValue + (maxValue - minValue) * ((supportLinesCount - 1 - i).toFloat() / (supportLinesCount - 1))).toLong()),
                            )
                        }
                        if (supportLinesCount >= 2) {
                            Text(
                                modifier = Modifier.align(BiasAlignment(1f, -1f - underLineDash)),
                                text = folderSizeText(maxValue.toLong()),
                            )
                            Text(
                                modifier = Modifier.align(
                                    BiasAlignment(
                                        1f,
                                        1f,
                                    ),
                                ),
                                text = folderSizeText(minValue.toLong()),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        LazyRow(
                            state = graphScrollState,
                        ) {
                            for (i in normalizedPoints.indices) {
                                item {
                                    CenterPointItemWithSubAndLines(
                                        point = normalizedPoints[i],
                                        leftPoint = if (i > 0) normalizedPoints[i - 1].coordinate else null,
                                        rightPoint = if (i < normalizedPoints.size - 1) normalizedPoints[i + 1].coordinate else null,
                                        columnHeight = canvasHeight,
                                        linesCount = supportLinesCount,
                                        onClick = onColumnClick,
                                        showSubLine = showSubLine,
                                        columnWidth = columnWidth,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else if (showControls) {
        Row {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((canvasHeight + 50).dp),
            ) {
                EmptyScreen(
                    textResource = R.string.no_enough_stat_data,
                    actions = if (groupByMode != GraphGroupByMode.NONE) {
                        listOf(
                            EmptyScreenAction(
                                stringResId = R.string.disable_stat_graph_grouping,
                                icon = Icons.Outlined.Cancel,
                                onClick = { groupByMode = GraphGroupByMode.NONE },
                            ),
                        )
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
fun CenterPointItemWithSubAndLines(
    point: GraphicPoint,
    leftPoint: Float?,
    rightPoint: Float?,
    columnHeight: Float,
    linesCount: Int,
    onClick: ((Dialog?) -> Unit)?,
    showSubLine: Boolean,
    columnWidth: Float,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .width(columnWidth.dp)
            .clickable(
                onClick = {
                    onClick?.invoke(point.dialog)
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .height(columnHeight.dp)
                .fillMaxWidth(),
        ) {
            val lineColor = LocalContentColor.current
            Canvas(
                modifier = Modifier
                    .height(columnHeight.dp)
                    .fillMaxWidth(),
            ) {
                val pointOffset = Offset(
                    with(density) { (columnWidth / 2).dp.toPx() },
                    with(density) { point.coordinate.dp.toPx() },
                )
                if (leftPoint != null) {
                    drawLine(
                        strokeWidth = 5f,
                        color = lineColor,
                        start = pointOffset,
                        end = Offset(
                            with(density) { ((columnWidth / 2) - columnWidth).dp.toPx() },
                            with(density) { leftPoint.dp.toPx() },
                        ),
                    )
                }
                if (rightPoint != null) {
                    drawLine(
                        strokeWidth = 5f,
                        color = lineColor,
                        start = pointOffset,
                        end = Offset(
                            with(density) { ((columnWidth / 2) + columnWidth).dp.toPx() },
                            with(density) { rightPoint.dp.toPx() },
                        ),
                    )
                }
                for (i in range(0, linesCount)) {
                    drawSupportLine((columnHeight / (linesCount - 1)) * i, density)
                }
            }
        }
        if (showSubLine) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = point.subLine,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

fun DrawScope.drawSupportLine(y: Float, density: Density) {
    drawLine(
        color = Color.Gray,
        start = Offset(
            with(density) { 0.dp.toPx() },
            with(density) { y.dp.toPx() },
        ),
        end = Offset(
            with(density) { 80.dp.toPx() },
            with(density) { y.dp.toPx() },
        ),
        alpha = 0.5F,
        strokeWidth = 5F,
    )
}

@Composable
fun GraphGroupDropdownMenu(
    expanded: Boolean,
    groupByMode: GraphGroupByMode,
    onDismissRequest: () -> Unit,
    onGroupClicked: (GraphGroupByMode) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        listOfNotNull(
            if (groupByMode != GraphGroupByMode.NONE) GraphGroupByMode.NONE to stringResource(R.string.action_ungroup) else null,
            if (groupByMode != GraphGroupByMode.BY_DAY) GraphGroupByMode.BY_DAY to stringResource(R.string.action_group_by_day) else null,
            if (groupByMode != GraphGroupByMode.BY_MONTH) GraphGroupByMode.BY_MONTH to stringResource(R.string.action_group_by_month) else null,
        ).map { (mode, string) ->
            DropdownMenuItem(
                text = { Text(text = string) },
                onClick = {
                    onGroupClicked(mode)
                    onDismissRequest()
                },
            )
        }
    }
}

fun scaleData(data: List<GraphicPoint>, targetMax: Float): List<GraphicPoint> {
    val minValue = data.minOf { it.coordinate }
    val maxValue = data.maxOf { it.coordinate } - minValue
    return data
        .map {
            it.copy(
                coordinate = it.coordinate - minValue,
            )
        }
        .map {
            it.copy(
                coordinate = it.coordinate / (maxValue / targetMax),
            )
        }
        .map {
            it.copy(
                coordinate = (it.coordinate * -1) + targetMax,
            )
        }
}

data class GraphicPoint(
    val coordinate: Float,
    val subLine: String,
    val dialog: Dialog? = null,
)

enum class GraphGroupByMode {
    NONE,
    BY_DAY,
    BY_MONTH,
}
