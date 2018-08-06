package com.aaronbrecher.neverlate.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.ui.ListItemClickListener;

public class ListFragment extends Fragment{
    ListItemClickListener mListItemClickListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mListItemClickListener = (ListItemClickListener)context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString() +
            " must implement the ListItemClickListener interface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
