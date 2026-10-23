package com.rent.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** All tenant fields, in the order requested. Used by the Add form and the Edit dialog. */
@Composable
fun TenantFormFields(form: TenantForm, onChange: (TenantForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AquaField("Label", form.label, { onChange(form.copy(label = it)) })
        AquaField("Tenant Name", form.tenantName, { onChange(form.copy(tenantName = it)) })
        AquaField("Shop or Room No", form.shopOrRoomNo, { onChange(form.copy(shopOrRoomNo = it)) })
        AquaField(
            "Mobile", form.mobile, { onChange(form.copy(mobile = it)) },
            keyboardType = KeyboardType.Phone
        )
        DateField("Tenancy Start Date", form.start) { v ->
            var f = form.copy(start = v)
            if (!form.endManual) {
                parseDate(v)?.let { f = f.copy(end = it.plusMonths(11).toString()) }
            }
            onChange(f)
        }
        DateField("Tenancy End Date", form.end) { v ->
            onChange(form.copy(end = v, endManual = true))
        }
        AquaField(
            "Rent", form.rent, { onChange(form.copy(rent = it)) },
            keyboardType = KeyboardType.Decimal
        )
        AquaField(
            "Advance", form.advance, { onChange(form.copy(advance = it)) },
            keyboardType = KeyboardType.Decimal
        )
        AquaField("Address", form.address, { onChange(form.copy(address = it)) }, lines = 2)
        AquaField("EB Number", form.ebNumber, { onChange(form.copy(ebNumber = it)) })
        AquaField("Note", form.note, { onChange(form.copy(note = it)) }, lines = 5)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String, onValueChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }

    AquaField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = "yyyy-MM-dd",
        trailing = {
            IconButton(onClick = { open = true }) { Text("📅") }
        }
    )

    if (open) {
        val initial = parseDate(value) ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        onValueChange(
                            Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        )
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
