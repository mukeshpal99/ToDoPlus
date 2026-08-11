package com.aerogtd.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerogtd.Tab
import com.aerogtd.core.database.*
import com.aerogtd.core.database.Context
import com.aerogtd.features.dashboard.presentation.NextActionEngine
import com.aerogtd.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    tasks: List<Task>,
    projects: List<Project>,
    contexts: List<Context>,
    waitingList: List<WaitingFor>,
    onToggleComplete: (String, Boolean) -> Unit,
    onTabChange: (Tab) -> Unit,
    onReviewClick: () -> Unit,
    onAddContext: (String) -> Unit,
    onProcessTask: (Task, TaskPriority, TaskEnergy, Int, Long?, String?) -> Unit,
    onDelegateTask: (Task, String, TaskPriority, Long?, String?) -> Unit,
    onSomedayTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onDoneClick: () -> Unit,
    onUpdateTask: (Task) -> Unit
) {
    val today = remember {
        val cal = Calendar.getInstance()
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
    }

    var activeTask by remember { mutableStateOf<Task?>(null) }
    var isClarifyReadOnly by remember { mutableStateOf(false) }

    val actionTasks = remember(tasks, waitingList) {
        tasks.filter { it.projectId == null && !it.isInbox && !it.isSomeday && it.completedAt == null && waitingList.none { w -> w.taskId == it.id && w.resolvedAt == null } }
    }

    val doneTasks = remember(tasks) {
        tasks.filter { it.projectId == null && it.completedAt != null }
    }

    val recommended = remember(tasks, waitingList) {
        val nonDelegatedTasks = tasks.filter { it.projectId == null && waitingList.none { w -> w.taskId == it.id && w.resolvedAt == null } }
        NextActionEngine.getRecommendedTask(nonDelegatedTasks, null, null, null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(today, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            letterSpacing = 0.3.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("Today", style = MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(
                        onClick = onReviewClick,
                        modifier = Modifier.size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Weekly Review",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Focus card (recommended task)
            recommended?.let { task ->
                item {
                    FocusCard(task = task, projects = projects, contexts = contexts, waitingList = waitingList, onComplete = { onToggleComplete(task.id, true) })
                }
            }

            // Stats row
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        value = "${actionTasks.size}",
                        label = "Actions",
                        onClick = onDoneClick
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        value = "${doneTasks.size}",
                        label = "Done",
                        onClick = onDoneClick
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        value = "${tasks.count { it.projectId == null && it.isInbox && it.completedAt == null }}",
                        label = "Tasks",
                        onClick = { onTabChange(Tab.TASKS) }
                    )
                }
            }

            // Section label
            if (actionTasks.isNotEmpty()) {
                item {
                    Text("NEXT ACTIONS", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                }
            }

            // Task rows
            items(actionTasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    projects = projects,
                    contexts = contexts,
                    waitingList = waitingList,
                    onComplete = { onToggleComplete(task.id, true) },
                    onTap = { activeTask = task; isClarifyReadOnly = true }
                )
            }

            if (actionTasks.isEmpty() && recommended == null) {
                item { EmptyState(icon = Icons.Default.CheckCircle, message = "All clear for today!") }
            }
        }

        activeTask?.let { task ->
            ClarifySheet(
                task = task,
                projects = projects,
                contexts = contexts,
                waitingList = waitingList,
                onDismiss = { activeTask = null },
                onAddContext = onAddContext,
                onProcess = { newTitle, pri, energy, dur, dueDate, cId -> onProcessTask(task.copy(title = newTitle), pri, energy, dur, dueDate, cId); activeTask = null },
                onDelegate = { newTitle, person, pri, dueDate, cId -> onDelegateTask(task.copy(title = newTitle), person, pri, dueDate, cId); activeTask = null },
                onSomeday = { newTitle, pri, dueDate -> onSomedayTask(task.copy(title = newTitle, priority = pri, dueDate = dueDate)); activeTask = null },
                onTrash = { onDeleteTask(task.id); activeTask = null },
                forceReadOnly = isClarifyReadOnly,
                onUpdateTitle = { newTitle -> onUpdateTask(task.copy(title = newTitle)) },
                onActNow = { newTitle, pri, energy, dur, dueDate, cId -> onProcessTask(task.copy(title = newTitle, isSomeday = false), pri, energy, dur, dueDate, cId); activeTask = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        Triple("Clear Tasks", "Process every item in your tasks list using the Clarify wizard.", Icons.Default.Inbox),
        Triple("Check Projects", "Every active project must have at least 1 Next Action.", Icons.Default.FolderOpen),
        Triple("Review Delegations", "Follow up on waiting items. Chase overdue deliverables.", Icons.Default.Schedule),
        Triple("Someday", "Activate ideas from Someday that are now ready.", Icons.Default.WatchLater),
        Triple("Look Ahead", "Review upcoming due dates and prepare checklists.", Icons.Default.Today)
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp).padding(bottom = 40.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weekly Review", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${step + 1} of ${steps.size}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (step + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(0.1f)
            )
            Spacer(Modifier.height(28.dp))

            val (title, desc, icon) = steps[step]
            Box(modifier = Modifier.size(56.dp)
                .background(MaterialTheme.colorScheme.primary.copy(0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                lineHeight = 22.sp)
            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)) { Text("Back", style = MaterialTheme.typography.labelLarge) }
                }
                Button(
                    onClick = { if (step < steps.size - 1) step++ else onDismiss() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (step == steps.size - 1) Color(0xFF4CAF50)
                                         else MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (step == steps.size - 1) "Complete Review ✓" else "Next Step →",
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
