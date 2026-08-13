package com.aerogtd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerogtd.core.database.Context
import com.aerogtd.core.database.Project
import com.aerogtd.core.database.Task
import com.aerogtd.core.database.TaskPriority
import com.aerogtd.core.database.WaitingFor

@Composable
fun FocusCard(task: Task, projects: List<Project>, contexts: List<Context>, waitingList: List<WaitingFor>, onComplete: () -> Unit) {
    val ctxName = task.contextIds.firstOrNull()?.let { id -> contexts.find { it.id == id }?.name }
    val delegation = waitingList.find { it.taskId == task.id && it.resolvedAt == null }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .background(Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("FOCUS NOW", style = MaterialTheme.typography.labelSmall,
                        color = Color.White, letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                ctxName?.let {
                    Text("@$it", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(task.title, style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            val projName = task.projectId?.let { pId -> projects.find { it.id == pId }?.title }
            val subtitle = buildString {
                if (projName != null) {
                    append("$projName  •  ")
                }
                if (delegation != null) {
                    append("Delegated to ${delegation.person}  •  ")
                }
                append("${task.durationMinutes} min  •  ${task.priority.name.lowercase().replaceFirstChar { it.uppercase() }} priority")
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark Complete", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun StatPill(modifier: Modifier = Modifier, value: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(task: Task, projects: List<Project>, contexts: List<Context>, waitingList: List<WaitingFor>, onComplete: () -> Unit, onTap: () -> Unit) {
    val ctxName = task.contextIds.firstOrNull()?.let { id -> contexts.find { it.id == id }?.name }
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFE53935)
        TaskPriority.MEDIUM -> Color(0xFFFF8F00)
        TaskPriority.LOW -> Color(0xFF4CAF50)
    }
    val delegation = waitingList.find { it.taskId == task.id && it.resolvedAt == null }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) { onComplete(); true }
            else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color(0xFF4CAF50).copy(0.15f), RoundedCornerShape(12.dp))
                .padding(start = 20.dp), contentAlignment = Alignment.CenterStart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(8.dp))
                    Text("Complete", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4CAF50))
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onTap() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                // Priority dot
                Box(modifier = Modifier.size(8.dp).background(priorityColor, CircleShape))
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val projName = task.projectId?.let { pId -> projects.find { it.id == pId }?.title }
                    if (ctxName != null || task.durationMinutes > 0 || projName != null || delegation != null) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            projName?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            delegation?.let {
                                val prefix = if (projName != null) "•  " else ""
                                Text("${prefix}Delegated to ${it.person}", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF8F00))
                            }
                            ctxName?.let {
                                val prefix = if (projName != null || delegation != null) "•  @" else "@"
                                Text("$prefix$it", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary.copy(0.8f))
                            }
                            if (task.durationMinutes > 0) {
                                val prefix = if (projName != null || ctxName != null || delegation != null) "•  " else ""
                                Text("$prefix${task.durationMinutes}m", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            }
                            if (task.imagePath != null) {
                                val prefix = if (projName != null || ctxName != null || delegation != null || task.durationMinutes > 0) "•  " else ""
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(prefix, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                    Icon(
                                        imageVector = Icons.Default.Attachment,
                                        contentDescription = "Has image attachment",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text("Image", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun InboxTaskCard(
    task: Task,
    projects: List<Project>,
    contexts: List<Context>,
    waitingList: List<WaitingFor>,
    onTap: () -> Unit,
    onEditTap: () -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onTrash: () -> Unit
) {
    val ctxName = task.contextIds.firstOrNull()?.let { id -> contexts.find { it.id == id }?.name }
    val delegation = remember(task, waitingList) {
        waitingList.filter { it.taskId == task.id }.run {
            find { it.resolvedAt == null } ?: lastOrNull()
        }
    }
    val isCompleted = task.completedAt != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompleted) {
                IconButton(
                    onClick = { onToggleComplete(false) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle, contentDescription = "Complete",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (task.isInbox) {
                IconButton(
                    onClick = { onToggleComplete(true) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Circle, contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggleComplete(true) },
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(0.4f) else MaterialTheme.colorScheme.onSurface
                )
                val projName = task.projectId?.let { pId -> projects.find { it.id == pId }?.title }
                if (!task.isInbox || projName != null || delegation != null) {
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        projName?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        delegation?.let {
                            val prefix = if (projName != null) "•  " else ""
                            Text("${prefix}Delegated to ${it.person}", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF8F00))
                        }
                        if (!task.isInbox) {
                            ctxName?.let {
                                val prefix = if (projName != null || delegation != null) "•  @" else "@"
                                Text("$prefix$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (task.durationMinutes > 0) {
                                val prefix = if (projName != null || ctxName != null || delegation != null) "•  " else ""
                                Text("$prefix${task.durationMinutes}m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            }
                            if (task.isSomeday) {
                                Text("•  Someday", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF8F00))
                            }
                        }
                    }
                }
            }
            if (isCompleted) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                    fontWeight = FontWeight.SemiBold
                )
            } else if (task.isInbox) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Review →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onEditTap() }
                )
            } else {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onEditTap,
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

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(0.08f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(0.5f),
                    modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 22.sp)
        }
    }
}
