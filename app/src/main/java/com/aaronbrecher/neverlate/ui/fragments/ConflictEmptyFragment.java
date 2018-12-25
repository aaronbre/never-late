package com.aaronbrecher.neverlate.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository;
import com.aaronbrecher.neverlate.interfaces.NavigationControl;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConflictEmptyFragment extends Fragment {
    private NavigationControl mNavController;
    @Inject
    EventCompatibilityRepository mCompatibilityRepository;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        NeverLateApp.getApp().getAppComponent().inject(this);
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
        mCompatibilityRepository.queryCompatibility().observe(this, list -> {
            if(list != null && list.size() > 0){
                mNavController.navigateToDestination(R.id.conflictAnalysisFragment);
            }
        });
        return inflater.inflate(R.layout.fragment_conflict_empty, container, false);
    }
}
