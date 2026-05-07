package com.n0tez.app;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.n0tez.app.databinding.ActivityMediaGalleryBinding;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MediaGalleryActivity extends AppCompatActivity {
    private MediaAdapter adapter;
    private ActivityMediaGalleryBinding binding;
    private final List<MediaItem> displayItems = new ArrayList<>();
    private final List<MediaItem> allMediaItems = new ArrayList<>();
    private MediaType currentFilter = null;

    public enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO
    }

    public static final class MediaItem {
        private final long dateModified;
        private final File file;
        private final String name;
        private final MediaType type;

        public MediaItem(File file, MediaType type, String name, long dateModified) {
            this.file = file;
            this.type = type;
            this.name = name;
            this.dateModified = dateModified;
        }

        public final File getFile() { return this.file; }
        public final MediaType getType() { return this.type; }
        public final String getName() { return this.name; }
        public final long getDateModified() { return this.dateModified; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupUI();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadMedia();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Multimedia Gallery");
        }

        adapter = new MediaAdapter(displayItems, this::showMediaOptions);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerView.setAdapter(adapter);

        binding.chipAll.setOnClickListener(v -> filterMedia(null));
        binding.chipImages.setOnClickListener(v -> filterMedia(MediaType.IMAGE));
        binding.chipVideos.setOnClickListener(v -> filterMedia(MediaType.VIDEO));
        binding.chipAudio.setOnClickListener(v -> filterMedia(MediaType.AUDIO));
    }

    private void loadMedia() {
        new Thread(() -> {
            allMediaItems.clear();
            
            // Scan standard directories
            scanDirectory(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FaceShot-BuildingBlock"));
            scanDirectory(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "FaceShot-BuildingBlock"));
            scanDirectory(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "FaceShot-BuildingBlock"));
            
            // Also scan external files dir if used
            File externalFiles = getExternalFilesDir(null);
            if (externalFiles != null) {
                scanDirectory(externalFiles);
            }
    
            // Sort by date modified desc
            Collections.sort(allMediaItems, (o1, o2) -> Long.compare(o2.dateModified, o1.dateModified));
            
            runOnUiThread(() -> filterMedia(currentFilter));
        }).start();
    }

    private void scanDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName();
                        if (isImageFile(name)) {
                            allMediaItems.add(new MediaItem(file, MediaType.IMAGE, name, file.lastModified()));
                        } else if (isVideoFile(name)) {
                            allMediaItems.add(new MediaItem(file, MediaType.VIDEO, name, file.lastModified()));
                        } else if (isAudioFile(name)) {
                            allMediaItems.add(new MediaItem(file, MediaType.AUDIO, name, file.lastModified()));
                        }
                    }
                }
            }
        }
    }

    private void filterMedia(MediaType type) {
        currentFilter = type;
        displayItems.clear();
        if (type == null) {
            displayItems.addAll(allMediaItems);
        } else {
            for (MediaItem item : allMediaItems) {
                if (item.type == type) {
                    displayItems.add(item);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showMediaOptions(MediaItem item) {
        String[] options = {"Open", "Share", "Edit", "Export to Device", "Delete"};
        new AlertDialog.Builder(this)
            .setTitle(item.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: openMedia(item); break;
                    case 1: shareMedia(item); break;
                    case 2: editMedia(item); break;
                    case 3: exportMedia(item); break;
                    case 4: deleteMedia(item); break;
                }
            })
            .show();
    }

    private void openMedia(MediaItem item) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", item.getFile());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(item));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareMedia(MediaItem item) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", item.getFile());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(getMimeType(item));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share " + item.getName()));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void editMedia(MediaItem item) {
        Intent intent = null;
        switch (item.getType()) {
            case IMAGE:
                intent = new Intent(this, PhotoEditorActivity.class);
                intent.putExtra("IMAGE_FILE_PATH", item.getFile().getAbsolutePath());
                break;
            case VIDEO:
                intent = new Intent(this, VideoEditorActivity.class);
                intent.putExtra("VIDEO_FILE_PATH", item.getFile().getAbsolutePath());
                break;
            case AUDIO:
                intent = new Intent(this, VoiceRecorderActivity.class); // Assuming VoiceRecorder can edit or just play
                intent.putExtra("AUDIO_FILE_PATH", item.getFile().getAbsolutePath());
                break;
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    private void exportMedia(MediaItem item) {
        try {
            switch (item.getType()) {
                case IMAGE: exportImageToGallery(item); break;
                case VIDEO: exportVideoToGallery(item); break;
                case AUDIO: exportAudioToDevice(item); break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportImageToGallery(MediaItem item) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, item.getName());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceShot-BuildingBlock");
        }
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            copyFileToUri(item.getFile(), uri);
            Toast.makeText(this, "Image exported to gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportVideoToGallery(MediaItem item) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, item.getName());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FaceShot-BuildingBlock");
        }
        Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            copyFileToUri(item.getFile(), uri);
            Toast.makeText(this, "Video exported to gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportAudioToDevice(MediaItem item) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, item.getName());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/m4a");
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/FaceShot-BuildingBlock");
        }
        Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            copyFileToUri(item.getFile(), uri);
            Toast.makeText(this, "Audio exported to Music folder", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFileToUri(File src, Uri destUri) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(src);
             OutputStream outputStream = getContentResolver().openOutputStream(destUri)) {
            if (outputStream == null) return;
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void deleteMedia(MediaItem item) {
        new AlertDialog.Builder(this)
            .setTitle("Delete " + item.getName() + "?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (item.getFile().delete()) {
                    Toast.makeText(this, "Deleted " + item.getName(), Toast.LENGTH_SHORT).show();
                    loadMedia();
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private String getMimeType(MediaItem item) {
        switch (item.getType()) {
            case IMAGE: return "image/*";
            case VIDEO: return "video/*";
            case AUDIO: return "audio/*";
            default: return "*/*";
        }
    }

    private boolean isImageFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    private boolean isVideoFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".3gp");
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".ogg") || lower.endsWith(".flac");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Adapter
    public static class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
        private final List<MediaItem> items;
        private final OnItemClickListener onItemClick;

        public interface OnItemClickListener {
            void onItemClick(MediaItem item);
        }

        public MediaAdapter(List<MediaItem> items, OnItemClickListener onItemClick) {
            this.items = items;
            this.onItemClick = onItemClick;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView fileName;
            final ImageView thumbnail;
            final ImageView typeIcon;

            public ViewHolder(View view) {
                super(view);
                thumbnail = view.findViewById(R.id.thumbnail);
                typeIcon = view.findViewById(R.id.typeIcon);
                fileName = view.findViewById(R.id.fileName);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MediaItem item = items.get(position);
            holder.fileName.setText(item.getName());
            
            switch (item.getType()) {
                case IMAGE:
                    holder.typeIcon.setImageResource(R.drawable.ic_note); // Placeholder/Icon
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        Bitmap bitmap = BitmapFactory.decodeFile(item.getFile().getAbsolutePath(), options);
                        holder.thumbnail.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        holder.thumbnail.setImageResource(R.drawable.ic_note);
                    }
                    break;
                case VIDEO:
                    holder.typeIcon.setImageResource(R.drawable.ic_play); // Ensure ic_play exists
                    holder.thumbnail.setImageResource(R.drawable.ic_widget); // Placeholder
                    break;
                case AUDIO:
                    holder.typeIcon.setImageResource(R.drawable.ic_mic); // Ensure ic_mic exists
                    holder.thumbnail.setImageResource(R.drawable.ic_mic);
                    break;
            }
            
            holder.itemView.setOnClickListener(v -> onItemClick.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
