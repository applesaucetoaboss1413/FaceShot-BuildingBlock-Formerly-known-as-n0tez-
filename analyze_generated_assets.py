import os
from PIL import Image

res_dir = r"M:\n0tez\app\src\main\res"

expected_sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

def check_generated_assets():
    print("--- Generated Asset Analysis ---")
    
    # Check Floating Bubble (drawable folders)
    print("\n[Floating Bubble]")
    for density, size in expected_sizes.items():
        path = os.path.join(res_dir, f"drawable-{density}", "ic_floating_bubble.png")
        if os.path.exists(path):
            try:
                with Image.open(path) as img:
                    status = "✅" if img.size == (size, size) else "❌ SIZE MISMATCH"
                    print(f"{status} {density}: {img.size} (Expected: {size}x{size})")
            except Exception as e:
                print(f"❌ {density}: Error reading - {e}")
        else:
            print(f"❌ {density}: File missing at {path}")

    # Check Launcher (mipmap folders)
    print("\n[Launcher Icons]")
    for density, size in expected_sizes.items():
        path = os.path.join(res_dir, f"mipmap-{density}", "ic_launcher.png")
        if os.path.exists(path):
            try:
                with Image.open(path) as img:
                    status = "✅" if img.size == (size, size) else "❌ SIZE MISMATCH"
                    print(f"{status} {density}: {img.size} (Expected: {size}x{size})")
            except Exception as e:
                print(f"❌ {density}: Error reading - {e}")
        else:
            print(f"❌ {density}: File missing at {path}")

    # Check Adaptive Foreground
    print("\n[Adaptive Foregrounds]")
    adaptive_sizes = {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432
    }
    for density, size in adaptive_sizes.items():
        path = os.path.join(res_dir, f"mipmap-{density}", "ic_launcher_foreground.png")
        if os.path.exists(path):
            try:
                with Image.open(path) as img:
                    status = "✅" if img.size == (size, size) else "❌ SIZE MISMATCH"
                    print(f"{status} {density}: {img.size} (Expected: {size}x{size})")
            except Exception as e:
                print(f"❌ {density}: Error reading - {e}")
        else:
            print(f"❌ {density}: File missing at {path}")

if __name__ == "__main__":
    check_generated_assets()
