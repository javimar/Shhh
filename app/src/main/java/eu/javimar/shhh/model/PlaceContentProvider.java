package eu.javimar.shhh.model;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import eu.javimar.shhh.model.PlaceContract.PlaceEntry;

public class PlaceContentProvider extends ContentProvider
{
    private static final String LOG_TAG = PlaceContentProvider.class.getName();

    public static final int PLACES = 100;
    public static final int PLACE_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static
    {
        // Add URI matches
        sUriMatcher.addURI(PlaceContract.CONTENT_AUTHORITY, PlaceContract.PATH_PLACES, PLACES);
        sUriMatcher.addURI(PlaceContract.CONTENT_AUTHORITY, PlaceContract.PATH_PLACES + "/#", PLACE_ID);
    }

   private PlaceDbHelper mDbHelper;

    @Override
    public boolean onCreate()
    {
        mDbHelper = new PlaceDbHelper(getContext());
        return true;
    }


    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder)
    {
        // Get readable database
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        int match = sUriMatcher.match(uri);
        Cursor cursor;

        switch (match)
        {
            case PLACES:
                cursor = db.query(PlaceContract.PlaceEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;

            case PLACE_ID:
                selection = PlaceEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                // This will perform a query on the place table WHERE _id equals ID to return a
                // Cursor containing that row of the table.
                cursor = db.query(PlaceEntry.TABLE_NAME, projection, selection, selectionArgs,
                    null, null, sortOrder);
                break;
            // Default exception
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Set a notification URI on the Cursor and return that Cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the desired Cursor
        return cursor;
    }


    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values)
    {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        Uri returnUri; // URI to be returned
        switch (match)
        {
            case PLACES:
                long id = db.insert(PlaceEntry.TABLE_NAME, null, values);
                if (id > 0)
                {
                    returnUri = ContentUris.withAppendedId(PlaceEntry.CONTENT_URI, id);
                }
                else
                {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            // Default case throws an UnsupportedOperationException
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return constructed uri (this points to the newly inserted row of data)
        return returnUri;
    }


    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);

        int placesDeleted; // starts as 0
        switch (match)
        {
            // Handle the single item case, recognized by the ID included in the URI path
            case PLACE_ID:
                selection = PlaceEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                placesDeleted = db.delete(PlaceEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        // Notify the resolver of a change and return the number of items deleted
        if (placesDeleted != 0)
        {
            // A place (or more) was deleted, set notification
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of places deleted
        return placesDeleted;
    }


    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs)
    {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        // Keep track of the number of updated places
        int placesUpdated;

        switch (match)
        {
            case PLACE_ID:
                // Get the place ID from the URI path
                String id = uri.getPathSegments().get(1);
                // Use selections/selectionArgs to filter for this ID
                placesUpdated = db.update(PlaceContract.PlaceEntry.TABLE_NAME, values, "_id=?",
                        new String[] { id });
                break;
            // Default exception
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver of a change and return the number of items updated
        if (placesUpdated != 0)
        {
            // A place (or more) was updated, set notification
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of places deleted
        return placesUpdated;
    }


    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not used in Shhh");
    }
}
