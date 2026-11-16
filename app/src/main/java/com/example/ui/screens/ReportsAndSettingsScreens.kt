package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanyUser
import com.example.data.model.ShipmentStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Emerald900
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.viewmodel.CompanyWorkspaceData
import com.example.ui.viewmodel.CompanyWorkspaceViewModel
import com.example.util.ReportExportHelper

enum class ReportType(val label: String) {
    SHIPMENTS("Pengiriman"),
    WEIGHINGS("Tonase & Susut"),
    FLEET("Armada"),
    DRIVERS("Driver"),
    COSTS("Biaya Operasional"),
    FINANCE("Keuangan & Tagihan"),
    ACTIVITY("Aktivitas Karyawan")
}

@Composable
fun ReportsScreen(data: CompanyWorkspaceData) {
    val context = LocalContext.current
    var reportType by remember { mutableStateOf(ReportType.SHIPMENTS) }
    var search by remember { mutableStateOf("") }
    var exportNotice by remember { mutableStateOf<String?>(null) }

    val companyName = data.company?.name ?: "Perusahaan CPO"

    val (headers, rows) = remember(reportType, data, search) {
        buildReportTable(reportType, data, search)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Pusat Laporan & Ekspor", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Filter laporan operasional & keuangan lalu ekspor langsung ke Excel (CSV) atau PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportType.entries.forEach { type ->
                    FilterChip(
                        selected = reportType == type,
                        onClick = { reportType = type },
                        label = { Text(type.label) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter nomor DO, pelanggan, plat, atau nama...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val res = ReportExportHelper.exportCsvAndShare(
                            context = context,
                            fileNamePrefix = "laporan_${reportType.name.lowercase()}",
                            headers = headers,
                            rows = rows
                        )
                        exportNotice = res.fold(
                            onSuccess = { "File CSV Excel siap dibagikan: ${it.name}" },
                            onFailure = { "Gagal mengekspor CSV: ${it.localizedMessage}" }
                        )
                    },
                    enabled = rows.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("export_csv_button")
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Excel (CSV)")
                }
                OutlinedButton(
                    onClick = {
                        val res = ReportExportHelper.exportPdfAndShare(
                            context = context,
                            title = "Laporan ${reportType.label}",
                            companyName = companyName,
                            subtitle = "${rows.size} baris data • CPO Logistics MGE",
                            headers = headers,
                            rows = rows
                        )
                        exportNotice = res.fold(
                            onSuccess = { "File PDF siap dibagikan: ${it.name}" },
                            onFailure = { "Gagal membuat PDF: ${it.localizedMessage}" }
                        )
                    },
                    enabled = rows.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("export_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF")
                }
            }
        }

        exportNotice?.let { msg ->
            item {
                Surface(
                    color = StatusInfoBg,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(18.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = StatusInfo)
                    }
                }
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pratinjau Laporan ${reportType.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${rows.size} Baris",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (rows.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Data Untuk Diekspor",
                    message = "Tambahkan transaksi pada modul terkait atau ubah kata kunci pencarian.",
                    icon = Icons.Filled.Assessment
                )
            }
        } else {
            items(rows.take(50)) { row ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        headers.forEachIndexed { idx, header ->
                            val value = row.getOrElse(idx) { "-" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    header,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildReportTable(
    type: ReportType,
    data: CompanyWorkspaceData,
    search: String
): Pair<List<String>, List<List<String>>> {
    val q = search.trim()
    return when (type) {
        ReportType.SHIPMENTS -> {
            val headers = listOf("No. DO", "Surat Jalan", "Tanggal", "Pelanggan", "Armada", "Driver", "Tonase", "Status")
            val filtered = data.shipments.filter {
                q.isBlank() || it.doNumber.contains(q, true) || it.customerName.contains(q, true) || it.vehiclePlate.contains(q, true)
            }
            headers to filtered.map {
                listOf(
                    it.doNumber,
                    it.suratJalanNumber,
                    it.shipmentDate,
                    it.customerName,
                    it.vehiclePlate,
                    it.driverName,
                    ReportExportHelper.formatTons(it.targetTonnage),
                    ShipmentStatus.fromCode(it.status).label
                )
            }
        }
        ReportType.WEIGHINGS -> {
            val headers = listOf("No. DO", "Tiket Muat", "Tiket Bongkar", "Muat", "Bongkar", "Selisih", "Tagih", "Nilai")
            val filtered = data.weighings.filter {
                q.isBlank() || it.doNumber.contains(q, true) || it.loadTicketNumber.contains(q, true)
            }
            headers to filtered.map {
                listOf(
                    it.doNumber,
                    it.loadTicketNumber,
                    it.unloadTicketNumber.ifBlank { "-" },
                    ReportExportHelper.formatTons(it.loadTonnage),
                    ReportExportHelper.formatTons(it.unloadTonnage),
                    ReportExportHelper.formatTons(it.differenceTonnage),
                    ReportExportHelper.formatTons(it.billableTonnage),
                    ReportExportHelper.formatIdr(it.totalDeliveryValue)
                )
            }
        }
        ReportType.FLEET -> {
            val headers = listOf("No. Polisi", "Jenis", "Kapasitas", "Status", "STNK", "KIR", "Laik Jalan")
            val filtered = data.vehicles.filter {
                q.isBlank() || it.plateNumber.contains(q, true) || it.vehicleType.contains(q, true)
            }
            headers to filtered.map {
                listOf(
                    it.plateNumber,
                    it.vehicleType,
                    "${it.capacityTons}T",
                    it.status,
                    it.stnkExpiry.ifBlank { "-" },
                    it.kirExpiry.ifBlank { "-" },
                    if (it.isRoadworthy) "Ya" else "Tidak"
                )
            }
        }
        ReportType.DRIVERS -> {
            val headers = listOf("Nama Driver", "No. HP", "No. SIM", "Expired SIM", "Armada", "Status")
            val filtered = data.drivers.filter {
                q.isBlank() || it.fullName.contains(q, true) || it.assignedPlate.contains(q, true)
            }
            headers to filtered.map {
                listOf(it.fullName, it.phone, it.simNumber, it.simExpiry, it.assignedPlate.ifBlank { "-" }, it.status)
            }
        }
        ReportType.COSTS -> {
            val headers = listOf("No. DO", "BBM", "Tol", "Uang Jalan", "Gaji/Kasbon", "Total Biaya", "Laba Bersih", "Approval")
            val filtered = data.costs.filter {
                q.isBlank() || it.doNumber.contains(q, true)
            }
            headers to filtered.map {
                listOf(
                    it.doNumber,
                    ReportExportHelper.formatIdr(it.fuelCost),
                    ReportExportHelper.formatIdr(it.tollCost),
                    ReportExportHelper.formatIdr(costAllowance(it)),
                    ReportExportHelper.formatIdr(it.driverSalary + it.driverCashAdvance),
                    ReportExportHelper.formatIdr(it.totalCost),
                    ReportExportHelper.formatIdr(it.netIncome),
                    it.approvalStatus
                )
            }
        }
        ReportType.FINANCE -> {
            val headers = listOf("No. Invoice", "No. DO", "Pelanggan", "Tonase", "Total Tagihan", "Terbayar", "Status")
            val filtered = data.invoices.filter {
                q.isBlank() || it.invoiceNumber.contains(q, true) || it.customerName.contains(q, true) || it.doNumber.contains(q, true)
            }
            headers to filtered.map {
                listOf(
                    it.invoiceNumber,
                    it.doNumber,
                    it.customerName,
                    ReportExportHelper.formatTons(it.billableTonnage),
                    ReportExportHelper.formatIdr(it.totalAmount),
                    ReportExportHelper.formatIdr(it.paidAmount),
                    it.paymentStatus
                )
            }
        }
        ReportType.ACTIVITY -> {
            val headers = listOf("Aksi", "Modul", "Pelaku", "Role", "Detail")
            val filtered = data.logs.filter {
                q.isBlank() || it.action.contains(q, true) || it.actorName.contains(q, true) || it.details.contains(q, true)
            }
            headers to filtered.map {
                listOf(it.action, it.module, it.actorName, it.actorRole, it.details)
            }
        }
    }
}

private fun costAllowance(c: com.example.data.model.OperationalCost): Double =
    c.tripAllowance + c.loadingUnloadingCost + c.serviceCost + c.additionalCost

@Composable
fun CompanySettingsScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    val company = data.company
    val isSpv = currentUser.role == "SPV"
    var showScriptDialog by remember { mutableStateOf(false) }

    var address by remember(company?.id) { mutableStateOf(company?.address ?: "") }
    var phone by remember(company?.id) { mutableStateOf(company?.phone ?: "") }
    var email by remember(company?.id) { mutableStateOf(company?.email ?: "") }
    var spreadsheetId by remember(company?.id) { mutableStateOf(company?.spreadsheetId ?: "") }
    var appsScriptUrl by remember(company?.id) { mutableStateOf(company?.appsScriptUrl ?: "") }
    var toleranceStr by remember(company?.id) { mutableStateOf((company?.shrinkageTolerancePct ?: 0.5).toString()) }
    var rateStr by remember(company?.id) { mutableStateOf((company?.defaultRatePerTon ?: 450000.0).toLong().toString()) }
    var inviteEmail by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pengaturan Perusahaan & Integrasi", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${company?.name ?: currentUser.companyName} • Kode: ${company?.code ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    label = if (isSpv) "Akses SPV" else "Mode Baca",
                    containerColor = if (isSpv) StatusSuccessBg else StatusWarningBg,
                    contentColor = if (isSpv) StatusSuccess else StatusWarning
                )
            }
        }

        if (!isSpv) {
            item {
                EmptyStateCard(
                    title = "Pengaturan Dibatasi untuk Supervisor (SPV)",
                    message = "Sesuai aturan RBAC tenant, Karyawan tidak dapat mengubah pengaturan operasional, Spreadsheet ID, atau manajemen akun perusahaan.",
                    icon = Icons.Filled.Settings
                )
            }
        } else if (company != null) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Koneksi Google Sheets Perusahaan", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Setiap perusahaan memiliki Spreadsheet ID & Web App URL terpisah untuk sinkronisasi otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = spreadsheetId,
                            onValueChange = { spreadsheetId = it },
                            label = { Text("Google Spreadsheet ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = appsScriptUrl,
                            onValueChange = { appsScriptUrl = it },
                            label = { Text("Google Apps Script Web App URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showScriptDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lihat Script")
                            }
                            Button(
                                onClick = { viewModel.syncToGoogleSheets(data) },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f).testTag("settings_sync_sheets_button")
                            ) {
                                Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Sekarang")
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Profil & Parameter Operasional CPO", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telepon Kantor") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Perusahaan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Kantor") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = toleranceStr,
                                onValueChange = { toleranceStr = it },
                                label = { Text("Toleransi Susut (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rateStr,
                                onValueChange = { rateStr = it },
                                label = { Text("Tarif Default/Ton (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.saveCompanySettings(
                                    company = company,
                                    address = address,
                                    phone = phone,
                                    email = email,
                                    spreadsheetId = spreadsheetId,
                                    appsScriptUrl = appsScriptUrl,
                                    tolerancePct = toleranceStr.toDoubleOrNull() ?: 0.5,
                                    defaultRatePerTon = rateStr.toDoubleOrNull() ?: 450000.0
                                )
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("save_company_settings_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Pengaturan Tenant")
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Manajemen Akun Karyawan (${data.staff.size})", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                label = { Text("Email Google Karyawan") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("invite_karyawan_email_input")
                            )
                            Button(
                                onClick = {
                                    viewModel.inviteKaryawan(company, inviteEmail)
                                    inviteEmail = ""
                                },
                                enabled = !isSaving && inviteEmail.contains("@"),
                                modifier = Modifier.testTag("invite_karyawan_button")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undang")
                            }
                        }

                        if (data.invites.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Undangan Karyawan", style = MaterialTheme.typography.labelLarge)
                            data.invites.forEach { inv ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${inv.email} (${inv.role})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    StatusBadge(
                                        label = inv.status,
                                        containerColor = if (inv.status == "ACCEPTED") StatusSuccessBg else StatusWarningBg,
                                        contentColor = if (inv.status == "ACCEPTED") StatusSuccess else StatusWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(data.staff, key = { it.id }) { staff ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(staff.name, style = MaterialTheme.typography.titleSmall)
                            Text(staff.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                staff.role,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = Emerald900
                            )
                        }
                        if (staff.userId != currentUser.userId && staff.role == "KARYAWAN") {
                            Switch(
                                checked = staff.status == "ACTIVE",
                                onCheckedChange = { viewModel.toggleStaffStatus(staff) }
                            )
                        } else {
                            StatusBadge(staff.status, StatusSuccessBg, StatusSuccess)
                        }
                    }
                }
            }
        }
    }

    if (showScriptDialog) {
        AppsScriptTemplateDialog(onDismiss = { showScriptDialog = false })
    }
}
