package com.aero.control.helpers.Android;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.aero.control.R;

/**
 * A circular page indicator for ViewPager. Draws a series of circles representing
 * pages, with the current page filled and interactive touch support for navigating
 * between pages.
 */
public class CirclePageIndicator extends View implements PageIndicator {
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId;
    private boolean mCentered;
    private int mCurrentPage;
    private boolean mIsDragging;
    private float mLastMotionX;
    private ViewPager.OnPageChangeListener mListener;
    private int mOrientation;
    private float mPageOffset;
    private final Paint mPaintFill;
    private final Paint mPaintPageFill;
    private final Paint mPaintStroke;
    private float mRadius;
    private int mScrollState;
    private boolean mSnap;
    private int mSnapPage;
    private int mTouchSlop;
    private ViewPager mViewPager;

    /**
     * Creates a new CirclePageIndicator with default attributes.
     *
     * @param context the context in which the indicator is created
     */
    public CirclePageIndicator(Context context) {
        this(context, null);
    }

    /**
     * Creates a new CirclePageIndicator with XML attributes.
     *
     * @param context the context in which the indicator is created
     * @param attrs the attribute set from XML
     */
    public CirclePageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.vpiCirclePageIndicatorStyle);
    }

    private static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(1, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    /**
     * Creates a new CirclePageIndicator with XML attributes and a default style.
     *
     * @param context the context in which the indicator is created
     * @param attrs the attribute set from XML
     * @param defStyle the default style resource
     */
    public CirclePageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPaintPageFill = new Paint(1);
        this.mPaintStroke = new Paint(1);
        this.mPaintFill = new Paint(1);
        this.mLastMotionX = -1.0f;
        this.mActivePointerId = -1;
        if (!isInEditMode()) {
            Resources res = getResources();
            int defaultPageColor = res.getColor(R.color.default_circle_indicator_page_color);
            int defaultFillColor = res.getColor(R.color.default_circle_indicator_fill_color);
            int defaultStrokeColor = res.getColor(R.color.default_circle_indicator_stroke_color);
            float defaultRadius = dpToPx(5.0f, getResources());
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CirclePageIndicator, defStyle, 0);
            this.mCentered = a.getBoolean(2, true);
            this.mOrientation = a.getInt(0, 0);
            this.mPaintPageFill.setStyle(Paint.Style.FILL);
            this.mPaintPageFill.setColor(a.getColor(5, defaultPageColor));
            this.mPaintStroke.setStyle(Paint.Style.STROKE);
            this.mPaintStroke.setColor(a.getColor(8, defaultStrokeColor));
            this.mPaintStroke.setStrokeWidth(a.getDimension(3, 2.0f));
            this.mPaintFill.setStyle(Paint.Style.FILL);
            this.mPaintFill.setColor(a.getColor(4, defaultFillColor));
            this.mRadius = a.getDimension(6, defaultRadius);
            this.mSnap = a.getBoolean(7, false);
            Drawable background = a.getDrawable(1);
            if (background != null) {
                setBackgroundDrawable(background);
            }
            a.recycle();
            ViewConfiguration configuration = ViewConfiguration.get(context);
            this.mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        }
    }

    /**
     * Sets whether the indicator circles should be centered in the view.
     *
     * @param centered true to center the circles, false otherwise
     */
    public void setCentered(boolean centered) {
        this.mCentered = centered;
        invalidate();
    }

    public boolean isCentered() {
        return this.mCentered;
    }

    public void setPageColor(int pageColor) {
        this.mPaintPageFill.setColor(pageColor);
        invalidate();
    }

    public int getPageColor() {
        return this.mPaintPageFill.getColor();
    }

    public void setFillColor(int fillColor) {
        this.mPaintFill.setColor(fillColor);
        invalidate();
    }

    public int getFillColor() {
        return this.mPaintFill.getColor();
    }

    public void setOrientation(int orientation) {
        switch (orientation) {
            case 0:
            case 1:
                this.mOrientation = orientation;
                requestLayout();
                return;
            default:
                throw new IllegalArgumentException("Orientation must be either HORIZONTAL or VERTICAL.");
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setStrokeColor(int strokeColor) {
        this.mPaintStroke.setColor(strokeColor);
        invalidate();
    }

    public int getStrokeColor() {
        return this.mPaintStroke.getColor();
    }

    public void setStrokeWidth(float strokeWidth) {
        this.mPaintStroke.setStrokeWidth(strokeWidth);
        invalidate();
    }

    public float getStrokeWidth() {
        return this.mPaintStroke.getStrokeWidth();
    }

    public void setRadius(float radius) {
        this.mRadius = radius;
        invalidate();
    }

    public float getRadius() {
        return this.mRadius;
    }

    public void setSnap(boolean snap) {
        this.mSnap = snap;
        invalidate();
    }

    public boolean isSnap() {
        return this.mSnap;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int count;
        int longSize;
        int longPaddingBefore;
        int longPaddingAfter;
        int shortPaddingBefore;
        float dX;
        float dY;
        float dX2;
        float dY2;
        super.onDraw(canvas);
        if (this.mViewPager != null && (count = this.mViewPager.getAdapter().getCount()) != 0) {
            if (this.mCurrentPage >= count) {
                setCurrentItem(count - 1);
                return;
            }
            if (this.mOrientation == 0) {
                longSize = getWidth();
                longPaddingBefore = getPaddingLeft();
                longPaddingAfter = getPaddingRight();
                shortPaddingBefore = getPaddingTop();
            } else {
                longSize = getHeight();
                longPaddingBefore = getPaddingTop();
                longPaddingAfter = getPaddingBottom();
                shortPaddingBefore = getPaddingLeft();
            }
            float threeRadius = this.mRadius * 3.0f;
            float shortOffset = shortPaddingBefore + this.mRadius;
            float longOffset = longPaddingBefore + this.mRadius;
            if (this.mCentered) {
                longOffset += (((longSize - longPaddingBefore) - longPaddingAfter) / 2.0f) - ((count * threeRadius) / 2.0f);
            }
            float pageFillRadius = this.mRadius;
            if (this.mPaintStroke.getStrokeWidth() > 0.0f) {
                pageFillRadius -= this.mPaintStroke.getStrokeWidth() / 2.0f;
            }
            for (int iLoop = 0; iLoop < count; iLoop++) {
                float drawLong = longOffset + (iLoop * threeRadius);
                if (this.mOrientation == 0) {
                    dX2 = drawLong;
                    dY2 = shortOffset;
                } else {
                    dX2 = shortOffset;
                    dY2 = drawLong;
                }
                if (this.mPaintPageFill.getAlpha() > 0) {
                    canvas.drawCircle(dX2, dY2, pageFillRadius, this.mPaintPageFill);
                }
                if (pageFillRadius != this.mRadius) {
                    canvas.drawCircle(dX2, dY2, this.mRadius, this.mPaintStroke);
                }
            }
            float cx = (this.mSnap ? this.mSnapPage : this.mCurrentPage) * threeRadius;
            if (!this.mSnap) {
                cx += this.mPageOffset * threeRadius;
            }
            if (this.mOrientation == 0) {
                dX = longOffset + cx;
                dY = shortOffset;
            } else {
                dX = shortOffset;
                dY = longOffset + cx;
            }
            canvas.drawCircle(dX, dY, this.mRadius, this.mPaintFill);
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent ev) {
        if (super.onTouchEvent(ev)) {
            return true;
        }
        if (this.mViewPager == null || this.mViewPager.getAdapter().getCount() == 0) {
            return false;
        }
        int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case 0:
                this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                this.mLastMotionX = ev.getX();
                return true;
            case 1:
            case 3:
                if (!this.mIsDragging) {
                    int count = this.mViewPager.getAdapter().getCount();
                    int width = getWidth();
                    float halfWidth = width / 2.0f;
                    float sixthWidth = width / 6.0f;
                    if (this.mCurrentPage > 0 && ev.getX() < halfWidth - sixthWidth) {
                        if (action != 3) {
                            this.mViewPager.setCurrentItem(this.mCurrentPage - 1);
                        }
                        return true;
                    }
                    if (this.mCurrentPage < count - 1 && ev.getX() > halfWidth + sixthWidth) {
                        if (action != 3) {
                            this.mViewPager.setCurrentItem(this.mCurrentPage + 1);
                        }
                        return true;
                    }
                }
                this.mIsDragging = false;
                this.mActivePointerId = -1;
                if (this.mViewPager.isFakeDragging()) {
                    this.mViewPager.endFakeDrag();
                }
                return true;
            case 2:
                int activePointerIndex = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                float x = MotionEventCompat.getX(ev, activePointerIndex);
                float deltaX = x - this.mLastMotionX;
                if (!this.mIsDragging && Math.abs(deltaX) > this.mTouchSlop) {
                    this.mIsDragging = true;
                }
                if (this.mIsDragging) {
                    this.mLastMotionX = x;
                    if (this.mViewPager.isFakeDragging() || this.mViewPager.beginFakeDrag()) {
                        this.mViewPager.fakeDragBy(deltaX);
                    }
                }
                return true;
            case 4:
            default:
                return true;
            case 5:
                int index = MotionEventCompat.getActionIndex(ev);
                this.mLastMotionX = MotionEventCompat.getX(ev, index);
                this.mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                return true;
            case 6:
                int pointerIndex = MotionEventCompat.getActionIndex(ev);
                int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                if (pointerId == this.mActivePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                this.mLastMotionX = MotionEventCompat.getX(ev, MotionEventCompat.findPointerIndex(ev, this.mActivePointerId));
                return true;
        }
    }

    @Override // com.aero.control.helpers.Android.PageIndicator
    public void setViewPager(ViewPager view) {
        if (this.mViewPager != view) {
            if (this.mViewPager != null) {
                this.mViewPager.setOnPageChangeListener(null);
            }
            if (view.getAdapter() == null) {
                throw new IllegalStateException("ViewPager does not have adapter instance.");
            }
            this.mViewPager = view;
            this.mViewPager.setOnPageChangeListener(this);
            invalidate();
        }
    }

    @Override // com.aero.control.helpers.Android.PageIndicator
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override // com.aero.control.helpers.Android.PageIndicator
    public void setCurrentItem(int item) {
        if (this.mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        this.mViewPager.setCurrentItem(item);
        this.mCurrentPage = item;
        invalidate();
    }

    @Override // com.aero.control.helpers.Android.PageIndicator
    public void notifyDataSetChanged() {
        invalidate();
    }

    @Override // android.support.v4.view.ViewPager.OnPageChangeListener
    public void onPageScrollStateChanged(int state) {
        this.mScrollState = state;
        if (this.mListener != null) {
            this.mListener.onPageScrollStateChanged(state);
        }
    }

    @Override // android.support.v4.view.ViewPager.OnPageChangeListener
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        this.mCurrentPage = position;
        this.mPageOffset = positionOffset;
        invalidate();
        if (this.mListener != null) {
            this.mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override // android.support.v4.view.ViewPager.OnPageChangeListener
    public void onPageSelected(int position) {
        if (this.mSnap || this.mScrollState == 0) {
            this.mCurrentPage = position;
            this.mSnapPage = position;
            invalidate();
        }
        if (this.mListener != null) {
            this.mListener.onPageSelected(position);
        }
    }

    @Override // com.aero.control.helpers.Android.PageIndicator
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        this.mListener = listener;
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOrientation == 0) {
            setMeasuredDimension(measureLong(widthMeasureSpec), measureShort(heightMeasureSpec));
        } else {
            setMeasuredDimension(measureShort(widthMeasureSpec), measureLong(heightMeasureSpec));
        }
    }

    private int measureLong(int measureSpec) {
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        if (specMode == 1073741824 || this.mViewPager == null) {
            return specSize;
        }
        int count = this.mViewPager.getAdapter().getCount();
        int result = (int) (getPaddingLeft() + getPaddingRight() + (count * 2 * this.mRadius) + ((count - 1) * this.mRadius) + 1.0f);
        if (specMode == Integer.MIN_VALUE) {
            return Math.min(result, specSize);
        }
        return result;
    }

    private int measureShort(int measureSpec) {
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        if (specMode == 1073741824) {
            return specSize;
        }
        int result = (int) ((2.0f * this.mRadius) + getPaddingTop() + getPaddingBottom() + 1.0f);
        if (specMode == Integer.MIN_VALUE) {
            return Math.min(result, specSize);
        }
        return result;
    }

    @Override // android.view.View
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mCurrentPage = savedState.currentPage;
        this.mSnapPage = savedState.currentPage;
        requestLayout();
    }

    @Override // android.view.View
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = this.mCurrentPage;
        return savedState;
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.aero.control.helpers.Android.CirclePageIndicator.SavedState.1
            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentPage;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentPage = in.readInt();
        }

        @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.currentPage);
        }
    }
}
