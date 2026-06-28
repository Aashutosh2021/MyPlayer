
# FEATURE_DEPENDENCY_MATRIX.md

# Feature Dependency Matrix

**Purpose**

This document maps every major feature to the exact architecture layers it depends on.

Before implementing any feature, identify the affected row(s) and only load the required documentation and source files.

---

# Legend

| Symbol | Meaning             |
| ------ | ------------------- |
| ✅     | Direct Dependency   |
| ⚠️   | Possible Dependency |
| ❌     | No Dependency       |

---

# Layer Definitions

| Layer    | Description                |
| -------- | -------------------------- |
| UI       | Compose / XML Screens      |
| NAV      | Navigation                 |
| VM       | ViewModel                  |
| UC       | Use Cases / Business Logic |
| SVC      | Services / Managers        |
| REPO     | Repository                 |
| DB       | Database                   |
| DAO      | Room DAO                   |
| API      | Remote APIs                |
| CACHE    | Memory / Disk Cache        |
| WORKER   | Background Workers         |
| PLAYER   | Playback Engine            |
| SECURITY | Security Layer             |
| CONFIG   | Settings / Preferences     |
| NOTIFY   | Notification System        |

---

# Feature Matrix

| Feature           | UI | NAV  | VM | UC   | SVC  | REPO | DB   | DAO  | API  | CACHE | WORKER | PLAYER | SECURITY | CONFIG | NOTIFY |
| ----------------- | -- | ---- | -- | ---- | ---- | ---- | ---- | ---- | ---- | ----- | ------ | ------ | -------- | ------ | ------ |
| Local Music Scan  | ✅ | ❌   | ✅ | ✅   | ✅   | ✅   | ✅   | ✅   | ❌   | ✅    | ⚠️   | ❌     | ❌       | ⚠️   | ❌     |
| Music Playback    | ✅ | ⚠️ | ✅ | ✅   | ✅   | ⚠️ | ❌   | ❌   | ⚠️ | ✅    | ❌     | ✅     | ❌       | ✅     | ✅     |
| Queue Management  | ✅ | ❌   | ✅ | ✅   | ✅   | ⚠️ | ❌   | ❌   | ❌   | ✅    | ❌     | ✅     | ❌       | ❌     | ⚠️   |
| Playlist          | ✅ | ✅   | ✅ | ✅   | ⚠️ | ✅   | ✅   | ✅   | ❌   | ✅    | ❌     | ⚠️   | ❌       | ❌     | ❌     |
| Favorites         | ✅ | ❌   | ✅ | ⚠️ | ❌   | ✅   | ✅   | ✅   | ❌   | ⚠️  | ❌     | ⚠️   | ❌       | ❌     | ❌     |
| Search            | ✅ | ⚠️ | ✅ | ✅   | ⚠️ | ✅   | ⚠️ | ⚠️ | ✅   | ✅    | ❌     | ❌     | ❌       | ❌     | ❌     |
| Online Streaming  | ✅ | ❌   | ✅ | ✅   | ✅   | ✅   | ⚠️ | ❌   | ✅   | ✅    | ❌     | ✅     | ⚠️     | ❌     | ✅     |
| Downloads         | ✅ | ❌   | ✅ | ✅   | ✅   | ✅   | ✅   | ✅   | ✅   | ✅    | ✅     | ⚠️   | ❌       | ⚠️   | ✅     |
| Lyrics            | ✅ | ❌   | ✅ | ⚠️ | ✅   | ⚠️ | ⚠️ | ❌   | ✅   | ✅    | ❌     | ✅     | ❌       | ❌     | ❌     |
| Recommendation    | ✅ | ⚠️ | ✅ | ✅   | ✅   | ✅   | ⚠️ | ❌   | ✅   | ✅    | ❌     | ⚠️   | ❌       | ❌     | ❌     |
| Equalizer / DSP   | ✅ | ❌   | ✅ | ✅   | ✅   | ❌   | ❌   | ❌   | ❌   | ❌    | ❌     | ✅     | ❌       | ✅     | ❌     |
| Sleep Timer       | ✅ | ❌   | ✅ | ⚠️ | ✅   | ❌   | ❌   | ❌   | ❌   | ❌    | ⚠️   | ✅     | ❌       | ✅     | ✅     |
| Theme             | ✅ | ⚠️ | ✅ | ❌   | ❌   | ❌   | ❌   | ❌   | ❌   | ⚠️  | ❌     | ❌     | ❌       | ✅     | ❌     |
| Settings          | ✅ | ⚠️ | ✅ | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ❌   | ⚠️  | ❌     | ⚠️   | ⚠️     | ✅     | ❌     |
| Authentication    | ✅ | ✅   | ✅ | ✅   | ✅   | ✅   | ✅   | ✅   | ✅   | ⚠️  | ❌     | ❌     | ✅       | ⚠️   | ❌     |
| Profile           | ✅ | ✅   | ✅ | ⚠️ | ⚠️ | ✅   | ✅   | ✅   | ✅   | ⚠️  | ❌     | ❌     | ⚠️     | ⚠️   | ❌     |
| Cross Device Sync | ✅ | ✅   | ✅ | ✅   | ✅   | ✅   | ✅   | ✅   | ✅   | ✅    | ✅     | ✅     | ✅       | ✅     | ✅     |

---

# Feature → Documentation Map

| Feature        | Read First                                                           |
| -------------- | -------------------------------------------------------------------- |
| Playback       | 20_AI_CONTEXT → 03_FILE_INDEX → 16_CALL_GRAPH → 08_DATA_FLOW      |
| Queue          | 20_AI_CONTEXT → 03_FILE_INDEX → 07_EVENT_GRAPH                     |
| Playlist       | 11_FEATURE_MAP → 10_DATABASE_MAP → 03_FILE_INDEX                   |
| Downloads      | 11_FEATURE_MAP → 09_API_MAP → 10_DATABASE_MAP → 16_CALL_GRAPH     |
| Recommendation | 11_FEATURE_MAP → 08_DATA_FLOW → 06_DEPENDENCY_GRAPH                |
| Search         | 09_API_MAP → 08_DATA_FLOW → 03_FILE_INDEX                          |
| Lyrics         | 09_API_MAP → 16_CALL_GRAPH                                          |
| Equalizer      | 02_ARCHITECTURE → 16_CALL_GRAPH → 03_FILE_INDEX                    |
| Settings       | 13_CONFIGURATION_MAP → 03_FILE_INDEX                                |
| Theme          | 12_UI_MAP → 13_CONFIGURATION_MAP                                    |
| Authentication | 09_API_MAP → 14_GLOBAL_VARIABLES → 17_PROJECT_RULES                |
| Sync           | 02_ARCHITECTURE → 06_DEPENDENCY_GRAPH → 08_DATA_FLOW → 09_API_MAP |

---

# AI Decision Rules

## Before editing

1. Identify the requested feature.
2. Find the feature row in this matrix.
3. Load only the required documentation.
4. Open only the related source files.
5. Do not scan unrelated folders.
6. Reuse existing architecture.
7. Modify the minimum possible files.

---

# Documentation Update Rules

If a new feature is added:

* Update this matrix.
* Update 11_FEATURE_MAP.md.
* Update 03_FILE_INDEX.md.
* Update 20_AI_CONTEXT.md.

If a feature is removed:

* Remove its row.
* Update dependency mappings.
* Update affected documentation.

---

# Goal

This matrix is the primary routing table for all AI agents.

It should allow an AI to determine which parts of the project are relevant **before** reading source code, reducing unnecessary codebase scans and minimizing token usage while preserving architectural consistency.
