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
import android.widget.TextView;
import edu.wwu.cs412.tvfanatic.Constants;
import edu.wwu.cs412.tvfanatic.R;
import edu.wwu.cs412.tvfanatic.ViewReview;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.cache.ImageFetcher;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;
import edu.wwu.cs412.tvfanatic.util.DateUtil;

/**
 * A fragment that displays a list of recent replies to the user's comments.
 */
public class RecentCommentsFragment extends ListFragment {
	private static final String TAG = "RecentCommentsFragment";

	public RecentCommentsFragment() {
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.home_recent_comments, container, false);
		setListAdapter(new RecentCommentsAdapter(getActivity()));
		return v;
    }

	/**
	 * Begins querying the server for the list of the logged-in user's recent reviews in a background task.
	 */
	public void queryData() {
		Account account = Account.getLoggedInUser();
		if (account != null) {
			Log.v(TAG, "Querying Recent Comments");
			String queryStr = account.getSecretQueryString();
			// GET show data
			GETRequestTask task = new GETRequestTask(Constants.API_URL + "user/me/replies?" + queryStr,
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
			Log.v(TAG, "Loading Recent Comments data");
			JSONArray data = result.getJSONArray("data");
			List<RecentReply> replies = new ArrayList<RecentReply>(data.length());
			
			for (int i = 0; i < data.length(); i++) {
				JSONObject replyJson = data.getJSONObject(i);
				RecentReply r = new RecentReply();
				r.reviewId = replyJson.getInt("review_id");
				r.reviewTitle = replyJson.getString("review_title");
				r.episodeTitle = replyJson.getString("episode_title");
				r.showTitle = replyJson.getString("show_title");
				r.seasonEpisodeNumber = String.format("S%d E%d", 
						replyJson.getInt("season"), replyJson.getInt("episode_number"));
				r.reviewerDisplayName = replyJson.getString("reviewer_display_name");
				
				r.myComment.id = replyJson.getInt("comment_id");
				r.myComment.content = replyJson.getString("comment_content");
				r.myComment.postedDate = DateUtil.formatDate(replyJson.getString("comment_posted_at"));
				r.myComment.userId = replyJson.getInt("commenter_user_id");
				r.myComment.displayName = replyJson.getString("commenter_display_name");

				r.reply.id = replyJson.getInt("reply_comment_id");
				r.reply.content = replyJson.getString("reply_content");
				r.reply.postedDate = DateUtil.formatDate(replyJson.getString("reply_posted_at"));
				r.reply.userId = replyJson.getInt("replier_user_id");
				r.reply.displayName = replyJson.getString("replier_display_name");
				
				r.seasonEpisodeNumber = String.format("S%s E%s", replyJson.getString("season"), 
						replyJson.getString("episode_number"));
				
				r.imageUrl = Constants.API_URL + replyJson.getString("show_image_path");
				
//				Log.v(TAG, i + ")  " + r.toString());
				replies.add(r);
			}
			
			// Adapter will call notifyDataSetChanged
			((RecentCommentsAdapter) getListAdapter()).setData(replies);
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing JSON", e);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		RecentReply reply = (RecentReply) getListAdapter().getItem(position);
		String url = Constants.API_URL + "review/" + reply.reviewId;
		Intent intent = new Intent(getActivity(), ViewReview.class);
		intent.putExtra(Constants.TVF_PACKAGE + ".url", url);
		intent.putExtra(Constants.TVF_PACKAGE + ".reply_comment_id", reply.reply.id);
		getActivity().startActivity(intent);
	}
	
	private static class RecentReply {
		public Comment myComment = new Comment();
		public Comment reply = new Comment();
		public String imageUrl;
		public String showTitle;
		public String episodeTitle;
		public String seasonEpisodeNumber; // Format: "S? E?", ?'s replaced with numbers
		public String reviewTitle;
		public String reviewerDisplayName;
		public int reviewId;
	}
	
	private static class Comment {
		public int id;
		public String content;
		public int userId;
		public String displayName;
		public String postedDate;
	}
	
	private static class RecentCommentsAdapter extends BaseAdapter {
		private List<RecentReply> data = new ArrayList<RecentReply>(1);
		private LayoutInflater inflater;
		
		public RecentCommentsAdapter(Context context) {
			this.inflater = LayoutInflater.from(context);
		}
		
		public void setData(List<RecentReply> data) {
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
			// The ID of the list element is the database id of the reply comment
			return data.get(position).reply.id;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
        	View view = convertView;
			if (view == null)
				view = inflater.inflate(R.layout.home_recent_comments_list_element, null);
			
			RecentReply r = (RecentReply) getItem(position);
			
			TextView commentContentView = (TextView) view.findViewById(R.id.comment_content);
			TextView commentDateView = (TextView) view.findViewById(R.id.comment_date);
			TextView replyUserView = (TextView) view.findViewById(R.id.reply_user);
			TextView replyContentView = (TextView) view.findViewById(R.id.reply_content);
			TextView replyDateView = (TextView) view.findViewById(R.id.reply_date);
			ImageView imageView = (ImageView) view.findViewById(R.id.image);
			
			TextView seasonEpiNumView = (TextView) view.findViewById(R.id.season_episode_number);
			TextView episodeTitleView = (TextView) view.findViewById(R.id.episode_title);
			TextView reviewTitleView = (TextView) view.findViewById(R.id.review_title);
			TextView reviewerDisplayNameView = (TextView) view.findViewById(R.id.reviewer_display_name);
			
			commentContentView.setText(r.myComment.content);
			commentDateView.setText(r.myComment.postedDate);
			replyUserView.setText(r.reply.displayName + " said:");
			replyContentView.setText(r.reply.content);
			replyDateView.setText(r.reply.postedDate);
			ImageFetcher.getInstance().loadImage(r.imageUrl, imageView);

			seasonEpiNumView.setText(r.seasonEpisodeNumber);
			episodeTitleView.setText(r.episodeTitle);
			reviewTitleView.setText(r.reviewTitle); // TODO: Shorten if needed to fit in space
			reviewerDisplayNameView.setText("by " + r.reviewerDisplayName);
			
			return view;
		}
	}
}
