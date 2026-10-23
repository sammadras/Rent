package com.rent.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class RentViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(
        AppDatabase.get(app),
        app.getSharedPreferences("rent_prefs", Context.MODE_PRIVATE)
    )

    /** The month Home is currently showing. Refreshed on resume and once a minute. */
    val month = MutableStateFlow(YearMonth.now())

    val tenants: StateFlow<List<Tenant>> =
        repo.tenants.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ledger: StateFlow<List<LedgerEntry>> =
        repo.ledger.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val arrears: StateFlow<List<Arrear>> =
        repo.arrears.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activity: StateFlow<List<ActivityLogEntry>> =
        repo.activity.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val paidIds: StateFlow<List<Long>> =
        month.flatMapLatest { repo.paidTenantIds(it.toString()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refreshMonth() {
        viewModelScope.launch {
            val now = YearMonth.now()
            runCatching { repo.rollover(now) }
            month.value = now
        }
    }

    suspend fun addTenant(form: TenantForm): Result<Tenant> = repo.addTenant(form)

    suspend fun updateTenant(id: Long, form: TenantForm): Result<Tenant> =
        repo.updateTenant(id, form)

    suspend fun vacate(id: Long, note: String): Result<Tenant> = repo.vacate(id, note)

    suspend fun payRent(tenantId: Long, note: String): Result<Unit> =
        repo.payRent(tenantId, note, month.value)

    suspend fun payArrear(arrear: Arrear, note: String): Result<Unit> =
        repo.payArrear(arrear, note)
}
