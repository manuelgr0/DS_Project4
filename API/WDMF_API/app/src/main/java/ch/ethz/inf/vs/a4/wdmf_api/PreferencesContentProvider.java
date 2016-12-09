package ch.ethz.inf.vs.a4.wdmf_api;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class PreferencesContentProvider extends ContentProvider {
    private String[] columnNames;

    public PreferencesContentProvider() {
        columnNames = new String[3];
        columnNames[0] = WDMF_Connector.networkNamePK;
        columnNames[1] = WDMF_Connector.bufferSizePK;
        columnNames[2] = WDMF_Connector.timeoutPK;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // Read only: Don't allow  something other than queries
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Read only: Don't allow  something other than queries
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Log.d("XYXY", "Content query for URI " + uri.toString() + " arrived.");

        // Implementation: Ignore all parameters and just provide all relevant preferences

        // Read preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String maxBufferSize = pref.getString(WDMF_Connector.bufferSizePK, "-1");
        String networkName = pref.getString(WDMF_Connector.networkNamePK, "ERROR");
        String timeout = pref.getString(WDMF_Connector.timeoutPK, "-1");

        // Create and fill cursor to return
        MatrixCursor result = new MatrixCursor(columnNames);
        Object[] row = {networkName, Long.valueOf(maxBufferSize), Integer.valueOf(timeout)};
        result.addRow(row);

        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Read only: Don't allow  something other than queries
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
