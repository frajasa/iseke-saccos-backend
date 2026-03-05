# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ISEKE is a SACCOS (Savings and Credit Cooperative Society) management system for Tanzania. It consists of a Spring Boot GraphQL backend (this repo) and a Next.js frontend (`E:\iseke-frontend`).

## Build & Run Commands

```bash
# Backend (this repo)
mvn compile                    # Compile
mvn spring-boot:run            # Run dev server (port 8080)
mvn test                       # Run tests
mvn clean package -DskipTests  # Build JAR

# Frontend (E:\iseke-frontend)
npm run dev                    # Dev server (port 3000)
npm run build                  # Production build
npm run lint                   # ESLint
```

**Prerequisites:** Java 21, Maven 3.5.6+, PostgreSQL on port 5454, database `saccos_db`.

## Architecture

### Backend (Spring Boot 3.5.6 + GraphQL)

**API:** GraphQL-first (not REST). Single endpoint at `/graphql`, IDE at `/graphiql`.

**Layered architecture:**
- `controller/` — GraphQL resolvers (`@Controller` + `@QueryMapping`/`@MutationMapping`). Despite the naming, `MemberController` is also a GraphQL resolver.
- `service/` — Business logic. Services handle validation, GL posting, audit logging.
- `repository/` — Spring Data JPA repositories extending `JpaRepository<Entity, UUID>`.
- `entity/` — JPA entities mapped to PostgreSQL tables. All use UUID primary keys.
- `dto/` — DTOs for complex query responses (reports, statements, dashboard stats).
- `inputs/` — GraphQL input types as Java classes with Lombok `@Data`.
- `enums/` — Shared enums used in entities, GraphQL schema, and frontend.
- `security/` — JWT auth: `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetailsService`.
- `exception/` — Custom exceptions (`BusinessException`, `ResourceNotFoundException`, etc.) with `GraphQLExceptionHandler`.

**GraphQL schema:** `src/main/resources/graphql/schema.graphqls` — single file defining all types, queries, mutations, enums, and inputs. Custom scalars: `Date`, `DateTime`, `Decimal`.

**Double-entry accounting:** Every financial transaction (deposit, withdrawal, loan disbursement, repayment) posts balanced debit/credit entries to the General Ledger via `AccountingService.postToGeneralLedger()`. Products (`SavingsProduct`, `LoanProduct`) link to GL accounts.

**Pagination pattern:** Queries returning paginated data use Spring's `Page<Entity>`. The resolver accepts `@Argument Integer page, @Argument Integer size`, defaults to page=0 size=20, creates `PageRequest.of(pageNumber, pageSize)`. The GraphQL schema uses `*Page` types (e.g., `MemberPage`, `LoanAccountPage`) with `content`, `totalElements`, `totalPages` fields.

**Auth:** JWT tokens (24h expiry), BCrypt passwords, role-based access (`ADMIN`, `MANAGER`, `CASHIER`, `LOAN_OFFICER`, `ACCOUNTANT`, `MEMBER`). Default admin: `admin`/`admin123`.

**ESS (Employee Self-Service) Module:** Member self-service portal. Members with `MEMBER` role + `linkedMemberId` on User entity access ESS endpoints.
- `EssResolver` — GraphQL endpoints for member dashboard, service requests, payroll deductions.
- `EssService` — Core ESS logic: dashboard aggregation, loan applications, withdrawal requests.
- `PayrollDeductionService` — Payroll batch processing, individual deduction execution.
- Entities: `Employer`, `EssServiceRequest`, `PayrollDeduction`, `PayrollDeductionBatch`.
- Workflow: Member submits request (PENDING) → Admin reviews (APPROVED/REJECTED) → Processing → Completed.
- Payroll: Admin sets up deductions per member/employer → Monthly batch processes all active deductions as real transactions.

**Payment Gateway Integration:** Mobile money and bank payment processing.
- `PaymentOrchestrationService` — Central payment coordinator, expiry scheduler.
- `PaymentGatewayRegistry` — Routes to provider-specific gateways.
- Gateways: `MpesaGateway` (Vodacom M-Pesa), `TigopesaGateway` (Tigopesa via secure.tigo.com OAuth2 + payment-auth API), `NmbBankGateway`.
- Entities: `PaymentRequest`, `PaymentProviderConfig`, `PaymentReconciliation`.
- Enums: `PaymentProvider` (MPESA, TIGOPESA, YAS, NMB_BANK, INTERNAL), `PaymentRequestStatus`, `PaymentDirection`.
- Config: `TigopesaProperties` (clientId, clientSecret, account, pin, merchantName, testMode).

### Frontend (Next.js 15 + React 19 + Apollo Client)

**Key directories:**
- `app/` — Next.js App Router pages. Route groups: `dashboard/`, `members/`, `loans/`, `savings/`, `branches/`, `users/`, `transactions/`.
- `lib/graphql/queries.ts` — All GraphQL queries and mutations (~1100 lines).
- `lib/types.ts` — TypeScript interfaces and enums mirroring the backend schema.
- `lib/utils.ts` — Utilities: `formatCurrency()` (TZS), `formatDate()`, `getStatusColor()`, `cn()`.
- `lib/error-utils.ts` — Error classification, `isNullListError()` for handling null list GraphQL errors.
- `lib/apollo-client.ts` — Apollo Client setup with JWT token injection and error handling.
- `lib/auth.ts` — NextAuth config with credentials provider calling backend GraphQL login.
- `components/` — Reusable UI: `Sidebar`, `AuthLayout`, `ErrorDisplay`, `Button`, `Card`.

**Auth:** NextAuth with JWT strategy, 1-hour sessions, middleware protects all routes except `/login`.

**Styling:** Tailwind CSS v4 with custom design system (Navy Blue primary #003B73, Green success #008751, Orange accent #FF6B35). Dark mode supported.

**Environment:** `NEXT_PUBLIC_GRAPHQL_URL` (default `http://localhost:8080/graphql`), `NEXTAUTH_SECRET`, `NEXTAUTH_URL`.

## Key Patterns to Follow

**Adding a new paginated list query:**
1. Repository: Add `Page<Entity> findByX(X x, Pageable pageable)` method
2. Service: Add method accepting `Pageable`, delegating to repository
3. Resolver: Add `@QueryMapping` with `@Argument Integer page, @Argument Integer size`, create `PageRequest`, call service
4. Schema: Add query to `type Query` and add `EntityPage` type with `content/totalElements/totalPages`
5. Frontend `queries.ts`: Add `GET_ENTITIES` gql query
6. Frontend `types.ts`: Add `EntityPage` interface
7. Frontend page: Follow `app/members/page.tsx` pattern (useState for page/filter, useQuery, table, pagination)

**Adding a new mutation:**
1. Add input class in `inputs/` with Lombok `@Data`
2. Add input type in `schema.graphqls`
3. Add service method with business logic
4. Add `@MutationMapping` in resolver
5. Add gql mutation in frontend `queries.ts`

**Error handling in frontend:** Wrap queries with `isNullListError()` check — the backend returns null for empty lists which GraphQL treats as an error for non-nullable list types. Use `ErrorDisplay` component for actual errors.

**Financial transactions:** Always post GL entries when money moves. Use existing patterns in `TransactionService` and `LoanAccountService.disburseLoan()`.

## Database

PostgreSQL with JPA `ddl-auto: none`. Migrations in `src/main/resources/db/migration/`. Schema setup SQL in project root (`chart_of_accounts_setup.sql`, `alter_tables_for_gl_accounts.sql`).

**Migration files (run manually via psql, no Flyway/Liquibase):**
- `V2__saccos_compliance_gaps.sql` — Password security, transaction reversals, GL links for savings products.
- `V3__payment_gateway_integration.sql` — `payment_provider_configs`, `payment_requests`, `payment_reconciliations` tables.
- `V4__ess_module.sql` — `employers`, `payroll_deductions`, `payroll_deduction_batches`, `ess_service_requests` tables. Adds `employer_id`/`employee_number`/`department` to `members`, `linked_member_id` to `users`.

**Connection:** `localhost:5454`, user `postgres`, database `saccos_db`, schema `public`.
