package edu.wwu.cs412.tvfanatic.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class POSTRequestTask extends AsyncTask<Void, Integer, JSONObject> {
	private int progress = 0;
	
	String url;
	List<NameValuePair> params;

	private AsyncTaskCompleteListener<JSONObject> callback;

	public POSTRequestTask(String url, List<NameValuePair> params,
							AsyncTaskCompleteListener<JSONObject> callback) {
		this.callback = callback;
		this.url = url;
		this.params = params;
	}

	@Override
	protected JSONObject doInBackground(Void... none) {
		return sendRequest(this.url, this.params);
	}

	private void setProgress(int value) {
		this.progress = value;
		publishProgress(this.progress);
	}

	protected void onPostExecute(JSONObject result) {
		callback.onTaskComplete(result);
	}

	private JSONObject sendRequest(String url, List<NameValuePair> params) {
		HttpClient httpClient = CustomClient.getHttpClient();
		try {
			HttpPost request = new HttpPost(url);
			setProgress(25);
			
			request.setEntity(new UrlEncodedFormEntity(params));
			setProgress(50);
			
			HttpResponse response = httpClient.execute(request);
			setProgress(75);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			String recieved = reader.readLine();
			JSONObject json;
			try
			{
				json = new JSONObject(recieved);
			}
			catch (JSONException j)
			{
				//We didnt recieve a json as expected (probably a string), so return {data:return value}
				json = new JSONObject();
				//Recreate our reader so we read from the top again
				json.put("data", recieved);
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
