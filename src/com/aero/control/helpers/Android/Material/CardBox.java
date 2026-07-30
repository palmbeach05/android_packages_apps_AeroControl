package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.aero.control.R;

/* JADX INFO: loaded from: classes.dex */
public class CardBox extends LinearLayout {
    private ImageView mImageView;
    private TextView mTextTitle;

    public CardBox(Context context) {
        super(context);
        init(context, null);
    }

    public CardBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CardBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        String title = "";
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CardBox, 0, 0);
            title = ta.getString(0);
            ta.recycle();
        }
        setOrientation(0);
        setGravity(16);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        inflater.inflate(R.layout.cardbox_layout, (ViewGroup) this, true);
        this.mTextTitle = (TextView) findViewById(R.id.card_title);
        this.mImageView = (ImageView) findViewById(R.id.card_content);
        this.mTextTitle.setText(title);
    }

    public void setTitle(String title) {
        this.mTextTitle.setText(title);
        invalidate();
    }

    public String getTitle() {
        return this.mTextTitle.getText().toString();
    }

    public void setContent(Drawable drawable) {
        this.mImageView.setImageDrawable(drawable);
        invalidate();
    }

    public Drawable getContent() {
        return this.mImageView.getDrawable();
    }

    public void setBackground(int drawable) {
        if (Build.VERSION.SDK_INT < 16) {
            setBackgroundDrawable(getResources().getDrawable(drawable));
        } else {
            setBackground(getResources().getDrawable(drawable));
        }
    }
}
