package eu.javimar.shhh;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import eu.javimar.shhh.background.receivers.GeofenceTransitionsBroadcastReceiver;
import eu.javimar.shhh.model.PlaceObject;

import static eu.javimar.shhh.MainActivity.sPlaceList;
import static eu.javimar.shhh.util.PrefUtils.getGeofenceRadiusFromPreferences;

@SuppressWarnings("all")
/**
 * To build GEOFENCES, several objects are needed:
 *
 * 1. GEOFENCES object list --> from the PlaceBuffer sent to updateGeofencesList() method
 * 2. GEOFENCE Request --> getGeofencingRequest() method
 * 3. GoogleApiClient --> from the constructor
 * 4. PendingIntent --> getGeofencePendingIntent() method
 * 5. BroadcastReceiver or IntentService, or some other process to act when a geofence triggers
 *
 */
public class Geofencing implements ResultCallback
{
    // Constants
    public static final String LOG_TAG = Geofencing.class.getSimpleName();
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours in millis

    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    public Geofencing(Context context, GoogleApiClient client)
    {
        mContext = context;
        mGoogleApiClient = client;
        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<>();
    }

    /**
     * Registers the list of Geofences specified in mGeofenceList with Google Place Services
     * mGoogleApiClient connects to Google Place Services
     * getGeofencingRequest() gets the list of Geofences to be registered
     * getGeofencePendingIntent() gets the pending intent to launch the IntentService
     * when the Geofence is triggered
     * Triggers onResult(Result) when the geofences have been registered successfully
     */
    public void registerAllGeofences()
    {
        // Check that the API client is connected and that the list has Geofences in it
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected() ||
                mGeofenceList == null || mGeofenceList.size() == 0)
        {
            return;
        }

        try
        {
            // Add the ApiClient, the request and the pending intent
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        }
        catch (SecurityException securityException)
        {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e(LOG_TAG, securityException.getMessage());
        }
    }

    // Unregisters all the Geofences created by this app from Google Place Services
    public void unRegisterAllGeofences()
    {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected())
        {
            return;
        }
        try
        {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // same pending intent that was used in registerGeofences
                    // it doesn't need the getGeofencingRequest as registerAllGeofences does
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        }
        catch (SecurityException securityException)
        {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e(LOG_TAG, securityException.getMessage());
        }
    }


    /**
     * Go through all places in master list, creating a GEOFENCE instance
     * which gets added to a list mGeofenceList
     */
    public void updateGeofencesList()
    {
        mGeofenceList = new ArrayList<>();
        float geofenceRadius = getGeofenceRadiusFromPreferences(mContext);

        if (sPlaceList == null || sPlaceList.size() < 1) return;

        // iterate through master array of Places
        for (PlaceObject place : sPlaceList)
        {
            // Read the place information from the array
            String placeUID = place.getPlaceId();
            double placeLat = place.getCoordinates().getLatitude();
            double placeLng = place.getCoordinates().getLongitude();

            // Build a GEOFENCE object like this
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID) // we can use this ID to ensure uniqueness
                    // give them an expiration time
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLng, geofenceRadius)
                    // Sets the delay between GEOFENCE_TRANSITION_ENTER and GEOFENCE_TRANSITION_DWELLING
                    //.setLoiteringDelay(60000) // 1 minute
                    // transitions of interest
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            // Add it to the list
            mGeofenceList.add(geofence);
        }
    }

    /**
     * Builds a GeofencingRequest object using the mGeofenceList ArrayList of Geofences
     * Used by {@code #registerGeofences}
     *
     * @return the GeofencingRequest object
     */
    private GeofencingRequest getGeofencingRequest()
    {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // when building a GeofencingRequest you need to specify an initial trigger that specifies
        // what happens when the device is already inside.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    /**
     *  Specify which intent to launch when the geofence entry or exit event, triggers.
     */
    private PendingIntent getGeofencePendingIntent()
    {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null)
        {
            return mGeofencePendingIntent;
        }

        // FLAG_UPDATE_CURRENT allows for reusing this Intent so no need to create new one
        Intent intent = new Intent(mContext, GeofenceTransitionsBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result)
    {
        if(!result.getStatus().isSuccess())
        {
            Log.e(LOG_TAG, String.format("Error adding/removing geofence : %s",
                    result.getStatus().toString()));
        }
    }
}

