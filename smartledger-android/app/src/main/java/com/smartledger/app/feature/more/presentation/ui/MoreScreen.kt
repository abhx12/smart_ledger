package com.smartledger.app.feature.more.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * "More" tab — Shows less-frequently used features:
 * Inventory, Production, Outsourcing, Workforce, Reports.
 * Placeholder — will be fully implemented in respective phases.
 */
@Composable
fun MoreScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToProduction: () -> Unit,
    onNavigateToOutsourcing: () -> Unit,
    onNavigateToWorkforce: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("More — Phases 5-12", style = MaterialTheme.typography.bodyLarge)
    }
}
