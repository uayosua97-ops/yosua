package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLog
import com.example.data.model.Company
import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.data.remote.friendlyMessage
import com.example.data.repository.SuperAdminRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SuperAdminDashboardData(
    val companies: List<Company> = emptyList(),
    val users: List<CompanyUser> = emptyList(),
    val invites: List<CompanyInvite> = emptyList(),
    val logs: List<ActivityLog> = emptyList()
)

sealed interface SuperAdminUiState {
    data object Loading : SuperAdminUiState
    data class Ready(val data: SuperAdminDashboardData) : SuperAdminUiState
    data class Error(val message: String) : SuperAdminUiState
}

class SuperAdminViewModel(
    private val repository: SuperAdminRepository,
    private val currentUser: CompanyUser
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val uiState: StateFlow<SuperAdminUiState> = combine(
        repository.observeCompanies(),
        repository.observeAllUsers(),
        repository.observeAllInvites(),
        repository.observePlatformLogs()
    ) { companies, users, invites, logs ->
        SuperAdminUiState.Ready(
            SuperAdminDashboardData(
                companies = companies,
                users = users,
                invites = invites,
                logs = logs
            )
        ) as SuperAdminUiState
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("SuperAdminVM", "Error observing platform data", rootError)
        emit(SuperAdminUiState.Error(rootError.friendlyMessage("Gagal memuat data platform.")))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SuperAdminUiState.Loading
    )

    fun clearToast() {
        _toastMessage.value = null
    }

    fun createCompany(
        name: String,
        code: String,
        address: String,
        phone: String,
        email: String,
        spreadsheetId: String,
        appsScriptUrl: String,
        plan: String,
        tolerancePct: Double,
        defaultRatePerTon: Double
    ) {
        if (name.trim().length < 2 || code.trim().length < 2) {
            _toastMessage.value = "Nama dan kode perusahaan wajib diisi."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createCompany(
                actor = currentUser,
                name = name,
                code = code,
                address = address,
                phone = phone,
                email = email,
                spreadsheetId = spreadsheetId,
                appsScriptUrl = appsScriptUrl,
                subscriptionPlan = plan,
                shrinkageTolerancePct = tolerancePct,
                defaultRatePerTon = defaultRatePerTon
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Perusahaan $name berhasil didaftarkan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal mendaftarkan perusahaan.") }
            )
        }
    }

    fun updateCompany(
        company: Company,
        name: String,
        code: String,
        address: String,
        phone: String,
        email: String,
        spreadsheetId: String,
        appsScriptUrl: String,
        plan: String,
        tolerancePct: Double,
        defaultRatePerTon: Double
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val updated = company.copy(
                name = name.trim(),
                code = code.trim().uppercase(),
                address = address.trim(),
                phone = phone.trim(),
                email = email.trim(),
                spreadsheetId = spreadsheetId.trim(),
                appsScriptUrl = appsScriptUrl.trim(),
                subscriptionPlan = plan,
                shrinkageTolerancePct = tolerancePct,
                defaultRatePerTon = defaultRatePerTon
            )
            val res = repository.updateCompany(currentUser, updated)
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Konfigurasi ${updated.name} diperbarui." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal memperbarui perusahaan.") }
            )
        }
    }

    fun toggleCompanyStatus(company: Company) {
        viewModelScope.launch {
            val res = repository.toggleCompanyActive(currentUser, company, !company.isActive)
            res.fold(
                onSuccess = {
                    _toastMessage.value = if (!company.isActive) {
                        "${company.name} diaktifkan."
                    } else {
                        "${company.name} dinonaktifkan."
                    }
                },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal mengubah status perusahaan.") }
            )
        }
    }

    fun inviteSpv(email: String, company: Company) {
        if (!email.contains("@")) {
            _toastMessage.value = "Masukkan email Google SPV yang valid."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createUserInvite(
                actor = currentUser,
                email = email,
                companyId = company.id,
                companyName = company.name,
                role = "SPV"
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Undangan SPV dikirim untuk $email (${company.name})." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal membuat undangan SPV.") }
            )
        }
    }

    fun toggleUserStatus(user: CompanyUser) {
        val next = if (user.status == "ACTIVE") "INACTIVE" else "ACTIVE"
        viewModelScope.launch {
            val res = repository.toggleUserStatus(currentUser, user, next)
            res.fold(
                onSuccess = { _toastMessage.value = "Status ${user.name} -> $next" },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal mengubah status akun.") }
            )
        }
    }
}
