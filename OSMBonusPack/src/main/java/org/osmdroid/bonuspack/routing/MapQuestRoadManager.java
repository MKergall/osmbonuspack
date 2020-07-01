package org.osmdroid.bonuspack.routing;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;

/** class to get a route between a start and a destination point, going through a list of waypoints.
 *
 * It uses MapQuest open, public and free API, based on OpenStreetMap data. <br>
 * Return a "Road" object.
 * @see Road
 * @see <a href="https://developer.mapquest.com/documentation/open/guidance-api/v2/route/get/">MapQuest Open Guidance API</a>
 * @author M.Kergall
 */
public class MapQuestRoadManager extends RoadManager {

    static final String MAPQUEST_GUIDANCE_SERVICE = "http://open.mapquestapi.com/guidance/v2/route?";
    protected String mApiKey;

    /**
     * @param apiKey MapQuest API key, mandatory to use the MapQuest Open service.
     * @see <a href="http://developer.mapquest.com">MapQuest API</a> for registration.
     */
    public MapQuestRoadManager(String apiKey) {
        super();
        mApiKey = apiKey;
    }

    /**
     * Build the URL to MapQuest service returning a route in XML format
     * @param waypoints: array of waypoints, as [lat, lng], from start point to end point.
     */
    protected String getUrl(ArrayList<GeoPoint> waypoints) {
        StringBuilder urlString = new StringBuilder(MAPQUEST_GUIDANCE_SERVICE);
        urlString.append("key="+mApiKey);
        urlString.append("&from=");
        GeoPoint p = waypoints.get(0);
        urlString.append(geoPointAsString(p));

        for (int i=1; i<waypoints.size(); i++){
            p = waypoints.get(i);
            urlString.append("&to="+geoPointAsString(p));
        }

        urlString.append("&shapeFormat=cmp6"); //encoded polyline, much faster - BUT NOT WORKING!

        urlString.append("&narrativeType=text"); //or "none"
        //Locale locale = Locale.getDefault();
        //urlString.append("&locale="+locale.getLanguage()+"_"+locale.getCountry());

        urlString.append("&unit=k&fishbone=false");

        //urlString.append("&generalizeAfter=500" /*+&generalize=2"*/);
        //500 points max, 2 meters tolerance

        //Warning: MapQuest Open API doc is sometimes WRONG:
        //- use fishbone, not enableFishbone
        //- locale (fr_FR, en_US) is not supported anymore in V2.
        //- generalize and generalizeAfter are not properly implemented
        urlString.append(mOptions);
        return urlString.toString();
    }

    /**
     * @param waypoints list of GeoPoints. Must have at least 2 entries, start and end points.
     * @return the road
     */
    @Override public Road getRoad(ArrayList<GeoPoint> waypoints) {
        String url = getUrl(waypoints);
        Log.d(BonusPackHelper.LOG_TAG, "MapQuestRoadManager.getRoute:"+url);
        String jString = BonusPackHelper.requestStringFromUrl(url);
        if (jString == null) {
            return new Road(waypoints);
        }
        Road road = new Road();
        ArrayList<RoadLink> links = new ArrayList<>();
        try {
            JSONObject jRoot = new JSONObject(jString);
            JSONObject jGuidance = jRoot.getJSONObject("guidance");

            JSONArray jGuidanceLinkCollection = jGuidance.getJSONArray("GuidanceLinkCollection");
            int n = jGuidanceLinkCollection.length();
            for (int i=0; i<n; i++){
                JSONObject jLink = jGuidanceLinkCollection.getJSONObject(i);
                RoadLink link = new RoadLink();
                link.mLength = jLink.getDouble("length");
                link.mSpeed = jLink.getDouble("speed");
                link.mShapeIndex = jLink.getInt("shapeIndex");
                link.mDuration = link.mLength / link.mSpeed * 3600.0;
                links.add(link);
                road.mLength += link.mLength;
                road.mDuration += link.mDuration;
            }

            JSONArray jGuidanceNodeCollection = jGuidance.getJSONArray("GuidanceNodeCollection");
            n = jGuidanceNodeCollection.length();
            for (int i=0; i<n; i++) {
                JSONObject jNode = jGuidanceNodeCollection.getJSONObject(i);
                RoadNode node = new RoadNode();
                int turnCost = jNode.optInt("turnCost", 0);
                node.mDuration += turnCost;
                road.mDuration += turnCost;
                int maneuverType = jNode.optInt("maneuverType", 0);
                node.mManeuverType = maneuverType;
                JSONArray jLinkIds = jNode.optJSONArray("linkIds");
                if (jLinkIds != null)
                    node.mNextRoadLink = jLinkIds.getInt(0);
                //TODO: info
                node.mInstructions = jNode.optString("preTts", "");
                road.mNodes.add(node);
            }

            //String shape = jGuidance.getString("shapePoints"); => no way to get the shape in a compressed form.
            //road.mRouteHigh = PolylineEncoder.decode(shape, 1, false);
            JSONArray jShape = jGuidance.getJSONArray("shapePoints");
            n = jShape.length();
            road.mRouteHigh = new ArrayList<>(n);
            for (int i=0; i<n/2; i++){
                double lat = jShape.getDouble(i*2);
                double lng = jShape.getDouble(i*2+1);
                GeoPoint p = new GeoPoint(lat, lng);
                road.mRouteHigh.add(p);
            }

            JSONObject jSummary = jGuidance.getJSONObject("summary");
            JSONObject jBoundingBox = jSummary.getJSONObject("boundingBox");
            road.mBoundingBox = new BoundingBox(jBoundingBox.getDouble("maxLat"),
                    jBoundingBox.getDouble("maxLng"),
                    jBoundingBox.getDouble("minLat"),
                    jBoundingBox.getDouble("minLng"));

            road.mNodes = finalizeNodes(road.mNodes, links, road.mRouteHigh);
            road.mRouteHigh = finalizeRoadShape(road, links);
            road.buildLegs(waypoints);
            road.mStatus = Road.STATUS_OK;

        } catch (JSONException e) {
            e.printStackTrace();
            return new Road(waypoints);
        }
        Log.d(BonusPackHelper.LOG_TAG, "MapQuestRoadManager.getRoute - finished");
        return road;
    }

    /**
     * Note that alternate roads are not supported by MapQuest. This will always return 1 entry only.
     */
    @Override public Road[] getRoads(ArrayList<GeoPoint> waypoints) {
        Road road = getRoad(waypoints);
        Road[] roads = new Road[1];
        roads[0] = road;
        return roads;
    }

    protected ArrayList<RoadNode> finalizeNodes(ArrayList<RoadNode> mNodes,
                                                ArrayList<RoadLink> mLinks, ArrayList<GeoPoint> polyline){
        int n = mNodes.size();
        if (n == 0)
            return mNodes;
        ArrayList<RoadNode> newNodes = new ArrayList<RoadNode>(n);
        RoadNode lastNode = null;
        for (int i=1; i<n-1; i++){ //1, n-1 => first and last MapQuest nodes are irrelevant.
            RoadNode node = mNodes.get(i);
            RoadLink link = mLinks.get(node.mNextRoadLink);
            if (lastNode!=null && (node.mInstructions == null || node.mManeuverType == 0)){
                //this node is irrelevant, don't keep it,
                //but update values of last node:
                lastNode.mLength += link.mLength;
                lastNode.mDuration += (node.mDuration + link.mDuration);
            } else {
                //TODO: check not the end node (n-2)
                node.mLength = link.mLength;
                node.mDuration += link.mDuration;
                int locationIndex = link.mShapeIndex;
                node.mLocation = polyline.get(locationIndex);
                newNodes.add(node);
                lastNode = node;
            }
        }
        //switch to the new array of nodes:
        return newNodes;
    }

    /**
     * Clean-up 2 useless portions of MapQuest road shape: before start node, and after end node.
     * @return new road shape
     */
    public ArrayList<GeoPoint> finalizeRoadShape(Road road, ArrayList<RoadLink> links){
        ArrayList<GeoPoint> newShape = new ArrayList<GeoPoint>(road.mRouteHigh.size());
        RoadNode nodeStart = road.mNodes.get(0);
        RoadNode nodeEnd = road.mNodes.get(road.mNodes.size()-1);
        int shapeIndexStart = links.get(nodeStart.mNextRoadLink).mShapeIndex;
        int shapeIndexEnd = links.get(nodeEnd.mNextRoadLink).mShapeIndex;
        for (int i=shapeIndexStart; i<=shapeIndexEnd; i++){
            newShape.add(road.mRouteHigh.get(i));
        }
        return newShape;
    }

    /** Road Link is a portion of road between 2 "nodes" or intersections */
    class RoadLink {
        /** in km/h */
        public double mSpeed;
        /** in km */
        public double mLength;
        /** in sec */
        public double mDuration;
        /** starting point of the link, as index in initial polyline */
        public int mShapeIndex;
    }

}
