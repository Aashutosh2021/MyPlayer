# AGENT_BOOT.md

# AI Agent Boot Sequence

## Purpose

This file defines the mandatory startup procedure for every AI agent before making any modification to the project.

Never skip these steps.

---

# STEP 1 — Load Global Context

Read the following files in order.

1. PROMPT.md
2. 20_AI_CONTEXT.md
3. 01_PROJECT_MEMORY.md
4. 17_PROJECT_RULES.md
5. 18_CURRENT_STATE.md
6. 03_FILE_INDEX.md

These files contain the compressed memory of the entire project.

Do NOT scan the full codebase before reading them.

---

# STEP 2 — Understand Request

Identify the user's intent.

Possible categories

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

UI

* 12_UI_MAP.md

Feature

* 11_FEATURE_MAP.md

Architecture

* 02_ARCHITECTURE.md

Database

* 10_DATABASE_MAP.md

API

* 09_API_MAP.md

Dependencies

* 06_DEPENDENCY_GRAPH.md

Data Flow

* 08_DATA_FLOW.md

Events

* 07_EVENT_GRAPH.md

Calls

* 16_CALL_GRAPH.md

Configuration

* 13_CONFIGURATION_MAP.md

---

# STEP 4 — Locate Source Files

Use

03_FILE_INDEX.md

to locate the minimum required files.

Never scan unrelated folders.

---

# STEP 5 — Implementation Rules

Modify the minimum possible files.

Reuse existing architecture.

Never duplicate logic.

Never rename public classes.

Never rename APIs.

Never change architecture unless requested.

Never create unnecessary services.

Never create duplicate ViewModels.

Never create duplicate repositories.

---

# STEP 6 — Before Finishing

Update

18_CURRENT_STATE.md

Update

03_FILE_INDEX.md

Update

20_AI_CONTEXT.md

only if architecture changed.

Do not modify other documentation unless necessary.

---

# END
