## Authentication Flow

Register:

- `POST /api/auth/register`

Login:

- `POST /api/auth/login`

Current user:

```text
GET /api/auth/me
Authorization: Bearer <token>
```

Use the returned JWT token for protected endpoints.