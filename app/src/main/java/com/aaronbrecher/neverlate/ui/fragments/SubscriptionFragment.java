package com.aaronbrecher.neverlate.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.aaronbrecher.neverlate.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class SubscriptionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_subscription,container, false);
        getActivity().setTitle(R.string.subscription_title);
        TextView premiumList = rootView.findViewById(R.id.subscription_bullet_list);
        TextView standardList = rootView.findViewById(R.id.standard_bullet_list);
        Button subscribeButton = rootView.findViewById(R.id.subscribe_button);
        premiumList.setText(R.string.subscription_bullet_list);
        standardList.setText(R.string.standard_bullet_list);
        subscribeButton.setOnClickListener((view)-> Navigation.findNavController(getActivity(), R.id.nav_host_fragment).navigate(R.id.action_subscriptionFragment_to_purchaseSubscriptionFragment));
        return rootView;
    }
}
