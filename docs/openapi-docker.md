# OpenAPI, Swagger UI, and Docker

## Swagger UI

Springdoc is configured for Spring Boot 3.5.x. Swagger UI and the OpenAPI document are enabled only with the `dev` profile:

- UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The `prod` profile and the base configuration keep both disabled. Spring Security allows anonymous access to the documentation routes when they are enabled. Client and document endpoints remain protected by JWT.

In Swagger UI:

1. Send `POST /auth/login` with `AUTH_USERNAME` and the password corresponding to the BCrypt hash configured in `AUTH_PASSWORD_HASH`.
2. Copy the `accessToken` field from the response.
3. Click **Authorize** and paste the token without the `Bearer ` prefix; Swagger UI applies the Bearer scheme.
4. Call the `/api/v1/**` endpoints. Login does not require authentication.

The operations describe the DTOs and their validation, HTTP status codes, and `ProblemDetail` errors (`application/problem+json`). The client and document controllers declare the Bearer scheme; the login controller remains public.

## Build the Docker image

The `Dockerfile` uses Maven with Java 21 to build the application and a Java 21 JRE image to run it. The runtime process uses an unprivileged user. H2 is in-memory, so neither Compose nor an additional database service is required.

```sh
docker build -t clients-api:local .
```

Run in development, passing the credentials and key through the environment (never in the Dockerfile or image):

```sh
docker run --rm --name clients-api -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e AUTH_USERNAME="$AUTH_USERNAME" \
  -e AUTH_PASSWORD_HASH="$AUTH_PASSWORD_HASH" \
  -e JWT_SECRET="$JWT_SECRET" \
  clients-api:local
```

The `AUTH_USERNAME`, `AUTH_PASSWORD_HASH`, and `JWT_SECRET` variables are required. The password hash must use BCrypt; the HS256 secret must be at least 32 bytes. For production, use `SPRING_PROFILES_ACTIVE=prod`; Swagger is disabled in that profile.
