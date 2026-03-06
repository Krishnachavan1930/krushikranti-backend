# KrushiKranti Backend

> REST API backend for the KrushiKranti Agricultural Marketplace Platform.

---

## Tech Stack

| Technology         | Version   |
|--------------------|-----------|
| Java               | 17        |
| Spring Boot        | 3.2.3     |
| Spring Security    | (bundled) |
| Spring Data JPA    | (bundled) |
| MySQL              | 8.x       |
| JJWT               | 0.12.5    |
| SpringDoc OpenAPI  | 2.3.0     |
| Maven              | 3.9+      |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.x running locally

### Database Setup

```sql
CREATE DATABASE krushikranti_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

> **Important:** Change `app.jwt.secret` to a secure random string before deploying to production.

### Run the Application

```bash
# From the backend/ directory
mvn spring-boot:run
```

The server starts on **http://localhost:8080**

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api/v1/api-docs
```

---

## API Endpoints Overview

### Authentication (`/api/v1/auth`)

| Method | Endpoint    | Description      | Auth Required |
|--------|-------------|------------------|---------------|
| POST   | /register   | Register user    | No            |
| POST   | /login      | Login            | No            |

### Products (`/api/v1/products`)

| Method | Endpoint             | Description          | Auth Required    |
|--------|----------------------|----------------------|------------------|
| GET    | /                    | Get all products     | No               |
| GET    | /{id}                | Get product by ID    | No               |
| GET    | /farmer/{farmerId}   | Farmer's products    | FARMER / ADMIN   |
| POST   | /                    | Create product       | FARMER           |
| PUT    | /{id}                | Update product       | FARMER / ADMIN   |
| DELETE | /{id}                | Delete product       | FARMER / ADMIN   |

### Users (`/api/v1/users`)

| Method | Endpoint  | Description          | Auth Required |
|--------|-----------|----------------------|---------------|
| GET    | /me       | Current user profile | Any logged-in |
| GET    | /         | All users            | ADMIN         |
| DELETE | /{id}     | Delete user          | ADMIN         |

---

## Roles

| Role         | Description                     |
|--------------|---------------------------------|
| ROLE_FARMER  | Can list, manage their products |
| ROLE_BUYER   | Can browse and order products   |
| ROLE_AGENT   | Intermediary between parties    |
| ROLE_ADMIN   | Full platform administration    |

---

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/krushikranti/
│   │   │   ├── KrushiKrantiApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   └── CorsConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Order.java
│   │   │   │   └── OrderItem.java
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```
