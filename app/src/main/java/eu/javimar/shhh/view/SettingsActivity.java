package eu.javimar.shhh.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.javimar.shhh.R;


public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    public static class LocationPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener
    {

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);

            // update the preference summary when the settings activity is launched.
            // Given the key of a preference, we use findPreference to get the Preference object,
            // and setup the preference using a helper method called bindPreferenceSummaryToValue().
            List<Preference> pref = new ArrayList<Preference>();

            pref.add(findPreference(getString(R.string.pref_activate_geofences_key)));
            pref.add(findPreference(getString(R.string.pref_activate_notification_key)));
            pref.add(findPreference(getString(R.string.pref_georadius_key)));

            bindPreferenceSummaryToValue(pref);
        }


        /**
         * Set the current PreferenceFragment instance as the listener on
         * each preference. We also read the current value of the preference stored in
         * the SharedPreferences on the device, and display that in the preference summary
         * (so that the user can see the current value of the preference).
         */
        private void bindPreferenceSummaryToValue(List<Preference> pref)
        {
            Iterator<Preference> iterator = pref.iterator();
            Preference preference;

            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(pref.get(0).getContext());

            while(iterator.hasNext())
            {
                preference = iterator.next();
                preference.setOnPreferenceChangeListener(this);
                if(preference instanceof CheckBoxPreference || preference instanceof SwitchPreference)
                {
                    Boolean prefBoolean = sharedPreferences.getBoolean(preference.getKey(), false);
                    onPreferenceChange(preference, prefBoolean);
                }
                else
                {
                    String preferenceString = sharedPreferences.getString(preference.getKey(), "50");
                    onPreferenceChange(preference, preferenceString);
                }
            }
        }


        /**
         * Get notified when a preference changes. Then when a single Preference has
         * been changed by the user, the onPreferenceChange() method will be invoked with
         * the key of the preference that was changed
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String stringValue = value.toString();

            // Update the onPreferenceChange() method to properly update the summary of
            // a Preference (using the label, instead of the key)
            if(preference instanceof SwitchPreference)
            {
                // these are the boolean preferences
                switch (stringValue)
                {
                    case "true":
                        if(preference.getKey().equals(getString(R.string.pref_activate_notification_key)))
                        {
                            preference.setSummary(R.string.pref_notification_enabled);
                        }
                        else preference.setSummary(R.string.pref_geofence_enabled);
                        break;
                    case "false":
                        if(preference.getKey().equals(getString(R.string.pref_activate_notification_key)))
                        {
                            preference.setSummary(R.string.pref_notification_disabled);
                        }
                        else preference.setSummary(R.string.pref_geofence_disabled);
                        break;
                    default:
                        preference.setSummary(stringValue);
                        break;
                }
            }
            else preference.setSummary(stringValue);
            return true;
        }

    }// end inner class


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}