package com.smartledger.app.feature.orders.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder — Full implementation in Phase 7 */
@Composable
fun OrderListScreen(onNavigateToCreate: () -> Unit, onNavigateToDetail: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Orders — Phase 7", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Placeholder — Full implementation in Phase 7 */
@Composable
fun CreateOrderScreen(onOrderCreated: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Create Order — Phase 7", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Placeholder — Full implementation in Phase 7 */
@Composable
fun OrderDetailScreen(orderId: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Order Detail ($orderId) — Phase 7", style = MaterialTheme.typography.bodyLarge)
    }
}
