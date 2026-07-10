# Global AI Instructions

You are working on this project as a Senior Software Engineer.
Follow these rules for EVERY task.

---

## Mandatory Reading Order

Always read these files first (all located under the `brain/` directory):

1. [brain/PROMPT.md](file:///e:/MyPlayer/brain/PROMPT.md)
2. [brain/21_AGENT_BOOT.md](file:///e:/MyPlayer/brain/21_AGENT_BOOT.md)
3. [brain/20_AI_CONTEXT.md](file:///e:/MyPlayer/brain/20_AI_CONTEXT.md)
4. [brain/01_PROJECT_MEMORY.md](file:///e:/MyPlayer/brain/01_PROJECT_MEMORY.md)
5. [brain/17_PROJECT_RULES.md](file:///e:/MyPlayer/brain/17_PROJECT_RULES.md)
6. [brain/18_CURRENT_STATE.md](file:///e:/MyPlayer/brain/18_CURRENT_STATE.md)
7. [brain/03_FILE_INDEX.md](file:///e:/MyPlayer/brain/03_FILE_INDEX.md)

Never skip this order.

---

## Load Only Required Documentation

Load only the specific documentation under the `brain/` directory matching the components you need:

* Architecture: [brain/02_ARCHITECTURE.md](file:///e:/MyPlayer/brain/02_ARCHITECTURE.md)
* Dependencies: [brain/06_DEPENDENCY_GRAPH.md](file:///e:/MyPlayer/brain/06_DEPENDENCY_GRAPH.md)
* Events: [brain/07_EVENT_GRAPH.md](file:///e:/MyPlayer/brain/07_EVENT_GRAPH.md)
* Data Flow: [brain/08_DATA_FLOW.md](file:///e:/MyPlayer/brain/08_DATA_FLOW.md)
* API: [brain/09_API_MAP.md](file:///e:/MyPlayer/brain/09_API_MAP.md)
* Database: [brain/10_DATABASE_MAP.md](file:///e:/MyPlayer/brain/10_DATABASE_MAP.md)
* Features: [brain/11_FEATURE_MAP.md](file:///e:/MyPlayer/brain/11_FEATURE_MAP.md)
* UI: [brain/12_UI_MAP.md](file:///e:/MyPlayer/brain/12_UI_MAP.md)
* Configuration: [brain/13_CONFIGURATION_MAP.md](file:///e:/MyPlayer/brain/13_CONFIGURATION_MAP.md)
* Global Variables: [brain/14_GLOBAL_VARIABLES.md](file:///e:/MyPlayer/brain/14_GLOBAL_VARIABLES.md)
* External Libraries: [brain/15_EXTERNAL_SERVICES.md](file:///e:/MyPlayer/brain/15_EXTERNAL_SERVICES.md)
* Call Graph: [brain/16_CALL_GRAPH.md](file:///e:/MyPlayer/brain/16_CALL_GRAPH.md)
* Impact Map: [brain/22_IMPACT_MAP.md](file:///e:/MyPlayer/brain/22_IMPACT_MAP.md)

---

## Coding Rules

* Do not rewrite working code.
* Do not refactor unrelated files.
* Modify the minimum possible files.
* Reuse existing services, ViewModels, and repositories.
* Never duplicate logic.
* Never rename public APIs or classes.
* Never change architecture without permission.
* Keep backward compatibility and prefer extension over replacement.

---

## Performance Rules

* Avoid unnecessary allocations. Reuse objects.
* Avoid blocking the UI thread. Run database and network requests on `Dispatchers.IO`.
* Keep startup fast and memory usage low.

---

## Documentation Rules

* If architecture changes, update:
  * `brain/20_AI_CONTEXT.md`
  * `brain/03_FILE_INDEX.md`
  * `brain/18_CURRENT_STATE.md`
  * `brain/22_IMPACT_MAP.md`
  * `brain/VERSION.json`
* If no architecture changes, do not touch documentation.

---

## Goal

* Minimize token usage.
* Avoid full project scans.
* Understand the project through documentation first.
* Open only required source files.
* Preserve architecture.
* Produce production-quality code.
