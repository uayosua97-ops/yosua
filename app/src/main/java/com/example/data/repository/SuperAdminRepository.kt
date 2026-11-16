package com.example.data.repository

import com.example.data.model.ActivityLog
import com.example.data.model.Company
import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
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

class SuperAdminRepository(private val db: FirebaseFirestore) {

    fun observeCompanies(): Flow<List<Company>> =
        db.collection("companies")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Company::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllUsers(): Flow<List<CompanyUser>> =
        db.collection("company_users")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CompanyUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.companyName }
            }

    fun observeAllInvites(): Flow<List<CompanyInvite>> =
        db.collection("company_invites")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CompanyInvite::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observePlatformLogs(): Flow<List<ActivityLog>> =
        db.collection("super_admin_logs")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ActivityLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    suspend fun createCompany(
        actor: CompanyUser,
        name: String,
        code: String,
        address: String,
        phone: String,
        email: String,
        spreadsheetId: String,
        appsScriptUrl: String,
        subscriptionPlan: String,
        shrinkageTolerancePct: Double,
        defaultRatePerTon: Double
    ): Result<String> {
        val companyId = "comp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("companies").document(companyId)
        return try {
            val status = when {
                spreadsheetId.isNotBlank() && appsScriptUrl.isNotBlank() -> "CONNECTED"
                spreadsheetId.isNotBlank() -> "PENDING"
                else -> "DISCONNECTED"
            }
            docRef.set(
                mapOf(
                    "name" to name.trim(),
                    "code" to code.trim().uppercase(Locale.ROOT),
                    "address" to address.trim(),
                    "phone" to phone.trim(),
                    "email" to email.trim().lowercase(Locale.ROOT),
                    "spreadsheetId" to spreadsheetId.trim(),
                    "appsScriptUrl" to appsScriptUrl.trim(),
                    "connectionStatus" to status,
                    "subscriptionPlan" to subscriptionPlan,
                    "isActive" to true,
                    "shrinkageTolerancePct" to shrinkageTolerancePct,
                    "defaultRatePerTon" to defaultRatePerTon,
                    "ownerUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logPlatformAction(actor, "CREATE_COMPANY", "COMPANY", companyId, "Mendaftarkan perusahaan ${name.trim()} ($code)")
            Result.success(companyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCompany(
        actor: CompanyUser,
        company: Company
    ): Result<Unit> {
        require(company.id.isNotBlank()) { "Company ID missing" }
        val docRef = db.collection("companies").document(company.id)
        return try {
            val status = when {
                company.spreadsheetId.isNotBlank() && company.appsScriptUrl.isNotBlank() -> "CONNECTED"
                company.spreadsheetId.isNotBlank() -> "PENDING"
                else -> "DISCONNECTED"
            }
            docRef.update(
                mapOf(
                    "name" to company.name,
                    "code" to company.code,
                    "address" to company.address,
                    "phone" to company.phone,
                    "email" to company.email,
                    "spreadsheetId" to company.spreadsheetId,
                    "appsScriptUrl" to company.appsScriptUrl,
                    "connectionStatus" to status,
                    "subscriptionPlan" to company.subscriptionPlan,
                    "shrinkageTolerancePct" to company.shrinkageTolerancePct,
                    "defaultRatePerTon" to company.defaultRatePerTon,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logPlatformAction(actor, "UPDATE_COMPANY", "COMPANY", company.id, "Memperbarui konfigurasi ${company.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleCompanyActive(
        actor: CompanyUser,
        company: Company,
        active: Boolean
    ): Result<Unit> {
        require(company.id.isNotBlank()) { "Company ID missing" }
        val docRef = db.collection("companies").document(company.id)
        return try {
            docRef.update(
                mapOf(
                    "isActive" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logPlatformAction(
                actor,
                if (active) "ACTIVATE_COMPANY" else "DEACTIVATE_COMPANY",
                "COMPANY",
                company.id,
                "${if (active) "Mengaktifkan" else "Menonaktifkan"} tenant ${company.name}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createUserInvite(
        actor: CompanyUser,
        email: String,
        companyId: String,
        companyName: String,
        role: String
    ): Result<String> {
        val inviteId = "inv_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("company_invites").document(inviteId)
        val emailLower = email.trim().lowercase(Locale.ROOT)
        return try {
            docRef.set(
                mapOf(
                    "email" to emailLower,
                    "companyId" to companyId,
                    "companyName" to companyName,
                    "role" to role,
                    "status" to "PENDING",
                    "invitedByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logPlatformAction(actor, "CREATE_INVITE", "USERS", inviteId, "Mengundang $emailLower sebagai $role di $companyName")
            Result.success(inviteId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleUserStatus(
        actor: CompanyUser,
        user: CompanyUser,
        newStatus: String
    ): Result<Unit> {
        require(user.id.isNotBlank()) { "User ID missing" }
        val docRef = db.collection("company_users").document(user.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logPlatformAction(actor, "TOGGLE_USER_STATUS", "USERS", user.id, "Status ${user.email} -> $newStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun logPlatformAction(
        actor: CompanyUser,
        action: String,
        module: String,
        targetId: String,
        details: String
    ) {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("super_admin_logs").document(logId)
        runCatching {
            docRef.set(
                mapOf(
                    "companyId" to "platform",
                    "actorUid" to actor.userId,
                    "actorName" to actor.name,
                    "actorRole" to actor.role,
                    "action" to action,
                    "module" to module,
                    "targetId" to targetId,
                    "details" to details,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
