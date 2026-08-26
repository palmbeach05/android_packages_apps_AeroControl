package com.aero.control.sliderFragments;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.SplashScreen;

/**
 * Fragment displaying information about the per-app monitoring feature in the
 * first-run tutorial slider. Allows users to enable the monitoring service.
 */
public class PerAppFragment extends Fragment {
    public static final String ARG_PAGE = "PerApp";
    public static final Typeface kitkatFont = Typeface.create("sans-serif-condensed", 0);

    /**
     * Creates a new instance of this fragment with the specified page number.
     *
     * @param pageNumber the page number in the tutorial sequence
     * @return a new PerAppFragment instance
     */
    public static PerAppFragment create(int pageNumber) {
        PerAppFragment fragment = new PerAppFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override // android.support.v4.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override // android.support.v4.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.perappshow_fragment, container, false);
        TextView heading = (TextView) rootView.findViewById(R.id.text_heading);
        heading.setText(R.string.introduction_perapp_heading);
        heading.setTypeface(kitkatFont);
        TextView content = (TextView) rootView.findViewById(R.id.text_content);
        content.setText(R.string.introduction_perapp_content);
        content.setTypeface(kitkatFont);
        CheckBox checkbox = (CheckBox) rootView.findViewById(R.id.show_checkbox);
        checkbox.setTypeface(kitkatFont);
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.show_progress);
        progressBar.setVisibility(4);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.aero.control.sliderFragments.PerAppFragment.1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences mSharedPreference = PreferenceManager.getDefaultSharedPreferences(PerAppFragment.this.getActivity());
                SharedPreferences.Editor editor = mSharedPreference.edit();
                progressBar.setVisibility(0);
                if (checked) {
                    editor.putBoolean("per_app_service", true);
                } else {
                    editor.putBoolean("per_app_service", false);
                }
                editor.commit();
                new Handler().postDelayed(new Runnable() { // from class: com.aero.control.sliderFragments.PerAppFragment.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        progressBar.setVisibility(4);
                    }
                }, 1500L);
            }
        });
        return rootView;
    }

    @Override // android.support.v4.app.Fragment
    public void onResume() {
        super.onResume();
        ((SplashScreen) getActivity()).initDefaultSkip();
    }
}
