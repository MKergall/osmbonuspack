package org.osmdroid.bonuspack.kml;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KML Placemark. Support the following Geometry: Point, LineString, and Polygon. 
 * @author M.Kergall
 */
public class KmlPlacemark extends KmlFeature implements Cloneable, Parcelable {
	
	/** the KML Geometry of the Placemark. Null if none. */
	public KmlGeometry mGeometry;

	/** constructs a Placemark of unknown Geometry */
	public KmlPlacemark(){
		super();
	}
	
	/**
	 * constructs a Placemark with a Point Geometry.  
	 * @param position position of the Point
	 */
	public KmlPlacemark(GeoPoint position){
		this();
		mGeometry = new KmlPoint(position);
	}
	
	/** constructs a Placemark from a Marker, as a KML Point */
	public KmlPlacemark(Marker marker){
		this(marker.getPosition());
		mName = marker.getTitle();
		mDescription = marker.getSnippet();
		mVisibility = marker.isEnabled();
		mId = marker.getId();
		//TODO: Style / IconStyle => transparency, hotspot, bearing. 
	}

	/** constructs a Placemark from a Polygon overlay, as a KML Polygon */
	public KmlPlacemark(Polygon polygon, KmlDocument kmlDoc){
		this();
		mName = polygon.getTitle();
		mDescription = polygon.getSnippet();
		mGeometry = new KmlPolygon();
		mGeometry.mCoordinates = (ArrayList<GeoPoint>)polygon.getPoints();
		/*DEBUG DEBUG DEBUG
		We just want to do that:
		//((KmlPolygon)mGeometry).mHoles = (ArrayList<ArrayList<GeoPoint>>)polygon.getHoles();
		And we have to do that:
		List<List<GeoPoint>> lh = polygon.getHoles();
		((KmlPolygon)mGeometry).mHoles = new ArrayList<ArrayList<GeoPoint>>(lh.size());
		for (List<GeoPoint> h:lh){
			((KmlPolygon)mGeometry).mHoles.add(h);
			now copy every fucking point...
		}
		*/
		mVisibility = polygon.isEnabled();
		mId = polygon.getId();
		//Style:
		Style style = new Style();
		style.mPolyStyle = new ColorStyle(polygon.getFillColor());
		style.mLineStyle = new LineStyle(polygon.getStrokeColor(), polygon.getStrokeWidth());
		mStyle = kmlDoc.addStyle(style);
	}

	/** constructs a Placemark from a Polyline overlay, as a KML LineString */
	public KmlPlacemark(Polyline polyline, KmlDocument kmlDoc){
		this();
		mName = polyline.getTitle();
		mDescription = polyline.getSnippet();
		mGeometry = new KmlLineString();
		mGeometry.mCoordinates = (ArrayList<GeoPoint>)polyline.getPoints();
		mVisibility = polyline.isEnabled();
		mId = polyline.getId();
		//Style:
		Style style = new Style();
		style.mLineStyle = new LineStyle(polyline.getColor(), polyline.getWidth());
		mStyle = kmlDoc.addStyle(style);
	}

	/** GeoJSON constructor */
	public KmlPlacemark(JsonObject json){
		this();
		if (json.has("id"))
			mId = json.get("id").getAsString();
		JsonElement element = json.get("geometry");
		if (element != null && !element.isJsonNull()) {
			JsonObject geometry = element.getAsJsonObject();
			mGeometry = KmlGeometry.parseGeoJSON(geometry);
		}
		if (json.has("properties")){
			//Parse properties:
			element = json.get("properties");
			if (!element.isJsonNull()) {
				JsonObject properties = json.getAsJsonObject("properties");
				Set<Map.Entry<String, JsonElement>> entrySet = properties.entrySet();
				for (Map.Entry<String, JsonElement> entry : entrySet) {
					String key = entry.getKey();
					JsonElement je = entry.getValue();
					String value;
					try {
						value = je.getAsString();
					} catch (Exception e) {
						value = je.toString();
					}
					if (key != null && value != null)
						setExtendedData(key, value);
				}
				//Put "name" property in standard KML format:
				if (mExtendedData != null && mExtendedData.containsKey("name")) {
					mName = mExtendedData.get("name");
					mExtendedData.remove("name");
				}
			}
		}
	}
	
	@Override public BoundingBox getBoundingBox(){
		if (mGeometry != null)
			return mGeometry.getBoundingBox();
		else
			return null;
	}
	
	@Override public Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlDocument kmlDocument){
		if (mGeometry != null)
			return mGeometry.buildOverlay(map, defaultStyle, styler, this, kmlDocument);
		else 
			return null;
	}
	
	@Override public void writeKMLSpecifics(Writer writer){
		if (mGeometry != null)
			mGeometry.saveAsKML(writer);
	}
	
	protected JsonObject geoJSONProperties(){
		try {
			JsonObject json = new JsonObject();
			if (mName != null){
				json.addProperty("name", mName);
			}
			if (mExtendedData != null){
				for (Map.Entry<String, String> entry : mExtendedData.entrySet()) {
					String name = entry.getKey();
					String value = entry.getValue();
					json.addProperty(name, value);
				}
			}
			return json;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** @return this as a GeoJSON object. */
	@Override public JsonObject asGeoJSON(boolean isRoot){
		JsonObject json = new JsonObject();
		json.addProperty("type", "Feature");
		if (mId != null)
			json.addProperty("id", mId);
		if (mGeometry != null)
			json.add("geometry", mGeometry.asGeoJSON());
		else
			json.add("geometry", JsonNull.INSTANCE);
		json.add("properties", geoJSONProperties());
		return json;
	}
	
	//Cloneable implementation ------------------------------------

	@Override public KmlPlacemark clone(){
		KmlPlacemark kmlPlacemark = (KmlPlacemark)super.clone();
		if (mGeometry != null)
			kmlPlacemark.mGeometry = mGeometry.clone();
		return kmlPlacemark;
	}
	
	//Parcelable implementation ------------
	
	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeParcelable(mGeometry, flags);
	}
	
	public static final Creator<KmlPlacemark> CREATOR = new Creator<KmlPlacemark>() {
		@Override public KmlPlacemark createFromParcel(Parcel source) {
			return new KmlPlacemark(source);
		}
		@Override public KmlPlacemark[] newArray(int size) {
			return new KmlPlacemark[size];
		}
	};
	
	public KmlPlacemark(Parcel in){
		super(in);
		mGeometry = in.readParcelable(KmlGeometry.class.getClassLoader());
	}
}
