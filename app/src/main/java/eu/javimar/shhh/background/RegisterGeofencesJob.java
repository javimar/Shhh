package eu.javimar.shhh.background;

import android.content.Context;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import java.util.concurrent.TimeUnit;

import eu.javimar.shhh.background.services.GeofenceReregistrationFirebaseJobService;


@SuppressWarnings("all")
public class RegisterGeofencesJob
{
    // Interval at which to re-register geofences
    private static final int INTERVAL_MINUTES = 60 * 24; // every 24
    private static final int INTERVAL_SECONDS =
            (int)(TimeUnit.MINUTES.toSeconds(INTERVAL_MINUTES));
    private static final int SYNC_FLEXTIME_SECONDS = INTERVAL_SECONDS;
    private static final String GEOFENCES_JOB_TAG = "geofence_registration_job_tag";

    private static boolean sInitialized = false;

    public synchronized static void scheduleRegisteringGeofences(@NonNull final Context context)
    {
        if (sInitialized) return;

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        // Create the Job to periodically re-register geofences since these have an expiration date
        Job constraintReminderJob = dispatcher.newJobBuilder()

                .setService(GeofenceReregistrationFirebaseJobService.class)

                // Set the UNIQUE tag used to identify this Job.
                .setTag(GEOFENCES_JOB_TAG)

                /*
                 * Network constraints on which this Job should run. In this app, we're using the
                 * device charging constraint so that the job executes in all networks
                 */
                .setConstraints(Constraint.ON_ANY_NETWORK)

                /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.FOREVER)

                // We want this to continuously happen, so we tell this Job to recur.
                .setRecurring(true)

                /*
                 * We want this to happen every INTERVAL_MINUTES minutes or so. The first argument for
                 * Trigger class's static executionWindow method is the start of the time frame
                 * when the job should be performed. The second argument is the latest point in time at
                 * which the data should be synch'ed. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off.
                 */
                .setTrigger(Trigger.executionWindow(INTERVAL_SECONDS, SYNC_FLEXTIME_SECONDS))

                /*
                 * If a Job with the tag provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)

                // Once the Job is ready, call the builder's build method to return the Job
                .build();

        // Schedule the Job with the dispatcher
        dispatcher.schedule(constraintReminderJob);

        // Mark job initialized
        sInitialized = true;
    }


    /** Will get called when user disables geofences in preferences */
    public static void cancelAllJobs(Context context)
    {
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        dispatcher.cancelAll();
    }
}
