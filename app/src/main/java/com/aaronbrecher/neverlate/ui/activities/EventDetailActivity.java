package com.aaronbrecher.neverlate.ui.activities;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;

import javax.inject.Inject;

public class EventDetailActivity extends AppCompatActivity{
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    private DetailActivityViewModel mViewModel;
    private FloatingActionButton mFab;
    private Event mEvent;
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(DetailActivityViewModel.class);
        mIntent = getIntent();

        if (!mIntent.hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
            Toast.makeText(this, "Unable to load event please try again", Toast.LENGTH_LONG).show();
            finish();
        }
        mEvent = mIntent.getParcelableExtra(Constants.EVENT_DETAIL_INTENT_EXTRA);
        mFab = findViewById(R.id.detail_edit_fab);
        setTitle(mEvent.getTitle());
        mViewModel.setEvent(mEvent);
        if(GeofenceUtils.eventIsPassedCurrentTime(mEvent.getEndTime())){
            //show message that event has already passed.
        }
        else {
            EventDetailFragment eventDetailFragment = new EventDetailFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.event_detail_fragment_container, eventDetailFragment, Constants.EVENT_DETAIL_FRAGMENT_TAG)
                    .commit();
        }
        mFab.setOnClickListener(v -> {
            int id = mEvent.getId();
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            startActivity(intent);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mIntent = intent;
    }
}
