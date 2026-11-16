package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String) {
    SUPER_ADMIN("Super Admin"),
    SPV("Supervisor (SPV)"),
    KARYAWAN("Karyawan")
}

enum class ShipmentStatus(val code: String, val label: String, val stepIndex: Int) {
    DRAFT("DRAFT", "Draft", 0),
    PENDING_APPROVAL("PENDING_APPROVAL", "Menunggu Persetujuan", 1),
    READY_TO_LOAD("READY_TO_LOAD", "Siap Dimuat", 2),
    LOADING("LOADING", "Dalam Proses Muat", 3),
    IN_TRANSIT("IN_TRANSIT", "Dalam Perjalanan", 4),
    ARRIVED("ARRIVED", "Tiba di Lokasi Bongkar", 5),
    UNLOADING("UNLOADING", "Proses Bongkar", 6),
    COMPLETED("COMPLETED", "Selesai", 7),
    ISSUE("ISSUE", "Bermasalah", 8),
    CANCELLED("CANCELLED", "Dibatalkan", 9);

    companion object {
        fun fromCode(code: String): ShipmentStatus =
            entries.firstOrNull { it.code == code } ?: DRAFT
    }
}

enum class VehicleStatus(val code: String, val label: String) {
    AVAILABLE("AVAILABLE", "Tersedia"),
    IN_USE("IN_USE", "Digunakan"),
    MAINTENANCE("MAINTENANCE", "Dalam Perawatan"),
    INACTIVE("INACTIVE", "Tidak Aktif");

    companion object {
        fun fromCode(code: String): VehicleStatus =
            entries.firstOrNull { it.code == code } ?: AVAILABLE
    }
}

enum class DriverStatus(val code: String, val label: String) {
    ACTIVE("ACTIVE", "Aktif"),
    ON_TRIP("ON_TRIP", "Bertugas"),
    INACTIVE("INACTIVE", "Tidak Aktif");

    companion object {
        fun fromCode(code: String): DriverStatus =
            entries.firstOrNull { it.code == code } ?: ACTIVE
    }
}

@IgnoreExtraProperties
data class Company(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val spreadsheetId: String = "",
    val appsScriptUrl: String = "",
    val connectionStatus: String = "PENDING",
    val subscriptionPlan: String = "ENTERPRISE",
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val shrinkageTolerancePct: Double = 0.5,
    val defaultRatePerTon: Double = 450000.0,
    val ownerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CompanyUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "KARYAWAN",
    val status: String = "ACTIVE",
    val inviteId: String = "",
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val roleEnum: UserRole
        get() = when (role) {
            "SUPER_ADMIN" -> UserRole.SUPER_ADMIN
            "SPV" -> UserRole.SPV
            else -> UserRole.KARYAWAN
        }
}

@IgnoreExtraProperties
data class CompanyInvite(
    @DocumentId val id: String = "",
    val email: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val role: String = "KARYAWAN",
    val status: String = "PENDING",
    val invitedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val code: String = "",
    val picName: String = "",
    val phone: String = "",
    val address: String = "",
    val loadingLocation: String = "",
    val unloadingLocation: String = "",
    val defaultPricePerTon: Double = 450000.0,
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Vehicle(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val plateNumber: String = "",
    val vehicleType: String = "Tangki CPO 25T",
    val capacityTons: Double = 25.0,
    val ownerName: String = "",
    val status: String = "AVAILABLE",
    val stnkExpiry: String = "",
    val kirExpiry: String = "",
    val lastServiceDate: String = "",
    @get:PropertyName("isRoadworthy") val isRoadworthy: Boolean = true,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Driver(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val fullName: String = "",
    val nik: String = "",
    val phone: String = "",
    val address: String = "",
    val simNumber: String = "",
    val simExpiry: String = "",
    val assignedVehicleId: String = "",
    val assignedPlate: String = "",
    val status: String = "ACTIVE",
    val totalTrips: Long = 0L,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Shipment(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val doNumber: String = "",
    val suratJalanNumber: String = "",
    val shipmentDate: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val loadingLocation: String = "",
    val unloadingLocation: String = "",
    val senderCompany: String = "",
    val receiverCompany: String = "",
    val productType: String = "Crude Palm Oil (CPO)",
    val targetTonnage: Double = 25.0,
    val pricePerTon: Double = 450000.0,
    val totalContractValue: Double = 0.0,
    val vehicleId: String = "",
    val vehiclePlate: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val status: String = "DRAFT",
    val etaText: String = "",
    val notes: String = "",
    val statusHistory: List<String> = emptyList(),
    val approvedByUid: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WeighingRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val shipmentId: String = "",
    val doNumber: String = "",
    val loadTicketNumber: String = "",
    val unloadTicketNumber: String = "",
    val loadTonnage: Double = 0.0,
    val unloadTonnage: Double = 0.0,
    val differenceTonnage: Double = 0.0,
    val tolerancePct: Double = 0.5,
    val toleranceTons: Double = 0.0,
    val billableTonnage: Double = 0.0,
    val pricePerTon: Double = 0.0,
    val totalDeliveryValue: Double = 0.0,
    @get:PropertyName("isOverShrinkage") val isOverShrinkage: Boolean = false,
    val recordedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OperationalCost(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val shipmentId: String = "",
    val doNumber: String = "",
    val fuelCost: Double = 0.0,
    val tollCost: Double = 0.0,
    val loadingUnloadingCost: Double = 0.0,
    val serviceCost: Double = 0.0,
    val tripAllowance: Double = 0.0,
    val driverSalary: Double = 0.0,
    val driverCashAdvance: Double = 0.0,
    val additionalCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val revenue: Double = 0.0,
    val grossIncome: Double = 0.0,
    val netIncome: Double = 0.0,
    val approvalStatus: String = "PENDING",
    val approvedByUid: String = "",
    val notes: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val invoiceNumber: String = "",
    val shipmentId: String = "",
    val doNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val billableTonnage: Double = 0.0,
    val pricePerTon: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "UNPAID",
    val dueDate: String = "",
    val lastPaymentDate: String = "",
    val paymentMethod: String = "",
    val notes: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MaintenanceRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val vehicleId: String = "",
    val vehiclePlate: String = "",
    val serviceType: String = "",
    val serviceDate: String = "",
    val cost: Double = 0.0,
    val workshopName: String = "",
    val notes: String = "",
    val nextServiceDate: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val action: String = "",
    val module: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
