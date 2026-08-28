package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;

/**
 * An enhanced ListPreference with custom layout, checkbox for save-on-boot functionality,
 * and integrated help button. Supports Material Design styling and persistent state
 * management through shared preferences.
 */
public class CustomListPreference extends ListPreference {
    private CompoundButton mCheckBox;
    private Boolean mChecked;
    private Context mContext;
    private View mCustomImageButton;
    private String mHelpContent;
    private Boolean mHideOnBoot;
    private String mName;
    private View.OnClickListener mOnClickListener;
    private SharedPreferences mSharedPreference;
    private Boolean mShowHelp;
    private TextView mSummary;
    private CharSequence mSummaryPref;
    private TextView mTitle;

    /**
     * Creates a new CustomListPreference with custom layout and help support.
     *
     * @param context the context in which the preference is created
     */
    public CustomListPreference(Context context) {
        super(context);
        this.mName = super.getKey();
        this.mOnClickListener = new View.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomListPreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                String key = CustomListPreference.this.getKey();
                if (key == null) {
                    key = CustomListPreference.this.getName();
                }
                if (CustomListPreference.this.mHelpContent == null) {
                    CustomListPreference.this.mHelpContent = HelpTextHolder.instance(CustomListPreference.this.mContext).getText(key);
                }
                AlertDialog dialog = new AlertDialog.Builder(CustomListPreference.this.mContext).setTitle(CustomListPreference.this.getTitle()).setMessage(CustomListPreference.this.mHelpContent).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomListPreference.1.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog2, int which) {
                    }
                }).create();
                dialog.show();
            }
        };
        setContext(context);
        setLayoutResource(R.layout.preference_enhanced);
        this.mSharedPreference = PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    @Override // android.preference.Preference
    public Context getContext() {
        return this.mContext;
    }

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

    public Boolean isHelpEnabled() {
        if (this.mShowHelp == null) {
            this.mShowHelp = true;
        }
        return this.mShowHelp;
    }

    public void setChecked(Boolean checked) {
        this.mChecked = checked;
    }

    public Boolean isChecked() {
        if (this.mSharedPreference.getString(getName(), null) != null) {
            setChecked(true);
        }
        if (this.mChecked == null) {
            setChecked(false);
        }
        return this.mChecked;
    }

    @Override // android.preference.ListPreference, android.preference.Preference
    public CharSequence getSummary() {
        return this.mSummaryPref;
    }

    @Override // android.preference.ListPreference, android.preference.Preference
    public void setSummary(CharSequence value) {
        this.mSummaryPref = value;
    }

    @Override // android.preference.Preference
    public void setKey(String key) {
        setName(key);
    }

    @Override // android.preference.Preference
    public String getKey() {
        return getName();
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return this.mName;
    }

    @Override // android.preference.Preference
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        boolean effectiveEnabled = isEnabled();
        applyEnabledStateToViews(effectiveEnabled);
        if (this.mCheckBox != null) {
            this.mCheckBox.setEnabled(effectiveEnabled);
        }
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

    @Override // android.preference.Preference
    protected void onBindView(final View view) {
        super.onBindView(view);
        this.mTitle = (TextView) view.findViewById(R.id.preference_title);
        this.mSummary = (TextView) view.findViewById(R.id.preference_summary);
        this.mTitle.setText(super.getTitle());
        this.mSummary.setText(this.mSummaryPref);
        this.mTitle.setTypeface(FilePath.kitkatFont);
        this.mSummary.setTypeface(FilePath.kitkatFont);
        this.mCheckBox = (CompoundButton) view.findViewById(R.id.checkbox_pref);
        this.mCheckBox.setOnCheckedChangeListener(null);
        this.mCheckBox.setChecked(isChecked().booleanValue());
        this.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CustomListPreference.this.onCheck(isChecked);
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
                    CustomListPreference.this.performCustomClick();
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

    public void onCheck(boolean checked) {
        SharedPreferences.Editor editor = this.mSharedPreference.edit();
        setChecked(Boolean.valueOf(checked));
        if (checked) {
            editor.putString(getName(), super.getValue());
        } else {
            editor.remove(getName());
        }
        editor.commit();
    }
}
