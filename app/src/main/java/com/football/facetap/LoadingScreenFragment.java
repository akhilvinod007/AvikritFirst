package com.football.facetap;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoadingScreenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoadingScreenFragment extends Fragment {

    // TODO: Rename and change types and number of parameters
    public static LoadingScreenFragment newInstance() {
        LoadingScreenFragment fragment = new LoadingScreenFragment();
        return fragment;
    }

    public LoadingScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dbloader, container, false);
    }


}
