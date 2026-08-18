<div align="center">

# 🏥 SmartPharma — Backend API

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

</div>

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Domain Modules](#-domain-modules)
- [Multi-Tenancy & Security](#-multi-tenancy--security)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [Environment Variables](#-environment-variables)
- [Available Commands](#-available-commands)
- [Folder Structure](#-folder-structure)
- [Related Repositories](#-related-repositories)

---

## 📖 Project Overview

The **SmartPharma backend** is the Spring Boot REST API powering the pharmacy management system — handling authentication, inventory, sales, purchasing, demand forecasting, notifications, and reporting for the [web frontend](https://github.com/amer-rouby/smartpharma-frontend) and [mobile app](https://github.com/amer-rouby/smartpharma-mobile). It is a **multi-tenant** system: every pharmacy's data is isolated by `pharmacy_id`, derived server-side from the authenticated user's JWT — never trusted from client input.

---

## 🛠 Tech Stack

| Layer | Technology |
|:------|:-----------|
| **Framework** | Spring Boot 3.2.0 |
| **Language** | Java 17 |
| **Database** | PostgreSQL (via Spring Data JPA / Hibernate 6) |
| **Security** | Spring Security + JWT (access + refresh tokens), TOTP-based 2FA |
| **Build** | Maven |
| **PDF generation** | Microsoft Playwright (headless browser rendering — chosen over library-based PDF generation for correct Arabic text shaping) |
| **Excel export** | Apache POI |
| **Mapping** | MapStruct |
| **API docs** | springdoc-openapi (Swagger UI) |
| **Email** | Spring Mail (SMTP) |
| **Messaging** | WhatsApp Cloud API integration |
| **Testing** | JUnit 5, Spring Boot Test, Spring Security Test |

---

## 🏗 Architecture

```
src/main/java/com/smartpharma/
├── config/            # Spring configuration (CORS, scheduling, payment gateway registry, platform backup job)
├── controller/         # REST controllers (~20 resources)
├── dto/               # Request/response DTOs
├── entity/            # JPA entities
├── repository/        # Spring Data JPA repositories
├── scheduler/         # @Scheduled jobs (demand-prediction generation, session cleanup)
├── security/          # JWT filter/service, 2FA (TOTP), rate limiting, platform-admin auth
├── service/           # Business logic interfaces
│   └── impl/          # Implementations
└── util/              # SecurityUtils (tenant/user context extraction) and helpers
```

### Architecture Principles

- **Layered architecture** — Controller → Service → Repository, DTOs at the boundary (entities never leave the service layer).
- **Tenant isolation by construction** — `SecurityUtils.getCurrentPharmacyId()` derives the acting pharmacy from the JWT on every request; endpoints never accept a client-supplied `pharmacyId` for authorization decisions.
- **`ddl-auto=update`** — schema evolves additively from JPA entity annotations; no Flyway migrations (kept disabled). New non-nullable columns on populated tables must ship with a default or be added nullable and backfilled.
- **Scheduled jobs** — a weekly job (re)generates demand forecasts per pharmacy with a rolling retention window; a daily job reconciles past predictions against actual sales for accuracy tracking.

---

## 📦 Domain Modules

| Module | Responsibilities |
|:-------|:------------------|
| **Auth** | Login, registration, refresh tokens, TOTP-based 2FA |
| **Products & Categories** | Catalog, barcode lookup, paginated/searchable listing |
| **Stock** | Batches (lot/expiry tracking), movements, manual adjustments, low-stock & expiry alerts |
| **Demand Prediction** | Moving-average forecasting with seasonality/trend factors, confidence scoring, accuracy tracking against real sales |
| **Sales** | POS transactions, prescription-image validation for prescription-only products, invoice numbering |
| **Purchasing** | Purchase orders (draft → approved → received), supplier management, email/WhatsApp delivery to suppliers |
| **Payments** | Multi-gateway payment recording, refund lifecycle |
| **Expenses** | Categorized operating expense tracking |
| **Reports** | Sales, financial, stock, and expiry reports with PDF/Excel export |
| **Notifications** | In-app + email notifications, per-user preferences (channels, quiet hours, language), bilingual (ar/en) content |
| **Users & Settings** | Role-based user management (ADMIN / MANAGER / PHARMACIST / VIEWER), per-pharmacy settings (currency, enabled payment methods, prescription policy) |
| **Backup** | Platform-operator-only whole-database backup/restore (separate auth key), self-service per-pharmacy data export |
| **Share Links** | Temporary, expiring links for sharing predictions/reports externally |

---

## 🔐 Multi-Tenancy & Security

| Layer | Mechanism |
|:------|:----------|
| **Tenant isolation** | Every tenant-scoped query filters by `pharmacy_id` derived from the JWT, never from request parameters |
| **Authentication** | JWT access + refresh tokens |
| **Two-factor auth** | Real TOTP (RFC 6238), not a stub |
| **Rate limiting** | IP-based limiting + account lockout on repeated failed logins |
| **Password/session** | Configurable session timeout, scheduled session cleanup |
| **Platform operations** | Whole-database backup/restore is gated by a separate `X-Platform-Admin-Key`, entirely outside the normal JWT/role system — no pharmacy admin, regardless of role, can reach it |
| **Secrets** | All secrets (DB credentials, JWT signing key, mail/WhatsApp credentials, platform admin key) are supplied via environment variables — nothing sensitive is committed |

---

## 📋 Prerequisites

| Requirement | Version |
|:------------|:--------|
| **JDK** | 17 |
| **Maven** | 3.9+ (or use the wrapper if present) |
| **PostgreSQL** | 14+ |

---

## 🚀 Installation & Setup

### 1️⃣ Clone the repository
```bash
git clone https://github.com/amer-rouby/smartpharma-backend.git
cd smartpharma-backend
```

### 2️⃣ Create the database
```sql
CREATE DATABASE smartpharma;
```
Tables are created/updated automatically on first run (`ddl-auto=update`) under the `smartpharma` schema.

### 3️⃣ Set required environment variables
See [Environment Variables](#-environment-variables) below. At minimum, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` must be set.

### 4️⃣ Run
```bash
mvn spring-boot:run
```
The API starts on `http://localhost:8081/api`.

### 5️⃣ Run tests
```bash
mvn test
```

---

## ⚙️ Environment Variables

| Variable | Required | Description |
|:---------|:---------|:-------------|
| `DB_URL` | ✅ | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/smartpharma` |
| `DB_USERNAME` | ✅ | Database user |
| `DB_PASSWORD` | ✅ | Database password |
| `JWT_SECRET` | ✅ | Base64 secret for signing JWTs — generate with `openssl rand -base64 64` |
| `CORS_ALLOWED_ORIGINS` | – | Comma-separated allowed frontend origins (defaults cover the web dev server and the mobile app's dev/Capacitor origins) |
| `PLATFORM_ADMIN_API_KEY` | – | Key required for whole-database backup/restore endpoints; unset means those endpoints refuse everyone |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | – | SMTP config for outgoing email (notifications, purchase orders to suppliers) |
| `WHATSAPP_PHONE_NUMBER_ID` / `WHATSAPP_ACCESS_TOKEN` | – | WhatsApp Cloud API credentials for sending purchase orders via WhatsApp |

None of these have committed defaults for secrets — a blank mail/WhatsApp config fails closed with a clear error rather than silently no-op'ing.

---

## 📜 Available Commands

| Command | Description |
|:--------|:------------|
| `mvn spring-boot:run` | Run the API locally |
| `mvn test` | Run the test suite |
| `mvn clean install` | Full build + test |
| `mvn package` | Build a runnable JAR |

API documentation (Swagger UI) is available at `/swagger-ui.html` once the app is running.

---

## 📁 Folder Structure

```
smartpharma-backend/
├── src/
│   ├── main/
│   │   ├── java/com/smartpharma/     # Application source (see Architecture above)
│   │   └── resources/
│   │       └── application.properties # Base config (reads secrets from env vars)
│   └── test/
│       ├── java/com/smartpharma/     # Unit + integration tests
│       └── resources/
│           └── application.properties # Test-only config (points at a local dev database)
├── uploads/                          # Runtime file storage (profile photos, prescription images) — gitignored
├── pom.xml
└── .gitignore
```

---

## 🔗 Related Repositories

- **Web frontend**: [smartpharma-frontend](https://github.com/amer-rouby/smartpharma-frontend) — Angular
- **Mobile app**: [smartpharma-mobile](https://github.com/amer-rouby/smartpharma-mobile) — Ionic + Angular

---

## 📄 License

This project is proprietary and protected by intellectual property rights.
