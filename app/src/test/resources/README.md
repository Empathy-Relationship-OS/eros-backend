# Test Resources

## test.json

This file contains **dummy test-only Firebase service account credentials**. These are **NOT real credentials**.

### Fake Values

- `project_id`: "test-project-id" (dummy value)
- `client_email`: "test@test-project-id.iam.gserviceaccount.com" (fake email)
- `private_key`: Generated test RSA key (not a real Firebase private key)

### Purpose

This file is used for integration tests that require a valid JSON structure for Firebase configuration, but do not actually connect to Firebase services. The credentials are intentionally fake and will not work with any real Firebase project.

### Security Note

Real Firebase service account files are excluded from version control via `.gitignore` patterns:
- `*-service-account.json`
- `firebase-service-account.json`

This test file is explicitly tracked in git for testing purposes only.
