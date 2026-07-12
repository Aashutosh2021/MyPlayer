# AI Agent Boot Sequence

## Purpose

This file defines the mandatory startup procedure for every AI agent before making any modification to the project.
Never skip these steps. All memory graph files have been consolidated into the `brain/` directory.

---

# STEP 1 — Load Global Context

Read the following files from the `brain/` directory in order:

1. [brain/PROMPT.md](file:///e:/MyPlayer/brain/PROMPT.md)
2. [brain/20_AI_CONTEXT.md](file:///e:/MyPlayer/brain/20_AI_CONTEXT.md)
3. [brain/01_PROJECT_MEMORY.md](file:///e:/MyPlayer/brain/01_PROJECT_MEMORY.md)
4. [brain/17_PROJECT_RULES.md](file:///e:/MyPlayer/brain/17_PROJECT_RULES.md)
5. [brain/18_CURRENT_STATE.md](file:///e:/MyPlayer/brain/18_CURRENT_STATE.md)
6. [brain/03_FILE_INDEX.md](file:///e:/MyPlayer/brain/03_FILE_INDEX.md)
7. .gemini/skills/skills-lock.json
8. [.gemini/skills](gemini/skills)
9. [.gemini/SYSTEM_PROMPT.md]()

These files contain the compressed memory of the entire project. Do NOT scan the full codebase before reading them.

---

# STEP 2 — Understand Request

Identify the user's intent. Possible categories:

* Bug Fix
* New Feature
* UI Change
* Performance Optimization
* Database Change
* API Change
* Security
* Refactoring
* Documentation

---

# STEP 3 — Load Only Required Maps

Based on the task category, read only the relevant brain files:

* UI: [brain/12_UI_MAP.md](file:///e:/MyPlayer/brain/12_UI_MAP.md)
* Feature: [brain/11_FEATURE_MAP.md](file:///e:/MyPlayer/brain/11_FEATURE_MAP.md)
* Architecture: [brain/02_ARCHITECTURE.md](file:///e:/MyPlayer/brain/02_ARCHITECTURE.md)
* Database: [brain/10_DATABASE_MAP.md](file:///e:/MyPlayer/brain/10_DATABASE_MAP.md)
* API: [brain/09_API_MAP.md](file:///e:/MyPlayer/brain/09_API_MAP.md)
* Dependencies: [brain/06_DEPENDENCY_GRAPH.md](file:///e:/MyPlayer/brain/06_DEPENDENCY_GRAPH.md)
* Data Flow: [brain/08_DATA_FLOW.md](file:///e:/MyPlayer/brain/08_DATA_FLOW.md)
* Events: [brain/07_EVENT_GRAPH.md](file:///e:/MyPlayer/brain/07_EVENT_GRAPH.md)
* Calls: [brain/16_CALL_GRAPH.md](file:///e:/MyPlayer/brain/16_CALL_GRAPH.md)
* Configuration: [brain/13_CONFIGURATION_MAP.md](file:///e:/MyPlayer/brain/13_CONFIGURATION_MAP.md)

---

# STEP 4 — Locate Source Files

Use [brain/03_FILE_INDEX.md](file:///e:/MyPlayer/brain/03_FILE_INDEX.md) to locate the minimum required files. Never scan unrelated folders.

---

# STEP 5 — Implementation Rules

* Modify the minimum possible files.
* Reuse existing architecture.
* Never duplicate logic.
* Never rename public classes.
* Never rename APIs.
* Never change architecture unless requested.
* Never create unnecessary services.
* Never create duplicate ViewModels.
* Never create duplicate repositories.

---

# STEP 6 — Before Finishing

Update the following files in the `brain/` directory to keep the AI Brain synchronized:

* [brain/18_CURRENT_STATE.md](file:///e:/MyPlayer/brain/18_CURRENT_STATE.md)
* [brain/03_FILE_INDEX.md](file:///e:/MyPlayer/brain/03_FILE_INDEX.md)
* [brain/VERSION.json](file:///e:/MyPlayer/brain/VERSION.json) (recompute hashes and increment docs version)

Update [brain/20_AI_CONTEXT.md](file:///e:/MyPlayer/brain/20_AI_CONTEXT.md) only if architecture changed. Do not modify other documentation unless necessary.

---

# END
