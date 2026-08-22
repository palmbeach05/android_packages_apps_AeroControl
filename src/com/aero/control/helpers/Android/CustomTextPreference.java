package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.Android.Material.CheckBox;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;

/* JADX INFO: loaded from: classes.dex */
public class CustomTextPreference extends EditTextPreference implements CheckBox.OnCheckListener {
    private Boolean mChecked;
    private Context mContext;
    private View mCustomImageButton;
    private String mHelpContent;
    private Boolean mHideOnBoot;
    private String mName;
    private View.OnClickListener mOnClickListener;
    private CompoundButton mPlatformCheckBox;
    private SharedPreferences mSharedPreference;
    private Boolean mShowHelp;
    private TextView mSummary;
    private CharSequence mSummaryPref;
    private String mText;
    private TextView mTitle;

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

    @Override // android.preference.Preference
    public Context getContext() {
        return this.mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setHideOnBoot(Boolean checked) {
        this.mHideOnBoot = checked;
    }

    public Boolean isHidden() {
        if (this.mHideOnBoot == null) {
            this.mHideOnBoot = false;
        }
        return this.mHideOnBoot;
    }

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
            this.mChecked = false;
        }
        return this.mChecked;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return this.mName;
    }

    public void setPrefText(String title) {
        this.mText = title;
        if (this.mTitle != null) {
            this.mTitle.setText(title);
        }
    }

    public void setPrefSummary(CharSequence summary) {
        this.mSummaryPref = summary;
        if (this.mSummary != null) {
            this.mSummary.setText(summary);
        }
    }

    public CharSequence getPrefSummary() {
        return this.mSummaryPref;
    }

    @Override // android.preference.Preference
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        boolean effectiveEnabled = isEnabled();
        if (this.mPlatformCheckBox != null) {
            this.mPlatformCheckBox.setEnabled(effectiveEnabled);
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
        View checkBoxView;
        if (Build.VERSION.SDK_INT >= 21) {
            this.mPlatformCheckBox = (CompoundButton) view.findViewById(R.id.checkbox_pref);
            this.mPlatformCheckBox.setOnCheckedChangeListener(null);
            this.mPlatformCheckBox.setChecked(isChecked().booleanValue());
            this.mPlatformCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    CustomTextPreference.this.onCheck(isChecked);
                }
            });
            checkBoxView = this.mPlatformCheckBox;
        } else {
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox_pref);
            checkbox.setOncheckListener(this);
            checkbox.setChecked(isChecked().booleanValue());
            checkBoxView = checkbox;
        }
        this.mCustomImageButton = view.findViewById(R.id.info_button);
        View separator_checkbox = view.findViewById(R.id.separator_checkbox);
        View seperator_info = view.findViewById(R.id.separator_info);
        View preferenceContent = view.findViewById(R.id.preference_content);
        if (preferenceContent != null) {
            preferenceContent.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    CustomTextPreference.this.performClick(null);
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

    @Override // com.aero.control.helpers.Android.Material.CheckBox.OnCheckListener
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
