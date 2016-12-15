package ch.ethz.inf.vs.a4.wdmf_api.ui;


import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;
import ch.ethz.inf.vs.a4.wdmf_api.R;
import ch.ethz.inf.vs.a4.wdmf_api.service.MainService;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static Context context;
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if(preference.getKey().equals(WDMF_Connector.bufferSizePK)){
                try {
                    int s = Integer.parseInt(stringValue);
                    if (s < 1) {
                        Log.d("Settings Activity", "Preference buffer size was invalid (" + s + ").");
                        return false;
                    }
                    preference.setSummary(s + "KB");

                } catch (Throwable t) {
                    Log.d("Settings Activity", "Preference value for buffer size was no parsable to an integer.", t);
                    return false;
                }
            } else  if(preference.getKey().equals(WDMF_Connector.timeoutPK)){
                try {
                    int s = Integer.parseInt(stringValue);
                    preference.setSummary(s + "min");
                } catch (Throwable t) {
                    Log.d("Settings Activity", "Preference value for timeout was no parsable to an integer.", t);
                    return false;
                }
            }else  if(
                    preference.getKey().equals(WDMF_Connector.maxNoContactTimeForeignDevicesPK)
                    || preference.getKey().equals(WDMF_Connector.maxNoContactTimePK)
                    || preference.getKey().equals(WDMF_Connector.sleepTimePK)
                    ){
                try {
                    int s = Integer.parseInt(stringValue);
                    if (s  > 999  || s < 1) {
                        Log.d("Settings Activity", "Preference value for time was invalid (" + s + ").");
                        return false;
                    }
                    preference.setSummary(s + "s");
                } catch (Throwable t) {
                    Log.d("Settings Activity", "Preference value for time was no parsable to an integer. (" + stringValue + ").", t);
                    return false;
                }
            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            // Call MainService.updateFromPreferences with a start command and the intent field set accordingly
            // This is the very simplest way to communicate to the Service, binding to it is not possible
            //  anymore as soon as other clients have connected to the Messenger.
            Intent i = new Intent();
            i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));
            i.putExtra("updatePrefs", true);
            context.startService(i);

            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        // Check for value (could be string or bool)
        if(preference.getKey().equals(WDMF_Connector.lockPreferencesPK)){
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), true));
        }
        else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || BasicPreferenceFragment.class.getName().equals(fragmentName)
                || AdvancedPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows basic preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BasicPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_basic);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.networkNamePK));
            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.timeoutPK));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AdvancedPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_advanced);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.lockPreferencesPK));
            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.bufferSizePK));
            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.maxNoContactTimeForeignDevicesPK));
            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.maxNoContactTimePK));
            bindPreferenceSummaryToValue(findPreference(WDMF_Connector.sleepTimePK));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}