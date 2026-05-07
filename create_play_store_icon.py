from PIL import Image
import os

source_path = r"M:\n0tez\.github\workflows\Primary ApkImage.jpeg"
output_path = r"M:\n0tez\art\play_store_icon_512.png"

try:
    with Image.open(source_path) as img:
        # Convert to RGBA (32-bit)
        img = img.convert("RGBA")
        
        # Resize to 512x512
        img = img.resize((512, 512), Image.Resampling.LANCZOS)
        
        # Save as PNG
        img.save(output_path, "PNG")
        print(f"Successfully created Play Store icon at: {output_path}")
        
except Exception as e:
    print(f"Error creating Play Store icon: {e}")
