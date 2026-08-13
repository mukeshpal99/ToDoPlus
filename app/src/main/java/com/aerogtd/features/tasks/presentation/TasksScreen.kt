package com.aerogtd.features.tasks.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aerogtd.core.database.*
import com.aerogtd.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    projects: List<Project>,
    contexts: List<Context>,
    waitingList: List<WaitingFor>,
    initialSubTab: Int,
    onSubTabChange: (Int) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    onAddContext: (String) -> Unit,
    onProcess: (Task, TaskPriority, TaskEnergy, Int, Long?, String?) -> Unit,
    onDelegate: (Task, String, TaskPriority, Long?, String?) -> Unit,
    onSomeday: (Task) -> Unit,
    onTrash: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialSubTab, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var activeTask by remember { mutableStateOf<Task?>(null) }
    var isClarifyReadOnly by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    var selectedFilterCtx by remember { mutableStateOf<String?>(null) }

    var filterSomeday by remember { mutableStateOf(false) }

    LaunchedEffect(initialSubTab) {
        if (pagerState.currentPage != initialSubTab) {
            pagerState.scrollToPage(initialSubTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onSubTabChange(pagerState.currentPage)
    }

    val newItems = remember(tasks, selectedFilterCtx) { 
        tasks.filter { it.projectId == null && it.isInbox && it.completedAt == null && (selectedFilterCtx == null || it.contextIds.contains(selectedFilterCtx)) } 
    }
    val reviewedItems = remember(tasks, selectedFilterCtx, filterSomeday) { 
        tasks.filter { 
            it.projectId == null && 
            !it.isInbox && 
            it.completedAt == null && 
            (!filterSomeday || it.isSomeday) && 
            (selectedFilterCtx == null || it.contextIds.contains(selectedFilterCtx)) 
        } 
    }
    val completedItems = remember(tasks, selectedFilterCtx) { 
        tasks.filter { it.projectId == null && it.completedAt != null && (selectedFilterCtx == null || it.contextIds.contains(selectedFilterCtx)) } 
             .sortedByDescending { it.completedAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tasks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pagerState.currentPage == 1) {
                    FilterChip(
                        selected = filterSomeday,
                        onClick = { filterSomeday = !filterSomeday },
                        label = { Text("Someday", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.WatchLater,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF8F00).copy(0.15f),
                            selectedLabelColor = Color(0xFFFF8F00),
                            selectedLeadingIconColor = Color(0xFFFF8F00)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filterSomeday,
                            borderColor = MaterialTheme.colorScheme.outline.copy(0.4f),
                            selectedBorderColor = Color(0xFFFF8F00).copy(0.6f)
                        )
                    )
                }
                Box {
                    IconButton(onClick = { filterExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Context")
                    }
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Contexts", fontWeight = if (selectedFilterCtx == null) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { selectedFilterCtx = null; filterExpanded = false }
                        )
                        contexts.forEach { ctx ->
                            DropdownMenuItem(
                                text = { Text("@${ctx.name}", fontWeight = if (selectedFilterCtx == ctx.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { selectedFilterCtx = ctx.id; filterExpanded = false }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Sub-tabs capsule switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf(
                "New (${newItems.size})",
                "Reviewed (${reviewedItems.size})",
                "Done (${completedItems.size})"
            )
            subTabs.forEachIndexed { index, label ->
                val isSelected = pagerState.currentPage == index
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val displayItems = when (page) {
                0 -> newItems
                1 -> reviewedItems
                else -> completedItems
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayItems.isEmpty()) {
                    item {
                        EmptyState(
                            icon = when (page) {
                                0 -> Icons.Default.Inbox
                                1 -> Icons.Default.DoneAll
                                else -> Icons.Default.CheckCircle
                            },
                            message = when (page) {
                                0 -> "Your task list is empty.\nTap + to capture thoughts."
                                1 -> "No reviewed tasks yet.\nClarify new items to organize them."
                                else -> "No completed tasks yet."
                            }
                        )
                    }
                }

                items(displayItems, key = { it.id }) { task ->
                    InboxTaskCard(
                        task = task,
                        projects = projects,
                        contexts = contexts,
                        waitingList = waitingList,
                        onTap = { activeTask = task; isClarifyReadOnly = true },
                        onEditTap = { activeTask = task; isClarifyReadOnly = false },
                        onToggleComplete = { isComplete -> onToggleComplete(task.id, isComplete) },
                        onTrash = { onTrash(task) }
                    )
                }
            }
        }
    }

    // Clarify / Edit bottom sheet
    activeTask?.let { task ->
        ClarifySheet(
            task = task,
            projects = projects,
            contexts = contexts,
            waitingList = waitingList,
            onDismiss = { activeTask = null },
            onAddContext = onAddContext,
            onProcess = { newTitle, pri, energy, dur, dueDate, cId -> onProcess(task.copy(title = newTitle), pri, energy, dur, dueDate, cId); activeTask = null },
            onDelegate = { newTitle, person, pri, dueDate, cId -> onDelegate(task.copy(title = newTitle), person, pri, dueDate, cId); activeTask = null },
            onSomeday = { newTitle, pri, dueDate -> onSomeday(task.copy(title = newTitle, priority = pri, dueDate = dueDate)); activeTask = null },
            onTrash = { onTrash(task); activeTask = null },
            onUpdateTitle = { newTitle ->
                val updated = task.copy(title = newTitle)
                activeTask = updated
                onUpdateTask(updated)
            },
            onUpdateTask = { updated ->
                activeTask = updated
                onUpdateTask(updated)
            },
            onActNow = { newTitle, pri, energy, dur, dueDate, cId -> onProcess(task.copy(title = newTitle, isSomeday = false), pri, energy, dur, dueDate, cId); activeTask = null }
        )
    }
}
