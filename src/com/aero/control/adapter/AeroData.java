package com.aero.control.adapter;

import android.graphics.drawable.Drawable;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AeroData {
    public String content;
    public int file;
    public Drawable image;
    public boolean isChecked = false;
    public String name;
    public String right_name;
    /**
     * Per-core frequency strings for the Overview frequency grid. A non-null
     * list of size 1-8 signals grid rendering in AeroAdapter; null or a list
     * larger than 8 entries falls back to plain content rendering.
     */
    public List<String> coreFrequencies;

    public AeroData(String name, String content, String right_name) {
        this.name = name;
        this.content = content;
        this.right_name = right_name;
    }

    public AeroData(int file, String content) {
        this.file = file;
        this.content = content;
    }

    public AeroData(Drawable image, String name) {
        this.image = image;
        this.name = name;
    }
}
