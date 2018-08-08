package com.aaronbrecher.neverlate.ui.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.databinding.EventDetailFragmentBinding;
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;

import javax.inject.Inject;

public class EventDetailFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    private DetailActivityViewModel mViewModel;
    private EventDetailFragmentBinding mBinding;

    final Observer<Event> mEventObserver = new Observer<Event>() {
        @Override
        public void onChanged(@Nullable Event event) {
            mBinding.setEvent(event);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        AppComponent appComponent = ((NeverLateApp)getActivity().getApplication()).getAppComponent();
        appComponent.inject(this);
        mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(DetailActivityViewModel.class);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = EventDetailFragmentBinding.inflate(inflater, container, false);
        mViewModel.getEvent().observe(getActivity(), mEventObserver);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.getEvent().removeObserver(mEventObserver);
    }
}
