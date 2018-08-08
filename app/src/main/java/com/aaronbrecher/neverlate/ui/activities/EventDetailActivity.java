package com.aaronbrecher.neverlate.ui.activities;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;

import javax.inject.Inject;

public class EventDetailActivity extends AppCompatActivity {
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    private DetailActivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(DetailActivityViewModel.class);
        Intent intent = getIntent();

        if (!intent.hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
            Toast.makeText(this, "Unable to load event please try again", Toast.LENGTH_LONG).show();
            finish();
        }
        Event event = intent.getParcelableExtra(Constants.EVENT_DETAIL_INTENT_EXTRA);
        setTitle(event.getTitle());
        mViewModel.setEvent(event);
        EventDetailFragment eventDetailFragment = new EventDetailFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.event_detail_fragment_container,eventDetailFragment, Constants.EVENT_DETAIL_FRAGMENT_TAG)
                .commit();

    }
}
