package eu.javimar.shhh.view;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.javimar.shhh.Geofencing;
import eu.javimar.shhh.R;
import eu.javimar.shhh.model.GeoPoint;
import eu.javimar.shhh.model.PlaceContract.PlaceEntry;
import eu.javimar.shhh.util.MyEventBusNotification;

import static eu.javimar.shhh.MainActivity.sAreGeofencesEnabled;
import static eu.javimar.shhh.MainActivity.sPlaceList;
import static eu.javimar.shhh.util.HelperUtils.deletePlaceFromDb;


@SuppressWarnings("all")
public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>
{
    private Context context;
    private Geofencing geofencing;

    /**
     * To Keep track of swiped Items create arraylist “itemsPendingRemoval”
     * on Swipe add to item to itemsPendingRemoval list
     * on Undo Remove item from itemsPendingRemoval list
     */
    private List<GeoPoint> itemsPendingRemoval; // GeoPoints are always unique for each place

    private static final int PENDING_REMOVAL_TIMEOUT = 3000; // 3sec
    private Handler handler = new Handler(); // handler for running delayed runnables
    // map of items to pending runnables, so we can cancel a place removal if needed
    HashMap<GeoPoint, Runnable> pendingRunnables = new HashMap<>();


    public PlaceAdapter(Context context, Geofencing geofencing)
    {
        this.context = context;
        this.geofencing = geofencing;
        itemsPendingRemoval = new ArrayList<>();
    }


    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // Get the RecyclerView item layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.place_item, parent, false);
        return new PlaceViewHolder(view);
    }


    @Override
    public void onBindViewHolder(PlaceViewHolder holder, final int position)
    {
        // get necessary information to compare if item is already waiting to be removed
        final GeoPoint placeLocation = getCoordinatesFromPlace(position);

        if (itemsPendingRemoval.contains(placeLocation))
        {
            // show swipe layout and hide the regular layout
            holder.regularLayout.setVisibility(View.GONE);
            holder.swipeLayout.setVisibility(View.VISIBLE);
            holder.undoTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    undoOption(placeLocation, position);
                }
            });
        }
        else
        {
            // Proceed normally with the regular layout and hide the swipe layout
            holder.regularLayout.setVisibility(View.VISIBLE);
            holder.swipeLayout.setVisibility(View.GONE);

            String placeName = sPlaceList.get(position).getPlaceName();
            String placeAddress = sPlaceList.get(position).getPlaceAddress();
            holder.nameTextView.setText(placeName);
            holder.addressTextView.setText(placeAddress);
        }
    }


    private void undoOption(GeoPoint placeLoc, int position)
    {
        Runnable pendingRemovalRunnable = pendingRunnables.get(placeLoc);
        pendingRunnables.remove(placeLoc);
        if (pendingRemovalRunnable != null) handler.removeCallbacks(pendingRemovalRunnable);
        itemsPendingRemoval.remove(placeLoc);
        // this will rebind the row in "normal" state
        notifyItemChanged(position);
    }


    /** Called when swipe action is initiated */
    public void pendingRemoval(final int position)
    {
        final GeoPoint placeLoc = getCoordinatesFromPlace(position);
        if (!itemsPendingRemoval.contains(placeLoc))
        {
            // there can be only one per run
            itemsPendingRemoval.add(placeLoc);
            // this will redraw row in "undoTextView" state
            notifyItemChanged(position);
            // create, store and post a runnable to remove the data
            Runnable pendingRemovalRunnable = new Runnable()
            {
                @Override
                public void run() {
                    remove(placeLoc, position);
                }
            };
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            pendingRunnables.put(placeLoc, pendingRemovalRunnable);
        }
    }


    // Delete the wifi from database
    public void remove(GeoPoint placeLoc, int position)
    {
        if (itemsPendingRemoval.contains(placeLoc))
        {
            // clear list
            itemsPendingRemoval.remove(placeLoc);
        }
        // delete place(_ID) from database
        Uri uri = ContentUris.withAppendedId(PlaceEntry.CONTENT_URI,
                getIdPlaceFromDb(position));
        deletePlaceFromDb(context, uri);

        //notifyItemRemoved(position);

        // delete place from master list as well to synch with DB, and refresh RecyclerView
        sPlaceList.remove(position);

        if(sPlaceList.size() < 1)
        {
            // Signal MainActivity, that RecyclerView is empty
            EventBus.getDefault().post(new MyEventBusNotification(Activity.RESULT_OK));
        }
        else this.notifyDataSetChanged();


        // after deleting a place, need to refresh geofences if enabled
        if(sAreGeofencesEnabled)
        {
            geofencing.unRegisterAllGeofences();
            geofencing.updateGeofencesList();
            geofencing.registerAllGeofences();
        }
    }


    /** Returns the row ID of a database column from its current list position */
    private int getIdPlaceFromDb(int position)
    {
        if (sPlaceList != null)
        {
            // need to retrieve the _ID from the database
            String[] projection = new String[]
                    {
                            PlaceEntry._ID,
                            PlaceEntry.COLUMN_PLACE_ID
                    };
            String selection =  PlaceEntry.COLUMN_PLACE_ID + "=? ";
            // get the ID of the Place and pass it to the query to get the _ID
            String [] selectionArgs = new String[]
                    { String.valueOf(sPlaceList.get(position).getPlaceId()) };

            Cursor cursor = context.getContentResolver()
                    .query(PlaceEntry.CONTENT_URI, projection, selection, selectionArgs, null);

            if (cursor == null || cursor.getCount() < 1)
            {
                return -1;
            }
            else
            {
                // return the ID
                cursor.moveToFirst();
                int i = cursor.getInt(cursor.getColumnIndex(PlaceEntry._ID));
                cursor.close();
                return i;
            }
        }
        else
        {
            return -1;
        }
    }

    public boolean isPendingRemoval(int position)
    {
        return itemsPendingRemoval.contains(getCoordinatesFromPlace(position));
    }


    private GeoPoint getCoordinatesFromPlace(int position)
    {
        double placeLng = sPlaceList.get(position).getCoordinates().getLongitude();
        double placeLat = sPlaceList.get(position).getCoordinates().getLatitude();
        return new GeoPoint(placeLng, placeLat);
    }


    @Override
    public int getItemCount()
    {
        return sPlaceList.size();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.tv_place_name)TextView nameTextView;
        @BindView(R.id.tv_place_address)TextView addressTextView;
        @BindView(R.id.regularLayout)LinearLayout regularLayout;
        @BindView(R.id.swipeLayout)LinearLayout swipeLayout;
        @BindView(R.id.undo)TextView undoTextView;

        public PlaceViewHolder(View itemView)
        {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
