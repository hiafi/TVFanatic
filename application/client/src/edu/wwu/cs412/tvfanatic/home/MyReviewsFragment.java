package edu.wwu.cs412.tvfanatic.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.wwu.cs412.tvfanatic.Constants;
import edu.wwu.cs412.tvfanatic.R;
import edu.wwu.cs412.tvfanatic.ViewReview;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.cache.ImageFetcher;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;
import edu.wwu.cs412.tvfanatic.util.ImageUtil;

public class MyReviewsFragment extends ListFragment {
	private static final String TAG = "MyReviewsFragment";
	
	public MyReviewsFragment() {
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.home_my_reviews, container, false);
		setListAdapter(new MyReviewsAdapter(getActivity()));
		return v;
	}
	
	/**
	 * Begins querying the server for the list of the logged-in user's recent reviews in a background task.
	 */
	public void queryData() {
		Account account = Account.getLoggedInUser();
		if (account != null) {
			Log.v(TAG, "Querying My Reviews");
			String queryStr = account.getSecretQueryString();
			// GET show data
			GETRequestTask task = new GETRequestTask(Constants.API_URL + "user/me/reviews?" + queryStr,
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
	
	// Callback for when the server responds with the list of the user's favorite shows.
	private void onDataReceived(JSONObject result) {
		try {
			Log.v(TAG, "Loading My Reviews data");
			JSONArray data = result.getJSONArray("data");
			List<MyReview> myReviews = new ArrayList<MyReview>(data.length());
			
			for (int i = 0; i < data.length(); i++) {
				JSONObject reviewJson = data.getJSONObject(i);
				MyReview review = new MyReview();
				review.reviewId = reviewJson.getInt("review_id");
				review.reviewTitle = reviewJson.getString("review_title");
				review.episodeTitle = reviewJson.getString("episode_title");
				review.seasonEpisodeNumber = String.format("S%s E%s", reviewJson.getString("season"), 
						reviewJson.getString("episode_number"));
				review.newCommentsCount = reviewJson.getInt("new_comments");
				if (!reviewJson.isNull("agree"))
					review.agree = reviewJson.getString("agree") + "% agree";
				
				review.imageUrl = Constants.API_URL + reviewJson.getString("show_image_path");
				
				myReviews.add(review);
			}
			
			// Adapter will call notifyDataSetChanged
			((MyReviewsAdapter) getListAdapter()).setData(myReviews);
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing JSON", e);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String url = Constants.API_URL + "review/" + id;
		Intent intent = new Intent(getActivity(), ViewReview.class);
		intent.putExtra(Constants.TVF_PACKAGE + ".url", url);
		getActivity().startActivity(intent);
	}

	private class MyReview {
		public int reviewId;
		public String imageUrl; // Full URL to image file on server
		public String seasonEpisodeNumber; // Format: "S? E?", ?'s replaced with numbers
		public String episodeTitle;
		public String agree; // Format: "?% agree", ? is integer agree percentage
		public String reviewTitle;
		public int newCommentsCount;
		
		@Override
		public String toString() {
			return "MyReview [reviewId=" + reviewId + ", imageUrl=" + imageUrl + ", seasonEpisodeNumber="
					+ seasonEpisodeNumber + ", episodeTitle=" + episodeTitle + ", agree=" + agree
					+ ", reviewTitle=" + reviewTitle + ", newCommentsCount=" + newCommentsCount + "]";
		}
	}
	
	private class MyReviewsAdapter extends BaseAdapter {
		private List<MyReview> data = new ArrayList<MyReview>();
		private LayoutInflater inflater;
		
		public MyReviewsAdapter(Context context) {
			this.inflater = LayoutInflater.from(context);
		}
		
		public void setData(List<MyReview> data) {
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
			return data.get(position).reviewId;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null)
				view = inflater.inflate(R.layout.home_my_reviews_list_element, null);
			
			MyReview elemData = (MyReview) getItem(position);
			
			TextView reviewTitleView = (TextView) view.findViewById(R.id.my_reviews_elem_review_title);
			TextView episodeTitleView = (TextView) view.findViewById(R.id.my_reviews_elem_episode_title);
			TextView agreeView = (TextView) view.findViewById(R.id.my_reviews_elem_agree);
			TextView seasonEpisodeNumberView = (TextView) view.findViewById(R.id.my_reviews_elem_season_episode_number);
			TextView newCommentsView = (TextView) view.findViewById(R.id.my_reviews_elem_new_comments);
			ImageView imageView = (ImageView) view.findViewById(R.id.my_reviews_elem_image);
			
			reviewTitleView.setText(elemData.reviewTitle);
			episodeTitleView.setText(elemData.episodeTitle);
			agreeView.setText(elemData.agree);
			seasonEpisodeNumberView.setText(elemData.seasonEpisodeNumber);
			newCommentsView.setText("+" + elemData.newCommentsCount);
			ImageFetcher.getInstance().loadImage(elemData.imageUrl, imageView);
			
			return view;
		}
	}
}
