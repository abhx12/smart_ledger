package com.smartledger.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartledger.app.core.ui.components.BottomNavBar
import com.smartledger.app.core.ui.components.BottomNavItem
import com.smartledger.app.feature.auth.presentation.ui.LoginScreen
import com.smartledger.app.feature.auth.presentation.ui.SplashScreen
import com.smartledger.app.feature.dashboard.presentation.ui.DashboardScreen
import com.smartledger.app.feature.finance.presentation.ui.FinanceScreen
import com.smartledger.app.feature.more.presentation.ui.MoreScreen
import com.smartledger.app.feature.orders.presentation.ui.CreateOrderScreen
import com.smartledger.app.feature.orders.presentation.ui.OrderDetailScreen
import com.smartledger.app.feature.orders.presentation.ui.OrderListScreen
import com.smartledger.app.feature.parties.presentation.ui.CreatePartyScreen
import com.smartledger.app.feature.inventory.presentation.ui.InventorySummaryScreen
import com.smartledger.app.feature.inventory.presentation.ui.MovementHistoryScreen
import com.smartledger.app.feature.parties.presentation.ui.PartyDetailScreen
import com.smartledger.app.feature.parties.presentation.ui.PartyListScreen
import com.smartledger.app.feature.procurement.presentation.ui.CreatePurchaseScreen
import com.smartledger.app.feature.procurement.presentation.ui.PurchaseDetailScreen
import com.smartledger.app.feature.procurement.presentation.ui.PurchaseListScreen
import com.smartledger.app.feature.procurement.presentation.ui.RecordReceiptScreen


/**
 * Root NavHost for the entire SmartLedger app.
 *
 * Two logical zones:
 * 1. Auth zone (no bottom bar): Splash, Login
 * 2. Main zone (with bottom bar): Dashboard, Parties, Orders, Finance, More + sub-screens
 *
 * Navigation logic:
 * - Starts at Splash → determines auth state → routes to Login or Dashboard
 * - Bottom nav tabs use top-level destinations with saveState/restoreState
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes that should show the bottom navigation bar
    val bottomNavRoutes = BottomNavItem.entries.map { it.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: AppRoute.DASHBOARD,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination of the graph to avoid
                            // building up a large stack of destinations on the back stack
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when re-selecting
                            launchSingleTop = true
                            // Restore state when re-selecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Auth zone ──────────────────────────────────────────────────
            composable(AppRoute.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(AppRoute.LOGIN) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(AppRoute.DASHBOARD) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppRoute.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(AppRoute.DASHBOARD) {
                            popUpTo(AppRoute.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // ── Bottom Nav: Dashboard ──────────────────────────────────────
            composable(AppRoute.DASHBOARD) {
                DashboardScreen()
            }

            // ── Bottom Nav: Parties ────────────────────────────────────────
            composable(AppRoute.PARTIES) {
                PartyListScreen(
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_PARTY) },
                    onNavigateToDetail = { partyId -> navController.navigate(AppRoute.partyDetail(partyId)) }
                )
            }
            composable(AppRoute.CREATE_PARTY) {
                CreatePartyScreen(
                    onPartyCreated = { navController.popBackStack() }
                )
            }
            composable(AppRoute.PARTY_DETAIL) { backStackEntry ->
                val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
                PartyDetailScreen(
                    partyId = partyId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Bottom Nav: Orders ─────────────────────────────────────────
            composable(AppRoute.ORDERS) {
                OrderListScreen(
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_ORDER) },
                    onNavigateToDetail = { orderId -> navController.navigate(AppRoute.orderDetail(orderId)) }
                )
            }
            composable(AppRoute.CREATE_ORDER) {
                CreateOrderScreen(
                    onOrderCreated = { navController.popBackStack() }
                )
            }
            composable(AppRoute.ORDER_DETAIL) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Bottom Nav: Finance ────────────────────────────────────────
            composable(AppRoute.FINANCE) {
                FinanceScreen(
                    onNavigateToPayment = { navController.navigate(AppRoute.RECORD_PAYMENT) },
                    onNavigateToExpense = { navController.navigate(AppRoute.RECORD_EXPENSE) },
                    onNavigateToAccounts = { navController.navigate(AppRoute.ACCOUNT_LIST) }
                )
            }

            // ── Bottom Nav: More ───────────────────────────────────────────
            composable(AppRoute.MORE) {
                MoreScreen(
                    onNavigateToProcurement = { navController.navigate(AppRoute.PURCHASE_LIST) },
                    onNavigateToInventory = { navController.navigate(AppRoute.INVENTORY_SUMMARY) },
                    onNavigateToProduction = { navController.navigate(AppRoute.PRODUCTION_LIST) },
                    onNavigateToOutsourcing = { navController.navigate(AppRoute.OUTSOURCING_LIST) },
                    onNavigateToWorkforce = { navController.navigate(AppRoute.WORK_RECORD_LIST) },
                    onNavigateToReports = { navController.navigate(AppRoute.REPORTS) }
                )
            }

            // ── Phase 5: Silk Procurement ──────────────────────────────────
            composable(AppRoute.PURCHASE_LIST) {
                PurchaseListScreen(
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_PURCHASE) },
                    onNavigateToDetail = { purchaseId -> navController.navigate(AppRoute.purchaseDetail(purchaseId)) }
                )
            }
            composable(AppRoute.CREATE_PURCHASE) {
                CreatePurchaseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPurchaseCreated = { navController.popBackStack() }
                )
            }
            composable(AppRoute.PURCHASE_DETAIL) { backStackEntry ->
                val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: ""
                PurchaseDetailScreen(
                    purchaseId = purchaseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecordReceipt = { id -> navController.navigate(AppRoute.recordReceipt(id)) }
                )
            }
            composable(AppRoute.RECORD_RECEIPT) { backStackEntry ->
                val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: ""
                RecordReceiptScreen(
                    purchaseId = purchaseId,
                    onNavigateBack = { navController.popBackStack() },
                    onReceiptRecorded = { navController.popBackStack() }
                )
            }

            // ── Phase 6: Inventory & Material Stock (Read-Only) ────────────
            composable(AppRoute.INVENTORY_SUMMARY) {
                InventorySummaryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMovements = { navController.navigate(AppRoute.MOVEMENT_HISTORY) }
                )
            }
            composable(AppRoute.MOVEMENT_HISTORY) {
                MovementHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Sub-routes (added to graph so they can be navigated to) ───
            // These will be fully implemented in Phases 8-12
            composable(AppRoute.PRODUCTION_LIST) { /* Phase 8 */ }
            composable(AppRoute.OUTSOURCING_LIST) { /* Phase 9 */ }
            composable(AppRoute.WORK_RECORD_LIST) { /* Phase 10 */ }
            composable(AppRoute.RECORD_PAYMENT) { /* Phase 11 */ }
            composable(AppRoute.RECORD_EXPENSE) { /* Phase 11 */ }
            composable(AppRoute.ACCOUNT_LIST) { /* Phase 11 */ }
            composable(AppRoute.ACCOUNT_TRANSFER) { /* Phase 11 */ }
            composable(AppRoute.REPORTS) { /* Phase 12 */ }
        }
    }
}
