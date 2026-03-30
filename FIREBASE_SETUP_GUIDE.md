# Firebase Console Setup Guide for MessApp

This guide details all Firebase Console configurations needed for the MessApp to work correctly.

---

## 1. Firestore Database Rules

### Step-by-Step:
1. Go to https://console.firebase.google.com/
2. Select your project
3. Click **Firestore Database** in left sidebar
4. Click **Rules** tab
5. Replace the default rules with one of the options below:

### Option A: Development Mode (Open Access) - FOR TESTING ONLY
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

### Option B: Production Mode (Secure) - RECOMMENDED
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - only authenticated users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Messes collection - authenticated users can read, owners can write
    match /messes/{messId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && resource.data.ownerId == request.auth.uid;
    }
    
    // Allow public read access to messes for guest mode
    match /messes/{messId} {
      allow read: if true;
    }
  }
}
```

6. Click **Publish**
7. Wait 1-2 minutes for rules to propagate

---

## 2. Firebase Authentication Setup

### Enable Email/Password Authentication:
1. In Firebase Console, click **Authentication** in left sidebar
2. Click **Get Started** (if first time)
3. Go to **Sign-in method** tab
4. Click **Email/Password**
5. Toggle **Enable** to ON
6. Click **Save**

### Enable Google Sign-In:
1. In **Sign-in method** tab
2. Click **Google**
3. Toggle **Enable** to ON
4. Enter your support email
5. Click **Save**

### Configure OAuth Redirect (Important for Google Sign-In):
1. Go to https://console.cloud.google.com/
2. Select your Firebase project
3. Go to **APIs & Services** → **Credentials**
4. Find your OAuth 2.0 Client ID (Web client)
5. Add your app's package name and SHA-1 certificate fingerprint

---

## 3. Get SHA-1 Certificate Fingerprint

### For Debug Builds:
**Windows:**
```bash
cd %USERPROFILE%\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Mac/Linux:**
```bash
cd ~/.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### Add to Firebase:
1. In Firebase Console, click ⚙️ **Settings** → **Project settings**
2. Go to **Your apps** section
3. Select your Android app
4. Click **Add fingerprint**
5. Paste the SHA-1 value
6. Click **Save**

---

## 4. Download Updated google-services.json

After adding SHA-1:
1. In **Project settings** → **Your apps**
2. Click your Android app
3. Click **Download google-services.json**
4. Replace the existing file in your app's `app/` folder
5. Rebuild your project

---

## 5. Firestore Database Structure (Manual Creation)

If needed, manually create these collections in Firebase Console:

### Collection: `users`
Document fields:
- `email` (string)
- `role` (string) - "USER" or "MESS_OWNER"
- `messId` (string) - ID of mess for user, or generated ID for owner
- `messName` (string) - only for MESS_OWNER
- `fcmToken` (string) - for push notifications

### Collection: `messes`
Document fields:
- `ownerId` (string) - UID of mess owner
- `name` (string) - mess name
- `studentCount` (number)
- `createdAt` (timestamp)

---

## 6. Common Issues & Fixes

### "Permission Denied" Error:
- Firestore rules not published or still propagating (wait 2-3 minutes)
- User not authenticated (check Authentication tab for signed-in users)
- Rules too restrictive for the operation

### "User data not found" Error:
- User authenticated but Firestore document doesn't exist
- Need to complete signup flow to create user document

### Google Sign-In Not Working:
- SHA-1 not added to Firebase project
- Google Sign-in method not enabled in Authentication
- `google-services.json` file outdated

---

## 7. Testing Checklist

After setup, verify these work:
- [ ] Email/Password signup creates user in Authentication
- [ ] Signup creates document in Firestore `users` collection
- [ ] Login retrieves user role from Firestore
- [ ] Mess Owner signup creates document in `messes` collection
- [ ] Google Sign-in works
- [ ] Guest mode can view messes (if using public read rules)

---

## 8. Security Recommendations for Production

1. **Enable App Check** to prevent abuse
2. **Set up Firebase Security Rules** properly (not open access)
3. **Enable Firebase Analytics** for monitoring
4. **Set up Firebase Crashlytics** for error reporting
5. **Enable Cloud Functions** for server-side operations (payments, sensitive data)

---

## Quick Reference Links

- Firebase Console: https://console.firebase.google.com/
- Google Cloud Console: https://console.cloud.google.com/
- Firestore Rules Docs: https://firebase.google.com/docs/firestore/security/get-started
