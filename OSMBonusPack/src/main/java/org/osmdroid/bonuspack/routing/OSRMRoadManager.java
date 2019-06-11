package org.osmdroid.bonuspack.routing;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.R;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;

/** get a route between a start and a destination point, going through a list of waypoints.
 * It uses OSRM, a free open source routing service based on OpenSteetMap data. <br>
 *
 * It requests by default the OSRM demo site.
 * Use setService() to request an other (for instance your own) OSRM service. <br>
 *
 * @see <a href="https://github.com/DennisOSRM/Project-OSRM/wiki/Server-api">OSRM</a>
 * @see <a href="https://github.com/Project-OSRM/osrm-backend/wiki/New-Server-api">V5 API</a>
 *
 * @author M.Kergall
 */
public class OSRMRoadManager extends RoadManager {

	static final String SERVICE = "https://router.project-osrm.org/route/v1/driving/";
	private final Context mContext;
	protected String mServiceUrl;
	protected String mUserAgent;

	/**
	 * mapping from OSRM StepManeuver types to MapQuest maneuver IDs:
	 */
	static final HashMap<String, Integer> MANEUVERS;
	static {
		MANEUVERS = new HashMap<>();
		MANEUVERS.put("new name", 2); //road name change
		MANEUVERS.put("turn-straight", 1); //Continue straight
		MANEUVERS.put("turn-slight right", 6); //Slight right
		MANEUVERS.put("turn-right", 7); //Right
		MANEUVERS.put("turn-sharp right", 8); //Sharp right
		MANEUVERS.put("turn-uturn", 12); //U-turn
		MANEUVERS.put("turn-sharp left", 5); //Sharp left
		MANEUVERS.put("turn-left", 4); //Left
		MANEUVERS.put("turn-slight left", 3); //Slight left
		MANEUVERS.put("depart", 24); //"Head" => used by OSRM as the start node. Considered here as a "waypoint".
			// TODO - to check...
		MANEUVERS.put("arrive", 24); //Arrived (at waypoint)
		MANEUVERS.put("roundabout-1", 27); //Round-about, 1st exit
		MANEUVERS.put("roundabout-2", 28); //2nd exit, etc ...
		MANEUVERS.put("roundabout-3", 29);
		MANEUVERS.put("roundabout-4", 30);
		MANEUVERS.put("roundabout-5", 31);
		MANEUVERS.put("roundabout-6", 32);
		MANEUVERS.put("roundabout-7", 33);
		MANEUVERS.put("roundabout-8", 34); //Round-about, 8th exit
		//TODO: other OSRM types to handle properly:
		MANEUVERS.put("merge-left", 20);
		MANEUVERS.put("merge-sharp left", 20);
		MANEUVERS.put("merge-slight left", 20);
		MANEUVERS.put("merge-right", 21);
		MANEUVERS.put("merge-sharp right", 21);
		MANEUVERS.put("merge-slight right", 21);
		MANEUVERS.put("merge-straight", 22);
		MANEUVERS.put("ramp-left", 17);
		MANEUVERS.put("ramp-sharp left", 17);
		MANEUVERS.put("ramp-slight left", 17);
		MANEUVERS.put("ramp-right", 18);
		MANEUVERS.put("ramp-sharp right", 18);
		MANEUVERS.put("ramp-slight right", 18);
		MANEUVERS.put("ramp-straight", 19);
		//MANEUVERS.put("fork", );
		//MANEUVERS.put("end of road", );
		//MANEUVERS.put("continue", );
	}
	
	//From: Project-OSRM-Web / WebContent / localization / OSRM.Locale.en.js
	// driving directions
	// %s: road name
	// %d: direction => removed
	// <*>: will only be printed when there actually is a road name
	static final HashMap<Integer, Object> DIRECTIONS;
	static {
		DIRECTIONS = new HashMap<>();
		DIRECTIONS.put(1, R.string.osmbonuspack_directions_1);
		DIRECTIONS.put(2, R.string.osmbonuspack_directions_2);
		DIRECTIONS.put(3, R.string.osmbonuspack_directions_3);
		DIRECTIONS.put(4, R.string.osmbonuspack_directions_4);
		DIRECTIONS.put(5, R.string.osmbonuspack_directions_5);
		DIRECTIONS.put(6, R.string.osmbonuspack_directions_6);
		DIRECTIONS.put(7, R.string.osmbonuspack_directions_7);
		DIRECTIONS.put(8, R.string.osmbonuspack_directions_8);
		DIRECTIONS.put(12, R.string.osmbonuspack_directions_12);
		DIRECTIONS.put(17, R.string.osmbonuspack_directions_17);
		DIRECTIONS.put(18, R.string.osmbonuspack_directions_18);
		DIRECTIONS.put(19, R.string.osmbonuspack_directions_19);
		//DIRECTIONS.put(20, R.string.osmbonuspack_directions_20);
		//DIRECTIONS.put(21, R.string.osmbonuspack_directions_21);
		//DIRECTIONS.put(22, R.string.osmbonuspack_directions_22);
		DIRECTIONS.put(24, R.string.osmbonuspack_directions_24);
		DIRECTIONS.put(27, R.string.osmbonuspack_directions_27);
		DIRECTIONS.put(28, R.string.osmbonuspack_directions_28);
		DIRECTIONS.put(29, R.string.osmbonuspack_directions_29);
		DIRECTIONS.put(30, R.string.osmbonuspack_directions_30);
		DIRECTIONS.put(31, R.string.osmbonuspack_directions_31);
		DIRECTIONS.put(32, R.string.osmbonuspack_directions_32);
		DIRECTIONS.put(33, R.string.osmbonuspack_directions_33);
		DIRECTIONS.put(34, R.string.osmbonuspack_directions_34);
	}

	public OSRMRoadManager(Context context){
		super();
		mContext = context;
		mServiceUrl = SERVICE;
		mUserAgent = BonusPackHelper.DEFAULT_USER_AGENT; //set user agent to the default one. 
	}
	
	/** allows to request on an other site than OSRM demo site */
	public void setService(String serviceUrl){
		mServiceUrl = serviceUrl;
	}

	/** allows to send to OSRM service a user agent specific to the app, 
	 * instead of the default user agent of OSMBonusPack lib. 
	 */
	public void setUserAgent(String userAgent){
		mUserAgent = userAgent;
	}
	
	protected String getUrl(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
		StringBuilder urlString = new StringBuilder(mServiceUrl);
		for (int i=0; i<waypoints.size(); i++){
			GeoPoint p = waypoints.get(i);
			if (i>0)
				urlString.append(';');
			urlString.append(geoPointAsLonLatString(p));
		}
		urlString.append("?alternatives="+(getAlternate?"true" : "false"));
		urlString.append("&overview=full&steps=true");
		urlString.append(mOptions);
		return urlString.toString();
	}

	/*
	protected void getInstructions(Road road, JSONArray jInstructions){
		try {
			int n = jInstructions.length();
			RoadNode lastNode = null;
			for (int i=0; i<n; i++){
				JSONArray jInstruction = jInstructions.getJSONArray(i);
				RoadNode node = new RoadNode();
				int positionIndex = jInstruction.getInt(3);
				node.mLocation = road.mRouteHigh.get(positionIndex);
				node.mLength = jInstruction.getInt(2)/1000.0;
				node.mDuration = jInstruction.getInt(4); //Segment duration in seconds.
				String direction = jInstruction.getString(0);
				String roadName = jInstruction.getString(1);
				if (lastNode!=null && "1".equals(direction) && "".equals(roadName)){
					//node "Continue" with no road name is useless, don't add it
					lastNode.mLength += node.mLength;
					lastNode.mDuration += node.mDuration;
				} else {
					node.mManeuverType = getManeuverCode(direction);
					node.mInstructions = buildInstructions(direction, roadName);
					//Log.d(BonusPackHelper.LOG_TAG, direction+"=>"+node.mManeuverType+"; "+node.mInstructions);
					road.mNodes.add(node);
					lastNode = node;
				}
			}
		} catch (JSONException e) {
			road.mStatus = Road.STATUS_TECHNICAL_ISSUE;
			e.printStackTrace();
		}
	}
	*/

	protected Road[] defaultRoad(ArrayList<GeoPoint> waypoints){
		Road[] roads = new Road[1];
		roads[0] = new Road(waypoints);
		return roads;
	}

	protected Road[] getRoads(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
		String url = getUrl(waypoints, getAlternate);
		Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads:" + url);
		String jString = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
		if (jString == null) {
			Log.e(BonusPackHelper.LOG_TAG, "OSRMRoadManager::getRoad: request failed.");
			return defaultRoad(waypoints);
		}

		try {
			JSONObject jObject = new JSONObject(jString);
			String jCode = jObject.getString("code");
			if (!"Ok".equals(jCode)) {
				Log.e(BonusPackHelper.LOG_TAG, "OSRMRoadManager::getRoad: error code=" + jCode);
				Road[] roads = defaultRoad(waypoints);
				if ("NoRoute".equals(jCode)) {
					roads[0].mStatus = Road.STATUS_INVALID;
				}
				return roads;
			} else {
				JSONArray jRoutes = jObject.getJSONArray("routes");
				Road[] roads = new Road[jRoutes.length()];
				for (int i=0; i<jRoutes.length(); i++){
					Road road = new Road();
					roads[i] = road;
					road.mStatus = Road.STATUS_OK;
					JSONObject jRoute = jRoutes.getJSONObject(i);
					String route_geometry = jRoute.getString("geometry");
					road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10, false);
					road.mBoundingBox = BoundingBox.fromGeoPoints(road.mRouteHigh);
					road.mLength = jRoute.getDouble("distance") / 1000.0;
					road.mDuration = jRoute.getDouble("duration");
					//legs:
					JSONArray jLegs = jRoute.getJSONArray("legs");
					for (int l=0; l<jLegs.length(); l++) {
						//leg:
						JSONObject jLeg = jLegs.getJSONObject(l);
						RoadLeg leg = new RoadLeg();
						road.mLegs.add(leg);
						leg.mLength = jLeg.getDouble("distance");
						leg.mDuration = jLeg.getDouble("duration");
						//steps:
						JSONArray jSteps = jLeg.getJSONArray("steps");
						RoadNode lastNode = null;
						String lastRoadName = "";
						for (int s=0; s<jSteps.length(); s++) {
							JSONObject jStep = jSteps.getJSONObject(s);
							RoadNode node = new RoadNode();
							node.mLength = jStep.getDouble("distance") / 1000.0;
							node.mDuration = jStep.getDouble("duration");
							JSONObject jStepManeuver = jStep.getJSONObject("maneuver");
							JSONArray jLocation = jStepManeuver.getJSONArray("location");
							node.mLocation = new GeoPoint(jLocation.getDouble(1), jLocation.getDouble(0));
							String direction = jStepManeuver.getString("type");
							if (direction.equals("turn") || direction.equals("ramp") || direction.equals("merge")){
								String modifier = jStepManeuver.getString("modifier");
								direction = direction + '-' + modifier;
							} else if (direction.equals("roundabout")){
								int exit = jStepManeuver.getInt("exit");
								direction = direction + '-' + exit;
							} else if (direction.equals("rotary")) {
								int exit = jStepManeuver.getInt("exit");
								direction = "roundabout" + '-' + exit; //convert rotary in roundabout...
							}
							node.mManeuverType = getManeuverCode(direction);
							String roadName = jStep.optString("name", "");
							node.mInstructions = buildInstructions(node.mManeuverType, roadName);
							if (lastNode != null && node.mManeuverType == 2 && lastRoadName.equals(roadName)) {
								//workaround for https://github.com/Project-OSRM/osrm-backend/issues/2273
								//"new name", but identical to previous name:
								//skip, but update values of last node:
								lastNode.mDuration += node.mDuration;
								lastNode.mLength += node.mLength;
							} else {
								road.mNodes.add(node);
								lastNode = node;
								lastRoadName = roadName;
							}
						} //steps
					} //legs
				} //routes
				Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads - finished");
				return roads;
			} //if code is Ok
		} catch (JSONException e) {
			e.printStackTrace();
			return defaultRoad(waypoints);
		}
	}

	@Override public Road[] getRoads(ArrayList<GeoPoint> waypoints) {
		return getRoads(waypoints, true);
	}

	@Override public Road getRoad(ArrayList<GeoPoint> waypoints) {
		Road[] roads = getRoads(waypoints, false);
		return roads[0];
	}

	protected int getManeuverCode(String direction){
		Integer code = MANEUVERS.get(direction);
		if (code != null)
			return code;
		else
			return 0;
	}

	protected String buildInstructions(int maneuver, String roadName){
		Integer resDirection = (Integer) DIRECTIONS.get(maneuver);
		if (resDirection == null)
			return null;
		String direction = mContext.getString(resDirection);
		String instructions;
		if (roadName.equals(""))
			//remove "<*>"
			instructions = direction.replaceFirst("\\[[^\\]]*\\]", "");
		else {
			direction = direction.replace('[', ' ');
			direction = direction.replace(']', ' ');
			instructions = String.format(direction, roadName);
		}
		return instructions;
	}
}
