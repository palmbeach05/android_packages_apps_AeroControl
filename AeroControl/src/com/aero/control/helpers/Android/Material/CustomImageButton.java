package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.aero.control.R;

/* JADX INFO: loaded from: classes.dex */
public class CustomImageButton extends LinearLayout {
    private ImageView mImageView;

    public CustomImageButton(Context context) {
        super(context);
        init(context, null);
    }

    public CustomImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

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

    public void setBackground(int drawable) {
        if (Build.VERSION.SDK_INT < 16) {
            this.mImageView.setBackgroundDrawable(getResources().getDrawable(drawable));
        } else {
            this.mImageView.setBackground(getResources().getDrawable(drawable));
        }
    }
}
