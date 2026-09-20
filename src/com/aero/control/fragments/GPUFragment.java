package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.aero.control.helpers.AeroLog;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.GpuController;
import com.aero.control.helpers.DisplayColorController;
import com.aero.control.helpers.OperationResult;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.ReadMode;
import com.aero.control.helpers.SysfsResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for configuring GPU settings including maximum frequency and governor.
 * Provides save/apply functionality for boot and profile persistence.
 */
public class GPUFragment extends PlaceHolderFragment implements Preference.OnPreferenceChangeListener {
    private static final AeroLog LOG = AeroLog.forClass(GPUFragment.class);
    private static final String NO_DATA_FOUND = "Unavailable";
    private PreferenceCategory PrefCat;
    private CustomPreference mColorControl;
    private AlertDialog mColorDialog;
    private String[] mColorValues;
    private final ExecutorService mColorWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService mGpuWorker = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Object mColorRequestLock = new Object();
    private volatile int mColorGeneration;
    private long mLatestColorRequest;
    private ColorWriteRequest mPendingColorWrite;
    private boolean mColorWriterRunning;
    private CustomListPreference mDisplayControl;
    private CustomPreference mDoubletap2Wake;
    private CustomPreference mGPUControl;
    private CustomListPreference mGPUControlFrequencies;
    private String mGPUGov;
    private CustomListPreference mGPUGovernor;
    private GPUGovernorFragment mGPUGovernorFragment;
    private final GpuController mGpuController = AeroActivity.hardware.gpu();
    private final DisplayColorController mDisplayColorController = AeroActivity.hardware.displayColor();
    private CustomPreference mSweep2wake;
    private PreferenceScreen root;

    private static final class ColorWriteRequest {
        final int generation;
        final long sequence;
        final String value;
        final boolean persist;
        final SharedPreferences.Editor editor;
        final String preferenceName;

        /** Captures one generation-aware color write for the background writer. */
        ColorWriteRequest(int generation, long sequence, String value, boolean persist,
                SharedPreferences.Editor editor, String preferenceName) {
            this.generation = generation;
            this.sequence = sequence;
            this.value = value;
            this.persist = persist;
            this.editor = editor;
            this.preferenceName = preferenceName;
        }
    }

    /**
     * Builds the GPU preference screen and initializes controls from supported hardware nodes.
     *
     * @param savedInstanceState previously saved fragment state, if any
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        boolean checkmSweep2wake;
        boolean checkDoubletap2wake;
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
        Preference.OnPreferenceClickListener preferenceClickListener = new Preference.OnPreferenceClickListener() {
            /** Delegates each supported preference click to the fragment handler. */
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
        if (!AeroActivity.genHelper.doesExist(FilePath.SWEEP2WAKE)) {
            gpuCategory.removePreference(this.mSweep2wake);
        }
        if (!AeroActivity.genHelper.doesExist(FilePath.DOUBLETAP2WAKE)) {
            gpuCategory.removePreference(this.mDoubletap2Wake);
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
                /** Opens the detailed GPU governor settings screen. */
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    if (GPUFragment.this.mGPUGovernorFragment == null) {
                        GPUFragment.this.mGPUGovernorFragment = new GPUGovernorFragment();
                    }
                    AeroActivity.mHandler.post(new Runnable() { // from class: com.aero.control.fragments.GPUFragment.1.1
                        /** Replaces the current fragment when its state can still be changed. */
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!GPUFragment.this.isAdded() || GPUFragment.this.getFragmentManager() == null) {
                                return;
                            }
                            try {
                                GPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, GPUFragment.this.mGPUGovernorFragment).addToBackStack("GPU Governor").commit();
                            } catch (IllegalStateException e) {
                                LOG.error("Could not commit fragment transaction because state is already saved.", e);
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
        this.mGPUControl.setEnabled(false);
        this.mGPUControlFrequencies.setEnabled(false);
        this.mGPUGovernor.setEnabled(false);
        try {
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
        } catch (ArrayIndexOutOfBoundsException e) {
            Toast.makeText(getActivity(), "GPU Control is not supported with your kernel.", 1).show();
        }
        initializeGpuPreferences(gpuCategory);
        this.mGPUControlFrequencies.setDialogIcon(R.drawable.gpu);
    }

    /** Reads GPU state off the UI thread, then applies the snapshot on the main thread. */
    private void initializeGpuPreferences(final PreferenceCategory gpuCategory) {
        this.mGpuWorker.execute(new Runnable() {
            /** Collects all controller-backed initialization values without blocking onCreate. */
            @Override
            public void run() {
                final SysfsResult<Boolean> controlAvailable = mGpuController.isControlAvailable();
                final SysfsResult<String> currentFrequency = mGpuController.readMaxFrequency();
                final SysfsResult<String[]> frequencyEntries = mGpuController
                        .readAvailableFrequencies(ReadMode.FREQUENCY_MHZ);
                final SysfsResult<String[]> frequencyValues = mGpuController
                        .readAvailableFrequencies(ReadMode.RAW);
                final SysfsResult<String[]> governors = mGpuController.readAvailableGovernors();
                final SysfsResult<String> currentGovernor = mGpuController.readGovernor();
                final SysfsResult<Boolean> controlEnabled = mGpuController.readControlEnabled();

                mMainHandler.post(new Runnable() {
                    /** Applies the GPU snapshot only while this fragment remains attached. */
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!controlAvailable.isSuccess() || !controlAvailable.getValue()) {
                            gpuCategory.removePreference(mGPUControl);
                        } else {
                            mGPUControl.setEnabled(true);
                        }
                        if (!currentFrequency.isSuccess()) {
                            gpuCategory.removePreference(mGPUControlFrequencies);
                        } else {
                            mGPUControlFrequencies.setEnabled(true);
                        }
                        if (frequencyEntries.isSuccess() && frequencyValues.isSuccess()) {
                            mGPUControlFrequencies.setEntries(frequencyEntries.getValue());
                            mGPUControlFrequencies.setEntryValues(frequencyValues.getValue());
                        } else {
                            mGPUControlFrequencies.setEntries(R.array.gpu_frequency_list);
                            mGPUControlFrequencies.setEntryValues(R.array.gpu_frequency_list_values);
                        }
                        if (governors.isSuccess() && currentGovernor.isSuccess()) {
                            mGPUGovernor.setEntries(governors.getValue());
                            mGPUGovernor.setEntryValues(governors.getValue());
                            mGPUGovernor.setValue(currentGovernor.getValue());
                            mGPUGovernor.setSummary(currentGovernor.getValue());
                            mGPUGovernor.setDialogIcon(R.drawable.device_old);
                            mGPUGovernor.setEnabled(true);
                        } else {
                            gpuCategory.removePreference(mGPUGovernor);
                        }
                        if (currentFrequency.isSuccess()) {
                            String currentFreq = currentFrequency.getValue();
                            mGPUControlFrequencies.setValue(currentFreq);
                            mGPUControlFrequencies.setSummary(formatFrequencySummary(currentFreq));
                        }
                        boolean gpuControlEnabled = controlEnabled.isSuccess()
                                && controlEnabled.getValue();
                        if (controlEnabled.isSuccess()) {
                            mGPUControl.setLookUpDefaultValue(gpuControlEnabled ? "1" : "0");
                        }
                        mGPUControl.setSummary(gpuControlEnabled
                                ? R.string.enabled : R.string.disabled);
                        mGPUControl.setClicked(Boolean.valueOf(gpuControlEnabled));
                        if (gpuCategory.getPreferenceCount() <= 0) {
                            gpuCategory.setTitle(R.string.no_gpu_data);
                        }
                    }
                });
            }
        });
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
        final int generation = ++this.mColorGeneration;
        this.mColorWorker.execute(new Runnable() {
            /** Reads the current hardware color values away from the main thread. */
            @Override
            public void run() {
                final SysfsResult<String[]> colorResult = mDisplayColorController.readColorValues();
                mMainHandler.post(new Runnable() {
                    /** Displays the color controls when the asynchronous read is still current. */
                    @Override
                    public void run() {
                        if (!isColorCallbackCurrent(generation)) {
                            return;
                        }
                        if (!colorResult.isSuccess() || colorResult.getValue().length < 3) {
                            Toast.makeText(getActivity(), R.string.no_data_found, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showColorControlDialog(colorResult.getValue(), editor, cusPref, generation);
                    }
                });
            }
        });
    }

    /**
     * Builds and displays color controls for values read from the active fragment generation.
     *
     * @param colorValues current red, green, and blue hardware values
     * @param editor preference editor retained for the existing color-control API
     * @param cusPref color-control preference used to decide whether values are persisted
     * @param generation fragment generation that owns this dialog
     */
    private void showColorControlDialog(String[] colorValues, final SharedPreferences.Editor editor,
            final CustomPreference cusPref, final int generation) {
        this.mColorValues = colorValues;
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

                /** Synchronizes a valid red text value with the slider and write queue. */
                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            redValues.setProgress(i);
                            GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Synchronizes a valid green text value with the slider and write queue. */
                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            greenValues.setProgress(i);
                            GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Synchronizes a valid blue text value with the slider and write queue. */
                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            blueValues.setProgress(i);
                            GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Queues the final red slider value when the user releases the control. */
                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Queues the final green slider value when the user releases the control. */
                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Queues the final blue slider value when the user releases the control. */
                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Synchronizes a valid red text value with the legacy slider and write queue. */
                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            redValues.setProgress(i);
                            GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Synchronizes a valid green text value with the legacy slider and write queue. */
                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            greenValues.setProgress(i);
                            GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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

                /** Synchronizes a valid blue text value with the legacy slider and write queue. */
                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int i = Integer.parseInt(s.toString());
                        if (i <= 255 && i >= 0) {
                            blueValues.setProgress(i);
                            GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
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
                /** Queues the selected red value from the legacy slider. */
                @Override // com.aero.control.helpers.Android.Material.Slider.OnValueChangedListener
                public void onValueChanged(int value) {
                    redValue.setText("" + value);
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
                }
            });
            greenValues.setOnValueChangedListener(new Slider.OnValueChangedListener() { // from class: com.aero.control.fragments.GPUFragment.6
                /** Queues the selected green value from the legacy slider. */
                @Override // com.aero.control.helpers.Android.Material.Slider.OnValueChangedListener
                public void onValueChanged(int value) {
                    greenValue.setText("" + value);
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
                }
            });
            blueValues.setOnValueChangedListener(new Slider.OnValueChangedListener() { // from class: com.aero.control.fragments.GPUFragment.7
                /** Queues the selected blue value from the legacy slider. */
                @Override // com.aero.control.helpers.Android.Material.Slider.OnValueChangedListener
                public void onValueChanged(int value) {
                    blueValue.setText("" + value);
                    GPUFragment.this.setColorValues(redValue, greenValue, blueValue, cusPref, editor, generation);
                }
            });
        }
        builder.setTitle(R.string.pref_display_color);
        builder.setView(layout);
        this.mColorDialog = builder.create();
        this.mColorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            /** Invalidates asynchronous work owned by the dialog being dismissed. */
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mColorGeneration == generation) {
                    invalidateColorWork();
                }
            }
        });
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
        return AeroActivity.shell.toMHz(value.substring(0, value.length() - 3));
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
    public void setColorValues(EditText redValue, EditText greenValue, EditText blueValue, CustomPreference cusPref, SharedPreferences.Editor editor) {
        setColorValues(redValue, greenValue, blueValue, cusPref, editor, this.mColorGeneration);
    }

    /**
     * Validates a generation's RGB fields and coalesces them into the background write queue.
     *
     * @param redValue red component input
     * @param greenValue green component input
     * @param blueValue blue component input
     * @param cusPref color-control preference used to decide whether values are persisted
     * @param editor preference editor used to persist the color value
     * @param generation fragment generation that requested the write
     */
    private void setColorValues(EditText redValue, EditText greenValue, EditText blueValue,
            CustomPreference cusPref, SharedPreferences.Editor editor, int generation) {
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
        if (!isColorCallbackCurrent(generation)) {
            return;
        }
        String rgbValues = red + " " + green + " " + blue;
        synchronized (this.mColorRequestLock) {
            long sequence = ++this.mLatestColorRequest;
            this.mPendingColorWrite = new ColorWriteRequest(generation, sequence, rgbValues,
                    cusPref.isChecked().booleanValue(), editor, cusPref.getName());
            if (this.mColorWriterRunning) {
                return;
            }
            this.mColorWriterRunning = true;
        }
        this.mColorWorker.execute(new Runnable() {
            /** Drains coalesced color writes on the fragment's single worker thread. */
            @Override
            public void run() {
                drainColorWrites();
            }
        });
    }

    /** Writes the newest pending color request and reports current-generation failures. */
    private void drainColorWrites() {
        while (true) {
            final ColorWriteRequest request;
            synchronized (this.mColorRequestLock) {
                request = this.mPendingColorWrite;
                this.mPendingColorWrite = null;
                if (request == null) {
                    this.mColorWriterRunning = false;
                    return;
                }
            }

            final SysfsResult<String> result = this.mDisplayColorController.writeColorValue(request.value);
            boolean isLatest;
            synchronized (this.mColorRequestLock) {
                isLatest = request.sequence == this.mLatestColorRequest
                        && request.generation == this.mColorGeneration;
            }
            if (result.isSuccess() && request.persist) {
            } else if (!result.isSuccess() && isLatest) {
                this.mMainHandler.post(new Runnable() {
                    /** Reports a failed write only while its request still owns the dialog. */
                    @Override
                    public void run() {
                        if (isColorCallbackCurrent(request.generation)
                                && request.sequence == mLatestColorRequest) {
                            Toast.makeText(getActivity(), R.string.hardware_operation_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }

    /** Returns whether a color callback still belongs to this attached fragment generation. */
    private boolean isColorCallbackCurrent(int generation) {
        return generation == this.mColorGeneration && isAdded() && getActivity() != null;
    }

    /** Invalidates queued callbacks and discards any color write that has not started. */
    private void invalidateColorWork() {
        ++this.mColorGeneration;
        synchronized (this.mColorRequestLock) {
            ++this.mLatestColorRequest;
            this.mPendingColorWrite = null;
        }
    }

    /** Dismisses the color dialog when the fragment leaves the foreground. */
    @Override // android.app.Fragment
    public void onPause() {
        invalidateColorWork();
        super.onPause();
        if (this.mColorDialog != null) {
            this.mColorDialog.dismiss();
            this.mColorDialog = null;
        }
    }

    /** Stops the color worker when the fragment is permanently destroyed. */
    @Override // android.app.Fragment
    public void onDestroy() {
        invalidateColorWork();
        this.mColorWorker.shutdownNow();
        this.mGpuWorker.shutdownNow();
        super.onDestroy();
    }

    /**
     * Routes preference-row clicks through the shared GPU click handler.
     *
     * @param preferenceScreen screen containing the clicked preference
     * @param preference clicked preference
     * @return true when the click is handled
     */
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
            if (!applyToggle(this.mSweep2wake, FilePath.SWEEP2WAKE)) return false;
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mDoubletap2Wake) {
            if (!applyToggle(this.mDoubletap2Wake, FilePath.DOUBLETAP2WAKE)) return false;
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mColorControl) {
            cusPref = (CustomPreference) preference;
            showColorControl(editor, cusPref);
        } else if (preference == this.mGPUControl) {
            return applyGpuControlToggle();
        }
        if (cusPref != null && cusPref.isChecked().booleanValue() && cusPref.isClicked() != null) {
            String state = cusPref.isClicked().booleanValue() ? "1" : "0";
            editor.putString(cusPref.getName(), state).commit();
        }
        return true;
    }

    /**
     * Applies a generic boolean sysfs preference on a worker thread.
     *
     * @param preference preference whose UI state is being toggled
     * @param path sysfs path that stores the boolean state
     * @return true after the asynchronous write has been scheduled
     */
    private boolean applyToggle(final CustomPreference preference, final String path) {
        final boolean previous = preference.isClicked().booleanValue();
        final boolean requested = !previous;
        new Thread(new Runnable() {
            /** Writes the requested state without blocking the UI thread. */
            @Override
            public void run() {
                final OperationResult result = AeroActivity.shell.setRootInfoResult(requested ? "1" : "0", path);
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    /** Applies the write result to the preference UI. */
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

    /**
     * Applies the GPU control switch through the typed controller on a worker thread.
     *
     * @return true after the asynchronous write has been scheduled
     */
    private boolean applyGpuControlToggle() {
        final boolean previous = this.mGPUControl.isClicked().booleanValue();
        final boolean requested = !previous;
        final boolean persistOnSuccess = this.mGPUControl.isChecked().booleanValue();
        if (persistOnSuccess) {
            this.mGPUControl.onCheck(false);
        }
        this.mGPUControl.setEnabled(false);
        new Thread(new Runnable() {
            /** Writes the requested GPU control state without blocking the UI thread. */
            @Override
            public void run() {
                final SysfsResult<Boolean> result = mGpuController.writeControlEnabled(requested);
                mMainHandler.post(new Runnable() {
                    /** Updates or restores the preference after the controller write completes. */
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        mGPUControl.setEnabled(true);
                        if (!result.isSuccess()) {
                            mGPUControl.setClicked(Boolean.valueOf(previous));
                            mGPUControl.setLookUpDefaultValue(previous ? "1" : "0");
                            Toast.makeText(getActivity(), R.string.storage_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        mGPUControl.setClicked(Boolean.valueOf(requested));
                        mGPUControl.setLookUpDefaultValue(requested ? "1" : "0");
                        commitGpuControlPersistence(persistOnSuccess);
                    }
                });
            }
        }).start();
        return true;
    }

    /** Commits boot persistence only after an asynchronous GPU control write succeeds. */
    private void commitGpuControlPersistence(boolean persist) {
        if (!persist) {
            return;
        }
        this.mGPUControl.onCheck(true);
        String state = this.mGPUControl.isClicked().booleanValue() ? "1" : "0";
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putString(this.mGPUControl.getName(), state).commit();
    }

    /**
     * Validates and asynchronously applies changes to GPU and display list preferences.
     *
     * @param preference preference being changed
     * @param newValue requested preference value
     * @return true when the change is accepted for processing
     */
    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String a = (String) newValue;
        String newSummary = "";
        if (preference == this.mGPUControlFrequencies) {
            newSummary = formatFrequencySummary(a);
            if (newSummary.equals(NO_DATA_FOUND)) {
                Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                return false;
            }
        } else if (preference == this.mGPUGovernor) {
            if (this.PrefCat != null) {
                this.root.removePreference(this.PrefCat);
            }
            newSummary = a;
        } else {
            if (preference == this.mDisplayControl) {
                if (!isConfiguredListValue(this.mDisplayControl, a)) {
                    Toast.makeText(getActivity(), R.string.no_data_found, 1).show();
                    return false;
                }
                final String displayPath = AeroActivity.shell.escapeShellArg(FilePath.DISPLAY_COLOR);
                final String[] commands = {"chmod 0664 " + displayPath, "echo " + AeroActivity.shell.escapeShellArg(a) + " > " + displayPath};
                new Thread(new Runnable() {
                    /** Writes the display control value without blocking the UI thread. */
                    @Override
                    public void run() {
                        final OperationResult result = AeroActivity.shell.setRootInfoResult(commands);
                        if (!isAdded()) {
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            /** Reports the display control write result to the user. */
                            @Override
                            public void run() {
                                if (!result.isSuccess()) {
                                    Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Toast.makeText(getActivity(), "Turn your display off/on :)", 1).show();
                            }
                        });
                    }
                }).start();
            }
            return true;
        }
        final String requestedValue = a;
        final String summary = newSummary;
        final Preference targetPreference = preference;
        final boolean isFrequency = preference == this.mGPUControlFrequencies;
        new Thread(new Runnable() {
            /** Writes the requested GPU frequency or governor through the controller. */
            @Override
            public void run() {
                final SysfsResult<String> result = isFrequency
                        ? mGpuController.writeMaxFrequency(requestedValue)
                        : mGpuController.writeGovernor(requestedValue);
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    /** Reports a failed write or updates the applied preference summary. */
                    @Override
                    public void run() {
                        if (!result.isSuccess()) {
                            Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!summary.equals("")) {
                            targetPreference.setSummary(summary);
                        }
                    }
                });
            }
        }).start();
        return true;
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
                try {
                    try {
                        completeParamterList = AeroActivity.shell.getDirInfo(this.mGPUGov + AeroActivity.shell.getInfo(this.mGPUGov + "governor"), true);
                        if (completeParamterList == null) {
                            Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                            LOG.warn("We found no parameters for this governor, maybe because it has none?");
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
                                LOG.error("I couldn't get any files!", e);
                            }
                        }
                    } catch (NullPointerException e2) {
                        Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                        LOG.error("Couldn't find any parameters for this governor!", e2);
                        if (0 == 0) {
                            Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                            LOG.warn("We found no parameters for this governor, maybe because it has none?");
                        }
                    }
                    return true;
                } catch (Throwable th) {
                    if (completeParamterList == null) {
                        Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", 1).show();
                        LOG.warn("We found no parameters for this governor, maybe because it has none?");
                        return true;
                    }
                    throw th;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
