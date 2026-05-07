import os
from PIL import Image, ImageDraw, ImageOps

# Source paths
BUBBLE_SOURCE = r"M:\n0tez\.github\workflows\Floating Overlay Image.jpeg"
LAUNCHER_SOURCE = r"M:\n0tez\.github\workflows\Primary ApkImage.jpeg"

# Output base paths
RES_PATH = r"app/src/main/res"
IOS_PATH = r"distribution/ios/AppIcon.appiconset"

# Ensure directories exist
os.makedirs(IOS_PATH, exist_ok=True)

# Density map for Android
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

# Requested resolutions for general optimized export
RESOLUTIONS = [48, 72, 96, 144, 192, 512]

def make_circle(img):
    mask = Image.new('L', img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0) + img.size, fill=255)
    output = ImageOps.fit(img, mask.size, centering=(0.5, 0.5))
    output.putalpha(mask)
    return output

def process_bubble():
    print("Processing Bubble Icon...")
    try:
        img = Image.open(BUBBLE_SOURCE).convert("RGBA")
        
        # Create circular version for the bubble
        circle_img = make_circle(img)

        # 1. Export optimized versions (generic folder or specific drawable folders)
        # We will put them in drawable folders for the app to use
        for name, size in DENSITIES.items():
            folder = os.path.join(RES_PATH, f"drawable-{name}")
            os.makedirs(folder, exist_ok=True)
            resized = circle_img.resize((size, size), Image.Resampling.LANCZOS)
            resized.save(os.path.join(folder, "ic_floating_bubble.png"), "PNG")
            print(f"Saved {name} bubble to {folder}")
        
        # Also save the 512 version (Play Store / High Res)
        folder = os.path.join(RES_PATH, "drawable-nodpi")
        os.makedirs(folder, exist_ok=True)
        circle_img.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(folder, "ic_floating_bubble_large.png"), "PNG")
        print("Saved 512px bubble")

    except Exception as e:
        print(f"Error processing bubble: {e}")

def process_launcher():
    print("Processing Launcher Icon...")
    try:
        img = Image.open(LAUNCHER_SOURCE).convert("RGBA")
        
        # 2. Launcher Icon (Mipmap)
        # Standard square/rounded icons for legacy
        # Adaptive icons: Foreground (image) + Background (white)
        
        # Background color (white)
        bg = Image.new("RGBA", (1024, 1024), (255, 255, 255, 255))
        
        for name, size in DENSITIES.items():
            folder = os.path.join(RES_PATH, f"mipmap-{name}")
            os.makedirs(folder, exist_ok=True)
            
            # Legacy Icon (Simple resize)
            resized = img.resize((size, size), Image.Resampling.LANCZOS)
            resized.save(os.path.join(folder, "ic_launcher.png"), "PNG")
            
            # Round Icon (Circular mask)
            round_icon = make_circle(img.resize((size, size), Image.Resampling.LANCZOS))
            round_icon.save(os.path.join(folder, "ic_launcher_round.png"), "PNG")
            
            # Adaptive Foreground (108dp -> px. 108dp at mdpi is 108px. wait. 
            # mdpi 1dp=1px. Icon size 48dp. Adaptive icon is 108dp * density.)
            # Density factors: mdpi=1, hdpi=1.5, xhdpi=2, xxhdpi=3, xxxhdpi=4
            
            adaptive_size_map = {
                "mdpi": 108,
                "hdpi": 162,
                "xhdpi": 216,
                "xxhdpi": 324,
                "xxxhdpi": 432
            }
            
            adaptive_size = adaptive_size_map.get(name, 108)
            
            # Foreground: Center the image in the 108dp canvas (72dp safe zone)
            # We'll make the image 72dp (2/3 of 108dp) and center it
            fg_image_size = int(adaptive_size * (72/108)) 
            fg_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
            
            resized_fg = img.resize((fg_image_size, fg_image_size), Image.Resampling.LANCZOS)
            offset = (adaptive_size - fg_image_size) // 2
            fg_canvas.paste(resized_fg, (offset, offset))
            
            fg_canvas.save(os.path.join(folder, "ic_launcher_foreground.png"), "PNG")
            
            print(f"Saved {name} launcher icons to {folder}")

        # iOS Icons
        # 1x=60, 2x=120, 3x=180 (iPhone)
        # 1x=76, 2x=152 (iPad)
        # 1024 (Marketing)
        ios_sizes = {
            "Icon-60.png": 60,
            "Icon-60@2x.png": 120,
            "Icon-60@3x.png": 180,
            "Icon-76.png": 76,
            "Icon-76@2x.png": 152,
            "Icon-1024.png": 1024
        }
        
        for filename, size in ios_sizes.items():
            resized = img.resize((size, size), Image.Resampling.LANCZOS)
            resized.save(os.path.join(IOS_PATH, filename), "PNG")
            print(f"Saved iOS icon {filename}")
            
    except Exception as e:
        print(f"Error processing launcher: {e}")

if __name__ == "__main__":
    process_bubble()
    process_launcher()
