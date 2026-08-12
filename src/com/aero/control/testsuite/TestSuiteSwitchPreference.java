package com.aero.control.testsuite;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.aero.control.R;

/* JADX INFO: loaded from: classes.dex */
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

    public TestSuiteSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TestSuiteSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestSuiteSwitchPreference(Context context) {
        super(context);
    }

    @Override // android.preference.SwitchPreference, android.preference.Preference
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch switchWidget = (Switch) view.findViewById(R.id.testsuite_switch);
        if (switchWidget == null) {
            return;
        }
        switchWidget.setOnCheckedChangeListener(null);
        switchWidget.setChecked(isChecked());
        switchWidget.setOnCheckedChangeListener(this.mSwitchListener);
    }
}