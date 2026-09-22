const crypto = require("crypto");

const { initializeApp } = require("firebase-admin/app");
const {
  getFirestore,
  FieldValue
} = require("firebase-admin/firestore");

const {
  onCall,
  HttpsError
} = require("firebase-functions/v2/https");

const {
  setGlobalOptions
} = require("firebase-functions/v2");

const {
  logger
} = require("firebase-functions");

initializeApp();

const db = getFirestore();

setGlobalOptions({
  region: "asia-southeast1",
  maxInstances: 5
});

/*
 * Sakay Na server-side security
 *
 * This first version provides:
 *
 * 1. Registration-attempt rate limiting
 * 2. Ride-request rate limiting
 * 3. Cancellation rate limiting
 * 4. Server-side security records
 *
 * App Check enforcement will be enabled after
 * the Android App Check configuration is completed.
 */


/* ---------------------------------------------------------
 * HELPERS
 * --------------------------------------------------------- */

function requireAuthenticated(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError(
      "unauthenticated",
      "You must be logged in."
    );
  }

  return request.auth.uid;
}


function normalizePhone(phone) {
  if (typeof phone !== "string") {
    return "";
  }

  return phone
    .replace(/[^\d+]/g, "")
    .trim();
}


function hashValue(value) {
  return crypto
    .createHash("sha256")
    .update(value)
    .digest("hex");
}


function getWindowStart(windowMs) {
  const now = Date.now();

  return Math.floor(now / windowMs) * windowMs;
}


/* ---------------------------------------------------------
 * REGISTRATION SECURITY
 *
 * Called BEFORE creating a new account.
 *
 * The client will later call this function from MainActivity.
 *
 * Limits:
 * - 3 attempts per phone in 24 hours
 * - 10 total attempts globally per 24 hours
 *
 * This does not create the Firebase Auth account.
 * It only decides whether registration may continue.
 * --------------------------------------------------------- */

exports.checkRegistrationAllowed = onCall(
  async (request) => {

    const phone = normalizePhone(
      request.data?.phone
    );

    if (!phone) {
      throw new HttpsError(
        "invalid-argument",
        "Phone number is required."
      );
    }

    /*
     * Do not store the raw phone number
     * in the rate-limit document.
     */
    const phoneHash = hashValue(phone);

    const dayWindow =
      getWindowStart(24 * 60 * 60 * 1000);

    const phoneKey =
      `registration_phone_${phoneHash}_${dayWindow}`;

    const globalKey =
      `registration_global_${dayWindow}`;

    const phoneRef =
      db.collection("securityRateLimits")
        .doc(phoneKey);

    const globalRef =
      db.collection("securityRateLimits")
        .doc(globalKey);

    let allowed = true;
    let reason = "";

    await db.runTransaction(async (transaction) => {

      const phoneSnapshot =
        await transaction.get(phoneRef);

      const globalSnapshot =
        await transaction.get(globalRef);

      const phoneCount =
        phoneSnapshot.exists
          ? Number(phoneSnapshot.data().count || 0)
          : 0;

      const globalCount =
        globalSnapshot.exists
          ? Number(globalSnapshot.data().count || 0)
          : 0;

      /*
       * Per-phone limit.
       */
      if (phoneCount >= 3) {
        allowed = false;
        reason =
          "Too many registration attempts for this phone number today.";
      }

      /*
       * Global safety limit.
       */
      if (globalCount >= 100) {
        allowed = false;
        reason =
          "Registration service is temporarily rate limited.";
      }

      /*
       * Always record the attempt.
       */
      transaction.set(
        phoneRef,
        {
          count: phoneCount + 1,
          windowStart: dayWindow,
          lastAttemptAt: FieldValue.serverTimestamp(),
          type: "REGISTRATION_PHONE"
        },
        {
          merge: true
        }
      );

      transaction.set(
        globalRef,
        {
          count: globalCount + 1,
          windowStart: dayWindow,
          lastAttemptAt: FieldValue.serverTimestamp(),
          type: "REGISTRATION_GLOBAL"
        },
        {
          merge: true
        }
      );
    });

    if (!allowed) {

      logger.warn(
        "Registration blocked",
        {
          phoneHash,
          reason
        }
      );

      return {
        allowed: false,
        reason
      };
    }

    return {
      allowed: true,
      reason: ""
    };
  }
);


/* ---------------------------------------------------------
 * RIDE REQUEST SECURITY
 *
 * Called by a passenger before creating a ride request.
 *
 * Limit:
 * - 10 ride-request attempts per passenger per hour
 * --------------------------------------------------------- */

exports.checkRideRequestAllowed = onCall(
  async (request) => {

    const uid =
      requireAuthenticated(request);

    const hourWindow =
      getWindowStart(60 * 60 * 1000);

    const key =
      `ride_request_${uid}_${hourWindow}`;

    const ref =
      db.collection("securityRateLimits")
        .doc(key);

    let allowed = true;

    await db.runTransaction(async (transaction) => {

      const snapshot =
        await transaction.get(ref);

      const count =
        snapshot.exists
          ? Number(snapshot.data().count || 0)
          : 0;

      if (count >= 10) {
        allowed = false;
      }

      transaction.set(
        ref,
        {
          uid,
          count: count + 1,
          windowStart: hourWindow,
          lastAttemptAt: FieldValue.serverTimestamp(),
          type: "RIDE_REQUEST"
        },
        {
          merge: true
        }
      );
    });

    if (!allowed) {

      logger.warn(
        "Ride request blocked",
        { uid }
      );

      return {
        allowed: false,
        reason:
          "Too many ride requests. Please try again later."
      };
    }

    return {
      allowed: true,
      reason: ""
    };
  }
);


/* ---------------------------------------------------------
 * CANCELLATION SECURITY
 *
 * Limit:
 * - 5 cancellations per passenger per hour
 * --------------------------------------------------------- */

exports.checkCancellationAllowed = onCall(
  async (request) => {

    const uid =
      requireAuthenticated(request);

    const hourWindow =
      getWindowStart(60 * 60 * 1000);

    const key =
      `cancellation_${uid}_${hourWindow}`;

    const ref =
      db.collection("securityRateLimits")
        .doc(key);

    let allowed = true;

    await db.runTransaction(async (transaction) => {

      const snapshot =
        await transaction.get(ref);

      const count =
        snapshot.exists
          ? Number(snapshot.data().count || 0)
          : 0;

      if (count >= 5) {
        allowed = false;
      }

      transaction.set(
        ref,
        {
          uid,
          count: count + 1,
          windowStart: hourWindow,
          lastAttemptAt: FieldValue.serverTimestamp(),
          type: "CANCELLATION"
        },
        {
          merge: true
        }
      );
    });

    if (!allowed) {

      logger.warn(
        "Cancellation blocked",
        { uid }
      );

      return {
        allowed: false,
        reason:
          "Too many cancellations. Please wait before cancelling another ride."
      };
    }

    return {
      allowed: true,
      reason: ""
    };
  }
);


/* ---------------------------------------------------------
 * SECURITY STATUS
 *
 * Admin can later use these records to identify
 * suspicious users.
 *
 * This function only allows a logged-in user to read
 * their own security status.
 * --------------------------------------------------------- */

exports.getMySecurityStatus = onCall(
  async (request) => {

    const uid =
      requireAuthenticated(request);

    const snapshot =
      await db.collection("users")
        .doc(uid)
        .get();

    if (!snapshot.exists) {
      return {
        exists: false,
        status: "UNKNOWN"
      };
    }

    const data =
      snapshot.data() || {};

    const status =
      data.securityStatus || "ACTIVE";

    return {
      exists: true,
      status
    };
  }
);
