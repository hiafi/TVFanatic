package edu.wwu.cs412.tvfanatic;

import edu.wwu.cs412.tvfanatic.account.AccountPreferenceActivity;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.FragmentManager.BackStackEntry;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

public class SearchBar extends Activity {
	private static final String TAG = "SearchBar";
	private static final int REQUEST_CODE_SETTINGS = 1;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		// make logo clickable
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
	    // Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
	    
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			// Open up account preferences activity
			Intent intent = new Intent(this, AccountPreferenceActivity.class);
			startActivityForResult(intent, REQUEST_CODE_SETTINGS);
			break;
		case android.R.id.home:
			// Not a perfect solution as it clears our backstack, but it works.
			Intent i = new Intent(this, TVFanatic.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			break;
		case R.id.menu_refresh:
			recreate();
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_SETTINGS) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String email = prefs.getString("account_email_preference", "");
			String displayName = prefs.getString("account_display_name_preference", "anonymous");
			boolean allowComments = prefs.getBoolean("account_allow_comments_preference", true);
			
			Log.v(TAG, String.format("Email: %s, Display name: %s, Allow comments: %b", 
					email, displayName, allowComments));
			
			// TODO: POST "/user/update" to update server with new user info
			// Might be done through a new 'update' method in Account class
		}
	}
}