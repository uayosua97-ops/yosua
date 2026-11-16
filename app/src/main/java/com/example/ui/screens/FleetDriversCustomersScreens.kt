package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanyUser
import com.example.data.model.Customer
import com.example.data.model.Driver
import com.example.data.model.DriverStatus
import com.example.data.model.Vehicle
import com.example.data.model.VehicleStatus
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Emerald900
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.viewmodel.CompanyWorkspaceData
import com.example.ui.viewmodel.CompanyWorkspaceViewModel
import com.example.util.ReportExportHelper

@Composable
fun FleetScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var maintenanceTarget by remember { mutableStateOf<Vehicle?>(null) }
    var deleteCandidate by remember { mutableStateOf<Vehicle?>(null) }

    val isSpv = currentUser.role == "SPV"
    val filtered = remember(data.vehicles, search) {
        if (search.isBlank()) data.vehicles
        else data.vehicles.filter {
            it.plateNumber.contains(search, ignoreCase = true) ||
                it.vehicleType.contains(search, ignoreCase = true) ||
                it.ownerName.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Manajemen Armada Tangki", style = MaterialTheme.typography.titleLarge)
                    Text("${data.vehicles.size} unit • ${data.maintenance.size} log servis", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { editingVehicle = null; showEditor = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Armada")
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari nomor polisi, jenis tangki, pemilik...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Armada Terdaftar",
                    message = "Tambahkan truk tangki CPO beserta masa berlaku STNK, KIR, dan kapasitas angkut.",
                    icon = Icons.Filled.DirectionsCar,
                    actionLabel = "Tambah Armada",
                    onAction = { editingVehicle = null; showEditor = true }
                )
            }
        } else {
            items(filtered, key = { it.id }) { vehicle ->
                val st = VehicleStatus.fromCode(vehicle.status)
                val (bg, fg) = when (st) {
                    VehicleStatus.AVAILABLE -> StatusSuccessBg to StatusSuccess
                    VehicleStatus.IN_USE -> StatusInfoBg to StatusInfo
                    VehicleStatus.MAINTENANCE -> StatusWarningBg to StatusWarning
                    VehicleStatus.INACTIVE -> StatusDangerBg to StatusDanger
                }
                val serviceLogs = data.maintenance.filter { it.vehicleId == vehicle.id }
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(vehicle.plateNumber, style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMonoFamily, color = Emerald900)
                                Text("${vehicle.vehicleType} • Kapasitas ${vehicle.capacityTons} Ton", style = MaterialTheme.typography.bodyMedium)
                            }
                            StatusBadge(st.label, bg, fg)
                        }
                        Text(
                            "Pemilik: ${vehicle.ownerName.ifBlank { "-" }} • Kelayakan: ${if (vehicle.isRoadworthy) "Laik Jalan" else "Perlu Cek"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "STNK: ${vehicle.stnkExpiry.ifBlank { "-" }} • KIR: ${vehicle.kirExpiry.ifBlank { "-" }} • Servis Terakhir: ${vehicle.lastServiceDate.ifBlank { "-" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (serviceLogs.isNotEmpty()) {
                            val lastLog = serviceLogs.first()
                            Text(
                                "Servis Terbaru: ${lastLog.serviceType} (${ReportExportHelper.formatIdr(lastLog.cost)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald900
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isSpv) {
                                    OutlinedButton(onClick = { editingVehicle = vehicle; showEditor = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                }
                                OutlinedButton(onClick = { maintenanceTarget = vehicle }) {
                                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Catat Servis")
                                }
                            }
                            if (isSpv) {
                                IconButton(onClick = { deleteCandidate = vehicle }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Armada", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        VehicleEditorDialog(
            existing = editingVehicle,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { plate, type, cap, owner, status, stnk, kir, service, worthy ->
                viewModel.saveVehicle(editingVehicle, plate, type, cap, owner, status, stnk, kir, service, worthy)
                showEditor = false
            }
        )
    }

    maintenanceTarget?.let { veh ->
        MaintenanceDialog(
            vehicle = veh,
            isSaving = isSaving,
            onDismiss = { maintenanceTarget = null },
            onSave = { type, date, cost, workshop, nextDate, notes ->
                viewModel.saveMaintenance(veh, type, date, cost, workshop, nextDate, notes)
                maintenanceTarget = null
            }
        )
    }

    deleteCandidate?.let { veh ->
        ConfirmDeleteDialog(
            title = "Hapus Armada ${veh.plateNumber}?",
            message = "Data armada ini akan dihapus dari daftar kendaraan perusahaan.",
            onConfirm = { viewModel.deleteVehicle(veh.id); deleteCandidate = null },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
fun DriversScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingDriver by remember { mutableStateOf<Driver?>(null) }
    var deleteCandidate by remember { mutableStateOf<Driver?>(null) }

    val isSpv = currentUser.role == "SPV"
    val filtered = remember(data.drivers, search) {
        if (search.isBlank()) data.drivers
        else data.drivers.filter {
            it.fullName.contains(search, ignoreCase = true) ||
                it.phone.contains(search, ignoreCase = true) ||
                it.assignedPlate.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Manajemen Pengemudi (Driver)", style = MaterialTheme.typography.titleLarge)
                    Text("${data.drivers.size} pengemudi terdaftar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { editingDriver = null; showEditor = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Driver")
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari nama driver, telepon, atau armada...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Driver Terdaftar",
                    message = "Tambahkan data pengemudi, nomor SIM BII Umum, masa berlaku SIM, dan unit armada.",
                    icon = Icons.Filled.People,
                    actionLabel = "Tambah Driver",
                    onAction = { editingDriver = null; showEditor = true }
                )
            }
        } else {
            items(filtered, key = { it.id }) { driver ->
                val st = DriverStatus.fromCode(driver.status)
                val tripCount = data.shipments.count { it.driverId == driver.id || it.driverName == driver.fullName }
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(driver.fullName, style = MaterialTheme.typography.titleMedium)
                                Text("HP: ${driver.phone} • NIK: ${driver.nik.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                            }
                            StatusBadge(
                                label = st.label,
                                containerColor = if (st == DriverStatus.ACTIVE) StatusSuccessBg else if (st == DriverStatus.ON_TRIP) StatusInfoBg else StatusDangerBg,
                                contentColor = if (st == DriverStatus.ACTIVE) StatusSuccess else if (st == DriverStatus.ON_TRIP) StatusInfo else StatusDanger
                            )
                        }
                        Text(
                            "SIM: ${driver.simNumber} (Exp: ${driver.simExpiry.ifBlank { "-" }}) • Unit: ${driver.assignedPlate.ifBlank { "-" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald900
                        )
                        Text("Riwayat Pengiriman: $tripCount trip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isSpv) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(onClick = { editingDriver = driver; showEditor = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit")
                                }
                                IconButton(onClick = { deleteCandidate = driver }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Driver", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        DriverEditorDialog(
            existing = editingDriver,
            vehicles = data.vehicles,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { name, nik, phone, addr, sim, simExp, vehId, plate, status ->
                viewModel.saveDriver(editingDriver, name, nik, phone, addr, sim, simExp, vehId, plate, status)
                showEditor = false
            }
        )
    }

    deleteCandidate?.let { drv ->
        ConfirmDeleteDialog(
            title = "Hapus Driver ${drv.fullName}?",
            message = "Profil pengemudi akan dihapus dari database perusahaan.",
            onConfirm = { viewModel.deleteDriver(drv.id); deleteCandidate = null },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
fun CustomersScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var deleteCandidate by remember { mutableStateOf<Customer?>(null) }

    val isSpv = currentUser.role == "SPV"
    val filtered = remember(data.customers, search) {
        if (search.isBlank()) data.customers
        else data.customers.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.code.contains(search, ignoreCase = true) ||
                it.loadingLocation.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Data Pelanggan & PKS/Refinery", style = MaterialTheme.typography.titleLarge)
                    Text("${data.customers.size} pelanggan aktif", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { editingCustomer = null; showEditor = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pelanggan")
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari nama pelanggan, kode, atau PKS muat...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Pelanggan",
                    message = "Tambahkan perusahaan pengirim/penerima CPO beserta rute muat/bongkar dan tarif kontrak per ton.",
                    icon = Icons.Filled.ReceiptLong,
                    actionLabel = "Tambah Pelanggan",
                    onAction = { editingCustomer = null; showEditor = true }
                )
            }
        } else {
            items(filtered, key = { it.id }) { cust ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${cust.name} (${cust.code})", style = MaterialTheme.typography.titleMedium)
                            Text(
                                ReportExportHelper.formatIdr(cust.defaultPricePerTon) + "/T",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = Emerald900,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("PIC: ${cust.picName.ifBlank { "-" }} • Telp: ${cust.phone.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Rute Default: ${cust.loadingLocation.ifBlank { "-" }} -> ${cust.unloadingLocation.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                        if (isSpv) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(onClick = { editingCustomer = cust; showEditor = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit")
                                }
                                IconButton(onClick = { deleteCandidate = cust }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Pelanggan", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        CustomerEditorDialog(
            existing = editingCustomer,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { name, code, pic, phone, addr, loadLoc, unloadLoc, rate ->
                viewModel.saveCustomer(editingCustomer, name, code, pic, phone, addr, loadLoc, unloadLoc, rate)
                showEditor = false
            }
        )
    }

    deleteCandidate?.let { cust ->
        ConfirmDeleteDialog(
            title = "Hapus Pelanggan ${cust.name}?",
            message = "Master pelanggan ini akan dihapus dari perusahaan.",
            onConfirm = { viewModel.deleteCustomer(cust.id); deleteCandidate = null },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleEditorDialog(
    existing: Vehicle?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String, String, String, String, Boolean) -> Unit
) {
    var plate by remember { mutableStateOf(existing?.plateNumber ?: "") }
    var type by remember { mutableStateOf(existing?.vehicleType ?: "Tangki CPO 25T") }
    var capacity by remember { mutableStateOf((existing?.capacityTons ?: 25.0).toString()) }
    var owner by remember { mutableStateOf(existing?.ownerName ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "AVAILABLE") }
    var stnk by remember { mutableStateOf(existing?.stnkExpiry ?: "2027-06-30") }
    var kir by remember { mutableStateOf(existing?.kirExpiry ?: "2027-03-31") }
    var lastService by remember { mutableStateOf(existing?.lastServiceDate ?: "2026-11-01") }
    var worthy by remember { mutableStateOf(existing?.isRoadworthy ?: true) }
    var statusMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Armada CPO" else "Edit Armada ${existing.plateNumber}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Nomor Polisi *") }, singleLine = true)
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Jenis Kendaraan *") }, singleLine = true)
                OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Kapasitas Angkut (Ton) *") }, singleLine = true)
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Nama Pemilik / Vendor") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = statusMenu, onExpandedChange = { statusMenu = !statusMenu }) {
                    OutlinedTextField(
                        value = VehicleStatus.fromCode(status).label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Armada") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                        VehicleStatus.entries.forEach { st ->
                            DropdownMenuItem(text = { Text(st.label) }, onClick = { status = st.code; statusMenu = false })
                        }
                    }
                }
                OutlinedTextField(value = stnk, onValueChange = { stnk = it }, label = { Text("Masa Berlaku STNK") }, singleLine = true)
                OutlinedTextField(value = kir, onValueChange = { kir = it }, label = { Text("Masa Berlaku KIR") }, singleLine = true)
                OutlinedTextField(value = lastService, onValueChange = { lastService = it }, label = { Text("Tanggal Servis Terakhir") }, singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Laik Jalan", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = worthy, onCheckedChange = { worthy = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(plate, type, capacity.toDoubleOrNull() ?: 25.0, owner, status, stnk, kir, lastService, worthy) },
                enabled = !isSaving && plate.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun MaintenanceDialog(
    vehicle: Vehicle,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String, String) -> Unit
) {
    var serviceType by remember { mutableStateOf("Servis Berkala & Cek Tangki") }
    var serviceDate by remember { mutableStateOf("2026-11-16") }
    var cost by remember { mutableStateOf("1500000") }
    var workshop by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf("2027-02-16") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Servis ${vehicle.plateNumber}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = serviceType, onValueChange = { serviceType = it }, label = { Text("Jenis Servis *") }, singleLine = true)
                OutlinedTextField(value = serviceDate, onValueChange = { serviceDate = it }, label = { Text("Tanggal Servis *") }, singleLine = true)
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Biaya Servis (Rp)") }, singleLine = true)
                OutlinedTextField(value = workshop, onValueChange = { workshop = it }, label = { Text("Nama Bengkel") }, singleLine = true)
                OutlinedTextField(value = nextDate, onValueChange = { nextDate = it }, label = { Text("Jadwal Servis Berikutnya") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(serviceType, serviceDate, cost.toDoubleOrNull() ?: 0.0, workshop, nextDate, notes) },
                enabled = !isSaving && serviceType.isNotBlank()
            ) { Text("Simpan Servis") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverEditorDialog(
    existing: Driver?,
    vehicles: List<Vehicle>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.fullName ?: "") }
    var nik by remember { mutableStateOf(existing?.nik ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var sim by remember { mutableStateOf(existing?.simNumber ?: "") }
    var simExp by remember { mutableStateOf(existing?.simExpiry ?: "2028-12-31") }
    var selectedVehicleId by remember { mutableStateOf(existing?.assignedVehicleId ?: "") }
    var selectedPlate by remember { mutableStateOf(existing?.assignedPlate ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "ACTIVE") }
    var vehMenu by remember { mutableStateOf(false) }
    var stMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Driver CPO" else "Edit Driver") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap *") }, singleLine = true)
                OutlinedTextField(value = nik, onValueChange = { nik = it }, label = { Text("NIK / No. Identitas") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon *") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, singleLine = true)
                OutlinedTextField(value = sim, onValueChange = { sim = it }, label = { Text("Nomor SIM *") }, singleLine = true)
                OutlinedTextField(value = simExp, onValueChange = { simExp = it }, label = { Text("Masa Berlaku SIM") }, singleLine = true)
                if (vehicles.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = vehMenu, onExpandedChange = { vehMenu = !vehMenu }) {
                        OutlinedTextField(
                            value = selectedPlate.ifBlank { "Pilih Armada" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Armada Ditugaskan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = vehMenu, onDismissRequest = { vehMenu = false }) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.plateNumber} (${v.vehicleType})") },
                                    onClick = { selectedVehicleId = v.id; selectedPlate = v.plateNumber; vehMenu = false }
                                )
                            }
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = stMenu, onExpandedChange = { stMenu = !stMenu }) {
                    OutlinedTextField(
                        value = DriverStatus.fromCode(status).label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = stMenu, onDismissRequest = { stMenu = false }) {
                        DriverStatus.entries.forEach { st ->
                            DropdownMenuItem(text = { Text(st.label) }, onClick = { status = st.code; stMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, nik, phone, address, sim, simExp, selectedVehicleId, selectedPlate, status) },
                enabled = !isSaving && name.isNotBlank() && phone.isNotBlank() && sim.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun CustomerEditorDialog(
    existing: Customer?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var code by remember { mutableStateOf(existing?.code ?: "") }
    var pic by remember { mutableStateOf(existing?.picName ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var loadLoc by remember { mutableStateOf(existing?.loadingLocation ?: "") }
    var unloadLoc by remember { mutableStateOf(existing?.unloadingLocation ?: "") }
    var rate by remember { mutableStateOf((existing?.defaultPricePerTon ?: 450000.0).toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Pelanggan CPO" else "Edit Pelanggan") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Perusahaan Pelanggan *") }, singleLine = true)
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Kode Pelanggan *") }, singleLine = true)
                OutlinedTextField(value = pic, onValueChange = { pic = it }, label = { Text("Nama PIC") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telepon") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, singleLine = true)
                OutlinedTextField(value = loadLoc, onValueChange = { loadLoc = it }, label = { Text("Lokasi Muat Default (PKS)") }, singleLine = true)
                OutlinedTextField(value = unloadLoc, onValueChange = { unloadLoc = it }, label = { Text("Lokasi Bongkar Default") }, singleLine = true)
                OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Tarif Angkutan / Ton (Rp)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, code, pic, phone, address, loadLoc, unloadLoc, rate.toDoubleOrNull() ?: 450000.0) },
                enabled = !isSaving && name.isNotBlank() && code.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
