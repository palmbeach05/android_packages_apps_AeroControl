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
import com.aero.control.helpers.HardwareMetricsPoller;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final AtomicInteger mInitializationGeneration = new AtomicInteger();
    private boolean mGpuControlAvailable;

    private static final class GpuInitialState {
        private SysfsResult<Boolean> sweepState;
        private SysfsResult<Boolean> doubleTapState;
        private SysfsResult<Boolean> controlState;
        private SysfsResult<Boolean> controlAvailable;
        private SysfsResult<String> currentFrequency;
        private SysfsResult<String[]> availableFrequencies;
        private SysfsResult<String[]> rawFrequencies;
        private SysfsResult<String[]> availableGovernors;
        private SysfsResult<String> currentGovernor;
        private SysfsResult<String> displayMode;
        private SysfsResult<Boolean> colorAvailable;
        private SysfsResult<String[]> colorValues;
        private String governorPath;
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int generation = this.mInitializationGeneration.incrementAndGet();
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
        this.mDisplayControl.setOnPreferenceChangeListener(this);
        CustomPreference gpu_gov_settings = (CustomPreference) this.root.findPreference("gpu_gov_settings");
        configureGpuGovernorNavigation(gpu_gov_settings);
        gpu_gov_settings.setEnabled(false);
        CharSequence[] display_entries = {getText(R.string.defy_red_colors), getText(R.string.defy_green_colors), getText(R.string.defy_energy_saver)};
        CharSequence[] display_values = {"31", "9", "0"};
        this.mDisplayControl.setEntries(display_entries);
        this.mDisplayControl.setEntryValues(display_values);
        this.mGPUControlFrequencies.setDialogIcon(R.drawable.gpu);
        setGpuControlsEnabled(false);
        loadInitialHardwareState(gpuCategory, generation);
    }

    private void configureGpuGovernorNavigation(CustomPreference preference) {
        preference.setOrder(35);
        preference.setHideOnBoot(true);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference clicked) {
                if (GPUFragment.this.mGPUGovernorFragment == null) {
                    GPUFragment.this.mGPUGovernorFragment = new GPUGovernorFragment();
                }
                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!GPUFragment.this.isAdded()
                                || GPUFragment.this.getFragmentManager() == null) {
                            return;
                        }
                        try {
                            GPUFragment.this.getFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.animator.fade_in,
                                            android.R.animator.fade_out)
                                    .replace(R.id.content_frame,
                                            GPUFragment.this.mGPUGovernorFragment)
                                    .addToBackStack("GPU Governor").commit();
                        } catch (IllegalStateException e) {
                            Log.e("Aero", "Could not commit fragment transaction, state already saved.", e);
                        }
                    }
                });
                return true;
            }
        });
    }

    private void setGpuControlsEnabled(boolean enabled) {
        this.mGPUControl.setEnabled(enabled);
        this.mSweep2wake.setEnabled(enabled);
        this.mDoubletap2Wake.setEnabled(enabled);
        this.mGPUControlFrequencies.setEnabled(enabled);
        this.mGPUGovernor.setEnabled(enabled);
        this.mDisplayControl.setEnabled(enabled);
        this.mColorControl.setEnabled(enabled);
    }

    private void setGpuTuningEnabled(boolean enabled) {
        this.mGPUControlFrequencies.setEnabled(enabled);
        this.mGPUGovernor.setEnabled(enabled);
    }

    private void loadInitialHardwareState(final PreferenceCategory gpuCategory,
            final int generation) {
        HardwareMetricsPoller.instance().executeOnWorker(new Runnable() {
            @Override
            public void run() {
                final GpuInitialState state = readInitialHardwareState();
                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded() || generation != mInitializationGeneration.get()) {
                            return;
                        }
                        applyInitialHardwareState(gpuCategory, state);
                    }
                });
            }
        });
    }

    private GpuInitialState readInitialHardwareState() {
        GpuInitialState state = new GpuInitialState();
        state.sweepState = AeroActivity.hardware.gpu().readToggle(
                GpuController.Toggle.SWEEP_TO_WAKE);
        state.doubleTapState = AeroActivity.hardware.gpu().readToggle(
                GpuController.Toggle.DOUBLE_TAP_TO_WAKE);
        state.controlState = AeroActivity.hardware.gpu().readToggle(
                GpuController.Toggle.CONTROL);
        state.controlAvailable = AeroActivity.hardware.gpu().isControlAvailable();
        state.currentFrequency = AeroActivity.hardware.gpu().readMaxFrequency();
        state.availableFrequencies =
                AeroActivity.hardware.gpu().readAvailableFrequencies(ReadMode.FREQUENCY_MHZ);
        state.rawFrequencies =
                AeroActivity.hardware.gpu().readAvailableFrequencies(ReadMode.RAW);
        state.displayMode = AeroActivity.hardware.gpu().readDisplayMode();
        state.colorAvailable = AeroActivity.hardware.displayColor().isAvailable();
        state.colorValues = AeroActivity.hardware.displayColor().readColorValues();
        state.availableGovernors = AeroActivity.hardware.gpu().readAvailableGovernors();
        state.currentGovernor = AeroActivity.hardware.gpu().readGovernor();
        for (String path : FilePath.GPU_GOV_ARRAY) {
            if (AeroActivity.genHelper.doesExist(path)) {
                state.governorPath = path;
            }
        }
        return state;
    }

    private void applyInitialHardwareState(PreferenceCategory gpuCategory,
            GpuInitialState state) {
        this.mGpuControlAvailable = state.controlAvailable.isSuccess()
                && state.controlAvailable.getValue();
        if (!state.sweepState.isSuccess()) {
            gpuCategory.removePreference(this.mSweep2wake);
        } else {
            boolean enabled = state.sweepState.getValue();
            this.mSweep2wake.setClicked(enabled);
            this.mSweep2wake.setLookUpDefaultValue(enabled ? "1" : "0");
        }
        if (!state.doubleTapState.isSuccess()) {
            gpuCategory.removePreference(this.mDoubletap2Wake);
        } else {
            boolean enabled = state.doubleTapState.getValue();
            this.mDoubletap2Wake.setClicked(enabled);
            this.mDoubletap2Wake.setLookUpDefaultValue(enabled ? "1" : "0");
        }
        if (!state.controlState.isSuccess() || !state.controlAvailable.isSuccess()
                || !state.controlAvailable.getValue()) {
            gpuCategory.removePreference(this.mGPUControl);
        } else {
            boolean enabled = state.controlState.getValue();
            this.mGPUControl.setClicked(enabled);
            this.mGPUControl.setLookUpDefaultValue(enabled ? "1" : "0");
        }
        String frequencyError = null;
        if (!state.currentFrequency.isSuccess()) {
            frequencyError = state.currentFrequency.getError();
        } else if (!state.availableFrequencies.isSuccess()) {
            frequencyError = state.availableFrequencies.getError();
        } else if (!state.rawFrequencies.isSuccess()) {
            frequencyError = state.rawFrequencies.getError();
        } else if (state.availableFrequencies.getValue().length == 0
                || state.availableFrequencies.getValue().length
                        != state.rawFrequencies.getValue().length) {
            frequencyError = "GPU frequency table is empty or inconsistent";
        }
        if (frequencyError != null) {
            Log.w("Aero", "Unable to read GPU frequency capabilities; showing fallback "
                    + "choices: " + frequencyError);
            this.mGPUControlFrequencies.setEntries(R.array.gpu_frequency_list);
            this.mGPUControlFrequencies.setEntryValues(R.array.gpu_frequency_list_values);
            if (state.currentFrequency.isSuccess()) {
                String frequency = state.currentFrequency.getValue();
                this.mGPUControlFrequencies.setValue(frequency);
                this.mGPUControlFrequencies.setSummary(formatFrequencySummary(frequency));
            } else {
                gpuCategory.removePreference(this.mGPUControlFrequencies);
            }
        } else {
            this.mGPUControlFrequencies.setEntries(state.availableFrequencies.getValue());
            this.mGPUControlFrequencies.setEntryValues(state.rawFrequencies.getValue());
            String frequency = state.currentFrequency.getValue();
            this.mGPUControlFrequencies.setValue(frequency);
            this.mGPUControlFrequencies.setSummary(formatFrequencySummary(frequency));
        }
        if (!state.displayMode.isSuccess()) {
            gpuCategory.removePreference(this.mDisplayControl);
        } else {
            this.mDisplayControl.setValue(state.displayMode.getValue());
            this.mDisplayControl.setSummary(this.mDisplayControl.getEntry());
        }
        if (!state.colorAvailable.isSuccess() || !state.colorAvailable.getValue()) {
            gpuCategory.removePreference(this.mColorControl);
        } else {
            this.mColorControl.setLookUpDefaultValue(state.colorValues.isSuccess()
                    ? joinValues(state.colorValues.getValue()) : NO_DATA_FOUND);
        }
        Preference governorSettings = this.root.findPreference("gpu_gov_settings");
        if (state.governorPath == null) {
            gpuCategory.removePreference(governorSettings);
        } else {
            this.mGPUGov = state.governorPath;
            governorSettings.setEnabled(true);
        }
        if (!state.availableGovernors.isSuccess() || !state.currentGovernor.isSuccess()) {
            gpuCategory.removePreference(this.mGPUGovernor);
        } else {
            this.mGPUGovernor.setEntries(state.availableGovernors.getValue());
            this.mGPUGovernor.setEntryValues(state.availableGovernors.getValue());
            this.mGPUGovernor.setValue(state.currentGovernor.getValue());
            this.mGPUGovernor.setSummary(state.currentGovernor.getValue());
            this.mGPUGovernor.setDialogIcon(R.drawable.device_old);
        }
        if (gpuCategory.getPreferenceCount() <= 0) {
            gpuCategory.setTitle(R.string.no_gpu_data);
        }
        setGpuControlsEnabled(true);
        if (this.mGpuControlAvailable) {
            setGpuTuningEnabled(state.controlState.isSuccess()
                    && state.controlState.getValue());
        }
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
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
    private void showColorControl(final SharedPreferences.Editor editor,
            final CustomPreference cusPref) {
        final int generation = this.mInitializationGeneration.get();
        HardwareMetricsPoller.instance().executeOnWorker(new Runnable() {
            @Override
            public void run() {
                final SysfsResult<String[]> colorValues =
                        AeroActivity.hardware.displayColor().readColorValues();
                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded() || generation != mInitializationGeneration.get()) {
                            return;
                        }
                        displayColorControl(editor, cusPref, colorValues);
                    }
                });
            }
        });
    }

    private void displayColorControl(final SharedPreferences.Editor editor,
            final CustomPreference cusPref, SysfsResult<String[]> colorValues) {
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
                final SysfsResult<String> writeResult =
                        AeroActivity.hardware.displayColor().writeColorValue(rgbValues);
                final SysfsResult<String> readbackResult =
                        AeroActivity.hardware.displayColor().readColorValue();
                if (!isAdded()) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        String actualValue = readbackResult.isSuccess()
                                ? normalizeColorValue(readbackResult.getValue()) : null;
                        String requestedValue = normalizeColorValue(rgbValues);
                        if (actualValue == null || !requestedValue.equals(actualValue)) {
                            Log.e("Aero", "Failed to update display color: "
                                    + (!readbackResult.isSuccess()
                                    ? readbackResult.getError()
                                    : "readback was " + readbackResult.getValue()));
                            Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!writeResult.isSuccess()) {
                            Log.w("Aero", "Display color readback confirms the requested value "
                                    + "despite a write verification error: "
                                    + writeResult.getError());
                        }
                        if (cusPref.isChecked().booleanValue()) {
                            boolean saved = editor.putString(cusPref.getName(), actualValue)
                                    .commit();
                            if (!saved) {
                                Log.e("Aero", "Failed to save display color preference");
                                Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private String normalizeColorValue(String value) {
        if (value == null) {
            return null;
        }
        String[] components = value.trim().split("\\s+");
        if (components.length != 3) {
            return null;
        }
        return components[0] + " " + components[1] + " " + components[2];
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mColorDialog != null) {
            this.mColorDialog.dismiss();
        }
    }

    @Override // android.app.Fragment
    public void onDestroy() {
        this.mInitializationGeneration.incrementAndGet();
        super.onDestroy();
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
        if (preference == this.mSweep2wake) {
            return applyToggle(this.mSweep2wake, GpuController.Toggle.SWEEP_TO_WAKE);
        } else if (preference == this.mDoubletap2Wake) {
            return applyToggle(this.mDoubletap2Wake,
                    GpuController.Toggle.DOUBLE_TAP_TO_WAKE);
        } else if (preference == this.mColorControl) {
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(getActivity()).edit();
            showColorControl(editor, this.mColorControl);
            return true;
        } else if (preference == this.mGPUControl) {
            return applyToggle(this.mGPUControl, GpuController.Toggle.CONTROL);
        }
        return false;
    }

    private boolean applyToggle(final CustomPreference preference, final GpuController.Toggle toggle) {
        final boolean previous = preference.isClicked().booleanValue();
        final boolean requested = !previous;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SysfsResult<Boolean> writeResult =
                        AeroActivity.hardware.gpu().writeToggle(toggle, requested);
                final SysfsResult<Boolean> readbackResult =
                        AeroActivity.hardware.gpu().readToggle(toggle);
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        if (readbackResult.isSuccess()) {
                            preference.setClicked(readbackResult.getValue());
                        } else {
                            preference.setClicked(Boolean.valueOf(previous));
                        }
                        if (!readbackResult.isSuccess()
                                || readbackResult.getValue().booleanValue() != requested) {
                            String error = !readbackResult.isSuccess()
                                    ? readbackResult.getError()
                                    : "requested " + requested + " but read back "
                                            + readbackResult.getValue();
                            if (!writeResult.isSuccess()) {
                                error += "; write failed: " + writeResult.getError();
                            }
                            Log.e("Aero", "Failed to update GPU toggle " + toggle + ": "
                                    + error);
                            Toast.makeText(getActivity(), R.string.hardware_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!writeResult.isSuccess()) {
                            Log.w("Aero", "GPU toggle readback confirms the requested state "
                                    + "despite a write verification error for " + toggle + ": "
                                    + writeResult.getError());
                        }
                        if (toggle == GpuController.Toggle.CONTROL
                                && mGpuControlAvailable) {
                            setGpuTuningEnabled(readbackResult.getValue());
                        }
                        if (preference.isChecked().booleanValue()) {
                            boolean saved = PreferenceManager.getDefaultSharedPreferences(
                                    getActivity()).edit()
                                    .putString(preference.getName(),
                                            readbackResult.getValue() ? "1" : "0")
                                    .commit();
                            if (!saved) {
                                Log.e("Aero", "Failed to save GPU toggle preference "
                                        + preference.getName());
                                Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        }).start();
        return true;
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String requestedValue = (String) newValue;
        final String previousValue;
        if (preference == this.mGPUControlFrequencies) {
            if (formatFrequencySummary(requestedValue).equals(NO_DATA_FOUND)) {
                Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                return false;
            }
            previousValue = this.mGPUControlFrequencies.getValue();
        } else if (preference == this.mGPUGovernor) {
            previousValue = this.mGPUGovernor.getValue();
        } else {
            if (preference == this.mDisplayControl) {
                if (!isConfiguredListValue(this.mDisplayControl, requestedValue)) {
                    Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                    return false;
                }
                previousValue = this.mDisplayControl.getValue();
            } else {
                return true;
            }
        }
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
                final SysfsResult<String> readbackResult;
                if (targetPreference == mGPUControlFrequencies) {
                    readbackResult = AeroActivity.hardware.gpu().readMaxFrequency();
                } else if (targetPreference == mGPUGovernor) {
                    readbackResult = AeroActivity.hardware.gpu().readGovernor();
                } else {
                    readbackResult = AeroActivity.hardware.gpu().readDisplayMode();
                }
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        String actualValue = readbackResult.isSuccess()
                                ? readbackResult.getValue().trim() : null;
                        if (actualValue == null) {
                            if (previousValue != null) {
                                updateGpuListPreference(
                                        (CustomListPreference) targetPreference, previousValue);
                            }
                            Log.e("Aero", "Unable to verify GPU setting: "
                                    + readbackResult.getError());
                            Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        updateGpuListPreference(
                                (CustomListPreference) targetPreference, actualValue);
                        if (!requestedValue.equals(actualValue)) {
                            String error = "GPU setting did not accept " + requestedValue
                                    + "; active value is " + actualValue;
                            if (!result.isSuccess()) {
                                error += "; write failed: " + result.getError();
                            }
                            Log.e("Aero", error);
                            Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!result.isSuccess()) {
                            Log.w("Aero", "GPU setting readback confirms the requested value "
                                    + "despite a write verification error: " + result.getError());
                        }
                        if (targetPreference == mGPUGovernor && PrefCat != null) {
                            root.removePreference(PrefCat);
                        }
                        if (targetPreference == mDisplayControl) {
                            Toast.makeText(getActivity(), "Turn your display off/on :)", 1).show();
                        }
                    }
                });
            }
        }).start();
        return true;
    }

    private void updateGpuListPreference(CustomListPreference preference, String value) {
        preference.setValue(value);
        if (preference == mGPUControlFrequencies) {
            preference.setSummary(formatFrequencySummary(value));
        } else if (preference == mGPUGovernor) {
            preference.setSummary(value);
        } else {
            preference.setSummary(preference.getEntry());
        }
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
        if (this.mGPUGov != null) {
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
