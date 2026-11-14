package com.aero.control.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

/**
 * Custom LinearLayout that automatically sets vertical orientation and observes
 * layout changes. Used as a container for card-style UI elements.
 */
public class CardLayout extends LinearLayout implements ViewTreeObserver.OnGlobalLayoutListener {
    /**
     * Constructs a CardLayout with the specified context and attribute set.
     *
     * @param context the context
     * @param attrs the attribute set from XML
     */
    public CardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayoutObserver();
    }

    /**
     * Constructs a CardLayout with the specified context.
     *
     * @param context the context
     */
    public CardLayout(Context context) {
        super(context);
        initLayoutObserver();
    }

    private void initLayoutObserver() {
        setOrientation(1);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
    public void onGlobalLayout() {
    }
}
