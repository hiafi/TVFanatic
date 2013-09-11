package edu.wwu.cs412.tvfanatic.util;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class StringUtil {
	public static String truncate(String text, int maxChars, boolean ellipses) {
		if (text != null && text.length() > maxChars) {
			if (ellipses)
				maxChars -= 3;
			String rString = text.substring(0, maxChars);
			if (ellipses)
				rString += "...";
			return rString;
		} else {
			return text;
		}
	}
	
	public static String buildUrl(String base_url, JSONObject get_params)
	{
		String newUrl = base_url;
    	if (get_params != null)
    	{
	    	Iterator<String> paramIter = get_params.keys();
	    	if (paramIter.hasNext())
	    	{
	    		newUrl += "?";
				while (paramIter.hasNext())
				{
					String param = paramIter.next();
					try {
						newUrl += param + "=" + get_params.getString(param);
						if (paramIter.hasNext())
						{
							newUrl += "&";
						}
					} catch (JSONException e) {
						Log.e("url error", "Invalid Parameter");
					}
				}
	    	}
    	}
    	return newUrl;
	}
}
