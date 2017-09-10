package eu.javimar.shhh.model;
import android.net.Uri;
import android.provider.BaseColumns;


@SuppressWarnings("WeakerAccess")
public class PlaceContract
{
    public static final String CONTENT_AUTHORITY = "eu.javimar.shhh";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // PATH for the "places" directory
    public static final String PATH_PLACES = "places";

    /**
     * Inner class that defines the table of Places IDs
     */
    public static final class PlaceEntry implements BaseColumns
    {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PLACES);

        public static final String TABLE_NAME = "places";
        public static final String COLUMN_PLACE_ID = "placeID";
    }
}