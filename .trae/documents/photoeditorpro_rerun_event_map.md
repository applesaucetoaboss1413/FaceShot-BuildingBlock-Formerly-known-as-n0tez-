# PhotoEditorPro 1.722.249 – Event → Handler → State Map (Rerun)

This is a static mapping from user actions (click/drag/gesture) to their primary handler entrypoints and state/persistence surfaces.

Rerun root:
- `M:\n0tez\_re\photoeditorpro_1.722.249_rerun_20260214_170017\`

## Toolbar / Tool Menu Clicks

Menu layout:
- [cg.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_res/res/layout/cg.xml)

Click handler:
- [EditToolsMenuLayout.onClick](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditToolsMenuLayout.java#L434-L668)

Generated mapping (view id → label → internal action code):
- [photoeditorpro_rerun_tool_menu_map.md](file:///M:/n0tez/.trae/documents/photoeditorpro_rerun_tool_menu_map.md)

Notes:
- `EditToolsMenuLayout` emits a `CD0` event (`new CD0(i)`) via `C0754IB.m2688a().m2689b(cd0)` after mapping the clicked view id to an integer action code.
- Some actions gate on runtime flags (e.g., network/feature gates, “pro” checks), and may branch analytics events before dispatch.

## Undo / Redo (AI Removal Editor)

Fragment (AI removal UI controller):
- [ViewOnClickListenerC4735h.onClick](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/fragment/imagefragment/ViewOnClickListenerC4735h.java#L1092-L1230)

Editor view (state + rendering):
- [AiRemovalEditorView](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/photoproc/editorview/AiRemovalEditorView.java)

State machine / persistence surface:
- `AiRemovalEditorView.f29492o0 : C10401y` is the history record.
  - `C10401y.f51559a`: “undo stack” (current history)
  - `C10401y.f51560b`: “redo stack”
  - Each entry is a `C9965t` snapshot containing brush/mode plus path lists (`f48906d`) and tool settings.

Undo action (button id `R.id.ml`):
- Moves the latest `C9965t` from `f51559a` → `f51560b`, then restores:
  - `f29494q0` path list (stroke history)
  - brush type + AI mode
  - triggers re-render steps: `m18371n()`, `m18374q()`, `m18373p()`, `invalidate()`

Redo action (button id `R.id.mi`):
- Moves the latest `C9965t` from `f51560b` → `f51559a`, then restores the same state surfaces.

## Drag / Gesture Mapping (Workspace Container)

Canvas/workspace container:
- [EditLayoutView](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditLayoutView.java)

Primary gesture entrypoints:
- `onDown`: resets gesture tracking flags.
- `onScroll`: classifies a gesture as horizontal vs vertical based on movement angle and `scaledTouchSlop`.
- `onInterceptTouchEvent`: intercepts when vertical drag should control a panel/workspace transition (gated by `m18124v()` and internal flags).
- `onTouchEvent`: applies vertical drag deltas to a rebound-driven spring (`C8137b`) and animates to the target position on release.
- `onSingleTapUp`: detects taps within a “hit rect” and triggers layout reset / collapse.

What this means behaviorally:
- The editor workspace has a vertical draggable state (panel/overlay visibility) controlled by `EditLayoutView` rather than being purely a standard Android `BottomSheetBehavior`.

