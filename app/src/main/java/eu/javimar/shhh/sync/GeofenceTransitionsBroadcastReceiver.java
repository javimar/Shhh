package eu.javimar.shhh.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import eu.javimar.shhh.MainActivity;
import eu.javimar.shhh.R;

import static eu.javimar.shhh.util.NotificationUtils.GEO_CHANNEL_ID;


public class GeofenceTransitionsBroadcastReceiver extends BroadcastReceiver
{
    public static final String LOG_TAG = GeofenceTransitionsBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Get the Geofence Event from the Intent sent through
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        geofencingEvent.getTriggeringLocation().toString();


        if (geofencingEvent.hasError())
        {
            Log.e(LOG_TAG, String.format("Error code : %d", geofencingEvent.getErrorCode()));
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        // Check which transition type has triggered this event
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
        {
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        }
        else
        {
            // Log the error and leave
            Log.e(LOG_TAG, String.format("Unknown transition : %d", geofenceTransition));
            return;
        }
        // Send the notification
        sendNotification(context, geofenceTransition);
    }


    /**
     * Posts a notification in the notification bar when a transition is detected
     * Uses different icon drawables for different transition types
     * If the user clicks the notification, control goes to the MainActivity
     *
     * @param context        The calling context for building a task stack
     * @param transitionType The geofence transition type, can be Geofence.GEOFENCE_TRANSITION_ENTER
     *                       or Geofence.GEOFENCE_TRANSITION_EXIT
     */
    private void sendNotification(Context context, int transitionType)
    {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GEO_CHANNEL_ID);

        // Check the transition type to display the relevant icon image
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
        {
            builder.setSmallIcon(R.drawable.ic_volume_off)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_off))
                    .setColor(ContextCompat.getColor(context, R.color.lightBlue300))
                    .setContentTitle(context.getString(R.string.geofence_silent_mode));
        }
        else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            builder.setSmallIcon(R.drawable.ic_volume)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume))
                    .setColor(ContextCompat.getColor(context, R.color.greenAccent700))
                    .setContentTitle(context.getString(R.string.geofences_back_to_normal));
        }

        // Continue building the notification
        builder
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_VIBRATE |
                             Notification.DEFAULT_LIGHTS |
                             Notification.DEFAULT_SOUND)
                .setContentText(context.getString(R.string.geofences_touch_to_launch))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.geofences_touch_to_launch)))
                .setContentIntent(notificationPendingIntent);


        // Get an instance of the Notification manager
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }
        // Issue the notification
        nm.notify(0, builder.build());
    }

    /**
     * Changes the ringer mode on the device to either silent or back to normal
     */
    private void setRingerMode(Context context, int mode)
    {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Check for DND permissions for API 24+
        if (android.os.Build.VERSION.SDK_INT < 24 ||
                (android.os.Build.VERSION.SDK_INT >= 24 &&
                        !nm.isNotificationPolicyAccessGranted()))
        {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);
        }
    }
}
