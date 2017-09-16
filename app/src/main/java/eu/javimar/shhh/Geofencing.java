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
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

import eu.javimar.shhh.sync.GeofenceTransitionsBroadcastReceiver;

@SuppressWarnings("all")
/**
 * To build GEOFENCES, several objects are needed:
 *
 * 1. GEOFENCES object list --> from the PlaceBuffer sent to addUpdateGeofences() method
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
    private static final float GEOFENCE_RADIUS = 75f; // in meters
    //private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours in millis
    private static final long GEOFENCE_TIMEOUT = 10 * 60 * 1000; // 10'

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
Log.e(LOG_TAG, "JAVIER REGISTERING ME PIRO size= " + mGeofenceList.size());
Log.e(LOG_TAG, "JAVIER REGISTERING ME PIRO API= " + mGoogleApiClient);
            return;
        }
        try
        {
Log.e(LOG_TAG, "JAVIER REGISTERING GEOFENCES\n");

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
Log.e(LOG_TAG, "JAVIER UNREGISTERING GEOFENCES\n");
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
     * Given a PlaceBuffer instance, go through all places in it, creating a GEOFENCE instance
     * which gets added to a list mGeofenceList
     */
    public void addUpdateGeofences(PlaceBuffer places)
    {
        mGeofenceList = new ArrayList<>();
        if (places == null || places.getCount() == 0) return;

        for (Place place : places)
        {
            // Read the place information from the DB cursor
            String placeUID = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLng = place.getLatLng().longitude;

            // Build a GEOFENCE object like this
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID) // we can use this ID to ensure uniqueness
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLng, GEOFENCE_RADIUS)
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
        // what happens when the device is already inside. INITIAL_TRIGGER_ENTER sets it immediately
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);


Log.e(LOG_TAG, "JAVIER getGeofencingRequest mGeofenceList size= " + mGeofenceList.size());

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


Log.e(LOG_TAG, "JAVIER STATUS= " + result.getStatus().getStatus().toString());



        if(!result.getStatus().isSuccess())
        {
            Log.e(LOG_TAG, String.format("Error adding/removing geofence : %s",
                    result.getStatus().toString()));
        }

    }

}

