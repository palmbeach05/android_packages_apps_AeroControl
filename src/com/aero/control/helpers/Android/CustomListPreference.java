package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.Android.Material.CheckBox;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;

/* JADX INFO: loaded from: classes.dex */
public class CustomListPreference extends ListPreference implements CheckBox.OnCheckListener {
    private CheckBox mCheckBox;
    private CompoundButton mPlatformCheckBox;
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
        if (enabled) {
            if (this.mTitle != null) {
                this.mTitle.setTextColor(this.mContext.getResources().getColor(R.color.text_color));
            }
            if (this.mSummary != null) {
                this.mSummary.setTextColor(this.mContext.getResources().getColor(R.color.text_color));
            }
        } else {
            if (this.mTitle != null) {
                this.mTitle.setTextColor(this.mContext.getResources().getColor(android.R.color.darker_gray));
            }
            if (this.mSummary != null) {
                this.mSummary.setTextColor(this.mContext.getResources().getColor(android.R.color.darker_gray));
            }
        }
        if (this.mCheckBox != null) {
            this.mCheckBox.setEnabled(enabled);
        }
        if (this.mPlatformCheckBox != null) {
            this.mPlatformCheckBox.setEnabled(enabled);
        }
    }

    @Override // android.preference.Preference
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mTitle = (TextView) view.findViewById(R.id.preference_title);
        this.mSummary = (TextView) view.findViewById(R.id.preference_summary);
        this.mTitle.setText(super.getTitle());
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
                    CustomListPreference.this.onCheck(isChecked);
                }
            });
            checkBoxView = this.mPlatformCheckBox;
        } else {
            this.mCheckBox = (CheckBox) view.findViewById(R.id.checkbox_pref);
            this.mCheckBox.setOncheckListener(this);
            this.mCheckBox.setChecked(isChecked().booleanValue());
            checkBoxView = this.mCheckBox;
        }
        View contentContainer = view.findViewById(R.id.preference_content);
        if (contentContainer != null) {
            contentContainer.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomListPreference.2
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    view.performClick();
                }
            });
        }
        this.mCustomImageButton = view.findViewById(R.id.info_button);
        View separator_checkbox = view.findViewById(R.id.separator_checkbox);
        View seperator_info = view.findViewById(R.id.separator_info);
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
    }

    @Override // com.aero.control.helpers.Android.Material.CheckBox.OnCheckListener
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
