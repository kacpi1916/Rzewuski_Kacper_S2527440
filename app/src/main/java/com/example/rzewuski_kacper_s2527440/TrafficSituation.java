package com.example.rzewuski_kacper_s2527440;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrafficSituation implements Serializable {
    private String recordId;
    private String generalPublicComment;
    private double latitude;
    private double longitude;
    private String type;
    private String startTime;
    private String endTime;

    public TrafficSituation() {}

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getGeneralPublicComment() { return generalPublicComment; }
    public void setGeneralPublicComment(String generalPublicComment) { this.generalPublicComment = generalPublicComment; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // calculate duration in days from start and end dates
    public long getDurationDays() {
        if (startTime == null || endTime == null || startTime.length() < 10 || endTime.length() < 10) {
            return -1;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            Date start = sdf.parse(startTime.substring(0, 10));
            Date end = sdf.parse(endTime.substring(0, 10));
            if (start != null && end != null) {
                long diffMillis = end.getTime() - start.getTime();
                long days = diffMillis / (1000L * 60 * 60 * 24);
                return days < 0 ? 0 : days;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
    // extract YYYY-MM-DD date string
    public String getFormattedDate(String rawDate) {
        if (rawDate == null || rawDate.length() < 10) {
            return "N/A";
        }
        return rawDate.substring(0, 10);
    }
}
