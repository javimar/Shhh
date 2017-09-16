package eu.javimar.shhh.sync;

import android.content.Context;
import eu.javimar.shhh.util.NotificationUtils;

import static eu.javimar.shhh.util.NotificationUtils.GEO_REGISTERING_NOTIFICATION_ID;


@SuppressWarnings("all")
public class BackgroundTasks
{
    /** ACTIONS TO PERFORM */
    public static final String ACTION_NOTIFY_USER_GEOFENCES_REGISTERED = "notify_geo_registered";
    public static final String ACTION_DISMISS_NOTIFICATION_GEO_REGISTRATION =
            "dismiss_notification_geofences_registration";


    public static void executeTask(Context context, String action)
    {
        switch (action)
        {

            case ACTION_NOTIFY_USER_GEOFENCES_REGISTERED:
                NotificationUtils.notify_geofences_loaded(context);
                break;

            case ACTION_DISMISS_NOTIFICATION_GEO_REGISTRATION:
                NotificationUtils.clearNotification(context, GEO_REGISTERING_NOTIFICATION_ID);
                break;
        }
    }
}
