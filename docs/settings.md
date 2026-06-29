# Settings & DataStore

MyPlayer uses Jetpack DataStore (Preferences DataStore) for persistent user settings, completely replacing the legacy `SharedPreferences` API.

## Implementation Details
- Located in `SettingsDataStore.kt`.
- DataStore provides a reactive `Flow` of preferences, meaning any changes to settings instantly propagate through the ViewModels and update the UI and background logic asynchronously.
- Write operations are asynchronous and safe from blocking the UI thread.

## Tracked Settings
As of RC-2, the following preferences are persisted:
- **Autoplay Enabled (Boolean):** The master switch for the Recommendation Engine. If false, playback stops at the end of the manual queue. Defaults to `true`.
