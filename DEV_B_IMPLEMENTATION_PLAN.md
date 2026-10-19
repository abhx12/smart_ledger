# SmartLedger — Developer B: Frontend Foundation Implementation Plan

**Branch:** `feature/dev-b/frontend-foundation`
**Developer:** B (Kotlin Android / Frontend)
**Developer A:** Spring Boot Java backend on `feature/dev-a/backend-foundation`
**Project:** SmartLedger — Business Visibility System for a Silk Warp Manufacturer

---

## Quick Context

SmartLedger is a digital business management system for a silk warp manufacturing business.
It replaces paper notebooks and tracks: raw silk purchases, inventory, customer orders,
production, outsourcing, payments, expenses, loans, chits, and a live financial dashboard.

**Dev B role:** Build the Kotlin Android client that consumes Dev A's REST API.

---

## Architecture

```
[Kotlin Android App]  <-->  HTTPS/REST  <-->  [Spring Boot API - Dev A]  <-->  [PostgreSQL]
```

The frontend NEVER touches the database directly.
All business logic, validations, and financial calculations live on the backend.

---

## Technology Stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| Platform | Android (minSdk 26 / Android 8.0+) |
| Architecture | MVVM + Clean Architecture |
| UI | Jetpack Compose |
| Navigation | Navigation Compose |
| HTTP Client | Retrofit 2 + OkHttp |
| JSON | Gson |
| Auth token storage | EncryptedSharedPreferences |
| Dependency Injection | Hilt |
| Async | Kotlin Coroutines + StateFlow |
| Build system | Gradle (Kotlin DSL) |

---

## Prerequisites — Installation Status

| Tool | Status | Action Required |
|---|---|---|
| Git 2.49 | INSTALLED | Nothing |
| Node.js v22 + npm 11 | INSTALLED | Nothing |
| Java JDK 17 | **MISSING** | Install (see instructions below) |
| Android Studio | **MISSING** | Install (see instructions below) |
| Android SDK | **MISSING** | Auto-installed with Android Studio |

### Install Java JDK 17

1. Go to: https://adoptium.net/temurin/releases/?version=17
2. Download: Windows x64 | JDK 17 | .msi package
3. Run installer — enable "Add to PATH" and "Set JAVA_HOME"
4. Verify: open PowerShell and run `java -version`
   - Expected: `openjdk version "17.x.x"`

### Install Android Studio

1. Go to: https://developer.android.com/studio
2. Download Android Studio (Hedgehog 2023.1.1 or newer)
3. Run installer — let it install Android SDK, API 35, AVD, Intel HAXM
4. After first launch, open SDK Manager and install:
   - SDK Platform: Android 14 (API 34) and Android 8 (API 26)
   - SDK Tools: Android Emulator, Android SDK Build-Tools
5. Create an Android Virtual Device (Pixel 6, API 34) for testing

---

## Project Setup

### Create the Android Project

Open Android Studio -> New Project -> Empty Activity (Jetpack Compose)

| Field | Value |
|---|---|
| Name | SmartLedger |
| Package name | com.smartledger.app |
| Save location | e:\smart_ledger\smart_ledger\smartledger-android |
| Language | Kotlin |
| Min SDK | API 26 |
| Build config | Kotlin DSL |

### Package Structure (Package-by-Feature)

```
com.smartledger.app/
  core/
    network/          <- Retrofit setup, interceptors
    auth/             <- Token storage, auth state
    di/               <- Hilt modules
    util/             <- Formatters, extensions
    ui/               <- Theme, shared composables
  feature/
    auth/             <- Login, splash screen
    dashboard/        <- Home dashboard
    parties/          <- Customer, supplier, manufacturer, worker
    procurement/      <- Purchases, material receipts
    inventory/        <- Stock summary, movements (read-only)
    orders/           <- Customer orders
    production/       <- Production batches
    outsourcing/      <- Outsourcing jobs
    workforce/        <- Work records
    finance/          <- Payments, expenses, accounts
    reports/          <- Business reports
  SmartLedgerApp.kt
```

Each feature has this internal structure:
```
feature/procurement/
  data/
    api/           <- Retrofit interface
    dto/           <- Request and Response data classes
    repository/    <- Repository implementation
  domain/
    ProcurementRepository.kt  <- Interface
  presentation/
    viewmodel/
    ui/            <- Composable screens
  di/              <- Hilt module for this feature
```

### Key Dependencies (app/build.gradle.kts)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.smartledger.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.smartledger.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug {
            // 10.0.2.2 = emulator alias for localhost (Dev A runs on port 8080)
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            buildConfigField("Boolean", "USE_MOCK", "true")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://api.smartledger.com/\"")
            buildConfigField("Boolean", "USE_MOCK", "false")
            isMinifyEnabled = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.security.crypto)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.splashscreen)
}
```

### Retrofit + Auth Interceptor Setup

NetworkModule.kt (Hilt singleton):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(tokenStorage: TokenStorage): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
```

AuthInterceptor.kt:
```kotlin
class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getAccessToken()
        val request = chain.request().newBuilder()
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        val response = chain.proceed(request)
        // TODO: on 401, attempt refresh then retry
        return response
    }
}
```

### Token Storage (ALWAYS use EncryptedSharedPreferences)

```kotlin
class TokenStorage @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "smartledger_secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun saveTokens(access: String, refresh: String) {
        prefs.edit().putString("access_token", access).putString("refresh_token", refresh).apply()
    }
    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun clearTokens() { prefs.edit().clear().apply() }
}
```

---

## API Contracts (Dev A's Backend)

Base URL: `http://10.0.2.2:8080` (emulator) / `https://api.smartledger.com` (production)
All endpoints prefixed: `/api/v1/`
Auth header: `Authorization: Bearer <JWT access token>`

### Auth
| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/auth/login | Login -> accessToken + refreshToken |
| POST | /api/v1/auth/refresh | Get new access token |

Login request: `{ "username": "owner", "password": "secret" }`
Login response: `{ "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }`

### Parties
| Method | Path | Purpose |
|---|---|---|
| GET | /api/v1/parties?type=CUSTOMER | List customers |
| GET | /api/v1/parties?type=SUPPLIER | List suppliers |
| GET | /api/v1/parties?type=MANUFACTURER | List manufacturers |
| GET | /api/v1/parties?type=WORKER | List workers |
| POST | /api/v1/parties | Create party |
| GET | /api/v1/parties/{id} | Get party |
| PATCH | /api/v1/parties/{id} | Update party |

### Purchases
| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/purchases | Create purchase |
| GET | /api/v1/purchases | List purchases |
| GET | /api/v1/purchases/{id} | Get purchase |
| POST | /api/v1/purchases/{id}/receipts | Record material receipt |

### Inventory (READ ONLY - never POST to /inventory/movements)
| Method | Path | Purpose |
|---|---|---|
| GET | /api/v1/inventory/summary | Current stock levels |
| GET | /api/v1/inventory/movements | Movement history |
| GET | /api/v1/inventory/external-wip | Material at manufacturers |

### Orders
| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/orders | Create order |
| GET | /api/v1/orders | List orders |
| GET | /api/v1/orders/{id} | Get order |
| PATCH | /api/v1/orders/{id} | Update order (fails if locked) |
| POST | /api/v1/orders/{id}/lock | Lock order |
| POST | /api/v1/orders/{id}/cancel | Cancel order |

### Production
| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/production-batches | Create batch |
| GET | /api/v1/production-batches/{id} | Get batch |
| POST | /api/v1/production-batches/{id}/start | Start production |
| POST | /api/v1/production-batches/{id}/complete | Complete production |

### Outsourcing
| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/outsourcing-jobs | Create job |
| GET | /api/v1/outsourcing-jobs/{id} | Get job |
| POST | /api/v1/outsourcing-jobs/{id}/issue | Issue material |
| POST | /api/v1/outsourcing-jobs/{id}/receive | Receive output |

### Finance
| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/payments | Record payment |
| GET | /api/v1/payments/{id} | Get payment |
| POST | /api/v1/expenses | Record expense |
| GET | /api/v1/expenses | List expenses |
| GET | /api/v1/finance/accounts | List accounts |
| POST | /api/v1/finance/transfers | Account transfer |

### Dashboard + Reports
| Method | Path | Purpose |
|---|---|---|
| GET | /api/v1/dashboard/summary | All KPIs |
| GET | /api/v1/reports/customer-outstanding | Receivables report |
| GET | /api/v1/reports/supplier-payables | Payables report |
| GET | /api/v1/reports/expenses | Expense report |

### Error Response Format (from Dev A)
```json
{
  "timestamp": "2026-10-19T17:00:00+05:30",
  "status": 409,
  "code": "INSUFFICIENT_INVENTORY",
  "message": "Only 42.500 kg of raw silk is available.",
  "path": "/api/v1/production-batches"
}
```

Important error codes and UI messages:
| Code | Status | Show in UI |
|---|---|---|
| RESOURCE_NOT_FOUND | 404 | "Item not found" |
| INSUFFICIENT_INVENTORY | 409 | "Not enough stock: X kg available" |
| ORDER_LOCKED | 409 | "Order is already in production" |
| INVALID_STATE_TRANSITION | 409 | "This action is not allowed now" |
| PAYMENT_ALLOCATION_EXCEEDED | 409 | "Allocation exceeds outstanding balance" |
| PURCHASE_RECEIPT_EXCEEDED | 409 | "Cannot receive more than ordered" |
| (401 Unauthorized) | 401 | Clear tokens, go to Login screen |

---

## Implementation Phases

### PHASE 1 — Foundation

Goal: Project builds, Hilt works, Retrofit connects, navigation runs.

Tasks:
- Create Android Studio project
- Set up package structure
- Add all Gradle dependencies
- Create SmartLedgerApp.kt with @HiltAndroidApp
- Create NetworkModule with Retrofit + OkHttp
- Create TokenStorage (EncryptedSharedPreferences)
- Create AuthInterceptor
- Define Material3 theme (colors, typography, shapes) in core/ui/theme/
- Create SmartLedgerNavHost with all routes defined
- Create BottomNavBar composable (5 main tabs)

---

### PHASE 2 — Authentication

Goal: Login works. Tokens stored. Splash auto-login.

Files to create:
```
feature/auth/
  data/api/AuthApiService.kt
  data/dto/LoginRequest.kt
  data/dto/LoginResponse.kt
  data/repository/AuthRepositoryImpl.kt
  domain/AuthRepository.kt
  presentation/viewmodel/LoginViewModel.kt
  presentation/ui/LoginScreen.kt
  presentation/ui/SplashScreen.kt
```

LoginScreen requirements:
- SmartLedger logo at top
- Username TextField
- Password TextField with show/hide toggle
- Login Button: disabled + CircularProgressIndicator during API call
- Error text area below button (show on failure)
- On success -> navigate to Dashboard

SplashScreen logic:
- Check TokenStorage.getAccessToken()
- If token exists -> navigate to Dashboard
- If no token -> navigate to Login

401 handling (in AuthInterceptor):
- Call POST /auth/refresh with refresh token
- On success -> save new access token -> retry original request
- On failure -> clearTokens() -> navigate to Login

---

### PHASE 3 — Dashboard

Goal: Business owner sees live financial position on home screen.

API: GET /api/v1/dashboard/summary

Expected response:
```json
{
  "cash": 125000.00,
  "bank": 850000.00,
  "rawSilkKg": 324.500,
  "customerOutstanding": 625000.00,
  "supplierPayable": 410000.00,
  "externalWipKg": 82.500,
  "pendingOrders": 12,
  "pendingProduction": 3,
  "pendingOutsourcing": 5
}
```

UI layout:
- Row 1: Cash card + Bank card (INR formatted)
- Row 2: Customer Outstanding + Supplier Payable (side by side)
- Row 3: Raw Silk kg + External WIP kg
- Row 4: Pending Orders count + Production count + Outsourcing count
- Pull-to-refresh on entire screen
- Shimmer loading while fetching

IMPORTANT: Dashboard is READ ONLY. Never allow writes from this screen.

---

### PHASE 4 — Party Management

Goal: Create and view customers, suppliers, manufacturers, workers.

Screens: PartyListScreen, CreatePartyScreen, PartyDetailScreen

PartyListScreen: tabbed by type (CUSTOMER / SUPPLIER / MANUFACTURER / WORKER), search bar

Key DTOs:
```kotlin
data class CreatePartyRequest(
    val name: String,
    val partyType: String,  // "CUSTOMER", "SUPPLIER", "MANUFACTURER", "WORKER"
    val phone: String?,
    val address: String?
)

data class PartyResponse(
    val id: String,
    val partyCode: String,  // Show "CUST-001", not the UUID
    val name: String,
    val partyType: String,
    val phone: String?,
    val address: String?,
    val status: String  // "ACTIVE" or "INACTIVE"
)
```

Rules:
- Phone is OPTIONAL — do not mark it required
- Show partyCode to users, never raw UUID
- Status badge: ACTIVE = green, INACTIVE = grey

---

### PHASE 5 — Procurement

Goal: Record silk purchases and material deliveries.

Screens: PurchaseListScreen, CreatePurchaseScreen, PurchaseDetailScreen, RecordReceiptScreen

Create Purchase form fields:
- Supplier (dropdown, type=SUPPLIER)
- Purchase date
- Ordered weight (kg) — must be > 0
- Rate per kg — must be > 0
- Payment type: CASH or CREDIT
- If CREDIT: credit days (>= 0)

Record Receipt form fields:
- Receipt date
- Received weight (kg)
- Accepted weight (kg)
- Rejected weight (kg)
- UI RULE: accepted + rejected must be <= received (validate before submit)
- IMPORTANT: Only accepted weight enters inventory. Show this to user.

Purchase status colors:
- PENDING_RECEIPT -> Orange
- PARTIALLY_RECEIVED -> Blue
- FULLY_RECEIVED -> Green
- CANCELLED -> Red

---

### PHASE 6 — Inventory (Read Only)

Goal: Show current stock. No manual stock edits.

CRITICAL: Do NOT create any button or form that POSTs to /api/v1/inventory/movements.
All stock changes happen via purchase receipts, production, outsourcing.

Screens:
- InventorySummaryScreen: Raw Silk / Kora Warp / Silk Warp / Vuda Warp / External WIP
- MovementHistoryScreen: timeline of all movements with dates

---

### PHASE 7 — Customer Orders

Goal: Create and track customer orders with multiple items.

Screens: OrderListScreen, CreateOrderScreen, OrderDetailScreen

Order has MULTIPLE ITEMS (dynamic add/remove in form):
Each item: product type, weight kg, rate per kg, warp specification text

Product types: SILK_WARP, KORA_WARP, VUDA_WARP

Locked order handling:
- If order is locked or status=IN_PROGRESS:
  - Disable edit
  - Show banner: "This order is in production and cannot be changed."
- Backend returns ORDER_LOCKED (409) if someone tries anyway

Order status colors:
- CREATED -> Grey
- CONFIRMED -> Blue
- IN_PROGRESS -> Orange
- COMPLETED -> Green
- CANCELLED -> Red

---

### PHASE 8 — Production

Goal: Create and complete in-house production batches.

Screens: ProductionListScreen, CreateProductionBatchScreen, ProductionDetailScreen, CompleteProductionScreen

Complete production form:
- Input weight (pre-filled from batch)
- Output weight (user enters)
- Wastage weight (user enters)
- Show visually: "Input = Output + Wastage"
- DISABLE submit if equation does not balance
- On INSUFFICIENT_INVENTORY: "Not enough raw silk (X kg needed, Y kg available)"

---

### PHASE 9 — Outsourcing

Goal: Track silk sent to external manufacturers and received back.

Screens: OutsourcingListScreen, CreateOutsourcingJobScreen, OutsourcingDetailScreen,
         IssueOutsourcingScreen, ReceiveOutsourcingScreen

Show business flow visually:
  Create Job -> Issue Material -> [At Manufacturer] -> Receive Output -> Completed

Status colors:
- CREATED -> Grey
- MATERIAL_ISSUED -> Blue
- PARTIALLY_RECEIVED -> Orange
- COMPLETED -> Green

---

### PHASE 10 — Workforce

Goal: Record daily work by workers.

Screens: WorkRecordListScreen, CreateWorkRecordScreen, WorkerPayableScreen

Work types:
- WARPING -> show quantity field (kg), hide hours
- ROLLING -> show hours field, hide quantity

Rate is stored historically per record.
Changing rate today does NOT change old records (by design).

---

### PHASE 11 — Finance

Goal: Record payments, expenses, account transfers.

Payments:
- RecordPaymentScreen: party, direction (IN/OUT), amount, method, account, date, allocations
- Show which orders/purchases the payment covers

Expenses:
- RecordExpenseScreen: category, amount, account, date, description
- ExpenseListScreen

Accounts:
- AccountListScreen
- AccountTransferScreen

Finance rules:
- Payment amount > 0
- Allocation total <= payment amount
- Allocation total <= outstanding
- Transfer source != destination
- Account transfer is NOT revenue or expense (show distinction in UI)
- Always show confirmation dialog before submitting financial operations

---

### PHASE 12 — Reports

Screens:
- CustomerOutstandingReport: per customer, per order
- SupplierPayablesReport: overdue rows highlighted
- ExpenseSummaryReport: by category + total
- InventoryMovementReport: filter by date range

---

## Common Code Patterns

### UiState (use in every ViewModel)

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val code: String?, val message: String) : UiState<Nothing>()
}
```

### Standard ViewModel

```kotlin
@HiltViewModel
class PurchaseListViewModel @Inject constructor(
    private val repository: PurchaseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<PurchaseResponse>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    init { load() }
    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _uiState.value = UiState.Success(repository.getPurchases())
            } catch (e: HttpException) {
                val err = parseApiError(e.response()?.errorBody())
                _uiState.value = UiState.Error(err?.code, err?.message ?: "Request failed")
            } catch (e: IOException) {
                _uiState.value = UiState.Error(null, "No network connection")
            }
        }
    }
}
```

### API Error Parser

```kotlin
data class ApiError(val status: Int, val code: String, val message: String, val path: String)

fun parseApiError(errorBody: ResponseBody?): ApiError? = try {
    Gson().fromJson(errorBody?.string(), ApiError::class.java)
} catch (e: Exception) { null }
```

### Indian Currency Formatter

```kotlin
fun Double.toIndianRupee(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(this)
}
// 125000.0.toIndianRupee() -> Rs.1,25,000.00
```

### Weight Formatter

```kotlin
fun Double.toWeightString(): String = "%.3f kg".format(this)
// 324.5.toWeightString() -> 324.500 kg
```

---

## UI Rules (Apply to Every Screen)

1. Never show raw UUIDs. Always show business IDs like PUR-2026-0012, CUST-001.
2. Money uses Rs. symbol and Indian comma format: Rs.1,25,000.
3. Weights show 3 decimal places: 324.500 kg.
4. All lists have pull-to-refresh.
5. Submit button is disabled + shows spinner during API call (prevents duplicate submissions).
6. Show loading state (CircularProgressIndicator or shimmer) while fetching.
7. Show empty state with message + action button when lists are empty.
8. Display dates in dd/MM/yyyy format. Send dates to API in ISO YYYY-MM-DD format.
9. Show confirmation dialog before submitting financial operations.
10. Show "No internet connection" banner when network is unavailable.

---

## Security Rules

1. NEVER store JWT in plain SharedPreferences. Always use EncryptedSharedPreferences.
2. OkHttp logging interceptor MUST be disabled in release builds.
3. Logout clears both tokens and navigates to Login.
4. On 401: try refresh -> if ok, retry original. If refresh fails, clear tokens + go to Login.
5. No credentials hardcoded in source code ever.

---

## Working with Developer A

### What Requires Dev A to Finish First

| Your Feature | Needs from Dev A |
|---|---|
| Login | POST /auth/login |
| Dashboard | GET /dashboard/summary |
| Parties | GET/POST /parties |
| Procurement | GET/POST /purchases, POST /purchases/{id}/receipts |
| Inventory | GET /inventory/summary |
| Orders | GET/POST /orders |
| Production | GET/POST /production-batches, /start, /complete |
| Outsourcing | GET/POST /outsourcing-jobs, /issue, /receive |
| Workforce | GET/POST /work-records |
| Finance | GET/POST /payments, /expenses, /finance/accounts |
| Reports | GET /reports/* |

### Working in Parallel with Mock Data

Until Dev A finishes endpoints, use mock mode:
```kotlin
class PurchaseRepositoryImpl @Inject constructor(
    private val api: PurchaseApiService
) : PurchaseRepository {
    override suspend fun getPurchases(): List<PurchaseResponse> {
        if (BuildConfig.USE_MOCK) {
            return listOf(
                PurchaseResponse(
                    id = "mock-1",
                    purchaseNumber = "PUR-2026-0001",
                    supplierName = "ABC Silks",
                    orderedWeightKg = 250.0,
                    status = "PARTIALLY_RECEIVED",
                    purchaseDate = "2026-10-01"
                )
            )
        }
        return api.getPurchases()
    }
}
```

---

## Git Workflow

You are on branch: feature/dev-b/frontend-foundation
Only make changes in this branch. Do not touch main or feature/dev-a/backend-foundation.

Commit frequently with descriptive messages:
```
feat(auth): implement login screen with encrypted token storage
feat(dashboard): add summary cards with INR formatting and pull-to-refresh
feat(procurement): add purchase list and create purchase form
fix(orders): handle ORDER_LOCKED error with user-friendly message
chore(setup): configure Hilt and Retrofit modules
```

Format: type(module): description
Types: feat / fix / chore / refactor / docs

---

## What Dev A Is Building (Backend)

Tech: Spring Boot 3.x Java backend, PostgreSQL, Flyway migrations, Spring Security + JWT

Package structure: com.smartledger with modules:
  identity, parties, procurement, inventory, orders, production, outsourcing,
  workforce, finance, loans, chits, notifications, reporting

Key backend patterns:
- ACID transactions via @Transactional at application service level
- Pessimistic locking for inventory (prevents concurrent over-consumption)
- Standard error JSON with code + message fields
- API versioned at /api/v1/
- Business number IDs (PUR-2026-0001) alongside internal UUIDs

Database tables:
  parties, purchases, material_receipts, material_movements,
  customer_orders, order_items, production_batches, production_order_items,
  outsourcing_jobs, outsourcing_order_items, work_records,
  payments, customer_payment_allocations, supplier_payment_allocations,
  manufacturer_payment_allocations, financial_accounts, financial_transactions,
  account_transfers, expenses, loans, chits, documents, notifications

---

## Definition of Done

Foundation:
- [ ] Android Studio project builds successfully
- [ ] Hilt DI configured end-to-end
- [ ] Retrofit + AuthInterceptor working
- [ ] EncryptedSharedPreferences token storage working
- [ ] Material3 theme defined
- [ ] NavHost with all routes defined

Auth:
- [ ] Login screen (username + password + spinner)
- [ ] Splash screen with auto-login check
- [ ] JWT saved securely to EncryptedSharedPreferences
- [ ] 401 auto-refresh -> retry
- [ ] Logout clears tokens and goes to Login

Dashboard:
- [ ] All 9 KPIs displayed
- [ ] Indian currency and weight formatting
- [ ] Pull-to-refresh working

Parties:
- [ ] Tabbed list (Customer / Supplier / Manufacturer / Worker)
- [ ] Create party form (phone optional)
- [ ] Party detail screen

Procurement:
- [ ] Purchase list with status filter
- [ ] Create purchase form (CASH/CREDIT)
- [ ] Record material receipt
- [ ] Purchase detail with receipt history

Inventory:
- [ ] Stock summary (read-only)
- [ ] Movement history

Orders:
- [ ] Order list
- [ ] Create order with multiple items
- [ ] Locked order warning

Production:
- [ ] Create batch
- [ ] Start + Complete with equation validation

Outsourcing:
- [ ] Create job, issue material, receive output
- [ ] External WIP visible

Workforce:
- [ ] Create work record (WARPING kg / ROLLING hours)
- [ ] Worker payable screen

Finance:
- [ ] Record payment with allocations
- [ ] Record expense
- [ ] Account list + transfer

Reports:
- [ ] Customer outstanding report
- [ ] Supplier payables report

---

*Written by Antigravity AI for Developer B — SmartLedger project*
*Branch: feature/dev-b/frontend-foundation*
*Date: 2026-10-19*
