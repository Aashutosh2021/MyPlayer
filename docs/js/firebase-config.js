// ─────────────────────────────────────────────────────────────────────────────
// MyPlayer — Firebase configuration
//
// HOW TO FILL THIS IN:
//   1. Go to https://console.firebase.google.com → your project
//   2. Project Settings (gear icon) → "Your apps" → Web app (</>)
//   3. Copy the values from the `firebaseConfig` object shown there
//   4. Paste them below, replacing every "PASTE_..." placeholder
//
// This file is the ONLY place you need to edit to wire the site to Firebase.
// It is loaded by index.html, releases.html and admin.html.
// ─────────────────────────────────────────────────────────────────────────────

export const firebaseConfig = {
  apiKey: "PASTE_API_KEY",
  authDomain: "PASTE_PROJECT_ID.firebaseapp.com",
  projectId: "PASTE_PROJECT_ID",
  storageBucket: "PASTE_PROJECT_ID.appspot.com",
  messagingSenderId: "PASTE_SENDER_ID",
  appId: "PASTE_APP_ID"
};

// Super-admin panel password hint is NOT stored here.
// Admin login uses Firebase Authentication (email + password) —
// create the admin user in: Firebase Console → Authentication → Users → Add user.
