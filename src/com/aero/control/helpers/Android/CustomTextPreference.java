package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;

/**
 * An enhanced EditTextPreference with custom layout, checkbox for save-on-boot functionality,
 * and integrated help button. Supports Material Design styling and persistent state
 * management through shared preferences.
 */
public class CustomTextPreference extends EditTextPreference {
    private Boolean mChecked;
    private Context mContext;
    private View mCustomImageButton;
    private String mHelpContent;
    private Boolean mHideOnBoot;
    private String mName;
    private View.OnClickListener mOnClickListener;
    private CompoundButton mCheckBox;
    private SharedPreferences mSharedPreference;
    private Boolean mShowHelp;
    private TextView mSummary;
    private CharSequence mSummaryPref;
    private String mText;
    private TextView mTitle;

    /**
     * Creates a new CustomTextPreference with custom layout and help support.
     *
     * @param context the context in which the preference is created
     */
    public CustomTextPreference(Context context) {
        super(context);
        this.mOnClickListener = new View.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomTextPreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                if (CustomTextPreference.this.mHelpContent == null) {
                    CustomTextPreference.this.mHelpContent = HelpTextHolder.instance(CustomTextPreference.this.mContext).getText(CustomTextPreference.this.getTitle().toString());
                }
                AlertDialog dialog = new AlertDialog.Builder(CustomTextPreference.this.mContext).setTitle(CustomTextPreference.this.getTitle().toString()).setMessage(CustomTextPreference.this.mHelpContent).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomTextPreference.1.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog2, int which) {
                    }
                }).create();
                dialog.show();
            }
        };
        setContext(context);
        setLayoutResource(R.layout.preference_enhanced);
    }

    /**
     * Returns the context associated with this preference.
     *
     * @return the context
     */
    @Override // android.preference.Preference
    public Context getContext() {
        return this.mContext;
    }

    /**
     * Sets the context for this preference.
     *
     * @param context the context to set
     */
    public void setContext(Context context) {
        this.mContext = context;
    }

    /**
     * Sets whether this preference should be hidden from boot-time save operations.
     *
     * @param checked true to hide this preference during boot, false otherwise
     */
    public void setHideOnBoot(Boolean checked) {
        this.mHideOnBoot = checked;
    }

    /**
     * Returns whether this preference is hidden from boot-time save operations.
     *
     * @return true if hidden during boot, false otherwise
     */
    public Boolean isHidden() {
        if (this.mHideOnBoot == null) {
            this.mHideOnBoot = false;
        }
        return this.mHideOnBoot;
    }

    /**
     * Enables or disables the help button for this preference.
     *
     * @param enable true to show the help button, false to hide it
     */
    public void setHelpEnable(boolean enable) {
        this.mShowHelp = Boolean.valueOf(enable);
    }

    /**
     * Returns whether the help button is enabled for this preference.
     *
     * @return true if the help button is shown, false otherwise
     */
    public Boolean isHelpEnabled() {
        if (this.mShowHelp == null) {
            this.mShowHelp = true;
        }
        return this.mShowHelp;
    }

    /**
     * Sets the checked state of the save-on-boot checkbox.
     *
     * @param checked true to check the checkbox, false to uncheck it
     */
    public void setChecked(Boolean checked) {
        this.mChecked = checked;
    }

    /**
     * Returns whether the save-on-boot checkbox is checked.
     *
     * @return true if checked, false otherwise
     */
    public Boolean isChecked() {
        if (this.mSharedPreference.getString(getName(), null) != null) {
            setChecked(true);
        }
        if (this.mChecked == null) {
            this.mChecked = false;
        }
        return this.mChecked;
    }

    /**
     * Sets the name identifier for this preference.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Returns the name identifier for this preference.
     *
     * @return the name
     */
    public String getName() {
        return this.mName;
    }

    /**
     * Sets the title text for this preference.
     *
     * @param title the title text to set
     */
    public void setPrefText(String title) {
        this.mText = title;
        if (this.mTitle != null) {
            this.mTitle.setText(title);
        }
    }

    /**
     * Sets the summary text for this preference.
     *
     * @param summary the summary text to set
     */
    public void setPrefSummary(CharSequence summary) {
        this.mSummaryPref = summary;
        if (this.mSummary != null) {
            this.mSummary.setText(summary);
        }
    }

    /**
     * Returns the summary text for this preference.
     *
     * @return the summary text
     */
    public CharSequence getPrefSummary() {
        return this.mSummaryPref;
    }

    /**
     * Updates the standard preference enabled state and synchronizes the custom row's
     * text colors and checkbox enabled state.
     *
     * @param enabled true to enable this preference, false to disable it
     */
    @Override // android.preference.Preference
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        boolean effectiveEnabled = isEnabled();
        if (this.mCheckBox != null) {
            this.mCheckBox.setEnabled(effectiveEnabled);
        }
        applyEnabledStateToViews(effectiveEnabled);
    }

    private void applyEnabledStateToViews(boolean enabled) {
        int primaryColor = resolveThemeColor(R.attr.aeroPrimaryTextColor, R.color.text_color);
        int secondaryColor = resolveThemeColor(R.attr.aeroSecondaryTextColor, R.color.text_color);
        if (enabled) {
            if (this.mTitle != null) {
                this.mTitle.setTextColor(primaryColor);
            }
            if (this.mSummary != null) {
                this.mSummary.setTextColor(secondaryColor);
            }
        } else {
            if (this.mTitle != null) {
                this.mTitle.setTextColor(applyDisabledAlpha(primaryColor));
            }
            if (this.mSummary != null) {
                this.mSummary.setTextColor(applyDisabledAlpha(secondaryColor));
            }
        }
    }

    private int resolveThemeColor(int attrResId, int fallbackColorResId) {
        TypedValue typedValue = new TypedValue();
        if (this.mContext.getTheme().resolveAttribute(attrResId, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            }
            return this.mContext.getResources().getColor(typedValue.resourceId);
        }
        return this.mContext.getResources().getColor(fallbackColorResId);
    }

    private int applyDisabledAlpha(int color) {
        TypedValue typedValue = new TypedValue();
        float disabledAlpha = 0.5f;
        if (this.mContext.getTheme().resolveAttribute(android.R.attr.disabledAlpha, typedValue, true)) {
            disabledAlpha = typedValue.getFloat();
        }
        int alpha = (int) (Color.alpha(color) * disabledAlpha);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Binds a row that provides the custom {@code preference_enhanced} layout IDs,
     * including {@link R.id#preference_content}, which invokes
     * {@link #performCustomClick()}. The checkbox and help button retain separate
     * listeners.
     *
     * @param view the custom preference row
     */
    @Override // android.preference.Preference
    protected void onBindView(final View view) {
        super.onBindView(view);
        this.mSharedPreference = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mTitle = (TextView) view.findViewById(R.id.preference_title);
        this.mSummary = (TextView) view.findViewById(R.id.preference_summary);
        this.mTitle.setText(this.mText);
        this.mSummary.setText(this.mSummaryPref);
        this.mTitle.setTypeface(FilePath.kitkatFont);
        this.mSummary.setTypeface(FilePath.kitkatFont);
        this.mCheckBox = (CompoundButton) view.findViewById(R.id.checkbox_pref);
        this.mCheckBox.setOnCheckedChangeListener(null);
        this.mCheckBox.setChecked(isChecked().booleanValue());
        this.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CustomTextPreference.this.onCheck(isChecked);
            }
        });
        View checkBoxView = this.mCheckBox;
        this.mCustomImageButton = view.findViewById(R.id.info_button);
        View separator_checkbox = view.findViewById(R.id.separator_checkbox);
        View seperator_info = view.findViewById(R.id.separator_info);
        View preferenceContent = view.findViewById(R.id.preference_content);
        if (preferenceContent != null) {
            preferenceContent.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    CustomTextPreference.this.performCustomClick();
                }
            });
        }
        if (isHelpEnabled().booleanValue()) {
            this.mCustomImageButton.setOnClickListener(this.mOnClickListener);
        } else {
            this.mCustomImageButton.setVisibility(8);
            seperator_info.setVisibility(8);
        }
        if (isHidden().booleanValue()) {
            checkBoxView.setVisibility(8);
            separator_checkbox.setVisibility(8);
        }
        applyEnabledStateToViews(isEnabled());
    }

    /**
     * Handles click events on the preference content area to open the text input dialog.
     */
    public void performCustomClick() {
        if (!isEnabled()) {
            return;
        }

        onClick();

        OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null) {
            listener.onPreferenceClick(this);
        }
    }

    /**
     * Called when the save-on-boot checkbox state changes. Persists or removes
     * the current preference value.
     *
     * @param checked true if the checkbox is now checked, false otherwise
     */
    public void onCheck(boolean checked) {
        SharedPreferences.Editor editor = this.mSharedPreference.edit();
        setChecked(Boolean.valueOf(checked));
        if (checked) {
            editor.putString(getName(), getPrefSummary().toString());
        } else {
            editor.remove(getName());
        }
        editor.commit();
    }
}
