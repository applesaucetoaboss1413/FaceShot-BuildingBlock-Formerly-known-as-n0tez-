# PhotoEditorPro (photo.editor.photoeditor.photoeditorpro_1.722.249) – Static Feature Inventory (Authorized)

This document is based on static inspection of the APKS contents (assets, resource packs, and native library inventory). It avoids copying or reproducing proprietary source code, and it does not attempt to reconstruct vendor-specific implementations from decompiled methods.

## Package Artifacts

- APKS entries
  - `base.apk` (3 DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`)
  - `split_config.arm64_v8a.apk` (native libraries)
  - `split_config.xxhdpi.apk` (density resources)

## Native Libraries (arm64-v8a)

The presence of these `.so` files strongly suggests the following capability groups:

- GPU / shader-based image processing
  - `libgpuimage-library.so` (GPUImage-style filters/pipeline)
  - `libnative-render.so` (custom rendering/compositing)
  - `libblur.so` (blur kernel(s))
- On-device AI inference / segmentation / matting / object removal
  - `libMNN.so`, `libmnncore.so` (MNN inference runtime)
  - `libsegCore.so` (segmentation core)
  - `libmatting.so` (alpha matting / cutout)
  - `libobjectremoval.so` (object removal / inpainting)
  - `libnms.so` (non-maximum suppression; common in detection pipelines)
- Face analysis and beauty/reshape
  - `libfacelandmarks.so` (face landmarks)
  - `libfacedeformation.so` (face reshape)
  - `libfaceretouching.so` (skin smoothing/retouch)
- “Effect SDK” and supporting utilities
  - `libEffectSDK.so`, `libGAKit.so`, `libcore_util.so`, `libcvalgo.so`, `libcvautils.so`

Also present: Crashlytics, AES, and internal telemetry/anti-tamper libs (`libcrashlytics*.so`, `libtiny-aes.so`, `libpglarmor.so`, etc.).

## Assets: Filters, Shaders, Models, Templates

### Shader Inventory (names)

The APK bundles a large shader set under `assets/res/shader/` with names consistent with photo adjustments, look-up-table filtering, and beauty/makeup pipelines:

- Global adjustments: `GLExposureFilter`, `GLFadeFilter`, `GLGrainFilter`, `GLHighlightShadowFilter`, `GLTintFilter`, `GLVignetteFilter`, `GLWarmthFilter`, `GPUSharpenFilter`, `GPUSharpenVFilter`
- LUT/look-up: `GPUImageLookUpFilter`, `lookupFilterFragment`
- Auto-adjust: `autoAdjust2`
- Selective/region tools: `GLSelectiveFilter`, `highPassDiffFilter`
- Beauty/makeup: `makeupFragmentFilter`, `makeup_fs_out`, `blush_vs_base`, `eyeProcessFilter1`, `eyeProcessFilter2`, `nasolabialFilter`
- Face reshape: `reshape_fs_base`, `reshape_vs_base`, `reshape_vs_base_makeup`, `reshape_vs_freeze`
- Cosmetics: `GPUEyeBrowsFilter`, `GPUEyeContactFilter`, `GPULipStickFilter`

Note: the shader payloads appear to be packaged/obfuscated rather than shipped as plain GLSL text.

### Filter Preset Tables

The APK includes preset “table_…” blobs in assets (likely LUTs or tone curve tables), including:

`table_antique`, `table_bright`, `table_circus`, `table_cocoa`, `table_dark`, `table_dew`, `table_dream`, `table_ginkgo`, `table_gold`, `table_grape`, `table_harvest`, `table_latte`, `table_lomo`, `table_memo`, `table_natural`, `table_pink`, `table_story`, `table_time`, `table_vintage`, `table_warm`.

### Models

`assets/model/` contains:

- `face_lmks_v1.3.0.model` (face landmark model)
- `thumb_v1.0.0.model` (thumbnail/embedding/classification-style model; exact use unclear)

### Lottie/UX Indicators

Lottie assets suggest UI flows for: blemish removal, wrinkle, cutout/removal, “enhance”, scanning/loading, and “yearbook” generation. This matches the presence of object removal/matting and face retouch libraries.

## Inferred Feature Surface (What the APK likely offers)

Based on the above artifacts, the photo editor feature set likely includes:

- Geometry: crop, rotate, flip, resize, straighten
- Global adjustments: exposure/brightness, contrast, saturation, warmth/temperature, tint, highlights/shadows, fade, vignette, grain, sharpen
- Filters: preset looks and LUT-based filters (e.g., vintage, warm, etc.)
- Retouch/beauty: smoothing/retouch, face reshape, makeup overlays (brows/contacts/lipstick/blush), wrinkle/nasolabial processing
- AI tools: background cutout/matting, segmentation, object removal/inpainting
- Overlays: text bubbles, fonts, templates, stickers/frames (indicated by packed `assets/res/*` resources)

## Dependency Mapping (High Level)

- UI/asset packs: `assets/res/*` (packed) and `assets/lottie/*` (animation)
- GPU pipeline: shader pack + GPUImage native lib
- AI pipeline: MNN runtime + segmentation/matting/object removal libs + model blobs
- Retouch pipeline: face landmark + deformation + retouch libs

## Clean-Room Implementation Guidance

To avoid copyright violations:

- Do not copy class/method structures from the APK.
- Use standard, well-known algorithms (color adjustments, convolution sharpen, vignette, LUT sampling).
- Treat the above list as a “capability checklist”, then implement your own UX and processing pipeline.

The repository includes a clean-room Android implementation (Kotlin) under `com.n0tez.app.photoeditor` with:

- CPU-based render pipeline (adjustments, vignette, grain, sharpen, LUT support)
- Drawing (brush strokes), text overlays, sticker overlays
- A simple editor UI integrated into `PhotoEditorActivity`

## Clean-Room Editor API (Implemented in This Repo)

### Data Model

- `PhotoEditorState`
  - `adjustments: Adjustments`
  - `filter: FilterPreset`
  - `lut: LutFilter?` (optional LUT applied after adjustments)
  - `overlays: List<OverlayElement>` (text/stickers)
  - `strokes: List<Stroke>` (drawing)

- `Adjustments`
  - `brightness [-1..1]`
  - `contrast [0..3]`
  - `saturation [0..3]`
  - `warmth [-1..1]` (red/blue bias)
  - `tint [-1..1]` (green/magenta bias)
  - `vignette [0..1]`, `grain [0..1]`, `sharpen [0..1]`

### Rendering

- `BitmapProcessor.renderFinalBitmap(contentResolver, sourceBitmap, state)`
  - Runs on `Dispatchers.Default` (suspend function).
  - Applies preset → adjustments → optional LUT → overlays/strokes.
  - Produces an `ARGB_8888` bitmap suitable for JPEG export.

### UI Integration

- `activity_photo_editor.xml` now contains:
  - `editorContainer: FrameLayout`
  - `imageView: ImageView` (base image)
  - `drawingView: DrawingOverlayView` (freehand drawing)
  - tool buttons: Adjust, Filters, Draw, Text, Sticker, Undo

- `PhotoEditorActivity`
  - Uses `BitmapProcessor` for preview (debounced) and for final save/share.
  - Uses `TransformTouchListener` for drag/scale/rotate of text/sticker overlays.

