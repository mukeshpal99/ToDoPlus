package com.aerogtd

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.aerogtd.features.tasks.presentation.TasksScreen
import com.aerogtd.features.project.presentation.ProjectsScreen
import com.aerogtd.features.project.presentation.ProjectDetailScreen
import com.aerogtd.features.project.presentation.AddProjectSheet
import com.aerogtd.features.waiting.presentation.WaitingScreen
import com.aerogtd.features.lists.presentation.*
import com.aerogtd.ui.components.QuickCaptureSheet
import com.aerogtd.ui.theme.ToDoPlusTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDatabase = AppDatabase.getDatabase(this)
        setContent {
            ToDoPlusTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        appDatabase = appDatabase,
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
    TASKS("Tasks", Icons.Outlined.Inbox, Icons.Filled.Inbox),
    PROJECTS("Projects", Icons.Outlined.FolderOpen, Icons.Filled.Folder),
    WAITING("Delegated", Icons.Outlined.Schedule, Icons.Filled.Schedule),
    LISTS("Lists", Icons.AutoMirrored.Outlined.FormatListBulleted, Icons.AutoMirrored.Filled.FormatListBulleted)
}

// ─── MAIN SCREEN ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(appDatabase: AppDatabase, showToast: (String) -> Unit) {
    var activeTab by remember { mutableStateOf(Tab.TODAY) }
    var tasksSubTab by remember { mutableStateOf(0) }
    val tabHistory = remember { ArrayDeque<Tab>() }
    var activeProjectDetail by remember { mutableStateOf<Project?>(null) }
    var tasks by remember { mutableStateOf(emptyList<Task>()) }
    var projects by remember { mutableStateOf(emptyList<Project>()) }
    var contexts by remember { mutableStateOf(emptyList<Context>()) }
    var waitingItems by remember { mutableStateOf(emptyList<WaitingFor>()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddProjectSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var showWeeklyReview by remember { mutableStateOf(false) }
    var activeListDetail by remember { mutableStateOf<CustomList?>(null) }
    var lists by remember { mutableStateOf(emptyList<CustomList>()) }
    var showAddListSheet by remember { mutableStateOf(false) }

    val reload = {
        tasks = appDatabase.taskDao().getTasks()
        projects = appDatabase.projectDao().getProjects()
        contexts = appDatabase.contextDao().getContexts()
        waitingItems = appDatabase.waitingForDao().getWaitingList()
        lists = appDatabase.customListDao().getCustomLists()
    }

    val onUpdateTask = { updatedTask: Task ->
        appDatabase.taskDao().updateTask(updatedTask)
        reload()
    }

    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) {
            tasks
        } else {
            tasks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.notes != null && it.notes.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.goal != null && it.goal.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val filteredLists = remember(lists, searchQuery) {
        if (searchQuery.isBlank()) {
            lists
        } else {
            lists.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(showSearchBar) {
        if (!showSearchBar) {
            searchQuery = ""
        }
    }

    val tasksCount = remember(tasks) { tasks.count { it.projectId == null && it.isInbox && it.completedAt == null } }

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
                                if (tab == Tab.TASKS && tasksCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("$tasksCount", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        floatingActionButton = {
            val isProjectDetailOpen = activeTab == Tab.PROJECTS && activeProjectDetail != null
            val isListDetailOpen = activeTab == Tab.LISTS && activeListDetail != null
            if (!isProjectDetailOpen && !isListDetailOpen) {
                if (activeTab == Tab.TODAY || activeTab == Tab.TASKS || activeTab == Tab.PROJECTS || activeTab == Tab.WAITING || activeTab == Tab.LISTS) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search FAB on all screens
                        SmallFloatingActionButton(
                            onClick = { showSearchBar = !showSearchBar },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp)
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Toggle Search",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Primary Add FAB depending on the screen
                        when (activeTab) {
                            Tab.TODAY, Tab.TASKS -> {
                                SmallFloatingActionButton(
                                    onClick = { showAddSheet = true },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Quick Add Task", modifier = Modifier.size(18.dp))
                                }
                            }
                            Tab.PROJECTS -> {
                                SmallFloatingActionButton(
                                    onClick = { showAddProjectSheet = true },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Project", modifier = Modifier.size(18.dp))
                                }
                            }
                            Tab.LISTS -> {
                                SmallFloatingActionButton(
                                    onClick = { showAddListSheet = true },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add List", modifier = Modifier.size(18.dp))
                                }
                            }
                            else -> {
                                // WAITING tab does not have an Add FAB, only the Search FAB is shown
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks, projects, lists...", color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            } else {
                                showSearchBar = false
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", modifier = Modifier.size(18.dp))
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
                                    appDatabase.taskDao().updateTask(it.copy(completedAt = now))
                                    appDatabase.waitingForDao().getWaitingList().find { w -> w.taskId == id }?.let { w ->
                                        appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = now))
                                    }
                                }
                                reload()
                                showToast(if (isComplete) "Done ✓" else "Task reopened")
                            },
                            onTabChange = { activeTab = it },
                            onReviewClick = { showWeeklyReview = true },
                            onAddContext = { name ->
                                appDatabase.contextDao().insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                reload()
                            },
                            onProcessTask = { task, priority, energy, duration, dueDate, cId ->
                                appDatabase.taskDao().updateTask(task.copy(
                                    isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                    durationMinutes = duration, dueDate = dueDate,
                                    completedAt = null,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                waitingItems.find { it.taskId == task.id }?.let { w ->
                                    appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = null))
                                }
                                reload()
                            },
                            onDelegateTask = { task, person, priority, dueDate, cId ->
                                appDatabase.taskDao().updateTask(task.copy(
                                    isInbox = false, isSomeday = false, priority = priority,
                                    dueDate = dueDate,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                appDatabase.waitingForDao().insertWaiting(WaitingFor(
                                    id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                    person = person, dateDelegated = System.currentTimeMillis(),
                                    reminderDate = dueDate,
                                    expectedResponse = null, resolvedAt = null
                                )); reload()
                            },
                            onSomedayTask = { task ->
                                appDatabase.taskDao().updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                reload()
                            },
                            onDeleteTask = { id ->
                                appDatabase.taskDao().deleteTask(id); reload()
                            },
                            onDoneClick = {
                                tasksSubTab = 2
                                activeTab = Tab.TASKS
                            },
                            onUpdateTask = onUpdateTask
                        )

                        Tab.TASKS -> TasksScreen(
                            tasks = filteredTasks, projects = filteredProjects, contexts = contexts,
                            waitingList = waitingItems,
                            initialSubTab = tasksSubTab,
                            onSubTabChange = { tasksSubTab = it },
                            onToggleComplete = { id, isComplete ->
                                tasks.find { it.id == id }?.let {
                                    val now = if (isComplete) System.currentTimeMillis() else null
                                    appDatabase.taskDao().updateTask(it.copy(completedAt = now))
                                    appDatabase.waitingForDao().getWaitingList().find { w -> w.taskId == id }?.let { w ->
                                        appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = now))
                                    }
                                }
                                reload()
                            },
                            onAddContext = { name ->
                                appDatabase.contextDao().insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                reload()
                            },
                            onProcess = { task, priority, energy, duration, dueDate, cId ->
                                appDatabase.taskDao().updateTask(task.copy(
                                    isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                    durationMinutes = duration, dueDate = dueDate,
                                    completedAt = null,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                appDatabase.waitingForDao().getWaitingList().find { it.taskId == task.id }?.let { w ->
                                    appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = null))
                                }
                                reload(); showToast(if (task.isSomeday) "Saved to Someday" else "Moved to Actions")
                            },
                            onDelegate = { task, person, priority, dueDate, cId ->
                                appDatabase.taskDao().updateTask(task.copy(
                                    isInbox = false, isSomeday = false, priority = priority,
                                    dueDate = dueDate,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                appDatabase.waitingForDao().insertWaiting(WaitingFor(
                                    id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                    person = person, dateDelegated = System.currentTimeMillis(),
                                    reminderDate = dueDate,
                                    expectedResponse = null, resolvedAt = null
                                )); reload(); showToast("Delegated to $person")
                            },
                            onSomeday = { task ->
                                appDatabase.taskDao().updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                reload(); showToast("Moved to Someday")
                            },
                            onTrash = { task ->
                                appDatabase.taskDao().deleteTask(task.id); reload(); showToast("Trashed")
                            },
                            onUpdateTask = onUpdateTask
                        )

                        Tab.PROJECTS -> {
                            val project = activeProjectDetail
                            if (project != null) {
                                ProjectDetailScreen(
                                    project = project,
                                    tasks = filteredTasks.filter { it.projectId == project.id },
                                    contexts = contexts,
                                    waitingList = waitingItems,
                                    onBack = { activeProjectDetail = null },
                                    onAddTask = { title, contextId ->
                                        appDatabase.taskDao().insertTask(Task(
                                            id = "t-${System.currentTimeMillis()}", projectId = project.id,
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
                                            appDatabase.taskDao().updateTask(it.copy(completedAt = now))
                                            appDatabase.waitingForDao().getWaitingList().find { w -> w.taskId == id }?.let { w ->
                                                appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = now))
                                            }
                                        }
                                        reload()
                                    },
                                    onAddContext = { name ->
                                        appDatabase.contextDao().insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                        reload()
                                    },
                                    onProcessTask = { task, priority, energy, duration, dueDate, cId ->
                                        appDatabase.taskDao().updateTask(task.copy(
                                            isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                            durationMinutes = duration, dueDate = dueDate,
                                            completedAt = null,
                                            contextIds = if (cId != null) listOf(cId) else emptyList()
                                        ))
                                        appDatabase.waitingForDao().getWaitingList().find { it.taskId == task.id }?.let { w ->
                                            appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = null))
                                        }
                                        reload()
                                    },
                                    onDelegateTask = { task, person, priority, dueDate, cId ->
                                        appDatabase.taskDao().updateTask(task.copy(
                                            isInbox = false, isSomeday = false, priority = priority,
                                            dueDate = dueDate,
                                            contextIds = if (cId != null) listOf(cId) else emptyList()
                                        ))
                                        appDatabase.waitingForDao().insertWaiting(WaitingFor(
                                            id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                            person = person, dateDelegated = System.currentTimeMillis(),
                                            reminderDate = dueDate,
                                            expectedResponse = null, resolvedAt = null
                                        )); reload()
                                    },
                                    onSomedayTask = { task ->
                                        appDatabase.taskDao().updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                        reload()
                                    },
                                    onDeleteTask = { id ->
                                        appDatabase.taskDao().deleteTask(id); reload()
                                    },
                                    onRenameProject = { newName ->
                                        appDatabase.projectDao().updateProject(project.copy(title = newName))
                                        activeProjectDetail = appDatabase.projectDao().getProjects().find { it.id == project.id }
                                        reload()
                                    },
                                    onDeleteProject = {
                                        appDatabase.projectDao().deleteProject(project.id)
                                        tasks.filter { it.projectId == project.id }.forEach {
                                            appDatabase.taskDao().deleteTask(it.id)
                                        }
                                        activeProjectDetail = null
                                        reload()
                                    },
                                    onUpdateTask = onUpdateTask,
                                    onCompleteProject = {
                                        appDatabase.projectDao().updateProject(project.copy(status = ProjectStatus.COMPLETED))
                                        activeProjectDetail = appDatabase.projectDao().getProjects().find { it.id == project.id }
                                        reload()
                                        showToast("Project completed ✓")
                                    }
                                )
                            } else {
                                ProjectsScreen(
                                    projects = filteredProjects,
                                    tasks = filteredTasks,
                                    onProjectClick = { project ->
                                        activeProjectDetail = project
                                        showSearchBar = false
                                    }
                                )
                            }
                        }

                        Tab.WAITING -> WaitingScreen(
                            waitingList = waitingItems,
                            tasks = filteredTasks,
                            projects = filteredProjects,
                            contexts = contexts,
                            onResolve = { id, resolved ->
                                waitingItems.find { it.id == id }?.let { w ->
                                    val now = if (resolved) System.currentTimeMillis() else null
                                    appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = now))
                                    tasks.find { it.id == w.taskId }?.let { t ->
                                        appDatabase.taskDao().updateTask(t.copy(completedAt = now))
                                    }
                                }; reload(); showToast(if (resolved) "Received ✓" else "Delegation reopened")
                            },
                            onAddContext = { name ->
                                appDatabase.contextDao().insertContext(Context("c-${UUID.randomUUID()}", name, "tag", "#3A6FDB"))
                                reload()
                            },
                            onProcessTask = { task, priority, energy, duration, dueDate, cId ->
                                appDatabase.taskDao().updateTask(task.copy(
                                    isInbox = false, isSomeday = task.isSomeday, priority = priority, energy = energy,
                                    durationMinutes = duration, dueDate = dueDate,
                                    completedAt = null,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                waitingItems.find { it.taskId == task.id }?.let { w ->
                                    appDatabase.waitingForDao().updateWaiting(w.copy(resolvedAt = null))
                                }
                                reload()
                            },
                            onDelegateTask = { task, person, priority, dueDate, cId ->
                                appDatabase.taskDao().updateTask(task.copy(
                                    isInbox = false, isSomeday = false, priority = priority,
                                    dueDate = dueDate,
                                    contextIds = if (cId != null) listOf(cId) else emptyList()
                                ))
                                appDatabase.waitingForDao().insertWaiting(WaitingFor(
                                    id = "w-${System.currentTimeMillis()}", taskId = task.id,
                                    person = person, dateDelegated = System.currentTimeMillis(),
                                    reminderDate = dueDate,
                                    expectedResponse = null, resolvedAt = null
                                )); reload()
                            },
                            onSomedayTask = { task ->
                                appDatabase.taskDao().updateTask(task.copy(isInbox = false, isSomeday = true, priority = TaskPriority.LOW, dueDate = null, durationMinutes = 0))
                                reload()
                            },
                            onDeleteTask = { id ->
                                appDatabase.taskDao().deleteTask(id); reload()
                            },
                            onUpdateTask = onUpdateTask
                        )

                        Tab.LISTS -> {
                            val list = activeListDetail
                            if (list != null) {
                                CustomListDetailScreen(
                                    list = list,
                                    appDatabase = appDatabase,
                                    onBack = { activeListDetail = null },
                                    onRenameList = { newName ->
                                        appDatabase.customListDao().renameCustomList(list.id, newName)
                                        activeListDetail = appDatabase.customListDao().getCustomLists().find { it.id == list.id }
                                        reload()
                                    },
                                    onDeleteList = {
                                        appDatabase.customListDao().deleteCustomList(list.id)
                                        activeListDetail = null
                                        reload()
                                    },
                                    showToast = showToast
                                )
                            } else {
                                ListsScreen(
                                    lists = filteredLists,
                                    appDatabase = appDatabase,
                                    onListClick = {
                                        activeListDetail = it
                                        showSearchBar = false
                                    },
                                    onDeleteList = { listId ->
                                        appDatabase.customListDao().deleteCustomList(listId)
                                        reload()
                                        showToast("List deleted")
                                    }
                                )
                            }
                        }
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
                appDatabase.taskDao().insertTask(Task(
                    id = "t-${System.currentTimeMillis()}", projectId = null, title = title,
                    notes = null, priority = TaskPriority.LOW, energy = TaskEnergy.LOW,
                    durationMinutes = 15, dueDate = null, startDate = null, completedAt = null,
                    isInbox = true, isSomeday = false, recurrenceRule = null,
                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                ))
                reload(); showAddSheet = false; showToast("Captured to Tasks ✓")
            }
        )
    }

    if (showAddProjectSheet) {
        AddProjectSheet(onDismiss = { showAddProjectSheet = false }, onSave = { title ->
            appDatabase.projectDao().insertProject(Project(
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

    if (showAddListSheet) {
        AddListSheet(onDismiss = { showAddListSheet = false }, onSave = { name ->
            appDatabase.customListDao().insertCustomList(CustomList(
                id = "cl-${UUID.randomUUID()}",
                name = name,
                createdAt = System.currentTimeMillis()
            ))
            reload()
            showToast("List created ✓")
            showAddListSheet = false
            activeListDetail = appDatabase.customListDao().getCustomLists().firstOrNull { it.name == name }
        })
    }

    val currentProject = activeProjectDetail
    val currentList = activeListDetail
    val isProjectActive = currentProject != null && activeTab == Tab.PROJECTS
    val isListActive = currentList != null && activeTab == Tab.LISTS
    // Back handler: project/list detail -> active tab -> previous tab -> exit
    BackHandler(enabled = isProjectActive || isListActive || tabHistory.isNotEmpty()) {
        when {
            isProjectActive -> activeProjectDetail = null
            isListActive -> activeListDetail = null
            tabHistory.isNotEmpty() -> activeTab = tabHistory.removeLast()
        }
    }
}
