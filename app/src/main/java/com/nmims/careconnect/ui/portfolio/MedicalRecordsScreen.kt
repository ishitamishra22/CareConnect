package com.nmims.careconnect.ui.portfolio

/*
 * MedicalRecordsScreen:
 * Displays a list of the patient's medical records (diagnoses, prescriptions, surgeries, vaccinations, lab tests).
 * Features:
 * - Records sorted newest first
 * - Visual chips and icons per record type
 * - FloatingActionButton "+" to add a new record
 * - Tap on card to edit the record
 * - Delete action button with Material 3 ConfirmDialog
 * - Graceful EmptyState when no records exist
 */

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.MedicalRecord
import com.nmims.careconnect.data.model.RecordType
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.portfolio.components.MedicalRecordCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@Composable
fun MedicalRecordsScreen(
    onNavigateToAddRecord: () -> Unit,
    onNavigateToEditRecord: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = PortfolioRepositoryProvider.get(context)

    var isLoading by remember { mutableStateOf(true) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var recordToDelete by remember { mutableStateOf<MedicalRecord?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    fun loadRecords() {
        coroutineScope.launch {
            records = repository.getRecords()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRecords()
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Medical Records",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddRecord,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medical Record"
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else if (records.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Default.HistoryEdu,
                title = "No Medical Records",
                message = "Track your medical diagnoses, prescriptions, surgeries, vaccinations, and lab results here.",
                actionText = "Add Your First Record",
                onActionClick = onNavigateToAddRecord,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PatientHeroCard(
                        title = "${records.size} Clinical History Records",
                        description = "Your verified entries are securely stored and formatted for consultation and hospital admission summaries.",
                        badgeText = "Personal History"
                    )
                }

                items(records, key = { it.id }) { record ->
                    MedicalRecordCard(
                        record = record,
                        onClick = { onNavigateToEditRecord(record.id) },
                        onDelete = { recordToDelete = record }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        recordToDelete?.let { record ->
            ConfirmDialog(
                title = "Delete Medical Record",
                message = "Are you sure you want to permanently delete \"${record.title}\"? This action cannot be undone.",
                confirmText = "Delete",
                onConfirm = {
                    val toDelete = recordToDelete ?: return@ConfirmDialog
                    coroutineScope.launch {
                        isDeleting = true
                        val deleted = repository.deleteRecord(toDelete.id)
                        if (deleted) {
                            Toast.makeText(context, "Record removed", Toast.LENGTH_SHORT).show()
                            loadRecords()
                        } else {
                            Toast.makeText(context, "Failed to remove record", Toast.LENGTH_SHORT).show()
                        }
                        recordToDelete = null
                        isDeleting = false
                    }
                },
                onDismiss = { recordToDelete = null }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicalRecordsScreenPreview() {
    CareConnectTheme {
        MedicalRecordsScreen(
            onNavigateToAddRecord = {},
            onNavigateToEditRecord = {},
            onNavigateBack = {}
        )
    }
}
