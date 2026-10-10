package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Abstract base class for custom Material Design views. Provides common functionality
 * for handling view attributes, animations, and background styling for Material Design
 * components.
 */
public abstract class CustomView extends RelativeLayout {
    protected static final String ANDROIDXML = "http://schemas.android.com/apk/res/android";
    protected static final String MATERIALDESIGNXML = "http://schemas.android.com/apk/res-auto";
    protected boolean animation;
    protected int backgroundColor;
    protected int backgroundResId;
    protected int beforeBackground;
    final int disabledBackgroundColor;
    public boolean isLastTouch;
    protected int minHeight;
    protected int minWidth;

    /**
     * Initializes default values for the custom view, such as minimum dimensions
     * and default colors. Subclasses must implement this method.
     */
    protected abstract void onInitDefaultValues();

    /**
     * Constructs a new CustomView with the specified context and attributes.
     *
     * @param context the context in which the view is running
     * @param attrs the attributes of the XML tag that is inflating the view
     */
    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.disabledBackgroundColor = Color.parseColor("#E2E2E2");
        this.backgroundResId = -1;
        this.animation = false;
        this.isLastTouch = false;
        onInitDefaultValues();
    }

    /**
     * Converts density-independent pixels (dp) to actual pixels (px) based on the screen density.
     *
     * @param dp the value in density-independent pixels
     * @param resources the resources object to retrieve display metrics from
     * @return the value in pixels
     */
    public static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(1, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    /**
     * Parses a string dimension value containing "dp" or "dip" and converts it to a float.
     *
     * @param value the dimension string (e.g., "16dp" or "24dip")
     * @return the numeric value as a float
     */
    public static float dipOrDpToFloat(String value) {
        String value2;
        if (value.contains("dp")) {
            value2 = value.replace("dp", "");
        } else {
            value2 = value.replace("dip", "");
        }
        return Float.parseFloat(value2);
    }

    /**
     * Calculates the absolute top position of a view relative to the screen.
     *
     * @param myView the view to get the position for
     * @return the top position in pixels
     */
    public static int getRelativeTop(View myView) {
        Rect bounds = new Rect();
        myView.getGlobalVisibleRect(bounds);
        return bounds.top;
    }

    /**
     * Calculates the absolute left position of a view relative to its root parent.
     *
     * @param myView the view to get the position for
     * @return the left position in pixels
     */
    public static int getRelativeLeft(View myView) {
        if (myView.getId() == 16908290) {
            return myView.getLeft();
        }
        return getRelativeLeft((View) myView.getParent()) + myView.getLeft();
    }

    /**
     * Applies XML attributes to the view, including minimum dimensions and background settings.
     *
     * @param attrs the attributes of the XML tag that is inflating the view
     */
    protected void setAttributes(AttributeSet attrs) {
        setMinimumHeight(dpToPx(this.minHeight, getResources()));
        setMinimumWidth(dpToPx(this.minWidth, getResources()));
        if (this.backgroundResId != -1 && !isInEditMode()) {
            setBackgroundResource(this.backgroundResId);
        }
        setBackgroundAttributes(attrs);
    }

    /**
     * Parses and applies background color attributes from XML to the view.
     *
     * @param attrs the attributes of the XML tag that is inflating the view
     */
    protected void setBackgroundAttributes(AttributeSet attrs) {
        int backgroundColor = attrs.getAttributeResourceValue(ANDROIDXML, "background", -1);
        if (backgroundColor != -1) {
            setBackgroundColor(getResources().getColor(backgroundColor));
            return;
        }
        int background = attrs.getAttributeIntValue(ANDROIDXML, "background", -1);
        if (background != -1 && !isInEditMode()) {
            setBackgroundColor(background);
        } else {
            setBackgroundColor(backgroundColor);
        }
    }

    @Override // android.view.View
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setBackgroundColor(this.beforeBackground);
        } else {
            setBackgroundColor(this.disabledBackgroundColor);
        }
    }

    @Override // android.view.View
    protected void onAnimationStart() {
        super.onAnimationStart();
        this.animation = true;
    }

    @Override // android.view.View
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        this.animation = false;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.animation) {
            invalidate();
        }
    }
}
