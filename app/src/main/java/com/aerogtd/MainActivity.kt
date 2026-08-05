package com.aerogtd

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aerogtd.core.database.*
import com.aerogtd.features.home.presentation.TodayScreen
import com.aerogtd.features.home.presentation.WeeklyReviewSheet
import com.aerogtd.features.inbox.presentation.InboxScreen
import com.aerogtd.features.project.presentation.ProjectsScreen
import com.aerogtd.features.project.presentation.ProjectDetailScreen
import com.aerogtd.features.project.presentation.AddProjectSheet
import com.aerogtd.features.waiting.presentation.WaitingScreen
import com.aerogtd.ui.components.QuickCaptureSheet
import com.aerogtd.ui.theme.ToDoPlusTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper.getInstance(this)
        setContent {
            ToDoPlusTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        dbHelper = dbHelper,
                        showToast = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }
}

// ─── NAV TABS ────────────────────────────────────────────────────────────────
enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    TODAY("Home", Icons.Outlined.Home, Icons.Filled.Home),
    INBOX("Inbox", Icons.Outlined.Inbox, Icons.Filled.Inbox),
    PROJECTS("Projects", Icons.Outlined.FolderOpen, Icons.Filled.Folder),
    WAITING("Delegated", Icons.Outlined.Schedule, Icons.Filled.Schedule)
}

// ─── MAIN SCREEN ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dbHelper: DatabaseHelper, showToast: (String) -> Unit) {
    var activeTab by remember { mutableStateOf(Tab.TODAY) }
    var inboxSubTab by remember { mutableStateOf(0) }
    val tabHistory = remember { ArrayDeque<Tab>() }
    var activeProjectDetail by remember { mutableStateOf<Project?>(null) }
    var tasks by remember { mutableStateOf(emptyList<Task>()) }
    var projects by remember { mutableStateOf(emptyList<Project>()) }
    var contexts by remember { mutableStateOf(emptyList<Context>()) }
    var waitingItems by remember { mutableStateOf(emptyList<WaitingFor>()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddProjectSheet by remember { mutableStateOf(false) }
    var showWeeklyReview by remember { mutableStateOf(false) }

    val reload = {
        tasks = dbHelper.getTasks()
        projects = dbHelper.getProjects()
        contexts = dbHelper.getContexts()
        waitingItems = dbHelper.getWaitingList()
    }

    val onUpdateTask = { updatedTask: Task ->
        dbHelper.updateTask(updatedTask)
        reload()
    }

    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) {
            tasks
        } else {
            tasks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                (it.notes ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredProjects = remember(projects, tasks, searchQuery) {
        if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter { project ->
                project.title.contains(searchQuery, ignoreCase = true) ||
                (project.goal ?: "").contains(searchQuery, ignoreCase = true) ||
                tasks.any { it.projectId == project.id && (it.title.contains(searchQuery, ignoreCase = true) || (it.notes ?: "").contains(searchQuery, ignoreCase = true)) }
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val inboxCount = remember(tasks) { tasks.count { it.isInbox && it.completedAt == null } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                Tab.values().forEach { tab ->
                    val selected = activeTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (activeTab != tab) {
                                tabHistory.addLast(activeTab)
                                activeTab = tab
                            }
                        },
                        icon = {
                            BadgedBox(badge = {
                                if (tab == Tab.INBOX && inboxCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("$inboxCount", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == Tab.TODAY || activeTab == Tab.INBOX || activeTab == Tab.PROJECTS || activeTab == Tab.WAITING) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp, 6.dp)
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (activeTab != Tab.WAITING) {
                        SmallFloatingActionButton(
                            onClick = {
                                if (activeTab == Tab.PROJECTS) showAddProjectSheet = true
                                else showAddSheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp, 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
                    )
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    }
                ) { tab ->
                    when (tab) {
                        Tab.TODAY -> TodayScreen(
                            tasks = filteredTasks,
                            projects = filteredProjects,
                            contexts = contexts,
                            waitingList = waitingItems,
                            onToggleComplete = { id, isComplete ->
                                tasks.find { it.id == id }?.let {
                                    val now = if (isComplete) System.currentTimeMillis() else null
                                    dbHelper.updateTask(it.copy(completedAt = now))
                                    dbHelper.getWaitingList().find { w -> w.taskId == id }?.let { w ->
                                        dbHelper.updateWaiting(w.copy(resolvedAt = now))
                                    }
                                }
                                reload()
                                showToast(if (isComplete) "Done ✓" else "Task reopened")
                            },
                            onTabChange = { activeTab = it },
                            onReviewClick = { showWeeklyReview = true },
                            onAddContext = { name ->
                                dbHelper.insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                reload()
                            },
                            onProcessTask = { task, priority, energy, duration, dueDate, cId ->
                                dbHelper.updateTask(task.copy(
                                    isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                    durationMinutes = duration, dueDate = dueDate,
                                    completedAt = null,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                waitingItems.find { it.taskId == task.id }?.let { w ->
                                    dbHelper.updateWaiting(w.copy(resolvedAt = null))
                                }
                                reload()
                            },
                            onDelegateTask = { task, person, priority, dueDate, cId ->
                                dbHelper.updateTask(task.copy(
                                    isInbox = false, isSomeday = false, priority = priority,
                                    dueDate = dueDate,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                dbHelper.insertWaiting(WaitingFor(
                                    id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                    person = person, dateDelegated = System.currentTimeMillis(),
                                    reminderDate = dueDate,
                                    expectedResponse = null, resolvedAt = null
                                )); reload()
                            },
                            onSomedayTask = { task ->
                                dbHelper.updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                reload()
                            },
                            onDeleteTask = { id ->
                                dbHelper.deleteTask(id); reload()
                            },
                            onDoneClick = {
                                inboxSubTab = 2
                                activeTab = Tab.INBOX
                            },
                            onUpdateTask = onUpdateTask
                        )

                        Tab.INBOX -> InboxScreen(
                            tasks = filteredTasks, projects = filteredProjects, contexts = contexts,
                            waitingList = waitingItems,
                            initialSubTab = inboxSubTab,
                            onSubTabChange = { inboxSubTab = it },
                            onToggleComplete = { id, isComplete ->
                                tasks.find { it.id == id }?.let {
                                    val now = if (isComplete) System.currentTimeMillis() else null
                                    dbHelper.updateTask(it.copy(completedAt = now))
                                    dbHelper.getWaitingList().find { w -> w.taskId == id }?.let { w ->
                                        dbHelper.updateWaiting(w.copy(resolvedAt = now))
                                    }
                                }
                                reload()
                            },
                            onAddContext = { name ->
                                dbHelper.insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                reload()
                            },
                            onProcess = { task, priority, energy, duration, dueDate, cId ->
                                dbHelper.updateTask(task.copy(
                                    isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                    durationMinutes = duration, dueDate = dueDate,
                                    completedAt = null,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                dbHelper.getWaitingList().find { it.taskId == task.id }?.let { w ->
                                    dbHelper.updateWaiting(w.copy(resolvedAt = null))
                                }
                                reload(); showToast(if (task.isSomeday) "Saved to Someday" else "Moved to Actions")
                            },
                            onDelegate = { task, person, priority, dueDate, cId ->
                                dbHelper.updateTask(task.copy(
                                    isInbox = false, isSomeday = false, priority = priority,
                                    dueDate = dueDate,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                dbHelper.insertWaiting(WaitingFor(
                                    id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                    person = person, dateDelegated = System.currentTimeMillis(),
                                    reminderDate = dueDate,
                                    expectedResponse = null, resolvedAt = null
                                )); reload(); showToast("Delegated to $person")
                            },
                            onSomeday = { task ->
                                dbHelper.updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                reload(); showToast("Moved to Someday")
                            },
                            onTrash = { task ->
                                dbHelper.deleteTask(task.id); reload(); showToast("Trashed")
                            },
                            onUpdateTask = onUpdateTask
                        )

                        Tab.PROJECTS -> ProjectsScreen(
                            projects = filteredProjects,
                            tasks = filteredTasks,
                            onProjectClick = { project ->
                                activeProjectDetail = project
                            }
                        )

                        Tab.WAITING -> WaitingScreen(
                            waitingList = waitingItems,
                            tasks = filteredTasks,
                            projects = filteredProjects,
                            contexts = contexts,
                            onResolve = { id, resolved ->
                                waitingItems.find { it.id == id }?.let { w ->
                                    val now = if (resolved) System.currentTimeMillis() else null
                                    dbHelper.updateWaiting(w.copy(resolvedAt = now))
                                    tasks.find { it.id == w.taskId }?.let { t ->
                                        dbHelper.updateTask(t.copy(completedAt = now))
                                    }
                                }; reload(); showToast(if (resolved) "Received ✓" else "Delegation reopened")
                            },
                            onAddContext = { name ->
                                dbHelper.insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                reload()
                            },
                            onProcessTask = { task, priority, energy, duration, dueDate, cId ->
                                dbHelper.updateTask(task.copy(
                                    isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                    durationMinutes = duration, dueDate = dueDate,
                                    completedAt = null,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                waitingItems.find { it.taskId == task.id }?.let { w ->
                                    dbHelper.updateWaiting(w.copy(resolvedAt = null))
                                }
                                reload()
                            },
                            onDelegateTask = { task, person, priority, dueDate, cId ->
                                dbHelper.updateTask(task.copy(
                                    isInbox = false, isSomeday = false, priority = priority,
                                    dueDate = dueDate,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                dbHelper.insertWaiting(WaitingFor(
                                    id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                    person = person, dateDelegated = System.currentTimeMillis(),
                                    reminderDate = dueDate,
                                    expectedResponse = null, resolvedAt = null
                                )); reload()
                            },
                            onSomedayTask = { task ->
                                dbHelper.updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                reload()
                            },
                            onDeleteTask = { id ->
                                dbHelper.deleteTask(id); reload()
                            },
                            onUpdateTask = onUpdateTask
                        )
                    }
                }
            }
        }
    }

    // ── Quick Capture Bottom Sheet ──
    if (showAddSheet) {
        QuickCaptureSheet(
            onDismiss = { showAddSheet = false },
            onCapture = { title ->
                dbHelper.insertTask(Task(
                    id = "t-${System.currentTimeMillis()}", projectId = null, title = title,
                    notes = null, priority = TaskPriority.LOW, energy = TaskEnergy.LOW,
                    durationMinutes = 15, dueDate = null, startDate = null, completedAt = null,
                    isInbox = true, isSomeday = false, recurrenceRule = null,
                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                ))
                reload(); showAddSheet = false; showToast("Captured to Inbox ✓")
            }
        )
    }

    if (showAddProjectSheet) {
        AddProjectSheet(onDismiss = { showAddProjectSheet = false }, onSave = { title ->
            dbHelper.insertProject(Project(
                id = "p-${System.currentTimeMillis()}", title = title,
                goal = null, outcome = null, deadline = null,
                status = ProjectStatus.ACTIVE,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            ))
            reload()
            showToast("Project created")
            showAddProjectSheet = false
        })
    }

    if (showWeeklyReview) {
        WeeklyReviewSheet(onDismiss = { showWeeklyReview = false })
    }

    val currentProject = activeProjectDetail
    // Back handler: project detail -> active tab -> previous tab -> exit
    BackHandler(enabled = currentProject != null || tabHistory.isNotEmpty()) {
        when {
            currentProject != null -> activeProjectDetail = null
            tabHistory.isNotEmpty() -> activeTab = tabHistory.removeLast()
        }
    }
    if (currentProject != null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ProjectDetailScreen(
                project = currentProject,
                tasks = filteredTasks.filter { it.projectId == currentProject.id },
                contexts = contexts,
                waitingList = waitingItems,
                onBack = { activeProjectDetail = null },
                onAddTask = { title, contextId ->
                    dbHelper.insertTask(Task(
                        id = "t-${System.currentTimeMillis()}", projectId = currentProject.id,
                        title = title, notes = null, priority = TaskPriority.MEDIUM,
                        energy = TaskEnergy.MEDIUM, durationMinutes = 30,
                        dueDate = null, startDate = null, completedAt = null,
                        isInbox = true, isSomeday = false, recurrenceRule = null,
                        createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                        contextIds = if (contextId != null) listOf(contextId) else emptyList()
                    )); reload()
                },
                onToggleComplete = { id, isComplete ->
                    tasks.find { it.id == id }?.let {
                        val now = if (isComplete) System.currentTimeMillis() else null
                        dbHelper.updateTask(it.copy(completedAt = now))
                        dbHelper.getWaitingList().find { w -> w.taskId == id }?.let { w ->
                            dbHelper.updateWaiting(w.copy(resolvedAt = now))
                        }
                    }
                    reload()
                },
                onAddContext = { name ->
                    dbHelper.insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                    reload()
                },
                onProcessTask = { task, priority, energy, duration, dueDate, cId ->
                    dbHelper.updateTask(task.copy(
                        isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                        durationMinutes = duration, dueDate = dueDate,
                        completedAt = null,
                        contextIds = if (cId != null) listOf(cId) else emptyList()
                    ))
                    dbHelper.getWaitingList().find { it.taskId == task.id }?.let { w ->
                        dbHelper.updateWaiting(w.copy(resolvedAt = null))
                    }
                    reload()
                },
                onDelegateTask = { task, person, priority, dueDate, cId ->
                    dbHelper.updateTask(task.copy(
                        isInbox = false, isSomeday = false, priority = priority,
                        dueDate = dueDate,
                        contextIds = if (cId != null) listOf(cId) else emptyList()
                    ))
                    dbHelper.insertWaiting(WaitingFor(
                        id = "w-${System.currentTimeMillis()}", taskId = task.id,
                        person = person, dateDelegated = System.currentTimeMillis(),
                        reminderDate = dueDate,
                        expectedResponse = null, resolvedAt = null
                    )); reload()
                },
                onSomedayTask = { task ->
                    dbHelper.updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                    reload()
                },
                onDeleteTask = { id ->
                    dbHelper.deleteTask(id); reload()
                },
                onRenameProject = { newName ->
                    dbHelper.updateProject(currentProject.copy(title = newName))
                    activeProjectDetail = dbHelper.getProjects().find { it.id == currentProject.id }
                    reload()
                },
                onDeleteProject = {
                    dbHelper.deleteProject(currentProject.id)
                    tasks.filter { it.projectId == currentProject.id }.forEach {
                        dbHelper.deleteTask(it.id)
                    }
                    activeProjectDetail = null
                    reload()
                },
                onUpdateTask = onUpdateTask,
                onCompleteProject = {
                    dbHelper.updateProject(currentProject.copy(status = ProjectStatus.COMPLETED))
                    activeProjectDetail = dbHelper.getProjects().find { it.id == currentProject.id }
                    reload()
                    showToast("Project completed ✓")
                }
            )
        }
    }
}
