package com.nmims.careconnect.ui.search

/*
 * HospitalDetailScreen:
 * Comprehensive view of an individual hospital's profile and services:
 * - CareTopBar with hospital name and back navigation
 * - Header card with location, contact, ratings, and implicit dialer intent (Intent.ACTION_DIAL)
 * - Specialties and Facilities sections
 * - Room types and daily tariffs table
 * - AndroidView WebView rendering the static aboutHtml content
 * - List of practicing specialist doctors at this hospital (tappable -> DoctorDetail)
 * - Verified patient reviews with ratings, dates, and comments
 * - Unified vertical scroll using LazyColumn with item blocks to prevent nested scroll conflicts
 */

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.Review
import com.nmims.careconnect.data.model.RoomType
import com.nmims.careconnect.data.repository.HospitalRepository
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.EmptyState
import com.nmims.careconnect.ui.common.LabelChip
import com.nmims.careconnect.ui.common.RatingBar
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.theme.CareConnectTheme

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.session.AppSession
import com.nmims.careconnect.ui.common.RatingInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HospitalDetailScreen(
    hospitalId: String,
    onNavigateBack: () -> Unit = {},
    onDoctorClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hospitalState by remember(hospitalId) { mutableStateOf(HospitalRepository.getHospital(hospitalId)) }
    val hospital = hospitalState

    if (hospital == null) {
        Scaffold(
            topBar = { CareTopBar(title = "Hospital Details", onBack = onNavigateBack) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.Default.LocalHospital,
                    message = "Hospital not found"
                )
            }
        }
        return
    }

    
    val doctors = remember(hospitalId) { HospitalRepository.getDoctorsForHospital(hospitalId) }
    var reviews by remember(hospitalId) { mutableStateOf(HospitalRepository.getReviewsFor(hospitalId)) }

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var isSubmittingReview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CareTopBar(
                title = hospital.name,
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hospital.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${hospital.area}, ${hospital.city}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (hospital.emergency24x7) {
                                LabelChip(
                                    text = "24x7 Emergency",
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = hospital.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingBar(rating = hospital.rating, size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${hospital.rating} (${hospital.reviewCount} reviews)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Call Button via Implicit ACTION_DIAL Intent
                            Button(
                                onClick = {
                                    val cleanPhone = hospital.phone.replace("\\s+".toRegex(), "")
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$cleanPhone")
                                    }
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Unable to launch phone dialer",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Specialties Section
            item {
                SectionCard(title = "Clinical Specialties") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hospital.specialties.forEach { specialty ->
                            LabelChip(
                                text = specialty,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Facilities Section
            item {
                SectionCard(title = "Hospital Facilities") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        hospital.facilities.forEach { facility ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = facility,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Room Types Table Section
            item {
                SectionCard(title = "Room Types & Tariffs") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Room Category",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = "Rate / Day",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2f)
                                )
                            }
                        }

                        // Room Rows
                        hospital.roomTypes.forEachIndexed { index, room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = "₹${room.pricePerDay}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = room.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(2f)
                                )
                            }
                            if (index < hospital.roomTypes.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }

            // About Hospital Section (WebView via AndroidView)
            item {
                SectionCard(title = "About this hospital") {
                    val styledHtml = remember(hospital.aboutHtml) {
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                color: #191C1D;
                                font-size: 13.5px;
                                line-height: 1.55;
                                margin: 0;
                                padding: 4px;
                            }
                            h2 {
                                color: #1F7A8C;
                                font-size: 16px;
                                margin-top: 0;
                                margin-bottom: 8px;
                            }
                            p {
                                margin-top: 0;
                                margin-bottom: 10px;
                            }
                            ul {
                                padding-left: 18px;
                                margin-top: 4px;
                                margin-bottom: 8px;
                            }
                            li {
                                margin-bottom: 4px;
                            }
                        </style>
                        </head>
                        <body>
                            ${hospital.aboutHtml}
                        </body>
                        </html>
                        """.trimIndent()
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                loadDataWithBaseURL(null, styledHtml, "text/html", "utf-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "utf-8", null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                }
            }

            // Doctors at this hospital
            item {
                Text(
                    text = "Doctors at this hospital",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (doctors.isEmpty()) {
                item {
                    Text(
                        text = "No doctors listed at this hospital currently.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(doctors, key = { it.id }) { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        hospital = hospital,
                        showCompareCheckbox = false,
                        onCardClick = { onDoctorClick(doctor.id) }
                    )
                }
            }

            // Patient Reviews Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Patient Reviews (${reviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (AppSession.currentRole == UserRole.PATIENT) {
                        OutlinedButton(
                            onClick = {
                                val myReview = reviews.firstOrNull { it.id == "${hospital.id}_${AppSession.currentPatientId}" }
                                reviewRating = myReview?.rating ?: 5
                                reviewComment = myReview?.comment ?: ""
                                showReviewDialog = true
                            }
                        ) {
                            Text("Write a review")
                        }
                    }
                }
            }

            items(reviews, key = { it.id }) { review ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = review.reviewerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = DateUtils.format(review.dateMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        RatingBar(rating = review.rating.toDouble(), size = 14.dp)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = review.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showReviewDialog) {
        val isCommentValid = reviewComment.trim().length in 10..200
        AlertDialog(
            onDismissRequest = {
                if (!isSubmittingReview) showReviewDialog = false
            },
            title = { Text("Write a review") },
            text = {
                Column {
                    Text(
                        text = "Your Rating:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingInput(
                        rating = reviewRating,
                        onRatingChange = { reviewRating = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { if (it.length <= 200) reviewComment = it },
                        label = { Text("Review comment") },
                        placeholder = { Text("Share your experience (10 - 200 characters)") },
                        supportingText = {
                            Text("${reviewComment.trim().length} / 200 characters")
                        },
                        isError = reviewComment.isNotBlank() && !isCommentValid,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        enabled = !isSubmittingReview
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isCommentValid || isSubmittingReview) return@Button
                        isSubmittingReview = true
                        coroutineScope.launch {
                            val newReview = Review(
                                id = "${hospital.id}_${AppSession.currentPatientId}",
                                targetId = hospital.id,
                                reviewerName = AppSession.currentUserName,
                                rating = reviewRating,
                                comment = reviewComment.trim(),
                                dateMillis = System.currentTimeMillis()
                            )
                            val success = HospitalRepository.addReview(newReview)
                            isSubmittingReview = false
                            if (success) {
                                Toast.makeText(context, "Review submitted", Toast.LENGTH_SHORT).show()
                                showReviewDialog = false
                                // Refresh screen state
                                hospitalState = HospitalRepository.getHospital(hospitalId)
                                reviews = HospitalRepository.getReviewsFor(hospitalId)
                            } else {
                                Toast.makeText(context, "Could not reach the server...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = isCommentValid && !isSubmittingReview
                ) {
                    if (isSubmittingReview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false },
                    enabled = !isSubmittingReview
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HospitalDetailScreenPreview() {
    CareConnectTheme {
        HospitalDetailScreen(hospitalId = "hosp_andheri")
    }
}
