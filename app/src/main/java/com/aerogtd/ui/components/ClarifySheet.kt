package com.aerogtd.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerogtd.core.database.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClarifySheet(
    task: Task,
    projects: List<Project>,
    contexts: List<Context>,
    waitingList: List<WaitingFor>,
    onDismiss: () -> Unit,
    onAddContext: (String) -> Unit,
    onProcess: (String, TaskPriority, TaskEnergy, Int, Long?, String?) -> Unit,
    onDelegate: (String, String, TaskPriority, Long?, String?) -> Unit,
    onSomeday: (String, TaskPriority, Long?) -> Unit,
    onTrash: () -> Unit,
    forceReadOnly: Boolean = false,
    onUpdateTitle: ((String) -> Unit)? = null,
    onActNow: ((String, TaskPriority, TaskEnergy, Int, Long?, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 0=Route, 1=NextAction detail, 2=Delegate detail
    var step by remember { mutableIntStateOf(if (task.isInbox) 0 else 1) }
    var priority by remember { mutableStateOf(task.priority) }
    var energy by remember { mutableStateOf(task.energy) }
    var selectedDuration by remember(task) {
        mutableStateOf(if (task.durationMinutes > 0) task.durationMinutes else 30)
    }
    var isEditMode by remember(forceReadOnly) { mutableStateOf(!forceReadOnly) }
    val isCompleted = task.completedAt != null
    val isReadOnly = isCompleted || !isEditMode
    var selectedCtx by remember { mutableStateOf<String?>(task.contextIds.firstOrNull()) }
    var selectedDueDate by remember { mutableStateOf<Long?>(task.dueDate) }
    var delegatePerson by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf(task.title) }

    val delegateNames = remember(waitingList) {
        waitingList.map { it.person }.distinct().sorted()
    }
    var menuExpanded by remember { mutableStateOf(false) }

    var showAddCtxDialog by remember { mutableStateOf(false) }
    var newCtxName by remember { mutableStateOf("") }
    var pendingSelectCtxName by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Auto-select newly created context when list updates
    LaunchedEffect(contexts) {
        pendingSelectCtxName?.let { name ->
            contexts.find { it.name.equals(name, ignoreCase = true) }?.let {
                selectedCtx = it.id
                pendingSelectCtxName = null
            }
        }
    }

    // Date formatter for display
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()) }

    // DatePickerDialog launcher
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance().apply { set(year, month, day, 23, 59, 0); set(Calendar.MILLISECOND, 0) }
            selectedDueDate = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).also { it.datePicker.minDate = System.currentTimeMillis() - 1000 }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(36.dp).height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.15f), CircleShape))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp).padding(bottom = 36.dp)) {

            // Header row: title + delete or edit action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Task Detail", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onUpdateTitle != null && !isReadOnly && titleText.isNotBlank() && titleText != task.title) {
                        IconButton(
                            onClick = {
                                onUpdateTitle.invoke(titleText)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF4CAF50).copy(0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Name",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (!isCompleted) {
                        if (isReadOnly) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(0.08f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Task",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { isEditMode = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(0.08f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Task",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(0.08f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Task",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.error.copy(0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(0.08f), RoundedCornerShape(10.dp))
                .padding(if (isCompleted) 12.dp else 4.dp)) {
                Column {
                    if (isCompleted) {
                        Text(task.title, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary, maxLines = 2)
                    } else {
                        BasicTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            singleLine = true,
                            readOnly = isReadOnly
                        )
                    }

                    val projName = task.projectId?.let { pId -> projects.find { it.id == pId }?.title }
                    val delegation = waitingList.find { it.taskId == task.id && it.resolvedAt == null }
                    if (projName != null || delegation != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            projName?.let {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Project: $it", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            delegation?.let {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF8F00).copy(0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Delegated to: ${it.person}", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF8F00))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            when (step) {
                // Step 0: Pick route
                0 -> {
                    Text("WHAT IS THIS?", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(12.dp))

                    ClarifyOptionButton(Icons.Default.FlashOn, "Do it now (< 2 min)",
                        "Complete immediately") {
                        onProcess(titleText, TaskPriority.LOW, TaskEnergy.LOW, 2, null, null)
                    }
                    Spacer(Modifier.height(8.dp))
                    ClarifyOptionButton(Icons.AutoMirrored.Filled.ArrowForward, "Next Action",
                        "Add to my action list") { step = 1 }
                    Spacer(Modifier.height(8.dp))
                    ClarifyOptionButton(Icons.Default.PersonAdd, "Delegate",
                        "Someone else should do this") { step = 2 }
                    Spacer(Modifier.height(8.dp))
                    ClarifyOptionButton(Icons.Default.WatchLater, "Someday",
                        "Park it for later review") { onSomeday(titleText, TaskPriority.LOW, null) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onTrash, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.4f))) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Trash — not needed", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Step 1: Next Action details
                1 -> {
                    Text("ASSIGN DETAILS", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(16.dp))

                    val isDurationReadOnly = isReadOnly || task.durationMinutes == 2

                    // Priority Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Priority", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TaskPriority.values().forEach { p ->
                                val color = when(p) {
                                    TaskPriority.HIGH -> Color(0xFFE53935)
                                    TaskPriority.MEDIUM -> Color(0xFFFF8F00)
                                    TaskPriority.LOW -> Color(0xFF4CAF50)
                                }
                                FilterChip(selected = priority == p, onClick = { priority = p },
                                    enabled = !isReadOnly,
                                    label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium) },
                                    leadingIcon = {
                                        if (priority == p) Icon(Icons.Default.Circle, null,
                                            tint = color, modifier = Modifier.size(8.dp))
                                    },
                                    shape = RoundedCornerShape(8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    if (!task.isSomeday) {
                        // Duration Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            
                            if (isDurationReadOnly) {
                                val durationText = if (selectedDuration >= 60 && selectedDuration % 60 == 0) "${selectedDuration / 60} hr" else "$selectedDuration min"
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                var durationDropdownExpanded by remember { mutableStateOf(false) }
                                val baseOptions = listOf(30, 60, 120, 240)
                                val durationOptions = remember(task) {
                                    val currentDur = if (task.durationMinutes > 0) task.durationMinutes else 30
                                    if (currentDur !in baseOptions) {
                                        (baseOptions + currentDur).sorted()
                                    } else {
                                        baseOptions
                                    }
                                }

                                fun formatDurationLabel(mins: Int): String {
                                    return when (mins) {
                                        30 -> "30 Min"
                                        60 -> "01 Hr"
                                        120 -> "02 Hr"
                                        240 -> "04 Hr"
                                        else -> {
                                            if (mins % 60 == 0) {
                                                val hrs = mins / 60
                                                String.format("%02d Hr", hrs)
                                            } else {
                                                "$mins Min"
                                            }
                                        }
                                    }
                                }

                                Box {
                                    Surface(
                                        onClick = { durationDropdownExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.width(120.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = formatDurationLabel(selectedDuration),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Duration",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = durationDropdownExpanded,
                                        onDismissRequest = { durationDropdownExpanded = false }
                                    ) {
                                        durationOptions.forEach { mins ->
                                            DropdownMenuItem(
                                                text = { Text(formatDurationLabel(mins)) },
                                                onClick = {
                                                    selectedDuration = mins
                                                    durationDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Context Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Context", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            modifier = Modifier.width(70.dp))
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            contexts.forEach { ctx ->
                                FilterChip(selected = selectedCtx == ctx.id,
                                    enabled = !isReadOnly,
                                    onClick = { selectedCtx = if (selectedCtx == ctx.id) null else ctx.id },
                                    label = { Text("@${ctx.name}", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp))
                            }
                            FilterChip(
                                selected = false,
                                enabled = !isReadOnly,
                                onClick = { showAddCtxDialog = true },
                                label = { Text("+ Context", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Due Date Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Due Date", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Surface(
                            onClick = { datePickerDialog.show() },
                            enabled = !isReadOnly,
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(180.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Pick date",
                                    tint = if (selectedDueDate != null) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (selectedDueDate != null)
                                               dateFormatter.format(Date(selectedDueDate!!))
                                           else "No due date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedDueDate != null) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedDueDate != null && !isReadOnly) {
                                    IconButton(
                                        onClick = { selectedDueDate = null },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Clear date",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                            modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    val showReopenButton = task.completedAt != null
                    val showSomeday = !task.isInbox && !task.isSomeday && !isCompleted && isReadOnly
                    val showActNow = task.isSomeday && !isCompleted && isReadOnly && onActNow != null

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isReadOnly && !showReopenButton) {
                            if (showSomeday) {
                                OutlinedButton(
                                    onClick = {
                                        onSomeday(titleText, TaskPriority.LOW, null)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8F00)),
                                    border = BorderStroke(1.dp, Color(0xFFFF8F00).copy(0.4f))
                                ) {
                                    Icon(Icons.Default.WatchLater, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF8F00))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Someday", style = MaterialTheme.typography.labelLarge)
                                }
                            } else if (showActNow) {
                                OutlinedButton(
                                    onClick = {
                                        onActNow?.invoke(titleText, priority, energy, selectedDuration, selectedDueDate, selectedCtx)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f))
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Act Now", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = if (showSomeday || showActNow) Modifier.weight(1f).height(50.dp) else Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = {
                                    onProcess(titleText, priority, energy, selectedDuration, selectedDueDate, selectedCtx)
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (showReopenButton) "Mark Incomplete"
                                           else if (task.isInbox) "Save to Actions"
                                           else "Save",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Step 2: Delegate details
                2 -> {
                    Text("DELEGATE DETAILS", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(16.dp))

                    val suggestions = remember(delegatePerson, delegateNames) {
                        if (delegatePerson.isBlank()) emptyList()
                        else delegateNames.filter { it.startsWith(delegatePerson, ignoreCase = true) && !it.equals(delegatePerson, ignoreCase = true) }
                    }

                    ExposedDropdownMenuBox(
                        expanded = menuExpanded && suggestions.isNotEmpty(),
                        onExpandedChange = { menuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = delegatePerson,
                            onValueChange = {
                                delegatePerson = it
                                menuExpanded = it.isNotBlank()
                            },
                            label = { Text("Person / Team") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f))
                        )
                        
                        ExposedDropdownMenu(
                            expanded = menuExpanded && suggestions.isNotEmpty(),
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            suggestions.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        delegatePerson = name
                                        menuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Text("Priority", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.values().forEach { p ->
                            val color = when(p) {
                                TaskPriority.HIGH -> Color(0xFFE53935)
                                TaskPriority.MEDIUM -> Color(0xFFFF8F00)
                                TaskPriority.LOW -> Color(0xFF4CAF50)
                            }
                            FilterChip(selected = priority == p, onClick = { priority = p },
                                label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    if (priority == p) Icon(Icons.Default.Circle, null,
                                        tint = color, modifier = Modifier.size(8.dp))
                                },
                                shape = RoundedCornerShape(8.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Text("Context", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        contexts.forEach { ctx ->
                            FilterChip(selected = selectedCtx == ctx.id,
                                onClick = { selectedCtx = if (selectedCtx == ctx.id) null else ctx.id },
                                label = { Text("@${ctx.name}", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp))
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showAddCtxDialog = true },
                            label = { Text("+ Add Context", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    Text("Due Date", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { datePickerDialog.show() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick date",
                                tint = if (selectedDueDate != null) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (selectedDueDate != null)
                                           dateFormatter.format(Date(selectedDueDate!!))
                                       else "No due date  (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedDueDate != null) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                            )
                            Spacer(Modifier.weight(1f))
                            if (selectedDueDate != null) {
                                IconButton(
                                    onClick = { selectedDueDate = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Clear date",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                        modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = { if (delegatePerson.isNotBlank()) onDelegate(titleText, delegatePerson, priority, selectedDueDate, selectedCtx) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = delegatePerson.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Delegate & Track", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${task.title}\" will be permanently deleted. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onTrash() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showAddCtxDialog) {
        AlertDialog(
            onDismissRequest = { showAddCtxDialog = false; newCtxName = "" },
            title = { Text("Add Context") },
            text = {
                Column {
                    Text("Enter a name for the new context (e.g. Computer, Errands, Phone):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCtxName,
                        onValueChange = { newCtxName = it },
                        singleLine = true,
                        placeholder = { Text("Context Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCtxName.isNotBlank()) {
                            pendingSelectCtxName = newCtxName.trim()
                            onAddContext(newCtxName.trim())
                            showAddCtxDialog = false
                            newCtxName = ""
                        }
                    },
                    enabled = newCtxName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCtxDialog = false; newCtxName = "" }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ClarifyOptionButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp)
                .background(MaterialTheme.colorScheme.primary.copy(0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(18.dp))
        }
    }
}
