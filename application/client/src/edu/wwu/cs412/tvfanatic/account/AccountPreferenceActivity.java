package edu.wwu.cs412.tvfanatic.account;

import edu.wwu.cs412.tvfanatic.R;
import edu.wwu.cs412.tvfanatic.account.Account.LoginListener;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class AccountPreferenceActivity extends PreferenceActivity implements LoginListener, OnSharedPreferenceChangeListener {
	private ProgressDialog pDialog;
	private Account user;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefSync();
	}
	
	@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (!pDialog.isShowing()) {
			// User changed preference
			Toast.makeText(getApplicationContext(), "CHANGED", Toast.LENGTH_SHORT).show();
			if (key.equals("account_allow_comments_preference")) {
				user.setAllowCommentsOnReviews(sharedPreferences.getBoolean(key, true));
			} else {
				user.setDisplayName(sharedPreferences.getString(key, "anonymous"));
			}
			user.saveOnline();
		}
    }
	
	private void prefSync() {
		pDialog = ProgressDialog.show(this, "Downloading...", "TV Fanatic is busy fetching your preferences!", true, false);
		user = Account.getLoggedInUser();
		Account.login(user, this);
	}
	
	private void loadPrefs() {
		// Update the PrefActivity after download
		addPreferencesFromResource(R.xml.account_prefs);
		((EditTextPreference) getPreferenceScreen().findPreference("account_display_name_preference")).setText(user.getDisplayName());
		((CheckBoxPreference) getPreferenceScreen().findPreference("account_allow_comments_preference")).setChecked(user.getAllowCommentsOnReviews());
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		pDialog.hide(); pDialog.dismiss();
	}
	
	@Override
	public void onLoginSucceeded() {
		// Get the latest preferences from the server
		loadPrefs();
	}
	@Override
	public void onLoginFailed() {
		loadPrefs();
	}
}
