package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.Android.Material.CheckBox;
import com.aero.control.helpers.Android.Material.CustomImageButton;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;

/* JADX INFO: loaded from: classes.dex */
public class CustomTextPreference extends EditTextPreference implements CheckBox.OnCheckListener {
    private Boolean mChecked;
    private Context mContext;
    private CustomImageButton mCustomImageButton;
    private String mHelpContent;
    private Boolean mHideOnBoot;
    private String mName;
    private View.OnClickListener mOnClickListener;
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
        if (enabled) {
            if (this.mTitle != null) {
                this.mTitle.setTextColor(this.mContext.getResources().getColor(R.color.text_color));
            }
            if (this.mSummary != null) {
                this.mSummary.setTextColor(this.mContext.getResources().getColor(R.color.text_color));
                return;
            }
            return;
        }
        if (this.mTitle != null) {
            this.mTitle.setTextColor(this.mContext.getResources().getColor(android.R.color.darker_gray));
        }
        if (this.mSummary != null) {
            this.mSummary.setTextColor(this.mContext.getResources().getColor(android.R.color.darker_gray));
        }
    }

    @Override // android.preference.Preference
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mSharedPreference = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mTitle = (TextView) view.findViewById(R.id.preference_title);
        this.mSummary = (TextView) view.findViewById(R.id.preference_summary);
        this.mTitle.setText(this.mText);
        this.mSummary.setText(this.mSummaryPref);
        this.mTitle.setTypeface(FilePath.kitkatFont);
        this.mSummary.setTypeface(FilePath.kitkatFont);
        CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox_pref);
        checkbox.setOncheckListener(this);
        this.mCustomImageButton = (CustomImageButton) view.findViewById(R.id.info_button);
        View separator_checkbox = view.findViewById(R.id.separator_checkbox);
        View seperator_info = view.findViewById(R.id.separator_info);
        if (isHelpEnabled().booleanValue()) {
            this.mCustomImageButton.setOnClickListener(this.mOnClickListener);
        } else {
            this.mCustomImageButton.setVisibility(8);
            seperator_info.setVisibility(8);
        }
        if (isChecked() != null) {
            checkbox.setChecked(isChecked().booleanValue());
        }
        if (isHidden().booleanValue()) {
            checkbox.setVisibility(8);
            separator_checkbox.setVisibility(8);
        }
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
