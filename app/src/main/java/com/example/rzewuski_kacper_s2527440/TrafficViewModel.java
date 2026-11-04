package com.example.rzewuski_kacper_s2527440;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
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

    public TrafficViewModel() {
        situationsData = new MutableLiveData<>();
        progressData = new MutableLiveData<>();
    }

    public LiveData<ArrayList<TrafficSituation>> getSituationsData() { return situationsData; }
    public LiveData<Integer> getProgressData() { return progressData; }

    public void fetchTrafficData(String myurl, String clientId, String clientKey) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                progressData.postValue(0);
                String result = "";
                BufferedReader in = null;
                try {
                    URL url = new URL(myurl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("Authorization", Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                    in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        result = result + inputLine;
                    }
                    in.close();
                    urlConnection.disconnect();
                } catch (IOException e) {
                    Log.e("Network", "Error fetching data");
                    return;
                }

                int i = result.indexOf("<?");
                if (i != -1) result = result.substring(i);

                ArrayList<TrafficSituation> situationsList = new ArrayList<>();
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(new StringReader(result));

                    int eventType = xpp.getEventType();
                    TrafficSituation currentSituation = null;
                    boolean isInsideComment = false;

                    int totalItems = result.split("<situationRecord").length - 1;
                    if (totalItems <= 0) totalItems = 1;
                    final int step = 100 / totalItems;
                    int currentProgress = 0;

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

                                currentProgress += step;
                                progressData.postValue(currentProgress); // Wysłanie postępu do widoku
                            }
                        }
                        eventType = xpp.next();
                    }
                    progressData.postValue(100);
                    situationsData.postValue(situationsList); // Wysłanie gotowej listy do widoku

                } catch (XmlPullParserException | IOException e) {
                    Log.e("Parsing", "Parsing error", e);
                }
            }
        }).start();
    }
}
