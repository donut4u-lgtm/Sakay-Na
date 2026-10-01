const crypto = require("crypto");

const { initializeApp } = require("firebase-admin/app");

const {
  getAuth
} = require("firebase-admin/auth");

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
 * ADMIN
 * --------------------------------------------------------- */

const ADMIN_UID =
  "Ld3rzaCvAGNlXBDCofB3mWjgXWp2";


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


function requireAdmin(request) {

  const uid =
    requireAuthenticated(request);

  if (uid !== ADMIN_UID) {

    throw new HttpsError(
      "permission-denied",
      "Admin access required."
    );
  }

  return uid;
}


function normalizePhone(phone) {

  if (typeof phone !== "string") {
    return "";
  }

  return phone
    .replace(/[^\d+]/g, "")
    .trim();
}


function phoneVariants(phone) {

  const digits =
    String(phone || "")
      .replace(/\D/g, "");

  let local = digits;

  if (digits.startsWith("63")) {

    local =
      "0" + digits.substring(2);

  } else if (
    digits.startsWith("9") &&
    digits.length === 10
  ) {

    local =
      "0" + digits;
  }

  if (
    !local.startsWith("0") ||
    local.length !== 11
  ) {

    return [];
  }

  const nine =
    local.substring(1);

  return [
    "0" + nine,
    "63" + nine,
    "+63" + nine,
    nine
  ];
}


function hashValue(value) {

  return crypto
    .createHash("sha256")
    .update(value)
    .digest("hex");
}


function getWindowStart(windowMs) {

  const now =
    Date.now();

  return Math.floor(
    now / windowMs
  ) * windowMs;
}


/* ---------------------------------------------------------
 * REGISTRATION SECURITY
 * --------------------------------------------------------- */

exports.checkRegistrationAllowed = onCall(
  async (request) => {

    const phone =
      normalizePhone(
        request.data?.phone
      );

    if (!phone) {

      throw new HttpsError(
        "invalid-argument",
        "Phone number is required."
      );
    }

    const phoneHash =
      hashValue(phone);

    const dayWindow =
      getWindowStart(
        24 * 60 * 60 * 1000
      );

    const phoneKey =
      `registration_phone_${phoneHash}_${dayWindow}`;

    const globalKey =
      `registration_global_${dayWindow}`;

    const phoneRef =
      db.collection(
        "securityRateLimits"
      ).doc(phoneKey);

    const globalRef =
      db.collection(
        "securityRateLimits"
      ).doc(globalKey);

    let allowed = true;
    let reason = "";

    await db.runTransaction(
      async (transaction) => {

        const phoneSnapshot =
          await transaction.get(
            phoneRef
          );

        const globalSnapshot =
          await transaction.get(
            globalRef
          );

        const phoneCount =
          phoneSnapshot.exists
            ? Number(
                phoneSnapshot.data()
                  .count || 0
              )
            : 0;

        const globalCount =
          globalSnapshot.exists
            ? Number(
                globalSnapshot.data()
                  .count || 0
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
            count:
              phoneCount + 1,

            windowStart:
              dayWindow,

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
            count:
              globalCount + 1,

            windowStart:
              dayWindow,

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
      db.collection(
        "securityRateLimits"
      ).doc(key);

    let allowed = true;

    await db.runTransaction(
      async (transaction) => {

        const snapshot =
          await transaction.get(ref);

        const count =
          snapshot.exists
            ? Number(
                snapshot.data()
                  .count || 0
              )
            : 0;

        if (count >= 10) {
          allowed = false;
        }

        transaction.set(
          ref,
          {
            uid,

            count:
              count + 1,

            windowStart:
              hourWindow,

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
      db.collection(
        "securityRateLimits"
      ).doc(key);

    let allowed = true;

    await db.runTransaction(
      async (transaction) => {

        const snapshot =
          await transaction.get(ref);

        const count =
          snapshot.exists
            ? Number(
                snapshot.data()
                  .count || 0
              )
            : 0;

        if (count >= 5) {
          allowed = false;
        }

        transaction.set(
          ref,
          {
            uid,

            count:
              count + 1,

            windowStart:
              hourWindow,

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
      data.securityStatus ||
      "ACTIVE";

    return {
      exists: true,
      status
    };
  }
);


/* ---------------------------------------------------------
 * ADMIN FIND USER BY PHONE
 * --------------------------------------------------------- */

exports.adminFindUserByPhone = onCall(
  async (request) => {

    requireAdmin(request);

    const variants =
      phoneVariants(
        request.data?.phone
      );

    if (!variants.length) {

      throw new HttpsError(
        "invalid-argument",
        "Enter a valid Philippine mobile number."
      );
    }

    let found = null;

    for (
      const variant of variants
    ) {

      const snapshot =
        await db.collection("users")
          .where(
            "phone",
            "==",
            variant
          )
          .limit(1)
          .get();

      if (!snapshot.empty) {

        found =
          snapshot.docs[0];

        break;
      }
    }

    if (!found) {

      return {
        found: false
      };
    }

    const data =
      found.data() || {};

    const role =
      String(
        data.role || ""
      ).toUpperCase();

    if (
      role !== "PASSENGER" &&
      role !== "DRIVER"
    ) {

      return {
        found: false
      };
    }

    return {

      found: true,

      uid:
        found.id,

      name:
        String(
          data.name ||
          data.fullName ||
          data.driverName ||
          "Not provided"
        ),

      role,

      phone:
        String(
          data.phone || ""
        )
    };
  }
);


/* ---------------------------------------------------------
 * ADMIN RESET USER PASSWORD
 * --------------------------------------------------------- */

exports.adminResetUserPassword = onCall(
  async (request) => {

    requireAdmin(request);

    const targetUid =
      String(
        request.data?.targetUid ||
        ""
      ).trim();

    const newPassword =
      String(
        request.data?.newPassword ||
        ""
      );

    if (!targetUid) {

      throw new HttpsError(
        "invalid-argument",
        "Target account is required."
      );
    }

    if (
      newPassword.length < 6
    ) {

      throw new HttpsError(
        "invalid-argument",
        "Password must be at least 6 characters."
      );
    }

    if (
      targetUid === ADMIN_UID
    ) {

      throw new HttpsError(
        "permission-denied",
        "The Admin account cannot be reset here."
      );
    }

    let userRecord;

    try {

      userRecord =
        await getAuth()
          .getUser(targetUid);

    } catch (error) {

      logger.error(
        "Admin password reset: target user lookup failed.",
        {
          targetUid,
          error:
            error.message
        }
      );

      throw new HttpsError(
        "not-found",
        "Account was not found."
      );
    }

    const email =
      String(
        userRecord.email || ""
      ).toLowerCase();

    if (
      !email.endsWith(
        "@sakayna.app"
      )
    ) {

      throw new HttpsError(
        "failed-precondition",
        "This is not a Sakay Na account."
      );
    }

    try {

      await getAuth()
        .updateUser(
          targetUid,
          {
            password:
              newPassword
          }
        );

      logger.info(
        "Admin reset a Sakay Na account password.",
        {
          adminUid:
            ADMIN_UID,

          targetUid
        }
      );

      return {

        success: true,

        uid:
          targetUid
      };

    } catch (error) {

      logger.error(
        "Admin password reset failed.",
        {
          adminUid:
            ADMIN_UID,

          targetUid,

          error:
            error.message
        }
      );

      throw new HttpsError(
        "internal",
        "Unable to reset the account password."
      );
    }
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

exports.notifyAdminNewDriverApplication =
  onDocumentCreated(
    {
      document:
        "users/{userId}",

      region:
        "asia-southeast1"
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
          approvalStatus ===
            "PENDING_APPROVAL" ||
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
            adminUid:
              ADMIN_UID
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
            adminUid:
              ADMIN_UID
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

          token:
            adminToken,

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


/* ---------------------------------------------------------
 * RIDE CHAT MESSAGE NOTIFICATION
 *
 * Firestore:
 *
 * rides/{rideId}/messages/{messageId}
 *
 * Passenger -> Driver
 * Driver -> Passenger
 *
 * Firestore remains the source of truth.
 * FCM delivers the visible notification.
 *
 * High priority is used for chat so FCM can attempt
 * delivery while the Android device is sleeping/idle.
 * --------------------------------------------------------- */

exports.notifyRideChatMessage =
  onDocumentCreated(
    {
      document:
        "rides/{rideId}/messages/{messageId}",

      region:
        "asia-southeast1"
    },

    async (event) => {

      const snapshot =
        event.data;

      if (!snapshot) {
        return;
      }

      const messageData =
        snapshot.data() || {};

      const rideId =
        String(
          event.params.rideId || ""
        ).trim();

      const messageId =
        String(
          event.params.messageId ||
          snapshot.id ||
          ""
        ).trim();

      if (!rideId || !messageId) {
        return;
      }

      const senderId =
        String(
          messageData.senderId || ""
        ).trim();

      const message =
        String(
          messageData.message ||
          messageData.text ||
          ""
        ).trim();

      if (!senderId || !message) {

        logger.warn(
          "Chat notification skipped: senderId or message is missing.",
          {
            rideId,
            messageId
          }
        );

        return;
      }

      /* ---------------------------------------------------
       * GET RIDE
       * --------------------------------------------------- */

      const rideSnapshot =
        await db.collection("rides")
          .doc(rideId)
          .get();

      if (!rideSnapshot.exists) {

        logger.warn(
          "Chat notification skipped: ride not found.",
          {
            rideId,
            messageId
          }
        );

        return;
      }

      const ride =
        rideSnapshot.data() || {};

      const passengerId =
        String(
          ride.passengerId || ""
        ).trim();

      const driverId =
        String(
          ride.driverId || ""
        ).trim();

      if (!passengerId || !driverId) {

        logger.info(
          "Chat notification skipped: ride has no passenger or driver.",
          {
            rideId,
            passengerId,
            driverId
          }
        );

        return;
      }

      /* ---------------------------------------------------
       * DETERMINE RECIPIENT
       * --------------------------------------------------- */

      let recipientId = "";

      if (
        senderId === passengerId
      ) {

        recipientId =
          driverId;

      } else if (
        senderId === driverId
      ) {

        recipientId =
          passengerId;

      } else {

        logger.warn(
          "Chat notification skipped: sender is not part of ride.",
          {
            rideId,
            messageId,
            senderId,
            passengerId,
            driverId
          }
        );

        return;
      }

      /* ---------------------------------------------------
       * GET RECIPIENT FCM TOKEN
       * --------------------------------------------------- */

      const recipientSnapshot =
        await db.collection("users")
          .doc(recipientId)
          .get();

      if (!recipientSnapshot.exists) {

        logger.warn(
          "Chat notification skipped: recipient profile not found.",
          {
            rideId,
            recipientId
          }
        );

        return;
      }

      const recipient =
        recipientSnapshot.data() || {};

      const fcmToken =
        String(
          recipient.fcmToken || ""
        ).trim();

      if (!fcmToken) {

        logger.warn(
          "Chat notification skipped: recipient has no FCM token.",
          {
            rideId,
            recipientId
          }
        );

        return;
      }

      /* ---------------------------------------------------
       * SENDER ROLE
       * --------------------------------------------------- */

      let senderRole =
        String(
          messageData.senderRole ||
          messageData.role ||
          ""
        ).toUpperCase();

      if (
        senderRole !== "PASSENGER" &&
        senderRole !== "DRIVER"
      ) {

        senderRole =
          senderId === driverId
            ? "DRIVER"
            : "PASSENGER";
      }

      const title =
        senderRole === "DRIVER"
          ? "🛺 Sakay Na — Driver"
          : "👤 Sakay Na — Passenger";

      const body =
        message.length > 120
          ? message.substring(0, 117) + "..."
          : message;

      /* ---------------------------------------------------
       * SEND FCM
       *
       * IMPORTANT:
       * No fixed Android notification "tag" is used.
       *
       * This prevents consecutive chat messages for the
       * same ride from intentionally replacing one another.
       * --------------------------------------------------- */

      try {

        await getMessaging().send({

          token:
            fcmToken,

          notification: {

            title:
              title,

            body:
              body
          },

          data: {

            type:
              "RIDE_CHAT_MESSAGE",

            rideId:
              rideId,

            messageId:
              messageId,

            senderId:
              senderId,

            senderRole:
              senderRole,

            title:
              title,

            message:
              body
          },

          android: {

            priority:
              "high",

            ttl:
              24 * 60 * 60 * 1000,

            notification: {

              channelId:
                "sakayna_ride_updates",

              sound:
                "default"
            }
          }
        });

        logger.info(
          "Ride chat notification sent.",
          {
            rideId,
            messageId,
            senderId,
            recipientId,
            senderRole
          }
        );

      } catch (error) {

        logger.error(
          "Ride chat notification failed.",
          {
            rideId,
            messageId,
            senderId,
            recipientId,
            error:
              error.message
          }
        );
      }
    }
  );
