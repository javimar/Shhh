package eu.javimar.shhh;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import eu.javimar.shhh.model.PlaceContract.PlaceEntry;
import eu.javimar.shhh.util.SwipeUtil;
import eu.javimar.shhh.view.AboutActivity;
import eu.javimar.shhh.view.PlaceAdapter;

import static eu.javimar.shhh.util.HelperUtils.PLAY_SERVICES_RESOLUTION_REQUEST;
import static eu.javimar.shhh.util.HelperUtils.getGeofencesSwitchFromPreferences;
import static eu.javimar.shhh.util.HelperUtils.isGooglePlayServicesAvailable;
import static eu.javimar.shhh.util.HelperUtils.isNetworkAvailable;
import static eu.javimar.shhh.util.HelperUtils.showSnackbar;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<Cursor>
{
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION_FINE = 0;
    private static final int REQUEST_LOCATION_PERMISSION_COARSE = 1;
    private static final int PLACE_PICKER_REQUEST = 3;
    private static final int PLACES_DB_LOADER = 5;

    public static boolean sHaveLocationPermission = false;
    private static boolean sGeoPrefChange = false;
    private static boolean sPreferencesOpened = false;

    // Views
    @BindView(R.id.fab) FloatingActionButton mFabAddPlace;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.rv_places) RecyclerView mRecyclerViewPlace;
    @BindView(R.id.tv_empty_view)TextView mEmptyView;

    // Save the recycler position on screen orientation and when coming back
    private static int sLastFirstVisiblePosition;

    private PlaceAdapter placeAdapter;

    // Google API location variable
    GoogleApiClient mGoogleApiClient;
    // Instance of the Geofencing class
    Geofencing mGeofencing;
    // Allows to keep track of the Preferences for enabling or disabling geofences
    private boolean areGeofencesEnabled;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mToolbar.setTitle(R.string.app_name);

        // set recycler view
        mRecyclerViewPlace.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewPlace.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
        placeAdapter = new PlaceAdapter(this, null);
        mRecyclerViewPlace.setAdapter(placeAdapter);

        // check if Google Play Services is installed
        isGooglePlayServicesAvailable(this);

        // manage control permissions for location services
        askForLocationPermission();

        // Build up the LocationServices API client
        // Also uses enableAutoManage to automatically when to connect/suspend the client
        mGoogleApiClient= new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                //.enableAutoManage(this, this)
                .build();

        // instantiate the Geofences class, passing the GoogleApiClient
        mGeofencing = new Geofencing(this, mGoogleApiClient);

        // retrieve geofences switch value from preferences (enabled/disabled)
        areGeofencesEnabled = getGeofencesSwitchFromPreferences(this);

        mFabAddPlace.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (isNetworkAvailable(MainActivity.this))
                {
                    addNewPlace();
                }
                else
                {
                    showSnackbar(MainActivity.this, mToolbar,
                            getString(R.string.no_internet_connection));
                }
            }
        });

        // add support for preferences changes callback
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        // schedule re-registration of geofences once they expire
        //RegisterGeofencesJob.scheduleRegisteringGeofences(this);

        // allow swipe functionality to delete items
        setSwipeForRecyclerView();
    }


    // LOADER LOGIC
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
    {
        switch (id)
        {
            case PLACES_DB_LOADER:
                return new CursorLoader(
                        this,
                        PlaceEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        if ((cursor == null || cursor.getCount() < 1))
        {
            placeAdapter.swapPlaces(null);
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }
        else
        {
            mEmptyView.setVisibility(View.GONE);
        }
        switch (loader.getId())
        {
            case PLACES_DB_LOADER:

                List<String> placeIds = new ArrayList<>();
                while (cursor.moveToNext())
                {
                    placeIds.add(cursor.getString(cursor.getColumnIndex(PlaceEntry.COLUMN_PLACE_ID)));
                }

                // iterate all Places stored locally retrieving names from google server
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient,
                        placeIds.toArray(new String[placeIds.size()]));

                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>()
                {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places)
                    {
                        placeAdapter.swapPlaces(places);
                        mGeofencing.addUpdateGeofences(places);
                        if(areGeofencesEnabled) mGeofencing.registerAllGeofences();
                    }
                });
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        placeAdapter.swapPlaces(null);
        mRecyclerViewPlace.invalidate();
    }


    /** Any changes in preferences will trigger this method */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key)
    {
        // check which preference trigger the listener
        if(key.equals(getString(R.string.pref_activate_geofences_key)))
        {
            sGeoPrefChange = true;
            // get the value of the preferences changed
            areGeofencesEnabled = pref.getBoolean(
                    getString(R.string.pref_activate_geofences_key), false);
        }
    }


    @Override
    public void onStart()
    {
        super.onStart();
        isGooglePlayServicesAvailable(this);

        if (!isNetworkAvailable(this))
        {
            showSnackbar(this, findViewById(android.R.id.content),
                    getString(R.string.no_internet_connection));
            return;
        }

        if (sHaveLocationPermission)
        {
            // connect the client only if we have permission
            if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected())
            {
                mGoogleApiClient.connect();
            }
        }

        if (sGeoPrefChange)
        {
            if (areGeofencesEnabled)
            {
                mGeofencing.registerAllGeofences();
                showSnackbar(this, mToolbar, getString(R.string.geofences_enabled));
                mToolbar.setLogo(R.drawable.ic_volume);
            }
            else
            {
                mGeofencing.unRegisterAllGeofences();
                showSnackbar(this, mToolbar, getString(R.string.geofences_disabled));
                mToolbar.setLogo(R.drawable.ic_volume_off);
            }
            sGeoPrefChange = false;
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (sHaveLocationPermission)
        {
            // disconnect the client only if we are not opening preferences
            if(sPreferencesOpened)
            {
                // reset boolean
                sPreferencesOpened = false;
            }
            else
            {
                if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected())
                {

Log.e(LOG_TAG, "JAVIER disconnecting....");

                    mGoogleApiClient.disconnect();
                }
            }
        }
    }


    private void setSwipeForRecyclerView()
    {
        SwipeUtil swipeHelper = new SwipeUtil(0, ItemTouchHelper.LEFT, this)
        {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
            {
                int swipedPosition = viewHolder.getAdapterPosition();
                placeAdapter.pendingRemoval(swipedPosition);
            }
            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
            {
                int position = viewHolder.getAdapterPosition();
                if (placeAdapter.isPendingRemoval(position))
                {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(swipeHelper);
        mItemTouchHelper.attachToRecyclerView(mRecyclerViewPlace);

        // set swipe label
        swipeHelper.setLeftSwipeLabel(getString(R.string.deleteString));
        // set swipe background-Color
        swipeHelper.setLeftcolorCode(ContextCompat.getColor(this, R.color.materialRed200));
    }


    @Override
    public void onResume()
    {
        // Save and restore position of the RecyclerView
        super.onResume();
        mRecyclerViewPlace.getLayoutManager().scrollToPosition(sLastFirstVisiblePosition);

        if(areGeofencesEnabled) mToolbar.setLogo(R.drawable.ic_volume);
        else mToolbar.setLogo(R.drawable.ic_volume_off);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        sLastFirstVisiblePosition = ((LinearLayoutManager)mRecyclerViewPlace
                .getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                sPreferencesOpened = true;
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Connection callbacks */
    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        getLoaderManager().restartLoader(PLACES_DB_LOADER, null, this);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result)
    {
        Log.i(LOG_TAG, "Connection has failed " + result.getErrorCode());
    }


    /** LOCATION PERMISSION */
    private void askForLocationPermission()
    {
        // if we don't have the permission to access fine location
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // true if app asks permission and user rejects request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {
                // we show an explanation why it is needed
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_location,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_LOCATION_PERMISSION_FINE);
                            }
                        }).show();
            }
            else
            {
                // no explanation needed, request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION_FINE);
            }
        }
        else
        {
            sHaveLocationPermission = true;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_COARSE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_LOCATION_PERMISSION_FINE:
                // If request is cancelled, the result arrays are empty.
                // permission granted
                // permission denied, disable functionality that depends on this permission.
                sHaveLocationPermission = grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;

            case REQUEST_LOCATION_PERMISSION_COARSE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission granted
                    sHaveLocationPermission = true;
                }
                break;
            // other 'case' lines to check for other permissions this app might request
        }
    }


    private void addNewPlace()
    {
        try
        {
            // Start a new Activity for the Place Picker API, this will trigger
            // onActivityResult when a place is selected or with the user cancels.
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            Intent i = builder.build(this);
            startActivityForResult(i, PLACE_PICKER_REQUEST);
        }
        catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e)
        {
            Log.e(LOG_TAG, String.format("GooglePlayServices not Available [%s]", e.getMessage()));
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // comes from asking user to install GooglePlayServices
        switch (requestCode)
        {
            case PLAY_SERVICES_RESOLUTION_REQUEST:
                if (resultCode == RESULT_OK)
                {
                    // Make sure the app is not already connected or attempting to connect
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected())
                    {
                        mGoogleApiClient.connect();
                    }
                }
                else if (resultCode == RESULT_CANCELED)
                {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.google_play_error),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction("OK", new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    finish();
                                }
                            }).show();
                }
                break;

            case PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK)
                {
                    Place place = PlacePicker.getPlace(this, data);
                    if (place == null)
                    {
                        Log.i(LOG_TAG, "No place selected");
                        return;
                    }

                    // Extract the place information from the API
                    String placeID = place.getId();

                    // Insert a new place into DB
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(PlaceEntry.COLUMN_PLACE_ID, placeID);
                    getContentResolver().insert(PlaceEntry.CONTENT_URI, contentValues);
                }
                break;
        }
    }


    @Override
    public void onDestroy()
    {

        super.onDestroy();
        if (sHaveLocationPermission)
        {
            // disconnect the client if it was connected
            if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected())
            {
                mGoogleApiClient.disconnect();
            }
        }
    }
}
