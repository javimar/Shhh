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


    public String convertToSexagesimal(Context c)
    {
        // latitude
        int latSeconds = (int)round(latitude * 3600);
        int latDegrees = latSeconds / 3600;
        latSeconds = abs(latSeconds % 3600);
        int latMinutes = latSeconds / 60;
        latSeconds %= 60;
        // longitude
        int longSeconds = (int)round(longitude * 3600);
        int longDegrees = longSeconds / 3600;
        longSeconds = abs(longSeconds % 3600);
        int longMinutes = longSeconds / 60;
        longSeconds %= 60;
        // Cardinal points
        String longCardinal = (longitude > 0) ?
                c.getString(R.string.location_east) :
                c.getString(R.string.location_west);
        String latCardinal = (latitude > 0) ?
                c.getString(R.string.location_north) :
                c.getString(R.string.location_south);
        // return string formatted
        return  latCardinal + " " + latDegrees + "\u00B0" + " " +
                latMinutes + "\u2032 " + latSeconds + "\u2033\n" +
                longCardinal + " " + longDegrees + "\u00B0 " +
                longMinutes + "\u2032 " + longSeconds + "\u2033";
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
