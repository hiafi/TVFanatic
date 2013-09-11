package edu.wwu.cs412.tvfanatic;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EpisodeGridAdapter extends BaseAdapter {
	public static class EpisodeGridData {
		public int id;
		public String url;
		public String title;
		public String dateAired;
		public int numReviews;
		public int episodeNumber;
	}
	
	private static final String TAG = "EpisodeGridAdapter";
	
	private List<EpisodeGridData> data;
	private LayoutInflater inflater;
	private ViewGroup grid;
	
	public EpisodeGridAdapter(Context context, List<EpisodeGridData> dataList) {
		this.data = dataList;
		this.inflater = LayoutInflater.from(context);
	}

	public EpisodeGridAdapter(Context context, ViewGroup grid, List<EpisodeGridData> dataList) {
		this.data = dataList;
		this.grid = grid;
		this.inflater = LayoutInflater.from(context);
	}
	
	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return data.get(position);
	}

	public long getItemId(int position) {
		return data.get(position).id;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (convertView == null)
			view = inflater.inflate(R.layout.season_grid_episode_element, null);
		
		EpisodeGridData data = (EpisodeGridData) getItem(position);
		TextView titleView = (TextView) view.findViewById(R.id.show_epi_title);
		TextView dateAiredView = (TextView) view.findViewById(R.id.show_epi_date_aired);
		TextView episodeNumberView = (TextView) view.findViewById(R.id.show_epi_number);
		TextView numReviewsView = (TextView) view.findViewById(R.id.show_epi_num_reviews);
		
		// Set values in element
		titleView.setText(data.title);
		dateAiredView.setText(data.dateAired);
		episodeNumberView.setText(Integer.toString(data.episodeNumber));
		numReviewsView.setText(data.numReviews + " Reviews");
		
		return view;
	}
}
