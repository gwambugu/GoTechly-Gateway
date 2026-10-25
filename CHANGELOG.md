# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **Angular Frontend** - New Angular 19+ frontend application
  - Created project structure in `src/main/webapp`
  - Implemented lazy-loaded feature modules for scalability
  
- **Layout Components**
  - Navbar component with responsive design, authentication-aware navigation
  - Footer component with links and copyright
  
- **Account Module** (`/account/*`)
  - Login page with JWT authentication
  - Registration page with form validation
  - Password reset request page
  - Password reset finish page
  
- **Default Module** (`/default/*`)
  - Home/Dashboard page (protected route)
  
- **Landing Page** (`/`)
  - Public landing page with hero section and features

- **Core Services**
  - AuthService for JWT token management
  - AccountService for user API integration
  - AuthGuard for route protection
  - AuthInterceptor for automatic token injection

- **Styling**
  - Global SCSS theme with variables (`src/styles/_variables.scss`)
  - Professional design with responsive layouts
  - Custom color scheme (primary: #1976d2)

- **Maven Integration**
  - Added `frontend-maven-plugin` to build Angular during Maven build
  - Added `maven-resources-plugin` to package frontend in JAR
  - Frontend now included in `static/` folder of JAR

### Changed
- Updated project to use Angular standalone components
- Modified pom.xml to include frontend build process

### Configuration
- Added API routes:
  - `/` - Landing page
  - `/login` - Login page
  - `/register` - Registration page
  - `/reset-password` - Password reset request
  - `/reset/finish` - Password reset completion
  - `/home` - Protected dashboard (requires authentication)
