import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

export const sendEmergencyAlert = functions.https.onCall(async (data, context) => {
    // 1. Verify Authentication
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated."
        );
    }

    const uid = context.auth.uid;

    // 2. Verify Admin Status
    const adminDoc = await admin.firestore().collection("admins").doc(uid).get();
    if (!adminDoc.exists) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "You must be an admin to broadcast emergency alerts."
        );
    }

    // 3. Extract Payload
    const title = data.title;
    const message = data.message;

    if (!title || !message) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "The function must be called with a title and message."
        );
    }

    // 4. Send FCM Message to Topic
    const payload = {
        notification: {
            title: title,
            body: message,
        },
        data: {
            title: title,
            message: message,
            type: "emergency",
        },
        topic: "bandarban_alerts"
    };

    try {
        const response = await admin.messaging().send(payload);
        return { success: true, messageId: response };
    } catch (error) {
        console.error("Error sending FCM message:", error);
        throw new functions.https.HttpsError(
            "internal",
            "Failed to send emergency alert."
        );
    }
});
