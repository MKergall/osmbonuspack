package org.osmdroid.bonuspack.kml;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.osmdroid.bonuspack.clustering.MarkerClusterer;
import org.osmdroid.bonuspack.overlays.GroundOverlay;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * KML Folder
 * @author M.Kergall
 */
public class KmlFolder extends KmlFeature implements Cloneable, Parcelable {

	/** List of KML Features it contains */
	public ArrayList<KmlFeature> mItems;

	public KmlFolder(){
		super();
		mItems = new ArrayList<KmlFeature>();
	}
	
	public KmlFolder(FolderOverlay overlay, KmlDocument kmlDoc){
		this();
		addOverlays(overlay.getItems(), kmlDoc);
		mName = overlay.getName();
		mDescription = overlay.getDescription();
		mVisibility = overlay.isEnabled();
	}
	
	public KmlFolder(MarkerClusterer overlay, KmlDocument kmlDoc){
		this();
		addOverlays(overlay.getItems(), kmlDoc);
		mName = overlay.getName();
		mDescription = overlay.getDescription();
		mVisibility = overlay.isEnabled();
	}
	
	/** GeoJSON constructor */
	public KmlFolder(JsonObject json){
		this();
		if (json.has("features")){
			JsonArray features = json.get("features").getAsJsonArray();
			for (JsonElement jsonFeature:features) {
		    	KmlFeature feature = KmlFeature.parseGeoJSON(jsonFeature.getAsJsonObject());
		        add(feature);
		    }
		}
	}
	
	@Override public BoundingBox getBoundingBox(){
		BoundingBox BB = null;
		for (KmlFeature item:mItems) {
			BoundingBox itemBB = item.getBoundingBox();
			if (itemBB != null){
				if (BB == null){
					BB = itemBB.clone();
				} else {
					BB = BB.concat(itemBB);
				}
			}
		}
		return BB;
	}
	
	/** 
	 * Converts the overlay to a KmlFeature and add it inside this. 
	 * Conversion from Overlay subclasses to KML Features is as follow: <br>
	 *   FolderOverlay, MarkerClusterer => Folder<br>
	 *   Marker => Point<br>
	 *   Polygon => Polygon<br>
	 *   Polyline => LineString<br>
	 *   GroundOverlay => GroundOverlay<br>
	 *   Else, add nothing. 
	 * @param overlay to convert and add
	 * @param kmlDoc for style handling. 
	 * @return true if OK, false if the overlay has not been added. 
	 */
	public boolean addOverlay(Overlay overlay, KmlDocument kmlDoc){
		if (overlay == null)
			return false;
		KmlFeature kmlItem;
		if (overlay instanceof GroundOverlay){
			kmlItem = new KmlGroundOverlay((GroundOverlay)overlay);
		} else if (overlay instanceof FolderOverlay){
			kmlItem = new KmlFolder((FolderOverlay)overlay, kmlDoc);
		} else if (overlay instanceof MarkerClusterer){
			kmlItem = new KmlFolder((MarkerClusterer)overlay, kmlDoc);
		} else if (overlay instanceof Marker){
			Marker marker = (Marker)overlay;
			kmlItem = new KmlPlacemark(marker);
		} else if (overlay instanceof Polygon){
			Polygon polygon = (Polygon)overlay;
			kmlItem = new KmlPlacemark(polygon, kmlDoc);
		} else if (overlay instanceof Polyline){
			Polyline polyline = (Polyline)overlay;
			kmlItem = new KmlPlacemark(polyline, kmlDoc);
		} else {
			return false;
		}
		mItems.add(kmlItem);
		return true;
	}
	
	/** 
	 * Adds all overlays inside this, converting them in KmlFeatures. 
	 * @param overlays list of overlays to add
	 * @param kmlDoc
	 */
	public void addOverlays(List<? extends Overlay> overlays, KmlDocument kmlDoc){
		if (overlays != null){
			for (Overlay item:overlays){
				addOverlay(item, kmlDoc);
			}
		}
	}
	
	/** Add an item in the KML Folder, at the end. */
	public void add(KmlFeature item){
		mItems.add(item);
	}
	
	/** 
	 * remove the item at itemPosition. No check for bad usage (itemPosition out of rank)
	 * @param itemPosition position of the item, starting from 0. 
	 * @return item removed
	 */
	public KmlFeature removeItem(int itemPosition){
		return mItems.remove(itemPosition);
	}

	/**
	 * Find a feature with a specific id in the folder.
	 * @param id feature id to find.
	 * @param recurse set to true if you want to find
	 * @return the feature found, or null if none has been found.
     */
	public KmlFeature findFeatureId(String id, boolean recurse){
		for (KmlFeature f:mItems) {
			if (f.mId != null && f.mId.equals(id))
				return f;
			if (recurse && f instanceof KmlFolder) {
				//if it's a folder, search recursively inside:
				KmlFeature ff = findFeatureId(id, recurse);
				if (ff != null)
					return ff;
			}
		}
		return null;
	}

	/**
	 * Build a FolderOverlay, containing (recursively) overlays from all items of this Folder. 
	 * @param map
	 * @param defaultStyle to apply when an item has no Style defined. 
	 * @param styler to apply
	 * @param kmlDocument for Styles
	 * @return the FolderOverlay built
	 */
	@Override public Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlDocument kmlDocument){
		FolderOverlay folderOverlay = new FolderOverlay();
		folderOverlay.setName(mName);
		folderOverlay.setDescription(mDescription);
		for (KmlFeature k:mItems){
			Overlay overlay = k.buildOverlay(map, defaultStyle, styler, kmlDocument);
			if (overlay != null)
				folderOverlay.add(overlay);
		}
		if (styler == null)
			folderOverlay.setEnabled(mVisibility);
		else 
			styler.onFeature(folderOverlay, this);
		return folderOverlay;
	}
	
	@Override public void writeKMLSpecifics(Writer writer){
		try {
			if (!mOpen)
				writer.write("<open>0</open>\n");
			for (KmlFeature item:mItems){
				item.writeAsKML(writer, false, null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public JsonObject geoJSONNamedCRS(String crsName){
		JsonObject crs = new JsonObject();
		crs.addProperty("type", "name");
		JsonObject properties = new JsonObject();
		properties.addProperty("name", crsName);
		crs.add("properties", properties);
		return crs;
	}
	
	/**
	 * Set isRoot to true if this is the root of the final GeoJSON structure. 
	 * Set isRoot to false if there is an enclosing FeatureCollection. 
	 * As GeoJSON doesn't support nested FeatureCollection, sub-items will be inserted directly in the result. 
	 * This is flattening the resulting GeoJSON hierarchy. 
	 * @return this as a GeoJSON FeatureCollection object. 
	 */
	@Override public JsonObject asGeoJSON(boolean isRoot){
		JsonObject json = new JsonObject();
		if (isRoot){
			json.add("crs", geoJSONNamedCRS("urn:ogc:def:crs:OGC:1.3:CRS84"));
		}
		JsonArray features = new JsonArray();
		for (KmlFeature item:mItems){
			JsonObject subJson = item.asGeoJSON(false);
			if (item instanceof KmlFolder){
				//Flatten the item contents:
				JsonArray subFeatures = subJson.getAsJsonArray("features");
				if (subFeatures != null){
					for (int i=0; i<subFeatures.size(); i++){
						JsonElement j = subFeatures.get(i);
						features.add(j);
					}
				}
			} else if (subJson != null) {
				features.add(subJson);
			}
		}
		json.add("features", features);
		json.addProperty("type", "FeatureCollection");
		return json;
	}
	
	//Cloneable implementation ------------------------------------

	public KmlFolder clone(){
		KmlFolder kmlFolder = (KmlFolder)super.clone();
		if (mItems != null){
			kmlFolder.mItems = new ArrayList<KmlFeature>(mItems.size());
			for (KmlFeature item:mItems)
				kmlFolder.mItems.add(item.clone());
		}
		return kmlFolder;
	}
	
	//Parcelable implementation ------------
	
	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeList(mItems);
	}
	
	public static final Creator<KmlFolder> CREATOR = new Creator<KmlFolder>() {
		@Override public KmlFolder createFromParcel(Parcel source) {
			return new KmlFolder(source);
		}
		@Override public KmlFolder[] newArray(int size) {
			return new KmlFolder[size];
		}
	};
	
	public KmlFolder(Parcel in){
		super(in);
		mItems = in.readArrayList(KmlFeature.class.getClassLoader());
	}
}
