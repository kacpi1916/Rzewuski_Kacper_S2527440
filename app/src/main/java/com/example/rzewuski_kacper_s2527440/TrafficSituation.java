package com.example.rzewuski_kacper_s2527440;

import java.io.Serializable;

public class TrafficSituation implements Serializable {
    private String recordId;
    private String generalPublicComment;

    private double latitude;
    private double longitude;
    private String type;

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
}
