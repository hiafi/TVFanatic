package edu.wwu.cs412.tvfanatic.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

public class DateUtil {
	private static final String TAG = "DateUtil";
	
	private static final SimpleDateFormat tvdbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat tvfDateFormat = new SimpleDateFormat("M.d.yyyy");
	
	public static String formatDate(String tvdbDateStr) {
		return toTvfFormat(fromTvdbFormat(tvdbDateStr));
	}
	
	public static Date fromTvdbFormat(String tvdbDateStr) {
		synchronized (tvdbDateFormat) {
			try {
				return tvdbDateFormat.parse(tvdbDateStr);
			} catch (ParseException e) {
				Log.e(TAG, "Failed to parse TVDB date: " + tvdbDateStr, e);
				return null;
			}
		}
	}
	
	public static String toTvfFormat(Date date) {
		synchronized (tvdbDateFormat) {
			return tvfDateFormat.format(date);
		}
	}
}
