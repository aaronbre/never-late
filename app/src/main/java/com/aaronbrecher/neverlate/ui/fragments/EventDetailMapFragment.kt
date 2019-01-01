package com.aaronbrecher.neverlate.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.Utils.SystemUtils
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.viewmodels.BaseViewModel
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

class EventDetailMapFragment : Fragment(), OnMapReadyCallback {

    @Inject
    internal lateinit var mViewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences
    @Inject
    internal lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var mViewModel: BaseViewModel
    private var mEventMarker: Marker? = null
    private var mUserLocationLatLng: LatLng? = null
    private var mMapFragment: SupportMapFragment? = null
    private var mEvent: Event? = null

    private val mEventObserver = Observer<Event> { event ->
        mMapFragment!!.getMapAsync(this@EventDetailMapFragment)
        mEvent = event
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val appComponent = (activity?.application as NeverLateApp).appComponent
        appComponent.inject(this)
        mViewModel = if (resources.getBoolean(R.bool.is_tablet)) {
            ViewModelProviders.of(activity!!, mViewModelFactory).get(MainActivityViewModel::class.java)
        } else {
            ViewModelProviders.of(activity!!, mViewModelFactory).get(DetailActivityViewModel::class.java)
        }

    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_event_detail_map, container, false)
        mMapFragment = childFragmentManager
                .findFragmentById(R.id.event_detail_map) as SupportMapFragment
        mViewModel.mEvent.observe(this, mEventObserver)
        return rootView
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        if (SystemUtils.hasLocationPermissions(activity)) {
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null)
                    mUserLocationLatLng = LatLng(location.latitude, location.longitude)
                setUpMap(googleMap)
            }
        } else {
            setUpMap(googleMap)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setUpMap(googleMap: GoogleMap) {
        if (mEventMarker != null) mEventMarker!!.remove()
        googleMap.isMyLocationEnabled = true
        if (!mEvent!!.location!!.isEmpty()) {
            val latLng = mEvent!!.locationLatlng
            val builder = LatLngBounds.Builder()
            if (null != latLng) {
                mEventMarker = googleMap.addMarker(MarkerOptions().position(latLng)
                        .title(mEvent!!.title))
                builder.include(latLng)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
            }

            if (null != mUserLocationLatLng) {
                builder.include(mUserLocationLatLng)
            }
            googleMap.setOnMapLoadedCallback {
                val cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 200)
                googleMap.animateCamera(cu)
            }
        } else if (null != mUserLocationLatLng) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mUserLocationLatLng, 10f))
        }
    }
}
