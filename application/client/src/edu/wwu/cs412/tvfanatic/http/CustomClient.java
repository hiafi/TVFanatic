package edu.wwu.cs412.tvfanatic.http;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class CustomClient {

	private static HttpClient customHttpClient;
	
	private static final int manager_timeout = 1000 * 1; //1000 * 60 milliseconds
	private static final int connection_timeout = 1000 * 5; //1000 * 60 milliseconds
	private static final int so_timeout = 1000 * 10; //1000 * 60 milliseconds
	
	// Prevent instantiation by other classes
	private CustomClient() {}
	
	public static synchronized HttpClient getHttpClient() {
		if (customHttpClient == null) {
			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpProtocolParams.setUserAgent(params, "Mozill/5.0 (Linux; U; Android 2.2.1; " +
					"en-us; Nexus One Build/FRG83) AplleWebKit/533.1 (KHTML, like Gecko) " +
					"Version/4.0 Mobile Safari/533.1");
			ConnManagerParams.setTimeout(params, manager_timeout);
			HttpConnectionParams.setConnectionTimeout(params, connection_timeout);
			HttpConnectionParams.setSoTimeout(params, so_timeout);
			
			SchemeRegistry schReg = new SchemeRegistry();
			schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
			ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
			customHttpClient = new DefaultHttpClient(conMgr, params);
		}
		return customHttpClient;
	}
}
