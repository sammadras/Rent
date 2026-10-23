package com.rent.app

import java.time.LocalDate

/** Editable text state for the add / edit tenant forms. */
data class TenantForm(
    val label: String = "",
    val tenantName: String = "",
    val shopOrRoomNo: String = "",
    val mobile: String = "",
    val start: String = LocalDate.now().toString(),
    val end: String = LocalDate.now().plusMonths(11).toString(),
    val rent: String = "",
    val advance: String = "",
    val address: String = "",
    val ebNumber: String = "",
    val note: String = "",
    /** true once the user typed in the end date, so changing the start date no longer overwrites it */
    val endManual: Boolean = false
) {
    /** Returns a user-facing error message, or null when the form is valid. */
    fun parseError(): String? {
        if (label.isBlank()) return "Label is required."
        if (tenantName.isBlank()) return "Tenant Name is required."
        val s = parseDate(start) ?: return "Tenancy Start Date must look like 2026-10-15 (yyyy-MM-dd)."
        val e = parseDate(end) ?: return "Tenancy End Date must look like 2027-09-15 (yyyy-MM-dd)."
        if (e.isBefore(s)) return "Tenancy End Date cannot be before the Start Date."
        if (rent.isNotBlank()) {
            val r = rent.trim().toDoubleOrNull() ?: return "Rent must be a number."
            if (r < 0) return "Rent cannot be negative."
        }
        if (advance.isNotBlank()) {
            val a = advance.trim().toDoubleOrNull() ?: return "Advance must be a number."
            if (a < 0) return "Advance cannot be negative."
        }
        if (mobile.isNotBlank() && mobile.count { it.isDigit() } < 7) {
            return "Mobile number looks too short."
        }
        return null
    }

    fun toTenant(id: Long): Tenant = Tenant(
        id = id,
        label = label.trim(),
        tenantName = tenantName.trim(),
        shopOrRoomNo = shopOrRoomNo.trim(),
        mobile = mobile.trim(),
        tenancyStartDate = parseDate(start)?.toString() ?: start.trim(),
        tenancyEndDate = parseDate(end)?.toString() ?: end.trim(),
        rent = rent.trim().toDoubleOrNull(),
        advance = advance.trim().toDoubleOrNull(),
        address = address.trim(),
        ebNumber = ebNumber.trim(),
        note = note.trim()
    )
}

fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s.trim()) }.getOrNull()

fun Tenant.toForm(): TenantForm = TenantForm(
    label = label,
    tenantName = tenantName,
    shopOrRoomNo = shopOrRoomNo,
    mobile = mobile,
    start = tenancyStartDate,
    end = tenancyEndDate,
    rent = fmtNum(rent),
    advance = fmtNum(advance),
    address = address,
    ebNumber = ebNumber,
    note = note,
    endManual = true
)
