package eu.javimar.shhh.background.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import static eu.javimar.shhh.util.HelperUtils.isDatabaseEmpty;
import static eu.javimar.shhh.util.PrefUtils.getGeofencesSwitchFromPreferences;

public class ListenToBootCompletedBroadcastReceiver extends BroadcastReceiver
{
    private static final String LOG_TAG = ListenToBootCompletedBroadcastReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Geofences have to be activated
        if(!getGeofencesSwitchFromPreferences(context)) return;

        // At least one place must be in database
        if(isDatabaseEmpty(context)) return;

        // A BOOT COMPLETED must have happened.
        if(!TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) return;

        // NOW, we can continue
Log.e(LOG_TAG, "JAVIER intent= " + intent.getAction());




    }
}
