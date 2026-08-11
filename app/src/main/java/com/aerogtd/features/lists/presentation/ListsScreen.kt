package com.aerogtd.features.lists.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerogtd.core.database.*
import com.aerogtd.ui.components.EmptyState
import com.aerogtd.ui.components.QuickCaptureSheet
import java.util.UUID

@Composable
fun ListsScreen(
    lists: List<CustomList>,
    appDatabase: AppDatabase,
    onListClick: (CustomList) -> Unit,
    onDeleteList: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Lists", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (lists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    message = "No lists yet.\nTap + to create one."
                )
            }
        }

        items(lists, key = { it.id }) { list ->
            val itemsList = remember(list.id) { appDatabase.customListItemDao().getCustomListItems(list.id) }
            val total = itemsList.size
            val done = itemsList.count { it.isCompleted }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onListClick(list) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatListBulleted,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            list.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (total == 0) "Empty list" else "$done of $total closed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteList(list.id) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(0.7f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Delete list", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomListDetailScreen(
    list: CustomList,
    appDatabase: AppDatabase,
    onBack: () -> Unit,
    onRenameList: (String) -> Unit,
    onDeleteList: () -> Unit,
    showToast: (String) -> Unit
) {
    var items by remember { mutableStateOf(emptyList<CustomListItem>()) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(list) { mutableStateOf(list.name) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val reloadItems = {
        items = appDatabase.customListItemDao().getCustomListItems(list.id)
    }

    LaunchedEffect(list.id) {
        reloadItems()
    }

    val pendingItems = remember(items) { items.filter { !it.isCompleted } }
    val completedItems = remember(items) { items.filter { it.isCompleted } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            // Top Navigation Header (matching ProjectDetailScreen style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            renameText = list.name
                            showRenameDialog = true
                        }
                ) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename List",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete List",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                if (showRenameDialog) {
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Rename List") },
                        text = {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (renameText.isNotBlank()) {
                                        onRenameList(renameText)
                                    }
                                    showRenameDialog = false
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("Delete List") },
                        text = { Text("Are you sure you want to delete this list? All items inside will be lost.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    onDeleteList()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Items list (matching ProjectDetailScreen layout)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No items. Tap + to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        }
                    }
                } else {
                    // Pending items
                    items(pendingItems, key = { it.id }) { item ->
                        ListItemRow(
                            item = item,
                            onToggle = { isChecked ->
                                appDatabase.customListItemDao().updateCustomListItem(item.copy(isCompleted = isChecked))
                                reloadItems()
                            },
                            onDelete = {
                                appDatabase.customListItemDao().deleteCustomListItem(item.id)
                                reloadItems()
                            }
                        )
                    }

                    if (completedItems.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Closed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }

                        items(completedItems, key = { it.id }) { item ->
                            ListItemRow(
                                item = item,
                                onToggle = { isChecked ->
                                    appDatabase.customListItemDao().updateCustomListItem(item.copy(isCompleted = isChecked))
                                    reloadItems()
                                },
                                onDelete = {
                                    appDatabase.customListItemDao().deleteCustomListItem(item.id)
                                    reloadItems()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add Item FAB (matching ProjectDetailScreen)
        SmallFloatingActionButton(
            onClick = { showCaptureSheet = true },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(18.dp))
        }

        if (showCaptureSheet) {
            QuickCaptureSheet(
                onDismiss = { showCaptureSheet = false },
                onCapture = { title ->
                    val newItem = CustomListItem(
                        id = "cli-${UUID.randomUUID()}",
                        listId = list.id,
                        name = title,
                        isCompleted = false,
                        createdAt = System.currentTimeMillis()
                    )
                    appDatabase.customListItemDao().insertCustomListItem(newItem)
                    reloadItems()
                    showCaptureSheet = false
                    showToast("Item added ✓")
                }
            )
        }
    }
}

@Composable
fun ListItemRow(
    item: CustomListItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggle(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Reopen item",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggle(true) }
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Create New List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                placeholder = { Text("e.g. Shopping List, Packing List") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
                )
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create List", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
