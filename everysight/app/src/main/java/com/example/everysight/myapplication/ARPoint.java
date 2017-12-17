package com.example.everysight.myapplication;

import android.location.Location;

/**
 * Created by ntdat on 1/16/17.
 */

public class ARPoint {
    Location location;
    String name;
    Boolean isRelative;
    public ARPoint(String name, double lat, double lon, double altitude,boolean isRelative) {
        this.name = name;
        location = new Location("ARPoint");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(altitude);
        this.isRelative = isRelative;
    }
    public float[] pointInENU(Location currentLocation, float[] ecefCurrentLocation){
        float[] pointInECEF = new float[4];
        if(isRelative){
            pointInECEF[0] = ecefCurrentLocation[0] + (float) (location.getLatitude());
            pointInECEF[1] = ecefCurrentLocation[1] + (float) (location.getLongitude());
            pointInECEF[2] = ecefCurrentLocation[2] + (float) (location.getAltitude());
        }else {
            pointInECEF = LocationHelper.WSG84toECEF(location);
        }
        return LocationHelper.ECEFtoENU(currentLocation, ecefCurrentLocation, pointInECEF);
    }
    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
