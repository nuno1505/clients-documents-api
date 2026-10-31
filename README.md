# Clients API

REST API built with Java 21 and Spring Boot for managing clients and their documents. It includes CRUD operations, validation, JPA persistence with H2, Flyway migrations, JWT authentication, and OpenAPI documentation in the `dev` profile.

## Requirements

- JDK 21
- Maven 3.9 or later
- Docker (optional)

## Run locally

The application requires credentials and a JWT secret provided through the environment. There are no default credentials. The password hash must use BCrypt, and the secret must be at least 32 bytes for HS256.

PowerShell:

```powershell
# Generate a BCrypt password hash (replace this with the password you will use to log in).
mvn dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
$classpath = "target/classes;" + (Get-Content target/classpath.txt -Raw)
jshell --class-path $classpath
```

At the `jshell>` prompt, run the following expression and copy the result (starting with `$2a$` or `$2b$`) into `AUTH_PASSWORD_HASH`:

```java
new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("your-password")
```

Exit JShell with `/exit`. Then configure the environment and start the application:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:AUTH_USERNAME = 'api-user'
$env:AUTH_PASSWORD_HASH = '<bcrypt-hash>'
$env:JWT_SECRET = '<secret-with-at-least-32-bytes>'
mvn spring-boot:run
```

Complete example with demo credentials:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:AUTH_USERNAME = 'api-user'
$env:AUTH_PASSWORD_HASH = '$2a$10$sD2PjyUvyp2yEiRHudYoyOgei7DCTIo9gKm2B18TZXwYI1ie3SMQ6'
$env:JWT_SECRET = 'zs32rHW1Y4XIlc1vokumUKBCGO0fEc2z2urbcuen11Q='
mvn spring-boot:run
```

The BCrypt hash above corresponds to the password `password`. The JWT secret was generated randomly for this example. Since it is documented in the repository, use a different secret in any shared or production environment.

Generate and store the JWT secret outside the repository. `AUTH_PASSWORD_HASH` contains the BCrypt hash, while login requests use the corresponding original password.

## Authentication and requests

The public login endpoint is `POST /auth/login`:

```powershell
$body = @{ username = 'api-user'; password = '<original-password>' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/auth/login' -ContentType 'application/json' -Body $body
```

Send the returned `accessToken` in the `Authorization: Bearer <token>` header when calling `/api/v1/**` endpoints. For example:

```powershell
$headers = @{ Authorization = 'Bearer <token>' }
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/clients' -Headers $headers
```

HS256 tokens expire after 900 seconds by default. The user is configured in memory; this API does not include account management.

## Tests

Run the build and all tests with:

```powershell
mvn clean verify
```

Integration tests use Spring Boot, MockMvc, the actual security configuration, H2, and Flyway migrations.

## Swagger / OpenAPI

With the `dev` profile, open [Swagger UI](http://localhost:8080/swagger-ui/index.html) or view the [OpenAPI JSON](http://localhost:8080/v3/api-docs). The UI lets you authenticate protected requests with the token obtained from `/auth/login`. Swagger is disabled in the `prod` profile.

## Docker

Build and run the image, providing credentials to the container through environment variables:

```powershell
docker build -t clients-api:local .
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e "AUTH_USERNAME=$env:AUTH_USERNAME" `
  -e "AUTH_PASSWORD_HASH=$env:AUTH_PASSWORD_HASH" `
  -e "JWT_SECRET=$env:JWT_SECRET" `
  clients-api:local
```

The container runs as an unprivileged user. The configured database is in-memory H2, so data is lost when the application stops or restarts. See [security](docs/security.md), [architecture](docs/architecture.md), and [OpenAPI/Docker](docs/openapi-docker.md) for details.
