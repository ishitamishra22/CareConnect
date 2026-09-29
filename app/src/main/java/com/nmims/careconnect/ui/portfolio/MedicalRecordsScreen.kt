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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.MedicalRecord
import com.nmims.careconnect.data.model.RecordType
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
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
                title = "Medical History Records",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddRecord,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            EmptyState(
                icon = Icons.Default.HistoryEdu,
                message = "No medical records recorded yet.\nTap the + button below to log your first diagnosis, prescription, or lab result.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    RecordItemCard(
                        record = record,
                        onClick = { onNavigateToEditRecord(record.id) },
                        onDelete = { recordToDelete = record }
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        recordToDelete?.let { target ->
            ConfirmDialog(
                title = "Delete Medical Record",
                message = "Are you sure you want to delete '${target.title}'? This action cannot be undone.",
                confirmText = "Delete",
                onConfirm = {
                    if (!isDeleting) {
                        isDeleting = true
                        val idToDelete = target.id
                        coroutineScope.launch {
                            val deleted = repository.deleteRecord(idToDelete)
                            isDeleting = false
                            recordToDelete = null
                            if (deleted) {
                                Toast.makeText(context, "Record deleted", Toast.LENGTH_SHORT).show()
                                loadRecords()
                            } else {
                                Toast.makeText(context, "Could not reach the server. Check your internet connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onDismiss = { if (!isDeleting) recordToDelete = null }
            )
        }
    }
}

@Composable
private fun RecordItemCard(
    record: MedicalRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon: ImageVector, chipBg, chipText) = when (record.type) {
        RecordType.DIAGNOSIS -> Triple(
            Icons.Default.MedicalInformation,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        RecordType.PRESCRIPTION -> Triple(
            Icons.Default.Medication,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        RecordType.SURGERY -> Triple(
            Icons.Default.Healing,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        RecordType.VACCINATION -> Triple(
            Icons.Default.Vaccines,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        RecordType.LAB_RESULT -> Triple(
            Icons.Default.Biotech,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        RecordType.OTHER -> Triple(
            Icons.Default.LocalHospital,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chipBg.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = record.type.label,
                            tint = chipText,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    LabelChip(
                        text = record.type.label,
                        containerColor = chipBg,
                        contentColor = chipText
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Date row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = DateUtils.format(record.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val docHosp = listOf(record.doctorName, record.hospitalName)
                    .filter { it.isNotBlank() }
                    .joinToString(" @ ")
                if (docHosp.isNotBlank()) {
                    Text(
                        text = " • $docHosp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = record.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
