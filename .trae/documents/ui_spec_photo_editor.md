# UI Spec – Photo Editor (N0tez)

This is the current UI contract for the photo editor surface, intended to stay stable while implementation details evolve.

## Canvas

- Preview: center-fit image, aspect preserved
- Overlay: drawing strokes rendered above preview, in bitmap coordinates
- Undo: removes the most recent stroke

## Tool Buttons

Guidelines:
- Icon above label to prevent label truncation/wrapping on narrow widths
- Labels are short, 1–2 words max

## Tokens

Design tokens live in:
- [editor_dimens.xml](file:///m:/n0tez/app/src/main/res/values/editor_dimens.xml)
- [editor_colors.xml](file:///m:/n0tez/app/src/main/res/values/editor_colors.xml)

Key token mapping from the reverse-engineered reference app:
- Tool item width: `editor_tool_item_width` (55dp)
- Tool bar height: `editor_tool_item_height` (84dp)
- Tool label size: `editor_text_tool_label` (11sp)

