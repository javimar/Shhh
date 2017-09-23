package eu.javimar.shhh.background.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import eu.javimar.shhh.R;
import eu.javimar.shhh.util.NotificationUtils;

import static eu.javimar.shhh.util.HelperUtils.isDatabaseEmpty;


public class GeofenceReregistrationFirebaseJobService extends JobService
{
    private AsyncTask<Void, Void, Void> mBackgroundTask;
    private Context context = GeofenceReregistrationFirebaseJobService.this;
    /**
     * This is called by the Job Dispatcher to tell us we should start our job. Keep in mind this
     * method is run on the application's main thread, so we need to offload work to a background
     * thread.
     *
     * @return True if your service needs to process the work (on a separate thread).
     * False if there's no more work to be done for this job.
     */

    @Override
    public boolean onStartJob(final JobParameters job)
    {
Log.e("FirebaseJobService", " JAVIER START JOB" );

        mBackgroundTask = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                startService(new Intent(context, RegisterGeofencesIntentService.class));
                return null;
            }

            @Override
            protected void onPostExecute(Void param)
            {
                // Issue notification if is set in preferences
                SharedPreferences pref = PreferenceManager
                        .getDefaultSharedPreferences(context);

                // does user have notifications enabled?
                boolean wantNotif =
                        pref.getBoolean(context
                                .getString(R.string.pref_activate_notification_key), true);

                if(wantNotif && !isDatabaseEmpty(context))
                {
                    NotificationUtils.notify_geofences_loaded(context);
                }

                // Call this to inform the JobManager you've finished executing.
                // false means no rescheduling
                jobFinished(job, false);
            }
        };
        mBackgroundTask.execute();
        return true;
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
