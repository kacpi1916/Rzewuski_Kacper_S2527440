package com.example.rzewuski_kacper_s2527440;

import android.os.Handler;
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

public class TrafficViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<TrafficSituation>> situationsData;
    private final MutableLiveData<Integer> progressData;
    private final MutableLiveData<String> errorData;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    public TrafficViewModel() {
        situationsData = new MutableLiveData<>();
        progressData = new MutableLiveData<>();
        errorData = new MutableLiveData<>();
    }

    // pass parsed traffic situations to the UI
    public LiveData<ArrayList<TrafficSituation>> getSituationsData() {
        return situationsData;
    }

    public LiveData<Integer> getProgressData() {
        return progressData;
    }

    //  pass network or parsing error messages to the UI
    public LiveData<String> getErrorData() {
        return errorData;
    }
    // data fetching, XML parsing
    public void fetchTrafficData(String[] urls, String clientId, String clientKey) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                progressData.postValue(0);
                ArrayList<TrafficSituation> allSituations = new ArrayList<>();
                boolean anySuccess = false;

                for (String myurl : urls) {
                    String result = downloadXml(myurl, clientId, clientKey);
                    if (result != null && !result.isEmpty()) {
                        anySuccess = true;
                        allSituations.addAll(parseXml(result));
                    }
                }

                if (anySuccess) {
                    situationsData.postValue(allSituations);
                    errorData.postValue(null);
                } else {
                    if (situationsData.getValue() != null && !situationsData.getValue().isEmpty()) {
                        errorData.postValue("No internet connection. Showing last available data.");
                    } else {
                        errorData.postValue("Unable to load traffic data. Please check your internet connection.");
                    }
                }

                progressData.postValue(100);
            }
        }).start();
    }

    // fetch XML
    private String downloadXml(String myurl, String clientId, String clientKey) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(myurl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
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
        } catch (IOException e) {
            Log.e("Network", "Error fetching data from " + myurl + ": " + e.getMessage(), e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result.toString();
    }

    // parse DATEX II XML into a list of TrafficSituation objects
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
                    } else if (xpp.getName().equalsIgnoreCase("overallStartTime") && currentSituation != null) {
                        try {
                            currentSituation.setStartTime(xpp.nextText());
                        } catch (Exception ignored) {
                        }
                    } else if (xpp.getName().equalsIgnoreCase("overallEndTime") && currentSituation != null) {
                        try {
                            currentSituation.setEndTime(xpp.nextText());
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

    // start auto refresh data from Datex II
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

        refreshHandler.post(refreshRunnable);
    }

    // stop auto refresh data from Datex II
    public void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    // stop background tasks when the ViewModel is cleared
    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
    }
}