package edu.wwu.cs412.tvfanatic;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;
import edu.wwu.cs412.tvfanatic.http.POSTRequestTask;
import edu.wwu.cs412.tvfanatic.util.ImageUtil;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewReview extends SearchBar {
	
	int id;
	String url;
	private ProgressDialog pDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pDialog = ProgressDialog.show(this, "Downloading...",
				"TV Fanatic is busy fetching your review's data!", true, false);
		setContentView(R.layout.view_review);
		
		Button comment = (Button)findViewById(R.id.review_comment_on);
		Button like = (Button)findViewById(R.id.review_agree_button);
		Button dislike = (Button)findViewById(R.id.review_disagree_button);
		
		comment.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setCommentTargetReview();
            }
		});
		
		like.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				agree();
            }
		});
		
		dislike.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				disagree();
            }
		});
		
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String reviewUrl = extras.getString(Constants.TVF_PACKAGE + ".url");
			
			url = reviewUrl +"?user_secret="+
					Account.getLoggedInUser().getSecret();
			// TODO: REST-GET the complete review data with id
			sendRequest();
		}
	}
	
	public void sendRequest()
	{
		GETRequestTask task = new GETRequestTask(url,
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						setReview(result);
					}
				});
		task.execute();
	}
	
	private void sendAgreePost(int agree)
	{
		String url = Constants.API_URL + "agree/" + this.id;
		Log.v("url", url);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		params.add(new BasicNameValuePair("agree", ""+agree));
		params.add(new BasicNameValuePair("user_secret", Account.getLoggedInUser().getSecret()));
		POSTRequestTask task = new POSTRequestTask(url, params,
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						Log.v("result", result.toString());
						finishAgree();
					}
				});
		task.execute();
	}
	
	private void agree()
	{
		sendAgreePost(1);
	}
	
	private void disagree()
	{
		sendAgreePost(0);
	}
	
	private void finishAgree()
	{
		Toast toast = Toast.makeText(this, 
				"Vote cast", 
				Toast.LENGTH_SHORT);
		toast.show();
		sendRequest();
	}
	
	public void setCommentTargetReview()
	{
		setCommentTarget(-1, getString(R.string.comment_target_review));
	}
	
	public void setCommentTarget(int user_id, String user_name)
	{
		FragmentManager fm = this.getFragmentManager();
		((WriteComment)fm.findFragmentById(R.id.review_write_comment_fragment)).setTarget(user_id, user_name);
	}
	
	private void setReview(JSONObject json)
	{
		TextView reviewTitleView = (TextView) findViewById(R.id.review_title_label);
		TextView reviewAuthorView = (TextView) findViewById(R.id.review_author_label);
		TextView reviewContentView = (TextView) findViewById(R.id.review_content_label);
		TextView reviewEpisodeView = (TextView) findViewById(R.id.review_episode_title_label);
		TextView reviewShowView = (TextView) findViewById(R.id.review_show_title_label);
		TextView reviewSeasonView = (TextView) findViewById(R.id.review_season_and_number_label);
		TextView reviewAgreePercent = (TextView) findViewById(R.id.review_agree_label);
		
		
		FragmentManager fm = this.getFragmentManager();
		Fragment reviewComments = fm.findFragmentById(R.id.review_comments_fragment);
		
		
		try {
			this.id = json.getInt("id");
			((ViewCommentsFragment)reviewComments).sendRequest(this.id);
			((WriteComment)fm.findFragmentById(R.id.review_write_comment_fragment)).review_id = this.id;
			reviewTitleView.setText(json.getString("title"));
			reviewAuthorView.setText(json.getString("user"));
			reviewContentView.setText(json.getString("content"));
			reviewEpisodeView.setText(json.getJSONObject("episode").getString("title"));
			double agree;
			try
			{
				agree = json.getDouble("agree");
				Log.v("agree", "agree "+agree);
				//if (agree == null) { agree = 0.0; }
				
			}
			catch (JSONException je)
			{
				//je.printStackTrace();
				agree = 0.0;
			}
			JSONObject episode_json = json.getJSONObject("episode");
			DecimalFormat df = new DecimalFormat("##");
			reviewAgreePercent.setText(df.format(agree * 100) + "% agree");
			reviewShowView.setText(episode_json.getJSONObject("show").getString("title"));
			String se = "S"+episode_json.getString("season")+
					" E"+episode_json.getString("number");
			reviewSeasonView.setText(se);
			Bitmap bm = ImageUtil.fromBase64(episode_json.getString("image"));
			
			ImageView imageView = (ImageView) findViewById(R.id.review_show_image);
			imageView.setImageBitmap(bm);
			ImageUtil.scaleImage(imageView);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pDialog.hide(); pDialog.dismiss();
	}
}
