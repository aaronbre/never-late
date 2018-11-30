package com.aaronbrecher.neverlate.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.interfaces.NavigationControl;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;

public class AppSnoozedFragment extends Fragment {
    private Button mButton;
    private NavigationControl mNavController;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mNavController = (NavigationControl) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement the ListItemClickListener interface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_app_snoozed, container, false);
        mButton = rootView.findViewById(R.id.app_snoozed_cancel_button);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mButton.setOnClickListener(v -> mNavController.navigateToDestination(R.id.snoozeFragment));
    }
}
