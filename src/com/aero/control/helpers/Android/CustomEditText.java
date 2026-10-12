package com.aero.control.helpers.Android;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * A custom EditText widget that extends the standard Android EditText.
 * Provides a base for additional customization and styling specific to the application.
 */
public class CustomEditText extends EditText {
    /**
     * Creates a new CustomEditText with default attributes.
     *
     * @param context the context in which the view is created
     */
    public CustomEditText(Context context) {
        super(context);
    }

    /**
     * Creates a new CustomEditText with XML attributes.
     *
     * @param context the context in which the view is created
     * @param attrs the attribute set from XML
     */
    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates a new CustomEditText with XML attributes and a default style.
     *
     * @param context the context in which the view is created
     * @param attrs the attribute set from XML
     * @param defStyle the default style resource
     */
    public CustomEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
