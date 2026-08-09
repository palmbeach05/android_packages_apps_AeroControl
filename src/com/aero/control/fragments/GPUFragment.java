package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.Android.CustomPreference;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.Shell;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public class GPUFragment extends PlaceHolderFragment implements Preference.OnPreferenceChangeListener {
    private static final String NO_DATA_FOUND = "Unavailable";
    private PreferenceCategory PrefCat;
    private CustomPreference mColorControl;
    private AlertDialog mColorDialog;
    private String[] mColorValues;
    private CustomListPreference mDisplayControl;
    private CustomPreference mDoubletap2Wake;
    private CustomPreference mGPUControl;
    private CustomListPreference mGPUControlFrequencies;
    private String mGPUFile;
    private String mGPUFreq;
    private String mGPUGov;
    private CustomListPreference mGPUGovernor;
    private GPUGovernorFragment mGPUGovernorFragment;
    private Shell mShell;
    private CustomPreference mSweep2wake;
    private PreferenceScreen root;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        boolean checkGpuControl;
        boolean checkmSweep2wake;
        boolean checkDoubletap2wake;
        String tmp;
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.layout.gpu_fragment);
        this.root = getPreferenceScreen();
        PreferenceCategory gpuCategory = (PreferenceCategory) findPreference("gpu_settings");
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.mGPUControl = new CustomPreference(getActivity());
        this.mGPUControl.setName("gpu_control_enable");
        this.mGPUControl.setTitle(R.string.pref_gpu_control_enable);
        this.mGPUControl.setSummary(R.string.pref_gpu_control_enable_summary);
        this.mGPUControl.setLookUpDefault(FilePath.GPU_CONTROL_ACTIVE);
        this.mGPUControl.setOrder(5);
        gpuCategory.addPreference(this.mGPUControl);
        this.mSweep2wake = new CustomPreference(getActivity());
        this.mSweep2wake.setName("sweeptowake");
        this.mSweep2wake.setTitle(R.string.pref_sweeptowake);
        this.mSweep2wake.setSummary(R.string.pref_sweeptowake_summary);
        this.mSweep2wake.setLookUpDefault(FilePath.SWEEP2WAKE);
        this.mSweep2wake.setOrder(10);
        gpuCategory.addPreference(this.mSweep2wake);
        this.mDoubletap2Wake = new CustomPreference(getActivity());
        this.mDoubletap2Wake.setName("doubletaptowake");
        this.mDoubletap2Wake.setTitle(R.string.pref_doubletaptowake);
        this.mDoubletap2Wake.setSummary(R.string.pref_doubletaptowake_summary);
        this.mDoubletap2Wake.setLookUpDefault(FilePath.DOUBLETAP2WAKE);
        this.mDoubletap2Wake.setOrder(15);
        gpuCategory.addPreference(this.mDoubletap2Wake);
        this.mGPUControlFrequencies = new CustomListPreference(getActivity());
        this.mGPUControlFrequencies.setName("gpu_max_freq");
        this.mGPUControlFrequencies.setTitle(R.string.pref_max_freq_gpu);
        this.mGPUControlFrequencies.setDialogTitle(R.string.pref_max_freq_gpu);
        this.mGPUControlFrequencies.setSummary(R.string.pref_max_freq_gpu_summary);
        this.mGPUControlFrequencies.setOrder(20);
        gpuCategory.addPreference(this.mGPUControlFrequencies);
        this.mGPUGovernor = new CustomListPreference(getActivity());
        this.mGPUGovernor.setName("set_gpu_governor");
        this.mGPUGovernor.setTitle("GPU Governor");
        this.mGPUGovernor.setDialogTitle("GPU Governor");
        this.mGPUGovernor.setSummary("GPU Governor");
        this.mGPUGovernor.setOrder(25);
        gpuCategory.addPreference(this.mGPUGovernor);
        this.mDisplayControl = new CustomListPreference(getActivity());
        this.mDisplayControl.setName("display_control");
        this.mDisplayControl.setTitle(R.string.pref_display_color);
        this.mDisplayControl.setDialogTitle(R.string.pref_display_color);
        this.mDisplayControl.setSummary(R.string.pref_display_color_summary);
        this.mDisplayControl.setOrder(30);
        gpuCategory.addPreference(this.mDisplayControl);
        this.mColorControl = (CustomPreference) this.root.findPreference("rgbValues");
        this.mColorControl.setOrder(40);
        this.mColorControl.setLookUpDefault(FilePath.COLOR_CONTROL);
        this.mGPUGovernor.setOnPreferenceChangeListener(this);
        this.mGPUGovernor.setOrder(45);
        this.mGPUControlFrequencies.setOnPreferenceChangeListener(this);
        this.mGPUControlFrequencies.setOrder(21);
        String[] arr$ = FilePath.GPU_FILES;
        int len$ = arr$.length;
        int i$ = 0;
        while (true) {
            if (i$ >= len$) {
                break;
            }
            String a = arr$[i$];
            if (!AeroActivity.genHelper.doesExist(a)) {
                i$++;
            } else {
                this.mGPUFile = a;
                break;
            }
        }
        if (!AeroActivity.genHelper.doesExist(FilePath.SWEEP2WAKE)) {
            gpuCategory.removePreference(this.mSweep2wake);
        }
        if (!AeroActivity.genHelper.doesExist(FilePath.DOUBLETAP2WAKE)) {
            gpuCategory.removePreference(this.mDoubletap2Wake);
        }
        if (!AeroActivity.genHelper.doesExist(FilePath.GPU_CONTROL_ACTIVE)) {
            gpuCategory.removePreference(this.mGPUControl);
        }
        if (this.mGPUFile == null) {
            gpuCategory.removePreference(this.mGPUControlFrequencies);
        }
        if (!AeroActivity.genHelper.doesExist(FilePath.COLOR_CONTROL)) {
            gpuCategory.removePreference(this.mColorControl);
        }
        if (AeroActivity.shell.getInfo(FilePath.DISPLAY_COLOR).equals(NO_DATA_FOUND)) {
            gpuCategory.removePreference(this.mDisplayControl);
        }
        CustomPreference gpu_gov_settings = (CustomPreference) this.root.findPreference("gpu_gov_settings");
        if (AeroActivity.genHelper.doesExist(FilePath.GPU_GOV_PATH)) {
            gpu_gov_settings.setOrder(35);
            gpu_gov_settings.setHideOnBoot(true);
            gpu_gov_settings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.GPUFragment.1
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    if (GPUFragment.this.mGPUGovernorFragment == null) {
                        GPUFragment.this.mGPUGovernorFragment = new GPUGovernorFragment();
                    }
                    AeroActivity.mHandler.post(new Runnable() { // from class: com.aero.control.fragments.GPUFragment.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!GPUFragment.this.isAdded() || GPUFragment.this.getFragmentManager() == null) {
                                return;
                            }
                            try {
                                GPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, GPUFragment.this.mGPUGovernorFragment).addToBackStack("GPU Governor").commit();
                            } catch (IllegalStateException e) {
                                Log.e("Aero", "Could not commit fragment transaction, state already saved.", e);
                            }
                        }
                    });
                    return true;
                }
            });
        } else {
            gpuCategory.removePreference(gpu_gov_settings);
        }
        CharSequence[] display_entries = {getText(R.string.defy_red_colors), getText(R.string.defy_green_colors), getText(R.string.defy_energy_saver)};
        CharSequence[] display_values = {"31", "9", "0"};
        this.mDisplayControl.setEntries(display_entries);
        this.mDisplayControl.setEntryValues(display_values);
        String[] arr$2 = FilePath.GPU_FREQ_ARRAY;
        for (String s : arr$2) {
            if (AeroActivity.genHelper.doesExist(s)) {
                this.mGPUFreq = s;
            }
        }
        if (this.mGPUFreq != null) {
            this.mGPUControlFrequencies.setEntries(AeroActivity.shell.getInfoArray(this.mGPUFreq, 1, 0));
            this.mGPUControlFrequencies.setEntryValues(AeroActivity.shell.getInfoArray(this.mGPUFreq, 0, 0));
        } else {
            this.mGPUControlFrequencies.setEntries(R.array.gpu_frequency_list);
            this.mGPUControlFrequencies.setEntryValues(R.array.gpu_frequency_list_values);
        }
        String[] arr$3 = FilePath.GPU_GOV_ARRAY;
        for (String s2 : arr$3) {
            if (AeroActivity.genHelper.doesExist(s2)) {
                this.mGPUGov = s2;
            }
        }
        if (this.mGPUGov != null) {
            if (AeroActivity.genHelper.doesExist(this.mGPUGov + "available_governors")) {
                tmp = this.mGPUGov + "available_governors";
            } else {
                tmp = this.mGPUGov + "governor";
            }
            this.mGPUGovernor.setEntries(AeroActivity.shell.getInfoArray(tmp, 0, 0));
            this.mGPUGovernor.setEntryValues(AeroActivity.shell.getInfoArray(tmp, 0, 0));
            this.mGPUGovernor.setValue(AeroActivity.shell.getInfo(this.mGPUGov + "governor"));
            this.mGPUGovernor.setSummary(AeroActivity.shell.getInfo(this.mGPUGov + "governor"));
            this.mGPUGovernor.setDialogIcon(R.drawable.device_old);
        } else {
            gpuCategory.removePreference(this.mGPUGovernor);
        }
        try {
            if (this.mGPUFile != null) {
                String currentFreq = AeroActivity.shell.getInfoArray(this.mGPUFile, 0, 0)[0];
                this.mGPUControlFrequencies.setValue(currentFreq);
                this.mGPUControlFrequencies.setSummary(formatFrequencySummary(currentFreq));
            }
            if (AeroActivity.shell.getInfo(FilePath.GPU_CONTROL_ACTIVE).equals("1")) {
                checkGpuControl = true;
                this.mGPUControl.setSummary(R.string.enabled);
            } else {
                checkGpuControl = false;
                this.mGPUControl.setSummary(R.string.disabled);
            }
            if (AeroActivity.shell.getInfo(FilePath.SWEEP2WAKE).equals("1")) {
                checkmSweep2wake = true;
                this.mSweep2wake.setSummary(R.string.enabled);
            } else {
                checkmSweep2wake = false;
                this.mSweep2wake.setSummary(R.string.disabled);
            }
            if (AeroActivity.shell.getInfo(FilePath.DOUBLETAP2WAKE).equals("1")) {
                checkDoubletap2wake = true;
                this.mDoubletap2Wake.setSummary(R.string.enabled);
            } else {
                checkDoubletap2wake = false;
                this.mDoubletap2Wake.setSummary(R.string.disabled);
            }
            this.mSweep2wake.setClicked(Boolean.valueOf(checkmSweep2wake));
            this.mDoubletap2Wake.setClicked(Boolean.valueOf(checkDoubletap2wake));
            this.mGPUControl.setClicked(Boolean.valueOf(checkGpuControl));
        } catch (ArrayIndexOutOfBoundsException e) {
            this.mGPUControlFrequencies.setSummary(NO_DATA_FOUND);
            this.mGPUControlFrequencies.setEnabled(false);
            this.mGPUControl.setEnabled(false);
            Toast.makeText(getActivity(), "GPU Control is not supported with your kernel.", 1).show();
        }
        this.mGPUControlFrequencies.setDialogIcon(R.drawable.gpu);
        if (gpuCategory.getPreferenceCount() <= 0) {
            gpuCategory.setTitle(R.string.no_gpu_data);
        }
    }

    private void showColorControl(final SharedPreferences.Editor editor, final CustomPreference cusPref) {
        if (this.mShell == null) {
            this.mShell = new Shell("su", true);
        }
        this.mColorValues = AeroActivity.shell.getInfoArray(FilePath.COLOR_CONTROL, 0, 0);
        if (this.mColorValues == null || this.mColorValues.length == 0 || this.mColorValues[0].equals(NO_DATA_FOUND)) {
            Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
            return;
        }
        if (this.mColorValues.length < 3) {
            Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.flower);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.gpu_color_control, (ViewGroup) null);
        final int colorMin = 20;
        final int colorMax = 255;
        final SeekBar redValues = (SeekBar) layout.findViewById(R.id.redValues);
        final SeekBar greenValues = (SeekBar) layout.findViewById(R.id.greenValues);
        final SeekBar blueValues = (SeekBar) layout.findViewById(R.id.blueValues);
        redValues.getProgressDrawable().setColorFilter(getResources().getColor(R.color.gpu_slider_red), PorterDuff.Mode.SRC_IN);
        greenValues.getProgressDrawable().setColorFilter(getResources().getColor(R.color.gpu_slider_green), PorterDuff.Mode.SRC_IN);
        blueValues.getProgressDrawable().setColorFilter(getResources().getColor(R.color.gpu_slider_blue), PorterDuff.Mode.SRC_IN);
        final EditText redValue = (EditText) layout.findViewById(R.id.redValue);
        final EditText greenValue = (EditText) layout.findViewById(R.id.greenValue);
        final EditText blueValue = (EditText) layout.findViewById(R.id.blueValue);
        try {
            redValues.setProgress(clampColorValue(Integer.parseInt(this.mColorValues[0]), colorMin, colorMax) - colorMin);
            greenValues.setProgress(clampColorValue(Integer.parseInt(this.mColorValues[1]), colorMin, colorMax) - colorMin);
            blueValues.setProgress(clampColorValue(Integer.parseInt(this.mColorValues[2]), colorMin, colorMax) - colorMin);
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
            return;
        }
        redValue.setText(this.mColorValues[0]);
        greenValue.setText(this.mColorValues[1]);
        blueValue.setText(this.mColorValues[2]);
        redValue.setEnabled(true);
        greenValue.setEnabled(true);
        blueValue.setEnabled(true);
        redValue.addTextChangedListener(new TextWatcher() { // from class: com.aero.control.fragments.GPUFragment.2
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int i = Integer.parseInt(s.toString());
                    if (i <= 255 && i >= 0) {
                        redValues.setProgress(clampColorValue(i, colorMin, colorMax) - colorMin);
                        GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                    } else {
                        redValue.setText("255");
                    }
                } catch (NumberFormatException e) {
                }
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        greenValue.addTextChangedListener(new TextWatcher() { // from class: com.aero.control.fragments.GPUFragment.3
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int i = Integer.parseInt(s.toString());
                    if (i <= 255 && i >= 0) {
                        greenValues.setProgress(clampColorValue(i, colorMin, colorMax) - colorMin);
                        GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                    } else {
                        greenValue.setText("255");
                    }
                } catch (NumberFormatException e) {
                }
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        blueValue.addTextChangedListener(new TextWatcher() { // from class: com.aero.control.fragments.GPUFragment.4
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int i = Integer.parseInt(s.toString());
                    if (i <= 255 && i >= 0) {
                        blueValues.setProgress(clampColorValue(i, colorMin, colorMax) - colorMin);
                        GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                    } else {
                        blueValue.setText("255");
                    }
                } catch (NumberFormatException e) {
                }
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        redValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    redValue.setText("" + (progress + colorMin));
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        greenValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    greenValue.setText("" + (progress + colorMin));
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        blueValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    blueValue.setText("" + (progress + colorMin));
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        builder.setTitle(R.string.pref_display_color);
        builder.setView(layout);
        this.mColorDialog = builder.create();
        this.mColorDialog.show();
    }

    private int clampColorValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Safely converts a raw GPU frequency value (expected to be reported in Hz,
     * with the driver-specific unit suffix trimmed) into a human readable MHz
     * summary. Device kernels can report malformed, empty or unexpectedly short
     * values, so the trailing-suffix trim is only attempted when the string is
     * long enough, avoiding a StringIndexOutOfBoundsException/crash.
     */
    private String formatFrequencySummary(String value) {
        if (value == null || value.length() <= 3) {
            return NO_DATA_FOUND;
        }
        return AeroActivity.shell.toMHz(value.substring(0, value.length() - 3));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setColorValues(EditText redValue, EditText greenValue, EditText blueValue, CustomPreference cusPref, SharedPreferences.Editor editor) {
        int red;
        int green;
        int blue;
        try {
            red = Integer.parseInt(redValue.getText().toString());
            green = Integer.parseInt(greenValue.getText().toString());
            blue = Integer.parseInt(blueValue.getText().toString());
        } catch (NumberFormatException e) {
            return;
        }
        if (red > 255 || blue > 255 || green > 255 || red < 0 || blue < 0 || green < 0) {
            Toast.makeText(getActivity(), "The values are out of range!", 1).show();
            return;
        }
        String rgbValues = red + " " + green + " " + blue;
        this.mShell.addCommand("echo " + rgbValues + " > " + FilePath.COLOR_CONTROL);
        if (new File(FilePath.COLOR_CONTROL_BIT).exists()) {
            this.mShell.addCommand("echo 1 > /sys/devices/platform/kcal_ctrl.0/kcal_ctrl");
        }
        this.mShell.runInteractive();
        if (cusPref.isChecked().booleanValue()) {
            editor.putString(cusPref.getName(), rgbValues).commit();
        }
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mColorDialog != null) {
            this.mColorDialog.dismiss();
        }
        if (this.mShell != null) {
            this.mShell.closeInteractive();
            this.mShell = null;
        }
    }

    @Override // android.preference.PreferenceFragment
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        CustomPreference cusPref = null;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (preference == this.mSweep2wake) {
            this.mSweep2wake.setClicked(Boolean.valueOf(this.mSweep2wake.isClicked().booleanValue() ? false : true));
            if (this.mSweep2wake.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.SWEEP2WAKE);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.SWEEP2WAKE);
            }
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mDoubletap2Wake) {
            this.mDoubletap2Wake.setClicked(Boolean.valueOf(this.mDoubletap2Wake.isClicked().booleanValue() ? false : true));
            if (this.mDoubletap2Wake.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.DOUBLETAP2WAKE);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.DOUBLETAP2WAKE);
            }
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mColorControl) {
            cusPref = (CustomPreference) preference;
            showColorControl(editor, cusPref);
        } else if (preference == this.mGPUControl) {
            this.mGPUControl.setClicked(Boolean.valueOf(this.mGPUControl.isClicked().booleanValue() ? false : true));
            if (this.mGPUControl.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.GPU_CONTROL_ACTIVE);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.GPU_CONTROL_ACTIVE);
            }
            cusPref = (CustomPreference) preference;
        }
        if (cusPref != null && cusPref.isChecked().booleanValue() && cusPref.isClicked() != null) {
            String state = cusPref.isClicked().booleanValue() ? "1" : "0";
            editor.putString(cusPref.getName(), state).commit();
        }
        return true;
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String a = (String) newValue;
        String newSummary = "";
        String path = "";
        if (preference == this.mGPUControlFrequencies) {
            if (this.mGPUFile == null || !AeroActivity.genHelper.doesExist(this.mGPUFile)) {
                Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                return false;
            }
            newSummary = formatFrequencySummary(a);
            if (newSummary.equals(NO_DATA_FOUND)) {
                Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                return false;
            }
            path = this.mGPUFile;
        } else if (preference == this.mGPUGovernor) {
            if (this.PrefCat != null) {
                this.root.removePreference(this.PrefCat);
            }
            if (this.mGPUGov == null) {
                String[] arr$ = FilePath.GPU_GOV_ARRAY;
                for (String s : arr$) {
                    if (AeroActivity.genHelper.doesExist(s)) {
                        this.mGPUGov = s;
                    }
                }
            }
            path = this.mGPUGov + "governor";
            newSummary = a;
        } else {
            if (preference == this.mDisplayControl) {
                String[] commands = {"chmod 0664 /sys/class/misc/mDisplayControl/display_brightness_value", "echo " + a + " > " + FilePath.DISPLAY_COLOR};
                AeroActivity.shell.setRootInfo(commands);
                Toast.makeText(getActivity(), "Turn your display off/on :)", 1).show();
            }
            return true;
        }
        AeroActivity.shell.setRootInfo(a, path);
        if (!newSummary.equals("")) {
            preference.setSummary(newSummary);
        }
        return true;
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mGPUGov == null) {
            String[] arr$ = FilePath.GPU_GOV_ARRAY;
            for (String s : arr$) {
                if (AeroActivity.genHelper.doesExist(s)) {
                    this.mGPUGov = s;
                }
            }
        }
        if (AeroActivity.genHelper.doesExist(this.mGPUGov)) {
            inflater.inflate(R.menu.cpu_menu, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_governor_settings /* 2131099747 */:
                String[] completeParamterList = null;
                try {
                    try {
                        completeParamterList = AeroActivity.shell.getDirInfo(this.mGPUGov + AeroActivity.shell.getInfo(this.mGPUGov + "governor"), true);
                        if (completeParamterList == null) {
                            Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                            Log.e("Aero", "We found no parameters for this governor, maybe because it has none?");
                        } else {
                            if (this.PrefCat != null) {
                                this.root.removePreference(this.PrefCat);
                            }
                            this.PrefCat = new PreferenceCategory(getActivity());
                            this.PrefCat.setTitle(R.string.perf_gpu_gov_settings);
                            this.root.addPreference(this.PrefCat);
                            try {
                                PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
                                h.genPrefFromDictionary(completeParamterList, this.mGPUGov + AeroActivity.shell.getInfo(this.mGPUGov + "governor"));
                            } catch (NullPointerException e) {
                                Log.e("Aero", "I couldn't get any files!", e);
                            }
                        }
                    } catch (NullPointerException e2) {
                        Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                        Log.e("Aero", "Couldn't find any parameters for this governor!", e2);
                        if (0 == 0) {
                            Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                            Log.e("Aero", "We found no parameters for this governor, maybe because it has none?");
                        }
                    }
                    return true;
                } catch (Throwable th) {
                    if (completeParamterList == null) {
                        Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                        Log.e("Aero", "We found no parameters for this governor, maybe because it has none?");
                        return true;
                    }
                    throw th;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
