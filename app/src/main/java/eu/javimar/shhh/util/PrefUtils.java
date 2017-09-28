package eu.javimar.shhh.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import eu.javimar.shhh.R;

/**
 * Utilities class to access Shared Preferences
 */
public final class PrefUtils
{
    // final class
    private PrefUtils() {}

    /** Retrieve job scheduled value */
    public static boolean getIsJobScheduledFromPreferences(Context c)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c);
        return pref.getBoolean(c.getString(R.string.pref_jobscheduled_key), false);
    }

    /** Save job scheduled value */
    public static void putIsJobScheduledToPreferences(Context c, boolean value)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c);
        pref.edit().
                putBoolean(c.getString(R.string.pref_jobscheduled_key), value).apply();
    }

    /** Retrieve geofence radius entered by the user */
    public static float getGeofenceRadiusFromPreferences(Context c)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c);
        return Float.parseFloat(pref.getString(c.getString(R.string.pref_georadius_key), "50"));
    }


    public static boolean getGeofencesSwitchFromPreferences(Context context)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(context.getString(R.string.pref_activate_geofences_key), false);
    }

}
