# LebVest Backend

A Spring Boot backend application managing transaction APIs for the LebVest platform (check my LebVest Frontend repo for more details) from individual account creation to managing payment workflows, project approvals, investment browsing and more.

## Table Of Contents

1. [Tech Stack](#tech-stack)
2. [Project Architecture](#project-architecture)
3. [Database Schema](#database-schema)
4. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Environment Variables](#environment-variables)
5. [Main Workflows](#main-workflows)
6. [API Documentation](#api-documentation)
7. [Testing](#testing)

## Tech Stack

### Core Technologies
- **Java 17** 
- **Maven 3.9.9**
- **Spring Boot 3.4.5**
- **Spring Security**
- **Spring Data JPA**
- **Hibernate**

### Database & Caching
- **MySQL 8.4.6**
- **Redis 8.1.5**

### Messaging & Integration
- **RabbitMQ**
- **Stripe**
- **AWS S3**
- **GreenMail** - Email testing (development)

## Project Architecture

### Overview

```
src/main/java/com/lebvest/
├── config/          # Configuration classes (Security, CORS, Redis, etc.)
├── controller/      # REST API endpoints
├── service/         # Business logic layer
├── model/           # Data models
│   ├── dto/         # Data Transfer Objects
│   ├── entities/    # JPA entities
│   ├── enums/       # Enumeration types
│   └── events/      # Event models
├── repository/      # Data access layer (JPA repositories)
├── filter/          # Security filters (JWT)
├── messaging/       # RabbitMQ message handlers
├── util/            # Utility classes
└── exception/       # Custom exception handlers
```

### Key Architectural Patterns

#### 1. **Profile-Based Configuration**
The application supports multiple profiles (dev, production) with different service implementations:
- **Production Profile**: Uses AWS S3 for file storage and SMTP for email
- **Dev Profile**: Uses local file storage and GreenMail for email testing


#### 2. **Security Architecture**
- JWT-based authentication with access and refresh tokens
- Role-based access control (ADMIN, INVESTOR, COMPANY)
- Method-level security
- CORS configuration for frontend integration

#### 3. **Asynchronous Processing**
- RabbitMQ queues for background jobs (file uploads, email sending)
- `Async methods for non-blocking operations
- WebSocket for real-time notifications

#### 4. **File Storage Strategy**
- **Production**: Files stored in AWS S3 with UUID-based paths
- **Development**: Files stored locally in `uploads/` directory
- File validation using Apache Tika for MIME type detection

## Database Schema

### Core Entities

#### User
- Base entity for all user types (Admin, Investor, Company)
- Fields: `id`, `email`, `password`, `name`, `roles`, `createdAt`
- Supports multiple roles per user

#### Investor
- Extends User with investor-specific data
- Fields: `portfolio_value`, `total_invested`, `total_returns`, `bio`, `imageUrl`, `kycVerified`, `kycStatus`
- Relationships:
  - One-to-One with `User`
  - One-to-One with `InvestorPreference`
  - One-to-Many with `InvestorInvestment`
  - One-to-Many with `Watchlist`
  - One-to-Many with `InvestorGoal`
  - One-to-Many with `InvestorNotification`

#### Company
- Represents companies seeking investment
- Fields: `name`, `description`, `logo`, `sector`, `foundedYear`, `location`, `status`
- Relationships:
  - One-to-One with `User`
  - One-to-One with `CompanyVerificationDocuments`
  - One-to-Many with `Investment`
  - One-to-Many with `CompanyTeamMember`
  - One-to-Many with `CompanyFinancial`
  - One-to-Many with `CompanyDocument`

#### Investment
- Represents investment opportunities posted by companies
- Fields: `title`, `description`, `category`, `riskLevel`, `expectedReturn`, `minInvestment`, `targetAmount`, `raisedAmount`, `status`
- Relationships:
  - Many-to-One with `Company`
  - One-to-Many with `InvestorInvestment`
  - One-to-Many with `InvestmentUpdate`
  - One-to-Many with `InvestmentTeamMember`
  - One-to-Many with `InvestmentDocument`

#### InvestorInvestment
- Links investors to their investments
- Fields: `amount`, `investedAt`, `currentValue`, `maturityDate`, `isMatured`, `payoutRequested`
- Relationships:
  - Many-to-One with `Investor`
  - Many-to-One with `Investment`

#### PayoutRequest
- Tracks payout requests from investors
- Fields: `amount`, `expectedReturn`, `status`, `evidenceUrl`, `adminNotes`
- Relationships:
  - Many-to-One with `Investor`
  - Many-to-One with `Investment`
  - Many-to-One with `Company`
  - One-to-One with `InvestorInvestment`

#### InvestmentRequest
- Investment requests submitted by investors (pending company approval)
- Fields: `amount`, `status`, `createdAt`, `acceptedAt`, `rejectedAt`
- Relationships:
  - Many-to-One with `Investor`
  - Many-to-One with `Investment`

### Status Enums

- **CompanyStatus**: `PENDING`, `APPROVED`, `REJECTED`, `VERIFIED`
- **InvestmentStatus**: `PENDING_REVIEW`, `ACTIVE`, `FUNDED`, `CLOSED`, `REJECTED`
- **PayoutStatus**: `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`
- **VerificationStatus**: `PENDING`, `APPROVED`, `REJECTED`
- **InvestmentRequestStatus**: `PENDING`, `ACCEPTED`, `REJECTED`

## Getting Started

### Prerequisites

1. **Java Development Kit (JDK)**
   - Version: 17 or higher (21 recommended)
   - Installation:
     ```bash
     # Fedora/RHEL
     sudo dnf install java-21-openjdk-devel
     
     # Ubuntu/Debian
     sudo apt-get install openjdk-17-jdk
     
     # macOS
     brew install openjdk@17
     ```

2. **Maven**
   - Version: 3.9.9 or higher
   - The project includes Maven Wrapper (`mvnw`), so Maven installation is optional

3. **MySQL**
   - Version: 8.4.6 or higher
   - Create a database named `lebvest` (or configure via environment variables)

4. **Redis** (Optional for development)
   - Version: 8.1.5 or higher
   - Required for caching and rate limiting

5. **RabbitMQ** (Optional for development)
   - Required for asynchronous message processing

### Environment Variables

The application uses environment variables for configuration. Create a `.env.dev` file for development or set environment variables for production.

#### Database Configuration
```bash
DB_HOST=localhost                    # Database host
DB_PORT=3306                        # Database port
DB_NAME=lebvest                     # Database name
DB_USERNAME=root                    # Database username
DB_PASSWORD=your_password           # Database password
DB_POOL_SIZE=5                      # Connection pool size (dev: 5, prod: 10)
DB_MIN_IDLE=2                      # Minimum idle connections
CREATE_DB_IF_NOT_EXIST=true        # Auto-create database (dev only)
DB_USE_SSL=false                    # Use SSL (dev: false, prod: true)
```

#### JWT Configuration
```bash
JWT_ACCESS_SECRET=your-very-long-and-secure-access-secret-key-minimum-256-bits
JWT_REFRESH_SECRET=your-very-long-and-secure-refresh-secret-key-minimum-256-bits
```

#### Email Configuration (Production - SMTP)
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

#### Email Configuration (Development - GreenMail)
```bash
MAIL_HOST=localhost
MAIL_PORT=3025
MAIL_USERNAME=dev@lebvest.local
MAIL_PASSWORD=devpassword
```

#### AWS S3 Configuration (Production)
```bash
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key
AWS_S3_BUCKET=lebvest-bucket
```

#### File Storage (Development)
```bash
UPLOADS_BASE_DIR=/path/to/uploads  # Local directory for file storage
```

#### Redis Configuration
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=                     # Optional
```

#### RabbitMQ Configuration
```bash
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

#### Stripe Configuration
```bash
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

#### Application Configuration
```bash
FRONTEND_URL=http://localhost:3000  # Frontend URL (dev: localhost:3000, prod: https://lebvest.com)
ADMIN_EMAIL=admin@lebvest.com       # Admin notification email
PORT=8080                            # Server port
SERVER_PORT=8080                     # Alternative server port variable
```

### Running the Application

#### Development Mode
```bash
# Set Java version (if multiple versions installed)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Activate dev profile
export SPRING_PROFILES_ACTIVE=dev

# Run with Maven
./mvnw spring-boot:run

# Or build and run
./mvnw clean package
java -jar target/lebvest-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### Production Mode
```bash
# Build the application
./mvnw clean package -DskipTests

# Run with production profile
java -jar target/lebvest-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

#### Creating Admin User
```bash
# Using the provided script
./scripts/create-admin.sh admin@lebvest.com admin123 "Admin User"

# Or manually using SQL
mysql -u root -p lebvest < scripts/create-admin.sql
```

## Main Workflows

### 1. Company Registration Workflow

1. **Initial Registration** (`POST /company-registration/register`)
   - Company submits basic information (name, email, sector, documents)
   - Documents are uploaded to `pending/` directory
   - Company status set to `PENDING`
   - Admin notification created

2. **Admin Review**
   - Admin reviews company registration request
   - Admin accepts (`POST /admin/accept-request`) or rejects (`PUT /admin/reject-request`)
   - If accepted: Documents moved from `pending/` to `accepted/`, status set to `APPROVED`
   - Email notifications sent to company

3. **Verification Documents Submission** (`POST /companies/me/verification`)
   - Company submits verification documents (legal docs, financials, etc.)
   - Status set to `PENDING` verification
   - Admin notification created

4. **Admin Verification Review**
   - Admin reviews verification documents
   - Admin approves (`POST /admin/approve-verification/{companyId}`) or rejects
   - If approved: Status set to `VERIFIED`, company can post investments

### 2. Investor Registration Workflow

1. **Registration** (`POST /investor-registration/register`)
   - Investor submits profile information, preferences, and KYC documents
   - At least one investment category and location preference required
   - Single risk tolerance level required
   - Documents validated using Apache Tika
   - Investor status set to `PENDING` KYC verification

2. **KYC Verification**
   - Admin reviews KYC documents
   - Admin approves (`POST /admin/approve-investor-verification/{investorId}`) or rejects
   - If approved: KYC status set to `APPROVED`, investor can make investments

### 3. Investment Workflow

1. **Company Creates Investment** (`POST /companies/me/investments`)
   - Company must be `VERIFIED` to post investments
   - Investment created with status `PENDING_REVIEW`
   - Admin notification created

2. **Admin Reviews Investment**
   - Admin reviews investment details
   - Admin approves (`POST /admin/projects/{id}/approve`) or rejects
   - If approved: Status set to `ACTIVE`, investment visible to investors

3. **Investor Makes Investment Request** (`POST /investments/{id}/invest`)
   - Investor submits investment request with amount
   - Investment request created with status `PENDING`
   - Company receives notification

4. **Company Reviews Investment Request**
   - Company views pending requests (`GET /companies/me/investment-requests`)
   - Company accepts (`POST /companies/me/investment-requests/{requestId}/accept`) or rejects
   - If accepted: Payment intent created via Stripe

5. **Payment Processing**
   - Investor completes payment via Stripe
   - Stripe webhook (`POST /payments/stripe/webhook`) processes payment confirmation
   - `InvestorInvestment` record created
   - Investment `raisedAmount` updated
   - Notifications sent to investor and company

### 4. Payout Workflow

1. **Investor Requests Payout** (`POST /api/investors/me/payouts/request/{investorInvestmentId}`)
   - Investment must be matured (`isMatured = true`)
   - Payout request created with status `PENDING`
   - Company receives notification

2. **Company Submits Evidence** (`POST /api/companies/me/payouts/{payoutRequestId}/submit-evidence`)
   - Company uploads payout evidence document
   - Status remains `PENDING` (awaiting admin review)

3. **Admin Reviews Payout**
   - Admin views payout requests (`GET /admin/payouts`)
   - Admin approves (`POST /admin/payouts/{payoutRequestId}/approve`) or rejects
   - If approved: Payout processed, `PayoutHistory` record created


## Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=InvestmentServiceTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Test Structure

Tests are located in `src/test/java/com/lebvest/` and include:

- **Service Tests**: Unit tests for business logic
- **Integration Tests**: Tests for API endpoints
- **Repository Tests**: Data access layer tests

### Test Configuration

Test configuration uses H2 in-memory database and is defined in `src/test/resources/application.yml`.

