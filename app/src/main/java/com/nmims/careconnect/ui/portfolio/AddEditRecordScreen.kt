package com.nmims.careconnect.ui.portfolio

/*
 * AddEditRecordScreen:
 * Lets the patient add a new medical history record or edit an existing one.
 * Includes:
 * - Record Type dropdown (Diagnosis, Prescription, Surgery, Vaccination, Lab Result, Other)
 * - Record Title (required)
 * - Date Picker Dialog (Material 3 DatePickerDialog, formatted date, cannot be in future)
 * - Doctor Name
 * - Hospital / Clinic Name
 * - Clinical Notes (multi-line)
 * - Save button (validates and invokes repository.addRecord / updateRecord)
 */

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.MedicalRecord
import com.nmims.careconnect.data.model.RecordType
import com.nmims.careconnect.data.repository.PortfolioRepositoryProvider
import com.nmims.careconnect.ui.common.CareTopBar
import com.nmims.careconnect.ui.common.DateUtils
import com.nmims.careconnect.ui.common.LoadingIndicator
import com.nmims.careconnect.ui.common.PrimaryButton
import com.nmims.careconnect.ui.common.SectionCard
import com.nmims.careconnect.ui.patient.components.PatientHeroCard
import com.nmims.careconnect.ui.theme.CareConnectTheme
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordScreen(
    recordId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = PortfolioRepositoryProvider.get(context)

    var isLoading by rememberSaveable { mutableStateOf(recordId != null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    var selectedType by rememberSaveable { mutableStateOf(RecordType.DIAGNOSIS) }
    var title by rememberSaveable { mutableStateOf("") }
    var dateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var doctorName by rememberSaveable { mutableStateOf("") }
    var hospitalName by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    var typeDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }

    // DatePicker State with future date restriction
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    // Load existing record if editing
    LaunchedEffect(recordId) {
        if (recordId != null) {
            val records = repository.getRecords()
            val existing = records.find { it.id == recordId }
            if (existing != null) {
                selectedType = existing.type
                title = existing.title
                dateMillis = existing.dateMillis
                doctorName = existing.doctorName
                hospitalName = existing.hospitalName
                notes = existing.notes
            }
            isLoading = false
        }
    }

    val trimmedTitle = title.trim()
    val isTitleValid = trimmedTitle.isNotBlank()

    fun handleSave() {
        if (!isTitleValid || isSaving) return
        isSaving = true

        val finalId = recordId ?: UUID.randomUUID().toString()
        val record = MedicalRecord(
            id = finalId,
            type = selectedType,
            title = trimmedTitle,
            dateMillis = dateMillis,
            doctorName = doctorName.trim(),
            hospitalName = hospitalName.trim(),
            notes = notes.trim()
        )

        coroutineScope.launch {
            val success = if (recordId == null) {
                repository.addRecord(record)
            } else {
                repository.updateRecord(record)
            }

            if (success) {
                Toast.makeText(
                    context,
                    if (recordId == null) "Medical record added" else "Medical record updated",
                    Toast.LENGTH_SHORT
                ).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, "Could not reach the server. Check your internet connection.", Toast.LENGTH_SHORT).show()
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            CareTopBar(
                title = if (recordId == null) "Add Medical Record" else "Edit Medical Record",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                PatientHeroCard(
                    title = if (recordId == null) "Clinical History Entry" else "Update Clinical Record",
                    description = "Accurate medical history facilitates precise specialist evaluation and automated admission triage.",
                    badgeText = "Health Record"
                )

                Spacer(modifier = Modifier.height(14.dp))

                SectionCard(title = "Record Details") {
                    // Record Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Record Type *") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            RecordType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title / Diagnosis *") },
                        placeholder = { Text("e.g. Annual Blood Sugar Screening") },
                        isError = title.isNotBlank() && !isTitleValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Date Picker Trigger Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true }
                    ) {
                        OutlinedTextField(
                            value = DateUtils.formatWithDay(dateMillis),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Date of Consultation / Record *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Pick Date",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionCard(title = "Healthcare Provider & Notes") {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Doctor / Specialist Name") },
                        placeholder = { Text("e.g. Dr. A. Sharma") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hospital / Clinic Name") },
                        placeholder = { Text("e.g. Apollo Hospital / Care Clinic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Clinical Notes & Prescriptions") },
                        placeholder = { Text("Enter doctor recommendations, dosage, or diagnostic results") },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = if (isSaving) "Saving..." else if (recordId == null) "Add Record" else "Save Changes",
                    onClick = { handleSave() },
                    enabled = isTitleValid && !isSaving
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // Material 3 Date Picker Dialog
        if (showDatePickerDialog) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selected ->
                                dateMillis = selected
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditRecordScreenPreview() {
    CareConnectTheme {
        AddEditRecordScreen(recordId = null, onNavigateBack = {})
    }
}
