package com.aerogtd.features.waiting.presentation

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaitingScreen(
    waitingList: List<WaitingFor>,
    tasks: List<Task>,
    projects: List<Project>,
    contexts: List<Context>,
    onResolve: (String, Boolean) -> Unit,
    onAddContext: (String) -> Unit,
    onProcessTask: (Task, TaskPriority, TaskEnergy, Int, Long?, String?) -> Unit,
    onDelegateTask: (Task, String, TaskPriority, Long?, String?) -> Unit,
    onSomedayTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onUpdateTask: (Task) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val pending = remember(waitingList) { waitingList.filter { it.resolvedAt == null } }
    val resolved = remember(waitingList) { waitingList.filter { it.resolvedAt != null } }

    var activeClarifyTask by remember { mutableStateOf<Task?>(null) }
    var isClarifyReadOnly by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 24.dp)) {
            Text("Delegated", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                    "Waiting (${pending.size})",
                    "Done (${resolved.size})"
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
                val list = if (page == 0) pending else resolved

                val displayItems = remember(list, tasks) {
                    list.filter { item -> tasks.any { it.id == item.taskId } }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (displayItems.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Schedule,
                                message = if (page == 0) "No delegations pending.\nAll deliverables received."
                                          else "No resolved delegations yet."
                            )
                        }
                    }

                    items(displayItems, key = { it.id }) { item ->
                        val task = tasks.find { it.id == item.taskId }
                        if (task != null) {
                            InboxTaskCard(
                                task = task,
                                projects = projects,
                                contexts = contexts,
                                waitingList = waitingList,
                                onTap = { activeClarifyTask = task; isClarifyReadOnly = true },
                                onEditTap = { activeClarifyTask = task; isClarifyReadOnly = false },
                                onToggleComplete = { isComplete -> onResolve(item.id, isComplete) },
                                onTrash = { onDeleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }

        // Clarify/Edit wizard sheet for delegated tasks
        activeClarifyTask?.let { task ->
            ClarifySheet(
                task = task,
                projects = projects,
                contexts = contexts,
                waitingList = waitingList,
                onDismiss = { activeClarifyTask = null },
                onAddContext = onAddContext,
                onProcess = { newTitle, pri, energy, dur, dueDate, cId -> onProcessTask(task.copy(title = newTitle), pri, energy, dur, dueDate, cId); activeClarifyTask = null },
                onDelegate = { newTitle, person, pri, dueDate, cId -> onDelegateTask(task.copy(title = newTitle), person, pri, dueDate, cId); activeClarifyTask = null },
                onSomeday = { newTitle, pri, dueDate -> onSomedayTask(task.copy(title = newTitle, priority = pri, dueDate = dueDate)); activeClarifyTask = null },
                onTrash = { onDeleteTask(task.id); activeClarifyTask = null },
                forceReadOnly = isClarifyReadOnly,
                onUpdateTitle = { newTitle -> onUpdateTask(task.copy(title = newTitle)) },
                onActNow = { newTitle, pri, energy, dur, dueDate, cId -> onProcessTask(task.copy(title = newTitle, isSomeday = false), pri, energy, dur, dueDate, cId); activeClarifyTask = null }
            )
        }
    }
}
