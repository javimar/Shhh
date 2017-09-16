package eu.javimar.shhh.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.javimar.shhh.R;

import static eu.javimar.shhh.MainActivity.sCurrentPosition;


public class AboutActivity extends AppCompatActivity
{
    @BindView(R.id.textLocation)TextView mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        ButterKnife.bind(this);

        setTitle(getString(R.string.activity_about));

        if(sCurrentPosition != null)
        {
            mLocation.setText(String.format(getString(R.string.about_location),
                    sCurrentPosition.convertToSexagesimal(this)));
        }
        else
        {
            mLocation.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
