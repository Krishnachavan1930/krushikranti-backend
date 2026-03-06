# KrushiKranti Backend Documentation

## 📋 Overview

KrushiKranti is an agricultural marketplace platform connecting farmers directly with consumers and wholesalers. The backend is built with **Spring Boot 3.4.3** and **Java 21**.

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Programming Language |
| **Spring Boot** | 3.4.3 | Application Framework |
| **Spring Security** | 6.x | Authentication & Authorization |
| **Spring Data JPA** | 3.x | ORM & Database Access |
| **MySQL** | 8.0 | Primary Database |
| **PostgreSQL** | 15+ | Alternative Database (optional) |
| **JWT (JJWT)** | 0.12.5 | Token-based Authentication |
| **Lombok** | 1.18.36 | Boilerplate Code Reduction |
| **SpringDoc OpenAPI** | 2.7.0 | API Documentation (Swagger) |
| **Cloudinary** | 1.36.0 | Image Upload & Storage |
| **Razorpay** | 1.4.6 | Payment Gateway |
| **JavaMailSender** | - | Email OTP Verification |

---

## 📁 Project Structure

```
backend/
├── pom.xml                          # Maven dependencies
├── start-backend.bat                # Windows startup script
├── mvnw.cmd                         # Maven wrapper
│
└── src/
    └── main/
        ├── java/com/krushikranti/
        │   │
        │   ├── KrushiKrantiApplication.java    # Main entry point
        │   │
        │   ├── config/                          # Configuration classes
        │   │   ├── SecurityConfig.java          # Spring Security config
        │   │   ├── CorsConfig.java              # CORS configuration
        │   │   └── CloudinaryConfig.java        # Cloudinary setup
        │   │
        │   ├── controller/                      # REST API endpoints
        │   │   ├── AuthController.java          # Auth endpoints
        │   │   ├── ProductController.java       # Product CRUD
        │   │   ├── OrderController.java         # Order management
        │   │   ├── PaymentController.java       # Razorpay integration
        │   │   ├── ChatController.java          # Real-time chat
        │   │   ├── BlogController.java          # Blog posts
        │   │   ├── UploadController.java        # Image uploads
        │   │   └── UserController.java          # User profile
        │   │
        │   ├── service/                         # Business logic
        │   │   ├── AuthService.java             # Registration, Login, OTP
        │   │   ├── ProductService.java          # Product operations
        │   │   ├── OrderService.java            # Order processing
        │   │   ├── PaymentService.java          # Payment handling
        │   │   ├── ChatService.java             # Chat functionality
        │   │   ├── BlogService.java             # Blog management
        │   │   ├── CloudinaryService.java       # Image uploads
        │   │   └── EmailService.java            # OTP email sending
        │   │
        │   ├── model/                           # JPA Entities
        │   │   ├── User.java                    # User entity
        │   │   ├── Product.java                 # Product entity
        │   │   ├── Order.java                   # Order entity
        │   │   ├── ChatMessage.java             # Chat message entity
        │   │   └── Blog.java                    # Blog entity
        │   │
        │   ├── repository/                      # Data access layer
        │   │   ├── UserRepository.java
        │   │   ├── ProductRepository.java
        │   │   ├── OrderRepository.java
        │   │   └── ...
        │   │
        │   ├── dto/                             # Data Transfer Objects
        │   │   ├── request/                     # Request DTOs
        │   │   │   ├── RegisterRequest.java
        │   │   │   ├── LoginRequest.java
        │   │   │   ├── OtpVerifyRequest.java
        │   │   │   └── ...
        │   │   └── response/                    # Response DTOs
        │   │       ├── ApiResponse.java
        │   │       ├── AuthResponse.java
        │   │       └── ...
        │   │
        │   ├── security/                        # Security components
        │   │   ├── JwtService.java              # JWT token handling
        │   │   ├── JwtAuthenticationFilter.java # Request filter
        │   │   └── UserDetailsServiceImpl.java  # User details loader
        │   │
        │   ├── exception/                       # Custom exceptions
        │   │   ├── GlobalExceptionHandler.java
        │   │   ├── ResourceNotFoundException.java
        │   │   ├── DuplicateResourceException.java
        │   │   └── ...
        │   │
        │   └── util/                            # Utility classes
        │
        └── resources/
            ├── application.properties           # MySQL config (default)
            ├── application-postgres.properties  # PostgreSQL config
            └── schema.sql                       # PostgreSQL schema
```

---

## 🔐 Authentication Flow

### 1. User Registration
```
POST /api/v1/auth/register
```
**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "Password123",
  "phone": "9876543210",
  "role": "ROLE_USER"
}
```
**Flow:**
1. Validate input (password: 8+ chars, 1 uppercase, 1 lowercase, 1 digit)
2. Check if email already exists
3. Generate 6-digit OTP
4. Save user with `isVerified = false`
5. Send OTP email asynchronously
6. Return success message

### 2. OTP Verification
```
POST /api/v1/auth/verify-otp
```
**Request Body:**
```json
{
  "email": "john@example.com",
  "otp": "123456"
}
```

### 3. Login
```
POST /api/v1/auth/login
```
**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "Password123"
}
```
**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "role": "ROLE_USER"
    }
  }
}
```

---

## 👥 User Roles

| Role | Permissions |
|------|-------------|
| `ROLE_USER` | Browse products, place orders, write reviews |
| `ROLE_FARMER` | List products, manage orders, view earnings |
| `ROLE_WHOLESALER` | Bulk orders, chat with farmers |
| `ROLE_ADMIN` | Full system access, user management |

---

## 📡 API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/register` | Register new user | ❌ |
| POST | `/verify-otp` | Verify email OTP | ❌ |
| POST | `/resend-otp` | Resend OTP | ❌ |
| POST | `/login` | User login | ❌ |
| GET | `/test-email` | Test email config | ❌ |

### Products (`/api/v1/products`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | List all products | ❌ |
| GET | `/{id}` | Get product details | ❌ |
| POST | `/` | Create product | 🔒 FARMER |
| PUT | `/{id}` | Update product | 🔒 FARMER |
| DELETE | `/{id}` | Delete product | 🔒 FARMER |
| GET | `/my-products` | Farmer's products | 🔒 FARMER |

### Orders (`/api/v1/orders`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create order | 🔒 USER |
| GET | `/user` | User's orders | 🔒 USER |
| GET | `/farmer` | Farmer's orders | 🔒 FARMER |
| PUT | `/{id}/status` | Update status | 🔒 FARMER |

### Payments (`/api/v1/payments`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/create-order` | Create Razorpay order | 🔒 |
| POST | `/verify` | Verify payment | 🔒 |

### Upload (`/api/upload`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/image` | Upload image | 🔒 |
| DELETE | `/{publicId}` | Delete image | 🔒 |

---

## 🗄️ Database Setup

### Option 1: MySQL (Default)

**Install MySQL 8.0:**
1. Download from https://dev.mysql.com/downloads/mysql/
2. Install and set root password

**Configuration (`application.properties`):**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/krushikranti_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

### Option 2: PostgreSQL

**Install PostgreSQL:**
1. Download from https://www.postgresql.org/download/
2. Create database: `CREATE DATABASE krushikranti_db;`
3. Run `schema.sql` to create tables

**Run with PostgreSQL profile:**
```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## 📧 Email Configuration (Gmail)

1. Enable 2-Step Verification in Google Account
2. Generate App Password:
   - Go to Google Account → Security → App passwords
   - Select "Mail" and generate

**Configuration:**
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 💳 Razorpay Configuration

1. Create account at https://razorpay.com
2. Get API keys from Dashboard → Settings → API Keys

**Configuration:**
```properties
razorpay.key.id=rzp_test_xxxxx
razorpay.key.secret=xxxxx
```

---

## 🖼️ Cloudinary Configuration

1. Create account at https://cloudinary.com
2. Get credentials from Dashboard

**Configuration:**
```properties
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret
```

---

## 🚀 Running the Backend

### Prerequisites
- Java 21 (JDK)
- Maven 3.9+
- MySQL 8.0 or PostgreSQL 15+

### Start Server
```bash
# Using batch file (Windows)
.\start-backend.bat

# Using Maven
.\mvnw.cmd spring-boot:run

# With PostgreSQL
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Access Points
- **API Base URL:** http://localhost:8080/api
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api/v1/api-docs

---

## 🔧 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MAIL_HOST` | SMTP host | smtp.gmail.com |
| `MAIL_PORT` | SMTP port | 587 |
| `MAIL_USERNAME` | Email address | - |
| `MAIL_PASSWORD` | App password | - |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud | - |
| `CLOUDINARY_API_KEY` | Cloudinary key | - |
| `CLOUDINARY_API_SECRET` | Cloudinary secret | - |
| `RAZORPAY_KEY_ID` | Razorpay key | - |
| `RAZORPAY_KEY_SECRET` | Razorpay secret | - |

---

## 📝 Password Requirements

- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit

**Example:** `Krishna123`, `Password@456`

---

## 🐛 Troubleshooting

### Port 8080 Already in Use
```powershell
Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### Database Connection Failed
1. Ensure MySQL/PostgreSQL is running
2. Verify credentials in `application.properties`
3. Check if database exists

### Email Not Sending
1. Use Gmail App Password (not regular password)
2. Check `spring.mail.username` and `spring.mail.password`
3. Test with: `GET /api/v1/auth/test-email?email=test@example.com`

---

## 📚 API Documentation

Interactive API documentation is available at:
```
http://localhost:8080/swagger-ui.html
```

---

*Last Updated: March 2026*
