package eu.javimar.shhh.view;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.javimar.shhh.MainActivity;
import eu.javimar.shhh.R;
import eu.javimar.shhh.model.GeoPoint;
import eu.javimar.shhh.model.PlaceContract.PlaceEntry;

import static eu.javimar.shhh.util.HelperUtils.deletePlaceFromDb;


@SuppressWarnings("all")
public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>
{
    private Context mContext;

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

    // Data source for the Adapter
    private PlaceBuffer mPlaces;


    public PlaceAdapter(Context context, PlaceBuffer places)
    {
        this.mContext = context;
        this.mPlaces = places;
        itemsPendingRemoval = new ArrayList<>();
    }


    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // Get the RecyclerView item layout
        LayoutInflater inflater = LayoutInflater.from(mContext);
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

            String placeName = mPlaces.get(position).getName().toString();
            String placeAddress = mPlaces.get(position).getAddress().toString();
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
        // delete _ID from db
        Uri uri = ContentUris.withAppendedId(PlaceEntry.CONTENT_URI,
                getIdPlaceFromDb(position));
        deletePlaceFromDb(mContext, uri);

        notifyItemRemoved(position);
    }


    /** Returns the row ID of a database column from its current list position */
    private int getIdPlaceFromDb(int position)
    {
        if (mPlaces != null)
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
                    { String.valueOf(mPlaces.get(position).getId()) };

            Cursor cursor = mContext.getContentResolver()
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
        double wifiLng = mPlaces.get(position).getLatLng().longitude;
        double wifiLat = mPlaces.get(position).getLatLng().latitude;
        return new GeoPoint(wifiLng, wifiLat);
    }


    public void swapPlaces(PlaceBuffer newPlaces)
    {
        if(mPlaces != null) mPlaces.release();
        mPlaces = newPlaces;
        // Force the RecyclerView to refresh
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount()
    {
        if(mPlaces == null) return 0;
        return mPlaces.getCount();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.tv_place_name)TextView nameTextView;
        @BindView(R.id.tv_place_address)TextView addressTextView;
        @BindView(R.id.regularLayout)RelativeLayout regularLayout;
        @BindView(R.id.swipeLayout)LinearLayout swipeLayout;
        @BindView(R.id.undo)TextView undoTextView;

        public PlaceViewHolder(View itemView)
        {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
