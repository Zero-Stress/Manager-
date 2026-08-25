import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Cloud Function: onDailyLogWrite
 * Triggered when a document is created, updated, or deleted in the dailylogs collection.
 * Sends notifications to all registered users about leaderboard changes.
 */
export const onDailyLogWrite = functions.firestore
  .document("dailylogs/{logId}")
  .onWrite(async (change, context) => {
    try {
      // Get all users with FCM tokens
      const usersSnapshot = await db
        .collection("users")
        .where("fcmToken", "!=", null)
        .where("status", "==", "confirmed")
        .get();

      if (usersSnapshot.empty) {
        console.log("No users with FCM tokens found");
        return;
      }

      // Prepare notification payload
      const logId = context.params.logId;

      // Determine the type of change
      let title = "🏆 Daily Leaderboard Updated";
      let body = "New match data has been recorded!";

      if (!change.after.exists) {
        // Document deleted
        title = "📊 Daily Log Reset";
        body = "Daily match records have been cleared.";
      } else if (change.before.exists && change.after.exists) {
        // Document updated
        const newData = change.after.data();
        body = `${newData?.playerName || "A player"} updated their stats - Check the leaderboard!`;
      } else {
        // New document created
        const newData = change.after.data();
        body = `${newData?.playerName || "A player"} recorded new match data!`;
      }

      // Send notifications to all users
      const tokens: string[] = [];
      usersSnapshot.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) {
          tokens.push(token);
        }
      });

      if (tokens.length === 0) {
        console.log("No valid FCM tokens to send to");
        return;
      }

      // Create the message payload
      const message: admin.messaging.MulticastMessage = {
        tokens: tokens,
        notification: {
          title: title,
          body: body,
        },
        data: {
          type: "daily_leaderboard",
          title: title,
          body: body,
          logId: logId,
          timestamp: Date.now().toString(),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "leaderboard_updates",
            clickAction: "OPEN_ACTIVITY_1",
          },
        },
      };

      // Send the notifications
      const response = await messaging.sendEachForMulticast(message);
      console.log(`Notifications sent: ${response.successCount} success, ${response.failureCount} failures`);

      // Handle failed tokens (remove invalid ones)
      if (response.failureCount > 0) {
        const failedTokens: string[] = [];
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            failedTokens.push(tokens[idx]);
          }
        });

        // Remove failed tokens from Firestore
        for (const token of failedTokens) {
          const usersWithToken = await db
            .collection("users")
            .where("fcmToken", "==", token)
            .get();

          usersWithToken.forEach(async (doc) => {
            await doc.ref.update({ fcmToken: null });
            console.log(`Removed invalid token from user ${doc.id}`);
          });
        }
      }
    } catch (error) {
      console.error("Error sending daily log notification:", error);
    }
  });

/**
 * Cloud Function: onAnnouncementWrite
 * Triggered when a new announcement is posted.
 * Sends notification to all users.
 */
export const onAnnouncementWrite = functions.firestore
  .document("announcements/{announcementId}")
  .onCreate(async (snap, context) => {
    try {
      const announcementData = snap.data();
      const message = announcementData?.message || "New announcement available";

      // Get all users with FCM tokens
      const usersSnapshot = await db
        .collection("users")
        .where("fcmToken", "!=", null)
        .where("status", "==", "confirmed")
        .get();

      if (usersSnapshot.empty) {
        console.log("No users with FCM tokens found for announcement");
        return;
      }

      const tokens: string[] = [];
      usersSnapshot.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) {
          tokens.push(token);
        }
      });

      if (tokens.length === 0) return;

      const notificationPayload: admin.messaging.MulticastMessage = {
        tokens: tokens,
        notification: {
          title: "📢 New Announcement",
          body: message,
        },
        data: {
          type: "announcement",
          title: "New Announcement",
          body: message,
          announcementId: context.params.announcementId,
          timestamp: Date.now().toString(),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "leaderboard_updates",
            clickAction: "OPEN_ACTIVITY_1",
          },
        },
      };

      const response = await messaging.sendEachForMulticast(notificationPayload);
      console.log(`Announcement notifications sent: ${response.successCount} success`);

      // Cleanup failed tokens
      if (response.failureCount > 0) {
        const failedTokens: string[] = [];
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            failedTokens.push(tokens[idx]);
          }
        });

        for (const token of failedTokens) {
          const usersWithToken = await db
            .collection("users")
            .where("fcmToken", "==", token)
            .get();

          usersWithToken.forEach(async (doc) => {
            await doc.ref.update({ fcmToken: null });
          });
        }
      }
    } catch (error) {
      console.error("Error sending announcement notification:", error);
    }
  });

/**
 * Cloud Function: weeklyLeaderboardSummary
 * Scheduled function that runs every Sunday at 9 PM to send weekly summary.
 */
export const weeklyLeaderboardSummary = functions.pubsub
  .schedule("0 21 * * 0") // Every Sunday at 9 PM
  .timeZoneAsia("Dhaka")
  .onRun(async (context) => {
    try {
      // Get all daily logs
      const dailyLogsSnapshot = await db.collection("dailylogs").get();

      if (dailyLogsSnapshot.empty) {
        console.log("No daily logs found for weekly summary");
        return;
      }

      // Aggregate weekly data
      const weeklyData: { [key: string]: any } = {};

      dailyLogsSnapshot.forEach((doc) => {
        const data = doc.data();
        const name = data.playerName;

        if (!weeklyData[name]) {
          weeklyData[name] = {
            matches: 0,
            wins: 0,
            kills: 0,
            damage: 0,
          };
        }

        weeklyData[name].matches += data.matches || 0;
        weeklyData[name].wins += data.wins || 0;
        weeklyData[name].kills += data.kills || 0;
        weeklyData[name].damage += data.damage || 0;
      });

      // Calculate scores and sort
      const leaderboard = Object.entries(weeklyData)
        .map(([name, data]: [string, any]) => ({
          name,
          score: Math.round((data.kills * 10) + (data.damage / 100) + (data.wins * 50)),
          matches: data.matches,
        }))
        .sort((a, b) => b.score - a.score)
        .slice(0, 10);

      if (leaderboard.length === 0) return;

      // Prepare summary message
      const top3 = leaderboard.slice(0, 3);
      let summary = "Weekly Top Performers:\n";
      top3.forEach((player, index) => {
        const medals = ["🥇", "🥈", "🥉"];
        summary += `${medals[index]} ${player.name} - ${player.score} pts\n`;
      });

      // Get all users with FCM tokens
      const usersSnapshot = await db
        .collection("users")
        .where("fcmToken", "!=", null)
        .where("status", "==", "confirmed")
        .get();

      const tokens: string[] = [];
      usersSnapshot.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) tokens.push(token);
      });

      if (tokens.length === 0) return;

      const notificationPayload: admin.messaging.MulticastMessage = {
        tokens: tokens,
        notification: {
          title: "🏆 Weekly Leaderboard Summary",
          body: summary,
        },
        data: {
          type: "weekly_leaderboard",
          title: "Weekly Leaderboard Summary",
          body: summary,
          timestamp: Date.now().toString(),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "leaderboard_updates",
            clickAction: "OPEN_ACTIVITY_1",
          },
        },
      };

      const response = await messaging.sendEachForMulticast(notificationPayload);
      console.log(`Weekly summary sent: ${response.successCount} success`);
    } catch (error) {
      console.error("Error sending weekly summary:", error);
    }
  });
