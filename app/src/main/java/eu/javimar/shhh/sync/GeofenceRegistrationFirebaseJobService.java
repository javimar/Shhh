package eu.javimar.shhh.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.List;

import eu.javimar.shhh.Geofencing;
import eu.javimar.shhh.R;
import eu.javimar.shhh.model.PlaceContract;


public class GeofenceRegistrationFirebaseJobService extends JobService
{
    private AsyncTask mBackgroundTask;

    private Geofencing geofencing;
    private GoogleApiClient googleApiClient;


    /**
     * The entry point to the Job. Implementations should offload work to another thread of
     * execution as soon as possible.
     *
     * This is called by the Job Dispatcher to tell us we should start our job. Keep in mind this
     * method is run on the application's main thread, so we need to offload work to a background
     * thread.
     *
     * @return whether there is more work remaining.
     */
    @Override
    public boolean onStartJob(final JobParameters job)
    {
        mBackgroundTask = new AsyncTask<Object, Void, List<String>>()
        {
            @Override
            protected List<String> doInBackground(Object... params)
            {
                Context context = GeofenceRegistrationFirebaseJobService.this;

Log.e("JAVIER", "retrieving places");
                // get all places in the database
                Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
                Cursor cursor = context.getContentResolver().query(
                        uri,
                        null,
                        null,
                        null,
                        null);

                if (cursor == null || cursor.getCount() == 0) return null;

                List<String> placesIds = new ArrayList<>();

                cursor.moveToPosition(-1);
                while (cursor.moveToNext())
                {
                    placesIds.add(cursor.getString(cursor
                            .getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
                }
                cursor.close();
                return placesIds;
            }





            // we know job is finished when onPostExecute() happens!
            @Override
            protected void onPostExecute(List<String> placesIds)
            {
                Context context = GeofenceRegistrationFirebaseJobService.this;

                // Build up the LocationServices API client
                googleApiClient= new GoogleApiClient.Builder(context)
                        .addApi(LocationServices.API)
                        .addApi(Places.GEO_DATA_API)
                        .build();

                geofencing = new Geofencing(context, googleApiClient);

                PendingResult<PlaceBuffer> placeResult =
                        Places.GeoDataApi.getPlaceById(googleApiClient,
                                placesIds.toArray(new String[placesIds.size()]));

                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>()
                {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places)
                    {
Log.e("JAVIER", "CALLBACK= ");

                        geofencing.addUpdateGeofences(places);
                        geofencing.registerAllGeofences();
                    }
                });



                // when the job is finished, issue a notification if is set in preferences
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                // does user have notifications enabled?
                boolean wantNotif =
                        pref.getBoolean(context
                                .getString(R.string.pref_activate_notification_key), true);
                if(wantNotif)
                {
                    BackgroundTasks.executeTask(context,
                            BackgroundTasks.ACTION_NOTIFY_USER_GEOFENCES_REGISTERED);
                }



                /*
                 * Once the AsyncTask is finished, the job is finished. To inform JobManager that
                 * you're done, you call jobFinished with the jobParameters that were passed to your
                 * job and a boolean representing whether the job needs to be rescheduled. This is
                 * usually if something didn't work and you want the job to try running again.
                 */
                jobFinished(job, false);
            }
        };
        mBackgroundTask.execute();
        return true; // Answers the question: "Is there still work going on?"
    }

    /**
     * Called when the scheduling engine has decided to interrupt the execution of a running job,
     * most likely because the runtime constraints associated with the job are no longer satisfied.
     *
     * @return whether the job should be retried
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters)
    {
        if (mBackgroundTask != null) mBackgroundTask.cancel(true);

        return true; // Answers the question: "Should this job be retried?"
    }
}
