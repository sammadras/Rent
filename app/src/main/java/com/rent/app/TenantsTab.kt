package com.rent.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class TenantMode { None, Add, ViewAll, Edit, Vacate }

private val tenantColumns: List<GridColumn<Tenant>> = listOf(
    GridColumn<Tenant>("Label", 110.dp) { it.label },
    GridColumn<Tenant>("Tenant Name", 150.dp) { it.tenantName },
    GridColumn<Tenant>("Shop or Room No", 120.dp) { it.shopOrRoomNo },
    GridColumn<Tenant>("Mobile", 120.dp) { it.mobile },
    GridColumn<Tenant>("Tenancy Start Date", 125.dp) { it.tenancyStartDate },
    GridColumn<Tenant>("Tenancy End Date", 125.dp) { it.tenancyEndDate },
    GridColumn<Tenant>("Rent", 90.dp, numeric = true) { fmtNum(it.rent) },
    GridColumn<Tenant>("Advance", 90.dp, numeric = true) { fmtNum(it.advance) },
    GridColumn<Tenant>("Address", 200.dp) { it.address },
    GridColumn<Tenant>("EB Number", 120.dp) { it.ebNumber },
    GridColumn<Tenant>("Note", 220.dp) { it.note }
)

@Composable
fun TenantsTab(vm: RentViewModel) {
    val tenants by vm.tenants.collectAsState()
    var mode by remember { mutableStateOf(TenantMode.None) }
    var message by remember { mutableStateOf<UiMsg?>(null) }
    var editing by remember { mutableStateOf<Tenant?>(null) }
    var vacating by remember { mutableStateOf<Tenant?>(null) }

    fun select(m: TenantMode) {
        mode = m
        message = null
    }

    Column(Modifier.fillMaxSize()) {
        AquaTitleBar("Tenants")

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WithTooltip("Add New Tenant") {
                    AquaButton("+", onClick = { select(TenantMode.Add) }, fontSize = 20.sp)
                }
                AquaButton("View All", onClick = { select(TenantMode.ViewAll) })
                AquaButton("Edit", onClick = { select(TenantMode.Edit) })
                AquaButton("Vacate", onClick = { select(TenantMode.Vacate) })
            }

            StatusMessage(message)

            when (mode) {
                TenantMode.None -> Text(
                    "Tap + to add a tenant, or use View All, Edit and Vacate to manage existing ones.",
                    color = Aqua.TextMuted,
                    fontSize = 14.sp
                )

                TenantMode.Add -> AddTenantPanel(
                    vm = vm,
                    onAdded = { message = UiMsg(it, false) },
                    modifier = Modifier.weight(1f)
                )

                TenantMode.ViewAll -> DataGrid(
                    rows = tenants,
                    columns = tenantColumns,
                    rowKey = { it.id },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                TenantMode.Edit -> DataGrid(
                    rows = tenants,
                    columns = tenantColumns,
                    rowKey = { it.id },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    actionTitle = "Edit"
                ) { t ->
                    AquaButton("Edit", onClick = { editing = t }, height = 28.dp, fontSize = 12.sp)
                }

                TenantMode.Vacate -> DataGrid(
                    rows = tenants,
                    columns = tenantColumns,
                    rowKey = { it.id },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    actionTitle = "Vacate"
                ) { t ->
                    AquaButton(
                        "Vacate",
                        onClick = { vacating = t },
                        tone = AquaTone.Red,
                        height = 28.dp,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    editing?.let { t ->
        TenantEditDialog(tenant = t, vm = vm, onClose = { editing = null })
    }

    vacating?.let { t ->
        VacateDialog(
            tenant = t,
            vm = vm,
            onDismiss = { vacating = null },
            onDone = {
                vacating = null
                message = UiMsg(it, false)
            }
        )
    }
}

@Composable
private fun AddTenantPanel(vm: RentViewModel, onAdded: (String) -> Unit, modifier: Modifier) {
    var form by remember { mutableStateOf(TenantForm()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TenantFormFields(form) { form = it }
        error?.let { StatusMessage(UiMsg(it, true)) }
        AquaButton("Add", onClick = {
            scope.launch {
                vm.addTenant(form)
                    .onSuccess { saved ->
                        form = TenantForm()
                        error = null
                        onAdded("Tenant ${saved.label} added successfully.")
                    }
                    .onFailure { e -> error = e.message ?: "Could not add the tenant." }
            }
        })
    }
}

@Composable
private fun TenantEditDialog(tenant: Tenant, vm: RentViewModel, onClose: () -> Unit) {
    var form by remember(tenant.id) { mutableStateOf(tenant.toForm()) }
    var saved by remember(tenant.id) { mutableStateOf<Tenant?>(null) }
    var error by remember(tenant.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AquaDialog(title = "Edit Tenant", onDismiss = onClose) {
        val done = saved
        if (done == null) {
            TenantFormFields(form) { form = it }
            error?.let { StatusMessage(UiMsg(it, true)) }
            AquaButton("Update", onClick = {
                scope.launch {
                    vm.updateTenant(tenant.id, form)
                        .onSuccess { updated ->
                            saved = updated
                            error = null
                        }
                        .onFailure { e -> error = e.message ?: "Could not update the tenant." }
                }
            })
        } else {
            StatusMessage(UiMsg("Tenant ${done.label} updated successfully", false))
            Text(done.image(), fontSize = 14.sp, color = Aqua.TextDark)
            AquaButton("Close", onClick = onClose, tone = AquaTone.Silver)
        }
    }
}

@Composable
private fun VacateDialog(
    tenant: Tenant,
    vm: RentViewModel,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit
) {
    var note by remember(tenant.id) { mutableStateOf("") }
    var confirming by remember(tenant.id) { mutableStateOf(false) }
    var error by remember(tenant.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AquaDialog(title = "Vacate ${tenant.label}", onDismiss = onDismiss) {
        if (!confirming) {
            AquaField("Vacating Note", note, { note = it }, lines = 5)
            AquaButton("Proceed", onClick = { confirming = true })
        } else {
            Text(
                "Are you sure? If yes the Tenant will be moved to archive.",
                fontWeight = FontWeight.SemiBold,
                color = Aqua.TextDark
            )
            error?.let { StatusMessage(UiMsg(it, true)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AquaButton("Yes", tone = AquaTone.Red, onClick = {
                    scope.launch {
                        vm.vacate(tenant.id, note)
                            .onSuccess { onDone("Tenant ${it.label} moved to archive.") }
                            .onFailure { e -> error = e.message ?: "Could not vacate the tenant." }
                    }
                })
                AquaButton("No", tone = AquaTone.Silver, onClick = { confirming = false })
            }
        }
    }
}
