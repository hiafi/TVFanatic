package edu.wwu.cs412.tvfanatic;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.wwu.cs412.tvfanatic.EpisodeGridAdapter.EpisodeGridData;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;
import edu.wwu.cs412.tvfanatic.http.POSTRequestTask;
import edu.wwu.cs412.tvfanatic.util.DateUtil;
import edu.wwu.cs412.tvfanatic.util.ImageUtil;
import edu.wwu.cs412.tvfanatic.util.StringUtil;

public class ViewShow extends SearchBar {
	private static String TAG = "ViewShow";
	private static final int EPISODE_TITLE_MAX_CHARS = 21;

	private TextView lblTitle;
	private TextView lblYears;
	private TextView lblActors;
	private TextView lblDescription;
	private ImageView imgShowImage;
	private Button btnFavorite;
	private RatingBar ratingBar;
	private ProgressDialog pDialog;
	private ViewPager seasonPager;
	private SeasonPagerAdapter seasonPagerAdapter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pDialog = ProgressDialog.show(this, "Downloading...",
				"TV Fanatic is busy fetching your show's data!", true, false);
		setContentView(R.layout.view_show);
		
		lblTitle = (TextView) findViewById(R.id.show_title_label);
		lblYears = (TextView) findViewById(R.id.show_years_label);
		lblActors = (TextView) findViewById(R.id.show_actors_label);
		lblDescription = (TextView) findViewById(R.id.show_description_label);
		imgShowImage = (ImageView) findViewById(R.id.show_image);
		imgShowImage.setImageBitmap(null);
		btnFavorite = (Button) findViewById(R.id.show_add_to_favorites_button);
		ratingBar = (RatingBar) findViewById(R.id.show_average_rating);
		
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			lblDescription.setText("No show ID was provided!");
			return;
		}

		String id = extras.getString(Constants.TVF_PACKAGE + ".show_id");
		Log.v(TAG, "Querying show " + id);

		String queryStr = "";
		Account account = Account.getLoggedInUser();
		
		// User does not need to be logged in - show data will still be fetched, but
		// 'is_favorited' response field will come back 'false' since the user is
		// not identified.
		if (account != null)
			queryStr = "?" + account.getSecretQueryString();
		
		// GET show data
		GETRequestTask task = new GETRequestTask(Constants.API_URL + "show/" + id + queryStr,
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						setShowData(result);
					}
				});
		task.execute();
	}

	private void setShowData(JSONObject data) {
		try {
			Log.v("setShowData", "Running");
			
			lblTitle.setText(data.getString("title").trim());
			String endYearStr = (data.getString("end_year").equals("0") ? "Present" : data
					.getString("end_year"));
			lblYears.setText(data.getString("start_year") + "-" + endYearStr);
			lblActors.setText(data.getString("actors"));
			lblDescription.setText(data.getString("description"));
			
			String ratingStr = data.getString("rating");
			if (ratingStr != null && !ratingStr.isEmpty())
				ratingBar.setRating(Float.parseFloat(ratingStr));
			
			Bitmap showImageBitmap = ImageUtil.fromBase64(data.getString("image"));
			if (showImageBitmap == null)
				Log.w(TAG, "Base64 image could not be decoded!");
			else
			{
				Log.v(TAG, String.format("Image: %d x %d", showImageBitmap.getWidth(), showImageBitmap.getHeight()));
				imgShowImage.setImageBitmap(showImageBitmap);
				ImageUtil.scaleImage(imgShowImage);
			}
			
			final int showId = data.getInt("id");
			int seasonCount = data.getInt("latest_season");
			seasonPagerAdapter = new SeasonPagerAdapter(getFragmentManager(), showId, seasonCount);
			seasonPager = (ViewPager) findViewById(R.id.show_season_pager);
			seasonPager.setAdapter(seasonPagerAdapter);
			seasonPager.setCurrentItem(seasonCount - 1);

			updateFavoriteButton(data.getBoolean("is_favorited"));
			btnFavorite.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.v(TAG, "Clicked 'Add to Favorites'");
					Account account = Account.getLoggedInUser();
					if (account == null)
						return;
					
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair("user_secret", account.getSecret()));
					
					POSTRequestTask req = new POSTRequestTask(Constants.API_URL + "favorite/" + showId, params,
						new AsyncTaskCompleteListener<JSONObject>() {
							public void onTaskComplete(JSONObject result) {
								try {
									if (result != null && result.has("success")) {
										String newFaveStatus = result.getString("success");
										Log.v(TAG, "User has " + newFaveStatus + " show " + showId);
										updateFavoriteButton(newFaveStatus.equals("favorited"));
									}
								} catch (JSONException e) {
									Log.e(TAG, "JSONException", e);
								}
							}
						}
					);
					req.execute();
				}
			});
			seasonPagerAdapter.notifyDataSetChanged();
		} catch (JSONException e) {
			Log.e(TAG, "JSONException - Probably just a bad show - ", e);
			Toast.makeText(this, "We couldn't find your show. Sorry :(", Toast.LENGTH_LONG).show();
			this.finish();
		}

		if (this.pDialog != null) {
			this.pDialog.dismiss();
		}
	}
	
	private void updateFavoriteButton(boolean isFavorited) {
		btnFavorite.setText(isFavorited ? R.string.remove_from_favorites : R.string.add_to_favorites);
	}
	
	private static class SeasonPagerAdapter extends FragmentPagerAdapter {
		private final int showId;
		private final int seasonCount;

		public SeasonPagerAdapter(FragmentManager fm, int showId, int seasonCount) {
			super(fm);
			this.showId = showId;
			this.seasonCount = seasonCount;
		}

		@Override
		public int getCount() {
			return seasonCount;
		}

		@Override
		public Fragment getItem(int position) {
			return SeasonFragment.newInstance(showId, position + 1);
		}
	}

	public static class SeasonFragment extends Fragment implements OnItemClickListener {
		private static final String TAG = "SeasonFragment";
		
		private int showId = 0;
		private int seasonNumber = 0;
		private List<EpisodeGridData> episodeList = null;
		
		private GridView seasonGridView;
		
		static SeasonFragment newInstance(int showId, int seasonNumber) {
//			Log.v(TAG, "Constructor: showId " + showId + ", seasonNumber: " + seasonNumber);
			SeasonFragment f = new SeasonFragment();

			Bundle args = new Bundle();
			args.putInt(Constants.TVF_PACKAGE + ".show_id", showId);
			args.putInt(Constants.TVF_PACKAGE + ".season_number", seasonNumber);
			f.setArguments(args);

			return f;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Bundle args = getArguments();
			if (args != null) {
				this.showId = args.getInt(Constants.TVF_PACKAGE + ".show_id");
				this.seasonNumber = args.getInt(Constants.TVF_PACKAGE + ".season_number");
			}
			Log.v(TAG, "onCreate: showId " + showId + ", seasonNumber: " + seasonNumber);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.season_fragment, container, false);
			TextView seasonLabel = (TextView) v.findViewById(R.id.show_season_label);
			seasonLabel.setText("SEASON " + seasonNumber);
			seasonGridView = (GridView) v.findViewById(R.id.show_season_episode_grid);
			return v;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			if (episodeList == null) {
				Log.v(TAG, "GET SEASON: showId " + showId + ", seasonNumber: " + seasonNumber);
				GETRequestTask seasonTask = new GETRequestTask(
					Constants.API_URL + "season/" + showId + "/" + seasonNumber, 
					new AsyncTaskCompleteListener<JSONObject>() {
						public void onTaskComplete(JSONObject result) {
							episodeList = createEpisodeList(result);
							populateEpisodeGrid();
						}
					}
				);
				seasonTask.execute();
			} else {
				populateEpisodeGrid();
			}
		}

		private List<EpisodeGridData> createEpisodeList(JSONObject seasonJson) {
			try {
				JSONArray episodeJSONArray = seasonJson.getJSONArray("episodes");
				final int count = episodeJSONArray.length();
				
				List<EpisodeGridData> episodeList = new ArrayList<EpisodeGridData>(count);
				for (int i = 0; i < count; i++) {
					JSONObject episodeJson = episodeJSONArray.getJSONObject(i);
					EpisodeGridData episode = new EpisodeGridData();
					episode.id = episodeJson.getInt("id");
					episode.url = episodeJson.getString("episode_url");
					episode.title = StringUtil.truncate(episodeJson.getString("title"), EPISODE_TITLE_MAX_CHARS, true);
					episode.numReviews = episodeJson.getInt("review_count");
					
					try
					{
						episode.dateAired = DateUtil.toTvfFormat(
								DateUtil.fromTvdbFormat(episodeJson.getString("date_aired")));
					}
					catch (NullPointerException ne)
					{
						episode.dateAired = "";
					}
					episode.episodeNumber = episodeJson.getInt("number");
					episodeList.add(episode);
					
//					Log.v(TAG, String.format("Ep %d.%d: %s", seasonNumber, episode.episodeNumber, episode.title));
				}
				return episodeList;
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing JSON", e);
			}
			return null;
		}
		
		public void populateEpisodeGrid() {
			try
			{
				EpisodeGridAdapter adapter = new EpisodeGridAdapter(getActivity(), episodeList);
				seasonGridView.setAdapter(adapter);
				seasonGridView.setOnItemClickListener(this);
			}
			catch (NullPointerException e) 
			{ 
				e.printStackTrace();
			}
		}
		
		
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Intent intent = new Intent(getActivity(), ViewEpisode.class);
			intent.putExtra(Constants.TVF_PACKAGE + ".show_title", ((TextView)getActivity().findViewById(R.id.show_title_label)).getText());
			intent.putExtra(Constants.TVF_PACKAGE + ".episode_url", episodeList.get(position).url);
			Log.v(TAG, "Opening episode: " + episodeList.get(position).url);
			startActivity(intent);
		}
	}
}
