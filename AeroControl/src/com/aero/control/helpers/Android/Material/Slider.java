package com.aero.control.helpers.Android.Material;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.aero.control.R;
import com.nineoldandroids.view.ViewHelper;

/* JADX INFO: loaded from: classes.dex */
public class Slider extends CustomView {
    private Ball ball;
    private Bitmap mBitmap;
    private Paint mEmptyPaint;
    private Paint mPaint;
    private PorterDuffXfermode mPorterDuffXfermode;
    private Canvas mTemp;
    private Paint mTransPaint;
    private int max;
    private int min;
    public NumberIndicator numberIndicator;
    private OnValueChangedListener onValueChangedListener;
    private boolean placedBall;
    private boolean press;
    private boolean showNumberIndicator;
    private int value;

    public interface OnValueChangedListener {
        void onValueChanged(int i);
    }

    public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.showNumberIndicator = false;
        this.press = false;
        this.value = 0;
        this.max = 100;
        this.min = 0;
        this.placedBall = false;
        if (this.mPaint == null) {
            this.mPaint = new Paint();
        }
        if (this.mTransPaint == null) {
            this.mTransPaint = new Paint();
        }
        if (this.mEmptyPaint == null) {
            this.mEmptyPaint = new Paint();
        }
        if (this.mPorterDuffXfermode == null) {
            this.mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        }
        setAttributes(attrs);
    }

    @Override // com.aero.control.helpers.Android.Material.CustomView
    protected void onInitDefaultValues() {
        this.minWidth = 80;
        this.minHeight = 48;
        this.backgroundColor = Color.parseColor("#4CAF50");
        this.backgroundResId = R.drawable.background_transparent;
    }

    @Override // com.aero.control.helpers.Android.Material.CustomView
    protected void setAttributes(AttributeSet attrs) {
        super.setAttributes(attrs);
        if (!isInEditMode()) {
            getBackground().setAlpha(0);
        }
        this.showNumberIndicator = attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "showNumberIndicator", false);
        this.min = attrs.getAttributeIntValue("http://schemas.android.com/apk/res-auto", "min", 0);
        this.max = attrs.getAttributeIntValue("http://schemas.android.com/apk/res-auto", "max", 100);
        this.value = attrs.getAttributeIntValue("http://schemas.android.com/apk/res-auto", "value", this.min);
        float size = 20.0f;
        String thumbSize = attrs.getAttributeValue("http://schemas.android.com/apk/res-auto", "thumbSize");
        if (thumbSize != null) {
            size = dipOrDpToFloat(thumbSize);
        }
        this.ball = new Ball(getContext());
        setBallParams(size);
        addView(this.ball);
        if (this.showNumberIndicator && !isInEditMode()) {
            this.numberIndicator = new NumberIndicator(getContext());
        }
    }

    private void setBallParams(float size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dpToPx(size, getResources()), dpToPx(size, getResources()));
        params.addRule(15, -1);
        this.ball.setLayoutParams(params);
    }

    @Override // com.aero.control.helpers.Android.Material.CustomView, android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!this.placedBall) {
            placeBall();
        }
        if (this.value == this.min) {
            if (this.mBitmap == null) {
                this.mBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            }
            if (this.mTemp == null) {
                this.mTemp = new Canvas(this.mBitmap);
            }
            this.mPaint.setColor(Color.parseColor("#B0B0B0"));
            this.mPaint.setStrokeWidth(dpToPx(2.0f, getResources()));
            this.mTemp.drawLine(getHeight() / 2, getHeight() / 2, getWidth() - (getHeight() / 2), getHeight() / 2, this.mPaint);
            this.mTransPaint.setColor(getResources().getColor(android.R.color.transparent));
            this.mTransPaint.setXfermode(this.mPorterDuffXfermode);
            this.mTemp.drawCircle(ViewHelper.getX(this.ball) + (this.ball.getWidth() / 2), ViewHelper.getY(this.ball) + (this.ball.getHeight() / 2), this.ball.getWidth() / 2, this.mTransPaint);
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mEmptyPaint);
        } else {
            this.mPaint.setColor(Color.parseColor("#B0B0B0"));
            this.mPaint.setStrokeWidth(dpToPx(2.0f, getResources()));
            canvas.drawLine(getHeight() / 2, getHeight() / 2, getWidth() - (getHeight() / 2), getHeight() / 2, this.mPaint);
            this.mPaint.setColor(this.backgroundColor);
            float division = (this.ball.xFin - this.ball.xIni) / (this.max - this.min);
            int value = this.value - this.min;
            canvas.drawLine(getHeight() / 2, getHeight() / 2, (getHeight() / 2) + (value * division), getHeight() / 2, this.mPaint);
            ViewHelper.setX(this.ball, ((value * division) + (getHeight() / 2)) - (this.ball.getWidth() / 2));
            this.ball.changeBackground();
        }
        if (this.press && !this.showNumberIndicator) {
            this.mPaint.setColor(this.backgroundColor);
            this.mPaint.setAntiAlias(true);
            canvas.drawCircle(ViewHelper.getX(this.ball) + (this.ball.getWidth() / 2), getHeight() / 2, getHeight() / 3, this.mPaint);
        }
        invalidate();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        int newValue;
        this.isLastTouch = true;
        if (isEnabled()) {
            if (event.getAction() == 0 || event.getAction() == 2) {
                if (this.numberIndicator != null && !this.numberIndicator.isShowing()) {
                    this.numberIndicator.show();
                }
                if (event.getX() <= getWidth() && event.getX() >= 0.0f) {
                    this.press = true;
                    float division = (this.ball.xFin - this.ball.xIni) / (this.max - this.min);
                    if (event.getX() > this.ball.xFin) {
                        newValue = this.max;
                    } else if (event.getX() < this.ball.xIni) {
                        newValue = this.min;
                    } else {
                        newValue = this.min + ((int) ((event.getX() - this.ball.xIni) / division));
                    }
                    if (this.value != newValue) {
                        this.value = newValue;
                        if (this.onValueChangedListener != null) {
                            this.onValueChangedListener.onValueChanged(newValue);
                        }
                    }
                    float x = event.getX();
                    if (x < this.ball.xIni) {
                        x = this.ball.xIni;
                    }
                    if (x > this.ball.xFin) {
                        x = this.ball.xFin;
                    }
                    ViewHelper.setX(this.ball, x);
                    this.ball.changeBackground();
                    if (this.numberIndicator != null) {
                        this.numberIndicator.indicator.x = x;
                        this.numberIndicator.indicator.finalY = getRelativeTop(this) - getHeight();
                        this.numberIndicator.indicator.finalSize = getHeight() / 2;
                        this.numberIndicator.numberIndicator.setText("");
                    }
                } else {
                    this.press = false;
                    this.isLastTouch = false;
                    if (this.numberIndicator != null) {
                        this.numberIndicator.dismiss();
                    }
                }
            } else if (event.getAction() == 1) {
                if (this.numberIndicator != null) {
                    this.numberIndicator.dismiss();
                }
                this.isLastTouch = false;
                this.press = false;
            }
        }
        return true;
    }

    private void placeBall() {
        ViewHelper.setX(this.ball, (getHeight() / 2) - (this.ball.getWidth() / 2));
        this.ball.xIni = ViewHelper.getX(this.ball);
        this.ball.xFin = (getWidth() - (getHeight() / 2)) - (this.ball.getWidth() / 2);
        this.ball.xCen = (getWidth() / 2) - (this.ball.getWidth() / 2);
        this.placedBall = true;
    }

    public OnValueChangedListener getOnValueChangedListener() {
        return this.onValueChangedListener;
    }

    public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    public void setThumbSize(float size) {
        setBallParams(size);
    }

    public int getValue() {
        return this.value;
    }

    public void setProgress(int value) {
        setProgress(value, false);
    }

    public void setProgress(int value, boolean inRunnable) {
        if (value <= this.min) {
            value = this.min;
        }
        if (value >= this.max) {
            value = this.max;
        }
        setValueInRunnable(value, inRunnable);
    }

    private void setValueInRunnable(final int value, final boolean inRunnable) {
        if (!this.placedBall && inRunnable) {
            post(new Runnable() { // from class: com.aero.control.helpers.Android.Material.Slider.1
                @Override // java.lang.Runnable
                public void run() {
                    Slider.this.setProgress(value, inRunnable);
                }
            });
            return;
        }
        this.value = value;
        float division = (this.ball.xFin - this.ball.xIni) / this.max;
        ViewHelper.setX(this.ball, ((value * division) + (getHeight() / 2)) - (this.ball.getWidth() / 2));
        this.ball.changeBackground();
    }

    public int getMax() {
        return this.max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMin() {
        return this.min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public boolean isShowNumberIndicator() {
        return this.showNumberIndicator;
    }

    public void showNumberIndicator(boolean showNumberIndicator) {
        this.showNumberIndicator = showNumberIndicator;
        if (!isInEditMode()) {
            this.numberIndicator = showNumberIndicator ? new NumberIndicator(getContext()) : null;
        }
    }

    @Override // android.view.View
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        if (isEnabled()) {
            this.beforeBackground = this.backgroundColor;
        }
    }

    private class Ball extends View {
        private float xCen;
        private float xFin;
        private float xIni;

        public Ball(Context context) {
            super(context);
            if (!isInEditMode()) {
                setBackgroundResource(R.drawable.background_switch_ball_uncheck);
            } else {
                setBackgroundResource(android.R.drawable.radiobutton_off_background);
            }
        }

        public void changeBackground() {
            if (!isInEditMode()) {
                if (Slider.this.value != Slider.this.min) {
                    setBackgroundResource(R.drawable.background_checkbox);
                    LayerDrawable layer = (LayerDrawable) getBackground();
                    GradientDrawable shape = (GradientDrawable) layer.findDrawableByLayerId(R.id.shape_background);
                    shape.setColor(Slider.this.backgroundColor);
                    return;
                }
                setBackgroundResource(R.drawable.background_switch_ball_uncheck);
            }
        }
    }

    public class NumberIndicator extends Dialog {
        private Indicator indicator;
        private TextView numberIndicator;

        public NumberIndicator(Context context) {
            super(context, R.style.Translucent);
        }

        @Override // android.app.Dialog
        protected void onCreate(Bundle savedInstanceState) {
            requestWindowFeature(1);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.number_indicator_spinner);
            setCanceledOnTouchOutside(false);
            RelativeLayout content = (RelativeLayout) findViewById(R.id.number_indicator_spinner_content);
            this.indicator = Slider.this.new Indicator(getContext());
            content.addView(this.indicator);
            this.numberIndicator = new TextView(getContext());
            this.numberIndicator.setTextColor(-1);
            this.numberIndicator.setGravity(17);
            content.addView(this.numberIndicator);
            this.indicator.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
        }

        @Override // android.app.Dialog, android.content.DialogInterface
        public void dismiss() {
            super.dismiss();
            this.indicator.y = 0.0f;
            this.indicator.size = 0.0f;
            this.indicator.animate = true;
        }

        @Override // android.app.Dialog
        public void onBackPressed() {
        }
    }

    private class Indicator extends RelativeLayout {
        private boolean animate;
        private float finalSize;
        private float finalY;
        private Paint mPaint;
        private boolean numberIndicatorResize;
        private float size;
        private float x;
        private float y;

        public Indicator(Context context) {
            super(context);
            this.x = 0.0f;
            this.y = 0.0f;
            this.size = 0.0f;
            this.finalY = 0.0f;
            this.finalSize = 0.0f;
            this.animate = true;
            this.numberIndicatorResize = false;
            if (this.mPaint == null) {
                this.mPaint = new Paint();
            }
            setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!this.numberIndicatorResize) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) Slider.this.numberIndicator.numberIndicator.getLayoutParams();
                params.height = ((int) this.finalSize) * 2;
                params.width = ((int) this.finalSize) * 2;
                Slider.this.numberIndicator.numberIndicator.setLayoutParams(params);
            }
            this.mPaint.setAntiAlias(true);
            this.mPaint.setColor(Slider.this.backgroundColor);
            if (this.animate) {
                if (this.y == 0.0f) {
                    this.y = this.finalY + (this.finalSize * 2.0f);
                }
                this.y -= CustomView.dpToPx(-13.0f, getResources());
                this.size += CustomView.dpToPx(2.0f, getResources());
            }
            canvas.drawCircle(CustomView.getRelativeLeft((View) Slider.this.ball.getParent()) + ViewHelper.getX(Slider.this.ball) + (Slider.this.ball.getWidth() / 2), this.y, this.size, this.mPaint);
            if (this.animate && this.size >= this.finalSize) {
                this.animate = false;
            }
            if (!this.animate) {
                ViewHelper.setX(Slider.this.numberIndicator.numberIndicator, ((CustomView.getRelativeLeft((View) Slider.this.ball.getParent()) + ViewHelper.getX(Slider.this.ball)) + (Slider.this.ball.getWidth() / 2)) - this.size);
                ViewHelper.setY(Slider.this.numberIndicator.numberIndicator, this.y - this.size);
                Slider.this.numberIndicator.numberIndicator.setText(Slider.this.value + "");
            }
            invalidate();
        }
    }
}
