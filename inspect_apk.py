import zipfile
import os
import datetime

apk_path = r"app/build/outputs/apk/debug/app-debug.apk"

def analyze_apk(path):
    if not os.path.exists(path):
        print(f"Error: APK not found at {path}")
        return

    size_bytes = os.path.getsize(path)
    size_mb = size_bytes / (1024 * 1024)
    mod_time = os.path.getmtime(path)
    timestamp = datetime.datetime.fromtimestamp(mod_time).strftime('%Y-%m-%d %H:%M:%S')

    print(f"APK Path: {path}")
    print(f"Size: {size_mb:.2f} MB ({size_bytes} bytes)")
    print(f"Last Modified: {timestamp}")

    try:
        with zipfile.ZipFile(path, 'r') as z:
            file_list = z.namelist()
            
            print("\n--- Key Components ---")
            has_manifest = "AndroidManifest.xml" in file_list
            has_dex = any(f.endswith(".dex") for f in file_list)
            has_res = any(f.startswith("res/") for f in file_list)
            has_meta = any(f.startswith("META-INF/") for f in file_list)
            
            print(f"AndroidManifest.xml: {'✅ Found' if has_manifest else '❌ Missing'}")
            print(f"classes.dex: {'✅ Found' if has_dex else '❌ Missing'}")
            print(f"Resources (res/): {'✅ Found' if has_res else '❌ Missing'}")
            print(f"META-INF (Signing): {'✅ Found' if has_meta else '❌ Missing'}")

            print("\n--- Native Libraries (ABIs) ---")
            abis = set()
            for f in file_list:
                if f.startswith("lib/") and f.count("/") >= 2:
                    # lib/arch/libname.so
                    parts = f.split("/")
                    abis.add(parts[1])
            
            if abis:
                print(f"Supported Architectures: {', '.join(sorted(abis))}")
            else:
                print("No native libraries found (lib/ folder empty or missing).")

            print("\n--- Icon Validation ---")
            icons = [f for f in file_list if "ic_launcher" in f or "ic_floating_bubble" in f]
            print(f"Found {len(icons)} icon assets.")
            # Sample a few
            for icon in icons[:5]:
                print(f"  - {icon}")

    except Exception as e:
        print(f"Error reading APK: {e}")

if __name__ == "__main__":
    analyze_apk(apk_path)
