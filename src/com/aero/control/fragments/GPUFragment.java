package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
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
import com.aero.control.helpers.Android.Material.Slider;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.FrequencyFormat;
import com.aero.control.helpers.GpuController;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.ReadMode;
import com.aero.control.helpers.SysfsDirectory;
import com.aero.control.helpers.SysfsResult;

/**
 * Fragment for configuring GPU settings including maximum frequency and governor.
 * Provides save/apply functionality for boot and profile persistence.
 */
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
    private String mGPUGov;
    private CustomListPreference mGPUGovernor;
    private GPUGovernorFragment mGPUGovernorFragment;
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
        this.mGPUControl.setOrder(5);
        gpuCategory.addPreference(this.mGPUControl);
        this.mSweep2wake = new CustomPreference(getActivity());
        this.mSweep2wake.setName("sweeptowake");
        this.mSweep2wake.setTitle(R.string.pref_sweeptowake);
        this.mSweep2wake.setSummary(R.string.pref_sweeptowake_summary);
        this.mSweep2wake.setOrder(10);
        gpuCategory.addPreference(this.mSweep2wake);
        this.mDoubletap2Wake = new CustomPreference(getActivity());
        this.mDoubletap2Wake.setName("doubletaptowake");
        this.mDoubletap2Wake.setTitle(R.string.pref_doubletaptowake);
        this.mDoubletap2Wake.setSummary(R.string.pref_doubletaptowake_summary);
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
        Preference.OnPreferenceClickListener preferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return handlePreferenceClick(preference);
            }
        };
        this.mGPUControl.setOnPreferenceClickListener(preferenceClickListener);
        this.mSweep2wake.setOnPreferenceClickListener(preferenceClickListener);
        this.mDoubletap2Wake.setOnPreferenceClickListener(preferenceClickListener);
        this.mColorControl.setOnPreferenceClickListener(preferenceClickListener);
        this.mGPUGovernor.setOnPreferenceChangeListener(this);
        this.mGPUGovernor.setOrder(45);
        this.mGPUControlFrequencies.setOnPreferenceChangeListener(this);
        this.mGPUControlFrequencies.setOrder(21);
        SysfsResult<Boolean> sweepState = AeroActivity.hardware.gpu().readToggle(
                com.aero.control.helpers.GpuController.Toggle.SWEEP_TO_WAKE);
        SysfsResult<Boolean> doubleTapState = AeroActivity.hardware.gpu().readToggle(
                com.aero.control.helpers.GpuController.Toggle.DOUBLE_TAP_TO_WAKE);
        SysfsResult<Boolean> controlState = AeroActivity.hardware.gpu().readToggle(
                com.aero.control.helpers.GpuController.Toggle.CONTROL);
        if (!sweepState.isSuccess()) {
            gpuCategory.removePreference(this.mSweep2wake);
        }
        if (!doubleTapState.isSuccess()) {
            gpuCategory.removePreference(this.mDoubletap2Wake);
        }
        SysfsResult<Boolean> controlAvailable =
                AeroActivity.hardware.gpu().isControlAvailable();
        if (!controlAvailable.isSuccess() || !controlAvailable.getValue()) {
            gpuCategory.removePreference(this.mGPUControl);
        }
        SysfsResult<String> currentGpuFrequency = AeroActivity.hardware.gpu().readMaxFrequency();
        if (!currentGpuFrequency.isSuccess()) {
            gpuCategory.removePreference(this.mGPUControlFrequencies);
        }
        SysfsResult<Boolean> colorControlAvailable =
                AeroActivity.hardware.displayColor().isAvailable();
        if (!colorControlAvailable.isSuccess() || !colorControlAvailable.getValue()) {
            gpuCategory.removePreference(this.mColorControl);
        }
        SysfsResult<String> displayMode = AeroActivity.hardware.gpu().readDisplayMode();
        if (!displayMode.isSuccess()) {
            gpuCategory.removePreference(this.mDisplayControl);
        }
        this.mGPUControl.setLookUpDefaultValue(
                controlState.isSuccess() && controlState.getValue() ? "1" : "0");
        this.mSweep2wake.setLookUpDefaultValue(
                sweepState.isSuccess() && sweepState.getValue() ? "1" : "0");
        this.mDoubletap2Wake.setLookUpDefaultValue(
                doubleTapState.isSuccess() && doubleTapState.getValue() ? "1" : "0");
        SysfsResult<String[]> initialColor = AeroActivity.hardware.displayColor().readColorValues();
        this.mColorControl.setLookUpDefaultValue(initialColor.isSuccess()
                ? joinValues(initialColor.getValue()) : NO_DATA_FOUND);
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
        SysfsResult<String[]> availableGpuFrequencies =
                AeroActivity.hardware.gpu().readAvailableFrequencies(ReadMode.FREQUENCY_MHZ);
        SysfsResult<String[]> rawGpuFrequencies =
                AeroActivity.hardware.gpu().readAvailableFrequencies(ReadMode.RAW);
        if (availableGpuFrequencies.isSuccess() && rawGpuFrequencies.isSuccess()) {
            this.mGPUControlFrequencies.setEntries(availableGpuFrequencies.getValue());
            this.mGPUControlFrequencies.setEntryValues(rawGpuFrequencies.getValue());
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
            SysfsResult<String[]> governors = AeroActivity.hardware.gpu().readAvailableGovernors();
            SysfsResult<String> currentGovernor = AeroActivity.hardware.gpu().readGovernor();
            if (governors.isSuccess() && currentGovernor.isSuccess()) {
                this.mGPUGovernor.setEntries(governors.getValue());
                this.mGPUGovernor.setEntryValues(governors.getValue());
                this.mGPUGovernor.setValue(currentGovernor.getValue());
                this.mGPUGovernor.setSummary(currentGovernor.getValue());
                this.mGPUGovernor.setDialogIcon(R.drawable.device_old);
            } else {
                gpuCategory.removePreference(this.mGPUGovernor);
            }
        } else {
            gpuCategory.removePreference(this.mGPUGovernor);
        }
        try {
            if (currentGpuFrequency.isSuccess()) {
                this.mGPUControlFrequencies.setValue(currentGpuFrequency.getValue());
                this.mGPUControlFrequencies.setSummary(
                        formatFrequencySummary(currentGpuFrequency.getValue()));
            }
            if (controlState.isSuccess() && controlState.getValue()) {
                checkGpuControl = true;
                this.mGPUControl.setSummary(R.string.enabled);
            } else {
                checkGpuControl = false;
                this.mGPUControl.setSummary(R.string.disabled);
            }
            if (sweepState.isSuccess() && sweepState.getValue()) {
                checkmSweep2wake = true;
                this.mSweep2wake.setSummary(R.string.enabled);
            } else {
                checkmSweep2wake = false;
                this.mSweep2wake.setSummary(R.string.disabled);
            }
            if (doubleTapState.isSuccess() && doubleTapState.getValue()) {
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

    /**
     * Displays the RGB color control dialog with sliders for red, green, and blue values.
     * Creates an interactive dialog allowing users to adjust display color values with
     * real-time preview using SeekBar (API 21+) or custom Slider components.
     *
     * @param editor the SharedPreferences editor for persisting color values
     * @param cusPref the CustomPreference associated with this color control
     */
    private void showColorControl(final SharedPreferences.Editor editor, final CustomPreference cusPref) {
        SysfsResult<String[]> colorValues = AeroActivity.hardware.displayColor().readColorValues();
        this.mColorValues = colorValues.isSuccess() ? colorValues.getValue() : null;
        if (this.mColorValues == null || this.mColorValues.length == 0
                || this.mColorValues[0].equals(NO_DATA_FOUND)) {
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
        final EditText redValue = (EditText) layout.findViewById(R.id.redValue);
        final EditText greenValue = (EditText) layout.findViewById(R.id.greenValue);
        final EditText blueValue = (EditText) layout.findViewById(R.id.blueValue);
        if (Build.VERSION.SDK_INT >= 21) {
            final SeekBar redValues = (SeekBar) layout.findViewById(R.id.redValues);
            final SeekBar greenValues = (SeekBar) layout.findViewById(R.id.greenValues);
            final SeekBar blueValues = (SeekBar) layout.findViewById(R.id.blueValues);
            try {
                redValues.setProgress(Integer.parseInt(this.mColorValues[0]));
                greenValues.setProgress(Integer.parseInt(this.mColorValues[1]));
                blueValues.setProgress(Integer.parseInt(this.mColorValues[2]));
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
            redValue.addTextChangedListener(new TextWatcher() {
                @Override // android.text.TextWatcher
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            redValues.setProgress(i);
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
            greenValue.addTextChangedListener(new TextWatcher() {
                @Override // android.text.TextWatcher
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            greenValues.setProgress(i);
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
            blueValue.addTextChangedListener(new TextWatcher() {
                @Override // android.text.TextWatcher
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            blueValues.setProgress(i);
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
                    if (!fromUser) {
                        return;
                    }
                    redValue.setText("" + progress);
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            });
            greenValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) {
                        return;
                    }
                    greenValue.setText("" + progress);
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            });
            blueValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) {
                        return;
                    }
                    blueValue.setText("" + progress);
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            });
        } else {
            final Slider redValues = (Slider) layout.findViewById(R.id.redValues);
            final Slider greenValues = (Slider) layout.findViewById(R.id.greenValues);
            final Slider blueValues = (Slider) layout.findViewById(R.id.blueValues);
            try {
                redValues.setProgress(Integer.parseInt(this.mColorValues[0]));
                greenValues.setProgress(Integer.parseInt(this.mColorValues[1]));
                blueValues.setProgress(Integer.parseInt(this.mColorValues[2]));
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
                            redValues.setProgress(i);
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
                            greenValues.setProgress(i);
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
                            blueValues.setProgress(i);
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
            redValues.setOnValueChangedListener(new Slider.OnValueChangedListener() { // from class: com.aero.control.fragments.GPUFragment.5
                @Override // com.aero.control.helpers.Android.Material.Slider.OnValueChangedListener
                public void onValueChanged(int value) {
                    redValue.setText("" + value);
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            });
            greenValues.setOnValueChangedListener(new Slider.OnValueChangedListener() { // from class: com.aero.control.fragments.GPUFragment.6
                @Override // com.aero.control.helpers.Android.Material.Slider.OnValueChangedListener
                public void onValueChanged(int value) {
                    greenValue.setText("" + value);
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            });
            blueValues.setOnValueChangedListener(new Slider.OnValueChangedListener() { // from class: com.aero.control.fragments.GPUFragment.7
                @Override // com.aero.control.helpers.Android.Material.Slider.OnValueChangedListener
                public void onValueChanged(int value) {
                    blueValue.setText("" + value);
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor);
                }
            });
        }
        builder.setTitle(R.string.pref_display_color);
        builder.setView(layout);
        this.mColorDialog = builder.create();
        this.mColorDialog.show();
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
        return FrequencyFormat.toMHz(value.substring(0, value.length() - 3));
    }

    /**
     * Applies RGB color values from the color control dialog to the display hardware.
     * Validates that values are within the 0-255 range and writes them to the kernel
     * color control interface via root shell commands.
     *
     * @param redValue the EditText containing the red component value
     * @param greenValue the EditText containing the green component value
     * @param blueValue the EditText containing the blue component value
     * @param cusPref the CustomPreference for this color control setting
     * @param editor the SharedPreferences editor for saving the color values if enabled
     */
    public void setColorValues(EditText redValue, EditText greenValue, EditText blueValue,
            final CustomPreference cusPref, final SharedPreferences.Editor editor) {
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
        final String rgbValues = red + " " + green + " " + blue;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SysfsResult<String> result =
                        AeroActivity.hardware.displayColor().writeColorValue(rgbValues);
                if (!isAdded()) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isSuccess()) {
                            Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (cusPref.isChecked().booleanValue()) {
                            editor.putString(cusPref.getName(), rgbValues).commit();
                        }
                    }
                });
            }
        }).start();
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mColorDialog != null) {
            this.mColorDialog.dismiss();
        }
    }

    @Override // android.preference.PreferenceFragment
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return handlePreferenceClick(preference);
    }

    /**
     * Handles click events for GPU preferences including GPU control, sweep2wake,
     * doubletap2wake, and RGB color control. Toggles boolean preferences and opens
     * the color control dialog when appropriate.
     *
     * @param preference the preference that was clicked
     * @return true if the click was handled
     */
    private boolean handlePreferenceClick(Preference preference) {
        CustomPreference cusPref = null;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (preference == this.mSweep2wake) {
            if (!applyToggle(this.mSweep2wake, GpuController.Toggle.SWEEP_TO_WAKE)) return false;
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mDoubletap2Wake) {
            if (!applyToggle(this.mDoubletap2Wake,
                    GpuController.Toggle.DOUBLE_TAP_TO_WAKE)) return false;
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mColorControl) {
            cusPref = (CustomPreference) preference;
            showColorControl(editor, cusPref);
        } else if (preference == this.mGPUControl) {
            if (!applyToggle(this.mGPUControl, GpuController.Toggle.CONTROL)) return false;
            cusPref = (CustomPreference) preference;
        }
        if (cusPref != null && cusPref.isChecked().booleanValue() && cusPref.isClicked() != null) {
            String state = cusPref.isClicked().booleanValue() ? "1" : "0";
            editor.putString(cusPref.getName(), state).commit();
        }
        return true;
    }

    private boolean applyToggle(final CustomPreference preference, final GpuController.Toggle toggle) {
        final boolean previous = preference.isClicked().booleanValue();
        final boolean requested = !previous;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SysfsResult<Boolean> result =
                        AeroActivity.hardware.gpu().writeToggle(toggle, requested);
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isSuccess()) {
                            preference.setClicked(Boolean.valueOf(previous));
                            Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        preference.setClicked(Boolean.valueOf(requested));
                    }
                });
            }
        }).start();
        return true;
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String a = (String) newValue;
        final boolean displayMode;
        final String newSummary;
        if (preference == this.mGPUControlFrequencies) {
            newSummary = formatFrequencySummary(a);
            if (newSummary.equals(NO_DATA_FOUND)) {
                Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                return false;
            }
            displayMode = false;
        } else if (preference == this.mGPUGovernor) {
            if (this.PrefCat != null) {
                this.root.removePreference(this.PrefCat);
            }
            newSummary = a;
            displayMode = false;
        } else {
            if (preference == this.mDisplayControl) {
                if (!isConfiguredListValue(this.mDisplayControl, a)) {
                    Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                    return false;
                }
                newSummary = "";
                displayMode = true;
            } else {
                return true;
            }
        }
        final String requestedValue = a;
        final String summary = newSummary;
        final Preference targetPreference = preference;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SysfsResult<String> result;
                if (targetPreference == mGPUControlFrequencies) {
                    result = AeroActivity.hardware.gpu().writeMaxFrequency(requestedValue);
                } else if (targetPreference == mGPUGovernor) {
                    result = AeroActivity.hardware.gpu().writeGovernor(requestedValue);
                } else {
                    result = AeroActivity.hardware.gpu().writeDisplayMode(requestedValue);
                }
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isSuccess()) {
                            Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!summary.equals("")) {
                            targetPreference.setSummary(summary);
                        }
                        if (displayMode) {
                            Toast.makeText(getActivity(), "Turn your display off/on :)", 1).show();
                        }
                    }
                });
            }
        }).start();
        return true;
    }

    private String joinValues(String[] values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(' ');
            result.append(value);
        }
        return result.toString();
    }

    /**
     * Checks whether a value is present in a preference's configured entry values.
     * Used to validate user input before applying display control settings.
     *
     * @param preference the CustomListPreference to check
     * @param candidate the value to validate
     * @return true if the candidate is a valid entry value, false otherwise
     */
    private boolean isConfiguredListValue(CustomListPreference preference, String candidate) {
        CharSequence[] entryValues = preference.getEntryValues();
        if (candidate == null || entryValues == null) {
            return false;
        }
        for (CharSequence entryValue : entryValues) {
            if (candidate.contentEquals(entryValue)) {
                return true;
            }
        }
        return false;
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
                String governorPath = null;
                try {
                    try {
                        SysfsDirectory governorRoot = new SysfsDirectory(this.mGPUGov);
                        SysfsResult<String> governor = AeroActivity.hardware.dynamicTunables()
                                .readSysfsParameter(governorRoot, "governor");
                        if (governor.isSuccess()) {
                            governorPath = this.mGPUGov + governor.getValue();
                            SysfsResult<String[]> listing = AeroActivity.hardware
                                    .dynamicTunables().listSysfsParameters(
                                            new SysfsDirectory(governorPath));
                            if (listing.isSuccess()) {
                                completeParamterList = listing.getValue();
                            } else {
                                Log.w("Aero", "Unable to discover GPU governor parameters: "
                                        + listing.getError());
                            }
                        } else {
                            Log.w("Aero", "Unable to read the active GPU governor: "
                                    + governor.getError());
                        }
                        if (completeParamterList == null
                                || completeParamterList.length == 0) {
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
                                h.genPrefFromDictionary(completeParamterList, governorPath);
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
                    } catch (IllegalArgumentException e) {
                        Log.w("Aero", "Rejected GPU governor parameter path", e);
                        Toast.makeText(getActivity(), "Looks like there are no parameters for this governor?", 1).show();
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
