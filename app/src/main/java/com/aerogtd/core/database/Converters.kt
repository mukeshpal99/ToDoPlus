package com.aerogtd.core.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromProjectStatus(status: ProjectStatus): String = status.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    @TypeConverter
    fun fromTaskEnergy(energy: TaskEnergy): String = energy.name

    @TypeConverter
    fun toTaskEnergy(value: String): TaskEnergy = TaskEnergy.valueOf(value)
}
