package com.example.data.repository

import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

sealed interface SessionResolution {
    data object Loading : SessionResolution
    data class Ready(val userProfile: CompanyUser) : SessionResolution
    data class NeedsOnboarding(
        val uid: String,
        val email: String,
        val displayName: String,
        val pendingInvite: CompanyInvite? = null
    ) : SessionResolution
    data class Error(val message: String) : SessionResolution
}

class AuthSessionRepository(private val db: FirebaseFirestore) {

    fun observeUserProfile(uid: String): Flow<CompanyUser?> =
        db.collection("company_users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        CompanyUser::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }

    suspend fun resolveOrBootstrapProfile(firebaseUser: FirebaseUser): SessionResolution {
        val uid = firebaseUser.uid
        val emailLower = (firebaseUser.email ?: "").trim().lowercase(Locale.ROOT)
        val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: emailLower.substringBefore("@").ifBlank { "User" }

        val userDocRef = db.collection("company_users").document(uid)
        return try {
            val existingSnap = userDocRef.get().await()
            if (existingSnap.exists()) {
                val profile = existingSnap.toObject(
                    CompanyUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (profile != null) {
                    runCatching {
                        userDocRef.update(
                            mapOf(
                                "lastLoginAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    return SessionResolution.Ready(profile)
                }
            }

            // 1. Bootstrapped Super Admin check
            if (emailLower == SUPER_ADMIN_EMAIL && firebaseUser.isEmailVerified) {
                val payload = mapOf(
                    "userId" to uid,
                    "companyId" to "platform",
                    "companyName" to "CPO MGE Platform",
                    "name" to displayName,
                    "email" to emailLower,
                    "role" to "SUPER_ADMIN",
                    "status" to "ACTIVE",
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(payload).await()
                val created = userDocRef.get().await().toObject(
                    CompanyUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: CompanyUser(
                    id = uid,
                    userId = uid,
                    companyId = "platform",
                    companyName = "CPO MGE Platform",
                    name = displayName,
                    email = emailLower,
                    role = "SUPER_ADMIN",
                    status = "ACTIVE"
                )
                return SessionResolution.Ready(created)
            }

            // 2. Check pending invitation for verified email
            val pendingInvite = findPendingInvite(emailLower)
            if (pendingInvite != null) {
                return SessionResolution.NeedsOnboarding(
                    uid = uid,
                    email = emailLower,
                    displayName = displayName,
                    pendingInvite = pendingInvite
                )
            }

            SessionResolution.NeedsOnboarding(
                uid = uid,
                email = emailLower,
                displayName = displayName,
                pendingInvite = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userDocRef.path)
            SessionResolution.Error(e.localizedMessage ?: "Gagal memverifikasi profil pengguna")
        }
    }

    suspend fun findPendingInvite(emailLower: String): CompanyInvite? {
        if (emailLower.isBlank()) return null
        val path = "company_invites"
        return try {
            val snap = db.collection(path)
                .whereEqualTo("email", emailLower)
                .whereEqualTo("status", "PENDING")
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.toObject(
                CompanyInvite::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            null
        }
    }

    suspend fun acceptPendingInvite(
        firebaseUser: FirebaseUser,
        invite: CompanyInvite
    ): Result<CompanyUser> {
        val uid = firebaseUser.uid
        val emailLower = (firebaseUser.email ?: "").trim().lowercase(Locale.ROOT)
        val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: emailLower.substringBefore("@").ifBlank { "User" }
        val userRef = db.collection("company_users").document(uid)
        val inviteRef = db.collection("company_invites").document(invite.id)

        return try {
            val batch = db.batch()
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "companyId" to invite.companyId,
                    "companyName" to invite.companyName,
                    "name" to displayName,
                    "email" to emailLower,
                    "role" to invite.role,
                    "status" to "ACTIVE",
                    "inviteId" to invite.id,
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                inviteRef,
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            val created = userRef.get().await().toObject(
                CompanyUser::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CompanyUser(
                id = uid,
                userId = uid,
                companyId = invite.companyId,
                companyName = invite.companyName,
                name = displayName,
                email = emailLower,
                role = invite.role,
                status = "ACTIVE"
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun registerNewCompanyWorkspace(
        firebaseUser: FirebaseUser,
        companyName: String,
        companyCode: String,
        phone: String,
        address: String,
        spreadsheetId: String,
        appsScriptUrl: String
    ): Result<CompanyUser> {
        val uid = firebaseUser.uid
        val emailLower = (firebaseUser.email ?: "").trim().lowercase(Locale.ROOT)
        val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: emailLower.substringBefore("@").ifBlank { "Supervisor" }
        val newCompanyId = "comp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val companyRef = db.collection("companies").document(newCompanyId)
        val userRef = db.collection("company_users").document(uid)

        return try {
            val batch = db.batch()
            batch.set(
                companyRef,
                mapOf(
                    "name" to companyName.trim(),
                    "code" to companyCode.trim().uppercase(Locale.ROOT),
                    "address" to address.trim(),
                    "phone" to phone.trim(),
                    "email" to emailLower,
                    "spreadsheetId" to spreadsheetId.trim(),
                    "appsScriptUrl" to appsScriptUrl.trim(),
                    "connectionStatus" to if (spreadsheetId.isNotBlank()) "CONNECTED" else "PENDING",
                    "subscriptionPlan" to "ENTERPRISE",
                    "isActive" to true,
                    "shrinkageTolerancePct" to 0.5,
                    "defaultRatePerTon" to 450000.0,
                    "ownerUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "companyId" to newCompanyId,
                    "companyName" to companyName.trim(),
                    "name" to displayName,
                    "email" to emailLower,
                    "role" to "SPV",
                    "status" to "ACTIVE",
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            val created = userRef.get().await().toObject(
                CompanyUser::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CompanyUser(
                id = uid,
                userId = uid,
                companyId = newCompanyId,
                companyName = companyName.trim(),
                name = displayName,
                email = emailLower,
                role = "SPV",
                status = "ACTIVE"
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, companyRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val SUPER_ADMIN_EMAIL = "ua.yosua97@gmail.com"
    }
}
