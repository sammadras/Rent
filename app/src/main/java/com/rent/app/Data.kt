package com.rent.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Locale

// ---------------------------------------------------------------------------
// Tables
// ---------------------------------------------------------------------------

@Entity(tableName = "Tenant")
data class Tenant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val tenantName: String,
    val shopOrRoomNo: String,
    val mobile: String,
    val tenancyStartDate: String,
    val tenancyEndDate: String,
    val rent: Double?,
    val advance: Double?,
    val address: String,
    val ebNumber: String,
    val note: String
)

@Entity(tableName = "TenantsArchive")
data class ArchivedTenant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalTenantId: Long,
    val label: String,
    val tenantName: String,
    val shopOrRoomNo: String,
    val mobile: String,
    val tenancyStartDate: String,
    val tenancyEndDate: String,
    val rent: Double?,
    val advance: Double?,
    val address: String,
    val ebNumber: String,
    val note: String,
    val vacationNote: String,
    val archivedAt: String
)

/** Ledger columns shown to the user: Timestamp, MonthYear, Label, Rent Note. tenantId is internal. */
@Entity(tableName = "Ledger", indices = [Index(value = ["tenantId", "monthYear"])])
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val timestamp: String,
    val monthYear: String,
    val label: String,
    val rentNote: String
)

/** A month whose rent was not recorded. Shown as a red button under "Arrears" on Home. */
@Entity(tableName = "Arrear", indices = [Index(value = ["tenantId", "monthYear"])])
data class Arrear(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val label: String,
    val monthYear: String
)

@Entity(tableName = "ActivityLog")
data class ActivityLogEntry(
    @PrimaryKey(autoGenerate = true) val activitySequenceNo: Long = 0,
    val timestamp: String,
    val beforeImage: String,
    val afterImage: String
)

// ---------------------------------------------------------------------------
// Record "images" used by the activity log (one text, one field per line)
// ---------------------------------------------------------------------------

fun fmtNum(d: Double?): String = when {
    d == null -> ""
    d % 1.0 == 0.0 -> d.toLong().toString()
    else -> String.format(Locale.US, "%.2f", d)
}

fun Tenant.image(): String = listOf(
    "Label: $label",
    "Tenant Name: $tenantName",
    "Shop or Room No: $shopOrRoomNo",
    "Mobile: $mobile",
    "Tenancy Start Date: $tenancyStartDate",
    "Tenancy End Date: $tenancyEndDate",
    "Rent: ${fmtNum(rent)}",
    "Advance: ${fmtNum(advance)}",
    "Address: $address",
    "EB Number: $ebNumber",
    "Note: $note"
).joinToString("\n")

fun LedgerEntry.image(): String = listOf(
    "Timestamp: $timestamp",
    "MonthYear: $monthYear",
    "Label: $label",
    "Rent Note: $rentNote"
).joinToString("\n")

fun Arrear.image(): String = listOf(
    "Label: $label",
    "MonthYear: $monthYear",
    "Status: Arrears"
).joinToString("\n")

// ---------------------------------------------------------------------------
// DAO + database
// ---------------------------------------------------------------------------

@Dao
interface RentDao {
    @Query("SELECT * FROM Tenant ORDER BY id")
    fun tenants(): Flow<List<Tenant>>

    @Query("SELECT * FROM Tenant ORDER BY id")
    suspend fun tenantsOnce(): List<Tenant>

    @Query("SELECT * FROM Tenant WHERE id = :id")
    suspend fun tenant(id: Long): Tenant?

    @Query("SELECT COUNT(*) FROM Tenant WHERE LOWER(label) = LOWER(:label) AND id != :excludeId")
    suspend fun labelCount(label: String, excludeId: Long): Int

    @Insert
    suspend fun insertTenant(tenant: Tenant): Long

    @Update
    suspend fun updateTenant(tenant: Tenant)

    @Delete
    suspend fun deleteTenant(tenant: Tenant)

    @Insert
    suspend fun insertArchive(archived: ArchivedTenant): Long

    @Query("SELECT * FROM Ledger ORDER BY id DESC")
    fun ledger(): Flow<List<LedgerEntry>>

    @Insert
    suspend fun insertLedger(entry: LedgerEntry): Long

    @Query("SELECT DISTINCT tenantId FROM Ledger WHERE monthYear = :month")
    fun paidTenantIds(month: String): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM Ledger WHERE tenantId = :tenantId AND monthYear = :month")
    suspend fun paidCount(tenantId: Long, month: String): Int

    @Query("SELECT * FROM Arrear ORDER BY monthYear, id")
    fun arrears(): Flow<List<Arrear>>

    @Query("SELECT COUNT(*) FROM Arrear WHERE tenantId = :tenantId AND monthYear = :month")
    suspend fun arrearCount(tenantId: Long, month: String): Int

    @Insert
    suspend fun insertArrear(arrear: Arrear): Long

    @Delete
    suspend fun deleteArrear(arrear: Arrear)

    @Query("UPDATE Arrear SET label = :label WHERE tenantId = :tenantId")
    suspend fun relabelArrears(tenantId: Long, label: String)

    @Insert
    suspend fun insertLog(entry: ActivityLogEntry): Long

    @Query("SELECT * FROM ActivityLog ORDER BY activitySequenceNo DESC")
    fun activity(): Flow<List<ActivityLogEntry>>
}

@Database(
    entities = [
        Tenant::class,
        ArchivedTenant::class,
        LedgerEntry::class,
        Arrear::class,
        ActivityLogEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rent.db"
                ).build().also { instance = it }
            }
    }
}
