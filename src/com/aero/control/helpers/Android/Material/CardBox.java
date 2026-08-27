package com.aero.control.helpers.Android.Material;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.aero.control.R;

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
        Drawable backgroundDrawable;

        // For API < 21, manually resolve theme attributes in card.xml
        if (Build.VERSION.SDK_INT < 21 && drawable == R.drawable.card) {
            backgroundDrawable = createCardDrawableWithThemeAttr(getContext());
        } else {
            // Use themed drawable resolution for API 21+
            if (Build.VERSION.SDK_INT >= 21) {
                backgroundDrawable = getContext().getResources().getDrawable(drawable, getContext().getTheme());
                backgroundDrawable = applyTouchFeedback(backgroundDrawable);
                // Use platform elevation instead of a simulated shadow drawable
                setElevation(getResources().getDimension(R.dimen.card_elevation));
                setTranslationZ(0f);
            } else {
                backgroundDrawable = getResources().getDrawable(drawable);
            }
        }

        if (Build.VERSION.SDK_INT < 16) {
            setBackgroundDrawable(backgroundDrawable);
        } else {
            setBackground(backgroundDrawable);
        }
    }

    /**
     * Wraps the card background in a RippleDrawable so clickable App Monitor
     * tabs get Material press feedback on API 21+. Uses the resolved
     * android:colorControlHighlight theme color, falling back to
     * android.R.color.darker_gray if the attribute cannot be resolved.
     */
    private Drawable applyTouchFeedback(Drawable base) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return base;
        }

        TypedValue typedValue = new TypedValue();
        int highlightColor = getResources().getColor(android.R.color.darker_gray);
        if (getContext().getTheme().resolveAttribute(android.R.attr.colorControlHighlight, typedValue, true)) {
            highlightColor = typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
                    ? typedValue.data
                    : getResources().getColor(typedValue.resourceId);
        }
        return new RippleDrawable(ColorStateList.valueOf(highlightColor), base, base);
    }

    /**
     * Creates the card drawable programmatically for API < 21,
     * resolving the ?attr/aeroCardBackground theme attribute manually.
     */
    private Drawable createCardDrawableWithThemeAttr(Context context) {
        // Resolve the aeroCardBackground theme attribute
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.aeroCardBackground, typedValue, true);
        int cardBackgroundColor = typedValue.data;

        // Get the card_grey color
        int cardGreyColor = getResources().getColor(R.color.card_grey);

        // Create the bottom layer (grey background)
        GradientDrawable bottomShape = new GradientDrawable();
        bottomShape.setShape(GradientDrawable.RECTANGLE);
        bottomShape.setColor(cardGreyColor);
        bottomShape.setCornerRadius(2 * getResources().getDisplayMetrics().density); // 2dp

        // Create the top layer (theme background)
        GradientDrawable topShape = new GradientDrawable();
        topShape.setShape(GradientDrawable.RECTANGLE);
        topShape.setColor(cardBackgroundColor);
        topShape.setCornerRadius(2 * getResources().getDisplayMetrics().density); // 2dp

        // Create layer list matching card.xml structure
        Drawable[] layers = new Drawable[] { bottomShape, topShape };
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // Set insets for the top layer (1px left, 1px right, 2dp bottom)
        int onePx = 1;
        int twoDp = (int) (2 * getResources().getDisplayMetrics().density);
        layerDrawable.setLayerInset(1, onePx, 0, onePx, twoDp);

        return layerDrawable;
    }
}
