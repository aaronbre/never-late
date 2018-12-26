package com.aaronbrecher.neverlate.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import com.aaronbrecher.neverlate.R

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

class SubscriptionFragment : Fragment() {

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_subscription, container, false)
        activity?.setTitle(R.string.subscription_title)
        val premiumList = rootView.findViewById<TextView>(R.id.subscription_bullet_list)
        val standardList = rootView.findViewById<TextView>(R.id.standard_bullet_list)
        val subscribeButton = rootView.findViewById<Button>(R.id.subscribe_button)
        premiumList.setText(R.string.subscription_bullet_list)
        standardList.setText(R.string.standard_bullet_list)
        subscribeButton.setOnClickListener { Navigation.findNavController(activity!!, R.id.nav_host_fragment).navigate(R.id.action_subscriptionFragment_to_purchaseSubscriptionFragment) }
        return rootView
    }
}
