package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanyUser
import com.example.data.model.Invoice
import com.example.data.model.OperationalCost
import com.example.data.model.Shipment
import com.example.data.model.WeighingRecord
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald800
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun WeighingScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    var search by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WeighingRecord?>(null) }

    val isSpv = currentUser.role == "SPV"
    val filtered = remember(data.weighings, search) {
        if (search.isBlank()) data.weighings
        else data.weighings.filter { w ->
            w.doNumber.contains(search, ignoreCase = true) ||
                w.loadTicketNumber.contains(search, ignoreCase = true) ||
                w.unloadTicketNumber.contains(search, ignoreCase = true)
        }
    }

    val totalDiff = remember(data.weighings) { data.weighings.sumOf { it.differenceTonnage } }
    val overCount = remember(data.weighings) { data.weighings.count { it.isOverShrinkage } }
    val totalValue = remember(data.weighings) { data.weighings.sumOf { it.totalDeliveryValue } }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Timbangan & Tonase CPO", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Toleransi susut tenant: ${data.company?.shrinkageTolerancePct ?: 0.5}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    enabled = data.shipments.isNotEmpty(),
                    modifier = Modifier.testTag("add_weighing_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Timbang")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Total Tonase Tagih",
                    value = ReportExportHelper.formatTons(data.weighings.sumOf { it.billableTonnage }),
                    subtitle = "${data.weighings.size} tiket tercatat",
                    icon = Icons.Filled.Scale,
                    accentColor = Emerald900,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Susut & Klaim",
                    value = ReportExportHelper.formatTons(totalDiff),
                    subtitle = "$overCount melebihi toleransi",
                    icon = Icons.Filled.Warning,
                    accentColor = if (overCount > 0) StatusDanger else Amber600,
                    modifier = Modifier.weight(1f)
                )
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
                        "Total Nilai Pengiriman Terukur",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        ReportExportHelper.formatIdr(totalValue),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari nomor DO atau tiket timbang...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (data.shipments.isEmpty()) "Buat DO Terlebih Dahulu" else "Belum Ada Data Timbangan",
                    message = if (data.shipments.isEmpty()) {
                        "Input timbangan muat dan bongkar membutuhkan Delivery Order aktif."
                    } else {
                        "Catat tiket timbang pabrik muat dan pelabuhan/pabrik bongkar untuk menghitung susut otomatis."
                    },
                    icon = Icons.Filled.Scale
                )
            }
        } else {
            items(filtered, key = { it.id }) { record ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    record.doNumber,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                                )
                                Text(
                                    "Muat: ${record.loadTicketNumber} • Bongkar: ${record.unloadTicketNumber.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                label = if (record.isOverShrinkage) "Susut Berlebih" else "Dalam Toleransi",
                                containerColor = if (record.isOverShrinkage) StatusDangerBg else StatusSuccessBg,
                                contentColor = if (record.isOverShrinkage) StatusDanger else StatusSuccess
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricMiniCol("Bruto/Muat", ReportExportHelper.formatTons(record.loadTonnage))
                            MetricMiniCol("Bongkar", ReportExportHelper.formatTons(record.unloadTonnage))
                            MetricMiniCol("Selisih", ReportExportHelper.formatTons(record.differenceTonnage))
                            MetricMiniCol("Ditagih", ReportExportHelper.formatTons(record.billableTonnage))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Toleransi ${record.tolerancePct}% (${ReportExportHelper.formatTons(record.toleranceTons)}) • Nilai: ${ReportExportHelper.formatIdr(record.totalDeliveryValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald900,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSpv) {
                                IconButton(onClick = { deleteTarget = record }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Timbangan", tint = StatusDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && data.shipments.isNotEmpty()) {
        WeighingInputDialog(
            shipments = data.shipments,
            defaultTolerancePct = data.company?.shrinkageTolerancePct ?: 0.5,
            isSaving = isSaving,
            onDismiss = { showCreateDialog = false },
            onSave = { shipment, loadTicket, unloadTicket, loadTons, unloadTons, tolPct ->
                viewModel.saveWeighing(shipment, loadTicket, unloadTicket, loadTons, unloadTons, tolPct)
                showCreateDialog = false
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Data Timbangan?",
            message = "Data tiket timbang DO ${target.doNumber} akan dihapus permanen.",
            onConfirm = {
                viewModel.deleteWeighing(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun MetricMiniCol(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeighingInputDialog(
    shipments: List<Shipment>,
    defaultTolerancePct: Double,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Shipment, String, String, Double, Double, Double) -> Unit
) {
    var selectedShipment by remember { mutableStateOf(shipments.first()) }
    var expanded by remember { mutableStateOf(false) }
    var loadTicket by remember { mutableStateOf("TKT-M-${System.currentTimeMillis() % 10000}") }
    var unloadTicket by remember { mutableStateOf("") }
    var loadTonsStr by remember { mutableStateOf(selectedShipment.targetTonnage.toString()) }
    var unloadTonsStr by remember { mutableStateOf(selectedShipment.targetTonnage.toString()) }
    var tolPctStr by remember { mutableStateOf(defaultTolerancePct.toString()) }

    val loadTons = loadTonsStr.toDoubleOrNull() ?: 0.0
    val unloadTons = unloadTonsStr.toDoubleOrNull() ?: 0.0
    val tolPct = tolPctStr.toDoubleOrNull() ?: defaultTolerancePct
    val diff = max(0.0, loadTons - unloadTons)
    val tolTons = loadTons * (tolPct / 100.0)
    val isOver = unloadTons > 0.0 && diff > tolTons
    val excess = max(0.0, diff - tolTons)
    val billable = if (unloadTons > 0.0) {
        if (isOver) max(0.0, loadTons - excess) else loadTons
    } else loadTons
    val deliveryVal = billable * selectedShipment.pricePerTon

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Timbangan CPO") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = "${selectedShipment.doNumber} • ${selectedShipment.customerName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Delivery Order *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        shipments.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.doNumber} (${s.customerName})") },
                                onClick = {
                                    selectedShipment = s
                                    loadTonsStr = s.targetTonnage.toString()
                                    unloadTonsStr = s.targetTonnage.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = loadTicket,
                    onValueChange = { loadTicket = it },
                    label = { Text("No. Tiket Muat *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("weighing_load_ticket_input")
                )
                OutlinedTextField(
                    value = unloadTicket,
                    onValueChange = { unloadTicket = it },
                    label = { Text("No. Tiket Bongkar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = loadTonsStr,
                        onValueChange = { loadTonsStr = it },
                        label = { Text("Muat (Ton) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("weighing_load_tons_input")
                    )
                    OutlinedTextField(
                        value = unloadTonsStr,
                        onValueChange = { unloadTonsStr = it },
                        label = { Text("Bongkar (Ton)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("weighing_unload_tons_input")
                    )
                }
                OutlinedTextField(
                    value = tolPctStr,
                    onValueChange = { tolPctStr = it },
                    label = { Text("Toleransi Susut (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = if (isOver) StatusDangerBg else StatusSuccessBg,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isOver) "PERINGATAN: Susut melebihi batas toleransi!" else "Susut dalam batas toleransi normal",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOver) StatusDanger else StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selisih: ${ReportExportHelper.formatTons(diff)} | Batas: ${ReportExportHelper.formatTons(tolTons)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Tonase Tagih: ${ReportExportHelper.formatTons(billable)} • Nilai: ${ReportExportHelper.formatIdr(deliveryVal)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedShipment, loadTicket, unloadTicket, loadTons, unloadTons, tolPct) },
                enabled = !isSaving && loadTicket.isNotBlank() && loadTons > 0.0,
                modifier = Modifier.testTag("save_weighing_button")
            ) {
                Text("Simpan Timbangan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    viewModel: CompanyWorkspaceViewModel,
    isSaving: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCostDialog by remember { mutableStateOf(false) }
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var paymentTarget by remember { mutableStateOf<Invoice?>(null) }

    val isSpv = currentUser.role == "SPV"

    Column {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Biaya Operasional & P&L") },
                modifier = Modifier.testTag("tab_operational_costs")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Tagihan & Pembayaran") },
                modifier = Modifier.testTag("tab_invoices")
            )
        }

        if (selectedTab == 0) {
            val totalRevenue = data.costs.sumOf { it.revenue }
            val totalExpense = data.costs.sumOf { it.totalCost }
            val netProfit = totalRevenue - totalExpense

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biaya Operasional Trip", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (isSpv) "Persetujuan SPV & analisis laba bersih per DO" else "Input biaya trip (memerlukan persetujuan SPV)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showCostDialog = true },
                            enabled = data.shipments.isNotEmpty(),
                            modifier = Modifier.testTag("add_cost_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Input Biaya")
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiMetricCard(
                            title = "Total Biaya Trip",
                            value = ReportExportHelper.formatIdr(totalExpense),
                            subtitle = "${data.costs.count { it.approvalStatus == "PENDING" }} menunggu SPV",
                            icon = Icons.Filled.Payments,
                            accentColor = StatusDanger,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Laba Bersih Trip",
                            value = ReportExportHelper.formatIdr(netProfit),
                            subtitle = "Omzet: ${ReportExportHelper.formatIdr(totalRevenue)}",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = Emerald800,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (data.costs.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Belum Ada Catatan Biaya",
                            message = "Catat BBM, tol, uang jalan, gaji & kasbon driver untuk menghitung margin bersih tiap pengiriman CPO.",
                            icon = Icons.Filled.Payments
                        )
                    }
                } else {
                    items(data.costs, key = { it.id }) { cost ->
                        val (bg, fg) = when (cost.approvalStatus) {
                            "APPROVED" -> StatusSuccessBg to StatusSuccess
                            "REJECTED" -> StatusDangerBg to StatusDanger
                            else -> StatusWarningBg to StatusWarning
                        }
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "DO: ${cost.doNumber}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                                    )
                                    StatusBadge(cost.approvalStatus, bg, fg)
                                }
                                Text(
                                    "BBM ${ReportExportHelper.formatIdr(cost.fuelCost)} • Tol ${ReportExportHelper.formatIdr(cost.tollCost)} • Uang Jalan ${ReportExportHelper.formatIdr(cost.tripAllowance)} • Gaji/Kasbon ${ReportExportHelper.formatIdr(cost.driverSalary + cost.driverCashAdvance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricMiniCol("Total Biaya", ReportExportHelper.formatIdr(cost.totalCost))
                                    MetricMiniCol("Pendapatan", ReportExportHelper.formatIdr(cost.revenue))
                                    MetricMiniCol("Laba Bersih", ReportExportHelper.formatIdr(cost.netIncome))
                                }
                                if (cost.notes.isNotBlank()) {
                                    Text("Catatan: ${cost.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (isSpv && cost.approvalStatus == "PENDING") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.approveCost(cost, approved = true) },
                                            modifier = Modifier.weight(1f).testTag("approve_cost_button")
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Setujui")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.approveCost(cost, approved = false) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tolak")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val totalBilled = data.totalBilledAmount
            val totalPaid = data.totalPaidAmount
            val outstanding = max(0.0, totalBilled - totalPaid)

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tagihan & Piutang Pelanggan", style = MaterialTheme.typography.titleLarge)
                            Text("${data.invoices.size} invoice diterbitkan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSpv) {
                            Button(
                                onClick = { showInvoiceDialog = true },
                                enabled = data.shipments.isNotEmpty(),
                                modifier = Modifier.testTag("create_invoice_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buat Tagihan")
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiMetricCard(
                            title = "Total Tagihan",
                            value = ReportExportHelper.formatIdr(totalBilled),
                            subtitle = "Terbayar: ${ReportExportHelper.formatIdr(totalPaid)}",
                            icon = Icons.Filled.ReceiptLong,
                            accentColor = Emerald900,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Sisa Piutang",
                            value = ReportExportHelper.formatIdr(outstanding),
                            subtitle = "${data.invoices.count { it.paymentStatus != "PAID" }} belum lunas",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = if (outstanding > 0) Amber600 else StatusSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (data.invoices.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Belum Ada Tagihan",
                            message = if (isSpv) "Terbitkan tagihan dari Delivery Order atau hasil timbangan untuk melacak pembayaran pelanggan." else "Belum ada invoice perusahaan yang diterbitkan oleh SPV.",
                            icon = Icons.Filled.ReceiptLong
                        )
                    }
                } else {
                    items(data.invoices, key = { it.id }) { inv ->
                        val (bg, fg) = when (inv.paymentStatus) {
                            "PAID" -> StatusSuccessBg to StatusSuccess
                            "PARTIAL" -> StatusInfoBg to StatusInfo
                            else -> StatusWarningBg to StatusWarning
                        }
                        val remaining = max(0.0, inv.totalAmount - inv.paidAmount)
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            inv.invoiceNumber,
                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                                        )
                                        Text(
                                            "${inv.customerName} • DO ${inv.doNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(inv.paymentStatus, bg, fg)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricMiniCol("Tonase Tagih", ReportExportHelper.formatTons(inv.billableTonnage))
                                    MetricMiniCol("Total", ReportExportHelper.formatIdr(inv.totalAmount))
                                    MetricMiniCol("Sisa", ReportExportHelper.formatIdr(remaining))
                                }
                                Text(
                                    "Jatuh Tempo: ${inv.dueDate.ifBlank { "-" }} • Terbayar: ${ReportExportHelper.formatIdr(inv.paidAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSpv && inv.paymentStatus != "PAID") {
                                    OutlinedButton(
                                        onClick = { paymentTarget = inv },
                                        modifier = Modifier.fillMaxWidth().testTag("record_payment_button")
                                    ) {
                                        Text("Catat Pembayaran")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCostDialog && data.shipments.isNotEmpty()) {
        OperationalCostDialog(
            data = data,
            isSaving = isSaving,
            onDismiss = { showCostDialog = false },
            onSave = { shipment, fuel, toll, loading, service, allowance, salary, kasbon, extra, rev, notes ->
                viewModel.saveOperationalCost(
                    shipment = shipment,
                    fuelCost = fuel,
                    tollCost = toll,
                    loadingCost = loading,
                    serviceCost = service,
                    tripAllowance = allowance,
                    driverSalary = salary,
                    cashAdvance = kasbon,
                    additionalCost = extra,
                    revenue = rev,
                    notes = notes
                )
                showCostDialog = false
            }
        )
    }

    if (showInvoiceDialog && data.shipments.isNotEmpty()) {
        CreateInvoiceDialog(
            data = data,
            isSaving = isSaving,
            onDismiss = { showInvoiceDialog = false },
            onSave = { invNum, shipment, tons, dueDate, notes ->
                viewModel.createInvoice(invNum, shipment, tons, dueDate, notes)
                showInvoiceDialog = false
            }
        )
    }

    paymentTarget?.let { inv ->
        RecordPaymentDialog(
            invoice = inv,
            isSaving = isSaving,
            onDismiss = { paymentTarget = null },
            onSave = { amount, date, method ->
                viewModel.recordPayment(inv, amount, date, method)
                paymentTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationalCostDialog(
    data: CompanyWorkspaceData,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Shipment, Double, Double, Double, Double, Double, Double, Double, Double, Double, String) -> Unit
) {
    var selectedShipment by remember { mutableStateOf(data.shipments.first()) }
    var expanded by remember { mutableStateOf(false) }

    fun defaultRevenue(s: Shipment): Double {
        val w = data.weighings.firstOrNull { it.shipmentId == s.id }
        return w?.totalDeliveryValue ?: s.totalContractValue
    }

    var fuelStr by remember { mutableStateOf("1500000") }
    var tollStr by remember { mutableStateOf("350000") }
    var loadingStr by remember { mutableStateOf("200000") }
    var serviceStr by remember { mutableStateOf("0") }
    var allowanceStr by remember { mutableStateOf("600000") }
    var salaryStr by remember { mutableStateOf("800000") }
    var kasbonStr by remember { mutableStateOf("0") }
    var extraStr by remember { mutableStateOf("0") }
    var revenueStr by remember { mutableStateOf(defaultRevenue(selectedShipment).toLong().toString()) }
    var notes by remember { mutableStateOf("") }

    val fuel = fuelStr.toDoubleOrNull() ?: 0.0
    val toll = tollStr.toDoubleOrNull() ?: 0.0
    val loading = loadingStr.toDoubleOrNull() ?: 0.0
    val service = serviceStr.toDoubleOrNull() ?: 0.0
    val allowance = allowanceStr.toDoubleOrNull() ?: 0.0
    val salary = salaryStr.toDoubleOrNull() ?: 0.0
    val kasbon = kasbonStr.toDoubleOrNull() ?: 0.0
    val extra = extraStr.toDoubleOrNull() ?: 0.0
    val rev = revenueStr.toDoubleOrNull() ?: 0.0
    val totalCost = fuel + toll + loading + service + allowance + salary + kasbon + extra
    val net = rev - totalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Biaya Operasional Trip") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = "${selectedShipment.doNumber} • ${selectedShipment.customerName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Delivery Order *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        data.shipments.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.doNumber} (${s.customerName})") },
                                onClick = {
                                    selectedShipment = s
                                    revenueStr = defaultRevenue(s).toLong().toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostNumField("BBM (Rp)", fuelStr, { fuelStr = it }, Modifier.weight(1f))
                    CostNumField("Tol (Rp)", tollStr, { tollStr = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostNumField("Muat/Bongkar", loadingStr, { loadingStr = it }, Modifier.weight(1f))
                    CostNumField("Servis Jalan", serviceStr, { serviceStr = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostNumField("Uang Jalan", allowanceStr, { allowanceStr = it }, Modifier.weight(1f))
                    CostNumField("Gaji Driver", salaryStr, { salaryStr = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostNumField("Kasbon Driver", kasbonStr, { kasbonStr = it }, Modifier.weight(1f))
                    CostNumField("Biaya Tambahan", extraStr, { extraStr = it }, Modifier.weight(1f))
                }
                CostNumField("Pendapatan Trip (Rp)", revenueStr, { revenueStr = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Biaya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Biaya: ${ReportExportHelper.formatIdr(totalCost)}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Estimasi Laba Bersih: ${ReportExportHelper.formatIdr(net)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (net >= 0) Emerald900 else StatusDanger
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedShipment, fuel, toll, loading, service, allowance, salary, kasbon, extra, rev, notes) },
                enabled = !isSaving,
                modifier = Modifier.testTag("save_cost_button")
            ) {
                Text("Simpan Biaya")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun CostNumField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInvoiceDialog(
    data: CompanyWorkspaceData,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Shipment, Double, String, String) -> Unit
) {
    var selectedShipment by remember { mutableStateOf(data.shipments.first()) }
    var expanded by remember { mutableStateOf(false) }

    fun defaultBillableTons(s: Shipment): Double =
        data.weighings.firstOrNull { it.shipmentId == s.id }?.billableTonnage ?: s.targetTonnage

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 14L * 86400_000L)) }
    var invNumber by remember { mutableStateOf("INV-CPO-${System.currentTimeMillis() % 100000}") }
    var tonsStr by remember { mutableStateOf(defaultBillableTons(selectedShipment).toString()) }
    var dueDate by remember { mutableStateOf(today) }
    var notes by remember { mutableStateOf("") }

    val tons = tonsStr.toDoubleOrNull() ?: 0.0
    val totalAmt = tons * selectedShipment.pricePerTon

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terbitkan Tagihan (Invoice)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = "${selectedShipment.doNumber} • ${selectedShipment.customerName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Delivery Order *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        data.shipments.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.doNumber} (${s.customerName})") },
                                onClick = {
                                    selectedShipment = s
                                    tonsStr = defaultBillableTons(s).toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = invNumber,
                    onValueChange = { invNumber = it },
                    label = { Text("Nomor Tagihan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tonsStr,
                    onValueChange = { tonsStr = it },
                    label = { Text("Tonase Ditagih (Ton) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Jatuh Tempo (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Pembayaran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Total Nilai Tagihan: ${ReportExportHelper.formatIdr(totalAmt)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Emerald900
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(invNumber, selectedShipment, tons, dueDate, notes) },
                enabled = !isSaving && invNumber.isNotBlank() && tons > 0.0
            ) {
                Text("Terbitkan Invoice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun RecordPaymentDialog(
    invoice: Invoice,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    val remaining = max(0.0, invoice.totalAmount - invoice.paidAmount)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var amountStr by remember { mutableStateOf(remaining.toLong().toString()) }
    var payDate by remember { mutableStateOf(today) }
    var method by remember { mutableStateOf("Transfer Bank") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Pembayaran ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Sisa tagihan: ${ReportExportHelper.formatIdr(remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Nominal Dibayar (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payDate,
                    onValueChange = { payDate = it },
                    label = { Text("Tanggal Bayar (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Metode Pembayaran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSave(amt, payDate, method) },
                enabled = !isSaving && amt > 0.0
            ) {
                Text("Simpan Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
