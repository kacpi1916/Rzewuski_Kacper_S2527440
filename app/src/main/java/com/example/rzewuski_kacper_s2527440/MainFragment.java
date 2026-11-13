package com.example.rzewuski_kacper_s2527440;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;

public class MainFragment extends Fragment implements View.OnClickListener {
    private android.widget.ListView listView;
    private Button startButton;
    private android.widget.ProgressBar mProgressBar;
    private TrafficViewModel viewModel;

    // Klucze API
    final String clientId = "b1abd090-02e5-48e8-928d-021c75c22bc3";
    final String clientKey = "6wFkPp40MgOuDeopmzNOGtypnuLmBG2e-OPFB-48ezP3O17cb.kujUImRPavqaK-";
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    public MainFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        startButton = view.findViewById(R.id.startButton);
        listView = view.findViewById(R.id.listView);
        mProgressBar = view.findViewById(R.id.bar1);
        mProgressBar.setMax(100);

        Button mapButton = view.findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(getActivity(), MapsActivity.class);

                ArrayList<TrafficSituation> currentList = viewModel.getSituationsData().getValue();

                if (currentList != null) {
                    intent.putExtra("situationsList", currentList);
                }
                startActivity(intent);
            }
        });
        startButton.setOnClickListener(this);

        // ViewModel connection
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);


        viewModel.getProgressData().observe(getViewLifecycleOwner(), progress -> {
            mProgressBar.setProgress(progress);
        });


        viewModel.getSituationsData().observe(getViewLifecycleOwner(), situations -> {
            if (situations != null) {
                TrafficAdapter adapter = new TrafficAdapter(requireContext(), situations);
                listView.setAdapter(adapter);
            }
        });

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            // Delegate task to ViewModel
            viewModel.fetchTrafficData(unplannedEventsURL, clientId, clientKey);
        }
    }
}

