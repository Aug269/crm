package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CapturedNotification
import com.example.data.model.CustomCategory
import com.example.ui.components.CategoryEditDialog
import com.example.ui.components.FilterBar
import com.example.ui.components.NotificationDetailSheet
import com.example.ui.components.PermissionCard
import com.example.ui.components.TestNotificationDialog
import com.example.ui.viewmodel.NotificationViewModel
import com.example.util.NotificationAccessHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NotificationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // UI State from ViewModel
    val notifications by viewModel.filteredNotifications.collectAsStateWithLifecycle()
    val bookmarkedNotifications by viewModel.bookmarkedNotifications.collectAsStateWithLifecycle()
    val appSummaries by viewModel.appSummaries.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPackage by viewModel.selectedPackage.collectAsStateWithLifecycle()
    val datePreset by viewModel.datePreset.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val isAnyFilterActive by viewModel.isAnyFilterActive.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var selectedNotificationForDetail by remember { mutableStateOf<CapturedNotification?>(null) }
    var notificationToAssignCategory by remember { mutableStateOf<CapturedNotification?>(null) }
    var showTestNotificationDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Periodic / on-resume permission check
    LaunchedEffect(Unit) {
        viewModel.checkPermission()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isPermissionGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPermissionGranted)
                                        stringResource(R.string.permission_granted_badge)
                                    else
                                        stringResource(R.string.permission_missing_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPermissionGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Send Test Notification
                    IconButton(
                        onClick = { showTestNotificationDialog = true },
                        modifier = Modifier.testTag("btn_top_test_notification")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = stringResource(R.string.btn_test_notification),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Overflow Menu
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("btn_top_overflow_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.btn_seed_sample_data)) },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                viewModel.seedSampleData()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Configurar Permissão") },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                NotificationAccessHelper.openNotificationAccessSettings(context)
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.btn_clear_all),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                showClearConfirmationDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                // Tab 0: Por Data / Timeline
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.tab_timeline)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_timeline)) },
                    modifier = Modifier.testTag("nav_tab_timeline")
                )

                // Tab 1: Por App
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (appSummaries.isNotEmpty()) {
                                    Badge { Text("${appSummaries.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = stringResource(R.string.tab_apps)
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.tab_apps)) },
                    modifier = Modifier.testTag("nav_tab_apps")
                )

                // Tab 2: Categorias
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (categories.isNotEmpty()) {
                                    Badge { Text("${categories.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Categorias"
                            )
                        }
                    },
                    label = { Text("Categorias") },
                    modifier = Modifier.testTag("nav_tab_categories")
                )

                // Tab 3: Favoritos
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (bookmarkedNotifications.isNotEmpty()) {
                                    Badge { Text("${bookmarkedNotifications.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = stringResource(R.string.tab_bookmarks)
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.tab_bookmarks)) },
                    modifier = Modifier.testTag("nav_tab_bookmarks")
                )

                // Tab 4: Estatísticas
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.tab_stats)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_stats)) },
                    modifier = Modifier.testTag("nav_tab_stats")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner (visible if not granted)
            if (!isPermissionGranted) {
                PermissionCard(
                    onOpenSettings = {
                        NotificationAccessHelper.openNotificationAccessSettings(context)
                    },
                    onCheckAgain = {
                        viewModel.checkPermission()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Show FilterBar for Timeline (tab 0) and Bookmarks (tab 3)
            if (selectedTab == 0 || selectedTab == 3) {
                FilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedPackage = selectedPackage,
                    onSelectPackage = { viewModel.selectPackage(it) },
                    appSummaries = appSummaries,
                    datePreset = datePreset,
                    onSelectDatePreset = { viewModel.setDatePreset(it) },
                    onSetCustomDateBounds = { start, end -> viewModel.setCustomDateBounds(start, end) },
                    selectedCategoryId = selectedCategoryId,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    categories = categories,
                    isAnyFilterActive = isAnyFilterActive,
                    onClearFilters = { viewModel.clearFilters() }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    TimelineScreen(
                        notifications = notifications,
                        isAnyFilterActive = isAnyFilterActive,
                        viewModel = viewModel,
                        onNotificationClick = { selectedNotificationForDetail = it },
                        onAssignCategoryClick = { notificationToAssignCategory = it },
                        onClearFiltersClick = { viewModel.clearFilters() },
                        onSeedSampleDataClick = { viewModel.seedSampleData() }
                    )
                }

                1 -> {
                    AppsListScreen(
                        appSummaries = appSummaries,
                        onSelectApp = { pkg ->
                            viewModel.selectPackage(pkg)
                            viewModel.setSelectedTab(0)
                        },
                        onDeleteAppNotifications = { pkg ->
                            viewModel.deleteByPackage(pkg)
                        }
                    )
                }

                2 -> {
                    CategoriesScreen(
                        categories = categories,
                        categoryCounts = categoryCounts,
                        onSelectCategoryForFilter = { catId ->
                            viewModel.selectCategory(catId)
                            viewModel.setSelectedTab(0)
                        },
                        onCreateCategory = { name, color ->
                            viewModel.createCategory(name, color)
                        },
                        onUpdateCategory = { cat ->
                            viewModel.updateCategory(cat)
                        },
                        onDeleteCategory = { cat ->
                            viewModel.deleteCategory(cat)
                        }
                    )
                }

                3 -> {
                    // Bookmarks View
                    TimelineScreen(
                        notifications = bookmarkedNotifications,
                        isAnyFilterActive = false,
                        viewModel = viewModel,
                        onNotificationClick = { selectedNotificationForDetail = it },
                        onAssignCategoryClick = { notificationToAssignCategory = it },
                        onClearFiltersClick = { },
                        onSeedSampleDataClick = { viewModel.seedSampleData() }
                    )
                }

                4 -> {
                    StatsScreen(
                        totalCount = totalCount,
                        todayCount = todayCount,
                        appSummaries = appSummaries,
                        categories = categories,
                        categoryCounts = categoryCounts
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedNotificationForDetail?.let { notif ->
        NotificationDetailSheet(
            notification = notif,
            categories = categories,
            onDismiss = { selectedNotificationForDetail = null },
            onAssignCategory = { category ->
                viewModel.assignCategory(notif.id, category)
                selectedNotificationForDetail = notif.copy(
                    categoryId = category?.id,
                    categoryName = category?.name,
                    categoryColor = category?.colorHex
                )
            },
            onToggleBookmark = {
                viewModel.toggleBookmark(notif.id, notif.isBookmarked)
                selectedNotificationForDetail = notif.copy(isBookmarked = !notif.isBookmarked)
            },
            onDelete = {
                viewModel.deleteNotification(notif)
            },
            onCreateNewCategoryClick = {
                showCreateCategoryDialog = true
            }
        )
    }

    // Assign Category Sheet / Dialog directly from notification card
    notificationToAssignCategory?.let { notif ->
        AlertDialog(
            onDismissRequest = { notificationToAssignCategory = null },
            title = {
                Text(
                    text = "Atribuir Categoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Escolha uma categoria para a notificação de ${notif.appName}:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            viewModel.assignCategory(notif.id, null)
                            notificationToAssignCategory = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sem Categoria (Remover)")
                    }

                    categories.forEach { cat ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Button(
                            onClick = {
                                viewModel.assignCategory(notif.id, cat)
                                notificationToAssignCategory = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = catColor.copy(alpha = 0.15f),
                                contentColor = catColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(catColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    notificationToAssignCategory = null
                    showCreateCategoryDialog = true
                }) {
                    Text("+ Nova Categoria")
                }
            },
            dismissButton = {
                TextButton(onClick = { notificationToAssignCategory = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Create Category Dialog
    if (showCreateCategoryDialog) {
        CategoryEditDialog(
            initialCategory = null,
            onDismiss = { showCreateCategoryDialog = false },
            onSave = { name, color ->
                viewModel.createCategory(name, color)
            }
        )
    }

    // Test Notification Dialog
    if (showTestNotificationDialog) {
        TestNotificationDialog(
            onDismiss = { showTestNotificationDialog = false },
            onSend = { appName, title, message ->
                viewModel.sendTestNotification(appName, title, message)
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_clear_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.confirm_clear_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllNotifications()
                        showClearConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_confirm_clear_all")
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
