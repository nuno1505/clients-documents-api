# Authentication and security configuration

The API uses stateless JWT with HS256. `POST /auth/login` is the public login endpoint; endpoints under `/api/v1/**` require `Authorization: Bearer <token>`. Tokens are valid for 15 minutes by default. Authentication and access failures are returned as `application/problem+json` (401 or 403).

## Development

Activate the `dev` profile (`SPRING_PROFILES_ACTIVE=dev`) and configure `AUTH_USERNAME`, `AUTH_PASSWORD_HASH`, and `JWT_SECRET` in the environment. The repository provides neither a default account nor a default JWT key. `AUTH_PASSWORD_HASH` must contain a BCrypt hash, never a plain-text password.

## Production

Activate `SPRING_PROFILES_ACTIVE=prod` and provide the following three variables through the environment's secret manager:

| Variable | Value |
|---|---|
| `AUTH_USERNAME` | In-memory username. |
| `AUTH_PASSWORD_HASH` | BCrypt password hash. |
| `JWT_SECRET` | Random secret of at least 32 bytes for HS256. |

Startup fails if any variable is missing or empty, or if the JWT secret is too short. Do not put real values for these variables in the repository.

To generate a random JWT secret, you can use `openssl rand -base64 32`. To generate `AUTH_PASSWORD_HASH`, use Spring Security's `BCryptPasswordEncoder` or an internal secrets management tool that produces BCrypt hashes. Provide the values through environment variables or a secret manager; do not store them in the repository.

## Login

Request:

```http
POST /auth/login
Content-Type: application/json

{"username":"<AUTH_USERNAME>","password":"<configured-password>"}
```

`200` response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

Use the `accessToken` value in the `Authorization: Bearer <jwt>` header when calling `/api/v1/**`. Invalid credentials return `401` without including the password or internal details in the response.
