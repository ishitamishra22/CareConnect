package com.nmims.careconnect.ui.portfolio

/*
 * DocumentsScreen:
 * Displays a 2-column LazyVerticalGrid of uploaded medical documents:
 * - Thumbnails loaded asynchronously from internal storage via FileStorageManager.loadBitmap()
 * - Document title and category LabelChip
 * - Formatted date added
 * - FloatingActionButton "+" to upload new document (-> Screen.AddDocument)
 * - Tap on document card opens DocumentViewer (-> Screen.DocumentViewer(id))
 * - Empty state when no documents have been uploaded
 */

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.MedicalDocument
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.data.storage.FileStorageManager
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.patient.components.EmptyStatePanel
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DocumentsScreen(
    onNavigateToAddDocument: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = PortfolioRepositoryProvider.get(context)
    val storageManager = repository.getStorageManager()

    var isLoading by remember { mutableStateOf(true) }
    var documents by remember { mutableStateOf<List<MedicalDocument>>(emptyList()) }

    LaunchedEffect(Unit) {
        documents = repository.getDocuments()
        isLoading = false
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = "Medical Documents",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddDocument,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload Document"
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else if (documents.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Default.Description,
                title = "No Documents Uploaded",
                message = "Securely upload lab test reports, discharge summaries, insurance policies, and doctor prescriptions.",
                actionText = "Upload First Document",
                onActionClick = onNavigateToAddDocument,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    PatientHeroCard(
                        title = "${documents.size} Attached Medical Documents",
                        description = "Stored securely in private application storage. Tap any document to preview or manage.",
                        badgeText = "Clinical Files"
                    )
                }

                items(documents, key = { it.id }) { document ->
                    DocumentCard(
                        document = document,
                        storageManager = storageManager,
                        onClick = { onNavigateToViewer(document.id) }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: MedicalDocument,
    storageManager: FileStorageManager,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(true) }

    LaunchedEffect(document.storagePath) {
        coroutineScope.launch {
            if (document.storagePath.isNotBlank()) {
                val loaded = withContext(Dispatchers.IO) {
                    storageManager.loadBitmap(document.storagePath)
                }
                bitmap = loaded
            }
            isLoadingBitmap = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    if (isLoadingBitmap) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = document.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                LabelChip(
                    text = document.category.label,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateUtils.format(document.addedOnMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DocumentsScreenPreview() {
    CareConnectTheme {
        DocumentsScreen(
            onNavigateToAddDocument = {},
            onNavigateToViewer = {},
            onNavigateBack = {}
        )
    }
}
