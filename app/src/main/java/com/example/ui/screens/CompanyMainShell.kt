package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CompanyUser
import com.example.ui.components.EmptyStateCard
import com.example.ui.viewmodel.CompanyWorkspaceViewModel
import com.example.ui.viewmodel.WorkspaceUiState
import kotlinx.coroutines.launch

enum class CompanyModuleRoute(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    SHIPMENTS("Pengiriman", Icons.Filled.LocalShipping),
    MONITORING("Monitoring", Icons.Filled.MyLocation),
    WEIGHING("Timbangan", Icons.Filled.Scale),
    FINANCE("Keuangan", Icons.Filled.Payments),
    FLEET("Armada", Icons.Filled.DirectionsCar),
    DRIVERS("Driver", Icons.Filled.People),
    CUSTOMERS("Pelanggan", Icons.Filled.ReceiptLong),
    REPORTS("Laporan", Icons.Filled.Assessment),
    SETTINGS("Pengaturan", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyMainShell(
    viewModel: CompanyWorkspaceViewModel,
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

    var currentRoute by remember { mutableStateOf(CompanyModuleRoute.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = currentRoute != CompanyModuleRoute.DASHBOARD) {
        currentRoute = CompanyModuleRoute.DASHBOARD
    }

    val visibleModules = remember(currentUser.role) {
        if (currentUser.role == "SPV") {
            CompanyModuleRoute.entries
        } else {
            CompanyModuleRoute.entries.filter { it != CompanyModuleRoute.SETTINGS }
        }
    }

    val primaryBottomTabs = remember {
        listOf(
            CompanyModuleRoute.DASHBOARD,
            CompanyModuleRoute.SHIPMENTS,
            CompanyModuleRoute.MONITORING,
            CompanyModuleRoute.WEIGHING,
            CompanyModuleRoute.FINANCE
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = currentUser.companyName.ifBlank { "CPO Logistics Tenant" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentUser.name} • ${currentUser.roleEnum.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    visibleModules.forEach { module ->
                        NavigationDrawerItem(
                            label = { Text(module.label) },
                            icon = { Icon(module.icon, contentDescription = null) },
                            selected = currentRoute == module,
                            onClick = {
                                currentRoute = module
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("open_drawer_button")
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu Modul")
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = currentRoute.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentUser.companyName} • ${currentUser.role}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        actions = {
                            val readyData = (uiState as? WorkspaceUiState.Ready)?.data
                            if (readyData != null) {
                                IconButton(
                                    onClick = { viewModel.syncToGoogleSheets(readyData) },
                                    enabled = !isSaving,
                                    modifier = Modifier.testTag("topbar_sync_sheets")
                                ) {
                                    Icon(Icons.Filled.CloudSync, contentDescription = "Sync Google Sheets")
                                }
                            }
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            primaryBottomTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab,
                                    onClick = { currentRoute = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
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
                        NavigationRail(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            visibleModules.forEach { module ->
                                NavigationRailItem(
                                    selected = currentRoute == module,
                                    onClick = { currentRoute = module },
                                    icon = { Icon(module.icon, contentDescription = module.label) },
                                    label = { Text(module.label, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (val state = uiState) {
                            is WorkspaceUiState.Loading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            is WorkspaceUiState.Error -> {
                                EmptyStateCard(
                                    title = "Gagal Memuat Workspace",
                                    message = state.message,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            is WorkspaceUiState.Ready -> {
                                val data = state.data
                                when (currentRoute) {
                                    CompanyModuleRoute.DASHBOARD -> CompanyDashboardScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        isSyncing = isSaving,
                                        onSyncSheets = { viewModel.syncToGoogleSheets(data) },
                                        onNavigateModule = { currentRoute = it }
                                    )
                                    CompanyModuleRoute.SHIPMENTS -> ShipmentsScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                    CompanyModuleRoute.MONITORING -> MonitoringScreen(
                                        data = data
                                    )
                                    CompanyModuleRoute.WEIGHING -> WeighingScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                    CompanyModuleRoute.FINANCE -> FinanceScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                    CompanyModuleRoute.FLEET -> FleetScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                    CompanyModuleRoute.DRIVERS -> DriversScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                    CompanyModuleRoute.CUSTOMERS -> CustomersScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                    CompanyModuleRoute.REPORTS -> ReportsScreen(data = data)
                                    CompanyModuleRoute.SETTINGS -> CompanySettingsScreen(
                                        data = data,
                                        currentUser = currentUser,
                                        viewModel = viewModel,
                                        isSaving = isSaving
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
