package edu.wwu.cs412.tvfanatic.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class GETRequestTask extends AsyncTask<Void, Integer, JSONObject> {

	private static final int timeout = 60 * 1000;
	private int progress = 0;
	String url;

	private AsyncTaskCompleteListener<JSONObject> callback;

	public GETRequestTask(String url,
			AsyncTaskCompleteListener<JSONObject> callback) {
		this.callback = callback;
		this.url = url;
	}

	@Override
	protected JSONObject doInBackground(Void... none) {
		return sendRequest(this.url);
	}

	private void setProgress(int value) {
		this.progress = value;
		publishProgress(this.progress);
	}

	protected void onPostExecute(JSONObject result) {
		callback.onTaskComplete(result);
	}

	private JSONObject sendRequest(String url) {
		HttpClient httpClient = CustomClient.getHttpClient();
		try {
			HttpGet request = new HttpGet(url);
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(params, timeout);
			request.setParams(params);
			setProgress(33);

			// Check cancellation before request
			if (this.isCancelled())
				return null;
			
			HttpResponse response = httpClient.execute(request);
			
			// Check cancellation immediately after a request
			if (this.isCancelled())
				return null;
			
			setProgress(67);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			
			// Check cancellation before JSON parsing
			if (this.isCancelled())
				return null;
			
			JSONObject json;
			String received = reader.readLine();
			try
			{
				json = new JSONObject(received);
			}
			catch (JSONException j)
			{
				//We didnt get a json, return a string under data
				json = new JSONObject();
				json.put("data", received);
			}
			
			setProgress(100);
			return json;

		} catch (IOException e) {
			Log.d("Error", "Http");
			e.printStackTrace();
			return null;
		} catch (JSONException j) {
			Log.d("Error", "JSON");
			j.printStackTrace();
			return null;
		}
	}

}
