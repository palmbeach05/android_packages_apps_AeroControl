package com.aero.control.testsuite;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.aero.control.R;

/**
 * Custom SwitchPreference that binds to a custom switch widget in the preference layout
 * instead of using the default SwitchPreference behavior. Ensures the switch state
 * stays synchronized with the preference value.
 */
public class TestSuiteSwitchPreference extends SwitchPreference {
    private final CompoundButton.OnCheckedChangeListener mSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (callChangeListener(Boolean.valueOf(isChecked))) {
                setChecked(isChecked);
                return;
            }
            buttonView.setOnCheckedChangeListener(null);
            buttonView.setChecked(!isChecked);
            buttonView.setOnCheckedChangeListener(TestSuiteSwitchPreference.this.mSwitchListener);
        }
    };

    /**
     * Creates a switch preference with the specified style.
     *
     * @param context the context
     * @param attrs the attribute set from XML
     * @param defStyle the default style resource
     */
    public TestSuiteSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Creates a switch preference.
     *
     * @param context the context
     * @param attrs the attribute set from XML
     */
    public TestSuiteSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates a switch preference.
     *
     * @param context the context
     */
    public TestSuiteSwitchPreference(Context context) {
        super(context);
    }

    @Override // android.preference.SwitchPreference, android.preference.Preference
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch switchWidget = (Switch) view.findViewById(R.id.switch_widget);
        if (switchWidget == null) {
            return;
        }
        switchWidget.setOnCheckedChangeListener(null);
        switchWidget.setChecked(isChecked());
        switchWidget.setOnCheckedChangeListener(this.mSwitchListener);
    }
}
