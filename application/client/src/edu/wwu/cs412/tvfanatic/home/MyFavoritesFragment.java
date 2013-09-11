package edu.wwu.cs412.tvfanatic.home;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.wwu.cs412.tvfanatic.Constants;
import edu.wwu.cs412.tvfanatic.R;
import edu.wwu.cs412.tvfanatic.ViewShow;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.cache.ImageFetcher;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;

public class MyFavoritesFragment extends Fragment implements OnClickListener {
	private static final String TAG = "MyFavoritesFragment";
	
	public MyFavoritesFragment() {
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.home_my_favorites, container, false);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	/**
	 * Begins querying the server for the list of the logged-in user's favorite shows in a background task.
	 */
	public void queryData() {
		Account account = Account.getLoggedInUser();
		if (account != null) {
			Log.v(TAG, "Querying My Favorites");
			String queryStr = account.getSecretQueryString();
			// GET show data
			GETRequestTask task = new GETRequestTask(Constants.API_URL + "user/me/favorites?" + queryStr,
					new AsyncTaskCompleteListener<JSONObject>() {
						public void onTaskComplete(JSONObject result) {
							onDataReceived(result);
						}
					});
			task.execute();
		} else {
			Log.w(TAG, "Not logged in!");
		}
	}

	public void onClick(View v) {
		int showId = v.getId();
		Log.v(TAG, "Opening show with ID: " + showId);
		Intent intent = new Intent(getActivity(), ViewShow.class);
		intent.putExtra(Constants.TVF_PACKAGE + ".show_id", Long.toString(showId));
		getActivity().startActivity(intent);
	}

	// Callback for when the server responds with the list of the user's favorite shows.
	private void onDataReceived(JSONObject result) {
		try {
			LinearLayout favoritesLayout = (LinearLayout) 
				getActivity().findViewById(R.id.home_my_favorites_list_view);
			favoritesLayout.removeAllViews();
			
			Log.v(TAG, "Loading My Favorites data");
			JSONArray data = result.getJSONArray("data");
			
			// Load the image of each 
			for (int i = 0; i < data.length(); i++) {
				// The show object is embedded within the Favorites object
				JSONObject showData = data.getJSONObject(i).getJSONObject("show");
				String imageUrl = Constants.API_URL + showData.getString("image_path");
				
				ImageView imageView = new ImageView(getActivity());
				imageView.setId(showData.getInt("id"));
				ImageFetcher.getInstance().loadImage(imageUrl, imageView);
				
				imageView.setOnClickListener(this);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 200);
				lp.setMargins(4, 0, 4, 0);
				favoritesLayout.addView(imageView, lp);
			}
			favoritesLayout.requestLayout();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
