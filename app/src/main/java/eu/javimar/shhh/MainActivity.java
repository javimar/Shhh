/**
 * Shhh!
 *
 * @author Javier Martín
 * @email: javimardeveloper@gmail.com
 * @link http://www.javimar.eu
 * @package eu.javimar.shhh
 * @version 1
 *
BSD 3-Clause License

Copyright (c) 2017, JaviMar
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

 * Neither the name of the copyright holder nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/
package eu.javimar.shhh;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import eu.javimar.shhh.background.services.RegisterGeofencesIntentService;
import eu.javimar.shhh.model.GeoPoint;
import eu.javimar.shhh.model.PlaceContract.PlaceEntry;
import eu.javimar.shhh.model.PlaceObject;
import eu.javimar.shhh.background.RegisterGeofencesJob;
import eu.javimar.shhh.util.MyEventBusNotification;
import eu.javimar.shhh.util.SwipeUtils;
import eu.javimar.shhh.view.AboutActivity;
import eu.javimar.shhh.view.PlaceAdapter;
import eu.javimar.shhh.view.SettingsActivity;

import static eu.javimar.shhh.model.PlaceContract.CONTENT_AUTHORITY;
import static eu.javimar.shhh.util.HelperUtils.PLAY_SERVICES_RESOLUTION_REQUEST;
import static eu.javimar.shhh.util.HelperUtils.convertDipsToPx;
import static eu.javimar.shhh.util.HelperUtils.isDatabaseEmpty;
import static eu.javimar.shhh.util.HelperUtils.isGooglePlayServicesAvailable;
import static eu.javimar.shhh.util.HelperUtils.isNetworkAvailable;
import static eu.javimar.shhh.util.HelperUtils.showSnackbar;
import static eu.javimar.shhh.util.PrefUtils.getGeofencesSwitchFromPreferences;
import static eu.javimar.shhh.util.PrefUtils.getIsJobScheduledFromPreferences;
import static eu.javimar.shhh.util.PrefUtils.putIsJobScheduledToPreferences;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION_FINE = 0;
    private static final int REQUEST_LOCATION_PERMISSION_COARSE = 1;
    private static final int PLACE_PICKER_REQUEST = 2;

    public static boolean sHaveLocationPermission = false;
    private static boolean sGeoPrefChange = false;
    private static boolean sOptionsItemSelectedOpened = false;

    // Views
    @BindView(R.id.fab) FloatingActionButton mFabAddPlace;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.rv_places) RecyclerView mRecyclerViewPlaces;
    @BindView(R.id.tv_empty_view)TextView mEmptyView;
    @BindView(R.id.iv_empty_image)ImageView mEmptyImage;
    @BindView(R.id.tv_no_connection_view)TextView mNoConnection;
    @BindView(R.id.iv_image_header)ImageView mHeaderImage;
    @BindView(R.id.collapsing_toolbar)CollapsingToolbarLayout mCollapsingToolbarLayout;
    @BindView(R.id.appbarlayout)AppBarLayout mAppBarLayout;

    // Save the recycler position on screen orientation and when coming back
    private static int sLastFirstVisiblePosition;

    private PlaceAdapter placeAdapter;

    // Google API location variable
    GoogleApiClient mGoogleApiClient;

    // Instance of the Geofencing class
    Geofencing mGeofencing;
    // Allows to keep track of the Preferences to enable or disable geofences
    public static boolean sAreGeofencesEnabled;

    // Master list of Places
    public static List<PlaceObject> sPlaceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // check if Google Play Services is installed
        isGooglePlayServicesAvailable(MainActivity.this);

        // manage control permissions for location services
        askForLocationPermission();

        // Build up the LocationServices API client
        mGoogleApiClient= new GoogleApiClient.Builder(MainActivity.this)
                .addConnectionCallbacks(MainActivity.this)
                .addOnConnectionFailedListener(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .build();

        // instantiate Geofences class, and pass the GoogleApiClient
        mGeofencing = new Geofencing(this, mGoogleApiClient);

        // retrieve geofences switch value from preferences (enabled/disabled)
        sAreGeofencesEnabled = getGeofencesSwitchFromPreferences(this);

        // add support for preferences changes callback
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);


        // Finally set the normal theme
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Toolbar logic, hide CollapsingToolbar title in favor of Toolbar
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(R.string.app_name);
        mCollapsingToolbarLayout.setTitleEnabled(false);

        // set RecyclerView and Adapter
        mRecyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewPlaces.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
        placeAdapter = new PlaceAdapter(this, mGeofencing);
        mRecyclerViewPlaces.setAdapter(placeAdapter);

        // set FAB button functionality
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

        // allow swipe functionality to delete items
        setSwipeForRecyclerView();
    }


    private void loadGeofencesInScreen()
    {
        // This IntentService will post an event in the bus when done
        startService(new Intent(this, RegisterGeofencesIntentService.class));
    }


    /** EventBus Subscription */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventNotification(MyEventBusNotification myEventNotification)
    {
        if(myEventNotification.getmResult() == Activity.RESULT_OK)
        {
            toggleVisibilityRecyclerEmptyViewOrNoConnection();
            placeAdapter.notifyDataSetChanged();
        }
    }

    private void toggleVisibilityRecyclerEmptyViewOrNoConnection()
    {
        if (!isNetworkAvailable(MainActivity.this))
        {
            mNoConnection.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mEmptyImage.setVisibility(View.GONE);
        }
        else if (sPlaceList.isEmpty())
        {
            mEmptyView.setVisibility(View.VISIBLE);
            mEmptyImage.setVisibility(View.VISIBLE);
            mNoConnection.setVisibility(View.GONE);
        }
        else
        {
            mEmptyView.setVisibility(View.GONE);
            mEmptyImage.setVisibility(View.GONE);
            mNoConnection.setVisibility(View.GONE);
        }
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
            sAreGeofencesEnabled = getGeofencesSwitchFromPreferences(this);

            if(sAreGeofencesEnabled && !isDatabaseEmpty(this))
            {
                // schedule re-registration of geofences once they expire
                if(!getIsJobScheduledFromPreferences(this))
                {
                    RegisterGeofencesJob.scheduleRegisteringGeofences(this);
                    // save in preferences that job is scheduled
                    putIsJobScheduledToPreferences(this, true);
                }
            }
            else
            {
                // cancel all jobs
                RegisterGeofencesJob.cancelAllJobs(this);
                // save in preferences that job is not scheduled
                putIsJobScheduledToPreferences(this, false);
            }
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
            if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected())
            {
                mGoogleApiClient.connect();
            }
        }

        if (sGeoPrefChange)
        {
            if (sAreGeofencesEnabled)
            {
                mGeofencing.updateGeofencesList();
                mGeofencing.registerAllGeofences();
                showSnackbar(this, mToolbar, getString(R.string.geofences_enabled));
            }
            else
            {
                mGeofencing.unRegisterAllGeofences();
                showSnackbar(this, mToolbar, getString(R.string.geofences_disabled));
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
            // donn't disconnect the client when accessing the menu
            if(sOptionsItemSelectedOpened)
            {
                // reset boolean
                sOptionsItemSelectedOpened = false;
            }
            else
            {
                if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected())
                {
                    mGoogleApiClient.disconnect();
                }
            }
        }
    }


    @Override
    public void onResume()
    {
        // Save and restore position of the RecyclerView
        super.onResume();

        // Subscribe to events
        EventBus.getDefault().register(this);

        mRecyclerViewPlaces.getLayoutManager().scrollToPosition(sLastFirstVisiblePosition);

        if(sAreGeofencesEnabled) mToolbar.setLogo(R.drawable.ic_volume_off);
        else mToolbar.setLogo(R.drawable.ic_volume);

        toggleVisibilityRecyclerEmptyViewOrNoConnection();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // Unsubscribe from events
        EventBus.getDefault().unregister(this);

        sLastFirstVisiblePosition = ((LinearLayoutManager) mRecyclerViewPlaces
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
                sOptionsItemSelectedOpened = true;
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_scoreapp:
                sOptionsItemSelectedOpened = true;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + CONTENT_AUTHORITY));
                startActivity(intent);
                return true;

            case R.id.action_about:
                sOptionsItemSelectedOpened = true;
                startActivity(new Intent(this, AboutActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Connection callbacks */
    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        if(sPlaceList.size() < 1)
        {
            // Load all data on screen
            loadGeofencesInScreen();
        }
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
                    String placeName = place.getName().toString();
                    String placeAddress = place.getAddress().toString();
                    LatLng coordinates = place.getLatLng();

                    // Insert a new place into DB. Can store only ID per Google's policy
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(PlaceEntry.COLUMN_PLACE_ID, placeID);
                    getContentResolver().insert(PlaceEntry.CONTENT_URI, contentValues);

                    // refresh Master List to match DB
                    sPlaceList.add(new PlaceObject(placeID, placeName, placeAddress,
                            new GeoPoint(coordinates.longitude, coordinates.latitude)));

                    // solve for empty view
                    toggleVisibilityRecyclerEmptyViewOrNoConnection();
                    // refresh RecyclerView to match DB
                    placeAdapter.notifyDataSetChanged();

                    // register if is set in preferences, we save calls to getPlaceById
                    // no need to pass through RegisterGeofencesIntentService
                    if(sAreGeofencesEnabled)
                    {
                        mGeofencing.updateGeofencesList();
                        mGeofencing.registerAllGeofences();
                    }
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        // Gets the layout params that will allow you to resize the layout
        ViewGroup.LayoutParams params = mAppBarLayout.getLayoutParams();

        //check config
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            mHeaderImage.setVisibility(View.GONE);
            // Changes the height and width to the specified *pixels*
            params.height = convertDipsToPx(this, 128);
        }
        else
        {
            mHeaderImage.setVisibility(View.VISIBLE);
            params.height = convertDipsToPx(this, 256);
        }
        mAppBarLayout.setLayoutParams(params);
        toggleVisibilityRecyclerEmptyViewOrNoConnection();
    }


    private void setSwipeForRecyclerView()
    {
        SwipeUtils swipeHelper = new SwipeUtils(0, ItemTouchHelper.LEFT, this)
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
        mItemTouchHelper.attachToRecyclerView(mRecyclerViewPlaces);

        // set swipe label
        swipeHelper.setLeftSwipeLabel(getString(R.string.deleteString));
        // set swipe background-Color
        swipeHelper.setLeftcolorCode(ContextCompat.getColor(this, R.color.materialRed200));
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
