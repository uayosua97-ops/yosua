package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLog
import com.example.data.model.Company
import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.data.model.Customer
import com.example.data.model.Driver
import com.example.data.model.Invoice
import com.example.data.model.MaintenanceRecord
import com.example.data.model.OperationalCost
import com.example.data.model.Shipment
import com.example.data.model.ShipmentStatus
import com.example.data.model.Vehicle
import com.example.data.model.WeighingRecord
import com.example.data.remote.SheetsSyncAdapter
import com.example.data.remote.friendlyMessage
import com.example.data.repository.TenantOperationsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompanyWorkspaceData(
    val company: Company? = null,
    val staff: List<CompanyUser> = emptyList(),
    val invites: List<CompanyInvite> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val shipments: List<Shipment> = emptyList(),
    val weighings: List<WeighingRecord> = emptyList(),
    val costs: List<OperationalCost> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val maintenance: List<MaintenanceRecord> = emptyList(),
    val logs: List<ActivityLog> = emptyList()
) {
    val activeShipmentsCount: Int
        get() = shipments.count {
            it.status in setOf("READY_TO_LOAD", "LOADING", "IN_TRANSIT", "ARRIVED", "UNLOADING")
        }
    val completedShipmentsCount: Int
        get() = shipments.count { it.status == "COMPLETED" }
    val pendingShipmentsCount: Int
        get() = shipments.count { it.status in setOf("DRAFT", "PENDING_APPROVAL", "ISSUE") }
    val totalCpoTonnage: Double
        get() = if (weighings.isNotEmpty()) {
            weighings.sumOf { it.billableTonnage }
        } else {
            shipments.filter { it.status != "CANCELLED" }.sumOf { it.targetTonnage }
        }
    val totalLoadTonnage: Double
        get() = weighings.sumOf { it.loadTonnage }
    val totalUnloadTonnage: Double
        get() = weighings.sumOf { it.unloadTonnage }
    val activeVehiclesCount: Int
        get() = vehicles.count { it.status in setOf("AVAILABLE", "IN_USE") }
    val activeDriversCount: Int
        get() = drivers.count { it.status != "INACTIVE" }
    val totalRevenue: Double
        get() = if (invoices.isNotEmpty()) {
            invoices.sumOf { it.totalAmount }
        } else {
            costs.sumOf { it.revenue }
        }
    val totalOperationalCost: Double
        get() = costs.sumOf { it.totalCost }
    val totalBilledAmount: Double
        get() = invoices.sumOf { it.totalAmount }
    val totalPaidAmount: Double
        get() = invoices.sumOf { it.paidAmount }
}

sealed interface WorkspaceUiState {
    data object Loading : WorkspaceUiState
    data class Ready(val data: CompanyWorkspaceData) : WorkspaceUiState
    data class Error(val message: String) : WorkspaceUiState
}

class CompanyWorkspaceViewModel(
    private val repository: TenantOperationsRepository,
    private val sheetsAdapter: SheetsSyncAdapter,
    val currentUser: CompanyUser
) : ViewModel() {

    private val companyId = currentUser.companyId

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val coreFlow: Flow<Triple<Company?, List<CompanyUser>, List<CompanyInvite>>> = combine(
        repository.observeCompany(companyId),
        repository.observeCompanyStaff(companyId),
        repository.observeCompanyInvites(companyId)
    ) { company, staff, invites -> Triple(company, staff, invites) }

    private val masterFlow: Flow<Triple<List<Customer>, List<Vehicle>, List<Driver>>> = combine(
        repository.observeCustomers(companyId),
        repository.observeVehicles(companyId),
        repository.observeDrivers(companyId)
    ) { customers, vehicles, drivers -> Triple(customers, vehicles, drivers) }

    private val opsFlow: Flow<Triple<List<Shipment>, List<WeighingRecord>, List<OperationalCost>>> = combine(
        repository.observeShipments(companyId),
        repository.observeWeighings(companyId),
        repository.observeOperationalCosts(companyId)
    ) { shipments, weighings, costs -> Triple(shipments, weighings, costs) }

    private val financeFlow: Flow<Triple<List<Invoice>, List<MaintenanceRecord>, List<ActivityLog>>> = combine(
        repository.observeInvoices(companyId),
        repository.observeMaintenance(companyId),
        repository.observeActivityLogs(companyId)
    ) { invoices, maintenance, logs -> Triple(invoices, maintenance, logs) }

    val uiState: StateFlow<WorkspaceUiState> = combine(
        coreFlow,
        masterFlow,
        opsFlow,
        financeFlow
    ) { core, master, ops, finance ->
        WorkspaceUiState.Ready(
            CompanyWorkspaceData(
                company = core.first,
                staff = core.second,
                invites = core.third,
                customers = master.first,
                vehicles = master.second,
                drivers = master.third,
                shipments = ops.first,
                weighings = ops.second,
                costs = ops.third,
                invoices = finance.first,
                maintenance = finance.second,
                logs = finance.third
            )
        ) as WorkspaceUiState
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("WorkspaceVM", "Error observing workspace data", rootError)
        emit(WorkspaceUiState.Error(rootError.friendlyMessage("Gagal memuat data operasional perusahaan.")))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = WorkspaceUiState.Loading
    )

    fun clearToast() {
        _toastMessage.value = null
    }

    fun syncToGoogleSheets(data: CompanyWorkspaceData) {
        val company = data.company ?: run {
            _toastMessage.value = "Data perusahaan belum dimuat."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = sheetsAdapter.syncCompanySnapshot(
                company = company,
                customers = data.customers,
                vehicles = data.vehicles,
                drivers = data.drivers,
                shipments = data.shipments,
                weighings = data.weighings,
                costs = data.costs,
                invoices = data.invoices,
                maintenance = data.maintenance
            )
            _isSaving.value = false
            _toastMessage.value = result.message
        }
    }

    fun saveCompanySettings(
        company: Company,
        address: String,
        phone: String,
        email: String,
        spreadsheetId: String,
        appsScriptUrl: String,
        tolerancePct: Double,
        defaultRatePerTon: Double
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateCompanySettings(
                actor = currentUser,
                company = company,
                address = address,
                phone = phone,
                email = email,
                spreadsheetId = spreadsheetId,
                appsScriptUrl = appsScriptUrl,
                shrinkageTolerancePct = tolerancePct,
                defaultRatePerTon = defaultRatePerTon
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Pengaturan perusahaan & Google Sheet disimpan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan pengaturan.") }
            )
        }
    }

    fun inviteKaryawan(company: Company, email: String) {
        if (!email.contains("@")) {
            _toastMessage.value = "Masukkan email Google karyawan yang valid."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.inviteKaryawan(currentUser, company, email)
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Undangan karyawan dibuat untuk $email." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal mengundang karyawan.") }
            )
        }
    }

    fun toggleStaffStatus(staff: CompanyUser) {
        val next = if (staff.status == "ACTIVE") "INACTIVE" else "ACTIVE"
        viewModelScope.launch {
            val res = repository.toggleStaffStatus(currentUser, staff, next)
            res.fold(
                onSuccess = { _toastMessage.value = "Status ${staff.name} -> $next" },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal mengubah status karyawan.") }
            )
        }
    }

    fun saveCustomer(
        existing: Customer?,
        name: String,
        code: String,
        picName: String,
        phone: String,
        address: String,
        loadingLocation: String,
        unloadingLocation: String,
        defaultPricePerTon: Double
    ) {
        if (name.trim().length < 2 || code.trim().isBlank()) {
            _toastMessage.value = "Nama dan kode pelanggan wajib diisi."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveCustomer(
                actor = currentUser,
                companyId = companyId,
                existing = existing,
                name = name,
                code = code,
                picName = picName,
                phone = phone,
                address = address,
                loadingLocation = loadingLocation,
                unloadingLocation = unloadingLocation,
                defaultPricePerTon = defaultPricePerTon
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Data pelanggan disimpan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan pelanggan.") }
            )
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            val res = repository.deleteCustomer(currentUser, companyId, customerId)
            res.fold(
                onSuccess = { _toastMessage.value = "Pelanggan dihapus." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menghapus pelanggan.") }
            )
        }
    }

    fun saveVehicle(
        existing: Vehicle?,
        plateNumber: String,
        vehicleType: String,
        capacityTons: Double,
        ownerName: String,
        status: String,
        stnkExpiry: String,
        kirExpiry: String,
        lastServiceDate: String,
        isRoadworthy: Boolean
    ) {
        if (plateNumber.trim().length < 3 || capacityTons <= 0.0) {
            _toastMessage.value = "Nomor polisi dan kapasitas angkut wajib valid."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveVehicle(
                actor = currentUser,
                companyId = companyId,
                existing = existing,
                plateNumber = plateNumber,
                vehicleType = vehicleType,
                capacityTons = capacityTons,
                ownerName = ownerName,
                status = status,
                stnkExpiry = stnkExpiry,
                kirExpiry = kirExpiry,
                lastServiceDate = lastServiceDate,
                isRoadworthy = isRoadworthy
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Armada $plateNumber disimpan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan armada.") }
            )
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            val res = repository.deleteVehicle(currentUser, companyId, vehicleId)
            res.fold(
                onSuccess = { _toastMessage.value = "Armada dihapus." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menghapus armada.") }
            )
        }
    }

    fun saveDriver(
        existing: Driver?,
        fullName: String,
        nik: String,
        phone: String,
        address: String,
        simNumber: String,
        simExpiry: String,
        assignedVehicleId: String,
        assignedPlate: String,
        status: String
    ) {
        if (fullName.trim().length < 2 || phone.trim().length < 5 || simNumber.trim().length < 3) {
            _toastMessage.value = "Nama, nomor HP, dan nomor SIM wajib diisi."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveDriver(
                actor = currentUser,
                companyId = companyId,
                existing = existing,
                fullName = fullName,
                nik = nik,
                phone = phone,
                address = address,
                simNumber = simNumber,
                simExpiry = simExpiry,
                assignedVehicleId = assignedVehicleId,
                assignedPlate = assignedPlate,
                status = status
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Data driver $fullName disimpan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan driver.") }
            )
        }
    }

    fun deleteDriver(driverId: String) {
        viewModelScope.launch {
            val res = repository.deleteDriver(currentUser, companyId, driverId)
            res.fold(
                onSuccess = { _toastMessage.value = "Driver dihapus." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menghapus driver.") }
            )
        }
    }

    fun createShipment(
        doNumber: String,
        suratJalanNumber: String,
        shipmentDate: String,
        customerId: String,
        customerName: String,
        loadingLocation: String,
        unloadingLocation: String,
        senderCompany: String,
        receiverCompany: String,
        productType: String,
        targetTonnage: Double,
        pricePerTon: Double,
        vehicleId: String,
        vehiclePlate: String,
        driverId: String,
        driverName: String,
        etaText: String,
        notes: String,
        submitForApproval: Boolean
    ) {
        if (doNumber.trim().length < 2 || customerName.trim().length < 2 || targetTonnage <= 0.0) {
            _toastMessage.value = "Nomor DO, pelanggan, dan target tonase wajib diisi."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createShipment(
                actor = currentUser,
                companyId = companyId,
                doNumber = doNumber,
                suratJalanNumber = suratJalanNumber,
                shipmentDate = shipmentDate,
                customerId = customerId,
                customerName = customerName,
                loadingLocation = loadingLocation,
                unloadingLocation = unloadingLocation,
                senderCompany = senderCompany,
                receiverCompany = receiverCompany,
                productType = productType,
                targetTonnage = targetTonnage,
                pricePerTon = pricePerTon,
                vehicleId = vehicleId,
                vehiclePlate = vehiclePlate,
                driverId = driverId,
                driverName = driverName,
                etaText = etaText,
                notes = notes,
                submitForApproval = submitForApproval
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Pengiriman $doNumber berhasil dibuat." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal membuat pengiriman.") }
            )
        }
    }

    fun updateShipmentStatus(shipment: Shipment, newStatus: ShipmentStatus, note: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateShipmentStatus(currentUser, shipment, newStatus, note)
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Status ${shipment.doNumber} -> ${newStatus.label}" },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal memperbarui status.") }
            )
        }
    }

    fun deleteShipment(shipmentId: String) {
        viewModelScope.launch {
            val res = repository.deleteShipment(currentUser, companyId, shipmentId)
            res.fold(
                onSuccess = { _toastMessage.value = "Pengiriman dihapus." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menghapus pengiriman.") }
            )
        }
    }

    fun saveWeighing(
        shipment: Shipment,
        loadTicket: String,
        unloadTicket: String,
        loadTonnage: Double,
        unloadTonnage: Double,
        tolerancePct: Double
    ) {
        if (loadTicket.isBlank() || loadTonnage <= 0.0) {
            _toastMessage.value = "Nomor tiket muat dan tonase muat wajib valid."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveWeighingRecord(
                actor = currentUser,
                companyId = companyId,
                shipment = shipment,
                loadTicketNumber = loadTicket,
                unloadTicketNumber = unloadTicket,
                loadTonnage = loadTonnage,
                unloadTonnage = unloadTonnage,
                tolerancePct = tolerancePct
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Data timbangan ${shipment.doNumber} tersimpan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan timbangan.") }
            )
        }
    }

    fun deleteWeighing(weighingId: String) {
        viewModelScope.launch {
            val res = repository.deleteWeighing(currentUser, companyId, weighingId)
            res.fold(
                onSuccess = { _toastMessage.value = "Data timbangan dihapus." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menghapus timbangan.") }
            )
        }
    }

    fun saveOperationalCost(
        shipment: Shipment,
        fuelCost: Double,
        tollCost: Double,
        loadingCost: Double,
        serviceCost: Double,
        tripAllowance: Double,
        driverSalary: Double,
        cashAdvance: Double,
        additionalCost: Double,
        revenue: Double,
        notes: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveOperationalCost(
                actor = currentUser,
                companyId = companyId,
                shipment = shipment,
                fuelCost = fuelCost,
                tollCost = tollCost,
                loadingUnloadingCost = loadingCost,
                serviceCost = serviceCost,
                tripAllowance = tripAllowance,
                driverSalary = driverSalary,
                driverCashAdvance = cashAdvance,
                additionalCost = additionalCost,
                revenue = revenue,
                notes = notes
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Biaya operasional ${shipment.doNumber} tercatat." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan biaya.") }
            )
        }
    }

    fun approveCost(cost: OperationalCost, approved: Boolean) {
        viewModelScope.launch {
            val res = repository.approveOperationalCost(currentUser, cost, approved)
            res.fold(
                onSuccess = { _toastMessage.value = if (approved) "Biaya disetujui." else "Biaya ditolak." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal memproses persetujuan.") }
            )
        }
    }

    fun createInvoice(
        invoiceNumber: String,
        shipment: Shipment,
        billableTonnage: Double,
        dueDate: String,
        notes: String
    ) {
        if (invoiceNumber.isBlank() || billableTonnage <= 0.0) {
            _toastMessage.value = "Nomor tagihan dan tonase wajib diisi."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createInvoiceFromShipment(
                actor = currentUser,
                companyId = companyId,
                invoiceNumber = invoiceNumber,
                shipment = shipment,
                billableTonnage = billableTonnage,
                dueDate = dueDate,
                notes = notes
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Tagihan $invoiceNumber dibuat." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal membuat tagihan.") }
            )
        }
    }

    fun recordPayment(invoice: Invoice, amount: Double, date: String, method: String) {
        if (amount <= 0.0) {
            _toastMessage.value = "Nominal pembayaran harus > 0."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.recordInvoicePayment(currentUser, invoice, amount, date, method)
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Pembayaran tagihan ${invoice.invoiceNumber} dicatat." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal mencatat pembayaran.") }
            )
        }
    }

    fun saveMaintenance(
        vehicle: Vehicle,
        serviceType: String,
        serviceDate: String,
        cost: Double,
        workshopName: String,
        nextServiceDate: String,
        notes: String
    ) {
        if (serviceType.isBlank()) {
            _toastMessage.value = "Jenis servis wajib diisi."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveMaintenanceRecord(
                actor = currentUser,
                companyId = companyId,
                vehicle = vehicle,
                serviceType = serviceType,
                serviceDate = serviceDate,
                cost = cost,
                workshopName = workshopName,
                nextServiceDate = nextServiceDate,
                notes = notes
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { _toastMessage.value = "Riwayat servis ${vehicle.plateNumber} disimpan." },
                onFailure = { _toastMessage.value = it.friendlyMessage("Gagal menyimpan servis.") }
            )
        }
    }
}
