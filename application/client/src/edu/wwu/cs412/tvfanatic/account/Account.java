package edu.wwu.cs412.tvfanatic.account;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.wwu.cs412.tvfanatic.Constants;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.POSTRequestTask;

public class Account {
	public interface LoginListener {
		public void onLoginSucceeded();
		public void onLoginFailed();
	}
	
	private static final String TAG = "Account";
	
	private static class PrefKeys {
		public static final String EMAIL = "account_email_preference";
		public static final String DISPLAY_NAME = "account_display_name_preference";
		public static final String ALLOW_COMMENTS = "account_allow_comments_preference";
	}
	
	private static Account loggedInUser = null;
	
	private static Context contextContext;
	private String email;
	private String displayName;
	private String secret;
	private boolean allowCommentsOnReviews;

	/**
	 * Returns the account info for the currently logged-in user, or <tt>null</tt> if user is not logged in.
	 */
	public static Account getLoggedInUser() {
		return loggedInUser;
	}

	public static Account fromPreferences(Context context) {
		contextContext = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String email = prefs.getString(PrefKeys.EMAIL, null);
		if (email == null)
			return null;
		
		String displayName = prefs.getString(PrefKeys.DISPLAY_NAME, "anonymous");
		boolean allowComments = prefs.getBoolean(PrefKeys.ALLOW_COMMENTS, true);
		return new Account(email, displayName, allowComments);
	}

	public static void login(Account account, LoginListener callback) {
		Log.v(TAG, "Logging in as " + account.email);
		Account.loginOrCreate(Constants.API_URL + "user/assert", account, callback);
	}

	public static void create(Context context, String email, String displayName, boolean allowComments,
			LoginListener callback) {
		Account account = new Account(email, displayName, allowComments);
		account.saveToPreferences(context);
		Account.loginOrCreate(Constants.API_URL + "user/assert", account, callback);
	}
	
	//Remove this function on release
	public static void useDebugAccount()
	{
		loggedInUser = null;
		Account account = new Account("unknown@example.com", "Philip Bjorge", true);
		account.setSecret("abc");
		loggedInUser = account;
	}

	// User login and create are currently very similar operations from the server's standpoint
	public static void loginOrCreate(String url, final Account account, 
			final LoginListener callback) {
		loggedInUser = null;
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("email", account.email));
		params.add(new BasicNameValuePair("display_name", account.displayName));
		params.add(new BasicNameValuePair("allow_comments", String.valueOf(account.allowCommentsOnReviews)));
		
		POSTRequestTask task = new POSTRequestTask(url, params,
			new AsyncTaskCompleteListener<JSONObject>() {
				public void onTaskComplete(JSONObject result) {
					try {
						if (result != null) {
							account.setAllowCommentsOnReviews(result.getInt("allow_comments_on_reviews") == 1);
							account.setDisplayName(result.getString("display_name"));
							account.setSecret(result.getString("user_secret"));
							Log.v(TAG, "User secret: " + account.secret);
							loggedInUser = account;
							callback.onLoginSucceeded();
							return;
						} else {
							Log.e(TAG, "Server failed to authenticate email '" + account.email + "'");
						}
					} catch (JSONException e) {
						Log.e(TAG, "JSON parse error", e);
					}
					callback.onLoginFailed();
				}
			}
		);
		task.execute();
	}
	
	private Account(String email, String displayName, boolean allowCommentsOnReviews) {
		this.email = email;
		this.displayName = displayName;
		this.allowCommentsOnReviews = allowCommentsOnReviews;
	}
	
	public void saveOnline() {
		Account account = getLoggedInUser();
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("display_name", account.getDisplayName()));
		params.add(new BasicNameValuePair("allow_comments", String.valueOf(account.getAllowCommentsOnReviews())));
		params.add(new BasicNameValuePair("user_secret", String.valueOf(account.getSecret())));
		POSTRequestTask task = new POSTRequestTask(Constants.API_URL + "user/update", params,
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						if (result != null) {
							// Settings updated
							return;
						} else {
							Account account = getLoggedInUser();
							Log.e(TAG, "Server failed to update user prefs with email '" + account.getEmail() + "'");
						}
					}
				}
			);
		task.execute();
	}
	
	public void saveToPreferences() {
		saveToPreferences(contextContext);
	}

	private void saveToPreferences(Context context) {
		Log.v(TAG, "Saving account to preferences");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PrefKeys.EMAIL, getEmail());
		editor.putString(PrefKeys.DISPLAY_NAME, getDisplayName());
		editor.putBoolean(PrefKeys.ALLOW_COMMENTS, getAllowCommentsOnReviews());
		editor.apply();
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getSecret() {
		return secret;
	}

	/**
	 * Returns user_secret as a URL-encoded GET query parameter, i.e. the string "user_secret=<i>secret</i>",
	 * where <i>secret</i> is the user_secret associated with this account.
	 */
	public String getSecretQueryString() {
		try {
			return "user_secret=" + URLEncoder.encode(getSecret(), "UTF-8");
		} catch (UnsupportedEncodingException e) { /* Should never happen: UTF-8 is universal */ }
		return "";
	}
	
	private void setSecret(String secret) {
		this.secret = secret;
	}

	public boolean getAllowCommentsOnReviews() {
		return allowCommentsOnReviews;
	}

	public void setAllowCommentsOnReviews(boolean allowCommentsOnReviews) {
		this.allowCommentsOnReviews = allowCommentsOnReviews;
	}

	@Override
	public String toString() {
		return "Account [email=" + email + ", displayName=" + displayName + ", secret=" + secret
				+ ", allowCommentsOnReviews=" + allowCommentsOnReviews + "]";
	}
}
