package com.aerogtd.core.database

import android.content.ContentValues
import android.content.Context as AndroidContext
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: AndroidContext) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "aerogtd.db"
        private const val DATABASE_VERSION = 4

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: AndroidContext): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE contexts (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                icon TEXT,
                color_hex TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE tags (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                color_hex TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE projects (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                goal TEXT,
                outcome TEXT,
                deadline INTEGER,
                status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED')),
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE tasks (
                id TEXT PRIMARY KEY,
                project_id TEXT,
                title TEXT NOT NULL,
                notes TEXT,
                priority TEXT NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
                energy TEXT NOT NULL CHECK (energy IN ('LOW', 'MEDIUM', 'HIGH')),
                duration_minutes INTEGER DEFAULT 0,
                due_date INTEGER,
                start_date INTEGER,
                completed_at INTEGER,
                is_inbox INTEGER DEFAULT 1,
                is_someday INTEGER DEFAULT 0,
                recurrence_rule TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE task_dependencies (
                task_id TEXT NOT NULL,
                dependency_task_id TEXT NOT NULL,
                PRIMARY KEY (task_id, dependency_task_id),
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (dependency_task_id) REFERENCES tasks(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE task_contexts (
                task_id TEXT NOT NULL,
                context_id TEXT NOT NULL,
                PRIMARY KEY (task_id, context_id),
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (context_id) REFERENCES contexts(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE task_tags (
                task_id TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                PRIMARY KEY (task_id, tag_id),
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE waiting_for (
                id TEXT PRIMARY KEY,
                task_id TEXT,
                person TEXT NOT NULL,
                date_delegated INTEGER NOT NULL,
                reminder_date INTEGER,
                expected_response TEXT,
                resolved_at INTEGER,
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)

        seedDefaultContexts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Turn off FK enforcement so we can drop in any order
        db.execSQL("PRAGMA foreign_keys = OFF")
        // Drop every possible table (IF EXISTS is safe if table doesn't exist)
        listOf(
            "checklist_items_instance", "checklist_instances",
            "checklist_items_template", "checklist_templates",
            "notes", "settings", "waiting_for",
            "task_tags", "task_contexts", "task_dependencies",
            "tasks", "projects", "tags", "contexts"
        ).forEach { table -> db.execSQL("DROP TABLE IF EXISTS $table") }
        db.execSQL("PRAGMA foreign_keys = ON")
        onCreate(db)
    }

    private fun seedDefaultContexts(db: SQLiteDatabase) {
        val contexts = listOf(
            Context("c-1", "Home",      "home",          "#4A90E2"),
            Context("c-2", "Office",    "briefcase",     "#50E3C2"),
            Context("c-3", "Personal",  "user",          "#BD10E0")
        )
        for (c in contexts) {
            val cv = ContentValues().apply {
                put("id", c.id); put("name", c.name)
                put("icon", c.icon); put("color_hex", c.colorHex)
            }
            db.insert("contexts", null, cv)
        }
    }

    // ── CONTEXTS ──────────────────────────────────────────────────────────────
    fun getContexts(): List<Context> {
        val list = mutableListOf<Context>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM contexts", null)
        if (cursor.moveToFirst()) do {
            list.add(Context(
                id       = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                name     = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                icon     = cursor.getString(cursor.getColumnIndexOrThrow("icon")),
                colorHex = cursor.getString(cursor.getColumnIndexOrThrow("color_hex"))
            ))
        } while (cursor.moveToNext())
        cursor.close()
        return list
    }

    fun insertContext(context: Context) {
        writableDatabase.insert("contexts", null, ContentValues().apply {
            put("id", context.id)
            put("name", context.name)
            put("icon", context.icon)
            put("color_hex", context.colorHex)
        })
    }

    // ── PROJECTS ──────────────────────────────────────────────────────────────
    fun getProjects(): List<Project> {
        val list = mutableListOf<Project>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM projects ORDER BY created_at DESC", null)
        if (cursor.moveToFirst()) do {
            list.add(Project(
                id        = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title     = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                goal      = cursor.getString(cursor.getColumnIndexOrThrow("goal")),
                outcome   = cursor.getString(cursor.getColumnIndexOrThrow("outcome")),
                deadline  = cursor.getLong(cursor.getColumnIndexOrThrow("deadline")).takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("deadline")) },
                status    = ProjectStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            ))
        } while (cursor.moveToNext())
        cursor.close()
        return list
    }

    fun insertProject(project: Project) {
        writableDatabase.insert("projects", null, ContentValues().apply {
            put("id", project.id); put("title", project.title)
            put("goal", project.goal); put("outcome", project.outcome)
            put("deadline", project.deadline); put("status", project.status.name)
            put("created_at", project.createdAt); put("updated_at", project.updatedAt)
        })
    }

    fun deleteProject(projectId: String) {
        writableDatabase.delete("projects", "id = ?", arrayOf(projectId))
    }

    fun updateProject(project: Project) {
        writableDatabase.update("projects", ContentValues().apply {
            put("title", project.title)
            put("goal", project.goal)
            put("outcome", project.outcome)
            put("deadline", project.deadline)
            put("status", project.status.name)
            put("updated_at", System.currentTimeMillis())
        }, "id = ?", arrayOf(project.id))
    }

    // ── TASKS ─────────────────────────────────────────────────────────────────
    fun getTasks(): List<Task> {
        val list = mutableListOf<Task>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tasks ORDER BY created_at DESC", null)
        if (cursor.moveToFirst()) do {
            val taskId = cursor.getString(cursor.getColumnIndexOrThrow("id"))

            val ctxCursor = db.rawQuery("SELECT context_id FROM task_contexts WHERE task_id = ?", arrayOf(taskId))
            val contexts = mutableListOf<String>()
            if (ctxCursor.moveToFirst()) do { contexts.add(ctxCursor.getString(0)) } while (ctxCursor.moveToNext())
            ctxCursor.close()

            val depCursor = db.rawQuery("SELECT dependency_task_id FROM task_dependencies WHERE task_id = ?", arrayOf(taskId))
            val deps = mutableListOf<String>()
            if (depCursor.moveToFirst()) do { deps.add(depCursor.getString(0)) } while (depCursor.moveToNext())
            depCursor.close()

            list.add(Task(
                id              = taskId,
                projectId       = cursor.getString(cursor.getColumnIndexOrThrow("project_id")),
                title           = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                notes           = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                priority        = TaskPriority.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("priority"))),
                energy          = TaskEnergy.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("energy"))),
                durationMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("duration_minutes")),
                dueDate         = cursor.getLong(cursor.getColumnIndexOrThrow("due_date")).takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("due_date")) },
                startDate       = cursor.getLong(cursor.getColumnIndexOrThrow("start_date")).takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("start_date")) },
                completedAt     = cursor.getLong(cursor.getColumnIndexOrThrow("completed_at")).takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("completed_at")) },
                isInbox         = cursor.getInt(cursor.getColumnIndexOrThrow("is_inbox")) == 1,
                isSomeday       = cursor.getInt(cursor.getColumnIndexOrThrow("is_someday")) == 1,
                recurrenceRule  = cursor.getString(cursor.getColumnIndexOrThrow("recurrence_rule")),
                createdAt       = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt       = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                contextIds      = contexts,
                dependencyIds   = deps
            ))
        } while (cursor.moveToNext())
        cursor.close()
        return list
    }

    fun insertTask(task: Task) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insert("tasks", null, taskContentValues(task))
            task.contextIds.forEach { cid ->
                db.insert("task_contexts", null, ContentValues().apply {
                    put("task_id", task.id); put("context_id", cid)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun updateTask(task: Task) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = taskContentValues(task)
            cv.put("updated_at", System.currentTimeMillis())
            cv.remove("id"); cv.remove("created_at")
            db.update("tasks", cv, "id = ?", arrayOf(task.id))
            db.delete("task_contexts", "task_id = ?", arrayOf(task.id))
            task.contextIds.forEach { cid ->
                db.insert("task_contexts", null, ContentValues().apply {
                    put("task_id", task.id); put("context_id", cid)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun deleteTask(taskId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Delete associated waiting_for rows first (covers installs with old SET NULL schema)
            db.delete("waiting_for", "task_id = ?", arrayOf(taskId))
            db.delete("tasks", "id = ?", arrayOf(taskId))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun taskContentValues(task: Task) = ContentValues().apply {
        put("id", task.id); put("project_id", task.projectId)
        put("title", task.title); put("notes", task.notes)
        put("priority", task.priority.name); put("energy", task.energy.name)
        put("duration_minutes", task.durationMinutes)
        put("due_date", task.dueDate); put("start_date", task.startDate)
        put("completed_at", task.completedAt)
        put("is_inbox", if (task.isInbox) 1 else 0)
        put("is_someday", if (task.isSomeday) 1 else 0)
        put("recurrence_rule", task.recurrenceRule)
        put("created_at", task.createdAt); put("updated_at", task.updatedAt)
    }

    // ── WAITING FOR ───────────────────────────────────────────────────────────
    fun getWaitingList(): List<WaitingFor> {
        val list = mutableListOf<WaitingFor>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM waiting_for WHERE task_id IS NOT NULL", null)
        if (cursor.moveToFirst()) do {
            list.add(WaitingFor(
                id               = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                taskId           = cursor.getString(cursor.getColumnIndexOrThrow("task_id")),
                person           = cursor.getString(cursor.getColumnIndexOrThrow("person")),
                dateDelegated    = cursor.getLong(cursor.getColumnIndexOrThrow("date_delegated")),
                reminderDate     = cursor.getLong(cursor.getColumnIndexOrThrow("reminder_date")).takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("reminder_date")) },
                expectedResponse = cursor.getString(cursor.getColumnIndexOrThrow("expected_response")),
                resolvedAt       = cursor.getLong(cursor.getColumnIndexOrThrow("resolved_at")).takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("resolved_at")) }
            ))
        } while (cursor.moveToNext())
        cursor.close()
        return list
    }

    fun insertWaiting(w: WaitingFor) {
        writableDatabase.insert("waiting_for", null, ContentValues().apply {
            put("id", w.id); put("task_id", w.taskId); put("person", w.person)
            put("date_delegated", w.dateDelegated); put("reminder_date", w.reminderDate)
            put("expected_response", w.expectedResponse); put("resolved_at", w.resolvedAt)
        })
    }

    fun updateWaiting(w: WaitingFor) {
        writableDatabase.update("waiting_for", ContentValues().apply {
            put("person", w.person); put("reminder_date", w.reminderDate)
            put("expected_response", w.expectedResponse); put("resolved_at", w.resolvedAt)
        }, "id = ?", arrayOf(w.id))
    }
}
