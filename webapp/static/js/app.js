/**
 * Eros Backend Test App
 * Main JavaScript application file
 */

// Configuration
const API_BASE_URL = 'http://localhost:8080';

// State management
let currentUser = null;
let idToken = null;

// DOM Elements
const authStatus = document.getElementById('auth-status');
const loginBtn = document.getElementById('login-btn');
const logoutBtn = document.getElementById('logout-btn');
const profileSection = document.getElementById('profile-section');
const photoSection = document.getElementById('photo-section');
const apiResponse = document.getElementById('api-response');

// ============================================================================
// Authentication Functions
// ============================================================================

/**
 * Listen for authentication state changes
 */
auth.onAuthStateChanged(async (user) => {
    if (user) {
        currentUser = user;
        idToken = await user.getIdToken();
        updateAuthUI(true);
        displayApiResponse({ event: 'Auth State Changed', user: { uid: user.uid, email: user.email } }, 'info');
    } else {
        currentUser = null;
        idToken = null;
        updateAuthUI(false);
        displayApiResponse({ event: 'Auth State Changed', message: 'User logged out' }, 'info');
    }
});

/**
 * Update authentication UI based on login state
 */
function updateAuthUI(isAuthenticated) {
    if (isAuthenticated) {
        authStatus.textContent = `Authenticated as: ${currentUser.email}`;
        authStatus.className = 'authenticated';
        loginBtn.style.display = 'none';
        logoutBtn.style.display = 'inline-block';
        profileSection.style.display = 'block';
        photoSection.style.display = 'block';
    } else {
        authStatus.textContent = 'Not authenticated';
        authStatus.className = 'not-authenticated';
        loginBtn.style.display = 'inline-block';
        logoutBtn.style.display = 'none';
        profileSection.style.display = 'none';
        photoSection.style.display = 'none';
    }
}

/**
 * Show login prompt
 */
loginBtn.addEventListener('click', async () => {
    const email = prompt('Enter email:');
    const password = prompt('Enter password:');

    if (email && password) {
        try {
            displayApiResponse({ action: 'Login', email }, 'info');
            await auth.signInWithEmailAndPassword(email, password);
            displayApiResponse({ action: 'Login', status: 'Success' }, 'success');
        } catch (error) {
            displayApiResponse({ action: 'Login', error: error.message }, 'error');
            alert('Login failed: ' + error.message + '\n\nNote: If you need to create an account, use Firebase Console or Authentication Emulator.');
        }
    }
});

/**
 * Logout
 */
logoutBtn.addEventListener('click', async () => {
    try {
        await auth.signOut();
        displayApiResponse({ action: 'Logout', status: 'Success' }, 'success');
    } catch (error) {
        displayApiResponse({ action: 'Logout', error: error.message }, 'error');
    }
});

// ============================================================================
// API Helper Functions
// ============================================================================

/**
 * Make authenticated API request
 */
async function apiRequest(endpoint, options = {}) {
    if (!idToken) {
        throw new Error('Not authenticated. Please login first.');
    }

    const url = `${API_BASE_URL}${endpoint}`;
    const headers = {
        'Authorization': `Bearer ${idToken}`,
        'Content-Type': 'application/json',
        ...options.headers
    };

    const response = await fetch(url, {
        ...options,
        headers
    });

    const contentType = response.headers.get('content-type');
    let data;

    if (contentType && contentType.includes('application/json')) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        throw new Error(`API Error (${response.status}): ${JSON.stringify(data)}`);
    }

    return data;
}

/**
 * Display API response in the response section
 */
function displayApiResponse(data, type = 'info') {
    const timestamp = new Date().toLocaleTimeString();
    const formatted = JSON.stringify(data, null, 2);
    const className = type === 'error' ? 'error' : type === 'success' ? 'success' : 'info';

    apiResponse.innerHTML = `<span class="${className}">[${timestamp}] ${type.toUpperCase()}</span>\n${formatted}`;
}

// ============================================================================
// User Profile Functions
// ============================================================================

/**
 * Check if user profile exists
 */
document.getElementById('check-exists-btn').addEventListener('click', async () => {
    try {
        displayApiResponse({ action: 'Checking if profile exists...' }, 'info');
        const data = await apiRequest('/users/exists');
        document.getElementById('profile-exists').innerHTML = `
            <strong>Profile Exists:</strong> ${data.exists ? 'Yes' : 'No'}<br>
            <strong>User ID:</strong> ${data.userId}
        `;
        displayApiResponse(data, 'success');
    } catch (error) {
        displayApiResponse({ action: 'Check Profile Exists', error: error.message }, 'error');
    }
});

/**
 * Get current user's profile
 */
document.getElementById('get-profile-btn').addEventListener('click', async () => {
    try {
        displayApiResponse({ action: 'Fetching profile...' }, 'info');
        const data = await apiRequest('/users/me');
        document.getElementById('profile-data').innerHTML = `
            <strong>User ID:</strong> ${data.userId}<br>
            <strong>First Name:</strong> ${data.firstName}<br>
            <strong>Last Name:</strong> ${data.lastName || 'N/A'}<br>
            <strong>Date of Birth:</strong> ${data.dateOfBirth}<br>
            <strong>Gender:</strong> ${data.gender}<br>
            <strong>Bio:</strong> ${data.bio || 'N/A'}<br>
            <strong>Location:</strong> ${data.location ? `${data.location.city}, ${data.location.state}` : 'N/A'}<br>
            <strong>Preferences:</strong> ${data.preferences ? JSON.stringify(data.preferences) : 'N/A'}
        `;
        displayApiResponse(data, 'success');
    } catch (error) {
        displayApiResponse({ action: 'Get Profile', error: error.message }, 'error');
    }
});

/**
 * Create user profile
 */
document.getElementById('create-profile-btn').addEventListener('click', async () => {
    const firstName = prompt('First Name:');
    const dateOfBirth = prompt('Date of Birth (YYYY-MM-DD):');
    const gender = prompt('Gender (MALE/FEMALE/NON_BINARY/OTHER):');

    if (!firstName || !dateOfBirth || !gender) {
        alert('All fields are required');
        return;
    }

    const requestBody = {
        userId: currentUser.uid,
        firstName: firstName,
        dateOfBirth: dateOfBirth,
        gender: gender
    };

    try {
        displayApiResponse({ action: 'Creating profile...', request: requestBody }, 'info');
        const data = await apiRequest('/users', {
            method: 'POST',
            body: JSON.stringify(requestBody)
        });
        displayApiResponse({ action: 'Create Profile', response: data }, 'success');
        alert('Profile created successfully!');
    } catch (error) {
        displayApiResponse({ action: 'Create Profile', error: error.message }, 'error');
        alert('Failed to create profile: ' + error.message);
    }
});

/**
 * Update user profile
 */
document.getElementById('update-profile-btn').addEventListener('click', async () => {
    const lastName = prompt('Last Name (optional):');
    const bio = prompt('Bio (optional):');
    const city = prompt('City (optional):');
    const state = prompt('State (optional):');

    const requestBody = {};

    if (lastName) requestBody.lastName = lastName;
    if (bio) requestBody.bio = bio;
    if (city || state) {
        requestBody.location = {};
        if (city) requestBody.location.city = city;
        if (state) requestBody.location.state = state;
    }

    if (Object.keys(requestBody).length === 0) {
        alert('No fields to update');
        return;
    }

    try {
        displayApiResponse({ action: 'Updating profile...', request: requestBody }, 'info');
        const data = await apiRequest('/users/me', {
            method: 'PATCH',
            body: JSON.stringify(requestBody)
        });
        displayApiResponse({ action: 'Update Profile', response: data }, 'success');
        alert('Profile updated successfully!');
    } catch (error) {
        displayApiResponse({ action: 'Update Profile', error: error.message }, 'error');
        alert('Failed to update profile: ' + error.message);
    }
});

// ============================================================================
// Photo Management Functions
// ============================================================================

/**
 * List user's photos
 */
document.getElementById('list-photos-btn').addEventListener('click', async () => {
    try {
        displayApiResponse({ action: 'Fetching photos...' }, 'info');
        const data = await apiRequest('/users/me/photos');

        const photoListDiv = document.getElementById('photo-list');
        if (data.items && data.items.length > 0) {
            photoListDiv.innerHTML = '<h3>Your Photos</h3>';
            data.items.forEach(photo => {
                const photoDiv = document.createElement('div');
                photoDiv.className = 'photo-item';
                photoDiv.innerHTML = `
                    <img src="${photo.url}" alt="Photo ${photo.id}">
                    <div><strong>Photo ID:</strong> ${photo.id}</div>
                    <div><strong>Display Order:</strong> ${photo.displayOrder}</div>
                    <button onclick="deletePhoto(${photo.id})">Delete</button>
                `;
                photoListDiv.appendChild(photoDiv);
            });
        } else {
            photoListDiv.innerHTML = '<p>No photos uploaded yet.</p>';
        }

        displayApiResponse(data, 'success');
    } catch (error) {
        displayApiResponse({ action: 'List Photos', error: error.message }, 'error');
    }
});

/**
 * Delete a photo
 */
async function deletePhoto(photoId) {
    if (!confirm(`Are you sure you want to delete photo ${photoId}?`)) {
        return;
    }

    try {
        displayApiResponse({ action: 'Deleting photo...', photoId }, 'info');
        await apiRequest(`/users/me/photos/${photoId}`, {
            method: 'DELETE'
        });
        displayApiResponse({ action: 'Delete Photo', photoId, status: 'Success' }, 'success');
        alert('Photo deleted successfully!');
        // Refresh the photo list
        document.getElementById('list-photos-btn').click();
    } catch (error) {
        displayApiResponse({ action: 'Delete Photo', error: error.message }, 'error');
        alert('Failed to delete photo: ' + error.message);
    }
}

/**
 * Upload photo (two-step process: presigned URL + confirm)
 */
document.getElementById('upload-photo-btn').addEventListener('click', () => {
    document.getElementById('photo-file-input').click();
});

document.getElementById('photo-file-input').addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    try {
        // Step 1: Get presigned URL
        displayApiResponse({ action: 'Requesting presigned URL...', fileName: file.name, fileSize: file.size }, 'info');

        const presignedRequest = {
            fileName: file.name,
            contentType: file.type,
            fileSize: file.size
        };

        const presignedResponse = await apiRequest('/users/me/photos/presigned-url', {
            method: 'POST',
            body: JSON.stringify(presignedRequest)
        });

        displayApiResponse({ action: 'Got presigned URL', response: presignedResponse }, 'info');

        // Step 2: Upload to S3
        displayApiResponse({ action: 'Uploading to S3...', url: presignedResponse.uploadUrl }, 'info');

        const uploadResponse = await fetch(presignedResponse.uploadUrl, {
            method: 'PUT',
            body: file,
            headers: {
                'Content-Type': file.type
            }
        });

        if (!uploadResponse.ok) {
            throw new Error(`S3 upload failed: ${uploadResponse.status}`);
        }

        displayApiResponse({ action: 'Uploaded to S3', status: 'Success' }, 'success');

        // Step 3: Confirm upload
        displayApiResponse({ action: 'Confirming upload with backend...' }, 'info');

        const confirmRequest = {
            s3Key: presignedResponse.s3Key,
            fileName: file.name,
            contentType: file.type,
            fileSize: file.size
        };

        const confirmResponse = await apiRequest('/users/me/photos', {
            method: 'POST',
            body: JSON.stringify(confirmRequest)
        });

        displayApiResponse({ action: 'Upload Complete', response: confirmResponse }, 'success');
        alert('Photo uploaded successfully!');

        // Refresh the photo list
        document.getElementById('list-photos-btn').click();

        // Clear file input
        event.target.value = '';

    } catch (error) {
        displayApiResponse({ action: 'Upload Photo', error: error.message }, 'error');
        alert('Failed to upload photo: ' + error.message);
        event.target.value = '';
    }
});

// ============================================================================
// Initialize
// ============================================================================

displayApiResponse({
    message: 'Eros Backend Test App Ready',
    note: 'Please login to begin testing',
    apiBaseUrl: API_BASE_URL
}, 'info');
