package com.aaronbrecher.neverlate.ui.activities;

import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.adapters.EventDetailPagerAdapter;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.PassedEventFragment;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import javax.inject.Inject;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

public class EventDetailActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, EventDetailFragment.EditedEventListener {
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    private DetailActivityViewModel mViewModel;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private Event mEvent;
    private Event mEditedEvent;
    private Intent mIntent;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mViewPager = findViewById(R.id.detail_view_pager);
        mTabLayout = findViewById(R.id.details_tab_layout);

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(DetailActivityViewModel.class);
        mIntent = getIntent();

        if (!mIntent.hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
            Toast.makeText(this, R.string.event_not_found_toast_text, Toast.LENGTH_LONG).show();
            finish();
        }
        //Samsung devices throw error when parsing this line fixes it
        mEvent = Event.convertJsonToEvent(mIntent.getStringExtra(Constants.EVENT_DETAIL_INTENT_EXTRA));
        if (mEvent == null) {
            Toast.makeText(this, R.string.event_not_found_toast_text, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        FloatingActionButton fab = findViewById(R.id.detail_edit_fab);
        if (mEvent.getLocation().isEmpty()) {
            fab.show();
        }
        setTitle(mEvent.getTitle());
        mViewModel.setEvent(mEvent);

        if (GeofenceUtils.eventIsPassedCurrentTime(mEvent.getEndTime())) {
            //show message that event has already passed.
            PassedEventFragment passedEventFragment = new PassedEventFragment();
            FrameLayout container = findViewById(R.id.detail_activity_missed_container);
            container.setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction().replace(R.id.detail_activity_missed_container, passedEventFragment).commit();
        } else {
            mViewPager.setAdapter(new EventDetailPagerAdapter(getSupportFragmentManager(), 2));
            mTabLayout.addTab(mTabLayout.newTab().setText("Details"));
            mTabLayout.addTab(mTabLayout.newTab().setText("Map"));
            mTabLayout.addOnTabSelectedListener(this);
            mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        }

        fab.setOnClickListener(v -> {
            int id = mEvent.getId();
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mIntent = intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        hideOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.cancel:
                mViewModel.setEvent(mEvent);
                hideOptionsMenu();
                Toast.makeText(this, R.string.event_update_cancelled_toast, Toast.LENGTH_SHORT).show();
                break;
            case R.id.save:
                if (!mEditedEvent.isWatching()) {
                    mViewModel.removeGeofenceForEvent(mEditedEvent);
                } else if (mEditedEvent.getTransportMode() != mEvent.getTransportMode()) {
                    mViewModel.resetFenceForEvent(mEditedEvent);
                }
                mEvent = mEditedEvent;
                mEditedEvent = null;
                mViewModel.setEvent(mEvent);
                mViewModel.updateEvent(mEvent);
                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
                dispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(dispatcher));
                hideOptionsMenu();
                Toast.makeText(this, R.string.event_updated_toast, Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showOptionsMenu() {
        mMenu.setGroupVisible(R.id.save_cancel_menu, true);
    }

    public void hideOptionsMenu() {
        mMenu.setGroupVisible(R.id.save_cancel_menu, false);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void updateEvent(Event event) {
        mEditedEvent = event;
    }
}
