package com.example.rzewuski_kacper_s2527440;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import android.location.Location;

public class MainFragment extends Fragment implements OnMapReadyCallback {

    private TrafficViewModel viewModel;
    private GoogleMap mMap;
    private EditText searchEditText;
    private Button searchButton;
    private ListView situationsListView;

    private List<TrafficSituation> allSituations = new ArrayList<>();
    private List<TrafficSituation> displayedSituations = new ArrayList<>();
    private TrafficAdapter listAdapter;
    private final ArrayList<TrafficSituation> listItems = new ArrayList<>();
    private EditText distanceEditText;
    private Button distanceButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        distanceEditText = view.findViewById(R.id.distanceEditText);
        distanceButton = view.findViewById(R.id.distanceButton);
        situationsListView = view.findViewById(R.id.situationsListView);

        // Setup ListView adapter
        listAdapter = new TrafficAdapter(getContext(), listItems);
        situationsListView.setAdapter(listAdapter);

        // Handle item click on the list: show details dialog and zoom map
        situationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
                if (position >= 0 && position < displayedSituations.size()) {
                    TrafficSituation selected = displayedSituations.get(position);
                    MainFragment.this.showSituationDetails(selected);

                    if (mMap != null && (selected.getLatitude() != 0.0 || selected.getLongitude() != 0.0)) {
                        LatLng location = new LatLng(selected.getLatitude(), selected.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f));
                    }
                }
            }
        });

        // initialise Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // connect ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        // observe traffic data
        viewModel.getSituationsData().observe(getViewLifecycleOwner(), new Observer<ArrayList<TrafficSituation>>() {
            @Override
            public void onChanged(ArrayList<TrafficSituation> situations) {
                if (situations != null) {
                    allSituations = situations;
                    Toast.makeText(MainFragment.this.getContext(), "Loaded events: " + situations.size(), Toast.LENGTH_SHORT).show();
                    MainFragment.this.updateUI(allSituations);
                }
            }
        });
        viewModel.getErrorData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String errorMessage) {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Toast.makeText(MainFragment.this.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });

        //  fetch both feeds on first launch
        if (viewModel.getSituationsData().getValue() == null) {
            String[] urls = {
                    "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml",
                    "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml"
            };
            viewModel.startAutoRefresh(
                    urls,
                    BuildConfig.TRAFFIC_CLIENT_ID,
                    BuildConfig.TRAFFIC_CLIENT_KEY,
                    5
            );
        }

        // handle Search button
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchEditText.getText().toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    MainFragment.this.updateUI(allSituations);
                    return;
                }

                List<TrafficSituation> filtered = new ArrayList<>();
                for (TrafficSituation situation : allSituations) {
                    if ((situation.getType() != null && situation.getType().toLowerCase().contains(query)) ||
                            (situation.getGeneralPublicComment() != null && situation.getGeneralPublicComment().toLowerCase().contains(query))) {
                        filtered.add(situation);
                    }
                }

                MainFragment.this.updateUI(filtered);
                Toast.makeText(MainFragment.this.getContext(), "Found: " + filtered.size(), Toast.LENGTH_SHORT).show();
            }
        });

        // handle filter by distance button
        distanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String distanceText = distanceEditText.getText().toString().trim();
                if (distanceText.isEmpty()) {
                    MainFragment.this.updateUI(allSituations);
                    return;
                }

                try {
                    double maxDistanceKm = Double.parseDouble(distanceText);
                    double glasgowLat = 55.8642;
                    double glasgowLng = -4.2518;

                    List<TrafficSituation> filtered = new ArrayList<>();
                    float[] results = new float[1];

                    for (TrafficSituation situation : allSituations) {
                        if (situation.getLatitude() == 0.0 && situation.getLongitude() == 0.0) {
                            continue;
                        }
                        Location.distanceBetween(
                                glasgowLat, glasgowLng,
                                situation.getLatitude(), situation.getLongitude(),
                                results
                        );
                        double distanceKm = results[0] / 1000.0;
                        if (distanceKm <= maxDistanceKm) {
                            filtered.add(situation);
                        }
                    }

                    MainFragment.this.updateUI(filtered);
                    Toast.makeText(MainFragment.this.getContext(), "Events within " + maxDistanceKm + " km: " + filtered.size(), Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainFragment.this.getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // displays map on Scotland
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        LatLng scotland = new LatLng(55.9533, -3.9);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scotland, 7f));

        if (!displayedSituations.isEmpty()) {
            updateMapMarkers(displayedSituations);
        }
    }

    // refresh map markers and the list view
    private void updateUI(List<TrafficSituation> situations) {
        displayedSituations = new ArrayList<>(situations);
        updateMapMarkers(displayedSituations);
        updateListView(displayedSituations);
    }

    // update list data and refresh an adapter
    private void updateListView(List<TrafficSituation> situations) {
        listItems.clear();
        listItems.addAll(situations);
        listAdapter.notifyDataSetChanged();
    }

    // displays full event details
    private void showSituationDetails(TrafficSituation situation) {
        String rawType = situation.getType() != null ? situation.getType() : "Unknown";
        String displayType = rawType.contains(":") ? rawType.substring(rawType.indexOf(":") + 1) : rawType;

        StringBuilder details = new StringBuilder();
        details.append("Record ID: ").append(situation.getRecordId() != null ? situation.getRecordId() : "N/A").append("\n\n");
        details.append("Type: ").append(displayType).append("\n\n");
        details.append("Start Date: ").append(situation.getFormattedDate(situation.getStartTime())).append("\n");
        details.append("End Date: ").append(situation.getFormattedDate(situation.getEndTime())).append("\n");

        long days = situation.getDurationDays();
        if (days == -1) {
            details.append("Duration: Ongoing / Not specified\n\n");
        } else {
            String dayLabel = (days == 1) ? "1 day" : (days + " days");
            details.append("Duration: ").append(dayLabel).append("\n\n");
        }

        details.append("Description:\n").append(situation.getGeneralPublicComment() != null ? situation.getGeneralPublicComment() : "No details available").append("\n\n");
        details.append("Coordinates: ").append(situation.getLatitude()).append(", ").append(situation.getLongitude());

        new AlertDialog.Builder(requireContext())
                .setTitle("Situation Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // add colour markers for current events
    private void updateMapMarkers(List<TrafficSituation> situations) {
        if (mMap == null) return;
        mMap.clear();

        for (TrafficSituation situation : situations) {
            if (situation.getLatitude() == 0.0 && situation.getLongitude() == 0.0) {
                continue;
            }

            LatLng position = new LatLng(situation.getLatitude(), situation.getLongitude());
            float markerColor = getMarkerColorForType(situation.getType());

            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(situation.getType())
                    .snippet(situation.getGeneralPublicComment())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        }
    }

    // returns a marker colour based on the event type
    private float getMarkerColorForType(String type) {
        if (type == null) {
            return BitmapDescriptorFactory.HUE_AZURE;
        }

        String lowerType = type.toLowerCase();

        if (lowerType.contains("maintenance") || lowerType.contains("roadwork") || lowerType.contains("construction")) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        }
        if (lowerType.contains("accident") || lowerType.contains("incident") || lowerType.contains("obstruction")) {
            return BitmapDescriptorFactory.HUE_RED;
        }
        if (lowerType.contains("management") || lowerType.contains("restriction")) {
            return BitmapDescriptorFactory.HUE_YELLOW;
        }

        return BitmapDescriptorFactory.HUE_AZURE;
    }


}
