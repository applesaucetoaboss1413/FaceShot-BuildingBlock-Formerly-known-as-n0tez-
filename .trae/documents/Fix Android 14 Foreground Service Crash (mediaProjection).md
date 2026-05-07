## Summary
FloatingWidgetService crashes on Android 14/targetSdk 34 because it is declared as a mediaProjection foreground service, which triggers privileged permission requirements that the app does not (and cannot) hold.

## What I Found
- The exact crash is consistent with calling `startForeground()` while the OS has classified the service as `foregroundServiceType="mediaProjection"`.
- In this repo, `AndroidManifest.xml` already declares:
  - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />`
  - `FloatingWidgetService` with `android:foregroundServiceType="mediaProjection"`
- The exception text indicates Android requires:
  - **All-of**: `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION`
  - **Any-of**: `android.permission.CAPTURE_VIDEO_OUTPUT` **or** `android:project_media`
- Those **any-of** permissions are **privileged/system-level**, so a normal Play Store app cannot satisfy them. That means keeping `foregroundServiceType="mediaProjection"` will remain a crash vector.

## Root Cause
The service is misclassified as a **mediaProjection** foreground service even though the app’s overlay/notepad feature is not performing screen capture.

When Android sees `foregroundServiceType="mediaProjection"`, it enforces media projection FGS rules at the `startForeground()` call site, leading to the `SecurityException`.

## Correct Fix (Recommended)
Remove the mediaProjection foreground service type so Android stops enforcing those permissions.

- In `AndroidManifest.xml`:
  - Remove: `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />`
  - Update the service declaration by **removing** `android:foregroundServiceType="mediaProjection"`.

This aligns the service with what it actually does (overlay UI + notification), and avoids privileged permission gates.

## Alternative Fix (Only if you truly need screen capture)
If you intended to ship screen recording / screen capture:
- Implement a real MediaProjection flow (user consent via MediaProjectionManager), start the foreground service only after consent, and use the correct foreground service type.
- This is a materially different feature with stricter Play policy and UX requirements.

## Implementation Plan (Repo Changes)
1. Update `app/src/main/AndroidManifest.xml`
   - Remove `FOREGROUND_SERVICE_MEDIA_PROJECTION` permission.
   - Remove `android:foregroundServiceType="mediaProjection"` from `FloatingWidgetService`.
2. Improve internal crash diagnostics (optional but useful)
   - Move one internal log event to occur before `startForeground()` so we can capture “service_start_attempt” even if `startForeground()` throws.
3. Verify
   - Build debug APK.
   - Run unit tests.
   - Validate flow: open app → grant overlay permission → start widget → open notepad (no crash).

## Risks & Mitigations
- Risk: If any feature actually requires mediaProjection, removing the type could break that feature → Mitigation: Confirm the app does not perform screen capture; if it does, implement the alternative flow.
- Risk: OEM-specific FGS enforcement differences → Mitigation: Keeping service type unspecified is the broadest-compatibility option for an overlay UI service.

## Acceptance Criteria
- [ ] App no longer throws `SecurityException` at `startForeground()`.
- [ ] Floating widget starts successfully on Android 14 (targetSdk 34).
- [ ] Overlay bubble and notepad open reliably after overlay permission grant.
- [ ] Debug build and unit tests pass.

## Questions for You
- Should FloatingWidgetService be *only* an overlay/notepad service (no screen capture)? If yes, the recommended fix is correct.

## Ready to Proceed
Yes — after you confirm, I’ll apply the manifest correction and run a build + tests to verify the crash is resolved.