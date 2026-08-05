package com.aerogtd.core.database

data class Context(
    val id: String,
    val name: String,
    val icon: String?,
    val colorHex: String?
)

data class Tag(
    val id: String,
    val name: String,
    val colorHex: String?
)

enum class ProjectStatus {
    ACTIVE, ON_HOLD, COMPLETED, ARCHIVED
}

data class Project(
    val id: String,
    val title: String,
    val goal: String?,
    val outcome: String?,
    val deadline: Long?,
    val status: ProjectStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class TaskEnergy {
    LOW, MEDIUM, HIGH
}

data class Task(
    val id: String,
    val projectId: String?,
    val title: String,
    val notes: String?,
    val priority: TaskPriority,
    val energy: TaskEnergy,
    val durationMinutes: Int,
    val dueDate: Long?,
    val startDate: Long?,
    val completedAt: Long?,
    val isInbox: Boolean,
    val isSomeday: Boolean,
    val recurrenceRule: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val contextIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val dependencyIds: List<String> = emptyList()
)

data class WaitingFor(
    val id: String,
    val taskId: String?,
    val person: String,
    val dateDelegated: Long,
    val reminderDate: Long?,
    val expectedResponse: String?,
    val resolvedAt: Long?
)
