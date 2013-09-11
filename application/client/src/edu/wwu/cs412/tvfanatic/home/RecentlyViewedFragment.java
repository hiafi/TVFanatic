package edu.wwu.cs412.tvfanatic.home;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import edu.wwu.cs412.tvfanatic.Constants;
import edu.wwu.cs412.tvfanatic.R;
import edu.wwu.cs412.tvfanatic.ViewShow;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.cache.ImageFetcher;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;
import edu.wwu.cs412.tvfanatic.util.ImageUtil;

public class RecentlyViewedFragment extends ListFragment {
	private static final String TAG = "RecentlyViewedFragment";

	public RecentlyViewedFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.home_recently_viewed, container, false);
        setListAdapter(new RecentlyViewedAdapter(getActivity()));
        return v;
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
			Log.v(TAG, "Querying Recently Viewed");
			String queryStr = account.getSecretQueryString();
			// GET show data
			GETRequestTask task = new GETRequestTask(Constants.API_URL + "user/me/recently_viewed?" + queryStr,
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

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		long showId = id;
		Log.v(TAG, "Opening show with ID: " + showId);
		Intent intent = new Intent(getActivity(), ViewShow.class);
		intent.putExtra(Constants.TVF_PACKAGE + ".show_id", Long.toString(showId));
		getActivity().startActivity(intent);
	}

	// Callback for when the server responds with the list of the user's favorite shows.
	private void onDataReceived(JSONObject result) {
		try {
			Log.v(TAG, "Data received for Recently Viewed fragment");
			
			JSONArray data = result.getJSONArray("data");
			List<RecentlyViewedShow> rvList = new ArrayList<RecentlyViewedShow>(data.length());
			
			for (int i = 0; i < data.length(); i++) {
				JSONObject replyJson = data.getJSONObject(i);
				
				RecentlyViewedShow r = new RecentlyViewedShow();
				r.imageUrl = Constants.API_URL + replyJson.getString("image_path");
				r.showUrl = replyJson.getString("show_url");
				r.showId = replyJson.getInt("id");
				
				rvList.add(r);
			}
			
			// Adapter will call notifyDataSetChanged
			((RecentlyViewedAdapter) getListAdapter()).setData(rvList);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private static class RecentlyViewedShow {
		public String imageUrl;
		public String showUrl;
		public int showId;
	}
	
	private static class RecentlyViewedAdapter extends BaseAdapter {
		private List<RecentlyViewedShow> data = new ArrayList<RecentlyViewedShow>(1);
		private LayoutInflater inflater;
		
		public RecentlyViewedAdapter(Context context) {
			this.inflater = LayoutInflater.from(context);
		}
		
		public void setData(List<RecentlyViewedShow> data) {
			this.data = data;
			this.notifyDataSetChanged();
		}
		
		public int getCount() {
			return data.size();
		}
		
		public Object getItem(int position) {
			return data.get(position);
		}
		
		public long getItemId(int position) {
			// The ID of the list element is the show ID
			return data.get(position).showId;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
        	View view = convertView;
			if (view == null)
				view = inflater.inflate(R.layout.home_recently_viewed_list_element, null);
			
			RecentlyViewedShow rvShow = (RecentlyViewedShow) getItem(position);
			String imageUrl = rvShow.imageUrl;
			ImageView imageView = (ImageView) view.findViewById(R.id.image);
			ImageFetcher.getInstance().loadImage(imageUrl, imageView);
			
			return view;
		}
	}
}
