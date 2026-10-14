package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.AppSummaryTuple
import com.example.data.model.CustomCategory
import com.example.ui.viewmodel.DateFilterPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedPackage: String?,
    onSelectPackage: (String?) -> Unit,
    appSummaries: List<AppSummaryTuple>,
    datePreset: DateFilterPreset,
    onSelectDatePreset: (DateFilterPreset) -> Unit,
    onSetCustomDateBounds: (start: Long?, end: Long?) -> Unit,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    categories: List<CustomCategory>,
    isAnyFilterActive: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAppDialog by remember { mutableStateOf(false) }
    var showDatePresetDialog by remember { mutableStateOf(false) }
    var showCustomDateRangePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_field"),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("btn_clear_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar busca"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Filter Chip
            val currentAppName = if (selectedPackage != null) {
                appSummaries.firstOrNull { it.packageName == selectedPackage }?.appName
                    ?: selectedPackage.substringAfterLast('.')
            } else null

            FilterChip(
                selected = selectedPackage != null,
                onClick = { showAppDialog = true },
                label = {
                    Text(
                        text = currentAppName?.let { "App: $it" } ?: "Aplicativo",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = if (selectedPackage != null) {
                    {
                        IconButton(
                            onClick = { onSelectPackage(null) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remover filtro de app",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null,
                modifier = Modifier.testTag("filter_chip_app"),
                shape = RoundedCornerShape(10.dp)
            )

            // Date Filter Chip
            FilterChip(
                selected = datePreset != DateFilterPreset.ALL,
                onClick = { showDatePresetDialog = true },
                label = {
                    Text(
                        text = if (datePreset == DateFilterPreset.ALL) "Período" else datePreset.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = if (datePreset != DateFilterPreset.ALL) {
                    {
                        IconButton(
                            onClick = { onSelectDatePreset(DateFilterPreset.ALL) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remover filtro de data",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null,
                modifier = Modifier.testTag("filter_chip_date"),
                shape = RoundedCornerShape(10.dp)
            )

            // Category Filter Chip
            val currentCategory = categories.firstOrNull { it.id == selectedCategoryId }
            FilterChip(
                selected = selectedCategoryId != null,
                onClick = { showCategoryDialog = true },
                label = {
                    Text(
                        text = currentCategory?.let { "Cat: ${it.name}" } ?: "Categoria",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    if (currentCategory != null) {
                        val color = try {
                            Color(android.graphics.Color.parseColor(currentCategory.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                trailingIcon = if (selectedCategoryId != null) {
                    {
                        IconButton(
                            onClick = { onSelectCategory(null) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remover filtro de categoria",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null,
                modifier = Modifier.testTag("filter_chip_category"),
                shape = RoundedCornerShape(10.dp)
            )

            // Clear All Filters button
            if (isAnyFilterActive) {
                AssistChip(
                    onClick = onClearFilters,
                    label = {
                        Text(
                            text = "Limpar Filtros",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.testTag("btn_clear_all_filters"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }

    // App Selection Dialog
    if (showAppDialog) {
        AlertDialog(
            onDismissRequest = { showAppDialog = false },
            title = {
                Text(
                    text = "Filtrar por Aplicativo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPackage == null,
                                onClick = {
                                    onSelectPackage(null)
                                    showAppDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Todos os Aplicativos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedPackage == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    items(appSummaries) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPackage == app.packageName,
                                onClick = {
                                    onSelectPackage(app.packageName)
                                    showAppDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedPackage == app.packageName) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${app.count} notificações capturadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Date Preset Dialog
    if (showDatePresetDialog) {
        AlertDialog(
            onDismissRequest = { showDatePresetDialog = false },
            title = {
                Text(
                    text = "Intervalo de Datas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DateFilterPreset.values().forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = datePreset == preset,
                                onClick = {
                                    if (preset == DateFilterPreset.CUSTOM) {
                                        showDatePresetDialog = false
                                        showCustomDateRangePicker = true
                                    } else {
                                        onSelectDatePreset(preset)
                                        showDatePresetDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (datePreset == preset) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePresetDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Custom Date Range Picker Dialog
    if (showCustomDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showCustomDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null) {
                            // Ensure end covers whole day
                            val endOfDay = end?.let { it + (24 * 60 * 60 * 1000L - 1) } ?: (start + 24 * 60 * 60 * 1000L - 1)
                            onSetCustomDateBounds(start, endOfDay)
                        }
                        showCustomDateRangePicker = false
                    }
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateRangePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Selecione o Intervalo",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                modifier = Modifier.height(450.dp)
            )
        }
    }

    // Category Selection Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = {
                Text(
                    text = "Filtrar por Categoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == null,
                                onClick = {
                                    onSelectCategory(null)
                                    showCategoryDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Todas as Categorias",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    items(categories) { cat ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    onSelectCategory(cat.id)
                                    showCategoryDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(catColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}
