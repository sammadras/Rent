package com.rent.app

import android.content.SharedPreferences
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val STAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private const val KEY_LAST_MONTH = "lastMonth"

fun nowStamp(): String = LocalDateTime.now().format(STAMP_FORMAT)

class Repository(
    private val db: AppDatabase,
    private val prefs: SharedPreferences
) {
    private val dao = db.dao()

    val tenants: Flow<List<Tenant>> = dao.tenants()
    val ledger: Flow<List<LedgerEntry>> = dao.ledger()
    val arrears: Flow<List<Arrear>> = dao.arrears()
    val activity: Flow<List<ActivityLogEntry>> = dao.activity()

    fun paidTenantIds(month: String): Flow<List<Long>> = dao.paidTenantIds(month)

    private suspend fun log(before: String, after: String) {
        dao.insertLog(
            ActivityLogEntry(timestamp = nowStamp(), beforeImage = before, afterImage = after)
        )
    }

    private suspend fun validate(form: TenantForm, excludeId: Long): String? {
        form.parseError()?.let { return it }
        val label = form.label.trim()
        if (dao.labelCount(label, excludeId) > 0) {
            return "Label \"$label\" is already used by another tenant."
        }
        return null
    }

    // ----- Tenants ---------------------------------------------------------

    suspend fun addTenant(form: TenantForm): Result<Tenant> = runCatching {
        validate(form, 0)?.let { throw IllegalArgumentException(it) }
        db.withTransaction {
            val draft = form.toTenant(0)
            val saved = draft.copy(id = dao.insertTenant(draft))
            log("", saved.image())
            saved
        }
    }

    suspend fun updateTenant(id: Long, form: TenantForm): Result<Tenant> = runCatching {
        validate(form, id)?.let { throw IllegalArgumentException(it) }
        db.withTransaction {
            val old = dao.tenant(id) ?: throw IllegalStateException("This tenant no longer exists.")
            val updated = form.toTenant(id)
            dao.updateTenant(updated)
            if (old.label != updated.label) dao.relabelArrears(id, updated.label)
            log(old.image(), updated.image())
            updated
        }
    }

    suspend fun vacate(id: Long, vacationNote: String): Result<Tenant> = runCatching {
        db.withTransaction {
            val t = dao.tenant(id) ?: throw IllegalStateException("This tenant no longer exists.")
            val note = vacationNote.trim()
            dao.insertArchive(
                ArchivedTenant(
                    originalTenantId = t.id,
                    label = t.label,
                    tenantName = t.tenantName,
                    shopOrRoomNo = t.shopOrRoomNo,
                    mobile = t.mobile,
                    tenancyStartDate = t.tenancyStartDate,
                    tenancyEndDate = t.tenancyEndDate,
                    rent = t.rent,
                    advance = t.advance,
                    address = t.address,
                    ebNumber = t.ebNumber,
                    note = t.note,
                    vacationNote = note,
                    archivedAt = nowStamp()
                )
            )
            dao.deleteTenant(t)
            log(t.image(), t.image() + "\nVacation Note: " + note)
            t
        }
    }

    // ----- Rent / arrears --------------------------------------------------

    suspend fun payRent(tenantId: Long, note: String, month: YearMonth): Result<Unit> = runCatching {
        db.withTransaction {
            val t = dao.tenant(tenantId) ?: throw IllegalStateException("This tenant no longer exists.")
            val key = month.toString()
            if (dao.paidCount(tenantId, key) > 0) {
                throw IllegalStateException("Rent for ${t.label} is already recorded for this month.")
            }
            val entry = LedgerEntry(
                tenantId = tenantId,
                timestamp = nowStamp(),
                monthYear = key,
                label = t.label,
                rentNote = note.trim()
            )
            dao.insertLedger(entry)
            log("", entry.image())
        }
    }

    suspend fun payArrear(arrear: Arrear, note: String): Result<Unit> = runCatching {
        db.withTransaction {
            val entry = LedgerEntry(
                tenantId = arrear.tenantId,
                timestamp = nowStamp(),
                monthYear = arrear.monthYear,
                label = arrear.label,
                rentNote = note.trim()
            )
            dao.insertLedger(entry)
            dao.deleteArrear(arrear)
            log(arrear.image(), entry.image())
        }
    }

    /**
     * Called whenever the app opens / resumes. For every month that has finished since the last
     * run, each tenant without a rent entry gets an Arrear (red button on Home).
     * Paid buttons become enabled again automatically because "paid" is looked up per month.
     */
    suspend fun rollover(now: YearMonth) {
        val last = prefs.getString(KEY_LAST_MONTH, null)
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        if (last == null) {
            prefs.edit().putString(KEY_LAST_MONTH, now.toString()).apply()
            return
        }
        var m: YearMonth = last
        while (m.isBefore(now)) {
            closeMonth(m)
            m = m.plusMonths(1)
            prefs.edit().putString(KEY_LAST_MONTH, m.toString()).apply()
        }
    }

    private suspend fun closeMonth(month: YearMonth) {
        val key = month.toString()
        db.withTransaction {
            for (t in dao.tenantsOnce()) {
                if (dao.paidCount(t.id, key) == 0 && dao.arrearCount(t.id, key) == 0) {
                    val arrear = Arrear(tenantId = t.id, label = t.label, monthYear = key)
                    dao.insertArrear(arrear)
                    log("", arrear.image())
                }
            }
        }
    }
}
