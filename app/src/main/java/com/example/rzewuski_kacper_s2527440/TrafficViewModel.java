package com.example.rzewuski_kacper_s2527440;

import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

import android.os.Handler;


public class TrafficViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<TrafficSituation>> situationsData;
    private final MutableLiveData<Integer> progressData;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    public TrafficViewModel() {
        situationsData = new MutableLiveData<>();
        progressData = new MutableLiveData<>();
    }

    public LiveData<ArrayList<TrafficSituation>> getSituationsData() {
        return situationsData;
    }

    public LiveData<Integer> getProgressData() {
        return progressData;
    }

    public void fetchTrafficData(String[] urls, String clientId, String clientKey) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                progressData.postValue(0);
                ArrayList<TrafficSituation> allSituations = new ArrayList<>();

                for (String myurl : urls) {
                    String result = downloadXml(myurl, clientId, clientKey);
                    if (result != null && !result.isEmpty()) {
                        allSituations.addAll(parseXml(result));
                    }
                }

                situationsData.postValue(allSituations);
                progressData.postValue(100);
            }
        }).start();
    }

    private String downloadXml(String myurl, String clientId, String clientKey) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(myurl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty(
                    "Authorization",
                    Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes())
            );
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            in.close();
            urlConnection.disconnect();
        } catch (IOException e) {
            Log.e("Network", "Error fetching data from " + myurl + ": " + e.getMessage(), e);
            return null;
        }
        return result.toString();
    }

    private ArrayList<TrafficSituation> parseXml(String result) {
        ArrayList<TrafficSituation> situationsList = new ArrayList<>();
        int i = result.indexOf("<?");
        if (i != -1) {
            result = result.substring(i);
        }

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

                        for (int attrIndex = 0; attrIndex < xpp.getAttributeCount(); attrIndex++) {
                            if (xpp.getAttributeName(attrIndex).contains("type")) {
                                currentSituation.setType(xpp.getAttributeValue(attrIndex));
                                break;
                            }
                        }
                    } else if (xpp.getName().equalsIgnoreCase("generalPublicComment")) {
                        isInsideComment = true;
                    } else if (xpp.getName().equalsIgnoreCase("value") && isInsideComment && currentSituation != null) {
                        try {
                            currentSituation.setGeneralPublicComment(xpp.nextText());
                        } catch (Exception ignored) {
                        }
                    } else if (xpp.getName().equalsIgnoreCase("latitude") && currentSituation != null) {
                        try {
                            currentSituation.setLatitude(Double.parseDouble(xpp.nextText()));
                        } catch (Exception ignored) {
                        }
                    } else if (xpp.getName().equalsIgnoreCase("longitude") && currentSituation != null) {
                        try {
                            currentSituation.setLongitude(Double.parseDouble(xpp.nextText()));
                        } catch (Exception ignored) {
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.getName().equalsIgnoreCase("generalPublicComment")) {
                        isInsideComment = false;
                    } else if (xpp.getName().equalsIgnoreCase("situationRecord") && currentSituation != null) {
                        situationsList.add(currentSituation);
                        currentSituation = null;
                    }
                }
                try {
                    eventType = xpp.next();
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("Parser", "Error parsing XML", e);
        }
        return situationsList;
    }

    public void startAutoRefresh(String[] urls, String clientId, String clientKey, long intervalMinutes) {
        stopAutoRefresh();

        long intervalMillis = intervalMinutes * 60 * 1000;
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("AutoRefresh", "Refreshing traffic data...");
                fetchTrafficData(urls, clientId, clientKey);
                refreshHandler.postDelayed(this, intervalMillis);
            }
        };

        // Start immediately
        refreshHandler.post(refreshRunnable);
    }

    public void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
    }
}
