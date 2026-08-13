package com.aerogtd.core.database

import androidx.room.*

@Entity(tableName = "contexts", indices = [Index(value = ["name"], unique = true)])
data class Context(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String?,
    @ColumnInfo(name = "color_hex") val colorHex: String?
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String?
)

enum class ProjectStatus {
    ACTIVE, ON_HOLD, COMPLETED, ARCHIVED
}

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val title: String,
    val goal: String?,
    val outcome: String?,
    val deadline: Long?,
    val status: ProjectStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class TaskEnergy {
    LOW, MEDIUM, HIGH
}

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["project_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Task(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?,
    val title: String,
    val notes: String?,
    val priority: TaskPriority,
    val energy: TaskEnergy,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "due_date") val dueDate: Long?,
    @ColumnInfo(name = "start_date") val startDate: Long?,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "is_inbox") val isInbox: Boolean,
    @ColumnInfo(name = "is_someday") val isSomeday: Boolean,
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @Ignore val contextIds: List<String> = emptyList(),
    @Ignore val tagIds: List<String> = emptyList(),
    @Ignore val dependencyIds: List<String> = emptyList()
) {
    // Secondary constructor required by Room to bypass ignored lists
    constructor(
        id: String, projectId: String?, title: String, notes: String?,
        priority: TaskPriority, energy: TaskEnergy, durationMinutes: Int,
        dueDate: Long?, startDate: Long?, completedAt: Long?,
        isInbox: Boolean, isSomeday: Boolean, recurrenceRule: String?,
        createdAt: Long, updatedAt: Long, imagePath: String?
    ) : this(
        id, projectId, title, notes, priority, energy, durationMinutes,
        dueDate, startDate, completedAt, isInbox, isSomeday, recurrenceRule,
        createdAt, updatedAt, imagePath, emptyList(), emptyList(), emptyList()
    )
}

@Entity(
    tableName = "waiting_for",
    indices = [Index(value = ["task_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WaitingFor(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String?,
    val person: String,
    @ColumnInfo(name = "date_delegated") val dateDelegated: Long,
    @ColumnInfo(name = "reminder_date") val reminderDate: Long?,
    @ColumnInfo(name = "expected_response") val expectedResponse: String?,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long?
)

@Entity(tableName = "custom_lists")
data class CustomList(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "custom_list_items",
    indices = [Index(value = ["list_id"])],
    foreignKeys = [
        ForeignKey(
            entity = CustomList::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomListItem(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id") val listId: String,
    val name: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// Joint tables mapping
@Entity(
    tableName = "task_contexts",
    primaryKeys = ["task_id", "context_id"],
    indices = [Index(value = ["context_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskContextCrossRef(
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "context_id") val contextId: String
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["task_id", "tag_id"],
    indices = [Index(value = ["tag_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskTagCrossRef(
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "tag_id") val tagId: String
)

@Entity(
    tableName = "task_dependencies",
    primaryKeys = ["task_id", "dependency_task_id"],
    indices = [Index(value = ["dependency_task_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["dependency_task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskDependencyCrossRef(
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "dependency_task_id") val dependencyTaskId: String
)
