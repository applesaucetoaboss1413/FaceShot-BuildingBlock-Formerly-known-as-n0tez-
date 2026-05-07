import os
from PIL import Image

source_bubble = r"M:\n0tez\.github\workflows\Floating Overlay Image.jpeg"
source_launcher = r"M:\n0tez\.github\workflows\Primary ApkImage.jpeg"

def check_image(path, name):
    if not os.path.exists(path):
        print(f"❌ {name} NOT FOUND at {path}")
        return
    
    try:
        with Image.open(path) as img:
            print(f"✅ {name}:")
            print(f"   - Path: {path}")
            print(f"   - Format: {img.format}")
            print(f"   - Size: {img.size} (Width x Height)")
            print(f"   - Mode: {img.mode}")
            
            # Check for potential quality issues
            if img.width < 512 or img.height < 512:
                print(f"   - ⚠️ WARNING: Source resolution ({img.size}) is lower than the target max (512x512). Upscaling will occur.")
            
            if img.width != img.height:
                print(f"   - ⚠️ WARNING: Image is not square. Distortion may occur if forced into a square container.")

    except Exception as e:
        print(f"❌ Error analyzing {name}: {e}")

if __name__ == "__main__":
    print("--- Source Image Analysis ---")
    check_image(source_bubble, "Floating Bubble Source")
    check_image(source_launcher, "Primary Launcher Source")
