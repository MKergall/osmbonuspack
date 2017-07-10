package org.osmdroid.bonuspack.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import org.osmdroid.util.BoundingBox;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

/** Useful functions and common constants. 
 * @author M.Kergall
 */
public class BonusPackHelper {

	/** Log tag. */
	public static final String LOG_TAG = "BONUSPACK";
	
	/** resource id value meaning "undefined resource id" */
	public static final int UNDEFINED_RES_ID = 0;
	
	/**	User agent sent to services by default */
	public static final String DEFAULT_USER_AGENT = "OsmBonusPack/1";
	
	/** @return true if the device is the emulator, false if actual device. 
	 */
	public static boolean isEmulator(){
		//return Build.MANUFACTURER.equals("unknown");
		return ("google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT));
	}

	/**
	 * @return the whole content of the http request, as a string
	 */
	private static String readStream(HttpConnection connection){
		return connection.getContentAsString();
	}

	/** sends an http request, and returns the whole content result in a String
	 * @param url
	 * @param userAgent
	 * @return the whole content, or null if any issue.
	 */
	public static String requestStringFromUrl(String url, String userAgent) {
		HttpConnection connection = new HttpConnection();
		if (userAgent != null)
			connection.setUserAgent(userAgent);
		connection.doGet(url);
		String result = connection.getContentAsString();
		connection.close();
		return result;
	}

	/** sends an http request, and returns the whole content result in a String.
	 * @param url
	 * @return the whole content, or null if any issue. 
	 */
	public static String requestStringFromUrl(String url) {
		return requestStringFromUrl(url, null);
	}

	/**
	 * Loads a bitmap from a url. 
	 * @param url
	 * @return the bitmap, or null if any issue. 
	 */
	public static Bitmap loadBitmap(String url) {
		Bitmap bitmap;
		try {
			InputStream is = (InputStream) new URL(url).getContent();
			if (is == null)
				return null;
			bitmap = BitmapFactory.decodeStream(new FlushedInputStream(is));
			//Alternative providing better handling on loading errors?
			/*
			Drawable d = Drawable.createFromStream(new FlushedInputStream(is), null);
			if (is != null)
				is.close();
			if (d != null)
				bitmap = ((BitmapDrawable)d).getBitmap();
			*/
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}

	/**
	 * Workaround on Android issue on bitmap loading
	 * @see <a href="http://stackoverflow.com/questions/4601352/createfromstream-in-android-returning-null-for-certain-url">Issue</a>
	 */
	static class FlushedInputStream extends FilterInputStream {
	    public FlushedInputStream(InputStream inputStream) {
	    	super(inputStream);
	    }

	    @Override public long skip(long n) throws IOException {
	        long totalBytesSkipped = 0L;
	        while (totalBytesSkipped < n) {
	            long bytesSkipped = in.skip(n - totalBytesSkipped);
	            if (bytesSkipped == 0L) {
	                  int byteValue = read();
	                  if (byteValue < 0) {
	                      break;  // we reached EOF
	                  } else {
	                      bytesSkipped = 1; // we read one byte
	                  }
	           }
	           totalBytesSkipped += bytesSkipped;
	        }
	        return totalBytesSkipped;
	    }
	}

//	/**
//	 * Parse a string-array resource with items like this: <item>key|value</item>
//	 * @param ctx
//	 * @param stringArrayResourceId
//	 * @return the keys=>values as an HashMap
//	 */
	public static HashMap<String, String> parseStringMapResource(Context ctx, int stringArrayResourceId) {
	    String[] stringArray = ctx.getResources().getStringArray(stringArrayResourceId);
	    HashMap<String, String> map = new HashMap<>(stringArray.length);
	    for (String entry : stringArray) {
	        String[] splitResult = entry.split("\\|", 2);
	        map.put(splitResult[0], splitResult[1]);
	    }
	    return map;
	}
}
