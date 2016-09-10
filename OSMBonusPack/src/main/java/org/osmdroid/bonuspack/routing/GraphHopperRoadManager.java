package org.osmdroid.bonuspack.routing;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;

/** get a route between a start and a destination point, going through a list of waypoints.
 * It uses GraphHopper, an open source routing service based on OpenSteetMap data. <br>
 * 
 * It requests by default the GraphHopper demo site. 
 * Use setService() to request another (for instance your own) GraphHopper-compliant service. <br> 
 * 
 * @see <a href="https://github.com/graphhopper/web-api/blob/master/docs-routing.md">GraphHopper</a>
 * @author M.Kergall
 */
public class GraphHopperRoadManager extends RoadManager {

	protected static final String SERVICE = "https://graphhopper.com/api/1/route?";
	public static final int STATUS_NO_ROUTE = Road.STATUS_TECHNICAL_ISSUE+1;
	
	protected String mServiceUrl;
	protected String mKey;
	protected boolean mWithElevation;
	protected boolean mAlternateAvailable;

	/** mapping from GraphHopper directions to MapQuest maneuver IDs: */
	static final HashMap<Integer, Integer> MANEUVERS;
	static {
		MANEUVERS = new HashMap<Integer, Integer>();
		MANEUVERS.put(0, 1); //Continue
		MANEUVERS.put(1, 6); //Slight right
		MANEUVERS.put(2, 7); //Right
		MANEUVERS.put(3, 8); //Sharp right
		MANEUVERS.put(-3, 5); //Sharp left
		MANEUVERS.put(-2, 4); //Left
		MANEUVERS.put(-1, 3); //Slight left
		MANEUVERS.put(4, 24); //Arrived
		MANEUVERS.put(5, 24); //Arrived at waypoint
	}
	
	/**
	 * @param apiKey GraphHopper API key, mandatory to use the public GraphHopper service.
	 * @see <a href="http://graphhopper.com/#enterprise">GraphHopper</a> to obtain an API key.
	 */
	public GraphHopperRoadManager(String apiKey, boolean alternateAvailable) {
		super();
		mServiceUrl = SERVICE;
		mKey = apiKey;
		mWithElevation = false;
		mAlternateAvailable = alternateAvailable;
	}
	
	/** allows to request on an other site than GraphHopper demo site */
	public void setService(String serviceUrl){
		mServiceUrl = serviceUrl;
	}
	
	/** set if altitude of every route point should be requested or not. Default is false. */
	public void setElevation(boolean withElevation){
		mWithElevation = withElevation;
	}

	protected String getUrl(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
		StringBuilder urlString = new StringBuilder(mServiceUrl);
		urlString.append("key="+mKey);
		for (int i=0; i<waypoints.size(); i++){
			GeoPoint p = waypoints.get(i);
			urlString.append("&point="+geoPointAsString(p));
		}
		//urlString.append("&instructions=true"); already set by default
		urlString.append("&elevation="+(mWithElevation?"true":"false"));
		if (getAlternate && mAlternateAvailable)
			urlString.append("&ch.disable=true&algorithm=alternative_route");
		urlString.append(mOptions);
		return urlString.toString();
	}

	protected Road[] defaultRoad(ArrayList<GeoPoint> waypoints) {
		Road[] roads = new Road[1];
		roads[0] = new Road(waypoints);
		return roads;
	}

	public Road[] getRoads(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
		String url = getUrl(waypoints, getAlternate);
		Log.d(BonusPackHelper.LOG_TAG, "GraphHopper.getRoads:" + url);
		String jString = BonusPackHelper.requestStringFromUrl(url);
		if (jString == null) {
			return defaultRoad(waypoints);
		}
		try {
			JSONObject jRoot = new JSONObject(jString);
			JSONArray jPaths = jRoot.optJSONArray("paths");
			if (jPaths == null || jPaths.length() == 0){
				return defaultRoad(waypoints);
				/*
				road = new Road(waypoints);
				road.mStatus = STATUS_NO_ROUTE;
				return road;
				*/
			}
			Road[] roads = new Road[jPaths.length()];
			for (int r = 0; r < jPaths.length(); r++) {
				JSONObject jPath = jPaths.getJSONObject(r);
				String route_geometry = jPath.getString("points");
				Road road = new Road();
				roads[r] = road;
				road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10, mWithElevation);
				JSONArray jInstructions = jPath.getJSONArray("instructions");
				int n = jInstructions.length();
				for (int i = 0; i < n; i++) {
					JSONObject jInstruction = jInstructions.getJSONObject(i);
					RoadNode node = new RoadNode();
					JSONArray jInterval = jInstruction.getJSONArray("interval");
					int positionIndex = jInterval.getInt(0);
					node.mLocation = road.mRouteHigh.get(positionIndex);
					node.mLength = jInstruction.getDouble("distance") / 1000.0;
					node.mDuration = jInstruction.getInt("time") / 1000.0; //Segment duration in seconds.
					int direction = jInstruction.getInt("sign");
					node.mManeuverType = getManeuverCode(direction);
					node.mInstructions = jInstruction.getString("text");
					road.mNodes.add(node);
				}
				road.mLength = jPath.getDouble("distance") / 1000.0;
				road.mDuration = jPath.getInt("time") / 1000.0;
				JSONArray jBBox = jPath.getJSONArray("bbox");
				road.mBoundingBox = new BoundingBox(jBBox.getDouble(3), jBBox.getDouble(2),
						jBBox.getDouble(1), jBBox.getDouble(0));
				road.mStatus = Road.STATUS_OK;
				road.buildLegs(waypoints);
				Log.d(BonusPackHelper.LOG_TAG, "GraphHopper.getRoads - finished");
			}
			return roads;
		} catch (JSONException e) {
			e.printStackTrace();
			return defaultRoad(waypoints);
		}
	}

	@Override public Road[] getRoads(ArrayList<GeoPoint> waypoints) {
		return getRoads(waypoints, true);
	}

	@Override
	public Road getRoad(ArrayList<GeoPoint> waypoints) {
		Road[] roads = getRoads(waypoints, false);
		return roads[0];
	}

	protected int getManeuverCode(int direction){
		Integer code = MANEUVERS.get(direction);
		if (code != null)
			return code;
		else 
			return 0;
	}

}
