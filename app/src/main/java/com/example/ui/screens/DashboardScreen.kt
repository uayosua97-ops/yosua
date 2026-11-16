package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanyUser
import com.example.ui.components.BarDatum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SimpleBarChartCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TonnageComparisonChartCard
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald900
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.viewmodel.CompanyWorkspaceData
import com.example.util.ReportExportHelper

@Composable
fun CompanyDashboardScreen(
    data: CompanyWorkspaceData,
    currentUser: CompanyUser,
    isSyncing: Boolean,
    onSyncSheets: () -> Unit,
    onNavigateModule: (CompanyModuleRoute) -> Unit
) {
    val company = data.company
    val isSpv = currentUser.role == "SPV"

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = company?.name ?: currentUser.companyName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Sheet: ${company?.spreadsheetId?.ifBlank { "Belum diatur" } ?: "-"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        StatusBadge(
                            label = if (company?.spreadsheetId?.isNotBlank() == true) "Sheet Ready" else "Lokal Cloud",
                            containerColor = if (company?.spreadsheetId?.isNotBlank() == true) StatusSuccessBg else StatusWarningBg,
                            contentColor = if (company?.spreadsheetId?.isNotBlank() == true) StatusSuccess else StatusWarning
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigateModule(CompanyModuleRoute.SHIPMENTS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buat DO")
                        }
                        OutlinedButton(
                            onClick = onSyncSheets,
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSyncing) "Sync..." else "Sync Sheet")
                        }
                    }
                }
            }
        }

        // KPI Grid Row 1
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Total Pengiriman",
                    value = "${data.shipments.size}",
                    subtitle = "${data.activeShipmentsCount} berjalan",
                    icon = Icons.Filled.LocalShipping,
                    accentColor = Emerald900,
                    modifier = Modifier.weight(1f).clickable { onNavigateModule(CompanyModuleRoute.SHIPMENTS) }
                )
                KpiMetricCard(
                    title = "Selesai / Tertunda",
                    value = "${data.completedShipmentsCount} / ${data.pendingShipmentsCount}",
                    subtitle = "DO terverifikasi",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = StatusSuccess,
                    modifier = Modifier.weight(1f).clickable { onNavigateModule(CompanyModuleRoute.MONITORING) }
                )
            }
        }

        // KPI Grid Row 2
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Total Tonase CPO",
                    value = ReportExportHelper.formatTons(data.totalCpoTonnage),
                    subtitle = "${data.weighings.size} tiket timbang",
                    icon = Icons.Filled.OilBarrel,
                    accentColor = Amber600,
                    modifier = Modifier.weight(1f).clickable { onNavigateModule(CompanyModuleRoute.WEIGHING) }
                )
                KpiMetricCard(
                    title = "Armada & Driver Aktif",
                    value = "${data.activeVehiclesCount} / ${data.activeDriversCount}",
                    subtitle = "${data.vehicles.size} unit • ${data.drivers.size} supir",
                    icon = Icons.Filled.DirectionsCar,
                    accentColor = StatusInfo,
                    modifier = Modifier.weight(1f).clickable { onNavigateModule(CompanyModuleRoute.FLEET) }
                )
            }
        }

        // Financial KPIs (SPV full visibility, Karyawan limited operational summary)
        if (isSpv) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiMetricCard(
                        title = "Total Pendapatan",
                        value = ReportExportHelper.formatIdr(data.totalRevenue),
                        subtitle = "Tagihan: ${ReportExportHelper.formatIdr(data.totalBilledAmount)}",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = Emerald800,
                        modifier = Modifier.weight(1f).clickable { onNavigateModule(CompanyModuleRoute.FINANCE) }
                    )
                    KpiMetricCard(
                        title = "Biaya Operasional",
                        value = ReportExportHelper.formatIdr(data.totalOperationalCost),
                        subtitle = "${data.costs.count { it.approvalStatus == "PENDING" }} menunggu SPV",
                        icon = Icons.Filled.Payments,
                        accentColor = StatusDanger,
                        modifier = Modifier.weight(1f).clickable { onNavigateModule(CompanyModuleRoute.FINANCE) }
                    )
                }
            }
        }

        // Charts
        item {
            SimpleBarChartCard(
                title = "Grafik Pengiriman Berdasarkan Status",
                subtitle = "Distribusi operasional pengiriman CPO perusahaan",
                bars = listOf(
                    BarDatum("Draft/Tunda", data.pendingShipmentsCount.toFloat(), StatusWarning),
                    BarDatum("Berjalan", data.activeShipmentsCount.toFloat(), Amber600),
                    BarDatum("Selesai", data.completedShipmentsCount.toFloat(), Emerald900),
                    BarDatum(
                        "Batal/Kendala",
                        data.shipments.count { it.status in setOf("ISSUE", "CANCELLED") }.toFloat(),
                        StatusDanger
                    )
                )
            )
        }

        item {
            TonnageComparisonChartCard(
                loadTons = data.totalLoadTonnage,
                unloadTons = data.totalUnloadTonnage,
                billableTons = data.totalCpoTonnage
            )
        }

        // Quick Module Grid
        item {
            Text("Modul Operasional", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickModuleChip("Armada", Icons.Filled.DirectionsCar, Modifier.weight(1f)) {
                    onNavigateModule(CompanyModuleRoute.FLEET)
                }
                QuickModuleChip("Driver", Icons.Filled.People, Modifier.weight(1f)) {
                    onNavigateModule(CompanyModuleRoute.DRIVERS)
                }
                QuickModuleChip("Pelanggan", Icons.Filled.ReceiptLong, Modifier.weight(1f)) {
                    onNavigateModule(CompanyModuleRoute.CUSTOMERS)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickModuleChip("Timbangan", Icons.Filled.Scale, Modifier.weight(1f)) {
                    onNavigateModule(CompanyModuleRoute.WEIGHING)
                }
                QuickModuleChip("Keuangan", Icons.Filled.Payments, Modifier.weight(1f)) {
                    onNavigateModule(CompanyModuleRoute.FINANCE)
                }
                QuickModuleChip("Laporan", Icons.Filled.Assessment, Modifier.weight(1f)) {
                    onNavigateModule(CompanyModuleRoute.REPORTS)
                }
            }
        }

        // Recent Activity
        item {
            Text("Aktivitas Operasional Terbaru", style = MaterialTheme.typography.titleMedium)
        }
        if (data.logs.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Aktivitas",
                    message = "Aktivitas pembuatan DO, input timbangan, dan biaya akan tercatat secara real-time di sini.",
                    icon = Icons.Filled.HourglassTop
                )
            }
        } else {
            items(data.logs.take(8), key = { it.id }) { log ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(log.action, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Emerald900)
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                            Text("${log.actorName} • ${log.module}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickModuleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Emerald900, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
