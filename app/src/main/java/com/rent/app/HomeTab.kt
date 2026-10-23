package com.rent.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun monthLabel(key: String): String =
    runCatching { YearMonth.parse(key).format(MONTH_LABEL) }.getOrDefault(key)

@Composable
fun HomeTab(vm: RentViewModel) {
    val tenants by vm.tenants.collectAsState()
    val paid by vm.paidIds.collectAsState()
    val arrears by vm.arrears.collectAsState()
    val month by vm.month.collectAsState()
    val scope = rememberCoroutineScope()

    var payTenant by remember { mutableStateOf<Tenant?>(null) }
    var payArrear by remember { mutableStateOf<Arrear?>(null) }
    var message by remember { mutableStateOf<UiMsg?>(null) }

    Column(Modifier.fillMaxSize()) {
        AquaTitleBar("Home")

        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Rent for ${monthLabel(month.toString())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Aqua.TextDark
                    )
                    StatusMessage(message)
                }
            }

            if (tenants.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No tenants yet. Add one from the Tenants tab and its button will appear here.",
                        color = Aqua.TextMuted,
                        fontSize = 14.sp
                    )
                }
            }

            items(tenants, key = { it.id }) { t ->
                val isPaid = t.id in paid
                AquaTile(
                    label = t.label,
                    sub = if (isPaid) "Paid ✓" else null,
                    enabled = !isPaid,
                    onClick = {
                        message = null
                        payTenant = t
                    }
                )
            }

            if (arrears.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Arrears",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Aqua.Red
                    )
                }
                items(arrears, key = { "arrear-${it.id}" }) { a ->
                    AquaTile(
                        label = a.label,
                        sub = monthLabel(a.monthYear),
                        tone = AquaTone.Red,
                        onClick = {
                            message = null
                            payArrear = a
                        }
                    )
                }
            }
        }
    }

    payTenant?.let { t ->
        RentNoteDialog(
            subtitle = "${t.label} — ${monthLabel(month.toString())}",
            onDismiss = { payTenant = null },
            onSave = { note ->
                scope.launch {
                    vm.payRent(t.id, note)
                        .onSuccess { message = UiMsg("Rent recorded for ${t.label}.", false) }
                        .onFailure { message = UiMsg(it.message ?: "Could not record rent.", true) }
                    payTenant = null
                }
            }
        )
    }

    payArrear?.let { a ->
        RentNoteDialog(
            subtitle = "${a.label} — arrears for ${monthLabel(a.monthYear)}",
            onDismiss = { payArrear = null },
            onSave = { note ->
                scope.launch {
                    vm.payArrear(a, note)
                        .onSuccess {
                            message = UiMsg("Arrears recorded for ${a.label} (${monthLabel(a.monthYear)}).", false)
                        }
                        .onFailure { message = UiMsg(it.message ?: "Could not record arrears.", true) }
                    payArrear = null
                }
            }
        )
    }
}

@Composable
private fun RentNoteDialog(subtitle: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var note by remember { mutableStateOf("RENT PAID.") }
    AquaDialog(title = "Rent Note", onDismiss = onDismiss) {
        Text(subtitle, fontWeight = FontWeight.Bold, color = Aqua.TextDark)
        AquaField("Rent Note", note, { note = it }, lines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AquaButton("Save", onClick = { onSave(note) })
            AquaButton("Cancel", onClick = onDismiss, tone = AquaTone.Silver)
        }
    }
}
