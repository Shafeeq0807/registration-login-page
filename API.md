# Session API

The JSON API and HTML forms use the same accounts, permissions, and sessions. No bearer token or JWT is issued. Requests must preserve the session cookie. CORS is not enabled; serve your client from the same origin.

| Method | Endpoint            | Access / result                                                                         |
| ------ | ------------------- | --------------------------------------------------------------------------------------- |
| GET    | `/api/csrf`         | Public; creates/reads session and returns `headerName` and `token`                      |
| POST   | `/api/register`     | JSON firstName, lastName, email, password; 201 with profile, 400 invalid, 409 duplicate |
| POST   | `/login`            | Form-urlencoded email and password; session login; redirects to dashboard on success    |
| GET    | `/api/me`           | Current profile; 401 if unauthenticated                                                 |
| GET    | `/api/users?page=0` | ADMIN only; 20 profiles per page                                                        |
| POST   | `/logout`           | Invalidates session; redirects to sign-in                                               |

All POST requests require the CSRF header returned by `/api/csrf`. Fetch a fresh token after login because authentication rotates the token. A missing/invalid token returns 403. Passwords are never returned. Example in a browser on the application origin:

```javascript
async function token() {
  return fetch("/api/csrf").then((response) => response.json());
}
const csrf = await token();
const response = await fetch("/api/register", {
  method: "POST",
  headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
  body: JSON.stringify({
    firstName: "Ada",
    lastName: "Lovelace",
    email: "ada@example.com",
    password: "Choose your own long passphrase",
  }),
});
// Check response.status before interpreting the result.
const loginToken = await token();
await fetch("/login", {
  method: "POST",
  headers: {
    "Content-Type": "application/x-www-form-urlencoded",
    [loginToken.headerName]: loginToken.token,
  },
  body: new URLSearchParams({
    email: "ada@example.com",
    password: "Choose your own long passphrase",
  }),
});
const profileResponse = await fetch("/api/me");
if (profileResponse.ok) console.log(await profileResponse.json());
```
