package com.aerogtd.core.database

import android.content.Context as AndroidContext
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── DAOs ────────────────────────────────────────────────────────────────────

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts")
    fun getContexts(): List<Context>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContext(context: Context)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    fun getTags(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTag(tag: Tag)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    fun getProjects(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :projectId")
    fun deleteProject(projectId: String)

    @Update
    fun updateProject(project: Project)
}

@Dao
abstract class TaskDao {
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    abstract fun getRawTasks(): List<Task>

    @Query("SELECT context_id FROM task_contexts WHERE task_id = :taskId")
    abstract fun getContextIdsForTask(taskId: String): List<String>

    @Query("SELECT dependency_task_id FROM task_dependencies WHERE task_id = :taskId")
    abstract fun getDependencyIdsForTask(taskId: String): List<String>

    @Transaction
    open fun getTasks(): List<Task> {
        val raw = getRawTasks()
        return raw.map { task ->
            task.copy(
                contextIds = getContextIdsForTask(task.id),
                dependencyIds = getDependencyIdsForTask(task.id)
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertRawTask(task: Task)

    @Query("DELETE FROM task_contexts WHERE task_id = :taskId")
    abstract fun deleteContextsForTask(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTaskContexts(crossRefs: List<TaskContextCrossRef>)

    @Query("DELETE FROM task_dependencies WHERE task_id = :taskId")
    abstract fun deleteDependenciesForTask(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTaskDependencies(crossRefs: List<TaskDependencyCrossRef>)

    @Transaction
    open fun insertTask(task: Task) {
        insertRawTask(task)
        
        deleteContextsForTask(task.id)
        if (task.contextIds.isNotEmpty()) {
            insertTaskContexts(task.contextIds.map { TaskContextCrossRef(task.id, it) })
        }

        deleteDependenciesForTask(task.id)
        if (task.dependencyIds.isNotEmpty()) {
            insertTaskDependencies(task.dependencyIds.map { TaskDependencyCrossRef(task.id, it) })
        }
    }

    @Transaction
    open fun updateTask(task: Task) {
        insertTask(task)
    }

    @Query("DELETE FROM tasks WHERE id = :taskId")
    abstract fun deleteRawTask(taskId: String)

    @Transaction
    open fun deleteTask(taskId: String) {
        deleteContextsForTask(taskId)
        deleteDependenciesForTask(taskId)
        deleteRawTask(taskId)
    }
}

@Dao
interface WaitingForDao {
    @Query("SELECT * FROM waiting_for WHERE task_id IS NOT NULL")
    fun getWaitingList(): List<WaitingFor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWaiting(waiting: WaitingFor)

    @Update
    fun updateWaiting(waiting: WaitingFor)

    @Query("DELETE FROM waiting_for WHERE task_id = :taskId")
    fun deleteWaitingForTask(taskId: String)
}

@Dao
interface CustomListDao {
    @Query("SELECT * FROM custom_lists ORDER BY created_at DESC")
    fun getCustomLists(): List<CustomList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCustomList(list: CustomList)

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    fun deleteCustomList(listId: String)

    @Query("UPDATE custom_lists SET name = :newName WHERE id = :listId")
    fun renameCustomList(listId: String, newName: String)
}

@Dao
interface CustomListItemDao {
    @Query("SELECT * FROM custom_list_items WHERE list_id = :listId ORDER BY created_at ASC")
    fun getCustomListItems(listId: String): List<CustomListItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCustomListItem(item: CustomListItem)

    @Update
    fun updateCustomListItem(item: CustomListItem)

    @Query("DELETE FROM custom_list_items WHERE id = :itemId")
    fun deleteCustomListItem(itemId: String)
}

// ─── DATABASE ────────────────────────────────────────────────────────────────

@Database(
    entities = [
        Context::class, Tag::class, Project::class, Task::class,
        WaitingFor::class, CustomList::class, CustomListItem::class,
        TaskContextCrossRef::class, TaskTagCrossRef::class, TaskDependencyCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contextDao(): ContextDao
    abstract fun tagDao(): TagDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun waitingForDao(): WaitingForDao
    abstract fun customListDao(): CustomListDao
    abstract fun customListItemDao(): CustomListItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: AndroidContext): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aerogtd.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default contexts
                        db.execSQL("INSERT INTO contexts (id, name, icon, color_hex) VALUES ('c-1', 'Home', 'home', '#4A90E2')")
                        db.execSQL("INSERT INTO contexts (id, name, icon, color_hex) VALUES ('c-2', 'Office', 'briefcase', '#50E3C2')")
                        db.execSQL("INSERT INTO contexts (id, name, icon, color_hex) VALUES ('c-3', 'Personal', 'user', '#BD10E0')")
                    }
                })
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
