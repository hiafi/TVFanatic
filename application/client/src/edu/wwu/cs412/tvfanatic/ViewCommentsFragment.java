package edu.wwu.cs412.tvfanatic;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;
import edu.wwu.cs412.tvfanatic.http.GETRequestTask;
import edu.wwu.cs412.tvfanatic.util.StringUtil;
import edu.wwu.cs412.tvfanatic.account.Account;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class ViewCommentsFragment extends Fragment {
	
	View view;
	
	public ViewCommentsFragment() {
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.view_comments, container, false);
        return view;
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
	}
	
	public void sendRequest(int review_id)
	{
		JSONObject get_params = new JSONObject();
		try {
			get_params.put("user_secret", Account.getLoggedInUser().getSecret());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GETRequestTask task = new GETRequestTask(
				StringUtil.buildUrl(Constants.API_URL + "comment/"+review_id, get_params),
				new AsyncTaskCompleteListener<JSONObject>() {
					public void onTaskComplete(JSONObject result) {
						try {
							JSONArray json = new JSONArray(result.getString("data"));
							setComments(json);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				});
		task.execute();
	}
	
	public void setComments(JSONArray json)
	{
		final ArrayList<Comment> list = Comment.createList(json);
		CommentAdapter adapter = new CommentAdapter(this.getActivity(), R.layout.comment_row, list);
		ListView commentsView = (ListView) getActivity().findViewById(R.id.comments_list_view);
        commentsView.setAdapter(adapter);
        
		ListView list_view = (ListView)view.findViewById(R.id.comments_list_view);
		final ViewReview activity = (ViewReview)this.getActivity();
		        
	    list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	    	{
	    		Comment com = list.get(position);
	    		activity.setCommentTarget(com.comment_id, com.user_name);
	    	}
	    });
	}
}
