package edu.wwu.cs412.tvfanatic;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CommentAdapter extends ArrayAdapter<Comment> {

	Context context;
	int layoutResourceId;
	ArrayList<Comment> data;
	
	public CommentAdapter(Context context, int layoutResourceId, ArrayList<Comment> data) 
	{
		super(context, layoutResourceId, data);
		this.context = context;
		this.layoutResourceId = layoutResourceId;
		this.data = data;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		CommentHolder holder = null;
		
		if (row == null)
		{
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new CommentHolder();
			holder.content = (TextView)row.findViewById(R.id.comment_content);
			holder.user = (TextView)row.findViewById(R.id.comment_user);
			row.setTag(holder);
		}
		else
		{
			holder = (CommentHolder)row.getTag();
		}
		
		Comment comment = data.get(position);
		
		String atString = "";
		if (!comment.parent_name.equals("null")) {
			atString = "<b>@" + comment.parent_name + ": </b>";
		}
		holder.content.setText(Html.fromHtml(atString + comment.text));
		holder.user.setText(Html.fromHtml(comment.user_name));
		
		return row;
		
	}
	
	static class CommentHolder
	{
		TextView content;
		TextView user;
		TextView to;
	}
}
