package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ActivityLog
import com.example.data.model.Company
import com.example.data.model.CompanyInvite
import com.example.data.model.CompanyUser
import com.example.data.model.Customer
import com.example.data.model.Driver
import com.example.data.model.Invoice
import com.example.data.model.MaintenanceRecord
import com.example.data.model.OperationalCost
import com.example.data.model.Shipment
import com.example.data.model.ShipmentStatus
import com.example.data.model.Vehicle
import com.example.data.model.WeighingRecord
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
import kotlin.math.max

class TenantOperationsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun companyDoc(companyId: String) = db.collection("companies").document(companyId)

    fun observeCompany(companyId: String): Flow<Company?> =
        companyDoc(companyId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(Company::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }

    fun observeCompanyStaff(companyId: String): Flow<List<CompanyUser>> =
        db.collection("company_users")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(CompanyUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name }
            }

    fun observeCompanyInvites(companyId: String): Flow<List<CompanyInvite>> =
        db.collection("company_invites")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(CompanyInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeCustomers(companyId: String): Flow<List<Customer>> =
        companyDoc(companyId).collection("customers")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name }
            }

    fun observeVehicles(companyId: String): Flow<List<Vehicle>> =
        companyDoc(companyId).collection("vehicles")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Vehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.plateNumber }
            }

    fun observeDrivers(companyId: String): Flow<List<Driver>> =
        companyDoc(companyId).collection("drivers")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Driver::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.fullName }
            }

    fun observeShipments(companyId: String): Flow<List<Shipment>> =
        companyDoc(companyId).collection("shipments")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Shipment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeWeighings(companyId: String): Flow<List<WeighingRecord>> =
        companyDoc(companyId).collection("weighings")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(WeighingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeOperationalCosts(companyId: String): Flow<List<OperationalCost>> =
        companyDoc(companyId).collection("operational_costs")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(OperationalCost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeInvoices(companyId: String): Flow<List<Invoice>> =
        companyDoc(companyId).collection("invoices")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Invoice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeMaintenance(companyId: String): Flow<List<MaintenanceRecord>> =
        companyDoc(companyId).collection("maintenance")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(MaintenanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeActivityLogs(companyId: String): Flow<List<ActivityLog>> =
        companyDoc(companyId).collection("activity_logs")
            .whereEqualTo("companyId", companyId)
            .snapshots()
            .map { snap ->
                snap.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    // --- COMPANY SETTINGS & TEAM ---
    suspend fun updateCompanySettings(
        actor: CompanyUser,
        company: Company,
        address: String,
        phone: String,
        email: String,
        spreadsheetId: String,
        appsScriptUrl: String,
        shrinkageTolerancePct: Double,
        defaultRatePerTon: Double
    ): Result<Unit> {
        require(company.id.isNotBlank())
        val docRef = companyDoc(company.id)
        return try {
            val status = when {
                spreadsheetId.isNotBlank() && appsScriptUrl.isNotBlank() -> "CONNECTED"
                spreadsheetId.isNotBlank() -> "PENDING"
                else -> "DISCONNECTED"
            }
            docRef.update(
                mapOf(
                    "address" to address.trim(),
                    "phone" to phone.trim(),
                    "email" to email.trim().lowercase(Locale.ROOT),
                    "spreadsheetId" to spreadsheetId.trim(),
                    "appsScriptUrl" to appsScriptUrl.trim(),
                    "connectionStatus" to status,
                    "shrinkageTolerancePct" to shrinkageTolerancePct,
                    "defaultRatePerTon" to defaultRatePerTon,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, company.id, "UPDATE_SETTINGS", "COMPANY_SETTINGS", company.id, "Memperbarui pengaturan operasional & koneksi Google Sheet")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun inviteKaryawan(actor: CompanyUser, company: Company, email: String): Result<String> {
        val inviteId = "inv_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("company_invites").document(inviteId)
        val emailLower = email.trim().lowercase(Locale.ROOT)
        return try {
            docRef.set(
                mapOf(
                    "email" to emailLower,
                    "companyId" to company.id,
                    "companyName" to company.name,
                    "role" to "KARYAWAN",
                    "status" to "PENDING",
                    "invitedByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, company.id, "INVITE_STAFF", "USERS", inviteId, "Mengundang karyawan $emailLower")
            Result.success(inviteId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleStaffStatus(actor: CompanyUser, staff: CompanyUser, newStatus: String): Result<Unit> {
        require(staff.id.isNotBlank())
        val docRef = db.collection("company_users").document(staff.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, staff.companyId, "UPDATE_STAFF_STATUS", "USERS", staff.id, "${staff.email} -> $newStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CUSTOMERS ---
    suspend fun saveCustomer(
        actor: CompanyUser,
        companyId: String,
        existing: Customer?,
        name: String,
        code: String,
        picName: String,
        phone: String,
        address: String,
        loadingLocation: String,
        unloadingLocation: String,
        defaultPricePerTon: Double
    ): Result<String> {
        val docId = existing?.id?.takeIf { it.isNotBlank() } ?: newId("cust")
        val docRef = companyDoc(companyId).collection("customers").document(docId)
        return try {
            if (existing == null) {
                docRef.set(
                    mapOf(
                        "companyId" to companyId,
                        "name" to name.trim(),
                        "code" to code.trim().uppercase(Locale.ROOT),
                        "picName" to picName.trim(),
                        "phone" to phone.trim(),
                        "address" to address.trim(),
                        "loadingLocation" to loadingLocation.trim(),
                        "unloadingLocation" to unloadingLocation.trim(),
                        "defaultPricePerTon" to defaultPricePerTon,
                        "isActive" to true,
                        "createdByUid" to actor.userId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                recordLog(actor, companyId, "CREATE_CUSTOMER", "CUSTOMERS", docId, "Menambahkan pelanggan ${name.trim()}")
            } else {
                docRef.update(
                    mapOf(
                        "name" to name.trim(),
                        "code" to code.trim().uppercase(Locale.ROOT),
                        "picName" to picName.trim(),
                        "phone" to phone.trim(),
                        "address" to address.trim(),
                        "loadingLocation" to loadingLocation.trim(),
                        "unloadingLocation" to unloadingLocation.trim(),
                        "defaultPricePerTon" to defaultPricePerTon,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                recordLog(actor, companyId, "UPDATE_CUSTOMER", "CUSTOMERS", docId, "Memperbarui pelanggan ${name.trim()}")
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existing == null) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(actor: CompanyUser, companyId: String, customerId: String): Result<Unit> {
        val docRef = companyDoc(companyId).collection("customers").document(customerId)
        return try {
            docRef.delete().await()
            recordLog(actor, companyId, "DELETE_CUSTOMER", "CUSTOMERS", customerId, "Menghapus pelanggan $customerId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- VEHICLES ---
    suspend fun saveVehicle(
        actor: CompanyUser,
        companyId: String,
        existing: Vehicle?,
        plateNumber: String,
        vehicleType: String,
        capacityTons: Double,
        ownerName: String,
        status: String,
        stnkExpiry: String,
        kirExpiry: String,
        lastServiceDate: String,
        isRoadworthy: Boolean
    ): Result<String> {
        val docId = existing?.id?.takeIf { it.isNotBlank() } ?: newId("veh")
        val docRef = companyDoc(companyId).collection("vehicles").document(docId)
        return try {
            if (existing == null) {
                docRef.set(
                    mapOf(
                        "companyId" to companyId,
                        "plateNumber" to plateNumber.trim().uppercase(Locale.ROOT),
                        "vehicleType" to vehicleType.trim(),
                        "capacityTons" to capacityTons,
                        "ownerName" to ownerName.trim(),
                        "status" to status,
                        "stnkExpiry" to stnkExpiry.trim(),
                        "kirExpiry" to kirExpiry.trim(),
                        "lastServiceDate" to lastServiceDate.trim(),
                        "isRoadworthy" to isRoadworthy,
                        "createdByUid" to actor.userId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                recordLog(actor, companyId, "CREATE_VEHICLE", "VEHICLES", docId, "Mendaftarkan armada ${plateNumber.uppercase(Locale.ROOT)}")
            } else {
                docRef.update(
                    mapOf(
                        "plateNumber" to plateNumber.trim().uppercase(Locale.ROOT),
                        "vehicleType" to vehicleType.trim(),
                        "capacityTons" to capacityTons,
                        "ownerName" to ownerName.trim(),
                        "status" to status,
                        "stnkExpiry" to stnkExpiry.trim(),
                        "kirExpiry" to kirExpiry.trim(),
                        "lastServiceDate" to lastServiceDate.trim(),
                        "isRoadworthy" to isRoadworthy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                recordLog(actor, companyId, "UPDATE_VEHICLE", "VEHICLES", docId, "Memperbarui armada ${plateNumber.uppercase(Locale.ROOT)}")
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existing == null) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteVehicle(actor: CompanyUser, companyId: String, vehicleId: String): Result<Unit> {
        val docRef = companyDoc(companyId).collection("vehicles").document(vehicleId)
        return try {
            docRef.delete().await()
            recordLog(actor, companyId, "DELETE_VEHICLE", "VEHICLES", vehicleId, "Menghapus armada $vehicleId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- DRIVERS ---
    suspend fun saveDriver(
        actor: CompanyUser,
        companyId: String,
        existing: Driver?,
        fullName: String,
        nik: String,
        phone: String,
        address: String,
        simNumber: String,
        simExpiry: String,
        assignedVehicleId: String,
        assignedPlate: String,
        status: String
    ): Result<String> {
        val docId = existing?.id?.takeIf { it.isNotBlank() } ?: newId("drv")
        val docRef = companyDoc(companyId).collection("drivers").document(docId)
        return try {
            if (existing == null) {
                docRef.set(
                    mapOf(
                        "companyId" to companyId,
                        "fullName" to fullName.trim(),
                        "nik" to nik.trim(),
                        "phone" to phone.trim(),
                        "address" to address.trim(),
                        "simNumber" to simNumber.trim(),
                        "simExpiry" to simExpiry.trim(),
                        "assignedVehicleId" to assignedVehicleId,
                        "assignedPlate" to assignedPlate,
                        "status" to status,
                        "totalTrips" to 0L,
                        "createdByUid" to actor.userId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                recordLog(actor, companyId, "CREATE_DRIVER", "DRIVERS", docId, "Menambahkan driver ${fullName.trim()}")
            } else {
                docRef.update(
                    mapOf(
                        "fullName" to fullName.trim(),
                        "nik" to nik.trim(),
                        "phone" to phone.trim(),
                        "address" to address.trim(),
                        "simNumber" to simNumber.trim(),
                        "simExpiry" to simExpiry.trim(),
                        "assignedVehicleId" to assignedVehicleId,
                        "assignedPlate" to assignedPlate,
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                recordLog(actor, companyId, "UPDATE_DRIVER", "DRIVERS", docId, "Memperbarui driver ${fullName.trim()}")
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existing == null) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDriver(actor: CompanyUser, companyId: String, driverId: String): Result<Unit> {
        val docRef = companyDoc(companyId).collection("drivers").document(driverId)
        return try {
            docRef.delete().await()
            recordLog(actor, companyId, "DELETE_DRIVER", "DRIVERS", driverId, "Menghapus driver $driverId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SHIPMENTS & DELIVERY ORDERS ---
    suspend fun createShipment(
        actor: CompanyUser,
        companyId: String,
        doNumber: String,
        suratJalanNumber: String,
        shipmentDate: String,
        customerId: String,
        customerName: String,
        loadingLocation: String,
        unloadingLocation: String,
        senderCompany: String,
        receiverCompany: String,
        productType: String,
        targetTonnage: Double,
        pricePerTon: Double,
        vehicleId: String,
        vehiclePlate: String,
        driverId: String,
        driverName: String,
        etaText: String,
        notes: String,
        submitForApproval: Boolean
    ): Result<String> {
        val docId = newId("shp")
        val docRef = companyDoc(companyId).collection("shipments").document(docId)
        val initialStatus = if (submitForApproval) ShipmentStatus.PENDING_APPROVAL.code else ShipmentStatus.DRAFT.code
        val history = listOf("${ShipmentStatus.fromCode(initialStatus).label} (${actor.name})")
        return try {
            docRef.set(
                mapOf(
                    "companyId" to companyId,
                    "doNumber" to doNumber.trim().uppercase(Locale.ROOT),
                    "suratJalanNumber" to suratJalanNumber.trim().uppercase(Locale.ROOT),
                    "shipmentDate" to shipmentDate.trim(),
                    "customerId" to customerId,
                    "customerName" to customerName.trim(),
                    "loadingLocation" to loadingLocation.trim(),
                    "unloadingLocation" to unloadingLocation.trim(),
                    "senderCompany" to senderCompany.trim(),
                    "receiverCompany" to receiverCompany.trim(),
                    "productType" to productType.trim(),
                    "targetTonnage" to targetTonnage,
                    "pricePerTon" to pricePerTon,
                    "totalContractValue" to (targetTonnage * pricePerTon),
                    "vehicleId" to vehicleId,
                    "vehiclePlate" to vehiclePlate,
                    "driverId" to driverId,
                    "driverName" to driverName,
                    "status" to initialStatus,
                    "etaText" to etaText.trim(),
                    "notes" to notes.trim(),
                    "statusHistory" to history,
                    "approvedByUid" to "",
                    "createdByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, companyId, "CREATE_SHIPMENT", "SHIPMENTS", docId, "Membuat DO ${doNumber.uppercase(Locale.ROOT)} ($initialStatus)")
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateShipmentStatus(
        actor: CompanyUser,
        shipment: Shipment,
        newStatus: ShipmentStatus,
        note: String = ""
    ): Result<Unit> {
        require(shipment.id.isNotBlank())
        val docRef = companyDoc(shipment.companyId).collection("shipments").document(shipment.id)
        return try {
            val timestampLabel = "${newStatus.label} • ${actor.name}" + if (note.isNotBlank()) " ($note)" else ""
            val updatedHistory = (shipment.statusHistory + timestampLabel).takeLast(30)
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.code,
                "statusHistory" to updatedHistory,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (note.isNotBlank()) updates["notes"] = note.trim()
            if (newStatus == ShipmentStatus.READY_TO_LOAD) updates["approvedByUid"] = actor.userId
            docRef.update(updates).await()
            recordLog(
                actor,
                shipment.companyId,
                "UPDATE_SHIPMENT_STATUS",
                "SHIPMENTS",
                shipment.id,
                "DO ${shipment.doNumber}: ${shipment.status} -> ${newStatus.code}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteShipment(actor: CompanyUser, companyId: String, shipmentId: String): Result<Unit> {
        val docRef = companyDoc(companyId).collection("shipments").document(shipmentId)
        return try {
            docRef.delete().await()
            recordLog(actor, companyId, "DELETE_SHIPMENT", "SHIPMENTS", shipmentId, "Menghapus pengiriman $shipmentId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- WEIGHING & TONNAGE ---
    suspend fun saveWeighingRecord(
        actor: CompanyUser,
        companyId: String,
        shipment: Shipment,
        loadTicketNumber: String,
        unloadTicketNumber: String,
        loadTonnage: Double,
        unloadTonnage: Double,
        tolerancePct: Double
    ): Result<String> {
        val docId = newId("wgh")
        val docRef = companyDoc(companyId).collection("weighings").document(docId)
        val diff = max(0.0, loadTonnage - unloadTonnage)
        val toleranceTons = loadTonnage * (tolerancePct / 100.0)
        val overShrinkage = unloadTonnage > 0.0 && diff > toleranceTons
        val excess = max(0.0, diff - toleranceTons)
        val billable = if (unloadTonnage > 0.0) {
            if (overShrinkage) max(0.0, loadTonnage - excess) else loadTonnage
        } else {
            loadTonnage
        }
        val totalDeliveryValue = billable * shipment.pricePerTon

        return try {
            docRef.set(
                mapOf(
                    "companyId" to companyId,
                    "shipmentId" to shipment.id,
                    "doNumber" to shipment.doNumber,
                    "loadTicketNumber" to loadTicketNumber.trim().uppercase(Locale.ROOT),
                    "unloadTicketNumber" to unloadTicketNumber.trim().uppercase(Locale.ROOT),
                    "loadTonnage" to loadTonnage,
                    "unloadTonnage" to unloadTonnage,
                    "differenceTonnage" to diff,
                    "tolerancePct" to tolerancePct,
                    "toleranceTons" to toleranceTons,
                    "billableTonnage" to billable,
                    "pricePerTon" to shipment.pricePerTon,
                    "totalDeliveryValue" to totalDeliveryValue,
                    "isOverShrinkage" to overShrinkage,
                    "recordedByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(
                actor,
                companyId,
                "RECORD_WEIGHING",
                "WEIGHING_DATA",
                docId,
                "Timbangan ${shipment.doNumber}: Muat ${loadTonnage}T / Bongkar ${unloadTonnage}T / Tagih ${String.format(Locale.US, "%.2f", billable)}T"
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWeighing(actor: CompanyUser, companyId: String, weighingId: String): Result<Unit> {
        val docRef = companyDoc(companyId).collection("weighings").document(weighingId)
        return try {
            docRef.delete().await()
            recordLog(actor, companyId, "DELETE_WEIGHING", "WEIGHING_DATA", weighingId, "Menghapus data timbangan $weighingId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- OPERATIONAL COSTS & P&L ---
    suspend fun saveOperationalCost(
        actor: CompanyUser,
        companyId: String,
        shipment: Shipment,
        fuelCost: Double,
        tollCost: Double,
        loadingUnloadingCost: Double,
        serviceCost: Double,
        tripAllowance: Double,
        driverSalary: Double,
        driverCashAdvance: Double,
        additionalCost: Double,
        revenue: Double,
        notes: String
    ): Result<String> {
        val docId = newId("cst")
        val docRef = companyDoc(companyId).collection("operational_costs").document(docId)
        val totalCost = fuelCost + tollCost + loadingUnloadingCost + serviceCost +
            tripAllowance + driverSalary + driverCashAdvance + additionalCost
        val netIncome = revenue - totalCost
        val approvalStatus = if (actor.role == "SPV") "APPROVED" else "PENDING"
        val approvedBy = if (actor.role == "SPV") actor.userId else ""

        return try {
            docRef.set(
                mapOf(
                    "companyId" to companyId,
                    "shipmentId" to shipment.id,
                    "doNumber" to shipment.doNumber,
                    "fuelCost" to fuelCost,
                    "tollCost" to tollCost,
                    "loadingUnloadingCost" to loadingUnloadingCost,
                    "serviceCost" to serviceCost,
                    "tripAllowance" to tripAllowance,
                    "driverSalary" to driverSalary,
                    "driverCashAdvance" to driverCashAdvance,
                    "additionalCost" to additionalCost,
                    "totalCost" to totalCost,
                    "revenue" to revenue,
                    "grossIncome" to revenue,
                    "netIncome" to netIncome,
                    "approvalStatus" to approvalStatus,
                    "approvedByUid" to approvedBy,
                    "notes" to notes.trim(),
                    "createdByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(
                actor,
                companyId,
                "CREATE_COST",
                "OPERATIONAL_COSTS",
                docId,
                "Biaya ${shipment.doNumber}: Total Rp ${totalCost.toLong()} | Net Rp ${netIncome.toLong()}"
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun approveOperationalCost(
        actor: CompanyUser,
        cost: OperationalCost,
        approved: Boolean
    ): Result<Unit> {
        require(cost.id.isNotBlank())
        val docRef = companyDoc(cost.companyId).collection("operational_costs").document(cost.id)
        val status = if (approved) "APPROVED" else "REJECTED"
        return try {
            docRef.update(
                mapOf(
                    "approvalStatus" to status,
                    "approvedByUid" to actor.userId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, cost.companyId, "APPROVE_COST", "OPERATIONAL_COSTS", cost.id, "Persetujuan biaya ${cost.doNumber}: $status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- INVOICES & PAYMENTS ---
    suspend fun createInvoiceFromShipment(
        actor: CompanyUser,
        companyId: String,
        invoiceNumber: String,
        shipment: Shipment,
        billableTonnage: Double,
        dueDate: String,
        notes: String
    ): Result<String> {
        val docId = newId("inv")
        val docRef = companyDoc(companyId).collection("invoices").document(docId)
        val totalAmount = billableTonnage * shipment.pricePerTon
        return try {
            docRef.set(
                mapOf(
                    "companyId" to companyId,
                    "invoiceNumber" to invoiceNumber.trim().uppercase(Locale.ROOT),
                    "shipmentId" to shipment.id,
                    "doNumber" to shipment.doNumber,
                    "customerId" to shipment.customerId,
                    "customerName" to shipment.customerName,
                    "billableTonnage" to billableTonnage,
                    "pricePerTon" to shipment.pricePerTon,
                    "totalAmount" to totalAmount,
                    "paidAmount" to 0.0,
                    "paymentStatus" to "UNPAID",
                    "dueDate" to dueDate.trim(),
                    "lastPaymentDate" to "",
                    "paymentMethod" to "",
                    "notes" to notes.trim(),
                    "createdByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, companyId, "CREATE_INVOICE", "INVOICES", docId, "Tagihan ${invoiceNumber.uppercase(Locale.ROOT)} dibuat")
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordInvoicePayment(
        actor: CompanyUser,
        invoice: Invoice,
        paymentAddAmount: Double,
        paymentDate: String,
        paymentMethod: String
    ): Result<Unit> {
        require(invoice.id.isNotBlank())
        val docRef = companyDoc(invoice.companyId).collection("invoices").document(invoice.id)
        val newPaid = (invoice.paidAmount + paymentAddAmount).coerceAtMost(invoice.totalAmount)
        val newStatus = when {
            newPaid >= invoice.totalAmount -> "PAID"
            newPaid > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }
        return try {
            docRef.update(
                mapOf(
                    "paidAmount" to newPaid,
                    "paymentStatus" to newStatus,
                    "lastPaymentDate" to paymentDate.trim(),
                    "paymentMethod" to paymentMethod.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, invoice.companyId, "RECORD_PAYMENT", "PAYMENTS", invoice.id, "Pembayaran ${invoice.invoiceNumber}: $newStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- MAINTENANCE ---
    suspend fun saveMaintenanceRecord(
        actor: CompanyUser,
        companyId: String,
        vehicle: Vehicle,
        serviceType: String,
        serviceDate: String,
        cost: Double,
        workshopName: String,
        nextServiceDate: String,
        notes: String
    ): Result<String> {
        val docId = newId("mnt")
        val docRef = companyDoc(companyId).collection("maintenance").document(docId)
        return try {
            docRef.set(
                mapOf(
                    "companyId" to companyId,
                    "vehicleId" to vehicle.id,
                    "vehiclePlate" to vehicle.plateNumber,
                    "serviceType" to serviceType.trim(),
                    "serviceDate" to serviceDate.trim(),
                    "cost" to cost,
                    "workshopName" to workshopName.trim(),
                    "notes" to notes.trim(),
                    "nextServiceDate" to nextServiceDate.trim(),
                    "createdByUid" to actor.userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLog(actor, companyId, "CREATE_MAINTENANCE", "MAINTENANCE", docId, "Servis ${vehicle.plateNumber}: $serviceType")
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun recordLog(
        actor: CompanyUser,
        companyId: String,
        action: String,
        module: String,
        targetId: String,
        details: String
    ) {
        val logId = newId("log")
        val docRef = companyDoc(companyId).collection("activity_logs").document(logId)
        runCatching {
            docRef.set(
                mapOf(
                    "companyId" to companyId,
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

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"
}
