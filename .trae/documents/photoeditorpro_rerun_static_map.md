# PhotoEditorPro 1.722.249 – Static Map (Rerun)

Source artifact:
- `C:\Users\HP\Downloads\photo.editor.photoeditor.photoeditorpro_1.722.249.apks`

Rerun output root:
- `M:\n0tez\_re\photoeditorpro_1.722.249_rerun_20260214_170017\`

## Extraction Outputs

- Container extraction: `apks_container/`
  - `base.apk`
  - `split_config.arm64_v8a.apk`
  - `split_config.xxhdpi.apk`
- Raw zip extraction (per-APK): `*_extracted/`
- Resource decode (apktool): `decoded_apktool3/`
  - `base/` (resources + decoded `AndroidManifest.xml`)
  - `arm64_v8a/` (JNI libs + decoded `AndroidManifest.xml`)
  - `xxhdpi/` (density resources + decoded `AndroidManifest.xml`)
- Decompile (jadx):
  - Sources: `jadx_src/`
  - Resources: `jadx_res/`

## Codebase Size (Relevant Packages)

Counts from `jadx_src/` (Java files):
- `com.camerasideas.collagemaker.activity`: 209
- `com.camerasideas.collagemaker.activity.widget`: 85
- `com.camerasideas.collagemaker.activity.fragment.imagefragment`: 89
- `com.camerasideas.collagemaker.photoproc`: 24
- `jp.co.cyberagent.android.gpuimage` (shaded): 2

## App Entrypoints (Manifest)

Decoded manifest:
- [AndroidManifest.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/decoded_apktool3/base/AndroidManifest.xml)

Application:
- `com.camerasideas.collagemaker.activity.CollageMakerApplication`

Launcher:
- `com.camerasideas.collagemaker.activity.DummyActivity` (MAIN/LAUNCHER)

Primary photo editor activities (non-exhaustive):
- `com.camerasideas.collagemaker.activity.ImageEditActivity`
- `com.camerasideas.collagemaker.activity.ImageAiEditActivity`
- `com.camerasideas.collagemaker.activity.ImageResultActivity`
- `com.camerasideas.collagemaker.activity.ImageAiResultActivity`
- `com.camerasideas.collagemaker.activity.ImageCropActivity`

## Core Editor Screen: ImageEditActivity

Source:
- [ImageEditActivity.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/ImageEditActivity.java)

Layout binding:
- `ImageEditActivity.mo17251N2()` returns `R.layout.a5`
  - [a5.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_res/res/layout/a5.xml)

Workspace/canvas root:
- `a5.xml` contains `com.camerasideas.collagemaker.activity.widget.EditLayoutView` as the main editor surface.
  - [EditLayoutView.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditLayoutView.java)
  - Class: `EditLayoutView extends ConstraintLayout`
  - Interfaces: `GestureDetector.OnGestureListener`, `DragFrameLayout.InterfaceC4619b`, `com.facebook.rebound.InterfaceC8139d`
  - Rendering surface dependencies visible in imports:
    - `jp.co.cyberagent.android.gpuimage.GPUImageView`
    - `com.camerasideas.collagemaker.photoproc.graphicsitems.BackgroundView`
    - `com.camerasideas.collagemaker.photoproc.graphicsitems.DoodleView`
    - `com.camerasideas.collagemaker.photoproc.graphicsitems.ItemView`

Toolbar/menu:
- `EditToolsMenuLayout extends LinearLayout implements View.OnClickListener`
  - [EditToolsMenuLayout.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditToolsMenuLayout.java)
  - Inflates `R.layout.cg`
    - [cg.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_res/res/layout/cg.xml)

## GPU / Shader Asset Inventory (Static)

Shader bundle location:
- `jadx_out/resources/assets/res/shader/*`
  - Example file: [GLVignetteFilter](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_out/resources/assets/res/shader/GLVignetteFilter)

Native libraries location (arm64 split):
- `decoded_apktool3/arm64_v8a/lib/arm64-v8a/*.so`

Notes:
- Several shader payloads are non-plain-text (packed/obfuscated). The runtime loader path (and any decryption/unpacking) must be identified from code/JNI to recover effective GLSL.
