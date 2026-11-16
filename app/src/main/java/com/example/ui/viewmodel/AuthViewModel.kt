package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.data.remote.friendlyMessage
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.SessionResolution
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface AuthUiState {
    data object CheckingSession : AuthUiState
    data object SignedOut : AuthUiState
    data class OnboardingRequired(
        val firebaseUser: FirebaseUser,
        val email: String,
        val displayName: String,
        val pendingInvite: CompanyInvite?
    ) : AuthUiState
    data class InactiveAccount(val userProfile: CompanyUser) : AuthUiState
    data class Authenticated(val userProfile: CompanyUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val sessionRepository: AuthSessionRepository
) : ViewModel() {

    private val auth = Firebase.auth

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun onFirebaseUserChanged(user: FirebaseUser?) {
        if (user == null) {
            _authState.value = AuthUiState.SignedOut
            return
        }
        resolveSession(user)
    }

    fun attemptAutoSignIn(context: Context, credentialManager: CredentialManager) {
        if (auth.currentUser != null) {
            resolveSession(auth.currentUser!!)
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            _authState.value = AuthUiState.SignedOut
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCred = GoogleAuthProvider.getCredential(token, null)
                    val authRes = auth.signInWithCredential(authCred).await()
                    authRes.user?.let { resolveSession(it) } ?: run {
                        _authState.value = AuthUiState.SignedOut
                    }
                } else {
                    _authState.value = AuthUiState.SignedOut
                }
            } catch (_: Exception) {
                _authState.value = AuthUiState.SignedOut
            }
        }
    }

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            _actionMessage.value = "Konfigurasi Google Sign-In tidak ditemukan."
            return
        }
        val activity = context as? Activity
        if (activity == null) {
            _actionMessage.value = "Activity context tidak tersedia untuk Google Sign-In."
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let { resolveSession(it) }
                } else {
                    _actionMessage.value = "Tipe kredensial tidak valid."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _actionMessage.value = e.localizedMessage ?: "Gagal masuk dengan akun Google."
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun resolveSession(user: FirebaseUser) {
        viewModelScope.launch {
            _authState.value = AuthUiState.CheckingSession
            when (val res = sessionRepository.resolveOrBootstrapProfile(user)) {
                is SessionResolution.Loading -> _authState.value = AuthUiState.CheckingSession
                is SessionResolution.Ready -> {
                    val profile = res.userProfile
                    if (profile.status != "ACTIVE") {
                        _authState.value = AuthUiState.InactiveAccount(profile)
                    } else {
                        _authState.value = AuthUiState.Authenticated(profile)
                    }
                }
                is SessionResolution.NeedsOnboarding -> {
                    _authState.value = AuthUiState.OnboardingRequired(
                        firebaseUser = user,
                        email = res.email,
                        displayName = res.displayName,
                        pendingInvite = res.pendingInvite
                    )
                }
                is SessionResolution.Error -> {
                    _authState.value = AuthUiState.Error(res.message)
                }
            }
        }
    }

    fun acceptPendingInvite(user: FirebaseUser, invite: CompanyInvite) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = sessionRepository.acceptPendingInvite(user, invite)
            _isBusy.value = false
            res.fold(
                onSuccess = { profile ->
                    _authState.value = AuthUiState.Authenticated(profile)
                },
                onFailure = { err ->
                    _actionMessage.value = err.friendlyMessage("Gagal menerima undangan perusahaan.")
                }
            )
        }
    }

    fun registerNewCompany(
        user: FirebaseUser,
        companyName: String,
        companyCode: String,
        phone: String,
        address: String,
        spreadsheetId: String,
        appsScriptUrl: String
    ) {
        if (companyName.trim().length < 2 || companyCode.trim().length < 2) {
            _actionMessage.value = "Nama dan kode perusahaan minimal 2 karakter."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = sessionRepository.registerNewCompanyWorkspace(
                firebaseUser = user,
                companyName = companyName,
                companyCode = companyCode,
                phone = phone,
                address = address,
                spreadsheetId = spreadsheetId,
                appsScriptUrl = appsScriptUrl
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { profile ->
                    _authState.value = AuthUiState.Authenticated(profile)
                },
                onFailure = { err ->
                    _actionMessage.value = err.friendlyMessage("Gagal mendaftarkan workspace perusahaan.")
                }
            )
        }
    }

    fun signOut(credentialManager: CredentialManager) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    _authState.value = AuthUiState.SignedOut
                }
            }
        }
    }

    private companion object {
        const val TAG = "AuthVM"
    }
}
