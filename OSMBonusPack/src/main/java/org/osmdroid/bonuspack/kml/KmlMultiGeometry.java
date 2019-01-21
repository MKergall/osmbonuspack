package org.osmdroid.bonuspack.kml;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.osmdroid.bonuspack.kml.KmlFeature.Styler;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Overlay;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * KML MultiGeometry and/or GeoJSON GeometryCollection. 
 * It can also parse GeoJSON MultiPoint, MultiLineString and MultiPolygon.
 * @author M.Kergall
 */
public class KmlMultiGeometry extends KmlGeometry implements Cloneable, Parcelable {

	/** list of KmlGeometry items. Can be empty if none, but is not null */
	public ArrayList<KmlGeometry> mItems;
	
	public KmlMultiGeometry(){
		super();
		mItems = new ArrayList<KmlGeometry>();
	}

	/** GeoJSON constructor */
	public KmlMultiGeometry(JsonObject json){
		this();
		String type = json.get("type").getAsString();
		if ("GeometryCollection".equals(type)){
			JsonArray geometries = json.get("geometries").getAsJsonArray();
	        if (geometries != null) {
	            for (JsonElement geometrieJSON:geometries) {
	            	mItems.add(parseGeoJSON(geometrieJSON.getAsJsonObject()));
	            }
	        }
		} else if ("MultiPoint".equals(type)){
			JsonArray coordinates = json.get("coordinates").getAsJsonArray();
			ArrayList<GeoPoint> positions = parseGeoJSONPositions(coordinates);
			for (GeoPoint p:positions){
				KmlPoint kmlPoint = new KmlPoint(p);
				mItems.add(kmlPoint);
			}
		} else if ("MultiLineString".equals(type)){
			JsonArray lineStrings = json.get("coordinates").getAsJsonArray();
			for (JsonElement lineStringE:lineStrings){
				JsonArray lineStringA = (JsonArray)lineStringE;
				KmlLineString lineString = new KmlLineString(lineStringA);
				mItems.add(lineString);
			}
		} else if ("MultiPolygon".equals(type)){
			JsonArray polygonsA = json.get("coordinates").getAsJsonArray();
			for (JsonElement polygonE:polygonsA){
				JsonArray polygonA = (JsonArray)polygonE;
				KmlPolygon polygon = new KmlPolygon(polygonA);
				mItems.add(polygon);
			}
		}
	}
	
	public void addItem(KmlGeometry item){
		mItems.add(item);
	}
	
	/** Build a FolderOverlay containing all overlays from this MultiGeometry items */
	@Override public Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlPlacemark kmlPlacemark, 
			KmlDocument kmlDocument){
		FolderOverlay folderOverlay = new FolderOverlay();
		for (KmlGeometry k:mItems){
			Overlay overlay = k.buildOverlay(map, defaultStyle, styler, kmlPlacemark, kmlDocument);
			folderOverlay.add(overlay);
		}
		return folderOverlay;
	}
	
	@Override public void saveAsKML(Writer writer) {
		try {
			writer.write("<MultiGeometry>\n");
			for (KmlGeometry item:mItems)
				item.saveAsKML(writer);
			writer.write("</MultiGeometry>\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override public JsonObject asGeoJSON() {
		JsonObject json = new JsonObject();
		json.addProperty("type", "GeometryCollection");
		JsonArray geometries = new JsonArray();
		for (KmlGeometry item:mItems)
			geometries.add(item.asGeoJSON());
		json.add("geometries", geometries);
		return json;
	}

	@Override public BoundingBox getBoundingBox(){
		BoundingBox finalBB = null;
		for (KmlGeometry item:mItems){
			BoundingBox itemBB = item.getBoundingBox();
			if (itemBB != null){
				if (finalBB == null){
					finalBB = itemBB.clone();
				} else {
					finalBB = finalBB.concat(itemBB);
				}
			}
		}
		return finalBB;
	}
	
	//Cloneable implementation ------------------------------------
	
	@Override public KmlMultiGeometry clone(){
		KmlMultiGeometry kmlMultiGeometry = (KmlMultiGeometry)super.clone();
		kmlMultiGeometry.mItems = new ArrayList<KmlGeometry>(mItems.size());
		for (KmlGeometry item:mItems)
			kmlMultiGeometry.mItems.add(item.clone());
		return kmlMultiGeometry;
	}
	
	//Parcelable implementation ------------
	
	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeList(mItems);
	}
	
	public static final Creator<KmlMultiGeometry> CREATOR = new Creator<KmlMultiGeometry>() {
		@Override public KmlMultiGeometry createFromParcel(Parcel source) {
			return new KmlMultiGeometry(source);
		}
		@Override public KmlMultiGeometry[] newArray(int size) {
			return new KmlMultiGeometry[size];
		}
	};
	
	public KmlMultiGeometry(Parcel in){
		super(in);
		mItems = in.readArrayList(KmlGeometry.class.getClassLoader());
	}
}
