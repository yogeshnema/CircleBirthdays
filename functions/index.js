const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const CHANNEL_ID = 'family_circle_notifications';
const ADMIN_PHONE = process.env.ADMIN_PHONE || (functions.config().app && functions.config().app.admin_phone) || '';
const ADMIN_NAME = process.env.ADMIN_NAME || (functions.config().app && functions.config().app.admin_name) || 'Admin';

function isAdminMember(member) {
    return !!member && (member.isAdmin || (!!ADMIN_PHONE && member.phoneNumber === ADMIN_PHONE && member.name === ADMIN_NAME));
}

// Helper to send to topic
async function sendToTopic(topic, title, body, data = {}) {
    const payload = {
        topic: topic,
        notification: { title, body },
        data: data,
        android: {
            priority: 'high',
            notification: {
                channelId: CHANNEL_ID,
                sound: 'default',
                defaultSound: true,
                defaultVibrateTimings: true
            }
        }
    };
    return admin.messaging().send(payload);
}

// 1. Direct Messages
exports.notifyNewMessage = functions.firestore
    .document('channels/{channelId}/messages/{messageId}')
    .onCreate(async (snapshot) => {
        const message = snapshot.data();

        // Suppress notification if sender is admin
        const senderDoc = await admin.firestore().collection('members').doc(message.senderId).get();
        const senderData = senderDoc.data();
        if (isAdminMember(senderData)) {
            return null;
        }

        const receiverDoc = await admin.firestore().collection('members').doc(message.receiverId).get();
        const receiverData = receiverDoc.data();

        if (receiverData && receiverData.fcmToken) {
            const payload = {
                token: receiverData.fcmToken,
                notification: {
                    title: `Message from ${message.senderName}`,
                    body: message.text,
                },
                data: { type: "CHAT", senderId: message.senderId },
                android: {
                    priority: 'high',
                    notification: { channelId: CHANNEL_ID, sound: 'default' }
                }
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

// 2. Daily Events at 7 AM (India Time)
exports.dailyEventReminder = functions.pubsub.schedule('0 7 * * *')
    .timeZone('Asia/Kolkata')
    .onRun(async () => {
        const today = new Date();
        const monthDay = `${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
        const membersSnapshot = await admin.firestore().collection('members').get();
        let events = [];
        membersSnapshot.forEach(doc => {
            const d = doc.data();
            // Suppress if admin
            if (isAdminMember(d)) return;

            if (d.dateOfBirth && d.dateOfBirth.includes(monthDay)) events.push(`🎂 ${d.name}'s Birthday`);
            if (d.marriageDate && d.marriageDate.includes(monthDay)) events.push(`💍 ${d.name}'s Anniversary`);
        });
        if (events.length > 0) return sendToTopic('events', "Today's Family Events", events.join(', '));
        return null;
    });

// 3. New Photo / Gallery Approval
exports.notifyGalleryUpdate = functions.firestore.document('memories/{id}').onWrite(async (change) => {
    const newData = change.after.data();
    const prevData = change.before.data();
    if (newData && newData.status === 'APPROVED' && (!prevData || prevData.status === 'PENDING')) {
        // Suppress notification if author is admin
        const authorDoc = await admin.firestore().collection('members').doc(newData.userId).get();
        const authorData = authorDoc.data();
        if (isAdminMember(authorData)) {
            return sendToTopic('admin_only', 'New Gallery Photo (Admin)', `${newData.userName} shared a new memory.`);
        }
        return sendToTopic('gallery_updates', 'New Gallery Photo', `${newData.userName} shared a new memory.`);
    }
    return null;
});

// 4. Admin Approvals Required
exports.notifyAdminApprovals = functions.firestore.document('pending_updates/{id}').onCreate(async (snap) => {
    return sendToTopic('admin_approvals', 'Approval Required', `New change request for ${snap.data().name}.`, { type: "ADMIN_APPROVALS" });
});

// 4b. Account Deletion Requests
exports.notifyDeletionRequest = functions.firestore.document('deletion_requests/{id}').onCreate(async (snap) => {
    return sendToTopic('admin_approvals', 'Deletion Request', `Account deletion requested for ${snap.data().name}.`, { type: "ADMIN_APPROVALS" });
});

// 4c. Relationship Override Requests
exports.notifyRelationshipOverrideRequest = functions.firestore.document('relationship_overrides/{id}').onCreate(async (snap) => {
    const data = snap.data();
    return sendToTopic('admin_approvals', 'Relationship Request', `${data.observerName} requested to label ${data.targetName} as "${data.relationship}".`, { type: "ADMIN_APPROVALS" });
});

// 6. User Status Update (When Admin approves/rejects)
exports.notifyUserStatusUpdate = functions.firestore.document('members/{id}').onUpdate(async (change) => {
    const newData = change.after.data();
    const prevData = change.before.data();

    if (newData.fcmToken) {
        // 6a. Profile Approval
        const statusJustApproved = newData.status === 'APPROVED' && prevData.status !== 'APPROVED';
        if (statusJustApproved) {
            return admin.messaging().send({
                token: newData.fcmToken,
                notification: { title: "Profile Approved", body: "Your profile has been approved by the admin." },
                data: { type: "PROFILE" },
                android: { priority: 'high', notification: { channelId: CHANNEL_ID, sound: 'default' } }
            });
        }

        // 6b. Relationship Override Approval (Manual relationship updated)
        const oldManual = prevData.manualRelationships || {};
        const newManual = newData.manualRelationships || {};
        const addedKeys = Object.keys(newManual).filter(k => !oldManual.hasOwnProperty(k));

        if (addedKeys.length > 0) {
            // Someone (an admin) updated a manual relationship for this user.
            // We want to notify the person who *requested* it (the observer).
            // This is tricky because the trigger is on the target member.
            // We'll need to fetch the observer's token if we want to notify them here,
            // OR we rely on the fact that the observer will see it next time they view the profile.
            // For now, let's notify the target user that their label was updated.
            return admin.messaging().send({
                token: newData.fcmToken,
                notification: { title: "Relationship Updated", body: "An admin has updated how a family member sees your profile." },
                data: { type: "PROFILE" },
                android: { priority: 'high', notification: { channelId: CHANNEL_ID, sound: 'default' } }
            });
        }
    }
    return null;
});

// 5. New Discussion Approval
exports.notifyDiscussionUpdate = functions.firestore.document('discussions/{id}').onWrite(async (change) => {
    const newData = change.after.data();
    const prevData = change.before.data();
    if (newData && newData.status === 'APPROVED' && (!prevData || prevData.status === 'PENDING')) {
        // Suppress notification if author is admin
        const authorDoc = await admin.firestore().collection('members').doc(newData.userId).get();
        const authorData = authorDoc.data();
        if (isAdminMember(authorData)) {
            return sendToTopic('admin_only', 'New Discussion (Admin)', `${newData.userName} started: ${newData.title}`);
        }
        return sendToTopic('all_discussions', 'New Discussion', `${newData.userName} started: ${newData.title}`);
    }
    return null;
});
