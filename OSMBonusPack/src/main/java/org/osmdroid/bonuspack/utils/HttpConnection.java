package org.osmdroid.bonuspack.utils;

import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * A "very very simple to use" class for performing http get and post requests. 
 * So many ways to do that, and potential subtle issues. 
 * If complexity should be added to handle even more issues, complexity should be put here and only here. 
 * 
 * Typical usage: 
 * <pre>HttpConnection connection = new HttpConnection();
 * connection.doGet("http://www.google.com");
 * InputStream stream = connection.getStream();
 * if (stream != null) {
 * 	//use this stream, for buffer reading, or XML SAX parsing, or whatever... 
 * }
 * connection.close();</pre>
 */
public class HttpConnection {

  private OkHttpClient client;
  private InputStream stream;
	private String mUserAgent;

	private final static int TIMEOUT_CONNECTION=3000; //ms
	private final static int TIMEOUT_SOCKET=10000; //ms
  private Object content;

  public HttpConnection(){
		stream = null;
    client = new OkHttpClient();
    client.setConnectTimeout(TIMEOUT_CONNECTION, TimeUnit.MILLISECONDS);
    client.setReadTimeout(TIMEOUT_SOCKET, TimeUnit.MILLISECONDS);
  }

	public void setUserAgent(String userAgent){
		mUserAgent = userAgent;
	}

	public void doGet(final String url) {
    try {
      Request.Builder request = new Request.Builder().url(url);
      if (mUserAgent != null)
        request.addHeader("User-Agent", mUserAgent);
      Response response = client.newCall(request.build()).execute();
      Integer status = response.code();
      if (status != 200) {
        Log.e(BonusPackHelper.LOG_TAG, "Invalid response from server: " + status.toString());
      } else {
        content = response.body().string();
        stream = response.body().byteStream();
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

	/**
	 * @return the opened InputStream, or null if creation failed for any reason.
	 */
	public InputStream getStream() {
		return stream;
	}
	
	/**
	 * @return the whole content as a String, or null if creation failed for any reason. 
	 */
	public String getContentAsString(){
    if(content != null) return content.toString();
    else return null;
	}
	
	/**
	 * Calling close once is mandatory. 
	 */
	public void close(){
		if (stream != null){
			try { 
				stream.close();
				stream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    if (client != null) client = null;
	}
	
}
