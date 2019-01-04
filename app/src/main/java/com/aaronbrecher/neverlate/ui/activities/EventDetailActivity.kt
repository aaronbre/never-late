package com.aaronbrecher.neverlate.ui.activities

import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.utils.BackgroundUtils
import com.aaronbrecher.neverlate.utils.GeofenceUtils
import com.aaronbrecher.neverlate.adapters.EventDetailPagerAdapter
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment
import com.aaronbrecher.neverlate.ui.fragments.PassedEventFragment
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

import javax.inject.Inject

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager

class EventDetailActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener, EventDetailFragment.EditedEventListener {
    @Inject
    lateinit var mViewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mViewModel: DetailActivityViewModel
    private lateinit var mViewPager: ViewPager
    private lateinit var mTabLayout: TabLayout
    private lateinit var mEvent: Event
    private var mEditedEvent: Event? = null
    private lateinit var mIntent: Intent
    private lateinit var mMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)
        (application as NeverLateApp)
                .appComponent
                .inject(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        mViewPager = findViewById(R.id.detail_view_pager)
        mTabLayout = findViewById(R.id.details_tab_layout)

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(DetailActivityViewModel::class.java)
        mIntent = intent

        if (!mIntent.hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
            Toast.makeText(this, R.string.event_not_found_toast_text, Toast.LENGTH_LONG).show()
            finish()
        }
        val event = Event.convertJsonToEvent(mIntent.getStringExtra(Constants.EVENT_DETAIL_INTENT_EXTRA))
        if (event == null) {
            Toast.makeText(this, R.string.event_not_found_toast_text, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        mEvent = event
        val fab = findViewById<FloatingActionButton>(R.id.detail_edit_fab)
        if (mEvent.location!!.isEmpty()) {
            fab.show()
        }
        title = mEvent.title
        mViewModel.setEvent(mEvent)

        if (GeofenceUtils.eventIsPassedCurrentTime(mEvent.endTime)) {
            //show message that event has already passed.
            val passedEventFragment = PassedEventFragment()
            val container = findViewById<FrameLayout>(R.id.detail_activity_missed_container)
            container.visibility = View.VISIBLE
            mTabLayout.visibility = View.GONE
            mViewPager.visibility = View.GONE
            supportFragmentManager.beginTransaction().replace(R.id.detail_activity_missed_container, passedEventFragment).commit()
        } else {
            mViewPager.adapter = EventDetailPagerAdapter(supportFragmentManager, 2)
            mTabLayout.addTab(mTabLayout.newTab().setText("Details"))
            mTabLayout.addTab(mTabLayout.newTab().setText("Map"))
            mTabLayout.addOnTabSelectedListener(this)
            mViewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(mTabLayout))
        }

        fab.setOnClickListener {
            val id = mEvent.id
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id.toLong())
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mIntent = intent
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        mMenu = menu
        menuInflater.inflate(R.menu.detail_menu, menu)
        hideOptionsMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.cancel -> {
                mViewModel.setEvent(mEvent)
                hideOptionsMenu()
                Toast.makeText(this, R.string.event_update_cancelled_toast, Toast.LENGTH_SHORT).show()
            }
            R.id.save -> {
                mEditedEvent?.let {
                    if (!it.watching!!) {
                        mViewModel.removeGeofenceForEvent(it)
                    } else if (it.transportMode != mEvent.transportMode) {
                        mViewModel.resetFenceForEventForTransportChange(it)
                    }
                    mEvent = it
                    mEditedEvent = null
                    mViewModel.setEvent(mEvent)
                    mViewModel.updateEvent(mEvent)
                    val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
                    dispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(dispatcher))
                    hideOptionsMenu()
                    Toast.makeText(this, R.string.event_updated_toast, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun showOptionsMenu() {
        mMenu.setGroupVisible(R.id.save_cancel_menu, true)
    }

    fun hideOptionsMenu() {
        mMenu.setGroupVisible(R.id.save_cancel_menu, false)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        mViewPager.currentItem = tab.position
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {

    }

    override fun onTabReselected(tab: TabLayout.Tab) {

    }

    override fun updateEvent(event: Event) {
        mEditedEvent = event
    }
}
