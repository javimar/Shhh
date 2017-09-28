package eu.javimar.shhh.model;

import android.content.Context;

import eu.javimar.shhh.R;

import static java.lang.Math.abs;
import static java.lang.Math.round;


@SuppressWarnings("all")
public class GeoPoint
{
    private double longitude, latitude;

    public GeoPoint(double longitude, double latitude)
    {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /** Necessary methods to support GeoPoint as a HashMap key to reflect "equality" of two objects.*/
    @Override
    public int hashCode()
    {
        return (int)((abs(longitude + latitude)));
    }

    @Override
    public boolean equals(Object geo)
    {
        if(!(geo instanceof GeoPoint))
            return false;

        GeoPoint g = (GeoPoint) geo;
        return (g.latitude == this.latitude) && (g.longitude == this.longitude);
    }

    // GETTERS and SETTERS
    public double getLongitude() {return longitude;}
    public void setLongitude(double longitude) {this.longitude = longitude;}
    public double getLatitude() {return latitude;}
    public void setLatitude(double latitude) {this.latitude = latitude;}

}
