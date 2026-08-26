package com.aero.control.helpers;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * ViewPager page transformer that applies a zoom-out animation effect as pages are
 * scrolled. Pages scale down and fade as they move away from the center.
 */
public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
    private static final float MIN_ALPHA = 0.5f;
    private static final float MIN_SCALE = 0.85f;

    /**
     * Applies the zoom-out transformation to the specified page based on its position
     * relative to the center of the screen.
     *
     * @param view the page view to transform
     * @param position the position of the page relative to the center (-1 to 1)
     */
    @Override // android.support.v4.view.ViewPager.PageTransformer
    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();
        if (position < -1.0f) {
            view.setAlpha(0.0f);
            return;
        }
        if (position <= 1.0f) {
            float scaleFactor = Math.max(MIN_SCALE, 1.0f - Math.abs(position));
            float vertMargin = (pageHeight * (1.0f - scaleFactor)) / 2.0f;
            float horzMargin = (pageWidth * (1.0f - scaleFactor)) / 2.0f;
            if (position < 0.0f) {
                view.setTranslationX(horzMargin - (vertMargin / 2.0f));
            } else {
                view.setTranslationX((-horzMargin) + (vertMargin / 2.0f));
            }
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            view.setAlpha((((scaleFactor - MIN_SCALE) / 0.14999998f) * MIN_ALPHA) + MIN_ALPHA);
            return;
        }
        view.setAlpha(0.0f);
    }
}
