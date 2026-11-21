# Changelog

<!--
Maintenance notes (never rendered in the app, never included in any version section):
This file is packaged with the app (copied into assets/CHANGELOG.en.md at build time) and is
shown offline in Settings → Version → Check for updates when the app language is not Chinese.

When releasing, add a new section at the TOP of this file:
- Level-2 heading `## <version> (YYYY-MM-DD)`; the version must match `versionName` in
  app/build.gradle.kts (a `v` prefix is allowed), and the date is the release day;
- Keep the same section order as CHANGELOG.md (zh) so both languages stay in sync;
- `###` headings inside a section are sub-headings and do not split sections.
-->

## 1.0.2 (2026-10-10)

This release adds Root mode, polishes the onboarding flow and release note display, and fixes an onboarding hang.

### ✨ New features

- **Root mode**: rooted devices can complete the permission grant during onboarding without installing Shizuku
- **Control panel opens after adding the tile in OOBE**: exTile now tries to pull up the control center once the tile is added, so you can tap "Edit" right away

### 🛠 Improvements

- Release notes in the update dialog are now rendered as Markdown
- "Progressive blur" now appears only when "Blur effects" is enabled, and its summary was removed
- Skip countdown shortened from 5 s to 3 s; donation and about page copy updated
- Debug tools now report "Root available" and the active privilege channel

### 🐛 Bug fixes

- Fixed importing tiles from system hanging during onboarding on some devices

## 1.0.1 (2026-10-04)

A maintenance release that fixes several onboarding issues and improves the tile catalog.

### ✨ New features

- **Import tiles from system**: new entry in the editor's three-dot menu
- **Cleaner add-tile sheet**: tiles without icons are hidden automatically
- **Smarter device gating**: the Workstation tile shows on Xiaomi-family tablets only; the cooling fan tile is gated by device codename
- **Smoother OOBE**: the skip button is now always visible, countdown shortened to 3 s

### 🐛 Bug fixes

- Disabled exTile tile actions until OOBE completes
- The OOBE import preview no longer shows or counts `edit` tiles
- Fixed the excessive top margin on the home screen and the debug tools entry icon

## 1.0.0 (2026-10-01)

The first stable release of exTile: the magic that balances simplicity and efficiency for your Android control center.

- **One-tap layout switch**: tap the exTile tile to instantly toggle between the collapsed and expanded layouts
- **One-page tile editor**: drag-and-drop in a 4-column grid, with undo
- **Rich tile collection**: built-in system catalog plus custom and third-party tiles, all searchable
- **Xiaomi / HyperOS extras**: automatic MIUI / HyperOS detection, with sync for "Wordless mode" and the fused device hub
- **Backup & restore**: JSON export / import / clipboard, multiple backup slots, one-tap import from the system
- **Usage stats**: expand / collapse counters shown on the home screen
- **Polished UI**: HyperOS-inspired design with dynamic color and dark mode; adaptive for phones, tablets and landscape
