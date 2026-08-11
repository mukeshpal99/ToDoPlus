package com.aerogtd.features.project.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.aerogtd.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<Project>,
    tasks: List<Task>,
    onProjectClick: (Project) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = In Progress, 1 = Completed

    val inProgressProjects = remember(projects) { projects.filter { it.status != ProjectStatus.COMPLETED } }
    val completedProjects = remember(projects) { projects.filter { it.status == ProjectStatus.COMPLETED } }
    val displayProjects = if (selectedTab == 0) inProgressProjects else completedProjects

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Projects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val subTabs = listOf(
                    "In Progress (${inProgressProjects.size})",
                    "Completed (${completedProjects.size})"
                )
                subTabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { selectedTab = index }
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
            Spacer(Modifier.height(8.dp))
        }

        if (displayProjects.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    message = if (selectedTab == 0) "No active projects yet.\nTap + to create one."
                              else "No completed projects yet."
                )
            }
        }

        items(displayProjects, key = { it.id }) { project ->
            val projTasks = tasks.filter { it.projectId == project.id }
            val done = projTasks.count { it.completedAt != null }
            val total = projTasks.size
            val progress = if (total == 0) 0f else done.toFloat() / total
            val hasNext = projTasks.any { it.completedAt == null && !it.isInbox }

            ProjectCard(project = project, done = done, total = total, progress = progress,
                hasNext = hasNext, onClick = { onProjectClick(project) })
        }
    }
}

@Composable
fun ProjectCard(project: Project, done: Int, total: Int, progress: Float,
                hasNext: Boolean, onClick: () -> Unit) {
    val isCompleted = project.status == ProjectStatus.COMPLETED
    val themeColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val bgTint = if (isCompleted) Color(0xFF4CAF50).copy(0.1f) else MaterialTheme.colorScheme.primary.copy(0.1f)
    val cardIcon = if (isCompleted) Icons.Default.CheckCircle else Icons.Filled.Folder

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(32.dp)
                    .background(bgTint, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(cardIcon, null, tint = themeColor,
                        modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!project.goal.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(project.goal, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!hasNext && total > 0 && !isCompleted) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Warning, contentDescription = "No next action",
                        tint = Color(0xFFFF8F00), modifier = Modifier.size(16.dp))
                }
            }
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                        color = themeColor,
                        trackColor = themeColor.copy(0.1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("$done/$total", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectDetailScreen(
    project: Project,
    tasks: List<Task>,
    contexts: List<Context>,
    waitingList: List<WaitingFor>,
    onBack: () -> Unit,
    onAddTask: (String, String?) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    onAddContext: (String) -> Unit,
    onProcessTask: (Task, TaskPriority, TaskEnergy, Int, Long?, String?) -> Unit,
    onDelegateTask: (Task, String, TaskPriority, Long?, String?) -> Unit,
    onSomedayTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRenameProject: (String) -> Unit,
    onDeleteProject: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onCompleteProject: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var activeClarifyTask by remember { mutableStateOf<Task?>(null) }
    var isClarifyReadOnly by remember { mutableStateOf(false) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var showIncompleteTasksError by remember { mutableStateOf(false) }

    val newItems = remember(tasks) { tasks.filter { it.isInbox && it.completedAt == null } }
    val reviewedItems = remember(tasks) { tasks.filter { !it.isInbox && it.completedAt == null } }
    val completedItems = remember(tasks) { tasks.filter { it.completedAt != null }.sortedByDescending { it.completedAt } }

    val isProjectCompleted = project.status == ProjectStatus.COMPLETED

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            // Top Navigation Header
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
                
                var showRenameDialog by remember { mutableStateOf(false) }
                var renameText by remember(project) { mutableStateOf(project.title) }
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isProjectCompleted) {
                            renameText = project.title
                            showRenameDialog = true
                        }
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isProjectCompleted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Project",
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isProjectCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed Project",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                    )
                } else {
                    IconButton(
                        onClick = {
                            val incompleteTasks = tasks.filter { it.completedAt == null }
                            if (incompleteTasks.isEmpty()) {
                                onCompleteProject()
                            } else {
                                showIncompleteTasksError = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = "Complete Project",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteConfirmDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Project",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                if (showRenameDialog) {
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Rename Project") },
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
                                        onRenameProject(renameText)
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
                        title = { Text("Delete Project") },
                        text = { Text("Are you sure you want to delete this project? All associated tasks will be lost.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    onDeleteProject()
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = fg
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Task list area
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (displayItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (page) {
                                        0 -> "No new steps."
                                        1 -> "No active actions."
                                        else -> "No completed actions."
                                    },
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                )
                            }
                        }
                    }

                    items(displayItems, key = { it.id }) { task ->
                        if (page == 2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(0.6f), RoundedCornerShape(12.dp))
                                    .clickable { activeClarifyTask = task; isClarifyReadOnly = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Complete",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                )
                            }
                        } else {
                            val priorityColor = when (task.priority) {
                                TaskPriority.HIGH -> Color(0xFFE53935)
                                TaskPriority.MEDIUM -> Color(0xFFFF8F00)
                                TaskPriority.LOW -> Color(0xFF4CAF50)
                            }
                            val ctxName = task.contextIds.firstOrNull()?.let { id -> contexts.find { it.id == id }?.name }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .clickable { activeClarifyTask = task; isClarifyReadOnly = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (task.isInbox) {
                                    Icon(
                                        Icons.Outlined.Circle, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(0.25f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { onToggleComplete(task.id, true) }
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                    val delegation = waitingList.find { it.taskId == task.id && it.resolvedAt == null }
                                    if (!task.isInbox || delegation != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (!task.isInbox) {
                                                Box(modifier = Modifier.size(6.dp).background(priorityColor, CircleShape))
                                            }
                                            delegation?.let {
                                                Text("Delegated to ${it.person}", style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFFF8F00))
                                            }
                                            if (!task.isInbox) {
                                                ctxName?.let {
                                                    Text("@$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                }
                                                if (task.durationMinutes > 0) {
                                                    Text("${task.durationMinutes}m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                                }
                                            }
                                        }
                                    }
                                }
                                if (task.isInbox) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Review →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { activeClarifyTask = task; isClarifyReadOnly = false }
                                    )
                                } else {
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { activeClarifyTask = task; isClarifyReadOnly = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Task",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

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
            Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(18.dp))
        }

        if (showCaptureSheet) {
            QuickCaptureSheet(
                onDismiss = { showCaptureSheet = false },
                onCapture = { title ->
                    onAddTask(title, null)
                    showCaptureSheet = false
                }
            )
        }

        // Clarify wizard sheet for new project steps
        activeClarifyTask?.let { task ->
            ClarifySheet(
                task = task,
                projects = listOf(project),
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

        if (showIncompleteTasksError) {
            AlertDialog(
                onDismissRequest = { showIncompleteTasksError = false },
                title = { Text("Cannot Complete Project", fontWeight = FontWeight.Bold) },
                text = { Text("All tasks associated with this project must be completed before you can mark the project as completed.") },
                confirmButton = {
                    Button(onClick = { showIncompleteTasksError = false }) {
                        Text("OK")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
            Text("New Project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Project title") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f))
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { if (title.isNotBlank()) onSave(title) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = title.isNotBlank(), shape = RoundedCornerShape(12.dp)) {
                Text("Create Project", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
