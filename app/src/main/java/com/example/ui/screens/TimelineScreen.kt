package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CapturedNotification
import com.example.ui.components.NotificationCard
import com.example.ui.viewmodel.NotificationViewModel

@Composable
fun TimelineScreen(
    notifications: List<CapturedNotification>,
    isAnyFilterActive: Boolean,
    viewModel: NotificationViewModel,
    onNotificationClick: (CapturedNotification) -> Unit,
    onAssignCategoryClick: (CapturedNotification) -> Unit,
    onClearFiltersClick: () -> Unit,
    onSeedSampleDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAnyFilterActive) Icons.Default.NotificationsOff else Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isAnyFilterActive)
                        stringResource(R.string.empty_search_title)
                    else
                        stringResource(R.string.empty_timeline_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isAnyFilterActive)
                        stringResource(R.string.empty_search_desc)
                    else
                        stringResource(R.string.empty_timeline_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isAnyFilterActive) {
                    Button(
                        onClick = onClearFiltersClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Limpar Filtros")
                    }
                } else {
                    Button(
                        onClick = onSeedSampleDataClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(R.string.btn_seed_sample_data))
                    }
                }
            }
        }
    } else {
        // Group notifications by dateKey
        val groupedByDate = remember(notifications) {
            notifications.groupBy { it.dateKey }
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("timeline_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            groupedByDate.forEach { (dateKey, itemsInDate) ->
                val firstTimestamp = itemsInDate.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                val headerTitle = viewModel.formatSectionHeader(dateKey, firstTimestamp)

                item(key = "header_$dateKey") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${itemsInDate.size} notificações",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                items(
                    items = itemsInDate,
                    key = { it.id }
                ) { notif ->
                    NotificationCard(
                        notification = notif,
                        onClick = { onNotificationClick(notif) },
                        onToggleBookmark = { viewModel.toggleBookmark(notif.id, notif.isBookmarked) },
                        onAssignCategoryClick = { onAssignCategoryClick(notif) }
                    )
                }
            }
        }
    }
}
