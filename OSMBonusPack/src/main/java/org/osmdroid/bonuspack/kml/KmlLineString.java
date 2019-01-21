package org.osmdroid.bonuspack.kml;

import android.content.Context;
import android.os.Parcel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.osmdroid.bonuspack.kml.KmlFeature.Styler;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * KML and/or GeoJSON LineString
 * @author M.Kergall
 */
public class KmlLineString extends KmlGeometry {
	static int mDefaultLayoutResId = BonusPackHelper.UNDEFINED_RES_ID; 

	public KmlLineString(){
		super();
	}

	/** GeoJSON constructor, from GeoJSON array of coordinates */
	protected KmlLineString(JsonArray coordinates){
		this();
		mCoordinates = KmlGeometry.parseGeoJSONPositions(coordinates);
	}

	/** GeoJSON constructor, from full GeoJSON LineString object */
	public KmlLineString(JsonObject json){
		this(json.get("coordinates").getAsJsonArray());
	}
	
	public void applyDefaultStyling(Polyline lineStringOverlay, Style defaultStyle, KmlPlacemark kmlPlacemark,
			KmlDocument kmlDocument, MapView map){
		Context context = map.getContext();
		Style style = kmlDocument.getStyle(kmlPlacemark.mStyle);
		if (style != null){
			lineStringOverlay.setColor(style.getOutlinePaint().getColor());
			lineStringOverlay.setWidth(style.getOutlinePaint().getStrokeWidth());
		} else if (defaultStyle!=null && defaultStyle.mLineStyle!=null){ 
			lineStringOverlay.setColor(defaultStyle.getOutlinePaint().getColor());
			lineStringOverlay.setWidth(defaultStyle.getOutlinePaint().getStrokeWidth());
		}
		if ((kmlPlacemark.mName!=null && !"".equals(kmlPlacemark.mName)) 
				|| (kmlPlacemark.mDescription!=null && !"".equals(kmlPlacemark.mDescription))
				|| (lineStringOverlay.getSubDescription()!=null && !"".equals(lineStringOverlay.getSubDescription()))
				){
			if (mDefaultLayoutResId == BonusPackHelper.UNDEFINED_RES_ID){
				String packageName = context.getPackageName();
				mDefaultLayoutResId = context.getResources().getIdentifier("layout/bonuspack_bubble", null, packageName);
			}
			lineStringOverlay.setInfoWindow(new BasicInfoWindow(mDefaultLayoutResId, map));
		}
		lineStringOverlay.setEnabled(kmlPlacemark.mVisibility);
	}
	
	/** Build the corresponding Polyline overlay */	
	@Override public Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlPlacemark kmlPlacemark, 
			KmlDocument kmlDocument){
		Polyline lineStringOverlay = new Polyline();
		lineStringOverlay.setGeodesic(true);
		lineStringOverlay.setPoints(mCoordinates);
		lineStringOverlay.setTitle(kmlPlacemark.mName);
		lineStringOverlay.setSnippet(kmlPlacemark.mDescription);
		lineStringOverlay.setSubDescription(kmlPlacemark.getExtendedDataAsText());
		lineStringOverlay.setRelatedObject(this);
		lineStringOverlay.setId(mId);
		if (styler != null)
			styler.onLineString(lineStringOverlay, kmlPlacemark, this);
		else {
			applyDefaultStyling(lineStringOverlay, defaultStyle, kmlPlacemark, kmlDocument, map);
		}
		return lineStringOverlay;
	}
	
	@Override public void saveAsKML(Writer writer){
		try {
			writer.write("<LineString>\n");
			writeKMLCoordinates(writer, mCoordinates);
			writer.write("</LineString>\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override public JsonObject asGeoJSON(){
		JsonObject json = new JsonObject();
		json.addProperty("type", "LineString");
		json.add("coordinates", KmlGeometry.geoJSONCoordinates(mCoordinates));
		return json;
	}
	
	@Override public BoundingBox getBoundingBox(){
		if (mCoordinates!=null)
			return BoundingBox.fromGeoPoints(mCoordinates);
		else 
			return null;
	}
	
	//Cloneable implementation ------------------------------------
	
	@Override public KmlLineString clone(){
		return (KmlLineString)super.clone();
	}
	
	//Parcelable implementation ------------
	
	@Override public int describeContents() {
		return 0;
	}

	//@Override public void writeToParcel(Parcel out, int flags) {
	//	super.writeToParcel(out, flags);
	//}
	
	public static final Creator<KmlLineString> CREATOR = new Creator<KmlLineString>() {
		@Override public KmlLineString createFromParcel(Parcel source) {
			return new KmlLineString(source);
		}
		@Override public KmlLineString[] newArray(int size) {
			return new KmlLineString[size];
		}
	};
	
	public KmlLineString(Parcel in){
		super(in);
	}
}
