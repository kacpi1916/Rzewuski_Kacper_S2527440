package com.example.rzewuski_kacper_s2527440;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class TrafficAdapter extends ArrayAdapter<TrafficSituation> {

    // adapter for displaying traffic events with icons, dates, and duration colour bars
    public TrafficAdapter(Context context, ArrayList<TrafficSituation> situations) {
        super(context, 0, situations);
    }

    // bind event data, category icon, and duration colour indicator to a list row
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TrafficSituation current = getItem(position);
        View durationBar = convertView.findViewById(R.id.viewDurationBar);
        ImageView typeIcon = convertView.findViewById(R.id.imageTypeIcon);
        TextView textType = convertView.findViewById(R.id.textType);
        TextView textComment = convertView.findViewById(R.id.textComment);
        TextView textDate = convertView.findViewById(R.id.textDate);

        if (current != null) {
            // display cleaned event type and generalPublicComment
            String rawType = current.getType();
            textType.setText(formatType(rawType, current.getRecordId()));
            textComment.setText(current.getGeneralPublicComment());

            // format date range and set duration colour bar
            long days = current.getDurationDays();
            String start = current.getFormattedDate(current.getStartTime());
            String end = current.getFormattedDate(current.getEndTime());

            if (days < 0) {
                durationBar.setBackgroundColor(Color.parseColor("#2196F3")); // Blue: ongoing/unspecified
                textDate.setText("Start: " + start + " (Ongoing)");
            } else if (days <= 3) {
                durationBar.setBackgroundColor(Color.parseColor("#4CAF50")); // Green: 0-3 days
                textDate.setText(start + " to " + end + " (" + days + "d)");
            } else if (days <= 14) {
                durationBar.setBackgroundColor(Color.parseColor("#FF9800")); // Orange: 4-14 days
                textDate.setText(start + " to " + end + " (" + days + "d)");
            } else {
                durationBar.setBackgroundColor(Color.parseColor("#F44336")); // Red: >14 days
                textDate.setText(start + " to " + end + " (" + days + "d)");
            }

            // set category icon and colour tint based on event type
            String lowerType = rawType != null ? rawType.toLowerCase() : "";
            if (lowerType.contains("roadworks") || lowerType.contains("maintenance") || lowerType.contains("construction")) {
                typeIcon.setImageResource(android.R.drawable.ic_menu_manage);
                typeIcon.setColorFilter(Color.parseColor("#FF9800"));
            } else if (lowerType.contains("accident") || lowerType.contains("incident") || lowerType.contains("obstruction") || lowerType.contains("vehicle")) {
                typeIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                typeIcon.setColorFilter(Color.parseColor("#F44336"));
            } else if (lowerType.contains("management") || lowerType.contains("lane") || lowerType.contains("carriageway") || lowerType.contains("restriction")) {
                typeIcon.setImageResource(android.R.drawable.ic_menu_directions);
                typeIcon.setColorFilter(Color.parseColor("#FBC02D"));
            } else {
                typeIcon.setImageResource(android.R.drawable.ic_dialog_info);
                typeIcon.setColorFilter(Color.parseColor("#2196F3"));
            }
        }

        return convertView;
    }

    // convert DATEX II CamelCase type into readable text
    private String formatType(String rawType, String fallbackId) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return "Event ID: " + fallbackId;
        }
        String cleaned = rawType.contains(":") ? rawType.substring(rawType.indexOf(':') + 1) : rawType;
        return cleaned.replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
