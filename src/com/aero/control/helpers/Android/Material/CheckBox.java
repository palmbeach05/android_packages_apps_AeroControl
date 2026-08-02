package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import com.aero.control.R;
import com.aero.control.helpers.ThemeHelper;

/* JADX INFO: loaded from: classes.dex */
public class CheckBox extends CustomView {
    private int backgroundColor;
    private boolean check;
    private Check checkView;
    private Paint mPaint;
    private OnCheckListener onCheckListener;
    private boolean press;
    private int step;

    public interface OnCheckListener {
        void onCheck(boolean z);
    }

    static /* synthetic */ int access$208(CheckBox x0) {
        int i = x0.step;
        x0.step = i + 1;
        return i;
    }

    static /* synthetic */ int access$210(CheckBox x0) {
        int i = x0.step;
        x0.step = i - 1;
        return i;
    }

    public CheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.backgroundColor = Color.parseColor("#4CAF50");
        this.step = 0;
        this.press = false;
        this.check = false;
        setAttributes(attrs);
    }

    @Override // com.aero.control.helpers.Android.Material.CustomView
    protected void onInitDefaultValues() {
        this.minWidth = 48;
        this.minHeight = 48;
        this.backgroundColor = Color.parseColor("#4CAF50");
        this.backgroundResId = R.drawable.background_checkbox;
    }

    @Override // com.aero.control.helpers.Android.Material.CustomView
    protected void setAttributes(AttributeSet attrs) {
        setBackgroundResource(R.drawable.background_checkbox);
        setMinimumHeight(dpToPx(48.0f, getResources()));
        setMinimumWidth(dpToPx(48.0f, getResources()));
        int[] attrsArray = new int[] { android.R.attr.background };
        android.content.res.TypedArray ta = getContext().obtainStyledAttributes(attrs, attrsArray);
        try {
            if (ta.hasValue(0)) {
                int backgroundColor = ta.getColor(0, -1);
                if (backgroundColor != -1) {
                    setBackgroundColor(backgroundColor);
                }
            }
        } finally {
            ta.recycle();
        }
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(Color.parseColor("#446D6D6D"));
        this.checkView = new Check(getContext());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dpToPx(20.0f, getResources()), dpToPx(20.0f, getResources()));
        params.addRule(13, -1);
        this.checkView.setLayoutParams(params);
        addView(this.checkView);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            this.isLastTouch = true;
            int action = event.getAction();
            switch (action) {
                case 0:
                    changeBackgroundColor(this.check ? makePressColor() : Color.parseColor("#446D6D6D"));
                    break;
                case 1:
                    changeBackgroundColor(getResources().getColor(android.R.color.transparent));
                    this.press = false;
                    if (event.getX() <= getWidth() && event.getX() >= 0.0f && event.getY() <= getHeight() && event.getY() >= 0.0f) {
                        this.isLastTouch = false;
                        this.check = !this.check;
                        if (this.onCheckListener != null) {
                            this.onCheckListener.onCheck(this.check);
                        }
                        if (this.check) {
                            this.step = 0;
                        }
                        if (this.check) {
                            this.checkView.changeBackground();
                        }
                    }
                    break;
                case 3:
                    changeBackgroundColor(getResources().getColor(android.R.color.transparent));
                    break;
            }
        }
        return true;
    }

    @Override // com.aero.control.helpers.Android.Material.CustomView, android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.press) {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, this.mPaint);
        }
    }

    private void changeBackgroundColor(int color) {
        LayerDrawable layer = (LayerDrawable) getBackground();
        GradientDrawable shape = (GradientDrawable) layer.findDrawableByLayerId(R.id.shape_background);
        shape.setColor(color);
    }

    protected int makePressColor() {
        int r = (this.backgroundColor >> 16) & MotionEventCompat.ACTION_MASK;
        int g = (this.backgroundColor >> 8) & MotionEventCompat.ACTION_MASK;
        int b = this.backgroundColor & MotionEventCompat.ACTION_MASK;
        return Color.argb(70, r + (-30) < 0 ? 0 : r - 30, g + (-30) < 0 ? 0 : g - 30, b + (-30) < 0 ? 0 : b - 30);
    }

    @Override // android.view.View
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        if (isEnabled()) {
            this.beforeBackground = this.backgroundColor;
        }
        changeBackgroundColor(color);
    }

    public void setChecked(boolean check) {
        invalidate();
        this.check = check;
        setPressed(false);
        changeBackgroundColor(getResources().getColor(android.R.color.transparent));
        if (check) {
            this.step = 0;
        }
        if (check) {
            this.checkView.changeBackground();
        }
    }

    public boolean isCheck() {
        return this.check;
    }

    private class Check extends View {
        private boolean forceReDraw;
        private Rect mDst;
        private BitmapFactory.Options mOpt;
        private Rect mSrc;
        private boolean needDrawBackground;
        private boolean needReDraw;
        private Bitmap sprite;

        public Check(Context context) {
            super(context);
            this.needDrawBackground = true;
            this.needReDraw = false;
            this.forceReDraw = false;
            this.mOpt = new BitmapFactory.Options();
            this.mOpt.inPreferredConfig = Bitmap.Config.RGB_565;
            setBackgroundResource(ThemeHelper.THEME_DARK.equals(ThemeHelper.getTheme(context)) ? R.drawable.background_checkbox_uncheck_dark : R.drawable.background_checkbox_uncheck);
            this.mOpt.inScaled = false;
            this.sprite = BitmapFactory.decodeResource(context.getResources(), R.drawable.sprite_check, this.mOpt);
        }

        public void changeBackground() {
            if (CheckBox.this.check) {
                setBackgroundResource(R.drawable.background_checkbox_check);
                LayerDrawable layer = (LayerDrawable) getBackground();
                GradientDrawable shape = (GradientDrawable) layer.findDrawableByLayerId(R.id.shape_background);
                shape.setColor(CheckBox.this.backgroundColor);
                return;
            }
            setBackgroundResource(ThemeHelper.THEME_DARK.equals(ThemeHelper.getTheme(getContext())) ? R.drawable.background_checkbox_uncheck_dark : R.drawable.background_checkbox_uncheck);
        }

        private void drawRect() {
            if (this.mSrc != null && this.mDst != null) {
                this.mSrc.set(CheckBox.this.step * 40, 0, (CheckBox.this.step * 40) + 40, 40);
                this.mDst.set(0, 0, getWidth() - 2, getHeight());
            } else {
                this.mSrc = new Rect(CheckBox.this.step * 40, 0, (CheckBox.this.step * 40) + 40, 40);
                this.mDst = new Rect(0, 0, getWidth() - 2, getHeight());
            }
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (CheckBox.this.check) {
                if (CheckBox.this.step < 11) {
                    CheckBox.access$208(CheckBox.this);
                    this.needDrawBackground = true;
                    this.needReDraw = true;
                }
                if (!this.needReDraw) {
                    this.forceReDraw = true;
                }
            } else {
                if (CheckBox.this.step >= 0) {
                    CheckBox.access$210(CheckBox.this);
                    this.needDrawBackground = true;
                    this.needReDraw = true;
                }
                if (CheckBox.this.step == -1 && this.needDrawBackground) {
                    changeBackground();
                    this.needDrawBackground = false;
                }
            }
            drawRect();
            canvas.drawBitmap(this.sprite, this.mSrc, this.mDst, (Paint) null);
            if (this.needReDraw || (this.forceReDraw && !this.needReDraw)) {
                invalidate();
                this.needReDraw = false;
                this.forceReDraw = false;
            }
        }
    }

    public void setOncheckListener(OnCheckListener onCheckListener) {
        this.onCheckListener = onCheckListener;
    }
}
