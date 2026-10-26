package com.nmims.careconnect.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nmims.careconnect.data.model.UserProfile
import com.nmims.careconnect.data.model.UserRole
import com.nmims.careconnect.data.repository.AdminRepository
import com.nmims.careconnect.ui.common.InitialsAvatar
import com.nmims.careconnect.ui.theme.LocalElevatedSurface
import kotlinx.coroutines.launch

/**
 * Screen listing all CareConnect users with search, role filters, and active/inactive status filters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    adminRepository: AdminRepository = AdminRepository.getInstance()
) {
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var selectedActiveFilter by remember { mutableStateOf<Boolean?>(null) } // null = all, true = active, false = inactive

    fun loadUsers() {
        isLoading = true
        coroutineScope.launch {
            val res = adminRepository.getUsers(
                roleFilter = selectedRoleFilter,
                activeFilter = selectedActiveFilter,
                searchQuery = searchQuery
            )
            users = res.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(selectedRoleFilter, selectedActiveFilter) {
        loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    loadUsers()
                },
                placeholder = { Text("Search by name, email, or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            loadUsers()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp)
            )

            // Role and Active Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedRoleFilter == null,
                        onClick = { selectedRoleFilter = null },
                        label = { Text("All Roles") }
                    )
                }
                items(UserRole.values()) { role ->
                    FilterChip(
                        selected = selectedRoleFilter == role,
                        onClick = { selectedRoleFilter = if (selectedRoleFilter == role) null else role },
                        label = { Text(role.label) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item {
                    FilterChip(
                        selected = selectedActiveFilter == true,
                        onClick = { selectedActiveFilter = if (selectedActiveFilter == true) null else true },
                        label = { Text("Active") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedActiveFilter == false,
                        onClick = { selectedActiveFilter = if (selectedActiveFilter == false) null else false },
                        label = { Text("Inactive") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No users found matching current filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(users, key = { it.uid }) { user ->
                        UserCard(
                            user = user,
                            onClick = { onUserClick(user.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserProfile,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(
                name = user.displayName.ifBlank { user.email },
                size = 44.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName.ifBlank { "User" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (user.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (user.active) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${user.role.label} • ${user.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
