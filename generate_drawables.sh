#!/bin/bash
# Generate all SVG vector drawables for Nebula Editor
# Run from project root

DRAWABLE="app/src/main/res/drawable"

# ic_nebula_logo
cat > "$DRAWABLE/ic_nebula_logo.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="28dp" android:height="28dp" android:viewportWidth="28" android:viewportHeight="28">
    <path android:fillColor="#FF6C8FFF" android:pathData="M14,14m-4,0a4,4 0,1 1,8 0a4,4 0,1 1,-8 0"/>
    <path android:strokeColor="#FFA855F7" android:strokeWidth="1.2" android:fillColor="#00000000"
        android:pathData="M14,1 C14,1 26,7 26,14 C26,21 14,27 14,27 C14,27 2,21 2,14 C2,7 14,1 14,1Z"/>
    <path android:strokeColor="#FF22D3EE" android:strokeWidth="0.8" android:fillColor="#00000000"
        android:pathData="M1,14 Q14,8 27,14 Q14,20 1,14Z"/>
</vector>
EOF

# ic_undo
cat > "$DRAWABLE/ic_undo.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M9,14L4,9l5,-5"/>
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M4,9h11a5,5 0,0 1,0 10h-3"/>
</vector>
EOF

# ic_redo
cat > "$DRAWABLE/ic_redo.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M15,14l5,-5 -5,-5"/>
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M20,9H9a5,5 0,0 0,0 10h3"/>
</vector>
EOF

# ic_camera
cat > "$DRAWABLE/ic_camera.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M23,19a2,2 0,0 1,-2 2H3a2,2 0,0 1,-2 -2V8a2,2 0,0 1,2 -2h4l2,-3h6l2,3h4a2,2 0,0 1,2 2z"/>
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:fillColor="#00000000" android:pathData="M12,9m-4,0a4,4 0,1 1,8 0a4,4 0,1 1,-8 0"/>
</vector>
EOF

# ic_delete
cat > "$DRAWABLE/ic_delete.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M3,6h18M19,6v14a2,2 0,0 1,-2 2H7a2,2 0,0 1,-2 -2V6m3,0V4a1,1 0,0 1,1 -1h4a1,1 0,0 1,1 1v2"/>
</vector>
EOF

# ic_add_media
cat > "$DRAWABLE/ic_add_media.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M12,5v14M5,12h14"/>
</vector>
EOF

# ic_keyframe
cat > "$DRAWABLE/ic_keyframe.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/text_secondary"
        android:pathData="M12,2l2.4,7.4H22l-6.2,4.5 2.4,7.4L12,17l-6.2,4.3 2.4,-7.4L2,9.4h7.6z"/>
</vector>
EOF

# ic_media
cat > "$DRAWABLE/ic_media.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M2,2h20v20H2zM7,2v20M17,2v20M2,12h20M2,7h5M17,7h5M2,17h5M17,17h5"/>
</vector>
EOF

# ic_text
cat > "$DRAWABLE/ic_text.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M4,7V4h16v3M9,20h6M12,4v16"/>
</vector>
EOF

# ic_fx
cat > "$DRAWABLE/ic_fx.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M13,2L3,14h9l-1,8 10,-12h-9z"/>
</vector>
EOF

# ic_audio
cat > "$DRAWABLE/ic_audio.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M9,18V5l12,-2v13M6,21m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0M18,19m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0"/>
</vector>
EOF

# ic_background
cat > "$DRAWABLE/ic_background.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M3,3h18v18H3zM8.5,8.5m-1.5,0a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0M21,15l-5,-5L5,21"/>
</vector>
EOF

# ic_settings
cat > "$DRAWABLE/ic_settings.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M12,12m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0M12,2v2M12,20v2M4.93,4.93l1.41,1.41M17.66,17.66l1.41,1.41M2,12h2M20,12h2M4.93,19.07l1.41,-1.41M17.66,6.34l1.41,-1.41"/>
</vector>
EOF

# ic_ratio
cat > "$DRAWABLE/ic_ratio.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M2,4h20v16H2zM9,4v16M15,4v16M2,12h20"/>
</vector>
EOF

# ic_crop
cat > "$DRAWABLE/ic_crop.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M6.13,1L6,16a2,2 0,0 0,2 2h15M1,6.13L16,6a2,2 0,0 1,2 2v15"/>
</vector>
EOF

# ic_rotate
cat > "$DRAWABLE/ic_rotate.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M1,4v6h6M3.51,15a9,9 0,1 0,0.49,-5.47"/>
</vector>
EOF

# ic_split
cat > "$DRAWABLE/ic_split.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M12,2v20M2,12l4,-4 4,4 4,-4 4,4 4,-4"/>
</vector>
EOF

# ic_reverse
cat > "$DRAWABLE/ic_reverse.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M2.5,2v6h6M2.66,15.57a10,10 0,1 0,0.57,-8.38"/>
</vector>
EOF

# ic_replace
cat > "$DRAWABLE/ic_replace.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M17,1l4,4 -4,4M3,11V9a4,4 0,0 1,4 -4h14M7,23l-4,-4 4,-4M21,13v2a4,4 0,0 1,-4 4H3"/>
</vector>
EOF

# ic_duplicate
cat > "$DRAWABLE/ic_duplicate.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M9,9h13v13H9zM5,15H4a2,2 0,0 1,-2 -2V4a2,2 0,0 1,2 -2h9a2,2 0,0 1,2 2v1"/>
</vector>
EOF

# ic_copy
cat > "$DRAWABLE/ic_copy.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M8,17l4,4 4,-4M12,12v9M20.88,18.09A5,5 0,0 0,18 9h-1.26A8,8 0,1 0,3 16.29"/>
</vector>
EOF

# ic_freeze
cat > "$DRAWABLE/ic_freeze.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M2,3h20v14H2zM8,21h8M12,17v4M9,9v4M12,7v8M15,9v4"/>
</vector>
EOF

# ic_pip
cat > "$DRAWABLE/ic_pip.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M2,4h20v16H2z"/>
    <path android:fillColor="@color/text_secondary" android:fillAlpha="0.5"
        android:pathData="M12,12h9v6H12z"/>
</vector>
EOF

# ic_sticker
cat > "$DRAWABLE/ic_sticker.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M20.59,13.41l-7.17,7.17a2,2 0,0 1,-2.83 0L2,12V2h10l8.59,8.59a2,2 0,0 1,0 2.82zM7,7h0.01"/>
</vector>
EOF

# ic_speed
cat > "$DRAWABLE/ic_speed.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M13,2L3,14h9l-1,8 10,-12h-9z"/>
</vector>
EOF

# ic_volume
cat > "$DRAWABLE/ic_volume.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M11,5L6,9H2v6h4l5,4zM19.07,4.93a10,10 0,0 1,0 14.14M15.54,8.46a5,5 0,0 1,0 7.07"/>
</vector>
EOF

# ic_play
cat > "$DRAWABLE/ic_play.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/white" android:pathData="M5,3L19,12 5,21z"/>
</vector>
EOF

# ic_pause
cat > "$DRAWABLE/ic_pause.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/white" android:pathData="M6,4h4v16H6zM14,4h4v16h-4z"/>
</vector>
EOF

# ic_skip_start
cat > "$DRAWABLE/ic_skip_start.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/text_secondary"
        android:pathData="M19,20L9,12 19,4zM5,19V5h2v14z"/>
</vector>
EOF

# ic_skip_back
cat > "$DRAWABLE/ic_skip_back.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/text_secondary"
        android:pathData="M19,20L9,12 19,4zM5,20L9,12 5,4z"/>
</vector>
EOF

# ic_skip_forward
cat > "$DRAWABLE/ic_skip_forward.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/text_secondary"
        android:pathData="M5,4L15,12 5,20zM19,4L15,12 19,20z"/>
</vector>
EOF

# ic_skip_end
cat > "$DRAWABLE/ic_skip_end.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/text_secondary"
        android:pathData="M5,4L15,12 5,20zM17,5v14h2V5z"/>
</vector>
EOF

# ic_fullscreen
cat > "$DRAWABLE/ic_fullscreen.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M8,3H5a2,2 0,0 0,-2 2v3M21,8V5a2,2 0,0 0,-2 -2h-3M3,16v3a2,2 0,0 0,2 2h3M16,21h3a2,2 0,0 0,2 -2v-3"/>
</vector>
EOF

# ic_magnet
cat > "$DRAWABLE/ic_magnet.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/accent_nova" android:strokeWidth="2.5"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M6,15A6,6 0,1 0,18 15v-3H6v3zM12,3v5M6,12V8M18,12V8"/>
</vector>
EOF

# ic_zoom_in
cat > "$DRAWABLE/ic_zoom_in.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_muted" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M11,11m-8,0a8,8 0,1 1,16 0a8,8 0,1 1,-16 0M21,21l-4.35,-4.35M11,8v6M8,11h6"/>
</vector>
EOF

# ic_zoom_out
cat > "$DRAWABLE/ic_zoom_out.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_muted" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M11,11m-8,0a8,8 0,1 1,16 0a8,8 0,1 1,-16 0M21,21l-4.35,-4.35M8,11h6"/>
</vector>
EOF

# ic_close
cat > "$DRAWABLE/ic_close.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/text_secondary" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M18,6L6,18M6,6l12,12"/>
</vector>
EOF

# ic_stop
cat > "$DRAWABLE/ic_stop.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/text_secondary" android:pathData="M6,6h12v12H6z"/>
</vector>
EOF

# ic_btn_icon_small (alias)
cp "$DRAWABLE/bg_btn_icon.xml" "$DRAWABLE/bg_btn_icon_small.xml"

echo "All drawables generated."
