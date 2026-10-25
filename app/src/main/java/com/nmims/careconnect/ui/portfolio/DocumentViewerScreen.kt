package com.nmims.careconnect.ui.portfolio

/*
 * DocumentViewerScreen:
 * Displays full details and the stored image of a specific medical document:
 * - Document title and category chip
 * - High-resolution loaded bitmap preview from internal storage
 * - InfoRows for document title, category, date added, and notes
 * - Delete action button with ConfirmDialog that deletes both metadata and internal image file
 */

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.MedicalDocument
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.ConfirmDialog
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.InfoRow
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch

@Composable
fun DocumentViewerScreen(
    documentId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = PortfolioRepositoryProvider.get(context)
    val storageManager = repository.getStorageManager()

    var isLoading by remember { mutableStateOf(true) }
    var document by remember { mutableStateOf<MedicalDocument?>(null) }
    var documentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        val docs = repository.getDocuments()
        val doc = docs.find { it.id == documentId }
        document = doc
        if (doc != null && doc.storagePath.isNotBlank()) {
            documentBitmap = storageManager.loadBitmap(doc.storagePath)
        }
        isLoading = false
    }

    fun handleDelete() {
        coroutineScope.launch {
            val deleted = repository.deleteDocument(documentId)
            if (deleted) {
                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, "Failed to delete document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = document?.title ?: "Document Viewer",
                onBack = onNavigateBack,
                actions = {
                    if (document != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Document",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else {
            val doc = document
            if (doc == null) {
                EmptyStatePanel(
                    icon = Icons.Default.BrokenImage,
                    title = "Document Not Found",
                    message = "This document may have been deleted or moved.",
                    actionText = "Back to Documents",
                    onActionClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Image Display Card
                    val bitmap = documentBitmap
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        if (bitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = doc.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Image file preview unavailable",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Document Details
                    SectionCard(title = "Clinical File Metadata") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LabelChip(
                                text = doc.category.label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(label = "Document Title", value = doc.title)
                        InfoRow(label = "Date Added", value = DateUtils.formatWithDay(doc.addedOnMillis))
                        if (doc.notes.isNotBlank()) {
                            InfoRow(label = "Clinical Notes", value = doc.notes)
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }

        if (showDeleteDialog) {
            ConfirmDialog(
                title = "Delete Document",
                message = "Are you sure you want to permanently delete \"${document?.title ?: "this document"}\" and its image file?",
                confirmText = "Delete",
                onConfirm = { handleDelete() },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DocumentViewerScreenPreview() {
    CareConnectTheme {
        DocumentViewerScreen(documentId = "demo-id", onNavigateBack = {})
    }
}
