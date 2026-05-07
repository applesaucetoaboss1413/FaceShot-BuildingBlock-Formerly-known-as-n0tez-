## Summary
- Rebuild a rigorous reverse-engineering pipeline for `photo.editor.photoeditor.photoeditorpro_1.722.249.apks`, then reconstruct a maintainable photo-editor module with verified behavior, responsive UI, and a reproducible build.

## What I Found
- The prior extraction output still exists in the repo at `M:\n0tez\_re\photoeditorpro_1.722.249\` (base_extracted/arm64_extracted/xxhdpi_extracted).
- A prior report exists at `M:\n0tez\.trae\documents\photoeditorpro_reverse_engineering_summary.md` describing native libs, shader/table/model inventories, and the current clean-room implementation.
- `base_extracted/AndroidManifest.xml` is in binary AXML form; decoding will be needed to recover readable manifest, resource IDs, and layout XML.

## Goals (Mapped to Your Deliverables)
- Static reconstruction: class hierarchy, resources, assets, layouts for canvas/toolbar/workspace.
- Logic extraction: filters/adjustments/overlays/crop/rotate; shader/native calls; parameter ranges.
- UI fidelity: responsive layout + full dimens coverage; eliminate truncation/wrapping.
- Interaction fidelity: event → handler → state/undo-redo → persistence.
- Codebase output: modular packages with English naming + comprehensive documentation.
- Verification: unit + instrumentation tests; pixel-perfect flows; deterministic baselines.
- Build: Gradle project producing a debuggable APK; Android Lint clean.
- UI spec: design tokens + XML spec for spacing/typography/colors.

## Plan
### 1) Re-extract APKS (Repeatable + Auditable)
- Re-unpack the `.apks` into a fresh `_re/<version>/` run folder while preserving the existing one.
- Extract and index:
  - `base.apk` (resources + classes*.dex)
  - ABI splits (arm64-v8a native libs)
  - all density splits present in the APKS (not just xxhdpi)
- Produce a machine-readable inventory:
  - `assets/` tree + hashes
  - `lib/<abi>/*.so` list + hashes
  - `res/` tree + resource type counts

### 2) Decode Resources and Layouts (AXML/AAPT2)
- Decode binary AXML:
  - AndroidManifest.xml (activities, intent filters, providers, exported flags)
  - layout XML (toolbar/workspace/canvas screens)
  - `values/` (dimens/styles/themes/colors/strings)
- Export a cross-configuration matrix:
  - `values/`, `values-sw*dp`, `values-w*dp`, and per-density overrides
  - capture all `dimens.xml` buckets and consolidate into a report

### 3) Decompile DEX and Build the Class/Call Graph
- Decompile `classes.dex`, `classes2.dex`, `classes3.dex`.
- Generate:
  - package tree
  - class hierarchy (extends/implements)
  - key entrypoints: Activities/Fragments/Views/Renderers/Engines
  - call graph slices for editor flows (load image → edit → export)
- Map resources ↔ code:
  - layout IDs → view bindings → event listeners
  - menu/tool button IDs → handlers

### 4) Feature Logic Extraction (GPU + Native + CPU)
- For each feature group, extract the full parameter model and execution path:
  - Adjustments (ranges, defaults, curves)
  - Filters/LUT (table formats, selection rules)
  - Overlays (text/stickers/frames), transforms, compositing order
  - Crop/rotate/flip/straighten logic
  - Undo/redo command model
- GPU/shader pipeline:
  - locate shader blobs and how they are loaded/parameterized
  - identify render passes and framebuffer usage
- Native pipeline:
  - map JNI entrypoints and native library boundaries
  - identify where MNN/segmentation/matting/object removal are invoked

### 5) Interaction/Event → Handler → State Machine Mapping
- Enumerate all gestures and actions:
  - click/long-press
  - drag/pan
  - pinch/rotate
- For each, map:
  - UI element → listener/gesture detector → command/state transition
  - undo/redo stack behavior
  - persistence (draft save/restore) behavior and storage location

### 6) Reconstructed Module (Maintainable Architecture)
- Create a new editor module that mirrors the original’s functional boundaries but uses clean, English names and stable APIs:
  - `ui.canvas`, `ui.toolbar`, `ui.timeline` (history/inspector), `ui.workspace`
  - `core.filters`, `core.adjustments`, `core.geometry`, `core.overlays`
  - `core.render` (GPU/native bridges), `data.repositories` (drafts/assets)
- Keep the existing `com.n0tez.app.photoeditor` clean-room code as a reference and migrate/replace systematically.
- Add exhaustive Kotlin/Java documentation where needed for long-term maintainability.

### 7) Responsive UI Fidelity + No Truncation
- Rebuild layouts using ConstraintLayout guidelines + dimension tokens.
- Replace “magic dp” with named dimens.
- Create a layout verification checklist for 4.7"–12" and key densities.
- Add automated UI checks (screenshot tests where feasible) to catch truncation/wrapping regressions.

### 8) Verification: Pixel-Perfect Flows + Determinism
- Define 10 canonical editing flows and freeze inputs/parameters.
- Build a test harness that runs the same operations and compares:
  - exported bitmap hashes for CPU paths
  - pixel-perfect comparison on a fixed reference device/emulator config
  - tolerance-based comparison where GPU drivers introduce nondeterminism
- Add unit tests for parameter parsing, command stacks, and serialization.
- Add instrumentation tests for gestures and end-to-end export.

### 9) Build + Lint Clean
- Provide a Gradle build that:
  - assembles a debuggable test APK
  - runs unit + instrumentation tests
  - runs Android Lint with zero errors/warnings (fix issues instead of suppressing)

### 10) UI Specification Deliverable
- Produce a UI spec package inside the repo:
  - design tokens (spacing/typography/colors)
  - mapping to Android resources (colors.xml, dimens.xml, styles.xml)
  - XML wireframes for key screens

## Dependencies / Approvals Needed
- If decompilation/decoding tools (jadx/apktool/aapt2) are missing locally, I will either:
  - use existing SDK tooling already present, or
  - request explicit approval before installing any new tooling.

## Acceptance Criteria (Concrete)
- Reconstructed editor launches without crashes.
- Controls do not truncate/wrap across 4.7"–12" devices (verified by automated + manual checklist).
- 10 representative flows match pixel-perfect on the reference configuration.
- Build produces a debug-signed APK and Lint is clean.

## Next Output You’ll Receive After Approval
- A generated “Reverse Engineering Report” (class tree, assets/resources, event maps).
- A reconstructed editor module in the repo with tests + build scripts.
- A UI spec/token package for the reskin work.