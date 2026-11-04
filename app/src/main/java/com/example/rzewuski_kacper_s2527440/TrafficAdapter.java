package com.example.rzewuski_kacper_s2527440;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class TrafficAdapter extends ArrayAdapter<TrafficSituation> {

    public TrafficAdapter(Context context, ArrayList<TrafficSituation> situations) {
        super(context, 0, situations);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TrafficSituation current = getItem(position);
        TextView textId = convertView.findViewById(R.id.textId);
        TextView textComment = convertView.findViewById(R.id.textComment);

        if (current != null) {
            textId.setText("Event ID: " + current.getRecordId());
            textComment.setText(current.getGeneralPublicComment());
        }

        return convertView;
    }
}
