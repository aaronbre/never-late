package com.aaronbrecher.neverlate.ui.activities;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.aaronbrecher.neverlate.R;
import com.ramotion.paperonboarding.PaperOnboardingFragment;
import com.ramotion.paperonboarding.PaperOnboardingPage;

import java.util.ArrayList;

public class Onboarding extends AppCompatActivity {
    private FragmentManager mFragmentManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        mFragmentManager = getSupportFragmentManager();

        final PaperOnboardingFragment onboardingFragment = PaperOnboardingFragment.newInstance(getDataForOnboarding());
    }

    private ArrayList<PaperOnboardingPage> getDataForOnboarding() {
        return null;
    }
}
