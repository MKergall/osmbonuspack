package org.osmdroid.bonuspack.location;

import android.location.Address;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements an equivalent to Android Geocoder class, based on OpenStreetMap data and GraphHopper API. <br>
 * @see <a href="https://graphhopper.com/api/1/docs/geocoding/">GraphHopper Reference</a>
 *
 * Important: to use the public GraphHopper service, you will have to get an API key.
 *
 * @author M.Kergall
 */
public class GeocoderGraphHopper {
	public static final String GRAPHHOPPER_SERVICE_URL = "https://graphhopper.com/api/1/geocode?";

	protected Locale mLocale;
	protected String mServiceUrl, mKey;

	public GeocoderGraphHopper(Locale locale, String appKey) {
		mLocale = locale;
		setService(GRAPHHOPPER_SERVICE_URL); //default service
		mKey = appKey;;
	}

	static public boolean isPresent(){
		return true;
	}
	
	/**
	 * Specify the url of the service provider to use. Can be your local instance for instance.
	 */
	public void setService(String serviceUrl){
		mServiceUrl = serviceUrl;
	}

	/**
	 * Build an Android Address object from the GraphHopper address in JSON format.
	 * @return Android Address, or null if input is not valid.
	 */
	protected Address buildAndroidAddress(JsonObject jResult) throws JsonSyntaxException{
		Address gAddress = new Address(mLocale);
		String displayName = "";
		if (!jResult.has("point") || !jResult.has("name"))
			return null;

		JsonObject jPoint = jResult.get("point").getAsJsonObject();
		gAddress.setLatitude(jPoint.get("lat").getAsDouble());
		gAddress.setLongitude(jPoint.get("lng").getAsDouble());

		int addressIndex = 0;
		if (jResult.has("name")){
			gAddress.setAddressLine(addressIndex++, jResult.get("name").getAsString());
			gAddress.setThoroughfare(jResult.get("name").getAsString());
			displayName = jResult.get("name").getAsString();
		}
		if (jResult.has("postcode")){
			gAddress.setAddressLine(addressIndex++, jResult.get("postcode").getAsString());
			gAddress.setPostalCode(jResult.get("postcode").getAsString());
			displayName += ", " + jResult.get("postcode").getAsString();
		}
		
		if (jResult.has("city")) {
			gAddress.setAddressLine(addressIndex++, jResult.get("city").getAsString());
			gAddress.setLocality(jResult.get("city").getAsString());
			displayName += ", " + jResult.get("city").getAsString();
		}
		if (jResult.has("state")){ //France: region
			gAddress.setAdminArea(jResult.get("state").getAsString());
		}
		if (jResult.has("country")){
			gAddress.setAddressLine(addressIndex++, jResult.get("country").getAsString());
			gAddress.setCountryName(jResult.get("country").getAsString());
			displayName += ", " + jResult.get("country").getAsString();
		}

		//Add non-standard (but very useful) information in Extras bundle:
		Bundle extras = new Bundle();
		if (jResult.has("extent")){
			JsonArray jBoundingBox = jResult.get("extent").getAsJsonArray();
			BoundingBox bb = new BoundingBox(
					jBoundingBox.get(3).getAsDouble(), jBoundingBox.get(1).getAsDouble(),
					jBoundingBox.get(1).getAsDouble(), jBoundingBox.get(2).getAsDouble());
			extras.putParcelable("boundingbox", bb);
		}
		if (jResult.has("osm_id")){
			long osm_id = jResult.get("osm_id").getAsLong();
			extras.putLong("osm_id", osm_id);
		}
		if (jResult.has("osm_type")){
			String osm_type = jResult.get("osm_type").getAsString();
			extras.putString("osm_type", osm_type);
		}
		extras.putString("display_name", displayName);
		gAddress.setExtras(extras);
		
		return gAddress;
	}
	
	/**
	 * Equivalent to Geocoder::getFromLocation(double latitude, double longitude, int maxResults). 
	 */
	public List<Address> getFromLocation(double latitude, double longitude, int maxResults) 
	throws IOException {
		String url = mServiceUrl + "reverse=true";
		if (mKey != null)
			url += "&key=" + mKey;
		url += "&locale=" + mLocale.getLanguage()
				+ "&limit=" + maxResults
				+ "&point=" + latitude + "," + longitude;
		Log.d(BonusPackHelper.LOG_TAG, "GeocoderGraphHopper::getFromLocation:"+url);
		String result = BonusPackHelper.requestStringFromUrl(url);
		if (result == null)
			throw new IOException();
		try {
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(result);
			JsonObject jResult = json.getAsJsonObject();
			JsonArray jHits = jResult.getAsJsonArray("hits");
			List<Address> list = new ArrayList<Address>(jHits.size());
			for (int i = 0; i < jHits.size(); i++) {
				Address gAddress = buildAndroidAddress(jHits.get(i).getAsJsonObject());
				if (gAddress != null)
					list.add(gAddress);
			}
			return list;
		} catch (JsonSyntaxException e) {
			throw new IOException();
		}
	}

	/**
	 * Equivalent to Geocoder::getFromLocation(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude)
	 * but adding bounded parameter. 
	 * @param bounded true = return only results which are inside the view box; false = view box is used as a preferred area to find search results. 
	 */
	public List<Address> getFromLocationName(String locationName, int maxResults, 
			double lowerLeftLatitude, double lowerLeftLongitude, 
			double upperRightLatitude, double upperRightLongitude,
			boolean bounded)
	throws IOException {
		String url = mServiceUrl;
		if (mKey != null)
			url += "key=" + mKey + "&";
		url += "locale=" + mLocale.getLanguage()
				+ "&limit=" + maxResults
				+ "&q=" + URLEncoder.encode(locationName);
		if (lowerLeftLatitude != 0.0 && upperRightLatitude != 0.0){
			url += "&point=" + (upperRightLatitude+upperRightLongitude/2) + ","
				+ (lowerLeftLongitude+lowerLeftLongitude)/2;
		}
		Log.d(BonusPackHelper.LOG_TAG, "GeocoderGraphHopper::getFromLocationName:"+url);
		String result = BonusPackHelper.requestStringFromUrl(url);
		//Log.d(BonusPackHelper.LOG_TAG, result);
		if (result == null)
			throw new IOException();
		try {
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(result);
			JsonObject jResult = json.getAsJsonObject();
			JsonArray jHits = jResult.getAsJsonArray("hits");
			List<Address> list = new ArrayList<Address>(jHits.size());
			for (int i=0; i<jHits.size(); i++){
				JsonObject jAddress = jHits.get(i).getAsJsonObject();
				Address gAddress = buildAndroidAddress(jAddress);
				if (gAddress != null)
					list.add(gAddress);
			}
			//Log.d(BonusPackHelper.LOG_TAG, "done");
			return list;
		} catch (JsonSyntaxException e) {
			throw new IOException();
		}
	}
	
	/**
	 * Equivalent to Geocoder::getFromLocation(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude)
	 * @see #getFromLocationName(String locationName, int maxResults) about extra data added in Address results. 
	 */
	public List<Address> getFromLocationName(String locationName, int maxResults, 
			double lowerLeftLatitude, double lowerLeftLongitude, 
			double upperRightLatitude, double upperRightLongitude)
	throws IOException {
		return getFromLocationName(locationName, maxResults, 
				lowerLeftLatitude, lowerLeftLongitude, 
				upperRightLatitude, upperRightLongitude, true);
	}

	/**
	 * Equivalent to Geocoder::getFromLocation(String locationName, int maxResults). <br>
	 * 
	 * Some useful information, returned by Nominatim, that doesn't fit naturally within Android Address, are added in the bundle Address.getExtras():<br>
	 * "boundingbox": the enclosing bounding box, as a BoundingBox<br>
	 * "osm_id": the OSM id, as a long<br>
	 * "osm_type": one of the 3 OSM types, as a string (node, way, or relation). <br>
	 * "display_name": the address, as a single String<br>
	 * "polygonpoints": the enclosing polygon of the location (depending on setOptions usage), as an ArrayList of GeoPoint<br>
	 */
	public List<Address> getFromLocationName(String locationName, int maxResults)
	throws IOException {
		return getFromLocationName(locationName, maxResults, 0.0, 0.0, 0.0, 0.0, false);
	}
	
}
