/**
 * Firebase Configuration
 *
 * IMPORTANT: Replace these values with your actual Firebase project credentials.
 * You can find these in your Firebase Console > Project Settings > General > Your apps
 */
const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Get Firebase Auth instance
const auth = firebase.auth();
