package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ShipmentStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TenantOperationsRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun companySpv_canRegisterWorkspaceCreateShipmentAndObserveRealtime() = runBlocking {
        val unique = UUID.randomUUID().toString().take(8)
        val spvEmail = "spv_a_$unique@cpo-test.com"
        signInTestUser(spvEmail)
        val firebaseUser = checkNotNull(auth.currentUser)

        val authRepo = AuthSessionRepository(firestore)
        val opsRepo = TenantOperationsRepository(firestore)

        val regResult = withTimeout(TIMEOUT_MS) {
            authRepo.registerNewCompanyWorkspace(
                firebaseUser = firebaseUser,
                companyName = "PT Maju Bersama $unique",
                companyCode = "MBJ",
                phone = "08123456789",
                address = "Dumai, Riau",
                spreadsheetId = "SHEET_$unique",
                appsScriptUrl = ""
            )
        }
        assertTrue(regResult.isSuccess)
        val spvProfile = regResult.getOrThrow()
        assertEquals("SPV", spvProfile.role)

        val shipmentId = withTimeout(TIMEOUT_MS) {
            opsRepo.createShipment(
                actor = spvProfile,
                companyId = spvProfile.companyId,
                doNumber = "DO-$unique",
                suratJalanNumber = "SJ-$unique",
                shipmentDate = "2026-11-16",
                customerId = "",
                customerName = "PT Refinery Dumai",
                loadingLocation = "PKS Pelalawan",
                unloadingLocation = "Tangki Dumai",
                senderCompany = spvProfile.companyName,
                receiverCompany = "PT Refinery Dumai",
                productType = "Crude Palm Oil (CPO)",
                targetTonnage = 25.0,
                pricePerTon = 450000.0,
                vehicleId = "",
                vehiclePlate = "BM 9012 AA",
                driverId = "",
                driverName = "Budi Santoso",
                etaText = "16 Nov 17:00",
                notes = "Test shipment",
                submitForApproval = true
            ).getOrThrow()
        }

        val shipments = withTimeout(FLOW_TIMEOUT_MS) {
            opsRepo.observeShipments(spvProfile.companyId).first { list ->
                list.any { it.id == shipmentId }
            }
        }
        val created = shipments.first { it.id == shipmentId }
        assertEquals("DO-${unique.uppercase()}", created.doNumber)
        assertNotNull(created.createdAt)
    }

    @Test
    fun crossCompanyAccess_isRejectedWithPermissionDenied() = runBlocking {
        val unique = UUID.randomUUID().toString().take(8)
        val authRepo = AuthSessionRepository(firestore)
        val opsRepo = TenantOperationsRepository(firestore)

        // Company A SPV creates Company A
        signInTestUser("spv_iso_a_$unique@cpo-test.com")
        val spvA = withTimeout(TIMEOUT_MS) {
            authRepo.registerNewCompanyWorkspace(
                firebaseUser = checkNotNull(auth.currentUser),
                companyName = "PT Tenant A $unique",
                companyCode = "TNA",
                phone = "081111",
                address = "Riau",
                spreadsheetId = "",
                appsScriptUrl = ""
            ).getOrThrow()
        }

        // Company B SPV creates Company B
        signInTestUser("spv_iso_b_$unique@cpo-test.com")
        withTimeout(TIMEOUT_MS) {
            authRepo.registerNewCompanyWorkspace(
                firebaseUser = checkNotNull(auth.currentUser),
                companyName = "PT Tenant B $unique",
                companyCode = "TNB",
                phone = "082222",
                address = "Jambi",
                spreadsheetId = "",
                appsScriptUrl = ""
            ).getOrThrow()
        }

        // Company B SPV tries to read Company A shipments -> must fail PERMISSION_DENIED
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                opsRepo.observeShipments(spvA.companyId).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun unauthenticatedUser_isRejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val opsRepo = TenantOperationsRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                opsRepo.observeShipments("comp_nonexistent").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun spvApproval_andWeighingCalculation_workCorrectly() = runBlocking {
        val unique = UUID.randomUUID().toString().take(8)
        val authRepo = AuthSessionRepository(firestore)
        val opsRepo = TenantOperationsRepository(firestore)

        signInTestUser("spv_wgh_$unique@cpo-test.com")
        val spv = withTimeout(TIMEOUT_MS) {
            authRepo.registerNewCompanyWorkspace(
                firebaseUser = checkNotNull(auth.currentUser),
                companyName = "PT Weigh Test $unique",
                companyCode = "WGH",
                phone = "083333",
                address = "Medan",
                spreadsheetId = "",
                appsScriptUrl = ""
            ).getOrThrow()
        }

        val shipId = withTimeout(TIMEOUT_MS) {
            opsRepo.createShipment(
                actor = spv,
                companyId = spv.companyId,
                doNumber = "DO-W-$unique",
                suratJalanNumber = "SJ-W-$unique",
                shipmentDate = "2026-11-16",
                customerId = "",
                customerName = "PT Wilmar",
                loadingLocation = "PKS Kisaran",
                unloadingLocation = "Kuala Tanjung",
                senderCompany = spv.companyName,
                receiverCompany = "PT Wilmar",
                productType = "CPO",
                targetTonnage = 25.0,
                pricePerTon = 400000.0,
                vehicleId = "",
                vehiclePlate = "BK 8899 XY",
                driverId = "",
                driverName = "Rahmat",
                etaText = "16 Nov",
                notes = "",
                submitForApproval = true
            ).getOrThrow()
        }

        val createdShipment = withTimeout(FLOW_TIMEOUT_MS) {
            opsRepo.observeShipments(spv.companyId).first { list -> list.any { it.id == shipId } }
        }.first { it.id == shipId }

        val approveResult = withTimeout(TIMEOUT_MS) {
            opsRepo.updateShipmentStatus(spv, createdShipment, ShipmentStatus.READY_TO_LOAD, "Disetujui SPV")
        }
        assertTrue(approveResult.isSuccess)

        val weighingId = withTimeout(TIMEOUT_MS) {
            opsRepo.saveWeighingRecord(
                actor = spv,
                companyId = spv.companyId,
                shipment = createdShipment,
                loadTicketNumber = "TKT-M-$unique",
                unloadTicketNumber = "TKT-B-$unique",
                loadTonnage = 25.0,
                unloadTonnage = 24.75,
                tolerancePct = 0.5
            ).getOrThrow()
        }

        val weighings = withTimeout(FLOW_TIMEOUT_MS) {
            opsRepo.observeWeighings(spv.companyId).first { list -> list.any { it.id == weighingId } }
        }
        val rec = weighings.first { it.id == weighingId }
        // 25.0 * 0.5% = 0.125 tolerance; diff = 0.25 -> excess = 0.125 -> billable = 24.875
        assertTrue(rec.isOverShrinkage)
        assertEquals(24.875, rec.billableTonnage, 0.001)
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
