package com.aero.control.sliderFragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.SplashScreen;

/**
 * Fragment displaying the introduction screen in the first-run tutorial slider.
 * Shows a welcome message and the app logo with animations.
 */
/* JADX INFO: loaded from: classes.dex */
public class IntroductionFragment extends Fragment {
    public static final String ARG_PAGE = "Introduction";
    public static final Typeface kitkatFont = Typeface.create("sans-serif-condensed", 0);

    /**
     * Creates a new instance of this fragment with the specified page number.
     *
     * @param pageNumber the page number in the tutorial sequence
     * @return a new IntroductionFragment instance
     */
    public static IntroductionFragment create(int pageNumber) {
        IntroductionFragment fragment = new IntroductionFragment();
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
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.introduction_fragment, container, false);
        TextView heading = (TextView) rootView.findViewById(R.id.text_heading);
        heading.setText(R.string.introduction_welcome_heading);
        heading.setTypeface(kitkatFont);
        TextView content = (TextView) rootView.findViewById(R.id.text_content);
        content.setText(R.string.introduction_welcome_content);
        content.setTypeface(kitkatFont);
        ImageView image = (ImageView) rootView.findViewById(R.id.image_content);
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_up);
        anim.setStartOffset(500L);
        image.setAnimation(anim);
        return rootView;
    }

    @Override // android.support.v4.app.Fragment
    public void onResume() {
        super.onResume();
        ((SplashScreen) getActivity()).initDefaultSkip();
    }
}
