package edu.wwu.cs412.tvfanatic;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import edu.wwu.cs412.tvfanatic.account.Account;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.POSTRequestTask;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WriteComment extends Fragment {

	View view;
	
	public int review_id = -1;
	
	int target_id = -1;
	String target_name = "Review";
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.write_comment, container, false);
        
        Button btn = (Button)view.findViewById(R.id.write_comment_done_button);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                askCreate();
            }
		});
		((EditText)view.findViewById(R.id.write_comment_content_edit)).clearFocus();
		((EditText)view.findViewById(R.id.write_comment_content_edit)).setSelected(false);
		
		return view;
    }
	
	public void setTarget(int id, String name)
	{
		target_id = id;
		target_name = name;
		((TextView)view.findViewById(R.id.write_comment_header_value)).setText(name);
		((EditText)view.findViewById(R.id.write_comment_content_edit)).setSelected(true);
	}
	
	private void askCreate()
	{
		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this.getActivity());
		myAlertDialog.setTitle("Create comment?");
		myAlertDialog.setMessage("Submit this comment? Once it has been created it cannot be removed.");
		myAlertDialog.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						createComment();
					}
				});
		myAlertDialog.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
		myAlertDialog.show();
	}
	
	private void createComment()
	{
		String url = Constants.API_URL + "comment/" + review_id;
		if (target_id >= 0)
		{
			url += "/" + target_id;
		}
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		String content = ((EditText)view.findViewById(R.id.write_comment_content_edit)).getText().toString();
		String user = Account.getLoggedInUser().getSecret();
		params.add(new BasicNameValuePair("comment", content));
		params.add(new BasicNameValuePair("user_secret", user));
		
		POSTRequestTask task = new POSTRequestTask(url, params,
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						finishComment();
					}
				}
			);
			task.execute();
	}
	
	private void finishComment()
	{
		Toast toast = Toast.makeText(this.getActivity(), 
				"Comment Created", 
				Toast.LENGTH_LONG);
		toast.show();
		EditText comment_field = (EditText)view.findViewById(R.id.write_comment_content_edit);
		comment_field.setText("");
		comment_field.setSelected(false);
		InputMethodManager inputManager = (InputMethodManager)
                this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE); 

		inputManager.hideSoftInputFromWindow(this.getActivity().getCurrentFocus().getWindowToken(),
		                   InputMethodManager.HIDE_NOT_ALWAYS);
		((ViewReview)this.getActivity()).sendRequest();
	}
}
