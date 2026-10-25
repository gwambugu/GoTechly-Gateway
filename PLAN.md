# Angular Frontend Plan

## 1. Project Setup
- Install Angular CLI (latest version)
- Create new Angular project in `src/main/webapp` (standard Spring Boot frontend location)
- Use Angular 19+ with standalone components
- Add Angular Material for UI components
- Configure proxy to backend API at `/api`

## 2. Core Architecture (Updated)
```
src/main/webapp/
├── app/
│   ├── core/                    # Singleton services, guards, interceptors
│   │   ├── auth/                # AuthService, AuthGuard, AuthInterceptor
│   │   └── services/            # UserService, AccountService
│   ├── shared/                  # Shared components, pipes, directives
│   │   ├── components/          # Navbar, Footer
│   │   └── models/               # DTOs matching backend
│   ├── features/                # Feature modules (lazy loaded)
│   │   ├── account/             # Login, Register, Password (lazy loaded)
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   └── password/
│   │   └── default/             # Default user role pages
│   │       └── home/            # Home dashboard
│   ├── app.component.ts         # Root with router-outlet
│   ├── app.routes.ts            # Routing configuration
│   └── app.config.ts            # App configuration
```

## 3. Layout Structure
- **Navbar**: Logo, navigation links, login/logout buttons (responsive)
- **Main**: `<router-outlet>` for dynamic content
- **Footer**: Copyright, links

## 4. API Integration

| Backend Endpoint | Angular Service Method |
|------------------|------------------------|
| POST /api/authenticate | login(credentials) |
| POST /api/register | register(user) |
| GET /api/account | getAccount() |
| POST /api/account/reset-password/init | requestPasswordReset(email) |
| POST /api/account/reset-password/finish | finishPasswordReset(key, newPassword) |

## 5. Authentication Flow
- Store JWT in localStorage (or sessionStorage based on rememberMe)
- AuthInterceptor adds `Authorization: Bearer <token>` to requests
- AuthGuard protects home and other authenticated routes

## 6. Pages to Implement

1. **Landing Page** (`/`) - Public home page with branding
2. **Login Page** (`/login`) - Username, password, remember me, forgot password link
3. **Registration Page** (`/register`) - Login, email, password, confirm password, name fields
4. **Home Page** (`/home`) - Protected dashboard after login
5. **Forgot Password** (`/reset-password`) - Request password reset via email
6. **Reset Password Finish** (`/reset/finish`) - Enter new password with key

## 7. Design Approach
- Clean, modern UI with Angular Material components
- Responsive layout (mobile-friendly)
- Form validation with error messages
- Loading states and error handling
- JHipster-inspired structure for scalability

## 8. Backend API Contract (from analysis)

### LoginVM (POST /api/authenticate)
```json
{
  "username": "string",
  "password": "string",
  "rememberMe": boolean
}
```
Response: `{ "id_token": "jwt-token" }`

### ManagedUserVM (POST /api/register)
```json
{
  "login": "string",
  "email": "string",
  "password": "string",
  "firstName": "string",
  "lastName": "string"
}
```

### KeyAndPasswordVM (POST /api/account/reset-password/finish)
```json
{
  "key": "string",
  "newPassword": "string"
}
```

### AdminUserDTO (GET /api/account)
```json
{
  "id": number,
  "login": "string",
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "imageUrl": "string",
  "activated": boolean,
  "langKey": "string",
  "authorities": ["string"]
}
```
