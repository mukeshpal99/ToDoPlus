# AeroGTD Android (Kotlin + Jetpack Compose)

An offline-first, calm productivity application inspired by Atul Gawande's *"The Checklist Manifesto"* and David Allen's *"Getting Things Done (GTD)"*.

Built natively for Android using Kotlin, Jetpack Compose, Material 3, and a local SQLite database helper.

---

## 1. Project Architecture

The application is structured into domain features and core layers:

```
app/src/main/
├── AndroidManifest.xml           # App declarations
├── java/com/aerogtd/
│   ├── MainActivity.kt           # Central activity hosting Compose UI and tab states
│   ├── core/
│   │   └── database/
│   │       ├── DatabaseHelper.kt # SQLite schema manager and database helper (CRUD operations)
│   │       └── Models.kt         # Data model entities and priority/energy enums
│   ├── features/
│   │   ├── dashboard/
│   │   │   └── presentation/
│   │   │       └── NextActionEngine.kt # GTD scoring logic for task recommendations
│   │   ├── home/
│   │   │   └── presentation/
│   │   │       ├── TodayScreen.kt      # Main Dashboard view showing focus tasks and recommended next action
│   │   │       └── WeeklyReviewSheet.kt # Guided weekly review workflow
│   │   ├── inbox/
│   │   │   └── presentation/
│   │   │       └── InboxScreen.kt      # Brain dump screen with New, Reviewed, and Done tabs
│   │   ├── project/
│   │   │   └── presentation/
│   │   │       ├── ProjectsScreen.kt   # Projects management divided into In Progress / Completed
│   │   │       └── ProjectDetailScreen.kt # Details, checklists, and manual progression for a project
│   │   └── waiting/
│   │       └── presentation/
│   │           └── WaitingScreen.kt    # Delegation dashboard tracking pending and resolved delegations
│   └── ui/
│       ├── components/
│       │   ├── ClarifySheet.kt   # Sequenced GTD wizard for processing & clarifying tasks
│       │   └── QuickCaptureSheet.kt # Simple bottom sheet for quick brain dumps
│       └── theme/
│           ├── Color.kt          # Sleek styling palettes (light and dark theme support)
│           └── Theme.kt          # Material 3 setup overrides
└── res/
    └── values/
        ├── strings.xml           # Strings mapping
        └── themes.xml            # Styling base reference
```

---

## 2. Core GTD & Checklist Implementations

*   **Brain Dumps Capture (Inbox)**: Captures raw inputs with zero mandatory metadata.
*   **Step-by-Step Clarification Wizard**: Evaluates inbox items sequentially. Non-actionable items go to Trash/Someday/Reference Note, and actionable items can be completed immediately under the 2-minute rule, or delegated/scheduled.
*   **Recommendation Scorer (Next Action Engine)**: Scopes tasks against context/energy filters, checks completed dependencies, and sorts using overdue parameters to recommend **exactly one** action.
*   **Procedure Manuals**: Spawns sequential checklists (Read-Do locks) and checklists validation logs (Do-Confirm sheets).
*   **Weekly guided Review Walkthrough**: Follows the 5-step maintenance guidelines to clear inputs, check project integrity alerts, and follow up delegations.

---

## 3. Recently Added Features

*   **Floating Search FAB & Real-time Filtering**:
    *   Tapping the secondary floating action button (located right above the "+" FAB) expands a top-level search bar.
    *   Entering text dynamically filters tasks (by title/notes) and projects (by title/goals or whether they contain matching tasks) instantly across all screens (Today, Inbox, Projects, Waiting).
*   **Divided Projects Tab**:
    *   Projects are now separated into capsule-based sub-tabs: **In Progress** and **Completed**.
    *   **Project Completion Validation**: Projects can only be marked completed if all underlying tasks are done. If any incomplete tasks exist, a prompt blocks the action.
    *   **Optimized Project Cards**: Features bottom-aligned linear progress indicators to maximize project title text visibility and a compact height layout.
*   **Someday Task Enhancements**:
    *   **"Act Now" Option**: Someday tasks can be promoted back to active Next Actions using the **Act Now** button on the task detail view screen.
    *   **Filtered Save Flow**: Normal edits to someday tasks preserve the someday state instead of accidentally moving them to active actions.
    *   **Someday Filter in Inbox**: Tapping the **Someday** filter chip on the Inbox/Reviewed screen filters the list to show only someday items.
*   **Wizard Inline Editing**:
    *   Task titles can be edited inline inside Step 0 of the Clarify wizard and saved directly using a green tick icon.
*   **Done Tab Ordering**:
    *   Tasks inside the Inbox/Done screen are ordered dynamically by recently completed timestamp (completed on top).

---

## 4. Getting Started in Android Studio

1.  Open **Android Studio**.
2.  Select **File -> Open** (or **Open an Existing Project**).
3.  Choose the root folder of this workspace: `/Users/mukeshpal/Desktop/task-manager`.
4.  Android Studio will automatically detect the Gradle files (`settings.gradle`, `build.gradle`), resolve dependencies, and download the SDK variables.
5.  Click the **Run (Green Play)** button to build and run the app on an Android Emulator or connected physical device.
