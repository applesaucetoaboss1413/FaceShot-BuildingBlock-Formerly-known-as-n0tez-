# Editor Module Structure (N0tez)

This repo is migrating the existing clean-room photo editor into a stable, modular package layout to support the upcoming multimedia-editing suite refactor.

## Current Implementation (Existing)

- `com.n0tez.app.photoeditor.*`
  - Rendering: `BitmapProcessor`
  - Models/state: `PhotoEditorModels.kt`
  - Canvas overlay: `DrawingOverlayView`, `ImageViewBitmapMapper`, `TransformTouchListener`
  - UI entry: `PhotoEditorActivity`

## Target Modular Packages (New)

- `com.n0tez.app.editor.core`
  - Domain types: `EditorState`, `Adjustments`, `FilterPreset`, `OverlayElement`, `Stroke`
  - History: `core.history.UndoRedoStack`
  - Rendering: `core.render.EditorRenderer`
- `com.n0tez.app.editor.ui.canvas`
  - Canvas container: `EditorCanvasView` (ImageView + DrawingOverlayView)
- `com.n0tez.app.editor.ui.toolbar`
  - Tool identifiers: `ToolId`

## Backwards Compatibility Strategy

- New packages currently delegate to the existing clean-room implementations to avoid breaking changes.
- As UI is refactored, callers can move imports from `com.n0tez.app.photoeditor.*` to `com.n0tez.app.editor.*` without changing runtime behavior.

