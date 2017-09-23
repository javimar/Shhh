package eu.javimar.shhh.util;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import eu.javimar.shhh.R;
import eu.javimar.shhh.model.PlaceContract;


public final class HelperUtils
{
    private HelperUtils() {}

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean isGooglePlayServicesAvailable(Activity activity)
    {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS)
        {
            if (googleApiAvailability.isUserResolvableError(status))
            {
                googleApiAvailability.getErrorDialog(activity, status,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }


    /** Returns true if the network is connected or about to become available */
    public static boolean isNetworkAvailable(Context context)
    {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    /** Little method to display colorful snackbar messages */
    public static void showSnackbar (Context context, View view, String message)
    {
        Snackbar snack = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View sbview = snack.getView();
        sbview.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        TextView textView = sbview.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(context, R.color.lightBlue300));
        snack.show();
    }


    /**
     * Helper method to delete a Place
     */
    public static void deletePlaceFromDb(Context context, Uri uri)
    {
        // Only perform the delete if this is an existing place.
        if (uri != null)
        {
            // Call the ContentResolver to delete palce at the given content URI.
            // Pass in null for the selection and selection args because the mCuuri
            // content URI already identifies the place we want.
            int rowsDeleted = context
                    .getContentResolver()
                    .delete(uri, null, null);

            // Show a message depending on whether or not the delete was successful.
            if (rowsDeleted == 0)
            {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(context, context.getString(R.string.adapter_delete_failed),
                        Toast.LENGTH_SHORT).show();
            }
            else
            {
                // Otherwise, the delete was successful
                Toast.makeText(context, context.getString(R.string.adapter_delete_ok),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Returns true if database has no elements */
    public static boolean isDatabaseEmpty(Context context)
    {
        Cursor cursor = context.getContentResolver()
                .query(PlaceContract.PlaceEntry.CONTENT_URI, null, null, null, null);
        if (cursor == null || cursor.getCount() < 1)
        {
            return true;
        }
        else
        {
            cursor.close();
            return false;
        }
    }


    public static int convertDipsToPx(Context context, float dips)
    {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dips * density);
    }
}
