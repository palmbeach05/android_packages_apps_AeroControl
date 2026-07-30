package com.aero.control.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

/* JADX INFO: loaded from: classes.dex */
public class CardLayout extends LinearLayout implements ViewTreeObserver.OnGlobalLayoutListener {
    public CardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayoutObserver();
    }

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
