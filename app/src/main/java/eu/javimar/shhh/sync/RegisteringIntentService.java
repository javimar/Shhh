package eu.javimar.shhh.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;


public class RegisteringIntentService extends IntentService
{
    private static final String TAG = RegisteringIntentService.class.getSimpleName();

    public RegisteringIntentService()
    {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent)
    {
        if (intent != null) {
            BackgroundTasks.executeTask(this, intent.getAction());
        }
    }
}
