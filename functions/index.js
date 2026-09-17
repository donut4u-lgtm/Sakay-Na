const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();

const auth = getAuth();
const db = getFirestore();

function normalizePhone(phone) {
  if (!phone) return "";

  return String(phone)
    .trim()
    .replace(/[^\d+]/g, "");
}

function firebaseIdentifier(phone) {
  return normalizePhone(phone) + "@sakyna.app";
}

exports.createFirstAdmin = onCall(async (request) => {

  const phone = normalizePhone(request.data?.phone);
  const password = String(request.data?.password || "");

  if (!phone) {
    throw new HttpsError(
      "invalid-argument",
      "Phone number is required."
    );
  }

  if (password.length < 6) {
    throw new HttpsError(
      "invalid-argument",
      "Password must be at least 6 characters."
    );
  }

  const existingAdmins = await db
    .collection("users")
    .where("role", "==", "ADMIN")
    .limit(1)
    .get();

  if (!existingAdmins.empty) {
    throw new HttpsError(
      "already-exists",
      "An Admin account already exists."
    );
  }

  const email = firebaseIdentifier(phone);

  let userRecord;

  try {
    userRecord = await auth.createUser({
      email: email,
      password: password
    });
  } catch (error) {

    if (error.code === "auth/email-already-exists") {
      throw new HttpsError(
        "already-exists",
        "This phone number already has an account."
      );
    }

    throw new HttpsError(
      "internal",
      "Unable to create the Admin account."
    );
  }

  await auth.setCustomUserClaims(userRecord.uid, {
    admin: true
  });

  await db
    .collection("users")
    .doc(userRecord.uid)
    .set({
      phone: phone,
      role: "ADMIN",
      admin: true,
      approved: true,
      approvalStatus: "APPROVED",
      online: false,
      createdAt: FieldValue.serverTimestamp()
    });

  return {
    success: true,
    message: "First Admin account created."
  };
});
