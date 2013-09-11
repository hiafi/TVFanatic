package edu.wwu.cs412.tvfanatic;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Comment
{
	public String text;
	public int user_id;
	public String user_name;
	public int parent_id = -1;
	public int comment_id;
	public String parent_name;
	
	public Comment(JSONObject json)
	{
		try {
			this.text = json.getString("content");
			JSONObject user = json.getJSONObject("user");
			this.user_id = user.getInt("id");
			this.comment_id = json.getInt("id");
			this.user_name = user.getString("display_name");
			this.parent_id = -1;
			this.parent_name = null;
			try { this.parent_id = json.getInt("parent_id"); }
				catch (JSONException e) {}
			try { this.parent_name = json.getString("parent_name"); }
				catch (JSONException e) {}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				throw e;
			} catch (JSONException e1) { e1.printStackTrace(); }
		}
	}
	
	public static ArrayList<Comment> createList(JSONArray json)
	{
		ArrayList<Comment> list = new ArrayList<Comment>();
		for (int i = 0; i < json.length(); i++)
		{
			try {
				list.add(new Comment(json.getJSONObject(i)));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list;
	}
}
