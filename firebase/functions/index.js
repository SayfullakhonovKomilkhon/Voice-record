/**
 * Deals Recorder - Firebase Cloud Functions
 * Powered by Google Gemini AI (Structured JSON Outputs)
 * 2nd Generation Cloud Functions (v2) with App Check Enforce
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const { getMessaging } = require("firebase-admin/messaging");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const fs = require("fs");
const path = require("path");
const os = require("os");

// Initialize Firebase Admin SDK
initializeApp();
const db = getFirestore();
const storage = getStorage();
const messaging = getMessaging();

/**
 * Cloud Function to process meeting audio.
 * Enforces App Check and checks monthly user quotas.
 * Utilizes Gemini 1.5/2.0 for structured transcript and executive analysis.
 */
exports.processMeeting = onCall({ enforceAppCheck: true, timeoutSeconds: 300 }, async (request) => {
  // 1. App Check & Auth Validation
  if (!request.auth) {
    logger.error("Unauthenticated call attempt");
    throw new HttpsError("unauthenticated", "User must be authenticated to trigger analysis.");
  }

  const uid = request.auth.uid;
  const { meetingId, vocabulary, duration } = request.data;

  if (!meetingId) {
    logger.error("Missing meetingId parameter");
    throw new HttpsError("invalid-argument", "Missing required 'meetingId' parameter.");
  }

  const providedDuration = duration || 120; // fallback in seconds (2 min)

  // Retrieve Gemini API Key from environment variables
  const geminiApiKey = process.env.GEMINI_API_KEY;
  if (!geminiApiKey) {
    logger.error("GEMINI_API_KEY is not configured in Cloud Functions env variables");
    throw new HttpsError("failed-precondition", "Gemini API key is not configured on the server backend.");
  }

  // 2. Monthly Usage checks
  // Free limits: 3 hours/month = 180 minutes = 10,800 seconds
  const userRef = db.collection("users").doc(uid);
  const userDoc = await userRef.get();
  
  let userData = {
    tier: "free",
    monthlyUsedSeconds: 0,
    lastResetMonth: new Date().getMonth()
  };

  if (userDoc.exists) {
    userData = { ...userData, ...userDoc.data() };
  }

  const currentMonth = new Date().getMonth();
  // Monthly usage reset logic
  if (userData.lastResetMonth !== currentMonth) {
    userData.monthlyUsedSeconds = 0;
    userData.lastResetMonth = currentMonth;
    await userRef.set({
      monthlyUsedSeconds: 0,
      lastResetMonth: currentMonth
    }, { merge: true });
  }

  const LIMIT_SECONDS = (userData.tier === "pro" ? 100 : 3) * 3600; // 3 hours for free users, 100 for Pro
  if (userData.monthlyUsedSeconds + providedDuration > LIMIT_SECONDS) {
    logger.warn(`User ${uid} exceeded quota. Current: ${userData.monthlyUsedSeconds}, Request: ${providedDuration}`);
    
    // Update meeting status to inform client of quota error
    await db.collection("meetings").doc(meetingId).update({
      status: "failed",
      errorText: "limit_exceeded"
    });
    
    throw new HttpsError("resource-exhausted", "limit_exceeded");
  }

  // Fetch target meeting info
  const meetingRef = db.collection("meetings").doc(meetingId);
  const meetingDoc = await meetingRef.get();

  if (!meetingDoc.exists) {
    throw new HttpsError("not-found", `Meeting with ID ${meetingId} not found.`);
  }

  const meetingData = meetingDoc.data();
  if (meetingData.ownerId !== uid) {
    throw new HttpsError("permission-denied", "You do not own this meeting registration.");
  }

  // Mark meeting as actively processing
  await meetingRef.update({
    status: "processing",
    errorText: null
  });

  const bucket = storage.bucket();
  const fileExtension = meetingData.audioExtension || "wav";
  const audioStoragePath = `meetings/${uid}/${meetingId}/audio.${fileExtension}`;
  const tempFilePath = path.join(os.tmpdir(), `meeting_${meetingId}.${fileExtension}`);

  try {
    // 3. Download the audio file to local temporary filesystem for Gemini inline upload
    logger.info(`Downloading audio file source: ${audioStoragePath}`);
    await bucket.file(audioStoragePath).download({ destination: tempFilePath });
    
    const audioBuffer = fs.readFileSync(tempFilePath);
    const base64Audio = audioBuffer.toString("base64");
    const mimeType = fileExtension === "wav" ? "audio/wav" : fileExtension === "aac" ? "audio/aac" : "audio/mpeg";

    logger.info("Initializing Google Gemini API connection");
    const genAI = new GoogleGenerativeAI(geminiApiKey);

    // 4. First Call: Transcription with structured model settings (forcing JSON array)
    logger.info("Running Gemini Transcription process");
    
    const transcriptionModelName = "gemini-1.5-flash"; // stable and rapid model for voice audio
    const tModel = genAI.getGenerativeModel({
      model: transcriptionModelName,
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema: {
          type: "ARRAY",
          items: {
            type: "OBJECT",
            properties: {
              speaker: { type: "STRING" },
              text: { type: "STRING" },
              start: { type: "NUMBER", description: "Start timestamp of current speaker speech in seconds" },
              end: { type: "NUMBER", description: "End timestamp of current speaker speech in seconds" }
            },
            required: ["speaker", "text", "start", "end"]
          }
        },
        temperature: 0.2
      }
    });

    const transcriptionPrompt = `You are a professional audio transcription engine. Carefully process this meeting audio file.
Extract the spoken words, perform diarization and segment speech into clean phrases by speaker.
Label distinct unidentified speakers strictly as 'Speaker 1', 'Speaker 2', etc. (Russian equivalent: 'Спикер 1', 'Спикер 2' or match current locale).
Transcribe the exact words in the language spoken. We expect Russian, English, or Uzbek.
Vocabulary terms to enhance spelling accuracy: ${vocabulary || "None"}.
Provide the segment timing timestamps in seconds relative to recording start.`;

    const transcriptionResponse = await tModel.generateContent([
      {
        inlineData: {
          data: base64Audio,
          mimeType: mimeType
        }
      },
      transcriptionPrompt
    ]);

    const transcriptJsonText = transcriptionResponse.response.text();
    const segments = JSON.parse(transcriptJsonText);
    logger.info(`Successfully parsed ${segments.length} speech segments from audio transcription.`);

    // 5. Second Call: Semantic Analysis & Protocol Synthesis
    logger.info("Running Gemini Semantic Analysis process");
    const analysisModel = genAI.getGenerativeModel({
      model: "gemini-1.5-flash",
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema: {
          type: "OBJECT",
          properties: {
            summary: { type: "STRING", description: "Brief executive summary of 3-5 sentences summarizing the context" },
            topics: {
              type: "ARRAY",
              items: { type: "STRING" },
              description: "Extracted keyword categories or discussion themes"
            },
            decisions: { type: "STRING", description: "Determined protocols, approvals, agreements, and who initiated" },
            tasks: {
              type: "ARRAY",
              items: {
                type: "OBJECT",
                properties: {
                  task: { type: "STRING", description: "Concrete description of task assignment" },
                  assignee: { type: "STRING", description: "Assignee person name or role" },
                  dueDate: { type: "STRING", description: "Target deadline or date of completion (DD-MM-YYYY)" }
                },
                required: ["task", "assignee", "dueDate"]
              }
            }
          },
          required: ["summary", "topics", "decisions", "tasks"]
        },
        temperature: 0.1
      }
    });

    const analysisPrompt = `You are an expert executive secretary and business analyst. 
Analyze the following JSON structured transcript segments taken from the business meeting.
Synthesize an organized meeting protocol. Provide the details in Russian.
Transcript:
${JSON.stringify(segments)}`;

    const analysisResponse = await analysisModel.generateContent(analysisPrompt);
    const analysisJsonText = analysisResponse.response.text();
    const analysis = JSON.parse(analysisJsonText);

    // 6. Save back to Firestore
    logger.info("Saving results back to Cloud Firestore");
    const batch = db.batch();

    // Set segment elements
    const segmentsCollRef = meetingRef.collection("segments");
    // Clear any stale segments if existing
    const oldSegs = await segmentsCollRef.get();
    oldSegs.forEach(doc => batch.delete(doc.ref));

    segments.forEach((seg, index) => {
      const segId = `segment_${String(index).padStart(4, "0")}`;
      batch.set(segmentsCollRef.doc(segId), {
        speaker: seg.speaker,
        text: seg.text,
        start: seg.start,
        end: seg.end,
        timestamp: index
      });
    });

    // Set Analysis components
    const analysisCollRef = meetingRef.collection("analysis");
    batch.set(analysisCollRef.doc("summary_overview"), { content: analysis.summary });
    batch.set(analysisCollRef.doc("summary_topics"), { items: analysis.topics });
    batch.set(analysisCollRef.doc("summary_decisions"), { content: analysis.decisions });
    batch.set(analysisCollRef.doc("summary_tasks"), { items: analysis.tasks });

    // Update meeting envelope information
    batch.update(meetingRef, {
      status: "completed",
      duration: providedDuration,
      summaryOverview: analysis.summary,
      summaryTopics: analysis.topics || [],
      summaryDecisions: analysis.decisions || "",
      summaryTasksCount: analysis.tasks ? analysis.tasks.length : 0,
      processedAt: FieldValue.serverTimestamp()
    });

    // Commit batch saving operations
    await batch.commit();

    // 7. Delete audio file from Firebase Storage for privacy reasons
    logger.info(`Deleting source audio file to guarantee user privacy: ${audioStoragePath}`);
    try {
      await bucket.file(audioStoragePath).delete();
    } catch (cleanupErr) {
      logger.error("Failed to delete processed audio file from GCS bucket storage:", cleanupErr);
    }

    // 8. Update user monthly usage counter
    await userRef.set({
      monthlyUsedSeconds: FieldValue.increment(providedDuration)
    }, { merge: true });

    // 9. Send FCM Push Notification
    logger.info("Dispatching completion push notification via FCM");
    if (userData.fcmTokens && Array.isArray(userData.fcmTokens) && userData.fcmTokens.length > 0) {
      const message = {
        notification: {
          title: "Встреча расшифрована! 🎉",
          body: `Встреча "${meetingData.title || "Без названия"}" обработана. Саммари и задачи сохранены.`
        },
        data: {
          click_action: "FLUTTER_NOTIFICATION_CLICK", // Standard tag
          meetingId: meetingId,
          type: "MEETING_PROCESSED"
        },
        tokens: userData.fcmTokens
      };

      try {
        const response = await messaging.sendEachForMulticast(message);
        logger.info(`FCM notifications dispatched. Success: ${response.successCount}, Failure: ${response.failureCount}`);
      } catch (fcmErr) {
        logger.error("Error sending push notifications through Firebase Cloud Messaging", fcmErr);
      }
    } else {
      logger.info("No registered FCM tokens found for user. Skipping push notice.");
    }

    // Return final success wrapper to client
    return {
      status: "success",
      meetingId: meetingId,
      summary: analysis.summary
    };

  } catch (err) {
    logger.error("Uncaught exception in processMeeting function:", err);
    
    // Update meeting status to inform UI of processing failure
    await meetingRef.update({
      status: "failed",
      errorText: err.message || "Unknown error during meeting transcription"
    });

    // Clean up local temp file if existing
    if (fs.existsSync(tempFilePath)) {
      try {
        fs.unlinkSync(tempFilePath);
      } catch (e) {}
    }

    throw new HttpsError("internal", err.message || "An internal error occurred during transcription processing.");
  }
});

/**
 * Cloud Function enrollVoice stub (to be fully implemented with biometrics training later)
 */
exports.enrollVoice = onCall({ enforceAppCheck: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  
  const uid = request.auth.uid;
  const { profileName, samplesCount } = request.data;
  
  logger.info(`Stub enrollVoice triggered for user: ${uid}, profile: ${profileName}`);
  
  return {
    status: "success",
    message: "Voice enrollment stub completed successfully. Voice biometrics matches will be supported in future releases.",
    profileId: "profile_stub_" + Math.random().toString(36).substr(2, 9)
  };
});
