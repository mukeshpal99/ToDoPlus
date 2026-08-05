package com.aerogtd.features.dashboard.presentation

import com.aerogtd.core.database.Task
import com.aerogtd.core.database.TaskEnergy
import com.aerogtd.core.database.TaskPriority
import kotlin.math.max
import kotlin.math.roundToInt

object NextActionEngine {

    fun getRecommendedTask(
        tasks: List<Task>,
        contextId: String?,
        maxDuration: Int?,
        energyLevel: TaskEnergy?
    ): Task? {
        val now = System.currentTimeMillis()
        
        // 1. Filter out completed, inbox, or someday tasks
        val activeTasks = tasks.filter { t -> 
            t.completedAt == null && !t.isInbox && !t.isSomeday 
        }

        // 2. Filter resolved dependencies
        val availableTasks = activeTasks.filter { t ->
            if (t.dependencyIds.isEmpty()) true
            else {
                // Every dependency task must be completed (have a completedAt timestamp)
                t.dependencyIds.all { depId ->
                    val depTask = tasks.find { it.id == depId }
                    depTask?.completedAt != null
                }
            }
        }

        // 3. User filter matches
        var filtered = availableTasks
        if (!contextId.isNullOrEmpty()) {
            filtered = filtered.filter { t -> t.contextIds.contains(contextId) }
        }
        if (maxDuration != null && maxDuration > 0) {
            filtered = filtered.filter { t -> t.durationMinutes <= maxDuration }
        }
        if (energyLevel != null) {
            filtered = filtered.filter { t -> t.energy == energyLevel }
        }

        if (filtered.isEmpty()) return null

        // 4. Scoring Engine
        val scoredList = filtered.map { t ->
            var score = 0
            
            // Due date calculations
            val dueDate = t.dueDate
            if (dueDate != null) {
                if (dueDate < now) {
                    score += 1000 // Overdue task takes absolute priority
                } else {
                    val msLeft = (dueDate - now).toDouble()
                    val daysLeft = msLeft / (1000 * 60 * 60 * 24)
                    if (daysLeft <= 1.0) {
                        score += 500 // Due today
                    } else {
                        score += (100 / daysLeft).roundToInt()
                    }
                }
            }

            // Priority weights
            when (t.priority) {
                TaskPriority.HIGH -> score += 300
                TaskPriority.MEDIUM -> score += 100
                TaskPriority.LOW -> score += 0
            }

            TaskWithScore(t, score)
        }

        // Sort descending by score, then ascending by created time
        val sorted = scoredList.sortedWith(
            compareByDescending<TaskWithScore> { it.score }
                .thenBy { it.task.createdAt }
        )

        return sorted.firstOrNull()?.task
    }

    private data class TaskWithScore(val task: Task, val score: Int)
}
