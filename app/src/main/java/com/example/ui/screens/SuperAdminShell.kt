package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Company
import com.example.data.model.CompanyUser
import com.example.data.remote.SheetsSyncAdapter
import com.example.ui.components.BarDatum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SimpleBarChartCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Amber600
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
import com.example.ui.viewmodel.SuperAdminUiState
import com.example.ui.viewmodel.SuperAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminShell(
    viewModel: SuperAdminViewModel,
    currentUser: CompanyUser,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCompanyDialog by remember { mutableStateOf(false) }
    var editingCompany by remember { mutableStateOf<Company?>(null) }
    var inviteTargetCompany by remember { mutableStateOf<Company?>(null) }
    var showAppsScriptDialog by remember { mutableStateOf(false) }

    val navItems = listOf(
        Triple("Platform", Icons.Filled.Dashboard, 0),
        Triple("Perusahaan", Icons.Filled.Apartment, 1),
        Triple("Pengguna", Icons.Filled.Group, 2),
        Triple("Audit", Icons.Filled.History, 3)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("CPO MGE • Super Admin", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${currentUser.name} • Platform Controller",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = { showAppsScriptDialog = true }) {
                            Icon(Icons.Filled.Code, contentDescription = "Template Google Apps Script")
                        }
                        IconButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_button")) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { (label, icon, idx) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    FloatingActionButton(
                        onClick = {
                            editingCompany = null
                            showCompanyDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("add_company_fab")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah Perusahaan")
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEach { (label, icon, idx) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val state = uiState) {
                        is SuperAdminUiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        is SuperAdminUiState.Error -> {
                            EmptyStateCard(
                                title = "Gagal Memuat Data Platform",
                                message = state.message,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        is SuperAdminUiState.Ready -> {
                            val data = state.data
                            when (selectedTab) {
                                0 -> SuperAdminOverviewTab(
                                    companies = data.companies,
                                    users = data.users,
                                    invites = data.invites,
                                    onRegisterCompany = {
                                        editingCompany = null
                                        showCompanyDialog = true
                                    },
                                    onViewAppsScript = { showAppsScriptDialog = true }
                                )
                                1 -> SuperAdminCompaniesTab(
                                    companies = data.companies,
                                    onEdit = {
                                        editingCompany = it
                                        showCompanyDialog = true
                                    },
                                    onToggleActive = { viewModel.toggleCompanyStatus(it) },
                                    onInviteSpv = { inviteTargetCompany = it }
                                )
                                2 -> SuperAdminUsersTab(
                                    users = data.users,
                                    invites = data.invites,
                                    onToggleUserStatus = { viewModel.toggleUserStatus(it) }
                                )
                                3 -> SuperAdminAuditTab(logs = data.logs)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompanyDialog) {
        CompanyEditorDialog(
            existing = editingCompany,
            isSaving = isSaving,
            onDismiss = { showCompanyDialog = false },
            onSave = { name, code, address, phone, email, sheetId, scriptUrl, plan, tol, rate ->
                if (editingCompany == null) {
                    viewModel.createCompany(name, code, address, phone, email, sheetId, scriptUrl, plan, tol, rate)
                } else {
                    viewModel.updateCompany(editingCompany!!, name, code, address, phone, email, sheetId, scriptUrl, plan, tol, rate)
                }
                showCompanyDialog = false
            }
        )
    }

    inviteTargetCompany?.let { company ->
        InviteSpvDialog(
            company = company,
            isSaving = isSaving,
            onDismiss = { inviteTargetCompany = null },
            onInvite = { email ->
                viewModel.inviteSpv(email, company)
                inviteTargetCompany = null
            }
        )
    }

    if (showAppsScriptDialog) {
        AppsScriptTemplateDialog(onDismiss = { showAppsScriptDialog = false })
    }
}

@Composable
private fun SuperAdminOverviewTab(
    companies: List<Company>,
    users: List<CompanyUser>,
    invites: List<com.example.data.model.CompanyInvite>,
    onRegisterCompany: () -> Unit,
    onViewAppsScript: () -> Unit
) {
    val activeCount = companies.count { it.isActive }
    val connectedCount = companies.count { it.spreadsheetId.isNotBlank() }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = Emerald900)
                        Text("Kontrol Pusat Multi-Tenant SaaS", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Super Admin mendaftarkan tenant perusahaan, menghubungkan Spreadsheet ID masing-masing perusahaan, dan memprovision akun SPV tanpa mengakses transaksi operasional internal perusahaan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onRegisterCompany) {
                            Text("Daftarkan Perusahaan")
                        }
                        OutlinedButton(onClick = onViewAppsScript) {
                            Text("Script Google Sheets")
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Total Perusahaan",
                    value = "${companies.size}",
                    subtitle = "$activeCount aktif",
                    icon = Icons.Filled.Apartment,
                    accentColor = Emerald900,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Spreadsheet Terhubung",
                    value = "$connectedCount",
                    subtitle = "${companies.size - connectedCount} belum link",
                    icon = Icons.Filled.Link,
                    accentColor = Amber600,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Total Pengguna",
                    value = "${users.size}",
                    subtitle = "${users.count { it.role == "SPV" }} SPV • ${users.count { it.role == "KARYAWAN" }} Karyawan",
                    icon = Icons.Filled.Group,
                    accentColor = StatusInfo,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Undangan Pending",
                    value = "${invites.count { it.status == "PENDING" }}",
                    subtitle = "Menunggu aktivasi Google",
                    icon = Icons.Filled.PersonAdd,
                    accentColor = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SimpleBarChartCard(
                title = "Distribusi Tenant & Koneksi Database",
                subtitle = "Ringkasan status perusahaan terdaftar",
                bars = listOf(
                    BarDatum("Aktif", activeCount.toFloat(), Emerald900),
                    BarDatum("Nonaktif", (companies.size - activeCount).toFloat(), StatusDanger),
                    BarDatum("Sheet Link", connectedCount.toFloat(), Amber600)
                )
            )
        }
    }
}

@Composable
private fun SuperAdminCompaniesTab(
    companies: List<Company>,
    onEdit: (Company) -> Unit,
    onToggleActive: (Company) -> Unit,
    onInviteSpv: (Company) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(companies, query) {
        if (query.isBlank()) companies
        else companies.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true) ||
                it.spreadsheetId.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari nama perusahaan, kode, atau Spreadsheet ID...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum Ada Perusahaan Terdaftar",
                    message = "Tekan tombol + untuk mendaftarkan perusahaan angkutan CPO dan menghubungkan Spreadsheet ID miliknya."
                )
            }
        } else {
            items(filtered, key = { it.id }) { company ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                                Text(company.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Kode: ${company.code} • Plan: ${company.subscriptionPlan}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                label = if (company.isActive) "Aktif" else "Nonaktif",
                                containerColor = if (company.isActive) StatusSuccessBg else StatusDangerBg,
                                contentColor = if (company.isActive) StatusSuccess else StatusDanger
                            )
                        }
                        Text(
                            text = "Spreadsheet ID: ${company.spreadsheetId.ifBlank { "(Belum dihubungkan)" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = if (company.spreadsheetId.isNotBlank()) Emerald900 else StatusWarning
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onEdit(company) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit / Sheet")
                                }
                                Button(onClick = { onInviteSpv(company) }) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Undang SPV")
                                }
                            }
                            Switch(
                                checked = company.isActive,
                                onCheckedChange = { onToggleActive(company) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperAdminUsersTab(
    users: List<CompanyUser>,
    invites: List<com.example.data.model.CompanyInvite>,
    onToggleUserStatus: (CompanyUser) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Akun Terdaftar (${users.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (users.isEmpty()) {
            item { EmptyStateCard("Belum Ada Pengguna", "Pengguna akan muncul setelah login dengan Google.") }
        } else {
            items(users, key = { it.id }) { user ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(user.name, style = MaterialTheme.typography.titleSmall)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${user.companyName} • ${user.role}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald900
                            )
                        }
                        if (user.role != "SUPER_ADMIN") {
                            Switch(
                                checked = user.status == "ACTIVE",
                                onCheckedChange = { onToggleUserStatus(user) }
                            )
                        } else {
                            StatusBadge("Platform", StatusInfoBg, StatusInfo)
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Undangan SPV / Karyawan (${invites.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(invites, key = { it.id }) { inv ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(inv.email, style = MaterialTheme.typography.titleSmall)
                        Text("${inv.companyName} • ${inv.role}", style = MaterialTheme.typography.bodySmall)
                    }
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

@Composable
private fun SuperAdminAuditTab(logs: List<com.example.data.model.ActivityLog>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                EmptyStateCard("Audit Log Kosong", "Aktivitas pendaftaran perusahaan dan akun akan tercatat otomatis di sini.")
            }
        } else {
            items(logs, key = { it.id }) { log ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.action, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Emerald900)
                            Text(log.module, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(log.details, style = MaterialTheme.typography.bodyMedium)
                        Text("Oleh ${log.actorName} (${log.actorRole})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyEditorDialog(
    existing: Company?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var code by remember { mutableStateOf(existing?.code ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var sheetId by remember { mutableStateOf(existing?.spreadsheetId ?: "") }
    var scriptUrl by remember { mutableStateOf(existing?.appsScriptUrl ?: "") }
    var plan by remember { mutableStateOf(existing?.subscriptionPlan ?: "ENTERPRISE") }
    var tol by remember { mutableStateOf((existing?.shrinkageTolerancePct ?: 0.5).toString()) }
    var rate by remember { mutableStateOf((existing?.defaultRatePerTon ?: 450000.0).toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Daftarkan Perusahaan Baru" else "Edit Perusahaan & Spreadsheet") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Perusahaan *") }, singleLine = true)
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Kode Tenant *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telepon") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Perusahaan") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, singleLine = true)
                OutlinedTextField(value = sheetId, onValueChange = { sheetId = it }, label = { Text("Google Spreadsheet ID") }, singleLine = true)
                OutlinedTextField(value = scriptUrl, onValueChange = { scriptUrl = it }, label = { Text("Apps Script Web App URL") }, singleLine = true)
                OutlinedTextField(value = plan, onValueChange = { plan = it.uppercase() }, label = { Text("Paket (STARTER / ENTERPRISE / CUSTOM)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tol, onValueChange = { tol = it }, label = { Text("Susut %") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Tarif/Ton") }, modifier = Modifier.weight(1.4f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safePlan = if (plan in listOf("STARTER", "ENTERPRISE", "CUSTOM")) plan else "ENTERPRISE"
                    onSave(
                        name, code, address, phone, email, sheetId, scriptUrl, safePlan,
                        tol.toDoubleOrNull() ?: 0.5,
                        rate.toDoubleOrNull() ?: 450000.0
                    )
                },
                enabled = !isSaving && name.isNotBlank() && code.isNotBlank()
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun InviteSpvDialog(
    company: Company,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Undang SPV untuk ${company.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Masukkan alamat Gmail / Google Workspace calon Supervisor. Saat login pertama kali, akun akan otomatis terhubung ke tenant ${company.name}.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Google SPV *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email) },
                enabled = !isSaving && email.contains("@")
            ) {
                Text("Kirim Undangan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AppsScriptTemplateDialog(onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Apps Script Receiver Template") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Pasang script ini di Google Apps Script perusahaan lalu Deploy sebagai Web App agar tombol Sinkronisasi Google Sheets langsung menulis ke 14 tab spreadsheet perusahaan.",
                    style = MaterialTheme.typography.bodySmall
                )
                ElevatedCard {
                    Text(
                        text = SheetsSyncAdapter.APPS_SCRIPT_TEMPLATE.trim(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                clipboard.setText(AnnotatedString(SheetsSyncAdapter.APPS_SCRIPT_TEMPLATE.trim()))
                onDismiss()
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Script")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
