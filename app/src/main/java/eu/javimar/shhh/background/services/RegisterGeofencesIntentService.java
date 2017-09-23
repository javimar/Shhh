package eu.javimar.shhh.background.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import eu.javimar.shhh.Geofencing;
import eu.javimar.shhh.model.GeoPoint;
import eu.javimar.shhh.model.PlaceContract.PlaceEntry;
import eu.javimar.shhh.model.PlaceObject;
import eu.javimar.shhh.util.MyEventNotification;

import static eu.javimar.shhh.MainActivity.sAreGeofencesEnabled;
import static eu.javimar.shhh.MainActivity.sPlaceList;
import static eu.javimar.shhh.util.PrefUtils.getGeofencesSwitchFromPreferences;


public class RegisterGeofencesIntentService extends IntentService
{
    private static final String TAG = RegisterGeofencesIntentService.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private Cursor cursor;

    public RegisterGeofencesIntentService()
    {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent)
    {
        if (intent == null) return; // exit

        // get all elements from the database
        cursor = getContentResolver().query(PlaceEntry.CONTENT_URI, null, null, null, null);

        if (cursor == null || (cursor.getCount() < 1)) // if empty, exit!
        {
            return;
        }

        // Build up the LocationServices API client
        googleApiClient= new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
                {
                    @Override
                    public void onConnected(@Nullable Bundle bundle)
                    {
                        loadDatabaseIntoListAndRegisterGeofences();
                    }

                    @Override
                    public void onConnectionSuspended(int i) { googleApiClient.connect(); }
                })
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .build();

        try
        {
            // connect the client only if we have permission
            if (!googleApiClient.isConnecting() || !googleApiClient.isConnected())
            {
                googleApiClient.connect();
            }
        }
        catch (SecurityException securityException)
        {
            // Catch exception generated if the app does not have ACCESS_FINE_LOCATION permission.
            Log.e(TAG, securityException.getMessage());
        }
    }

    private void loadDatabaseIntoListAndRegisterGeofences()
    {
        // reset and re-create
        sPlaceList = null;
        sPlaceList = new ArrayList<>();
        List<String> placeIds = new ArrayList<>();

        final Geofencing geofencing = new Geofencing(this, googleApiClient);

        cursor.moveToPosition(-1);
        while(cursor.moveToNext())
        {
            placeIds.add(cursor.getString(cursor.getColumnIndex(PlaceEntry.COLUMN_PLACE_ID)));
        }
        // free resources
        cursor.close();

Log.e(TAG, "JAVIER llamando a getPlaceById\n");

        // iterate all Places stored locally retrieving place fields from google server
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                .getPlaceById(googleApiClient,
                        placeIds.toArray(new String[placeIds.size()]));

        // populates the internal ArrayList of places (Master List)
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>()
        {
            @Override
            public void onResult(@NonNull PlaceBuffer places)
            {
                for (Place place : places) // build array of places
                {
                    sPlaceList.add(new PlaceObject(
                        place.getId(),
                        place.getName().toString(),
                        place.getAddress().toString(),
                        new GeoPoint(place.getLatLng().longitude, place.getLatLng().longitude)
                    ));
                }
                places.release();

                // Signal MainActivity, this IntentService has finished successfully
                EventBus.getDefault().post(new MyEventNotification(Activity.RESULT_OK));

                // retrieve geofences switch value from preferences. This is since if job is run "offline"
                // we need to update sAreGeofencesEnabled properly, otherwise it is not necessary
                sAreGeofencesEnabled = getGeofencesSwitchFromPreferences(RegisterGeofencesIntentService.this);
                // proceed with Geofences registration only if set.
                // We already checked if the cursor came empty
                if(sAreGeofencesEnabled)
                {
                    geofencing.updateGeofencesList();
                    geofencing.registerAllGeofences();
                }
            }
        });
    }
}
