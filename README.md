# registration-login-page

Account Studio is a Java 21 / Spring Boot 4.1 application with working registration, session login, a personal dashboard, and a paginated administrator directory. Includes a session-based JSON API.

## Run locally

```bash
./mvnw verify
./mvnw spring-boot:run
```

On Windows PowerShell use `.\mvnw.cmd verify` and `.\mvnw.cmd spring-boot:run`. Open http://localhost:8081.

## Security and persistence

- BCrypt (cost 12), email normalization, and database uniqueness constraints.
- CSRF protection on registration, login, and logout. Logout uses POST.
- Every public registration creates a USER; directory access requires ADMIN.
- Password hashes are never returned by the API or rendered in views.
- Local H2 database persists in ignored `data/`; Flyway applies versioned migrations.
- MySQL is supported through DB_URL, DB_USERNAME, and DB_PASSWORD environment variables. Use a dedicated new schema named `accounts` for this rework. Existing tutorial tables are not modified or automatically migrated.

## Administration

Register an account first, then an authorized database operator can explicitly grant administration:

```sql
UPDATE accounts SET role = 'ADMIN' WHERE email = 'your-verified-email@example.com';
```

There is no default administrator password or public role selection. Sign out and sign in after changing a role.

## Production configuration

Use HTTPS and set COOKIE_SECURE=true. Configure a dedicated database account, managed secrets, backups, and ingress rate limiting for /login, /register/save, and /api/register. This foundation does not include email verification, password recovery, or MFA; add those before exposing a sensitive production service.

The original Spring tutorial contained a database password in application.properties. That value has been removed from the working source but remains in historical commits. Rotate it if it is still used; repository history has not been rewritten.

See [API.md](API.md) for cookie and CSRF usage. `.env.example` is a template; Spring reads exported environment variables and does not load .env automatically.

## Development

Changes are checked by GitHub Actions. Keep credentials in environment variables and never commit `.env`, local databases, or build output.
