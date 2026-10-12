package com.aero.control.sliderFragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.SplashScreen;

/**
 * Fragment explaining the Set-On-Boot feature in the first-run tutorial slider.
 * Describes how settings can be automatically applied when the device boots.
 */
public class SetOnBootFragment extends Fragment {
    public static final String ARG_PAGE = "Set-On-Boot";
    public static final Typeface kitkatFont = Typeface.create("sans-serif-condensed", 0);

    /**
     * Creates a new instance of this fragment with the specified page number.
     *
     * @param pageNumber the page number in the tutorial sequence
     * @return a new SetOnBootFragment instance
     */
    public static SetOnBootFragment create(int pageNumber) {
        SetOnBootFragment fragment = new SetOnBootFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called when the fragment is created. Performs no special initialization.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override // android.support.v4.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Creates and returns the view hierarchy for this fragment.
     *
     * @param inflater the layout inflater
     * @param container the parent view group
     * @param savedInstanceState the saved instance state
     * @return the root view for this fragment
     */
    @Override // android.support.v4.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.set_on_boot_fragment, container, false);
        TextView heading = (TextView) rootView.findViewById(R.id.text_heading);
        heading.setText(R.string.introduction_set_on_boot_heading);
        heading.setTypeface(kitkatFont);
        TextView content = (TextView) rootView.findViewById(R.id.text_content);
        content.setText(R.string.introduction_set_on_boot_content);
        content.setTypeface(kitkatFont);
        return rootView;
    }

    /**
     * Called when the fragment resumes. Configures the skip button.
     */
    @Override // android.support.v4.app.Fragment
    public void onResume() {
        super.onResume();
        ((SplashScreen) getActivity()).initDefaultSkip();
    }
}
