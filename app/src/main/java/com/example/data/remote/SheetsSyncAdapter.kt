package com.example.data.remote

import com.example.data.model.Company
import com.example.data.model.Customer
import com.example.data.model.Driver
import com.example.data.model.Invoice
import com.example.data.model.MaintenanceRecord
import com.example.data.model.OperationalCost
import com.example.data.model.Shipment
import com.example.data.model.Vehicle
import com.example.data.model.WeighingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SheetsSyncResult(
    val success: Boolean,
    val message: String,
    val syncedSheetsCount: Int = 0
)

class SheetsSyncAdapter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun syncCompanySnapshot(
        company: Company,
        customers: List<Customer>,
        vehicles: List<Vehicle>,
        drivers: List<Driver>,
        shipments: List<Shipment>,
        weighings: List<WeighingRecord>,
        costs: List<OperationalCost>,
        invoices: List<Invoice>,
        maintenance: List<MaintenanceRecord>
    ): SheetsSyncResult = withContext(Dispatchers.IO) {
        if (company.spreadsheetId.isBlank()) {
            return@withContext SheetsSyncResult(
                success = false,
                message = "Spreadsheet ID belum diisi pada konfigurasi perusahaan."
            )
        }
        if (company.appsScriptUrl.isBlank()) {
            return@withContext SheetsSyncResult(
                success = false,
                message = "URL Web App Google Apps Script perusahaan belum dikonfigurasi."
            )
        }

        val payload = JSONObject().apply {
            put("action", "SYNC_COMPANY_DATA")
            put("companyId", company.id)
            put("companyCode", company.code)
            put("spreadsheetId", company.spreadsheetId)
            put("sheets", JSONObject().apply {
                put("COMPANY_SETTINGS", JSONArray().apply {
                    put(JSONObject().apply {
                        put("companyId", company.id)
                        put("name", company.name)
                        put("code", company.code)
                        put("shrinkageTolerancePct", company.shrinkageTolerancePct)
                        put("defaultRatePerTon", company.defaultRatePerTon)
                        put("subscriptionPlan", company.subscriptionPlan)
                        put("isActive", company.isActive)
                    })
                })
                put("CUSTOMERS", JSONArray().apply {
                    customers.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("code", c.code)
                            put("name", c.name)
                            put("picName", c.picName)
                            put("phone", c.phone)
                            put("loadingLocation", c.loadingLocation)
                            put("unloadingLocation", c.unloadingLocation)
                            put("defaultPricePerTon", c.defaultPricePerTon)
                        })
                    }
                })
                put("VEHICLES", JSONArray().apply {
                    vehicles.forEach { v ->
                        put(JSONObject().apply {
                            put("id", v.id)
                            put("plateNumber", v.plateNumber)
                            put("vehicleType", v.vehicleType)
                            put("capacityTons", v.capacityTons)
                            put("ownerName", v.ownerName)
                            put("status", v.status)
                            put("stnkExpiry", v.stnkExpiry)
                            put("kirExpiry", v.kirExpiry)
                            put("isRoadworthy", v.isRoadworthy)
                        })
                    }
                })
                put("DRIVERS", JSONArray().apply {
                    drivers.forEach { d ->
                        put(JSONObject().apply {
                            put("id", d.id)
                            put("fullName", d.fullName)
                            put("nik", d.nik)
                            put("phone", d.phone)
                            put("simNumber", d.simNumber)
                            put("simExpiry", d.simExpiry)
                            put("assignedPlate", d.assignedPlate)
                            put("status", d.status)
                            put("totalTrips", d.totalTrips)
                        })
                    }
                })
                put("SHIPMENTS", JSONArray().apply {
                    shipments.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("doNumber", s.doNumber)
                            put("suratJalanNumber", s.suratJalanNumber)
                            put("shipmentDate", s.shipmentDate)
                            put("customerName", s.customerName)
                            put("loadingLocation", s.loadingLocation)
                            put("unloadingLocation", s.unloadingLocation)
                            put("productType", s.productType)
                            put("targetTonnage", s.targetTonnage)
                            put("pricePerTon", s.pricePerTon)
                            put("vehiclePlate", s.vehiclePlate)
                            put("driverName", s.driverName)
                            put("status", s.status)
                        })
                    }
                })
                put("WEIGHING_DATA", JSONArray().apply {
                    weighings.forEach { w ->
                        put(JSONObject().apply {
                            put("id", w.id)
                            put("doNumber", w.doNumber)
                            put("loadTicketNumber", w.loadTicketNumber)
                            put("unloadTicketNumber", w.unloadTicketNumber)
                            put("loadTonnage", w.loadTonnage)
                            put("unloadTonnage", w.unloadTonnage)
                            put("differenceTonnage", w.differenceTonnage)
                            put("toleranceTons", w.toleranceTons)
                            put("billableTonnage", w.billableTonnage)
                            put("totalDeliveryValue", w.totalDeliveryValue)
                        })
                    }
                })
                put("OPERATIONAL_COSTS", JSONArray().apply {
                    costs.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("doNumber", c.doNumber)
                            put("fuelCost", c.fuelCost)
                            put("tollCost", c.tollCost)
                            put("loadingUnloadingCost", c.loadingUnloadingCost)
                            put("serviceCost", c.serviceCost)
                            put("tripAllowance", c.tripAllowance)
                            put("driverSalary", c.driverSalary)
                            put("driverCashAdvance", c.driverCashAdvance)
                            put("additionalCost", c.additionalCost)
                            put("totalCost", c.totalCost)
                            put("revenue", c.revenue)
                            put("netIncome", c.netIncome)
                            put("approvalStatus", c.approvalStatus)
                        })
                    }
                })
                put("INVOICES", JSONArray().apply {
                    invoices.forEach { inv ->
                        put(JSONObject().apply {
                            put("id", inv.id)
                            put("invoiceNumber", inv.invoiceNumber)
                            put("doNumber", inv.doNumber)
                            put("customerName", inv.customerName)
                            put("billableTonnage", inv.billableTonnage)
                            put("totalAmount", inv.totalAmount)
                            put("paidAmount", inv.paidAmount)
                            put("paymentStatus", inv.paymentStatus)
                            put("dueDate", inv.dueDate)
                        })
                    }
                })
                put("MAINTENANCE", JSONArray().apply {
                    maintenance.forEach { m ->
                        put(JSONObject().apply {
                            put("id", m.id)
                            put("vehiclePlate", m.vehiclePlate)
                            put("serviceType", m.serviceType)
                            put("serviceDate", m.serviceDate)
                            put("cost", m.cost)
                            put("workshopName", m.workshopName)
                        })
                    }
                })
            })
        }

        try {
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(company.appsScriptUrl.trim())
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SheetsSyncResult(
                        success = true,
                        message = "Sinkronisasi ke Google Sheets (${company.spreadsheetId}) berhasil.",
                        syncedSheetsCount = 9
                    )
                } else {
                    SheetsSyncResult(
                        success = false,
                        message = "Gagal sinkronisasi (${response.code}): ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            SheetsSyncResult(
                success = false,
                message = "Gagal terhubung ke Google Apps Script: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    companion object {
        const val APPS_SCRIPT_TEMPLATE = """
function doPost(e) {
  var lock = LockService.getScriptLock();
  lock.waitLock(15000);
  try {
    var payload = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.openById(payload.spreadsheetId);
    var requiredSheets = [
      'USERS','COMPANY_SETTINGS','CUSTOMERS','DRIVERS','VEHICLES',
      'SHIPMENTS','DELIVERY_ORDERS','WEIGHING_DATA','OPERATIONAL_COSTS',
      'INVOICES','PAYMENTS','MAINTENANCE','ACTIVITY_LOG','SYSTEM_SETTINGS'
    ];
    requiredSheets.forEach(function(name) {
      if (!ss.getSheetByName(name)) ss.insertSheet(name);
    });
    var sheets = payload.sheets || {};
    Object.keys(sheets).forEach(function(sheetName) {
      var rows = sheets[sheetName];
      if (!rows || !rows.length) return;
      var sheet = ss.getSheetByName(sheetName);
      var headers = Object.keys(rows[0]);
      var values = [headers];
      rows.forEach(function(row) {
        values.push(headers.map(function(h) { return row[h] !== undefined ? row[h] : ''; }));
      });
      sheet.clearContents();
      sheet.getRange(1, 1, values.length, headers.length).setValues(values);
    });
    return ContentService.createTextOutput(JSON.stringify({ ok: true }))
      .setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}
"""
    }
}
