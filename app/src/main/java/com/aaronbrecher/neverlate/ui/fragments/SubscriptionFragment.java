package com.aaronbrecher.neverlate.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aaronbrecher.neverlate.R;

public class SubscriptionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_subscription,container, false);
        TextView bulletList = rootView.findViewById(R.id.subscription_bullet_list);
        bulletList.setText(R.string.subscription_bullet_list);
        return rootView;
    }
}
