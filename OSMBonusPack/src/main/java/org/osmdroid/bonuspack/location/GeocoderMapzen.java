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
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements an equivalent to Android Geocoder class, based on OpenStreetMap data and MapZen API. <br>
 * @see <a href="https://mapzen.com/documentation/search/search/">MapZen API</a>
 *
 * Important: to use the MapZen service, you will have to request an API Key at MapZen.
 *
 * @author M.Kergall
 */
public class GeocoderMapzen {
	protected static final String SERVICE_URL = "https://search.mapzen.com/v1/";

	protected String mKey, mOptions;
    protected Locale mLocale;

	public GeocoderMapzen(String appKey, Locale locale) {
		mKey = appKey;
		mLocale = locale;
	}

	public GeocoderMapzen(String appKey) {
        this(appKey, Locale.getDefault());
	}

	static public boolean isPresent(){
		return true;
	}

	/**
	 * @param options if you want to specify additional options from MapZen API. Example: "boundary.country=fr&sources=oa"
	 */
	public void setOptions(String options){
		mOptions = options;
	}
	
	/** 
	 * Build an Android Address object from the MapZen Feature in JSON format.
	 * @return Android Address, or null if input is not valid.
	 */
	protected Address buildAndroidAddress(JsonObject jResult) throws JsonSyntaxException{
		Address gAddress = new Address(mLocale);

        JsonObject jGeometry = jResult.get("geometry").getAsJsonObject();

        JsonArray jCoordinates = jGeometry.get("coordinates").getAsJsonArray();
		gAddress.setLongitude(jCoordinates.get(0).getAsDouble());
		gAddress.setLatitude(jCoordinates.get(1).getAsDouble());

        JsonObject jProperties = jResult.get("properties").getAsJsonObject();
		int addressIndex = 0;
		if (jProperties.has("name")){
            gAddress.setAddressLine(addressIndex++, jProperties.get("name").getAsString());
            gAddress.setThoroughfare(jProperties.get("name").getAsString());
        }
		if (jProperties.has("postalcode")){
			gAddress.setAddressLine(addressIndex++, jProperties.get("postalcode").getAsString());
			gAddress.setPostalCode(jProperties.get("postalcode").getAsString());
		}
		if (jProperties.has("locality")) {
            gAddress.setAddressLine(addressIndex++, jProperties.get("locality").getAsString());
            gAddress.setLocality(jProperties.get("locality").getAsString());
        }
		if (jProperties.has("macrocounty")){ //France: departement
			gAddress.setSubAdminArea(jProperties.get("macrocounty").getAsString());
		}
		if (jProperties.has("region")){ //France: region
			gAddress.setAdminArea(jProperties.get("region").getAsString());
		}
		if (jProperties.has("country")){
			gAddress.setAddressLine(addressIndex++, jProperties.get("country").getAsString());
			gAddress.setCountryName(jProperties.get("country").getAsString());
		}
		if (jProperties.has("country_a"))
			gAddress.setCountryCode(jProperties.get("country_a").getAsString());

		//Add non-standard (but very useful) information in Extras bundle:
		Bundle extras = new Bundle();
		if (jResult.has("bbox")){
			JsonArray jBoundingBox = jResult.get("bbox").getAsJsonArray();
			BoundingBox bb = new BoundingBox(
					jBoundingBox.get(3).getAsDouble(), jBoundingBox.get(0).getAsDouble(),
					jBoundingBox.get(1).getAsDouble(), jBoundingBox.get(2).getAsDouble());
			extras.putParcelable("boundingbox", bb);
		}
		if (jProperties.has("id")){
			String osm_id = jProperties.get("id").getAsString();
			extras.putString("osm_id", osm_id);
		}
		if (jProperties.has("label")){
			String label = jProperties.get("label").getAsString();
			extras.putString("display_name", label);
		}
		gAddress.setExtras(extras);
		
		return gAddress;
	}
	
	/**
	 * Equivalent to Geocoder::getFromLocation(double latitude, double longitude, int maxResults). 
	 */
	public List<Address> getFromLocation(double latitude, double longitude, int maxResults) 
	throws IOException {
		String url = SERVICE_URL + "reverse?";
		if (mKey != null)
			url += "api_key=" + mKey + "&";
		url += "point.lat=" + latitude
    			+ "&point.lon=" + longitude
                + "&size=" + maxResults;
		url += "&lang=" + mLocale.getLanguage();
        if (mOptions != null){
            url += '&' + mOptions;
        }
		Log.d(BonusPackHelper.LOG_TAG, "GeocoderMapzen::getFromLocation:"+url);
		String result = BonusPackHelper.requestStringFromUrl(url);
		if (result == null)
			throw new IOException();
		try {
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(result);
            JsonObject jsonObj = json.getAsJsonObject();
            JsonArray jResults = jsonObj.get("features").getAsJsonArray();
			List<Address> list = new ArrayList<Address>(jResults.size());
            for (int i=0; i<jResults.size(); i++){
                JsonObject jResult = jResults.get(i).getAsJsonObject();
                Address gAddress = buildAndroidAddress(jResult);
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
	 * @param bounded true = return only results which are inside the view box. Else, view box is ignored.
	 */
	public List<Address> getFromLocationName(String locationName, int maxResults, 
			double lowerLeftLatitude, double lowerLeftLongitude, 
			double upperRightLatitude, double upperRightLongitude,
			boolean bounded)
	throws IOException {
		String url = SERVICE_URL + "search?";
		if (mKey != null)
			url += "api_key=" + mKey + "&";
		url += "size=" + maxResults
				+ "&text=" + URLEncoder.encode(locationName);
		if (bounded){
			//viewbox = left, top, right, bottom:
			url += "&boundary.rect.max_lon=" + lowerLeftLongitude
				+ "&boundary.rect.max_lat" + upperRightLatitude
				+ "&boundary.rect.min_lon" + upperRightLongitude
				+ "&boundary.rect.min_lat" + lowerLeftLatitude;
		}
		if (mOptions != null){
			url += '&' + mOptions;
		}
		Log.d(BonusPackHelper.LOG_TAG, "GeocoderMapZen::getFromLocationName:"+url);
		String result = BonusPackHelper.requestStringFromUrl(url);
		//Log.d(BonusPackHelper.LOG_TAG, result);
		if (result == null)
			throw new IOException();
		try {
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(result);
            JsonObject jsonObj = json.getAsJsonObject();
			JsonArray jResults = jsonObj.get("features").getAsJsonArray();
			List<Address> list = new ArrayList<Address>(jResults.size());
			for (int i=0; i<jResults.size(); i++){
				JsonObject jResult = jResults.get(i).getAsJsonObject();
				Address gAddress = buildAndroidAddress(jResult);
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
	 * "display_name": the address, as a single String<br>
	 */
	public List<Address> getFromLocationName(String locationName, int maxResults)
	throws IOException {
		return getFromLocationName(locationName, maxResults, 0.0, 0.0, 0.0, 0.0, false);
	}
	
}
