const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const SUPER_ADMIN_UID = "super_admin_1";
const SPV_A_UID = "spv_company_a";
const STAFF_A_UID = "staff_company_a";
const SPV_B_UID = "spv_company_b";
const COMPANY_A_ID = "comp_maju_bersama";
const COMPANY_B_ID = "comp_sawit_makmur";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedTenants() {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("super_admins").doc(SUPER_ADMIN_UID).set({ active: true });
    await db.collection("companies").doc(COMPANY_A_ID).set({
      name: "PT Maju Bersama",
      code: "MBJ",
      address: "Dumai, Riau",
      phone: "0811000111",
      email: "ops@majubersama.co.id",
      spreadsheetId: "SHEET_MBJ_001",
      appsScriptUrl: "",
      connectionStatus: "CONNECTED",
      subscriptionPlan: "ENTERPRISE",
      isActive: true,
      shrinkageTolerancePct: 0.5,
      defaultRatePerTon: 450000,
      ownerUid: SPV_A_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("companies").doc(COMPANY_B_ID).set({
      name: "PT Sawit Makmur",
      code: "SMK",
      address: "Pontianak",
      phone: "0811000222",
      email: "ops@sawitmakmur.co.id",
      spreadsheetId: "SHEET_SMK_002",
      appsScriptUrl: "",
      connectionStatus: "CONNECTED",
      subscriptionPlan: "ENTERPRISE",
      isActive: true,
      shrinkageTolerancePct: 0.3,
      defaultRatePerTon: 500000,
      ownerUid: SPV_B_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("company_users").doc(SPV_A_UID).set({
      userId: SPV_A_UID,
      companyId: COMPANY_A_ID,
      companyName: "PT Maju Bersama",
      name: "Budi SPV A",
      email: "budi@majubersama.co.id",
      role: "SPV",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("company_users").doc(STAFF_A_UID).set({
      userId: STAFF_A_UID,
      companyId: COMPANY_A_ID,
      companyName: "PT Maju Bersama",
      name: "Andi Staff A",
      email: "andi@majubersama.co.id",
      role: "KARYAWAN",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("company_users").doc(SPV_B_UID).set({
      userId: SPV_B_UID,
      companyId: COMPANY_B_ID,
      companyName: "PT Sawit Makmur",
      name: "Siti SPV B",
      email: "siti@sawitmakmur.co.id",
      role: "SPV",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
    await db
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .doc("ship_a1")
      .set({
        companyId: COMPANY_A_ID,
        doNumber: "DO-MBJ-001",
        suratJalanNumber: "SJ-MBJ-001",
        shipmentDate: "2026-11-15",
        customerName: "PT Refinery Dumai",
        loadingLocation: "PKS Pelalawan",
        unloadingLocation: "Tangki Dumai",
        productType: "Crude Palm Oil (CPO)",
        targetTonnage: 25.0,
        pricePerTon: 450000,
        status: "PENDING_APPROVAL",
        statusHistory: ["DRAFT", "PENDING_APPROVAL"],
        createdByUid: STAFF_A_UID,
        createdAt: now,
        updatedAt: now,
      });
  });
}

test("Unauthenticated user: denied all reads", async () => {
  await seedTenants();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("companies").get());
  await assertFails(
    unauthDb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .where("companyId", "==", COMPANY_A_ID)
      .get()
  );
});

test("Tenant isolation: Company B SPV cannot read or list Company A shipments", async () => {
  await seedTenants();
  const spvBDb = testEnv.authenticatedContext(SPV_B_UID).firestore();
  await assertFails(
    spvBDb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .doc("ship_a1")
      .get()
  );
  await assertFails(
    spvBDb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .where("companyId", "==", COMPANY_A_ID)
      .get()
  );
});

test("Super Admin isolation: Super Admin cannot mutate Company A operational shipments directly", async () => {
  await seedTenants();
  const adminDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: "ua.yosua97@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(adminDb.collection("companies").get());
  await assertFails(
    adminDb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .doc("ship_a1")
      .get()
  );
});

test("Email spoofing test: unverified Super Admin email is rejected", async () => {
  await seedTenants();
  const spoofDb = testEnv
    .authenticatedContext("attacker_uid", {
      email: "ua.yosua97@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofDb.collection("companies").get());
});

test("RBAC test: Karyawan cannot self-escalate role or approve shipment to READY_TO_LOAD", async () => {
  await seedTenants();
  const staffADb = testEnv.authenticatedContext(STAFF_A_UID).firestore();

  // Can list own company shipments with companyId filter
  await assertSucceeds(
    staffADb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .where("companyId", "==", COMPANY_A_ID)
      .get()
  );

  // Cannot approve shipment to READY_TO_LOAD
  await assertFails(
    staffADb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .doc("ship_a1")
      .update({
        status: "READY_TO_LOAD",
        updatedAt: new Date(),
      })
  );

  // Cannot escalate own role in company_users
  await assertFails(
    staffADb.collection("company_users").doc(STAFF_A_UID).update({
      role: "SPV",
      updatedAt: new Date(),
    })
  );
});

test("Shadow update & Resource poisoning test: ghost field and oversized ID rejected", async () => {
  await seedTenants();
  const spvADb = testEnv.authenticatedContext(SPV_A_UID).firestore();

  // Valid SPV approval succeeds
  await assertSucceeds(
    spvADb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .doc("ship_a1")
      .update({
        status: "READY_TO_LOAD",
        approvedByUid: SPV_A_UID,
        updatedAt: new Date(),
      })
  );

  // Shadow update with ghost field fails
  await assertFails(
    spvADb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("shipments")
      .doc("ship_a1")
      .update({
        notes: "Updated",
        ghostAdminOverride: true,
        updatedAt: new Date(),
      })
  );

  // Oversized / invalid ID fails
  const hugeId = "a".repeat(200);
  await assertFails(
    spvADb
      .collection("companies")
      .doc(COMPANY_A_ID)
      .collection("customers")
      .doc(hugeId)
      .set({
        companyId: COMPANY_A_ID,
        name: "Test Mill",
        code: "TM",
        isActive: true,
        createdByUid: SPV_A_UID,
        createdAt: new Date(),
      })
  );
});
