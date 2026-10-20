package com.smartledger.app.feature.parties.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder — Full implementation in Phase 4 */
@Composable
fun PartyListScreen(onNavigateToCreate: () -> Unit, onNavigateToDetail: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Parties — Phase 4", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Placeholder — Full implementation in Phase 4 */
@Composable
fun CreatePartyScreen(onPartyCreated: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Create Party — Phase 4", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Placeholder — Full implementation in Phase 4 */
@Composable
fun PartyDetailScreen(partyId: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Party Detail ($partyId) — Phase 4", style = MaterialTheme.typography.bodyLarge)
    }
}
