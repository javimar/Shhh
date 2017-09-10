package eu.javimar.shhh.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import eu.javimar.shhh.R;


public class GeofenceRegistrationFirebaseJobService extends JobService
{
    private static final String LOG_TAG =
            GeofenceRegistrationFirebaseJobService.class.getSimpleName();

    private AsyncTask mBackgroundTask;

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
        mBackgroundTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params)
            {
                Context context = GeofenceRegistrationFirebaseJobService.this;
                //LoadingTasks.executeTask(context, LoadingTasks.ACTION_LOAD_EVENTS, null);
                return null;
            }

            // we know job is finished when onPostExecute() happens!
            @Override
            protected void onPostExecute(Object o)
            {
                Context context = GeofenceRegistrationFirebaseJobService.this;

                // when the job is finished, issue a notification if is set in preferences
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                /*boolean wantNotif =
                        pref.getBoolean(context.getString(R.string.pref_loading_alerts_key), true);
                if(wantNotif)
                {
                    LoadingTasks.executeTask(context, LoadingTasks.ACTION_NOTIFY_USER_EVENTS_LOADED, null);
                }*/

                /*
                 * Once the AsyncTask is finished, the job is finished. To inform JobManager that
                 * you're done, you call jobFinished with the jobParamters that were passed to your
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
