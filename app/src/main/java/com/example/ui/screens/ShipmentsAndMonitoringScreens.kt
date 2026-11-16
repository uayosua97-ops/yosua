package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanyUser
import com.example.data.model.Customer
import com.example.data.model.Driver
import com.example.data.model.Shipment
import com.example.data.model.ShipmentStatus
import com.example.data.model.Vehicle
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ShipmentStatusBadge
import com.example.ui.theme.Emerald900
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.CompanyWorkspaceData
import com.example.ui.viewmodel.CompanyWorkspaceViewModel
import com.example.util.ReportExportHelper

@Composable
fun ShipmentsScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedShipment by remember { mutableStateOf<Shipment?>(null) }
    var deleteCandidate by remember { mutableStateOf<Shipment?>(null) }

    val isSpv = currentUser.role == "SPV"
    val filtered = remember(data.shipments, search, statusFilter) {
        data.shipments.filter { s ->
            val matchesFilter = statusFilter == "ALL" || s.status == statusFilter
            val matchesSearch = search.isBlank() ||
                s.doNumber.contains(search, ignoreCase = true) ||
                s.suratJalanNumber.contains(search, ignoreCase = true) ||
                s.customerName.contains(search, ignoreCase = true) ||
                s.vehiclePlate.contains(search, ignoreCase = true) ||
                s.driverName.contains(search, ignoreCase = true)
            matchesFilter && matchesSearch
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
                    Text("Manajemen Pengiriman CPO", style = MaterialTheme.typography.titleLarge)
                    Text("${data.shipments.size} Delivery Order terdaftar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("create_shipment_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DO Baru")
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari No. DO, Surat Jalan, Pelanggan, Nopol, Driver...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("Semua (${data.shipments.size})") }
                )
                ShipmentStatus.entries.forEach { st ->
                    val count = data.shipments.count { it.status == st.code }
                    FilterChip(
                        selected = statusFilter == st.code,
                        onClick = { statusFilter = st.code },
                        label = { Text("${st.label} ($count)") }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Pengiriman CPO",
                    message = "Buat Delivery Order dan Surat Jalan baru untuk memulai pengiriman.",
                    icon = Icons.Filled.LocalShipping,
                    actionLabel = "Buat Delivery Order",
                    onAction = { showCreateDialog = true }
                )
            }
        } else {
            items(filtered, key = { it.id }) { shipment ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${shipment.doNumber} • ${shipment.suratJalanNumber}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = Emerald900
                                )
                                Text(shipment.customerName, style = MaterialTheme.typography.titleMedium)
                            }
                            ShipmentStatusBadge(shipment.status)
                        }

                        Text(
                            text = "${shipment.loadingLocation} -> ${shipment.unloadingLocation}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${shipment.productType} • ${ReportExportHelper.formatTons(shipment.targetTonnage)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = ReportExportHelper.formatIdr(shipment.targetTonnage * shipment.pricePerTon),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily
                            )
                        }
                        Text(
                            text = "Armada: ${shipment.vehiclePlate.ifBlank { "-" }} • Driver: ${shipment.driverName.ifBlank { "-" }} • Tgl: ${shipment.shipmentDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectedShipment = shipment }) {
                                    Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Status & Riwayat")
                                }
                                if (isSpv && shipment.status == ShipmentStatus.PENDING_APPROVAL.code) {
                                    Button(onClick = {
                                        viewModel.updateShipmentStatus(shipment, ShipmentStatus.READY_TO_LOAD, "Disetujui SPV")
                                    }) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Setujui")
                                    }
                                }
                            }
                            if (isSpv) {
                                IconButton(onClick = { deleteCandidate = shipment }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Pengiriman", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateShipmentDialog(
            companyName = data.company?.name ?: currentUser.companyName,
            defaultRate = data.company?.defaultRatePerTon ?: 450000.0,
            customers = data.customers,
            vehicles = data.vehicles,
            drivers = data.drivers,
            isSaving = isSaving,
            onDismiss = { showCreateDialog = false },
            onCreate = { doNo, sjNo, date, custId, custName, loadLoc, unloadLoc, sender, receiver, product, tons, rate, vehId, plate, drvId, drvName, eta, notes, submit ->
                viewModel.createShipment(
                    doNo, sjNo, date, custId, custName, loadLoc, unloadLoc,
                    sender, receiver, product, tons, rate, vehId, plate, drvId, drvName,
                    eta, notes, submit
                )
                showCreateDialog = false
            }
        )
    }

    selectedShipment?.let { ship ->
        ShipmentStatusDialog(
            shipment = ship,
            isSpv = isSpv,
            isSaving = isSaving,
            onDismiss = { selectedShipment = null },
            onUpdateStatus = { next, note ->
                viewModel.updateShipmentStatus(ship, next, note)
                selectedShipment = null
            }
        )
    }

    deleteCandidate?.let { ship ->
        ConfirmDeleteDialog(
            title = "Hapus Pengiriman ${ship.doNumber}?",
            message = "Data Delivery Order dan Surat Jalan ${ship.suratJalanNumber} akan dihapus permanen dari database perusahaan.",
            onConfirm = {
                viewModel.deleteShipment(ship.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
fun MonitoringScreen(data: CompanyWorkspaceData) {
    val context = LocalContext.current
    val activeTrips = remember(data.shipments) {
        data.shipments.filter {
            it.status in setOf("READY_TO_LOAD", "LOADING", "IN_TRANSIT", "ARRIVED", "UNLOADING", "ISSUE")
        }
    }
    val completedTrips = remember(data.shipments) {
        data.shipments.filter { it.status == "COMPLETED" }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Monitoring Perjalanan CPO", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pantau armada aktif, pengemudi bertugas, estimasi tiba, dan rute Google Maps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activeTrips.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Tidak Ada Armada Sedang Berjalan",
                    message = "Pengiriman dengan status Siap Muat hingga Proses Bongkar akan muncul secara real-time di sini.",
                    icon = Icons.Filled.Route
                )
            }
        } else {
            items(activeTrips, key = { it.id }) { trip ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trip.doNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = Emerald900
                            )
                            ShipmentStatusBadge(trip.status)
                        }
                        Text("Pelanggan: ${trip.customerName}", style = MaterialTheme.typography.titleSmall)
                        Text("Rute: ${trip.loadingLocation} -> ${trip.unloadingLocation}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Armada: ${trip.vehiclePlate.ifBlank { "Belum dipilih" }} • Driver: ${trip.driverName.ifBlank { "Belum dipilih" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (trip.etaText.isNotBlank()) {
                            Text("Estimasi Tiba (ETA): ${trip.etaText}", style = MaterialTheme.typography.labelMedium, color = Emerald900)
                        }
                        OutlinedButton(
                            onClick = {
                                val url = "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(trip.loadingLocation)}&destination=${Uri.encode(trip.unloadingLocation)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Rute di Google Maps")
                        }
                    }
                }
            }
        }

        item {
            Text("Riwayat Perjalanan Selesai (${completedTrips.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(completedTrips.take(10), key = { it.id }) { done ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${done.doNumber} • ${done.vehiclePlate}", style = MaterialTheme.typography.titleSmall, fontFamily = JetBrainsMonoFamily)
                        Text("${done.loadingLocation} -> ${done.unloadingLocation}", style = MaterialTheme.typography.bodySmall)
                    }
                    ShipmentStatusBadge(done.status)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateShipmentDialog(
    companyName: String,
    defaultRate: Double,
    customers: List<Customer>,
    vehicles: List<Vehicle>,
    drivers: List<Driver>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, String, String, String, String, Double, Double, String, String, String, String, String, String, Boolean) -> Unit
) {
    var doNumber by remember { mutableStateOf("DO-${(100..999).random()}") }
    var sjNumber by remember { mutableStateOf("SJ-${(100..999).random()}") }
    var date by remember { mutableStateOf("2026-11-16") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var customerName by remember { mutableStateOf(customers.firstOrNull()?.name ?: "") }
    var loadingLoc by remember { mutableStateOf(customers.firstOrNull()?.loadingLocation ?: "") }
    var unloadingLoc by remember { mutableStateOf(customers.firstOrNull()?.unloadingLocation ?: "") }
    var senderCompany by remember { mutableStateOf(companyName) }
    var receiverCompany by remember { mutableStateOf(customers.firstOrNull()?.name ?: "") }
    var productType by remember { mutableStateOf("Crude Palm Oil (CPO)") }
    var targetTonnage by remember { mutableStateOf("25.0") }
    var pricePerTon by remember { mutableStateOf((customers.firstOrNull()?.defaultPricePerTon ?: defaultRate).toLong().toString()) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(vehicles.firstOrNull { it.status == "AVAILABLE" } ?: vehicles.firstOrNull()) }
    var selectedDriver by remember { mutableStateOf<Driver?>(drivers.firstOrNull { it.status != "INACTIVE" } ?: drivers.firstOrNull()) }
    var etaText by remember { mutableStateOf("Estimasi 8 Jam") }
    var notes by remember { mutableStateOf("") }
    var submitApproval by remember { mutableStateOf(true) }

    var custMenu by remember { mutableStateOf(false) }
    var vehMenu by remember { mutableStateOf(false) }
    var drvMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Delivery Order CPO") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = doNumber, onValueChange = { doNumber = it }, label = { Text("No. DO *") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = sjNumber, onValueChange = { sjNumber = it }, label = { Text("No. SJ *") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Tanggal Pengiriman") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (customers.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = custMenu, onExpandedChange = { custMenu = !custMenu }) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Pilih Pelanggan *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = custMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = custMenu, onDismissRequest = { custMenu = false }) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${c.code})") },
                                    onClick = {
                                        selectedCustomer = c
                                        customerName = c.name
                                        receiverCompany = c.name
                                        if (c.loadingLocation.isNotBlank()) loadingLoc = c.loadingLocation
                                        if (c.unloadingLocation.isNotBlank()) unloadingLoc = c.unloadingLocation
                                        if (c.defaultPricePerTon > 0) pricePerTon = c.defaultPricePerTon.toLong().toString()
                                        custMenu = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = customerName, onValueChange = { customerName = it; receiverCompany = it }, label = { Text("Nama Pelanggan *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(value = loadingLoc, onValueChange = { loadingLoc = it }, label = { Text("Lokasi Muat (PKS) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unloadingLoc, onValueChange = { unloadingLoc = it }, label = { Text("Lokasi Bongkar (Refinery/Pelabuhan) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = senderCompany, onValueChange = { senderCompany = it }, label = { Text("Perusahaan Pengirim") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = receiverCompany, onValueChange = { receiverCompany = it }, label = { Text("Perusahaan Penerima") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = productType, onValueChange = { productType = it }, label = { Text("Jenis Produk") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = targetTonnage, onValueChange = { targetTonnage = it }, label = { Text("Target Ton") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = pricePerTon, onValueChange = { pricePerTon = it }, label = { Text("Harga/Ton (Rp)") }, modifier = Modifier.weight(1.3f), singleLine = true)
                }

                if (vehicles.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = vehMenu, onExpandedChange = { vehMenu = !vehMenu }) {
                        OutlinedTextField(
                            value = selectedVehicle?.let { "${it.plateNumber} (${it.capacityTons}T)" } ?: "Pilih Armada",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Armada") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = vehMenu, onDismissRequest = { vehMenu = false }) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.plateNumber} • ${v.vehicleType} (${v.status})") },
                                    onClick = { selectedVehicle = v; vehMenu = false }
                                )
                            }
                        }
                    }
                }

                if (drivers.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = drvMenu, onExpandedChange = { drvMenu = !drvMenu }) {
                        OutlinedTextField(
                            value = selectedDriver?.fullName ?: "Pilih Driver",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pengemudi") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = drvMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = drvMenu, onDismissRequest = { drvMenu = false }) {
                            drivers.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text("${d.fullName} (${d.status})") },
                                    onClick = { selectedDriver = d; drvMenu = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(value = etaText, onValueChange = { etaText = it }, label = { Text("Estimasi Tiba (ETA)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ajukan persetujuan SPV langsung", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = submitApproval, onCheckedChange = { submitApproval = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        doNumber, sjNumber, date,
                        selectedCustomer?.id ?: "", customerName,
                        loadingLoc, unloadingLoc, senderCompany, receiverCompany,
                        productType,
                        targetTonnage.toDoubleOrNull() ?: 25.0,
                        pricePerTon.toDoubleOrNull() ?: defaultRate,
                        selectedVehicle?.id ?: "", selectedVehicle?.plateNumber ?: "",
                        selectedDriver?.id ?: "", selectedDriver?.fullName ?: "",
                        etaText, notes, submitApproval
                    )
                },
                enabled = !isSaving && doNumber.isNotBlank() && customerName.isNotBlank() && loadingLoc.isNotBlank() && unloadingLoc.isNotBlank()
            ) {
                Text("Simpan DO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShipmentStatusDialog(
    shipment: Shipment,
    isSpv: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (ShipmentStatus, String) -> Unit
) {
    val availableStatuses = remember(isSpv, shipment.status) {
        if (isSpv) {
            ShipmentStatus.entries
        } else {
            ShipmentStatus.entries.filter { it != ShipmentStatus.READY_TO_LOAD }
        }
    }
    var selectedStatus by remember { mutableStateOf(ShipmentStatus.fromCode(shipment.status)) }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Status ${shipment.doNumber}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Status Saat Ini: ${ShipmentStatus.fromCode(shipment.status).label}", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedStatus.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Perbarui Status Perjalanan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        availableStatuses.forEach { st ->
                            DropdownMenuItem(
                                text = { Text("${st.stepIndex + 1}. ${st.label}") },
                                onClick = { selectedStatus = st; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Perubahan Status") },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Text("Riwayat Aktivitas Pengiriman", style = MaterialTheme.typography.titleSmall)
                if (shipment.statusHistory.isEmpty()) {
                    Text("Belum ada riwayat.", style = MaterialTheme.typography.bodySmall)
                } else {
                    shipment.statusHistory.reversed().forEach { entry ->
                        Text("• $entry", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateStatus(selectedStatus, note) },
                enabled = !isSaving
            ) {
                Text("Simpan Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
