package eu.javimar.shhh.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import eu.javimar.shhh.R;

public final class NotificationUtils
{
    // The id of the channel. Necessary for Android O
    public static final String GEO_CHANNEL_ID = "my_channel_for_geo_notifications";

    private static final int ACTION_DISMISS_NOTIFICATION_GEO_REGISTERING_PENDING_INTENT_ID = 72;
    public static final int GEO_REGISTERING_NOTIFICATION_ID = 69;

    /**
     * LOGIC RELATED TO NOTIFICATION OF GEOFENCES REGISTRATION
     */
    public static void notify_geofences_loaded(Context context)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GEO_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.materialRed200))
                .setSmallIcon(R.drawable.ic_ringer)
                .setLargeIcon(largeIcon(context))
                .setContentTitle(context.getString(R.string.notification_registration))
                .setContentText(context.getString(R.string.notification_nextrun))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.notification_nextrun)))
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        /* GEO_REGISTERING_NOTIFICATION_ID allows you to update or cancel the notification later on */
        notificationManager.notify(GEO_REGISTERING_NOTIFICATION_ID, builder.build());
    }


    private static Bitmap largeIcon(Context context)
    {
        Resources res = context.getResources();
        return BitmapFactory.decodeResource(res, R.drawable.ic_my_location);
    }

}
