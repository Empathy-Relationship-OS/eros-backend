# Plugins Module

## Overview

The Plugins Module configures and manages Ktor plugins that provide cross-cutting functionality across the entire EROS backend application.

## Core Responsibilities

- **Authentication Middleware**: JWT validation and user context injection
- **Error Handling**: Global exception handling and error response formatting
- **Request Validation**: Input validation and sanitization
- **Logging**: Structured logging and request/response tracking
- **Monitoring**: Health checks, metrics, and performance monitoring
- **CORS Configuration**: Cross-origin resource sharing policies
- **Rate Limiting**: Request throttling and abuse prevention
- **Content Negotiation**: JSON serialization/deserialization

## Key Plugins Configured

### Security
- **Authentication**: JWT bearer token validation
- **CORS**: Configured for mobile and web clients
- **Rate Limiting**: Protection against abuse (10 req/min on auth endpoints)
- **Request Validation**: Input sanitization and validation

### Monitoring & Logging
- **Call Logging**: Request/response logging with correlation IDs
- **Status Pages**: Custom error pages and exception handlers
- **Metrics**: Performance monitoring and health endpoints

### Content & Headers
- **Content Negotiation**: JSON request/response handling via kotlinx.serialization
- **Default Headers**: Security headers (X-Frame-Options, X-Content-Type-Options, etc.)
- **Compression**: Response compression for bandwidth optimization

## Plugin Configuration Files

```
plugins/
├── Authentication.kt    - JWT setup and user principal extraction
├── ErrorHandling.kt     - Global exception handlers
├── Validation.kt        - Request validation configuration
├── Monitoring.kt        - Logging and metrics setup
├── Security.kt          - CORS, rate limiting, security headers
├── Serialization.kt     - JSON configuration
└── Routing.kt           - Route configuration and OpenAPI/Swagger setup
```

## Installation Order

Plugin installation order matters in Ktor. The current order is:
1. Serialization (Content Negotiation)
2. Administration (Rate Limiting)
3. HTTP (CORS, Headers)
4. Monitoring (Call Logging)
5. Security (Authentication)
6. Routing (must be last)

## Integration with Feature Modules

Feature modules interact with plugins through:
- Route-level authentication requirements
- Custom validators for domain models
- Exception throwing (handled by global error handler)
- Logging context injection

## API Documentation (OpenAPI/Swagger)

### Overview

The application uses **OpenAPI 3.0** specification with **Swagger UI** for interactive API documentation.

**Endpoints:**
- **Swagger UI**: `http://localhost:8080/swagger` - Interactive API documentation
- **OpenAPI Spec**: `http://localhost:8080/openapi` - HTML-rendered OpenAPI specification
- **Raw YAML**: `app/src/main/resources/openapi/documentation.yaml` - Source OpenAPI file

### Adding New Endpoints to API Documentation

When you add a new endpoint to the application, **you must also update the OpenAPI specification** to keep the documentation in sync.

#### Step 1: Add Your Route (As Usual)

Add your route in the appropriate module (e.g., `users/routes/UserRoutes.kt`):

```kotlin
fun Route.myNewRoutes(service: MyService) {
    get("/my-endpoint") {
        // Your implementation
        call.respond(HttpStatusCode.OK, result)
    }
}
```

Register it in `app/src/main/kotlin/Routing.kt`:

```kotlin
authenticate("firebase-auth") {
    // ... existing routes
    myNewRoutes(myService)
}
```

#### Step 2: Document in OpenAPI YAML

Open `app/src/main/resources/openapi/documentation.yaml` and add your endpoint definition:

```yaml
paths:
  /my-endpoint:
    get:
      tags:
        - My Feature      # Group endpoints by feature
      summary: Short description
      description: |
        Detailed description of what this endpoint does.
        Can be multi-line.
      parameters:        # Optional: for query params or path params
        - name: id
          in: query
          required: true
          schema:
            type: string
      requestBody:       # Optional: for POST/PATCH/PUT
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MyRequest'
      responses:
        '200':
          description: Success response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/MyResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
```

#### Step 3: Define Schemas (If Needed)

If your endpoint uses new request/response models, add them to `components.schemas`:

```yaml
components:
  schemas:
    MyRequest:
      type: object
      required:
        - field1
        - field2
      properties:
        field1:
          type: string
          description: Description of field1
        field2:
          type: integer
          minimum: 1

    MyResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        result:
          type: string
```

#### Step 4: Verify Documentation

1. Build and run the application:

   ```bash
   ./gradlew run
   ```

2. Open Swagger UI:

   ```text
   http://localhost:8080/swagger
   ```

3. Verify your new endpoint appears in the correct tag group

4. Test the endpoint directly from Swagger UI (if applicable)

### OpenAPI Best Practices

**DO:**
- ✅ Use meaningful tags to group related endpoints
- ✅ Add detailed descriptions for complex operations
- ✅ Document all possible response codes
- ✅ Use schema references (`$ref`) to avoid duplication
- ✅ Include examples for clarity
- ✅ Document authentication requirements

**DON'T:**
- ❌ Leave endpoints undocumented
- ❌ Duplicate schema definitions (use `$ref`)
- ❌ Skip error response documentation
- ❌ Forget to document query/path parameters

### Example: Complete Endpoint Documentation

```yaml
paths:
  /users/{id}/settings:
    patch:
      tags:
        - User Settings
      summary: Update user settings
      description: Updates specific user settings. Requires USER role or higher.
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: User ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                notifications:
                  type: boolean
                theme:
                  type: string
                  enum: [light, dark, auto]
            example:
              notifications: true
              theme: "dark"
      responses:
        '200':
          description: Settings updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserSettings'
        '400':
          description: Invalid settings format
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
```

### Checklist for New Endpoints

- [ ] Route implemented in feature module
- [ ] Route registered in `Routing.kt`
- [ ] Path added to `openapi/documentation.yaml`
- [ ] Request/response schemas defined
- [ ] All response codes documented (200, 400, 401, 403, 404, etc.)
- [ ] Parameters documented (path, query, body)
- [ ] Authentication requirements noted
- [ ] Tested in Swagger UI

### Regenerating Frontend Clients

After updating the OpenAPI spec, frontend teams can regenerate API clients:

```bash
# TypeScript/Axios
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/openapi \
  -g typescript-axios \
  -o ./frontend/src/api

# Other generators: typescript-fetch, javascript, python, java, etc.
```

---

**Status:** Core infrastructure - Configured during application bootstrap
