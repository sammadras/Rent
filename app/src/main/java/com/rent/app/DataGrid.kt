package com.rent.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class GridColumn<T>(
    val title: String,
    val width: Dp,
    val numeric: Boolean = false,
    val maxLines: Int = 3,
    val text: (T) -> String
)

private val ACTION_WIDTH = 92.dp

/**
 * Excel-like grid: horizontally scrollable, striped rows, and every column heading sorts the rows
 * (tap once for ascending, again for descending) using the small up/down triangles.
 * An optional last column holds a per-row action button (Edit / Vacate).
 */
@Composable
fun <T> DataGrid(
    rows: List<T>,
    columns: List<GridColumn<T>>,
    rowKey: (T) -> Any,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    action: @Composable (T) -> Unit = {}
) {
    var sortCol by remember { mutableIntStateOf(-1) }
    var ascending by remember { mutableStateOf(true) }

    val sorted = remember(rows, columns, sortCol, ascending) {
        if (sortCol !in columns.indices) {
            rows
        } else {
            val col = columns[sortCol]
            val comparator: Comparator<T> =
                if (col.numeric) {
                    compareBy<T> { col.text(it).toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
                } else {
                    compareBy(String.CASE_INSENSITIVE_ORDER) { col.text(it) }
                }
            rows.sortedWith(if (ascending) comparator else comparator.reversed())
        }
    }

    val actionWidth = if (actionTitle != null) ACTION_WIDTH else 0.dp
    val totalWidth = columns.fold(actionWidth) { acc, c -> acc + c.width }

    Box(
        modifier = modifier
            .border(1.dp, Aqua.GridLine)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .width(totalWidth)
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFFF7F7F7), Color(0xFFD6D6D6))))
                    .gridDividers(columns.map { it.width })
            ) {
                columns.forEachIndexed { index, col ->
                    HeaderCell(
                        title = col.title,
                        width = col.width,
                        active = sortCol == index,
                        ascending = ascending,
                        onClick = {
                            if (sortCol == index) {
                                ascending = !ascending
                            } else {
                                sortCol = index
                                ascending = true
                            }
                        }
                    )
                }
                if (actionTitle != null) {
                    Box(
                        modifier = Modifier
                            .width(ACTION_WIDTH)
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(actionTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Aqua.TextDark)
                    }
                }
            }

            if (sorted.isEmpty()) {
                Text(
                    text = "No records.",
                    color = Aqua.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(sorted, key = { _, row -> rowKey(row) }) { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .background(if (index % 2 == 0) Color.White else Aqua.Stripe)
                            .gridDividers(columns.map { it.width }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEach { col ->
                            Text(
                                text = col.text(row),
                                fontSize = 13.sp,
                                color = Aqua.TextDark,
                                maxLines = col.maxLines,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .width(col.width)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        if (actionTitle != null) {
                            Box(
                                modifier = Modifier
                                    .width(ACTION_WIDTH)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                action(row)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Thin vertical lines between cells, drawn once per row. */
private fun Modifier.gridDividers(widths: List<Dp>): Modifier = this.drawBehind {
    var x = 0f
    for (w in widths) {
        x += w.toPx()
        drawLine(Aqua.GridLine, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
    }
    drawLine(
        Aqua.GridLine,
        Offset(0f, size.height),
        Offset(size.width, size.height),
        1.dp.toPx()
    )
}

@Composable
private fun HeaderCell(
    title: String,
    width: Dp,
    active: Boolean,
    ascending: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Aqua.TextDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "▲",
                fontSize = 7.sp,
                lineHeight = 8.sp,
                color = if (active && ascending) Aqua.BlueDark else Color(0xFFB0B0B0)
            )
            Text(
                "▼",
                fontSize = 7.sp,
                lineHeight = 8.sp,
                color = if (active && !ascending) Aqua.BlueDark else Color(0xFFB0B0B0)
            )
        }
    }
}
