package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.RelativeLayout;
import com.aero.control.R;

public class RippleView extends RelativeLayout {
    private int DURATION;
    private int FRAME_RATE;
    private int HEIGHT;
    private int PAINT_ALPHA;
    private int WIDTH;
    private boolean animationRunning;
    private Handler canvasHandler;
    private View childView;
    private int durationEmpty;
    private GestureDetector gestureDetector;
    private Boolean hasToZoom;
    private Boolean isCentered;
    private Bitmap originBitmap;
    private Paint paint;
    private float radiusMax;
    private int rippleColor;
    private int ripplePadding;
    private Integer rippleType;
    private Runnable runnable;
    private ScaleAnimation scaleAnimation;
    private int timer;
    private int timerEmpty;
    private float x;
    private float y;
    private int zoomDuration;
    private float zoomScale;

    public RippleView(Context context) {
        super(context);
        this.FRAME_RATE = 8;
        this.DURATION = 300;
        this.PAINT_ALPHA = 120;
        this.radiusMax = 0.0f;
        this.animationRunning = false;
        this.timer = 0;
        this.timerEmpty = 0;
        this.durationEmpty = -1;
        this.x = -1.0f;
        this.y = -1.0f;
        this.runnable = new Runnable() { // from class: com.aero.control.helpers.Android.Material.RippleView.1
            @Override // java.lang.Runnable
            public void run() {
                RippleView.this.invalidate();
            }
        };
    }

    public RippleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.FRAME_RATE = 8;
        this.DURATION = 300;
        this.PAINT_ALPHA = 120;
        this.radiusMax = 0.0f;
        this.animationRunning = false;
        this.timer = 0;
        this.timerEmpty = 0;
        this.durationEmpty = -1;
        this.x = -1.0f;
        this.y = -1.0f;
        this.runnable = new Runnable() { // from class: com.aero.control.helpers.Android.Material.RippleView.1
            @Override // java.lang.Runnable
            public void run() {
                RippleView.this.invalidate();
            }
        };
        init(context, attrs);
    }

    public RippleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.FRAME_RATE = 8;
        this.DURATION = 300;
        this.PAINT_ALPHA = 120;
        this.radiusMax = 0.0f;
        this.animationRunning = false;
        this.timer = 0;
        this.timerEmpty = 0;
        this.durationEmpty = -1;
        this.x = -1.0f;
        this.y = -1.0f;
        this.runnable = new Runnable() { // from class: com.aero.control.helpers.Android.Material.RippleView.1
            @Override // java.lang.Runnable
            public void run() {
                RippleView.this.invalidate();
            }
        };
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (!isInEditMode()) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RippleView);
            this.rippleColor = typedArray.getColor(4, getResources().getColor(R.color.dark_grey));
            this.rippleType = Integer.valueOf(typedArray.getInt(6, 0));
            this.hasToZoom = Boolean.valueOf(typedArray.getBoolean(8, false));
            this.isCentered = Boolean.valueOf(typedArray.getBoolean(5, false));
            this.DURATION = typedArray.getInteger(2, this.DURATION);
            this.FRAME_RATE = typedArray.getInteger(1, this.FRAME_RATE);
            this.PAINT_ALPHA = typedArray.getInteger(0, this.PAINT_ALPHA);
            this.ripplePadding = typedArray.getDimensionPixelSize(7, 0);
            this.canvasHandler = new Handler();
            this.zoomScale = typedArray.getFloat(9, 1.03f);
            this.zoomDuration = typedArray.getInt(3, 200);
            typedArray.recycle();
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(this.rippleColor);
            this.paint.setAlpha(this.PAINT_ALPHA);
            setWillNotDraw(false);
            this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() { // from class: com.aero.control.helpers.Android.Material.RippleView.2
                @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return true;
                }

                @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });
            setDrawingCacheEnabled(true);
        }
    }

    @Override // android.view.ViewGroup
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        this.childView = child;
        super.addView(child, index, params);
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.animationRunning) {
            if (this.DURATION <= this.timer * this.FRAME_RATE) {
                this.animationRunning = false;
                this.timer = 0;
                this.durationEmpty = -1;
                this.timerEmpty = 0;
                if (Build.VERSION.SDK_INT != 23) {
                    canvas.restore();
                }
                invalidate();
                return;
            }
            this.canvasHandler.postDelayed(this.runnable, this.FRAME_RATE);
            if (this.timer == 0) {
                canvas.save();
            }
            canvas.drawCircle(this.x, this.y, this.radiusMax * ((this.timer * this.FRAME_RATE) / this.DURATION), this.paint);
            this.paint.setColor(getResources().getColor(android.R.color.holo_red_light));
            if (this.rippleType.intValue() == 1 && this.originBitmap != null && (this.timer * this.FRAME_RATE) / this.DURATION > 0.4f) {
                if (this.durationEmpty == -1) {
                    this.durationEmpty = this.DURATION - (this.timer * this.FRAME_RATE);
                }
                this.timerEmpty++;
                Bitmap tmpBitmap = getCircleBitmap((int) (this.radiusMax * ((this.timerEmpty * this.FRAME_RATE) / this.durationEmpty)));
                canvas.drawBitmap(tmpBitmap, 0.0f, 0.0f, this.paint);
                tmpBitmap.recycle();
            }
            this.paint.setColor(this.rippleColor);
            if (this.rippleType.intValue() != 1) {
                this.paint.setAlpha((int) (this.PAINT_ALPHA - (this.PAINT_ALPHA * ((this.timer * this.FRAME_RATE) / this.DURATION))));
            } else if ((this.timer * this.FRAME_RATE) / this.DURATION > 0.6f) {
                this.paint.setAlpha((int) (this.PAINT_ALPHA - (this.PAINT_ALPHA * ((this.timerEmpty * this.FRAME_RATE) / this.durationEmpty))));
            } else {
                this.paint.setAlpha(this.PAINT_ALPHA);
            }
            this.timer++;
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.WIDTH = w;
        this.HEIGHT = h;
        this.scaleAnimation = new ScaleAnimation(1.0f, this.zoomScale, 1.0f, this.zoomScale, w / 2, h / 2);
        this.scaleAnimation.setDuration(this.zoomDuration);
        this.scaleAnimation.setRepeatMode(2);
        this.scaleAnimation.setRepeatCount(1);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (this.gestureDetector.onTouchEvent(event) && !this.animationRunning) {
            if (this.hasToZoom.booleanValue()) {
                startAnimation(this.scaleAnimation);
            }
            this.radiusMax = Math.max(this.WIDTH, this.HEIGHT);
            if (this.rippleType.intValue() != 2) {
                this.radiusMax /= 2.0f;
            }
            this.radiusMax -= this.ripplePadding;
            if (this.isCentered.booleanValue() || this.rippleType.intValue() == 1) {
                this.x = getMeasuredWidth() / 2;
                this.y = getMeasuredHeight() / 2;
            } else {
                this.x = event.getX();
                this.y = event.getY();
            }
            this.animationRunning = true;
            if (this.rippleType.intValue() == 1 && this.originBitmap == null) {
                this.originBitmap = getDrawingCache(true);
            }
            invalidate();
            this.childView.performClick();
        }
        return true;
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    private Bitmap getCircleBitmap(int radius) {
        Bitmap output = Bitmap.createBitmap(this.originBitmap.getWidth(), this.originBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect((int) (this.x - radius), (int) (this.y - radius), (int) (this.x + radius), (int) (this.y + radius));
        paint.setAntiAlias(true);
        canvas.drawRGB(0, 0, 0);
        canvas.drawCircle(this.x, this.y, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(this.originBitmap, rect, rect, paint);
        return output;
    }
}
