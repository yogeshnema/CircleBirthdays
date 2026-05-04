const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.notifyNewDiscussion = functions.firestore
    .document('discussions/{discussionId}')
    .onCreate(async (snapshot, context) => {
        const discussion = snapshot.data();
        if (discussion.status === 'APPROVED') {
            const payload = {
                notification: {
                    title: 'New Discussion',
                    body: `${discussion.userName} started a new discussion: ${discussion.title}`,
                },
                topic: 'all_discussions'
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyNewMessage = functions.firestore
    .document('channels/{channelId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const receiverId = message.receiverId;
        const senderName = message.senderName;

        const receiverDoc = await admin.firestore().collection('members').document(receiverId).get();
        const receiverData = receiverDoc.data();

        if (receiverData && receiverData.fcmToken) {
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: message.text,
                },
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'family_circle_notifications',
                        priority: 'high',
                        visibility: 'public'
                    }
                },
                data: {
                    type: "CHAT",
                    senderId: message.senderId,
                    senderName: senderName
                },
                token: receiverData.fcmToken
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyDiscussionApproved = functions.firestore
    .document('discussions/{discussionId}')
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const prevData = change.before.data();

        if (prevData.status === 'PENDING' && newData.status === 'APPROVED') {
            const payload = {
                notification: {
                    title: 'Discussion Approved',
                    body: `Your discussion "${newData.title}" has been approved.`,
                },
                topic: 'all_discussions'
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyNewMessage = functions.firestore
    .document('channels/{channelId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const receiverId = message.receiverId;
        const senderName = message.senderName;

        const receiverDoc = await admin.firestore().collection('members').document(receiverId).get();
        const receiverData = receiverDoc.data();

        if (receiverData && receiverData.fcmToken) {
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: message.text,
                },
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'family_circle_notifications',
                        priority: 'high',
                        visibility: 'public'
                    }
                },
                data: {
                    type: "CHAT",
                    senderId: message.senderId,
                    senderName: senderName
                },
                token: receiverData.fcmToken
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyNewComment = functions.firestore
    .document('discussions/{discussionId}')
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const prevData = change.before.data();

        if (newData.comments.length > prevData.comments.length) {
            const newComment = newData.comments[newData.comments.length - 1];
            const payload = {
                notification: {
                    title: 'New Comment',
                    body: `${newComment.userName} commented on "${newData.title}"`,
                },
                topic: `discussion_${context.params.discussionId}`
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyNewMessage = functions.firestore
    .document('channels/{channelId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const receiverId = message.receiverId;
        const senderName = message.senderName;

        const receiverDoc = await admin.firestore().collection('members').document(receiverId).get();
        const receiverData = receiverDoc.data();

        if (receiverData && receiverData.fcmToken) {
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: message.text,
                },
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'family_circle_notifications',
                        priority: 'high',
                        visibility: 'public'
                    }
                },
                data: {
                    type: "CHAT",
                    senderId: message.senderId,
                    senderName: senderName
                },
                token: receiverData.fcmToken
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyNewGalleryUpload = functions.firestore
    .document('memories/{memoryId}')
    .onCreate(async (snapshot, context) => {
        const memory = snapshot.data();
        if (memory.status === 'APPROVED') {
            const payload = {
                notification: {
                    title: 'New Gallery Upload',
                    body: `${memory.userName} shared a new photo.`,
                },
                topic: 'gallery_updates'
            };
            return admin.messaging().send(payload);
        }
        return null;
    });

exports.notifyNewMessage = functions.firestore
    .document('channels/{channelId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const receiverId = message.receiverId;
        const senderName = message.senderName;

        const receiverDoc = await admin.firestore().collection('members').document(receiverId).get();
        const receiverData = receiverDoc.data();

        if (receiverData && receiverData.fcmToken) {
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: message.text,
                },
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'family_circle_notifications',
                        priority: 'high',
                        visibility: 'public'
                    }
                },
                data: {
                    type: "CHAT",
                    senderId: message.senderId,
                    senderName: senderName
                },
                token: receiverData.fcmToken
            };
            return admin.messaging().send(payload);
        }
        return null;
    });
