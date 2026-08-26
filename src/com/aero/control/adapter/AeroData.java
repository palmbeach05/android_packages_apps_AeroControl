package com.aero.control.adapter;

import android.graphics.drawable.Drawable;
import java.util.List;

/**
 * Data model for items displayed in various list and grid adapters throughout
 * the application. Supports multiple display formats including text, images,
 * and multi-core CPU frequency grids.
 */
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

    /**
     * Creates a data item with a name, content, and optional right-aligned name.
     *
     * @param name the main label or title
     * @param content the primary content or value
     * @param right_name optional text to display on the right side
     */
    public AeroData(String name, String content, String right_name) {
        this.name = name;
        this.content = content;
        this.right_name = right_name;
    }

    /**
     * Creates a data item with a resource ID and content string.
     *
     * @param file the resource identifier
     * @param content the content text
     */
    public AeroData(int file, String content) {
        this.file = file;
        this.content = content;
    }

    /**
     * Creates a data item with a drawable image and name.
     *
     * @param image the drawable to display
     * @param name the label or title
     */
    public AeroData(Drawable image, String name) {
        this.image = image;
        this.name = name;
    }
}
