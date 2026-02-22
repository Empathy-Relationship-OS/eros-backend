# Eros Backend WebApp Test Client

A standalone web application for testing the Eros backend user flows locally. This is an **independent client application** that makes API calls to your Ktor backend, simulating how a real frontend would interact with your APIs.

## Features

- **Firebase Authentication**: Login with email/password using Firebase
- **User Profile Management**:
  - Check if user profile exists
  - Create new user profile
  - View user profile
  - Update user profile
  - Delete user account
- **Photo Management**:
  - List user photos
  - Upload photos (presigned S3 URL flow)
  - Delete photos
- **API Response Viewer**: Real-time display of API requests and responses

## Setup

### 1. Configure Firebase

Edit `webapp/static/js/firebase-config.js` and replace the placeholder values with your actual Firebase project credentials:

```javascript
const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID"
};
```

You can find these values in:
- Firebase Console → Project Settings → General → Your apps → Web app

### 2. Create Test Users in Firebase

Since this is a test app, you'll need to create test users directly in Firebase:

**Option A: Firebase Console**
1. Go to Firebase Console → Authentication → Users
2. Click "Add User"
3. Enter email and password
4. Click "Add User"

**Option B: Firebase Auth Emulator (Recommended for Local Development)**
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Initialize emulator: `firebase init emulators`
3. Select "Authentication Emulator"
4. Start emulator: `firebase emulators:start`
5. Update `firebase-config.js` to use emulator:
   ```javascript
   firebase.initializeApp(firebaseConfig);
   auth.useEmulator('http://localhost:9099');
   ```

### 3. Start Your Backend

```bash
# From project root
./gradlew run
```

The backend should be running at `http://localhost:8080`

### 4. Serve the WebApp

This is a **standalone client application**. You have several options to run it:

**Option A: Python HTTP Server (Simplest)**
```bash
# From the webapp/static directory
cd webapp/static
python3 -m http.server 3000
```
Then open: `http://localhost:3000`

**Option B: Node.js HTTP Server**
```bash
# Install http-server globally
npm install -g http-server

# From the webapp/static directory
cd webapp/static
http-server -p 3000
```
Then open: `http://localhost:3000`

**Option C: VS Code Live Server Extension**
1. Install "Live Server" extension in VS Code
2. Right-click on `webapp/static/index.html`
3. Select "Open with Live Server"

**Option D: Open Directly in Browser**
Simply open `webapp/static/index.html` in your browser. Note: Some browsers may block Firebase SDK scripts when opening files directly due to CORS. Use one of the HTTP server options above if you encounter issues.

## Usage Flow

### Complete User Flow Test

1. **Login**: Click "Login with Email" and enter your Firebase credentials
2. **Check Profile**: Click "Check if Profile Exists" to see if you have a backend profile
3. **Create Profile**: If profile doesn't exist, click "Create Profile" and fill in:
   - First Name (required)
   - Date of Birth in format YYYY-MM-DD (required)
   - Gender: MALE, FEMALE, NON_BINARY, or OTHER (required)
4. **View Profile**: Click "Get My Profile" to see your complete profile data
5. **Update Profile**: Click "Update Profile" to add optional fields:
   - Last Name
   - Bio
   - Location (City, State)
6. **Upload Photos**:
   - Click "Upload Photo"
   - Select an image file
   - The app will automatically:
     - Request a presigned S3 URL from backend
     - Upload the file directly to S3
     - Confirm the upload with the backend
7. **List Photos**: Click "List My Photos" to see all uploaded photos
8. **Delete Photos**: Click "Delete" button on any photo to remove it

### API Response Viewer

All API interactions are logged in real-time in the "API Response" section at the bottom of the page. This shows:
- Timestamp
- Action type (INFO, SUCCESS, ERROR)
- Request/Response JSON data
- Error messages (if any)

## Architecture

This webapp is a **completely independent client** that:
- Runs on its own HTTP server (separate from the backend)
- Authenticates with Firebase directly (client-side)
- Makes authenticated REST API calls to `http://localhost:8080`
- Simulates exactly how a real mobile app or web frontend would work

### File Structure
```
webapp/
├── README.md                     # This file
└── static/
    ├── index.html                # Main HTML page
    ├── css/
    │   └── styles.css            # Styling
    └── js/
        ├── firebase-config.js    # Firebase configuration (YOU MUST EDIT THIS)
        └── app.js                # Main application logic
```

## API Endpoints Used

### User Profile Endpoints
- `GET /users/exists` - Check if profile exists
- `GET /users/me` - Get current user profile
- `POST /users` - Create user profile
- `PATCH /users/me` - Update user profile
- `DELETE /users/me` - Delete user account

### Photo Endpoints
- `GET /users/me/photos` - List all photos
- `POST /users/me/photos/presigned-url` - Get presigned S3 upload URL
- `POST /users/me/photos` - Confirm photo upload
- `DELETE /users/me/photos/{photoId}` - Delete a photo

All endpoints require Firebase authentication via `Authorization: Bearer <token>` header.

## Troubleshooting

### "Not authenticated" errors
- Make sure you've logged in with Firebase credentials
- Check that your Firebase config is correct in `firebase-config.js`
- Verify the Firebase token hasn't expired (tokens expire after 1 hour)

### "User profile not found" errors
- You need to create a profile first using "Create Profile"
- The backend user profile is separate from Firebase authentication

### Photo upload failures
- Verify S3 is configured in backend `application.yaml`
- Check S3 bucket permissions
- Ensure CORS is configured on the S3 bucket

### CORS errors
- Make sure your backend CORS configuration allows requests from your webapp origin
- Backend should allow `http://localhost:3000` (or whatever port you're using)
- Check `app/src/main/kotlin/HTTP.kt` for CORS settings

### Firebase SDK not loading
- Don't open `index.html` directly in browser (use an HTTP server)
- Check browser console for script loading errors
- Verify internet connection (Firebase SDK loads from CDN)

## Configuration

### Changing Backend URL

If your backend runs on a different port or host, update `API_BASE_URL` in `webapp/static/js/app.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080';  // Change this if needed
```

### Using Firebase Emulator

To use Firebase Auth Emulator instead of production Firebase, add this line in `firebase-config.js` after initializing Firebase:

```javascript
firebase.initializeApp(firebaseConfig);
auth.useEmulator('http://localhost:9099');  // Add this line
```

## Security Notes

⚠️ **This is a TEST CLIENT for LOCAL DEVELOPMENT only**

- No production security measures
- Firebase config exposed in client-side JavaScript (this is normal for Firebase)
- Intended only for testing and development workflows
- Should NOT be deployed to production as-is

## Why This Structure?

This webapp is **intentionally separate** from the backend to:
1. Simulate real-world client/server architecture
2. Test CORS and authentication flows properly
3. Demonstrate how a real frontend would consume your APIs
4. Allow frontend and backend to be developed/tested independently
5. Mimic how mobile apps or production web apps would interact with your backend

## Next Steps

After testing the user flow:
1. Verify all API endpoints work correctly
2. Test error cases (invalid data, missing fields, etc.)
3. Verify Firebase token validation
4. Test photo upload/delete flows
5. Check database persistence
6. Monitor API response times
7. Test CORS configuration
