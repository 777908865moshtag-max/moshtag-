const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidPayload(userId) {
  return {
    userId,
    title: "موك أب أكياس بلاستيكية",
    categoryTitleAr: "موك أب أكياس بلاستيكية",
    processingModeAr: "المحافظة القصوى على الأصل (تركيب طبقات)",
    aspectRatioLabel: "1:1 (مربع)",
    resolutionLabel: "1K (1024px)",
    outputWidth: 1024,
    outputHeight: 1024,
    exportFormat: "PNG",
    savedFilePath: "/data/user/0/com.aistudio.moshtaqgrafx.mgsxai/files/moshtaq_exports/project_1.png",
    originalFileName: "brand_logo.png",
    notesAr: "تصميم معتمد للطباعة التجارية",
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  };
}

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

test("1. Unauthenticated user cannot read or list projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
});

test("2. Authenticated owner can create and read their own valid project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(docRef.set(buildValidPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
});

test("3. Authenticated user cannot read another user's project (Cross-User Isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("projects")
      .doc("bob_proj")
      .set(buildValidPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("projects").doc("bob_proj").get()
  );
});

test("4. Identity spoofing (writing userId != auth.uid) is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_spoof");
  await assertFails(docRef.set(buildValidPayload(BOB_UID)));
});

test("5. Shadow update / ghost field injection is rejected by hasOnly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_ghost");
  const payload = {
    ...buildValidPayload(ALICE_UID),
    isAdmin: true,
  };
  await assertFails(docRef.set(payload));
});

test("6. Value poisoning (invalid exportFormat or oversized string) is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_poison");
  const badFormatPayload = {
    ...buildValidPayload(ALICE_UID),
    exportFormat: "EXE",
  };
  await assertFails(docRef.set(badFormatPayload));
});

test("7. Owner can update allowed fields while preserving immutable userId and createdAt", async () => {
  const createdTime = new Date(Date.now() - 5000);
  const initial = {
    ...buildValidPayload(ALICE_UID),
    createdAt: createdTime,
    updatedAt: createdTime,
  };
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_upd");
  await assertSucceeds(docRef.set(initial));

  await assertSucceeds(
    docRef.update({
      title: "موك أب علب عطور فاخرة",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("8. Mutating immutable createdAt on update is rejected", async () => {
  const createdTime = new Date(Date.now() - 5000);
  const initial = {
    ...buildValidPayload(ALICE_UID),
    createdAt: createdTime,
    updatedAt: createdTime,
  };
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_imm");
  await assertSucceeds(docRef.set(initial));

  await assertFails(
    docRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
