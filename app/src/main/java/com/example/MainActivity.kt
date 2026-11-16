package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.data.remote.SheetsSyncAdapter
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.SuperAdminRepository
import com.example.data.repository.TenantOperationsRepository
import com.example.ui.screens.CompanyMainShell
import com.example.ui.screens.InactiveAccountScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SuperAdminShell
import com.example.ui.screens.TenantOnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CompanyWorkspaceViewModel
import com.example.ui.viewmodel.SuperAdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val firestore = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        val sessionRepo = AuthSessionRepository(firestore)
        val superAdminRepo = SuperAdminRepository(firestore)
        val tenantRepo = TenantOperationsRepository(firestore)
        val sheetsAdapter = SheetsSyncAdapter()
        val authViewModel = AuthViewModel(sessionRepo)

        setContent {
            MyApplicationTheme {
                CpoLogisticsAppRoot(
                    authViewModel = authViewModel,
                    superAdminRepo = superAdminRepo,
                    tenantRepo = tenantRepo,
                    sheetsAdapter = sheetsAdapter
                )
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
private fun CpoLogisticsAppRoot(
    authViewModel: AuthViewModel,
    superAdminRepo: SuperAdminRepository,
    tenantRepo: TenantOperationsRepository,
    sheetsAdapter: SheetsSyncAdapter
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isBusy by authViewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by authViewModel.actionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignIn(context, credentialManager)
        Firebase.auth.authStateFlow().collect { firebaseUser ->
            authViewModel.onFirebaseUserChanged(firebaseUser)
        }
    }

    when (val state = authState) {
        is AuthUiState.CheckingSession -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Memeriksa sesi & isolasi tenant...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is AuthUiState.SignedOut -> {
            SignInScreen(
                isBusy = isBusy,
                errorMessage = actionMessage,
                onGoogleSignIn = { authViewModel.signInWithGoogle(context, credentialManager) },
                onDismissError = { authViewModel.clearMessage() }
            )
        }

        is AuthUiState.OnboardingRequired -> {
            TenantOnboardingScreen(
                email = state.email,
                displayName = state.displayName,
                pendingInvite = state.pendingInvite,
                isBusy = isBusy,
                errorMessage = actionMessage,
                onAcceptInvite = { invite -> authViewModel.acceptPendingInvite(state.firebaseUser, invite) },
                onRegisterCompany = { name, code, phone, address, sheetId, scriptUrl ->
                    authViewModel.registerNewCompany(
                        user = state.firebaseUser,
                        companyName = name,
                        companyCode = code,
                        phone = phone,
                        address = address,
                        spreadsheetId = sheetId,
                        appsScriptUrl = scriptUrl
                    )
                },
                onRefreshInvite = { authViewModel.resolveSession(state.firebaseUser) },
                onSignOut = { authViewModel.signOut(credentialManager) }
            )
        }

        is AuthUiState.InactiveAccount -> {
            InactiveAccountScreen(
                user = state.userProfile,
                onSignOut = { authViewModel.signOut(credentialManager) }
            )
        }

        is AuthUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(modifier = Modifier.widthIn(max = 460.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Gagal Memuat Sesi", style = MaterialTheme.typography.headlineSmall)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        RowForErrorActions(
                            onRetry = {
                                val u = Firebase.auth.currentUser
                                if (u != null) authViewModel.resolveSession(u)
                                else authViewModel.onFirebaseUserChanged(null)
                            },
                            onSignOut = { authViewModel.signOut(credentialManager) }
                        )
                    }
                }
            }
        }

        is AuthUiState.Authenticated -> {
            val user = state.userProfile
            key(user.userId, user.companyId, user.role) {
                if (user.roleEnum == UserRole.SUPER_ADMIN) {
                    val vm = remember(user.userId) { SuperAdminViewModel(superAdminRepo, user) }
                    SuperAdminShell(
                        viewModel = vm,
                        currentUser = user,
                        onSignOut = { authViewModel.signOut(credentialManager) }
                    )
                } else {
                    val vm = remember(user.userId, user.companyId) {
                        CompanyWorkspaceViewModel(tenantRepo, sheetsAdapter, user)
                    }
                    CompanyMainShell(
                        viewModel = vm,
                        currentUser = user,
                        onSignOut = { authViewModel.signOut(credentialManager) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowForErrorActions(
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onRetry) { Text("Coba Lagi") }
        OutlinedButton(onClick = onSignOut) { Text("Keluar") }
    }
}
