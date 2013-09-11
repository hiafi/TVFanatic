package edu.wwu.cs412.tvfanatic;

import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.account.Account.LoginListener;
import edu.wwu.cs412.tvfanatic.account.AccountCreateDialog;
import edu.wwu.cs412.tvfanatic.cache.ImageFetcher;
import edu.wwu.cs412.tvfanatic.home.MyFavoritesFragment;
import edu.wwu.cs412.tvfanatic.home.MyReviewsFragment;
import edu.wwu.cs412.tvfanatic.home.RecentCommentsFragment;
import edu.wwu.cs412.tvfanatic.home.RecentlyViewedFragment;

public class TVFanatic extends SearchBar implements LoginListener {
	private static final String TAG = "TVFanatic";
	private static final String CACHE_DIR = "main_cache";

	private ImageFetcher imageFetcher = null;
	private boolean isLoggedIn = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		isLoggedIn = false;
		ImageFetcher.init(this, CACHE_DIR);
		imageFetcher = ImageFetcher.getInstance();
		
		// Uncomment the following method call if you want to erase your saved preferences and
		// force the "Create Account" dialog to appear again, rather than auto-login.
		
		//clearPreferences();
		
		try
		{
			Account account = edu.wwu.cs412.tvfanatic.account.Account.fromPreferences(this);
			if (account == null) {
				AccountManager acc_manage = AccountManager.get(getApplicationContext());
				android.accounts.Account[] accounts = acc_manage.getAccountsByType("com.google");
				String gaccount = null;
				if (accounts.length > 0) {
					gaccount = accounts[0].name;
				}

				showAccountCreateDialog(this, gaccount);
			} else {
				Account.login(account, this);
			}
		}
		catch (Exception e)
		{
			//TODO: remove me on release!
			Log.v("Error logging in", e.toString());
			Account.useDebugAccount();
		}
	}
	
    @Override
    public void onResume() {
        super.onResume();
        imageFetcher.setExitTasksEarly(false);
        refresh();
    }

	@Override
    protected void onPause() {
        super.onPause();
        imageFetcher.setExitTasksEarly(true);
        imageFetcher.flushCache();
    }

	@Override
	protected void onStart() {
		super.onStart();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageFetcher.closeCache();
    }

	private void clearPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.clear();
		editor.apply();
	}
	
	private void showAccountCreateDialog(LoginListener listener, String gaccount) {
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("account_create");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DialogFragment newFragment = AccountCreateDialog.newInstance(listener, gaccount);
	    newFragment.show(ft, "account_create");
	}

    private void refresh() {
		Log.v(TAG, "Refreshing content of Main activity");
    	
		if (isLoggedIn) {
			FragmentManager fm = getFragmentManager();
			
			// Load "My Favorites"
			MyFavoritesFragment myFavoritesFragment = (MyFavoritesFragment) 
					fm.findFragmentById(R.id.my_favorites_fragment);
			myFavoritesFragment.queryData();
			
			// Load "My Reviews"
			MyReviewsFragment myReviewsFragment = (MyReviewsFragment) 
					fm.findFragmentById(R.id.my_reviews_fragment);
			myReviewsFragment.queryData();
	
			// Load "Recent Comments"
			RecentCommentsFragment recentCommentsFragment = (RecentCommentsFragment) 
					fm.findFragmentById(R.id.recent_comments_fragment);
			recentCommentsFragment.queryData();
	
			// Load "Recently Viewed"
			RecentlyViewedFragment recentlyViewedFragment = (RecentlyViewedFragment) 
					fm.findFragmentById(R.id.recently_viewed_fragment);
			recentlyViewedFragment.queryData();
		}
	}

	public void onLoginSucceeded() {
		Log.v(TAG, "Login succeeded!");
		isLoggedIn = true;
		refresh();
	}

	public void onLoginFailed() {
		Log.v(TAG, "Login failed!");
		isLoggedIn = false;
	}
}
