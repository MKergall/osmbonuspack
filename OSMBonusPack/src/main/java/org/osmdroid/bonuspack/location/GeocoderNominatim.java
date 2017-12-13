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
import org.osmdroid.bonuspack.utils.StatusException;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements an equivalent to Android Geocoder class, based on OpenStreetMap data and Nominatim API. <br>
 * @see <a href="http://wiki.openstreetmap.org/wiki/Nominatim">Nominatim Reference</a>
 * @see <a href="http://open.mapquestapi.com/nominatim/">Nominatim at MapQuest Open</a>
 *
 * Important: to use the public Nominatim service, you will have to define a user agent,
 * and adhere to the <a href="http://wiki.openstreetmap.org/wiki/Nominatim_usage_policy">Nominatim usage policy</a>.
 *
 * @author M.Kergall
 */
public class GeocoderNominatim {
	public static final String NOMINATIM_SERVICE_URL = "http://nominatim.openstreetmap.org/";
	public static final String MAPQUEST_SERVICE_URL = "http://open.mapquestapi.com/nominatim/v1/";
	
	protected Locale mLocale;
	protected String mServiceUrl, mKey;
	protected String mUserAgent;
	protected boolean mPolygon;

	public GeocoderNominatim(Locale locale, String userAgent) {
		mLocale = locale;
		setOptions(false);
		setService(NOMINATIM_SERVICE_URL); //default service
		mUserAgent = userAgent;
	}

	public GeocoderNominatim(String userAgent) {
		this(Locale.getDefault(), userAgent);
	}

	static public boolean isPresent(){
		return true;
	}
	
	/**
	 * Specify the url of the Nominatim service provider to use. 
	 * Can be one of the predefined (NOMINATIM_SERVICE_URL or MAPQUEST_SERVICE_URL), 
	 * or another one, your local instance of Nominatim for instance. 
	 */
	public void setService(String serviceUrl){
		mServiceUrl = serviceUrl;
	}

	/**
	 * Set AppKey for MapQuest open service
	 */
	public void setKey(String appKey) {
		mKey = appKey;
	}

	/**
	 * @param polygon true to get the polygon enclosing the location. 
	 */
	public void setOptions(boolean polygon){
		mPolygon = polygon;
	}
	
	/** 
	 * Build an Android Address object from the Nominatim address in JSON format. 
	 * Current implementation is mainly targeting french addresses,
	 * and will be quite basic on other countries.
	 * @return Android Address, or null if input is not valid.
	 */
	protected Address buildAndroidAddress(JsonObject jResult) throws JsonSyntaxException{
		Address gAddress = new Address(mLocale);
		if (!jResult.has("lat") || !jResult.has("lon") || !jResult.has("address"))
			return null;

		gAddress.setLatitude(jResult.get("lat").getAsDouble());
		gAddress.setLongitude(jResult.get("lon").getAsDouble());
		JsonObject jAddress = jResult.get("address").getAsJsonObject();

		int addressIndex = 0;
		if (jAddress.has("road")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("road").getAsString());
			gAddress.setThoroughfare(jAddress.get("road").getAsString());
		} else if (jAddress.has("pedestrian")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("pedestrian").getAsString());
			gAddress.setThoroughfare(jAddress.get("pedestrian").getAsString());
		} else if (jAddress.has("footway")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("footway").getAsString());
			gAddress.setThoroughfare(jAddress.get("footway").getAsString());
		} else if (jAddress.has("cycleway")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("cycleway").getAsString());
			gAddress.setThoroughfare(jAddress.get("cycleway").getAsString());
		} else if (jAddress.has("bridleway")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("bridleway").getAsString());
			gAddress.setThoroughfare(jAddress.get("bridleway").getAsString());
		} else if (jAddress.has("highway")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("highway").getAsString());
			gAddress.setThoroughfare(jAddress.get("highway").getAsString());
		} else if (jAddress.has("address26")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("address26").getAsString());
			gAddress.setThoroughfare(jAddress.get("address26").getAsString());
		}

		/** since in documentations {@link Address#getSubThoroughfare()} says:
		 *  "This may correspond to the street number of the address" */
		if (jAddress.has("house_number"))
			gAddress.setSubThoroughfare(jAddress.get("house_number").getAsString());

		if (jAddress.has("suburb")){
			//gAddress.setAddressLine(addressIndex++, jAddress.getString("suburb"));
				//not kept => often introduce "noise" in the address.
			gAddress.setSubLocality(jAddress.get("suburb").getAsString());
		} else if (jAddress.has("sub-city")){
			gAddress.setSubLocality(jAddress.get("sub-city").getAsString());
		}

		if (jAddress.has("postcode")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("postcode").getAsString());
			gAddress.setPostalCode(jAddress.get("postcode").getAsString());
		}
		
		if (jAddress.has("city")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("city").getAsString());
			gAddress.setLocality(jAddress.get("city").getAsString());
		} else if (jAddress.has("town")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("town").getAsString());
			gAddress.setLocality(jAddress.get("town").getAsString());
		} else if (jAddress.has("village")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("village").getAsString());
			gAddress.setLocality(jAddress.get("village").getAsString());
		}
		
		if (jAddress.has("county")){ //France: departement
			gAddress.setSubAdminArea(jAddress.get("county").getAsString());
		} else if (jAddress.has("departement")){
			gAddress.setSubAdminArea(jAddress.get("departement").getAsString());
		}
		if (jAddress.has("state")){ //France: region
			gAddress.setAdminArea(jAddress.get("state").getAsString());
		} else if (jAddress.has("region")){
			gAddress.setAdminArea(jAddress.get("region").getAsString());
		}
		if (jAddress.has("country")){
			gAddress.setAddressLine(addressIndex++, jAddress.get("country").getAsString());
			gAddress.setCountryName(jAddress.get("country").getAsString());
		}
		if (jAddress.has("country_code"))
			gAddress.setCountryCode(jAddress.get("country_code").getAsString());

		/* Other possible OSM tags in Nominatim results not handled yet:
		 * state_district, ...
		*/
		
		//Add non-standard (but very useful) information in Extras bundle:
		Bundle extras = new Bundle();
		if (jResult.has("polygonpoints")){
			JsonArray jPolygonPoints = jResult.get("polygonpoints").getAsJsonArray();
			ArrayList<GeoPoint> polygonPoints = new ArrayList<>(jPolygonPoints.size());
			for (int i=0; i<jPolygonPoints.size(); i++){
				JsonArray jCoords = jPolygonPoints.get(i).getAsJsonArray();
				double lon = jCoords.get(0).getAsDouble();
				double lat = jCoords.get(1).getAsDouble();
				GeoPoint p = new GeoPoint(lat, lon);
				polygonPoints.add(p);
			}
			extras.putParcelableArrayList("polygonpoints", polygonPoints);
		}
		if (jResult.has("boundingbox")){
			JsonArray jBoundingBox = jResult.get("boundingbox").getAsJsonArray();
			BoundingBox bb = new BoundingBox(
					jBoundingBox.get(1).getAsDouble(), jBoundingBox.get(2).getAsDouble(), 
					jBoundingBox.get(0).getAsDouble(), jBoundingBox.get(3).getAsDouble());
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
		if (jResult.has("display_name")){
			String display_name = jResult.get("display_name").getAsString();
			extras.putString("display_name", display_name);
		}
		if (jResult.has("place_id")){
			String place_id = jResult.get("place_id").getAsString();
			extras.putString("place_id", place_id);
		}
		if (jResult.has("type")){
			String type = jResult.get("type").getAsString();
			extras.putString("type", type);
		}
		if (jResult.has("licence")){
			String licence = jResult.get("licence").getAsString();
			extras.putString("licence", licence);
		}
		//would be nice to have all location parts if needed
		if (jAddress.has("city")){
			String city = jAddress.get("city").getAsString();
			extras.putString("city", city);
		}
		if (jAddress.has("town")){
			String town = jAddress.get("town").getAsString();
			extras.putString("town", town);
		}
		if (jAddress.has("village")){
			String village = jAddress.get("village").getAsString();
			extras.putString("village", village);
		}
		if (jAddress.has("subway")){
			String subway = jAddress.get("subway").getAsString();
			extras.putString("subway", subway);
		}
		if (jAddress.has("golf_course")){
			String golf_course = jAddress.get("golf_course").getAsString();
			extras.putString("golf_course", golf_course);
		}
		if (jAddress.has("bus_stop")){
			String bus_stop = jAddress.get("bus_stop").getAsString();
			extras.putString("bus_stop", bus_stop);
		}
		if (jAddress.has("parking")){
			String parking = jAddress.get("parking").getAsString();
			extras.putString("parking", parking);
		}
		if (jAddress.has("house")){
			String house = jAddress.get("house").getAsString();
			extras.putString("house", house);
		}
		if (jAddress.has("building")){
			String building = jAddress.get("building").getAsString();
			extras.putString("building", building);
		}
		if (jAddress.has("city_district")){
			String city_district = jAddress.get("city_district").getAsString();
			extras.putString("city_district", city_district);
		}
		if (jAddress.has("pedestrian")){
			String highway = jAddress.get("pedestrian").getAsString();
			extras.putString("pedestrian", highway);
		}
		if (jAddress.has("footway")){
			String highway = jAddress.get("footway").getAsString();
			extras.putString("footway", highway);
		}
		if (jAddress.has("highway")){
			String highway = jAddress.get("highway").getAsString();
			extras.putString("highway", highway);
		}
		if (jAddress.has("sub-city")){
			String sub_city = jAddress.get("sub-city").getAsString();
			extras.putString("sub-city", sub_city);
		}
		if (jAddress.has("locality")){
			String locality = jAddress.get("locality").getAsString();
			extras.putString("locality", locality);
		}
		if (jAddress.has("isolated_dwelling")){
			String isolated_dwelling = jAddress.get("isolated_dwelling").getAsString();
			extras.putString("isolated_dwelling", isolated_dwelling);
		}
		if (jAddress.has("cycleway")){
			String cycleway = jAddress.get("cycleway").getAsString();
			extras.putString("cycleway", cycleway);
		}
		if (jAddress.has("hamlet")){
			String hamlet = jAddress.get("hamlet").getAsString();
			extras.putString("hamlet", hamlet);
		}
		if (jAddress.has("region")){
			String region = jAddress.get("region").getAsString();
			extras.putString("region", region);
		}
		if (jAddress.has("departement")){
			String departement = jAddress.get("departement").getAsString();
			extras.putString("departement", departement);
		}
		if (jAddress.has("neighbourhood")){
			String neighbourhood = jAddress.get("neighbourhood").getAsString();
			extras.putString("neighbourhood", neighbourhood);
		}
		if (jAddress.has("residential")){
			String residential = jAddress.get("residential").getAsString();
			extras.putString("residential", residential);
		}

		gAddress.setExtras(extras);
		
		return gAddress;
	}
	
	/**
	 * Equivalent to Geocoder::getFromLocation(double latitude, double longitude, int maxResults). 
	 */
	public List<Address> getFromLocation(double latitude, double longitude, int maxResults) throws IOException, StatusException {
		String url = mServiceUrl + "reverse?";
		if (mKey != null)
			url += "key=" + mKey + "&";
		url += "format=json"
			+ "&accept-language=" + mLocale.getLanguage()
			//+ "&addressdetails=1"
			+ "&lat=" + latitude 
			+ "&lon=" + longitude;
		Log.d(BonusPackHelper.LOG_TAG, "GeocoderNominatim::getFromLocation:"+url);
		String result = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
		if (result == null)
			throw new StatusException(HttpURLConnection.HTTP_NO_CONTENT);
		try {
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(result);
			JsonObject jResult = json.getAsJsonObject();
			Address gAddress = buildAndroidAddress(jResult);
			List<Address> list = new ArrayList<>(1);
			if (gAddress != null)
				list.add(gAddress);
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
			boolean bounded) throws IOException, StatusException {
		String url = mServiceUrl + "search?";
		if (mKey != null)
			url += "key=" + mKey + "&";
		url += "format=json"
				+ "&accept-language=" + mLocale.getLanguage()
				+ "&addressdetails=1"
				+ "&limit=" + maxResults
				+ "&q=" + URLEncoder.encode(locationName);
		if (lowerLeftLatitude != 0.0 && upperRightLatitude != 0.0){
			//viewbox = left, top, right, bottom:
			url += "&viewbox=" + lowerLeftLongitude
				+ "," + upperRightLatitude
				+ "," + upperRightLongitude
				+ "," + lowerLeftLatitude
				+ "&bounded="+(bounded ? 1 : 0);
		}
		if (mPolygon){
			//get polygon outlines for items found:
			url += "&polygon=1";
			//TODO: polygon param is obsolete. Should be replaced by polygon_geojson. 
			//Upgrade is on hold, waiting for MapQuest service to become compatible. 
		}
		Log.d(BonusPackHelper.LOG_TAG, "GeocoderNominatim::getFromLocationName:"+url);
		String result = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
		//Log.d(BonusPackHelper.LOG_TAG, result);
		if (result == null)
			throw new StatusException(HttpURLConnection.HTTP_NO_CONTENT);
		try {
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(result);
			JsonArray jResults = json.getAsJsonArray();
			List<Address> list = new ArrayList<>(jResults.size());
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
			double upperRightLatitude, double upperRightLongitude) throws IOException, StatusException {
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
	public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException, StatusException {
		return getFromLocationName(locationName, maxResults, 0.0, 0.0, 0.0, 0.0, false);
	}
	
}
