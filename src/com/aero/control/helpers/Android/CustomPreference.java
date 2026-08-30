package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HelpTextHolder;
import java.util.Map;

/**
 * Enhanced preference widget with boot-on persistence, integrated help dialogs, and
 * custom Material Design styling. Provides a checkbox to enable persistence of system
 * tuning values, an optional help button that displays contextual documentation, and
 * theme-aware text colors.
 */
public class CustomPreference extends Preference {
    private CompoundButton mCheckBox;
    private Boolean mChecked;
    private Boolean mClicked;
    private Context mContext;
    private View mCustomImageButton;
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

    /**
     * Constructs a custom preference with the specified context and attribute set.
     *
     * @param context the context
     * @param attrs the attribute set from XML
     */
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

    /**
     * Constructs a custom preference with the specified context.
     *
     * @param context the context
     */
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
     * Sets whether to hide the boot-on-startup checkbox for this preference.
     *
     * @param checked true to hide the checkbox, false to show it
     */
    public void setHideOnBoot(Boolean checked) {
        this.mHideOnBoot = checked;
    }

    /**
     * Returns whether the boot-on-startup checkbox is hidden.
     *
     * @return true if the checkbox is hidden, false otherwise
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
     * Returns whether the help button is enabled.
     *
     * @return true if the help button is shown, false if hidden
     */
    public Boolean isHelpEnabled() {
        if (this.mShowHelp == null) {
            this.mShowHelp = true;
        }
        return this.mShowHelp;
    }

    /**
     * Sets whether this preference is checked for boot-on-startup persistence.
     *
     * @param checked true if the preference should persist on boot, false otherwise
     */
    public void setChecked(Boolean checked) {
        this.mChecked = checked;
    }

    /**
     * Returns whether this preference is checked for boot-on-startup persistence.
     *
     * @return true if the preference persists on boot, false otherwise
     */
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

    /**
     * Sets the clicked state of the preference and updates the summary text accordingly.
     *
     * @param checked true to set enabled state, false for disabled state
     */
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

    /**
     * Returns whether the preference has been clicked/enabled.
     *
     * @return true if clicked/enabled, false otherwise
     */
    public Boolean isClicked() {
        if (this.mClicked == null) {
            return false;
        }
        return this.mClicked;
    }

    /**
     * Returns the summary text for this preference.
     *
     * @return the summary text
     */
    @Override // android.preference.Preference
    public CharSequence getSummary() {
        return this.mSummaryPref;
    }

    /**
     * Sets the summary text for this preference.
     *
     * @param value the summary text to set
     */
    @Override // android.preference.Preference
    public void setSummary(CharSequence value) {
        super.setSummary(value);
        this.mSummaryPref = value;
    }

    /**
     * Sets the key identifier for this preference.
     *
     * @param key the key to set
     */
    @Override // android.preference.Preference
    public void setKey(String key) {
        super.setKey(key);
        setName(key);
    }

    /**
     * Returns the key identifier for this preference.
     *
     * @return the key
     */
    @Override // android.preference.Preference
    public String getKey() {
        return getName();
    }

    /**
     * Sets the internal name identifier for this preference.
     *
     * @param name the name identifier
     */
    public void setName(String name) {
        this.mName = name;
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
        applyEnabledStateToViews(effectiveEnabled);
        if (this.mCheckBox != null) {
            this.mCheckBox.setEnabled(effectiveEnabled);
        }
    }

    /**
     * Applies the enabled or disabled text color styling to the title and summary views
     * based on the preference's current enabled state.
     *
     * @param enabled true to apply enabled colors, false to apply disabled (dimmed) colors
     */
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

    /**
     * Resolves a theme attribute to an actual color value, falling back to a color
     * resource if the attribute is not defined in the current theme.
     *
     * @param attrResId the theme attribute resource ID to resolve
     * @param fallbackColorResId the fallback color resource ID to use if the attribute is not found
     * @return the resolved color integer
     */
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

    /**
     * Applies the disabled alpha transparency to a color value, respecting the
     * theme's disabledAlpha attribute if available.
     *
     * @param color the original color value
     * @return the color with disabled alpha applied
     */
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
     * Returns the internal name identifier for this preference.
     *
     * @return the name identifier
     */
    public String getName() {
        return this.mName;
    }

    /**
     * Sets the file path to look up default values from when saving the preference.
     *
     * @param filepath the sysfs path to read default values from
     */
    public void setLookUpDefault(String filepath) {
        this.mLookUpDefault = filepath;
    }

    /**
     * Retrieves the default value for this preference from the configured lookup file path.
     * Handles special formatting for RGB values and voltage values.
     *
     * @param name the preference name used to determine how to parse the file
     * @return the formatted default value string, or the raw file content for standard preferences
     */
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
                CustomPreference.this.onCheck(isChecked);
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
                    CustomPreference.this.performClick(null);
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
     * Handles click events on the preference content area, dispatching to the
     * registered listener or launching the associated intent if configured.
     */
    public void performCustomClick() {
        if (!isEnabled()) {
            return;
        }

        OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null && listener.onPreferenceClick(this)) {
            return;
        }

        onClick();
            if (getIntent() != null) {
                getContext().startActivity(getIntent());
        }
    }

    /**
     * Called when the checkbox state changes. Saves or removes the preference value
     * based on the checked state.
     *
     * @param checked true if the checkbox is now checked, false otherwise
     */
    public void onCheck(boolean checked) {
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
