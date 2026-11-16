package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950

@Composable
fun SignInScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onDismissError: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cpo_hero_banner),
                    contentDescription = "CPO Logistics MGE Command Center",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Emerald950.copy(alpha = 0.35f),
                                    Emerald950.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .statusBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Amber600,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "MULTI-COMPANY SAAS TMS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "CPO Logistics MGE",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Platform Transportasi & Manajemen Pengiriman Crude Palm Oil Terisolasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Masuk ke Portal Perusahaan",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Autentikasi menggunakan Google Credential Manager untuk memastikan isolasi tenant, audit trail, dan sinkronisasi Google Sheets perusahaan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FeatureRow(Icons.Filled.Security, "Isolasi Multi-Tenant", "Setiap perusahaan memiliki database & Google Spreadsheet terpisah.")
                        FeatureRow(Icons.Filled.VerifiedUser, "Kontrol Akses RBAC", "Pembatasan ketat Super Admin, SPV, dan Karyawan.")
                        FeatureRow(Icons.Filled.OilBarrel, "Operasional CPO Lengkap", "DO/Surat Jalan, Timbangan Susut, Biaya Trip & Laporan PDF/CSV.")

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = onDismissError) {
                                        Text("OK")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onGoogleSignIn,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Emerald900.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Emerald900, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TenantOnboardingScreen(
    email: String,
    displayName: String,
    pendingInvite: CompanyInvite?,
    isBusy: Boolean,
    errorMessage: String?,
    onAcceptInvite: (CompanyInvite) -> Unit,
    onRegisterCompany: (String, String, String, String, String, String) -> Unit,
    onRefreshInvite: () -> Unit,
    onSignOut: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var companyCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var spreadsheetId by remember { mutableStateOf("") }
    var appsScriptUrl by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktivasi Akun Tenant", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "$displayName ($email)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Keluar")
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (pendingInvite != null) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Emerald900)
                                Text("Undangan Perusahaan Ditemukan!", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "Anda diundang bergabung ke ${pendingInvite.companyName} dengan role ${pendingInvite.role}.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { onAcceptInvite(pendingInvite) },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth().testTag("accept_invite_button")
                            ) {
                                Text("Gabung ke ${pendingInvite.companyName}")
                            }
                        }
                    }
                } else {
                    ElevatedCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Menunggu Undangan SPV/Admin?", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Jika Super Admin atau SPV telah mengundang email Anda, tekan muat ulang.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = onRefreshInvite) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Cek")
                            }
                        }
                    }
                }

                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Business, contentDescription = null, tint = Emerald900)
                            Text("Daftarkan Workspace Perusahaan Baru", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Anda akan terdaftar sebagai Supervisor (SPV) pada tenant perusahaan terisolasi milik Anda sendiri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Nama Perusahaan *") },
                            placeholder = { Text("Contoh: PT Sawit Makmur") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_company_name")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = companyCode,
                                onValueChange = { companyCode = it },
                                label = { Text("Kode *") },
                                placeholder = { Text("SMK") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("reg_company_code")
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("No. Telepon") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Operasional") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = spreadsheetId,
                            onValueChange = { spreadsheetId = it },
                            label = { Text("Google Spreadsheet ID (Opsional)") },
                            placeholder = { Text("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = appsScriptUrl,
                            onValueChange = { appsScriptUrl = it },
                            label = { Text("URL Google Apps Script Web App (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onRegisterCompany(
                                    companyName,
                                    companyCode,
                                    phone,
                                    address,
                                    spreadsheetId,
                                    appsScriptUrl
                                )
                            },
                            enabled = !isBusy && companyName.isNotBlank() && companyCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_company_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Buat Perusahaan & Masuk Dashboard")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InactiveAccountScreen(user: CompanyUser, onSignOut: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(modifier = Modifier.widthIn(max = 460.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(44.dp))
                Text("Akun Dinonaktifkan", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Akun ${user.email} pada ${user.companyName} sedang berstatus ${user.status}. Hubungi Supervisor atau Super Admin Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onSignOut) {
                    Text("Keluar")
                }
            }
        }
    }
}
