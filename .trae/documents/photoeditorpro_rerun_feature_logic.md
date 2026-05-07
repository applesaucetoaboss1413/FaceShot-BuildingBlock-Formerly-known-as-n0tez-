# PhotoEditorPro 1.722.249 – Feature Logic (Static, Rerun)

This document focuses on locating the real feature logic boundaries (GPU shaders, JNI calls, and the Java orchestration layer), with direct links into the rerun extraction outputs.

Rerun root:
- `M:\n0tez\_re\photoeditorpro_1.722.249_rerun_20260214_170017\`

## GPU / Shader Pipeline

### Encrypted shader loader

- [GPUImageNativeLibrary.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/jp/p185co/cyberagent/android/gpuimage/GPUImageNativeLibrary.java)
  - Loads native library: `System.loadLibrary("gpuimage-library")`
  - Shader fetch by integer ID:
    - `String m21353c(int id)` → `getShader(…, id-1)`
  - Key behavior:
    - Shader payloads are read from assets and decrypted at runtime via the native layer.

Supporting mapping:
- [C8923hE.m20880h](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p000/C8923hE.java#L3-L13) maps `id -> (id-1)` for native shader retrieval.

Shader assets (encrypted/packed):
- `jadx_out/resources/assets/res/shader/*`
  - Example: [GLVignetteFilter](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_out/resources/assets/res/shader/GLVignetteFilter)

### Filter/adjust orchestration using native shader IDs

- [C4874a.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/filter/core/adjust/C4874a.java)
  - Constructs multiple filter instances by passing decrypted shader strings:
    - `new C0424EL(…, GPUImageNativeLibrary.m21353c(…))`
  - Uses additional obfuscated-string decoder calls (`C11625dY0.m20306m(...)`) which are resolved via `itcore` at runtime (below).

## Native String Decoder / Obfuscation Boundary

- [Decoder.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/dev/p184in/decode/Decoder.java)
  - Loads: `System.loadLibrary("itcore")`
  - Provides native decode primitives:
    - `decodeBytesNative(String in, String key)`
    - `decodeStringNative(String in, String key)`
    - `decodeIntNative(int in, String key)`
- [C11625dY0.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p000/C11625dY0.java)
  - `m20306m(cipherText, key)` returns either plaintext bytes or `Decoder.decodeBytesNative(cipherText, key)` depending on runtime flag.

Impact:
- Many library names, asset names, tags, and some constants are not statically recoverable without executing `itcore`’s native decode.

## AI / ML Feature Boundaries (JNI Wrappers)

### Matting (background removal)

- [MattingUtils.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p001a/p002bd/jniutils/MattingUtils.java)
  - Loads: `System.loadLibrary("matting")` (fallback `C2743c4.m8464z(context, "matting")`)
  - Init signature:
    - `nInit(String[] sha1, String packageName, String modelPath, String cacheDir, int w=320, int h=512, boolean …)`
  - Inference:
    - `run(long handle, Bitmap src, Bitmap dst)`
  - Lifecycle:
    - `release(long handle)`

### Face landmarks

- [FaceLandmarksUtils.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p001a/p002bd/jniutils/FaceLandmarksUtils.java)
  - Loads: `System.loadLibrary("facelandmarks")`
  - Init signature:
    - `nInit(String[] sha1, String packageName, String detModelPath, String landmarksModelPath, Config config)`
  - Inference:
    - `nRun(long handle, Bitmap src, boolean mirror, FaceInfo out)`

### Face retouching

- [FaceRetouchingUtils.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p001a/p002bd/jniutils/FaceRetouchingUtils.java)
  - Loads: `System.loadLibrary("faceretouching")`
  - Init signature:
    - `nInit(String[] sha1, String packageName, String modelPath, int mode)`
  - Inference:
    - `run(long handle, Bitmap src, Bitmap dst, int[] params)`

### Face deformation

- [FaceDeformationUtils.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p001a/p002bd/jniutils/FaceDeformationUtils.java)
  - Loads: `System.loadLibrary("facedeformation")`
  - Init signature:
    - `nInit(String[] sha1, String packageName, float[] faceLandmarks, int width, int height, int rotation)`
  - Inference:
    - `nRun(handle, strength, adjustMode, posMode, index, FaceDeformationOutput out)`
  - Parameter enums (static list of supported face-shape axes):
    - [AdjustMode.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p001a/p002bd/jniutils/AdjustMode.java)
    - [PosMode.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/p001a/p002bd/jniutils/PosMode.java)

### Object removal

- [ObjectRemoval.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/photoproc/removal/ObjectRemoval.java)
  - Loads: `System.loadLibrary(C11625dY0.m20306m(...))` (decoded at runtime via `itcore`)
  - Inference:
    - `nProcess(Bitmap src, Bitmap mask, Bitmap dst)` (3-bitmap API; exact semantics must be verified dynamically)

Native library inventory (arm64):
- `decoded_apktool3/arm64_v8a/lib/arm64-v8a/*.so`

## Canvas / Overlay Rendering (Java Layer)

Primary editor canvas container:
- [EditLayoutView.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditLayoutView.java)
  - Uses:
    - `GPUImageView` for GPU pipeline preview
    - `DoodleView` and `ItemView` for overlay editing layers

Toolbar/menu logic:
- [EditToolsMenuLayout.java](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_src/com/camerasideas/collagemaker/activity/widget/EditToolsMenuLayout.java)
  - Inflates [cg.xml](file:///M:/n0tez/_re/photoeditorpro_1.722.249_rerun_20260214_170017/jadx_res/res/layout/cg.xml)
