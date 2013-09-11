package edu.wwu.cs412.tvfanatic;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.POSTRequestTask;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WriteReview extends SearchBar {
	private String url;
	private JSONObject data;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.write_review);
		
		//Load Data
		Bundle extras = getIntent().getExtras();
		this.url = extras.getString(Constants.TVF_PACKAGE + ".url");
		
		//Setup listeners
		Button btn = (Button)findViewById(R.id.write_review_done_button);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                askCreate();
            }
		});

		try {
			this.data = new JSONObject(extras.getString(Constants.TVF_PACKAGE + ".json"));
			TextView title = (TextView)findViewById(R.id.write_review_reviewing_value);
			String title_text;
			title_text = extras.getString(Constants.TVF_PACKAGE + ".show_title") + 
					" S"+ data.getString("season") + 
					"E" + data.getString("number") + 
					": " +data.getString("title");
			title.setText(title_text);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void onBackPressed() {
		askDiscard();
	}

	public void askDiscard() {
		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
		myAlertDialog.setTitle("Discard review?");
		myAlertDialog.setMessage("Your review has not been submitted. Discard it and return to episode?");
		myAlertDialog.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						WriteReview.super.onBackPressed();
					}
				});
		myAlertDialog.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
		myAlertDialog.show();
	}
	
	public void askCreate() {
		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
		myAlertDialog.setTitle("Create review?");
		myAlertDialog.setMessage("Submit this review? Once it has been created it cannot be removed.");
		myAlertDialog.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						createReview();
					}
				});
		myAlertDialog.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
		myAlertDialog.show();
	}
	
	private void createReview() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		String title = ((EditText)findViewById(R.id.write_review_title_edit)).getText().toString();
		String content = ((EditText)findViewById(R.id.write_review_content_edit)).getText().toString();;
		String user = Account.getLoggedInUser().getSecret();
		params.add(new BasicNameValuePair("title", title));
		params.add(new BasicNameValuePair("content", content));
		params.add(new BasicNameValuePair("user_secret", user));
		
		POSTRequestTask task = new POSTRequestTask(url, params,
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						finishReview();
					}
				}
			);
			task.execute();
	}
	
	public void finishReview()
	//Run this after the review has successfully been made
	{
		Toast toast = Toast.makeText(this, 
				"Review Created", 
				Toast.LENGTH_LONG);
		toast.show();
		WriteReview.super.onBackPressed();
	}
}
