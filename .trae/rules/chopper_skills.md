# CHOPPER TECHNICAL SKILLS REFERENCE

## SKILL 1: APK DECOMPILATION & EXTRACTION

### Standard Decompilation
```bash
# Basic decompilation
apktool d app.apk -o /output_dir

# Force decompilation (skip resource errors)
apktool d app.apk -o /output_dir --no-res

# Decompile with framework
apktool if framework-res.apk
apktool d app.apk -o /output_dir
```

### Resource Extraction
```bash
# Extract resources.arsc
unzip app.apk resources.arsc

# Dump resource IDs
aapt2 dump resources app.apk

# List all resources
aapt2 dump badging app.apk | grep "application-label"
```

### DEX to Java Conversion
```bash
# JADX CLI
jadx -d /output_dir app.apk

# JADX with specific options
jadx --show-bad-code --deobf -d /output_dir app.apk

# dex2jar alternative
d2j-dex2jar app.apk -o app.jar
```

### Native Library Extraction
```bash
# Extract .so files
unzip app.apk "lib/*" -d /output_dir

# List symbols in .so
nm -D libname.so

# Check dependencies
readelf -d libname.so

# Strings in native lib
strings libname.so | grep -i "interesting_pattern"
```

---

## SKILL 2: PACKAGE REFACTORING

### Smali Package Rename
```bash
# Rename package in all Smali files
find . -name "*.smali" -type f -exec sed -i 's/Lcom\/oldpackage\//Lcom\/newpackage\//g' {} \;

# Rename in XML files
find . -name "*.xml" -type f -exec sed -i 's/com\.oldpackage/com\.newpackage/g' {} \;

# Update AndroidManifest.xml
sed -i 's/package="com\.oldpackage"/package="com.newpackage"/g' AndroidManifest.xml
```

### Class Renaming
```bash
# Rename obfuscated class (a.b.c -> meaningful.name)
find . -name "*.smali" -exec sed -i 's/La\/b\/c\//Lcom\/internal\/util\//g' {} \;

# Update class definition
sed -i 's/\.class public La\/b\/c;/.class public Lcom\/internal\/util\/Helper;/g' file.smali
```

### Resource ID Updating
```python
# Python script to update resource IDs
import re

def update_resource_ids(smali_file, id_map):
    with open(smali_file, 'r') as f:
        content = f.read()
    
    for old_id, new_id in id_map.items():
        # Update const instructions
        pattern = f'const v\\d+, {old_id}'
        replacement = f'const v\\d+, {new_id}'
        content = re.sub(pattern, replacement, content)
    
    with open(smali_file, 'w') as f:
        f.write(content)
```

---

## SKILL 3: DEPENDENCY ANALYSIS & SHADING

### Dependency Detection
```bash
# List all imports in Java/Smali
grep -r "import " . | sort | uniq

# Find library usage
grep -r "Lokhttp3/" . | wc -l

# Check gradle dependencies (if available)
grep "implementation" build.gradle
```

### Package Shading (Manual)
```bash
# Shade OkHttp example: com.squareup.okhttp3 -> com.internal.http3
find . -name "*.smali" -exec sed -i 's/Lcom\/squareup\/okhttp3\//Lcom\/internal\/http3\//g' {} \;
find . -name "*.xml" -exec sed -i 's/com\.squareup\.okhttp3/com\.internal\.http3/g' {} \;

# Update META-INF services if present
sed -i 's/com\.squareup\.okhttp3/com\.internal\.http3/g' META-INF/services/*
```

### Dependency Conflict Resolution
```bash
# Check for duplicate classes
find . -name "*.smali" | sed 's/.*smali\///' | sort | uniq -d

# Identify version conflicts
grep -h "version" */build.gradle | sort | uniq
```

---

## SKILL 4: MANIFEST SURGERY

### Component Extraction
```bash
# Extract activities
grep -A 10 "<activity" AndroidManifest.xml

# Extract services
grep -A 10 "<service" AndroidManifest.xml

# Extract receivers
grep -A 10 "<receiver" AndroidManifest.xml
```

### Manifest Merging
```xml
<!-- Add merger rules -->
<manifest xmlns:tools="http://schemas.android.com/tools">
    <application
        tools:replace="android:label,android:theme"
        tools:merge="android:allowBackup">
    </application>
</manifest>
```

### Permission Consolidation
```bash
# List all permissions
grep "uses-permission" AndroidManifest.xml | sort | uniq

# Add runtime permission handling marker
# <uses-permission android:name="android.permission.CAMERA" tools:node="merge"/>
```

---

## SKILL 5: RESOURCE CONFLICT RESOLUTION

### Resource Prefixing
```bash
# Prefix all resource names in XML
find res/ -name "*.xml" -exec sed -i 's/name="btn_/name="donor_btn_/g' {} \;
find res/ -name "*.xml" -exec sed -i 's/name="ic_/name="donor_ic_/g' {} \;
find res/ -name "*.xml" -exec sed -i 's/@id\/btn_/@id\/donor_btn_/g' {} \;
```

### Resource File Renaming
```bash
# Rename drawable files
cd res/drawable
for file in ic_*.png; do
    mv "$file" "donor_$file"
done

# Update XML references
find ../layout -name "*.xml" -exec sed -i 's/@drawable\/ic_/@drawable\/donor_ic_/g' {} \;
```

### public.xml Management
```xml
<!-- Add to public.xml to prevent ID conflicts -->
<resources>
    <public type="id" name="donor_button" id="0x7f0a1000" />
    <public type="drawable" name="donor_icon" id="0x7f0801000" />
    <public type="string" name="donor_label" id="0x7f1001000" />
</resources>
```

---

## SKILL 6: SMALI SURGERY

### Method NOP-ing (Disable Telemetry)
```smali
# Original telemetry method
.method public sendAnalytics()V
    .locals 2
    # ... tracking code ...
    return-void
.end method

# NOP-ed version
.method public sendAnalytics()V
    .locals 0
    return-void
.end method
```

### SSL Pinning Removal
```smali
# Find certificate pinning check
.method checkCertificate()Z
    # Original returns boolean
    const/4 v0, 0x1  # Change 0x0 (false) to 0x1 (true)
    return v0
.end method
```

### Root Detection Bypass
```smali
# Find root check method
.method isRooted()Z
    .locals 1
    const/4 v0, 0x0  # Always return false
    return v0
.end method
```

### Adding Bridge Methods
```smali
# Bridge from Smali to Java
.method public static callDonorFeature(Ljava/lang/String;)V
    .locals 2
    
    # Call original donor method
    invoke-static {p0}, Lcom/donor/Feature;->process(Ljava/lang/String;)V
    
    return-void
.end method
```

---

## SKILL 7: BUILD & RECOMPILATION

### Standard Rebuild
```bash
# Rebuild APK
apktool b /decompiled_dir -o rebuilt.apk

# Zipalign
zipalign -v 4 rebuilt.apk aligned.apk

# Sign APK
apksigner sign --ks keystore.jks --ks-key-alias key0 aligned.apk

# Or use uber-apk-signer (automatic)
uber-apk-signer -a rebuilt.apk
```

### Error Resolution
```bash
# Resource not found error
# Fix: Add missing resource to res/values/public.xml
echo '<public type="id" name="missing_id" id="0x7f0a0999" />' >> res/values/public.xml

# Manifest merger error
# Fix: Add tools:replace attribute
# <application tools:replace="android:label">

# AAPT2 compile error
# Fix: Validate XML syntax
xmllint --noout res/layout/problematic.xml
```

### Incremental Build
```bash
# Build without resources (faster for testing)
apktool b -f decompiled_dir

# Build specific module
apktool b decompiled_dir/smali -o classes.dex
```

---

## SKILL 8: METADATA STRIPPING

### EXIF Removal
```bash
# Remove EXIF from images
exiftool -all= res/drawable/*.jpg
exiftool -all= res/drawable/*.png

# ImageMagick alternative
mogrify -strip res/drawable/*.jpg
```

### Timestamp Normalization
```bash
# Set all files to same timestamp
find . -type f -exec touch -t 202001010000 {} \;

# Or use specific date
touch -d "2020-01-01 00:00:00" $(find . -type f)
```

### Signature Removal
```bash
# Remove signing artifacts
rm -rf META-INF/
rm -rf original/

# Remove kotlin metadata
rm -rf kotlin/
```

---

## SKILL 9: ARCHITECTURE ADAPTATION

### Activity to Fragment Conversion
```kotlin
// Original Activity
class DonorActivity : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.donor_layout)
    }
}

// Converted Fragment
class DonorFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.donor_layout, container, false)
    }
}
```

### Callback to Coroutine Bridge
```kotlin
// Original callback-based
interface DonorCallback {
    fun onSuccess(data: String)
    fun onError(error: Throwable)
}

fun donorMethod(callback: DonorCallback) { ... }

// Coroutine bridge
suspend fun donorMethodAsync(): Result<String> = suspendCancellableCoroutine { continuation ->
    donorMethod(object : DonorCallback {
        override fun onSuccess(data: String) {
            continuation.resume(Result.success(data))
        }
        override fun onError(error: Throwable) {
            continuation.resume(Result.failure(error))
        }
    })
}
```

### RxJava to Flow Bridge
```kotlin
// RxJava Observable
val donorObservable: Observable<Data> = ...

// Convert to Flow
val targetFlow: Flow<Data> = donorObservable.asFlow()

// Or manual bridge
fun donorAsFlow(): Flow<Data> = callbackFlow {
    val subscription = donorObservable.subscribe(
        { data -> trySend(data) },
        { error -> close(error) },
        { close() }
    )
    awaitClose { subscription.dispose() }
}
```

---

## SKILL 10: VALIDATION & TESTING

### Static Analysis
```bash
# Decompile result and compare
jadx -d validation_output rebuilt.apk
diff -r original_source validation_output

# Check for donor remnants
grep -r "com.donor.package" rebuilt_decompiled/
grep -r "donor_string" rebuilt_decompiled/res/

# Verify resource linking
aapt2 dump resources rebuilt.apk | grep "error"
```

### Runtime Testing
```bash
# Install and test
adb install -r rebuilt.apk

# Monitor for crashes
adb logcat | grep -E "AndroidRuntime|FATAL"

# Check for network calls
adb logcat | grep -E "http|https"

# Memory profiling
adb shell dumpsys meminfo com.target.package
```

### Performance Validation
```bash
# APK size comparison
ls -lh original.apk rebuilt.apk

# Method count check
cat classes*.dex | head -c 92 | tail -c 4 | hexdump -e '1/4 "%d\n"'

# Startup time
adb shell am start -W com.target.package/.MainActivity
```

---

## SKILL 11: AUTOMATION SCRIPTS

### Batch Processing
```bash
#!/bin/bash
# Process multiple APKs

for apk in *.apk; do
    echo "Processing $apk"
    apktool d "$apk" -o "${apk%.apk}_decompiled"
    
    # Apply transformations
    find "${apk%.apk}_decompiled" -name "*.smali" -exec sed -i 's/OLD/NEW/g' {} \;
    
    # Rebuild
    apktool b "${apk%.apk}_decompiled" -o "${apk%.apk}_rebuilt.apk"
    uber-apk-signer -a "${apk%.apk}_rebuilt.apk"
done
```

### Error Auto-Recovery
```python
import subprocess
import re

def auto_fix_build(decompiled_path):
    while True:
        result = subprocess.run(['apktool', 'b', decompiled_path], 
                              capture_output=True, text=True)
        
        if result.returncode == 0:
            print("Build successful!")
            break
            
        # Parse error
        if "Resource not found" in result.stderr:
            match = re.search(r'@(\w+)/(\w+)', result.stderr)
            if match:
                res_type, res_name = match.groups()
                add_missing_resource(decompiled_path, res_type, res_name)
                continue
        
        # Other error handling...
        break

def add_missing_resource(path, res_type, res_name):
    # Add to public.xml or create placeholder
    pass
```

---

## SKILL 12: LOGGING & DOCUMENTATION

### rip_log.json Structure
```json
{
  "extraction_timestamp": "2026-02-15T10:30:00Z",
  "donor_apk": {
    "filename": "donor_app_v1.0.apk",
    "package": "com.donor.app",
    "version_code": 100,
    "version_name": "1.0.0"
  },
  "target_project": {
    "package": "com.myapp.project",
    "architecture": "MVVM",
    "di_framework": "Hilt",
    "reactive": "Coroutines + Flow"
  },
  "transformations": {
    "package_rename": {
      "from": "com.donor.app",
      "to": "com.myapp.project.features.extracted"
    },
    "resource_prefix": "extracted_",
    "classes_renamed": 47,
    "resources_renamed": 123
  },
  "dependencies": {
    "conflicts_resolved": [
      {
        "library": "okhttp3",
        "donor_version": "4.11.0",
        "target_version": "4.9.0",
        "resolution": "shaded to com.internal.http3"
      }
    ]
  },
  "stripped_components": [
    "com.google.firebase.analytics",
    "com.facebook.sdk",
    "io.sentry.android"
  ],
  "errors_resolved": [
    {
      "error": "Resource @drawable/splash not found",
      "resolution": "Created placeholder drawable",
      "timestamp": "2026-02-15T10:35:22Z"
    }
  ],
  "validation": {
    "build_successful": true,
    "runtime_tested": true,
    "no_crashes": true,
    "performance_impact": "< 5% startup time increase"
  }
}
```

---

## QUICK REFERENCE COMMANDS

```bash
# Essential workflow
apktool d app.apk                                    # Decompile
find . -name "*.smali" -exec sed 's/OLD/NEW/g' {} \; # Transform
apktool b decompiled_dir                             # Rebuild
uber-apk-signer -a rebuilt.apk                       # Sign
adb install -r rebuilt.apk                           # Test

# Analysis
jadx -d output app.apk                               # View Java
aapt2 dump resources app.apk                         # View resources
nm -D lib.so                                         # View native symbols

# Validation
adb logcat | grep AndroidRuntime                     # Monitor crashes
jadx -d verify rebuilt.apk                           # Verify output
grep -r "donor_package" rebuilt_output/              # Check remnants
```

This is your technical playbook. Reference these skills when executing CHOPPER operations.
