package com.aero.control.adapter;

import android.graphics.drawable.Drawable;

/* JADX INFO: loaded from: classes.dex */
public class AeroData {
    public String content;
    public int file;
    public Drawable image;
    public boolean isChecked = false;
    public String name;
    public String right_name;

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
