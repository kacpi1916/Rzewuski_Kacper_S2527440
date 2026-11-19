package com.example.rzewuski_kacper_s2527440;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class MainFragment extends Fragment implements OnMapReadyCallback {

    private TrafficViewModel viewModel;
    private GoogleMap mMap;
    private EditText searchEditText;
    private Button searchButton;
    private ListView situationsListView;

    private List<TrafficSituation> allSituations = new ArrayList<>();
    private List<TrafficSituation> displayedSituations = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private final List<String> listItems = new ArrayList<>();

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
        situationsListView = view.findViewById(R.id.situationsListView);

        // Setup ListView adapter
        listAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, listItems);
        situationsListView.setAdapter(listAdapter);

        // Handle item click on the list: show details dialog and zoom map
        situationsListView.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < displayedSituations.size()) {
                TrafficSituation selected = displayedSituations.get(position);
                showSituationDetails(selected);

                if (mMap != null && (selected.getLatitude() != 0.0 || selected.getLongitude() != 0.0)) {
                    LatLng location = new LatLng(selected.getLatitude(), selected.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f));
                }
            }
        });

        // 1. Initialize Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 2. Connect ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        // 3. Observe traffic data
        viewModel.getSituationsData().observe(getViewLifecycleOwner(), situations -> {
            if (situations != null) {
                allSituations = situations;
                Toast.makeText(getContext(), "Loaded events: " + situations.size(), Toast.LENGTH_SHORT).show();
                updateUI(allSituations);
            }
        });

        // 4. Fetch both feeds on first launch
        if (viewModel.getSituationsData().getValue() == null) {
            String[] urls = {
                    "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml",
                    "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml"
            };
            viewModel.fetchTrafficData(
                    urls,
                    "b1abd090-02e5-48e8-928d-021c75c22bc3",
                    "6wFkPp40MgOuDeopmzNOGtypnuLmBG2e-OPFB-48ezP3O17cb.kujUImRPavqaK-"
            );
        }

        // 5. Handle Search button
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim().toLowerCase();
            if (query.isEmpty()) {
                updateUI(allSituations);
                return;
            }

            List<TrafficSituation> filtered = new ArrayList<>();
            for (TrafficSituation situation : allSituations) {
                if ((situation.getType() != null && situation.getType().toLowerCase().contains(query)) ||
                        (situation.getGeneralPublicComment() != null && situation.getGeneralPublicComment().toLowerCase().contains(query))) {
                    filtered.add(situation);
                }
            }

            updateUI(filtered);
            Toast.makeText(getContext(), "Found: " + filtered.size(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Default view centred on Scotland
        LatLng scotland = new LatLng(55.9533, -3.9);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scotland, 7f));

        if (!displayedSituations.isEmpty()) {
            updateMapMarkers(displayedSituations);
        }
    }

    private void updateUI(List<TrafficSituation> situations) {
        displayedSituations = new ArrayList<>(situations);
        updateMapMarkers(displayedSituations);
        updateListView(displayedSituations);
    }

    private void updateListView(List<TrafficSituation> situations) {
        listItems.clear();
        for (TrafficSituation situation : situations) {
            String type = situation.getType() != null ? situation.getType() : "Unknown Type";
            String comment = situation.getGeneralPublicComment() != null ? situation.getGeneralPublicComment() : "No description available";
            listItems.add(type + "\n" + comment);
        }
        listAdapter.notifyDataSetChanged();
    }

    private void showSituationDetails(TrafficSituation situation) {
        StringBuilder details = new StringBuilder();
        details.append("Record ID: ").append(situation.getRecordId() != null ? situation.getRecordId() : "N/A").append("\n\n");
        details.append("Type: ").append(situation.getType() != null ? situation.getType() : "Unknown").append("\n\n");
        details.append("Description:\n").append(situation.getGeneralPublicComment() != null ? situation.getGeneralPublicComment() : "No details available").append("\n\n");
        details.append("Coordinates: ").append(situation.getLatitude()).append(", ").append(situation.getLongitude());

        new AlertDialog.Builder(requireContext())
                .setTitle("Situation Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateMapMarkers(List<TrafficSituation> situations) {
        if (mMap == null) return;
        mMap.clear();

        for (TrafficSituation situation : situations) {
            LatLng position = new LatLng(situation.getLatitude(), situation.getLongitude());
            float markerColor = getMarkerColorForType(situation.getType());

            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(situation.getType())
                    .snippet(situation.getGeneralPublicComment())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        }
    }

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
