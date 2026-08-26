package com.aero.control.sliderFragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.SplashScreen;
import com.aero.control.fragments.MiscSettingsFragment;
import com.aero.control.fragments.ProfileFragment;
import com.aero.control.fragments.StatisticsFragment;
import com.aero.control.helpers.FilePath;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Final fragment in the first-run tutorial slider that provides links to key
 * features and allows the user to complete the tutorial and launch the main app.
 */
public class TutorialFragment extends Fragment {
    private static final String ARG_PAGE = "Tutorial";
    private static final Typeface kitkatFont = Typeface.create("sans-serif-condensed", 0);
    private CheckBox mCheckbox;

    /**
     * Creates a new instance of this fragment with the specified page number.
     *
     * @param pageNumber the page number in the tutorial sequence
     * @return a new TutorialFragment instance
     */
    public static TutorialFragment create(int pageNumber) {
        TutorialFragment fragment = new TutorialFragment();
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
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tutorial_fragment, container, false);
        TextView heading = (TextView) rootView.findViewById(R.id.text_heading);
        heading.setText(R.string.introduction_tutorial_heading);
        heading.setTypeface(kitkatFont);
        TextView content = (TextView) rootView.findViewById(R.id.text_content);
        content.setText(R.string.introduction_tutorial_content);
        content.setTypeface(kitkatFont);
        this.mCheckbox = (CheckBox) rootView.findViewById(R.id.show_checkbox);
        this.mCheckbox.setTypeface(kitkatFont);
        return rootView;
    }

    @Override // android.support.v4.app.Fragment
    public void onResume() {
        super.onResume();
        ((SplashScreen) getActivity()).mSkip.setText(R.string.got_it);
        ((SplashScreen) getActivity()).mSkip.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.sliderFragments.TutorialFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                boolean check_state = TutorialFragment.this.mCheckbox.isChecked();
                try {
                    FileOutputStream fos = TutorialFragment.this.getActivity().openFileOutput(SplashScreen.FIRSTRUN_AERO, 0);
                    fos.write("1".getBytes());
                    if (!check_state) {
                        TutorialFragment.this.getActivity().openFileOutput("firstrun", 0).write("1".getBytes());
                        TutorialFragment.this.getActivity().openFileOutput("firstrun_cpu", 0).write("1".getBytes());
                        TutorialFragment.this.getActivity().openFileOutput(ProfileFragment.FILENAME_PERAPP, 0).write("1".getBytes());
                        TutorialFragment.this.getActivity().openFileOutput(ProfileFragment.FILENAME_PROFILES, 0).write("1".getBytes());
                        TutorialFragment.this.getActivity().openFileOutput(StatisticsFragment.FILENAME_STATISTICS, 0).write("1".getBytes());
                        TutorialFragment.this.getActivity().openFileOutput(FilePath.FILENAME, 0).write("1".getBytes());
                        TutorialFragment.this.getActivity().openFileOutput(MiscSettingsFragment.FILENAME_MISC, 0).write("1".getBytes());
                        fos = TutorialFragment.this.getActivity().openFileOutput("firstrun_appmonitor", 0);
                        fos.write("1".getBytes());
                    }
                    fos.close();
                } catch (IOException e) {
                    Log.e("Aero", "Could not save file(s). ", e);
                } catch (NullPointerException e2) {
                    Log.e("Aero", "OpenFileOutput probably was initialized on a null-object.", e2);
                }
                Intent i = new Intent(TutorialFragment.this.getActivity(), (Class<?>) AeroActivity.class);
                TutorialFragment.this.startActivity(i);
                TutorialFragment.this.getActivity().finish();
            }
        });
    }
}
