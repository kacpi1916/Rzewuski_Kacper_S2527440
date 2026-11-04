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

public class MainFragment extends Fragment implements View.OnClickListener {
    private android.widget.ListView listView;
    private Button startButton;
    private java.util.ArrayList<TrafficSituation> situationsList = new java.util.ArrayList<>();

    final String clientId = "b1abd090-02e5-48e8-928d-021c75c22bc3";
    final String clientKey = "6wFkPp40MgOuDeopmzNOGtypnuLmBG2e-OPFB-48ezP3O17cb.kujUImRPavqaK-";
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    public MainFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        startButton = view.findViewById(R.id.startButton);
        listView = view.findViewById(R.id.listView);
        startButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            new Thread(new Task(unplannedEventsURL)).start();
        }
    }

    private class Task implements Runnable {
        private String result = "";
        private String myurl;
        BufferedReader in = null;
        String inputLine = "";

        public Task(String aurl) {
            myurl = aurl;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(myurl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                try {
                    in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    while ((inputLine = in.readLine()) != null) {
                        result = result + inputLine;
                    }
                    in.close();
                } catch (IOException e) {
                    Log.e("urlConnection", "ioexception");
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("URL", "ioexception");
            }

            int i = result.indexOf("<?");
            if (i != -1) result = result.substring(i);

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(result));

                int eventType = xpp.getEventType();
                TrafficSituation currentSituation = null;
                boolean isInsideComment = false;

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equalsIgnoreCase("situationRecord")) {
                            currentSituation = new TrafficSituation();
                            currentSituation.setRecordId(xpp.getAttributeValue(null, "id"));
                        } else if (xpp.getName().equalsIgnoreCase("generalPublicComment")) {
                            isInsideComment = true;
                        } else if (xpp.getName().equalsIgnoreCase("value") && isInsideComment && currentSituation != null) {
                            currentSituation.setGeneralPublicComment(xpp.nextText());
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (xpp.getName().equalsIgnoreCase("generalPublicComment")) {
                            isInsideComment = false;
                        } else if (xpp.getName().equalsIgnoreCase("situationRecord") && currentSituation != null) {
                            situationsList.add(currentSituation);
                            currentSituation = null;
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (XmlPullParserException | IOException e) {
                Log.e("Parsing", "Parsing error", e);
            }

            if (getActivity() == null) return;

            listView.post(new Runnable() {
                @Override
                public void run() {
                    TrafficAdapter adapter = new TrafficAdapter(requireContext(), situationsList);
                    listView.setAdapter(adapter);
                }
            });
        }
    }
}

