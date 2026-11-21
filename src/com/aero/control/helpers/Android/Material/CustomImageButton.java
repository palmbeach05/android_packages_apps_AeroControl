package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.aero.control.R;

/**
 * A custom Material Design style image button that wraps an ImageView
 * with Material styling and background support.
 */
public class CustomImageButton extends LinearLayout {
    private ImageView mImageView;

    /**
     * Constructs a new CustomImageButton with the specified context.
     *
     * @param context the context in which the button is running
     */
    public CustomImageButton(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructs a new CustomImageButton with the specified context and attributes.
     *
     * @param context the context in which the button is running
     * @param attrs the attributes of the XML tag that is inflating the view
     */
    public CustomImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructs a new CustomImageButton with the specified context, attributes, and style.
     *
     * @param context the context in which the button is running
     * @param attrs the attributes of the XML tag that is inflating the view
     * @param defStyle the default style to apply to this view
     */
    public CustomImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        inflater.inflate(R.layout.imagebutton_layout, (ViewGroup) this, true);
        this.mImageView = (ImageView) findViewById(R.id.image_button);
        setBackground(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src", R.drawable.info));
    }

    /**
     * Sets the background drawable for the image button.
     *
     * @param drawable the resource ID of the drawable to set as background
     */
    public void setBackground(int drawable) {
        if (Build.VERSION.SDK_INT < 16) {
            this.mImageView.setBackgroundDrawable(getResources().getDrawable(drawable));
        } else {
            this.mImageView.setBackground(getResources().getDrawable(drawable));
        }
    }
}
