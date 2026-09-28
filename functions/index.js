const crypto = require("crypto");

const { initializeApp } = require("firebase-admin/app");

const {
  getFirestore,
  FieldValue
} = require("firebase-admin/firestore");

const {
  getMessaging
} = require("firebase-admin/messaging");

const {
  onCall,
  HttpsError
} = require("firebase-functions/v2/https");

const {
  onDocumentCreated
} = require("firebase-functions/v2/firestore");

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

    const phoneHash = hashValue(phone);

    const dayWindow =
      getWindowStart(
        24 * 60 * 60 * 1000
      );

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

    await db.runTransaction(
      async (transaction) => {

        const phoneSnapshot =
          await transaction.get(phoneRef);

        const globalSnapshot =
          await transaction.get(globalRef);

        const phoneCount =
          phoneSnapshot.exists
            ? Number(
                phoneSnapshot.data().count || 0
              )
            : 0;

        const globalCount =
          globalSnapshot.exists
            ? Number(
                globalSnapshot.data().count || 0
              )
            : 0;

        if (phoneCount >= 3) {

          allowed = false;

          reason =
            "Too many registration attempts for this phone number today.";
        }

        if (globalCount >= 100) {

          allowed = false;

          reason =
            "Registration service is temporarily rate limited.";
        }

        transaction.set(
          phoneRef,
          {
            count: phoneCount + 1,
            windowStart: dayWindow,
            lastAttemptAt:
              FieldValue.serverTimestamp(),
            type:
              "REGISTRATION_PHONE"
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
            lastAttemptAt:
              FieldValue.serverTimestamp(),
            type:
              "REGISTRATION_GLOBAL"
          },
          {
            merge: true
          }
        );
      }
    );

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
 * --------------------------------------------------------- */

exports.checkRideRequestAllowed = onCall(
  async (request) => {

    const uid =
      requireAuthenticated(request);

    const hourWindow =
      getWindowStart(
        60 * 60 * 1000
      );

    const key =
      `ride_request_${uid}_${hourWindow}`;

    const ref =
      db.collection("securityRateLimits")
        .doc(key);

    let allowed = true;

    await db.runTransaction(
      async (transaction) => {

        const snapshot =
          await transaction.get(ref);

        const count =
          snapshot.exists
            ? Number(
                snapshot.data().count || 0
              )
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
            lastAttemptAt:
              FieldValue.serverTimestamp(),
            type:
              "RIDE_REQUEST"
          },
          {
            merge: true
          }
        );
      }
    );

    if (!allowed) {

      logger.warn(
        "Ride request blocked",
        {
          uid
        }
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
 * --------------------------------------------------------- */

exports.checkCancellationAllowed = onCall(
  async (request) => {

    const uid =
      requireAuthenticated(request);

    const hourWindow =
      getWindowStart(
        60 * 60 * 1000
      );

    const key =
      `cancellation_${uid}_${hourWindow}`;

    const ref =
      db.collection("securityRateLimits")
        .doc(key);

    let allowed = true;

    await db.runTransaction(
      async (transaction) => {

        const snapshot =
          await transaction.get(ref);

        const count =
          snapshot.exists
            ? Number(
                snapshot.data().count || 0
              )
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
            lastAttemptAt:
              FieldValue.serverTimestamp(),
            type:
              "CANCELLATION"
          },
          {
            merge: true
          }
        );
      }
    );

    if (!allowed) {

      logger.warn(
        "Cancellation blocked",
        {
          uid
        }
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


/* ---------------------------------------------------------
 * ADMIN DRIVER APPLICATION NOTIFICATION
 *
 * Fires when a new DRIVER profile is created.
 *
 * It sends an FCM notification to the Admin phone/app.
 *
 * Existing driver registration data is not changed.
 * --------------------------------------------------------- */

const ADMIN_UID =
  "Ld3rzaCvAGNlXBDCofB3mWjgXWp2";


exports.notifyAdminNewDriverApplication =
  onDocumentCreated(
    {
      document: "users/{userId}",
      region: "asia-southeast1"
    },

    async (event) => {

      const snapshot =
        event.data;

      if (!snapshot) {
        return;
      }

      const driver =
        snapshot.data() || {};

      /* Only DRIVER accounts. */

      if (
        String(
          driver.role || ""
        ).toUpperCase() !== "DRIVER"
      ) {
        return;
      }

      /* Only pending applications. */

      const approved =
        driver.approved === true;

      const driverStatus =
        String(
          driver.driverStatus || ""
        ).toUpperCase();

      const approvalStatus =
        String(
          driver.approvalStatus || ""
        ).toUpperCase();

      const isPending =
        !approved &&
        (
          driverStatus === "PENDING" ||
          approvalStatus === "PENDING_APPROVAL" ||
          (
            !driverStatus &&
            !approvalStatus
          )
        );

      if (!isPending) {
        return;
      }

      /* Get Admin profile. */

      const adminSnapshot =
        await db.collection("users")
          .doc(ADMIN_UID)
          .get();

      if (!adminSnapshot.exists) {

        logger.warn(
          "Admin profile not found.",
          {
            adminUid: ADMIN_UID
          }
        );

        return;
      }

      const adminData =
        adminSnapshot.data() || {};

      const adminToken =
        adminData.fcmToken;

      if (
        typeof adminToken !== "string" ||
        !adminToken.trim()
      ) {

        logger.warn(
          "Admin has no FCM token. Admin must open/login to Sakay Na at least once.",
          {
            adminUid: ADMIN_UID
          }
        );

        return;
      }

      const driverName =
        String(
          driver.name ||
          driver.driverName ||
          "New driver"
        );

      const phone =
        String(
          driver.phone ||
          ""
        );

      const message =
        phone
          ? `${driverName} (${phone}) is waiting for approval.`
          : `${driverName} is waiting for approval.`;

      try {

        await getMessaging().send({

          token: adminToken,

          notification: {

            title:
              "🛺 New Driver Application",

            body:
              message
          },

          data: {

            type:
              "ADMIN_DRIVER_APPLICATION",

            driverId:
              snapshot.id,

            title:
              "🛺 New Driver Application",

            message:
              message
          },

          android: {

            priority:
              "high",

            notification: {

              channelId:
                "sakayna_ride_updates",

              sound:
                "default"
            }
          }
        });

        logger.info(
          "Admin driver application notification sent.",
          {
            driverId:
              snapshot.id,

            driverName:
              driverName
          }
        );

      } catch (error) {

        logger.error(
          "Failed to send Admin driver application notification.",
          {
            error:
              error.message,

            driverId:
              snapshot.id
          }
        );
      }
    }
  );
