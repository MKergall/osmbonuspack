package org.osmdroid.bonuspack.kml;

import android.content.Context;
import android.os.Parcel;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * KML gx:Track
 * @author M.Kergall
 */
public class KmlTrack extends KmlGeometry {
	static int mDefaultLayoutResId = BonusPackHelper.UNDEFINED_RES_ID;

	public ArrayList<Date> mWhen;
	//public ArrayList<GeoPoint> mAngles; //TODO later...

	public KmlTrack(){
		super();
		mCoordinates = new ArrayList<>();
		mWhen = new ArrayList<>();
	}

	/**
	 * @param sGxCoord gx:Coord, string with "lon lat alt", comma-separated.
	 * @return coord as a GeoPoint, or null if the element is empty or if parsing issue.
     */
	public static GeoPoint parseKmlGxCoord(String sGxCoord) {
		int end1 = sGxCoord.indexOf(' ');
		int end2 = sGxCoord.indexOf(' ', end1+1);
		try {
			double lon = Double.parseDouble(sGxCoord.substring(0, end1));
			double lat = Double.parseDouble(sGxCoord.substring(end1+1, end2));
			double alt = Double.parseDouble(sGxCoord.substring(end2+1, sGxCoord.length()));
			return new GeoPoint(lat, lon, alt);
		} catch (NumberFormatException e) {
			return null;
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public void addGxCoord(String sGxCoord){
		mCoordinates.add(parseKmlGxCoord(sGxCoord));
	}

	/**
	 * @param sWhen "when" string, in one of the KML dateTime formats. Local time format not supported yet.
	 * @return java Date if success, or null
     */
	public static Date parseKmlWhen(String sWhen) {
		SimpleDateFormat ft;
		switch (sWhen.length()) {
			case 4: ft = new SimpleDateFormat("yyyy"); break;
			case 7:
				ft = new SimpleDateFormat("yyyy-MM");
				break;
			case 10:
				ft = new SimpleDateFormat("yyyy-MM-dd");
				break;
			case 19:
				ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				break;
			case 20:
				ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				break;
			default:
				return null;
		}
		Date when;
		try {
			when = ft.parse(sWhen);
			return when;
		} catch (ParseException e) {
			return null;
		}
	}

	public void addWhen(String sWhen){
		if (mWhen == null)
			mWhen = new ArrayList<>();
		mWhen.add(parseKmlWhen(sWhen));
	}

	/**
	 * Add a time element (coord+when) to the track.
	 *
	 * @param coord
	 * @param when
	 */
	public void add(GeoPoint coord, Date when) {
		if (coord == null)
			mCoordinates.add(coord);
		else
			mCoordinates.add(coord.clone());
		mWhen.add(when);
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

	/** Build the corresponding overlay.
	 * Currently: a Polyline of gx:coords */
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
			styler.onTrack(lineStringOverlay, kmlPlacemark, this);
		else {
			applyDefaultStyling(lineStringOverlay, defaultStyle, kmlPlacemark, kmlDocument, map);
		}
		return lineStringOverlay;
	}

	static final SimpleDateFormat KML_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	@Override public void saveAsKML(Writer writer){
		try {
			writer.write("<gx:Track>\n");
			//write when:
			for (Date when:mWhen){
				writer.write("<when>");
				if (when != null)
					writer.write(KML_DATE_FORMAT.format(when));
				writer.write("</when>\n");
			}
			//write coords:
			for (GeoPoint coord:mCoordinates){
				writer.write("<gx:coord>");
				if (coord != null)
					writer.write(coord.getLongitude() + " " + coord.getLatitude() + " " + coord.getAltitude());
				writer.write("</gx:coord>\n");
			}
			writer.write("</gx:Track>\n");
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

	@Override public BoundingBox getBoundingBox() {
		return BoundingBox.fromGeoPoints(mCoordinates);
	}

	//Cloneable implementation ------------------------------------

	@Override public KmlTrack clone(){
		KmlTrack cloned = (KmlTrack) super.clone();
		cloned.mWhen = new ArrayList<>(mWhen.size());
		for (Date d : mWhen)
			cloned.mWhen.add((Date) d.clone());
		return cloned;
	}

	//Parcelable implementation ------------

	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeList(mWhen);
	}

	public static final Creator<KmlTrack> CREATOR = new Creator<KmlTrack>() {
		@Override public KmlTrack createFromParcel(Parcel source) {
			return new KmlTrack(source);
		}
		@Override public KmlTrack[] newArray(int size) {
			return new KmlTrack[size];
		}
	};

	public KmlTrack(Parcel in){
		super(in);
		mWhen = in.readArrayList(Date.class.getClassLoader());
	}
}
