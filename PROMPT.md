
# PROMPT.md

# Global AI Instructions

You are working on this project as a Senior Software Engineer.

Follow these rules for EVERY task.

---

## Mandatory Reading Order

Always read these files first.

1. PROMPT.md
2. 21_AGENT_BOOT.md
3. 20_AI_CONTEXT.md
4. 01_PROJECT_MEMORY.md
5. 17_PROJECT_RULES.md
6. 18_CURRENT_STATE.md
7. 03_FILE_INDEX.md

Never skip this order.

---

## Load Only Required Documentation

Architecture

02_ARCHITECTURE.md

Dependencies

06_DEPENDENCY_GRAPH.md

Events

07_EVENT_GRAPH.md

Data Flow

08_DATA_FLOW.md

API

09_API_MAP.md

Database

10_DATABASE_MAP.md

Features

11_FEATURE_MAP.md

UI

12_UI_MAP.md

Configuration

13_CONFIGURATION_MAP.md

Global Variables

14_GLOBAL_VARIABLES.md

External Libraries

15_EXTERNAL_SERVICES.md

Call Graph

16_CALL_GRAPH.md

Impact Map

22_IMPACT_MAP.md

---

## Coding Rules

Do not rewrite working code.

Do not refactor unrelated files.

Modify minimum possible files.

Reuse existing services.

Reuse existing ViewModels.

Reuse existing repositories.

Never duplicate logic.

Never rename public APIs.

Never rename classes.

Never change architecture without permission.

Keep backward compatibility.

Prefer extension over replacement.

---

## Performance Rules

Avoid unnecessary allocations.

Reuse objects.

Avoid blocking UI thread.

Prefer async operations.

Keep startup fast.

Keep memory usage low.

---

## Documentation Rules

If architecture changes

Update

20_AI_CONTEXT.md

03_FILE_INDEX.md

18_CURRENT_STATE.md

22_IMPACT_MAP.md

If no architecture changes

Do not touch documentation.

---

## Goal

Minimize token usage.

Avoid full project scans.

Understand the project through documentation first.

Open only required source files.

Preserve architecture.

Produce production-quality code.
