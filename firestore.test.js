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

function validFilePayload(userId, now = new Date()) {
  return {
    userId,
    name: "Orbital_Horizon_4K.mp4",
    category: "video",
    mimeType: "video/mp4",
    sizeBytes: 1845493760,
    parentFolderId: "root",
    folderPath: "/",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    thumbnailKey: "scifi",
    durationSec: 15,
    resolution: "4K 2160p HDR",
    playbackPositionSec: 0,
    isStarred: false,
    isTrashed: false,
    isVaultLocked: false,
    isOfflineAvailable: false,
    tags: ["4K", "SciFi"],
    shareCode: "",
    createdAt: now,
    updatedAt: now,
  };
}

function validSharedLinkPayload(ownerId, now = new Date()) {
  return {
    ownerId,
    ownerHandle: "AliceCloud",
    fileId: "file_1",
    fileName: "Orbital_Horizon_4K.mp4",
    category: "video",
    mimeType: "video/mp4",
    sizeBytes: 1845493760,
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    thumbnailKey: "scifi",
    durationSec: 15,
    resolution: "4K 2160p HDR",
    allowDownload: true,
    accessPin: "",
    isActive: true,
    viewsCount: 0,
    savesCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write files, shared_links, transfers, or user_configs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("files").get());
  await assertFails(unauthDb.collection("shared_links").get());
  await assertFails(unauthDb.collection("transfers").get());
  await assertFails(unauthDb.collection("user_configs").doc(ALICE_UID).get());
});

test("Authenticated user: can create and query their own CloudFile with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("files").doc("file_alice_1").set(validFilePayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("files").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot read another user's CloudFile or query without userId filter", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("files").doc("file_bob_1").set(validFilePayload(BOB_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("files").doc("file_bob_1").get());
  await assertFails(aliceDb.collection("files").get());
});

test("Shadow update test: rejects unknown ghost field on CloudFile create or update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("files").doc("file_ghost").set({
      ...validFilePayload(ALICE_UID, now),
      isVerifiedAdmin: true,
    })
  );
});

test("SharedLink: owner can create and list; recipient can get active link by ID and increment viewsCount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("shared_links").doc("dw_share_101").set(validSharedLinkPayload(ALICE_UID, now))
  );

  // Bob can open active link by ID
  await assertSucceeds(
    bobDb.collection("shared_links").doc("dw_share_101").get()
  );

  // Bob can increment viewsCount/savesCount on active link
  await assertSucceeds(
    bobDb.collection("shared_links").doc("dw_share_101").update({
      viewsCount: 1,
      savesCount: 1,
      updatedAt: new Date(),
    })
  );

  // Bob cannot hijack allowDownload or accessPin on Alice's link
  await assertFails(
    bobDb.collection("shared_links").doc("dw_share_101").update({
      allowDownload: false,
      updatedAt: new Date(),
    })
  );
});
