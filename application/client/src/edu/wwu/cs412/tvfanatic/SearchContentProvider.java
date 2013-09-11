package edu.wwu.cs412.tvfanatic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.SearchView;

public class SearchContentProvider extends ContentProvider {
    public static final String AUTHORITY = "edu.wwu.cs412.tvfanatic.search_content_provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/search");

    // UriMatcher constant for search suggestions
    private static final int SEARCH_SUGGEST = 1;

    private static final UriMatcher uriMatcher;

    private static final String[] SEARCH_SUGGEST_COLUMNS = {
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };
    
    private MatrixCursor asyncCursor;
    private GETRequestTask ongoingTask;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
    	asyncCursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS, 10);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // Use the UriMatcher to see what kind of query we have
        switch (uriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                String query = uri.getLastPathSegment().toLowerCase();
				try {
					// Avoid the problem of HTTPRequest1 finishing after HTTPRequest2
					// and updating with "old" results
					if (ongoingTask != null && ongoingTask.getStatus() != AsyncTask.Status.FINISHED)
						ongoingTask.cancel(true);
					ongoingTask = new GETRequestTask(Constants.API_URL + "search/" + URLEncoder.encode(query, "UTF-8"),
	    			new AsyncTaskCompleteListener<JSONObject>() {
	    			 	public void onTaskComplete(JSONObject result) {
	    			 		return;
	    			 	}
	        		});
					ongoingTask.execute();
					try {
						// Search Content Provider does not block
						updateHTTP(ongoingTask.get());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                return asyncCursor;
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    private void updateHTTP(JSONObject j) throws JSONException {
    	// Creates a new cursor when we get a HTTP response returns
		MatrixCursor nCursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS, 10);
    	JSONArray results = j.getJSONArray("data");
    	for (int i = 0; i < results.length(); i++) {
    		j = results.getJSONObject(i);
            nCursor.addRow(new String[] {
                    j.getString("id"), j.getString("title"), j.getString("id")
            });
    	}
        asyncCursor = nCursor;
	}

	@Override
    public int update(Uri uri, ContentValues arg1, String arg2, String[] arg3) {
        throw new UnsupportedOperationException();
    }
}