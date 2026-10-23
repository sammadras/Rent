package com.rent.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

data class LedgerFilter(
    val monthYear: String = "",
    val label: String = "",
    val rentNote: String = ""
) {
    val isEmpty: Boolean
        get() = monthYear.isBlank() && label.isBlank() && rentNote.isBlank()
}

private val MONTH_INPUT_PATTERNS = listOf(
    "yyyy-MM", "MMM yyyy", "MMMM yyyy", "MMM-yyyy", "MMMM-yyyy", "MM/yyyy", "M/yyyy"
)

/** Accepts "2026-10", "Oct 2026", "October 2026", "10/2026" ... and returns the stored "yyyy-MM". */
private fun normalizeMonth(input: String): String {
    val text = input.trim()
    for (pattern in MONTH_INPUT_PATTERNS) {
        val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter(Locale.ENGLISH)
        val parsed = runCatching { YearMonth.parse(text, formatter) }.getOrNull()
        if (parsed != null) return parsed.toString()
    }
    return text
}

/** MonthYear and Label must match; Rent Note matches when it contains every keyword typed. */
private fun LedgerEntry.matches(f: LedgerFilter): Boolean {
    if (f.monthYear.isNotBlank() && monthYear != normalizeMonth(f.monthYear)) return false
    if (f.label.isNotBlank() && !label.equals(f.label.trim(), ignoreCase = true)) return false
    if (f.rentNote.isNotBlank()) {
        val haystack = rentNote.lowercase()
        val words = f.rentNote.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (!words.all { haystack.contains(it) }) return false
    }
    return true
}

private val ledgerColumns: List<GridColumn<LedgerEntry>> = listOf(
    GridColumn<LedgerEntry>("Timestamp", 150.dp) { it.timestamp },
    GridColumn<LedgerEntry>("MonthYear", 100.dp) { it.monthYear },
    GridColumn<LedgerEntry>("Label", 120.dp) { it.label },
    GridColumn<LedgerEntry>("Rent Note", 280.dp) { it.rentNote }
)

private val logColumns: List<GridColumn<ActivityLogEntry>> = listOf(
    GridColumn<ActivityLogEntry>("ActivitySequenceNo", 110.dp, numeric = true) {
        it.activitySequenceNo.toString()
    },
    GridColumn<ActivityLogEntry>("Timestamp", 150.dp) { it.timestamp },
    GridColumn<ActivityLogEntry>("BeforeImage", 270.dp, maxLines = Int.MAX_VALUE) { it.beforeImage },
    GridColumn<ActivityLogEntry>("AfterImage", 270.dp, maxLines = Int.MAX_VALUE) { it.afterImage }
)

@Composable
fun LedgerTab(vm: RentViewModel) {
    val ledger by vm.ledger.collectAsState()
    val activity by vm.activity.collectAsState()

    var filter by remember { mutableStateOf<LedgerFilter?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    BackHandler(enabled = showLog) { showLog = false }

    val rows = remember(ledger, filter) {
        val f = filter
        if (f == null) ledger else ledger.filter { it.matches(f) }
    }

    Column(Modifier.fillMaxSize()) {
        AquaTitleBar(if (showLog) "Activity Log" else "Ledger")

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!showLog) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AquaButton("Search", onClick = { showSearch = true })
                    if (filter != null) {
                        AquaButton("Clear", onClick = { filter = null }, tone = AquaTone.Silver)
                    }
                    Spacer(Modifier.weight(1f))
                    AquaButton("Activity Log", onClick = { showLog = true }, tone = AquaTone.Silver)
                }
                if (filter != null) {
                    Text(
                        "Showing ${rows.size} of ${ledger.size} rows",
                        color = Aqua.TextMuted,
                        fontSize = 13.sp
                    )
                }
                DataGrid(
                    rows = rows,
                    columns = ledgerColumns,
                    rowKey = { it.id },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                Row {
                    AquaButton("Back to Ledger", onClick = { showLog = false }, tone = AquaTone.Silver)
                }
                DataGrid(
                    rows = activity,
                    columns = logColumns,
                    rowKey = { it.activitySequenceNo },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            initial = filter ?: LedgerFilter(),
            onDismiss = { showSearch = false },
            onSearch = { f ->
                filter = if (f.isEmpty) null else f
                showSearch = false
            }
        )
    }
}

@Composable
private fun SearchDialog(
    initial: LedgerFilter,
    onDismiss: () -> Unit,
    onSearch: (LedgerFilter) -> Unit
) {
    var monthYear by remember { mutableStateOf(initial.monthYear) }
    var label by remember { mutableStateOf(initial.label) }
    var rentNote by remember { mutableStateOf(initial.rentNote) }

    AquaDialog(title = "Search Ledger", onDismiss = onDismiss) {
        AquaField("MonthYear", monthYear, { monthYear = it }, placeholder = "e.g. 2026-10 or Oct 2026")
        AquaField("Label", label, { label = it })
        AquaField("Rent Note", rentNote, { rentNote = it }, placeholder = "keywords")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AquaButton("Search", onClick = { onSearch(LedgerFilter(monthYear, label, rentNote)) })
            AquaButton("Cancel", onClick = onDismiss, tone = AquaTone.Silver)
        }
    }
}
