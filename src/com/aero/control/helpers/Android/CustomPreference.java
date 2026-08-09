package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class CustomPreference extends Preference implements CompoundButton.OnCheckedChangeListener {
    private CheckBox mCheckBox;
    private Boolean mChecked;
    private Boolean mClicked;
    private Context mContext;
    private ImageButton mCustomImageButton;
    private String mHelpContent;
    private Boolean mHideOnBoot;
    private String mLookUpDefault;
    private String mName;
    private View.OnClickListener mOnClickListener;
    private SharedPreferences mSharedPreference;
    private Boolean mShowHelp;
    private TextView mSummary;
    private CharSequence mSummaryPref;
    private TextView mTitle;

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mName = super.getKey();
        this.mOnClickListener = new View.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomPreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                String key = CustomPreference.this.getKey();
                if (key == null) {
                    key = CustomPreference.this.getName();
                }
                if (CustomPreference.this.mHelpContent == null) {
                    CustomPreference.this.mHelpContent = HelpTextHolder.instance(CustomPreference.this.mContext).getText(key);
                }
                AlertDialog dialog = new AlertDialog.Builder(CustomPreference.this.mContext).setTitle(CustomPreference.this.getTitle()).setMessage(CustomPreference.this.mHelpContent).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomPreference.1.1
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
        setSummary(super.getSummary());
    }

    public CustomPreference(Context context) {
        super(context);
        this.mName = super.getKey();
        this.mOnClickListener = new View.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomPreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                String key = CustomPreference.this.getKey();
                if (key == null) {
                    key = CustomPreference.this.getName();
                }
                if (CustomPreference.this.mHelpContent == null) {
                    CustomPreference.this.mHelpContent = HelpTextHolder.instance(CustomPreference.this.mContext).getText(key);
                }
                AlertDialog dialog = new AlertDialog.Builder(CustomPreference.this.mContext).setTitle(CustomPreference.this.getTitle()).setMessage(CustomPreference.this.mHelpContent).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Android.CustomPreference.1.1
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
        try {
            if (this.mSharedPreference.getString(getName(), null) != null) {
                setChecked(true);
            }
        } catch (ClassCastException e) {
            setChecked(true);
        }
        if (this.mChecked == null) {
            setChecked(false);
        }
        return this.mChecked;
    }

    public void setClicked(Boolean checked) {
        this.mClicked = checked;
        if (this.mSummary != null) {
            if (this.mClicked.booleanValue()) {
                this.mSummary.setText(R.string.enabled);
                setSummary(R.string.enabled);
            } else {
                this.mSummary.setText(R.string.disabled);
                setSummary(R.string.disabled);
            }
        }
    }

    public Boolean isClicked() {
        if (this.mClicked == null) {
            return false;
        }
        return this.mClicked;
    }

    @Override // android.preference.Preference
    public CharSequence getSummary() {
        return this.mSummaryPref;
    }

    @Override // android.preference.Preference
    public void setSummary(CharSequence value) {
        super.setSummary(value);
        this.mSummaryPref = value;
    }

    @Override // android.preference.Preference
    public void setKey(String key) {
        super.setKey(key);
        setName(key);
    }

    @Override // android.preference.Preference
    public String getKey() {
        return getName();
    }

    public void setName(String name) {
        this.mName = name;
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
    }

    public String getName() {
        return this.mName;
    }

    public void setLookUpDefault(String filepath) {
        this.mLookUpDefault = filepath;
    }

    private String getLookUpDefault(String name) {
        String[] content;
        int switcher;
        String tmp1 = "";
        int i = 0;
        if (name.equals("rgbValues")) {
            content = AeroActivity.shell.getInfoArray(this.mLookUpDefault, 0, 0);
            switcher = 1;
        } else if (name.equals("voltage_values")) {
            content = AeroActivity.shell.getInfo(this.mLookUpDefault, false);
            switcher = 2;
        } else {
            return AeroActivity.shell.getInfo(this.mLookUpDefault);
        }
        String[] arr$ = content;
        for (String a : arr$) {
            switch (switcher) {
                case 1:
                    if (i == 0) {
                        tmp1 = tmp1 + a;
                    } else {
                        tmp1 = tmp1 + " " + a;
                    }
                    break;
                case 2:
                    String tmp2 = a.split(":")[1].replace(" ", "");
                    if (i == 0) {
                        tmp1 = tmp1 + tmp2.replace("mV", "");
                    } else {
                        tmp1 = tmp1 + " " + tmp2.replace("mV", "");
                    }
                    break;
                default:
                    return null;
            }
            i++;
        }
        return tmp1;
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
        this.mCheckBox = (CheckBox) view.findViewById(R.id.checkbox_pref);
        this.mCheckBox.setOnCheckedChangeListener(null);
        this.mCheckBox.setChecked(isChecked().booleanValue());
        this.mCheckBox.setOnCheckedChangeListener(this);
        this.mCustomImageButton = (ImageButton) view.findViewById(R.id.info_button);
        View separator_checkbox = view.findViewById(R.id.separator_checkbox);
        View seperator_info = view.findViewById(R.id.separator_info);
        if (isHelpEnabled().booleanValue()) {
            this.mCustomImageButton.setOnClickListener(this.mOnClickListener);
        } else {
            this.mCustomImageButton.setVisibility(8);
            seperator_info.setVisibility(8);
        }
        if (isHidden().booleanValue()) {
            this.mCheckBox.setVisibility(8);
            separator_checkbox.setVisibility(8);
        }
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
        SharedPreferences.Editor editor = this.mSharedPreference.edit();
        String tmp = null;
        Map<String, ?> allKeys = this.mSharedPreference.getAll();
        for (Map.Entry<String, ?> entry : allKeys.entrySet()) {
            String value = entry.getValue().toString();
            String key = entry.getKey().toString();
            if (key.equals(getName())) {
                tmp = value;
            }
        }
        if (tmp == null && this.mLookUpDefault != null) {
            tmp = getLookUpDefault(getName());
        }
        if (checked) {
            if (tmp != null) {
                editor.putString(getName(), tmp);
            }
        } else {
            editor.remove(getName());
        }
        editor.commit();
        setChecked(Boolean.valueOf(checked));
    }
}
