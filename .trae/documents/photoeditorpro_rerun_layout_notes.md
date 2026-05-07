# PhotoEditorPro 1.722.249 – Layout Notes (Rerun)

This summarizes the key responsive layout rules discovered in the decoded resources, focusing on the editor workspace and tool menu where truncation/wrapping is most visible.

## Tool Menu Layout (cg.xml)

Source:
- [cg.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_res/res/layout/cg.xml)

Key constants (as shipped):
- Tool item width: `55dp` (each tool is a vertical `LinearLayout`)
- Icon size: `24dp`
- Label text: `11sp`, `lines="2"`, `lineSpacingMultiplier="0.75"`, `ellipsize="end"`, centered
- Container height: `@dimen/rg` → `84dp` (default values bucket)

Dimension matrix:
- [photoeditorpro_rerun_dimens_matrix_cg.md](file:///M:/n0tez/.trae/documents/photoeditorpro_rerun_dimens_matrix_cg.md)

## Editor Workspace Layout (a5.xml)

Source:
- [a5.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_res/res/layout/a5.xml)

Core workspace view:
- `com.camerasideas.collagemaker.activity.widget.EditLayoutView`
  - [EditLayoutView.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditLayoutView.java)

