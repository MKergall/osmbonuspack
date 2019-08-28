package org.osmdroid.bonuspack.kml;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.osmdroid.bonuspack.overlays.GroundOverlay;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * KML GroundOverlay. 
 * 
 * mCoordinates contains the LatLonBox as 2 GeoPoints: North-West, and South-East. 
 * 
 * @author M.Kergall
 */
public class KmlGroundOverlay extends KmlFeature implements Cloneable, Parcelable {
	/** Overlay Icon href */
	public String mIconHref;
	/** Overlay Icon bitmap (can be null) */
	public Bitmap mIcon;
	/** Overlay color */
	public int mColor;
	/** GroundOverlay rotation - default = 0 */
	public float mRotation;
	/** if LatLonBox: NW and SE points - if gx:LatLonQuad: the 4 geopoints of the nonrectangular quadrilateral */
	public ArrayList<GeoPoint> mCoordinates;

	public KmlGroundOverlay(){
		super();
		mColor = 0xFF000000;
	}

	/** Constructs the KML feature from a GroundOverlay. */
	public KmlGroundOverlay(GroundOverlay overlay){
		this();
		mCoordinates = overlay.getAllBounds();
		//mIconHref = ???
		mIcon = overlay.getImage();
		mRotation = -overlay.getBearing();
		mColor = 255 - Color.alpha((int)(overlay.getTransparency()*255));
		mVisibility = overlay.isEnabled();
	}
	
	@Override public BoundingBox getBoundingBox(){
		return BoundingBox.fromGeoPoints(mCoordinates);
	}
	
	/** load the icon from its href. 
	 * @param href either the full url, or a relative path to a local file. 
	 * @param containerFile the KML container file - or null if irrelevant. 
	 * @param kmzContainer current KMZ file (as a ZipFile) - or null if irrelevant. 
	 */
	public void setIcon(String href, File containerFile, ZipFile kmzContainer){
		mIconHref = href;
		if (mIconHref.startsWith("http://") || mIconHref.startsWith("https://")){
			mIcon = BonusPackHelper.loadBitmap(mIconHref);
		} else if (kmzContainer == null) {
			if (containerFile != null){
				String actualFullPath = containerFile.getParent()+'/'+mIconHref;
				mIcon = BitmapFactory.decodeFile(actualFullPath);
			} else
				mIcon = null;
		} else {
			try {
				final ZipEntry fileEntry = kmzContainer.getEntry(href);
				InputStream stream = kmzContainer.getInputStream(fileEntry);
				mIcon = BitmapFactory.decodeStream(stream);
			} catch (Exception e) {
				mIcon = null;
			}
		}
	}
	
	public void setLatLonBox(double north, double south, double east, double west){
		mCoordinates = new ArrayList<GeoPoint>(2);
		mCoordinates.add(new GeoPoint(north, west));
		mCoordinates.add(new GeoPoint(south, east));
	}

	public void setLatLonQuad(ArrayList<GeoPoint> coords){
		mCoordinates = new ArrayList<GeoPoint>(coords.size());
		for (GeoPoint g:coords)
			mCoordinates.add(g.clone());
	}

	/** @return the corresponding GroundOverlay ready to display on the map */
	@Override public Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlDocument kmlDocument){
		GroundOverlay overlay = new GroundOverlay();
		if (mCoordinates.size()==2){
			//LatLonBox:
			GeoPoint pNW = mCoordinates.get(0);
			GeoPoint pSE = mCoordinates.get(1);
			overlay.setPositionFromBounds(pNW, pSE);
		} else if (mCoordinates.size()==4){
			//KML spec for nonrectangular quadrilateral:
			// "The coordinates must be specified in counter-clockwise order with the first coordinate corresponding
			// to the lower-left corner of the overlayed image." (car pourquoi faire simple quand on peut faire compliqué ?)
			overlay.setPositionFromBounds(mCoordinates.get(3), mCoordinates.get(2),
					mCoordinates.get(1),mCoordinates.get(0));
		} //else - error KML GroundOverlay not properly defined.

		if (mIcon != null){
			overlay.setImage(mIcon);
			//TODO: not clearly defined in KML spec, but color is supposed to be blended with the image. 
			float transparency = 1.0f - Color.alpha(mColor)/255.0f; //KML transparency is the transparency part of the "color" element. 
			overlay.setTransparency(transparency);
		} else {
			//when no image available, set it as a rectangle filled with the KML color
			Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
			bitmap.eraseColor(mColor);
			overlay.setImage(bitmap);
		}
		
		overlay.setBearing(-mRotation); //from KML counterclockwise to Google Maps API which is clockwise
		if (styler == null)
			overlay.setEnabled(mVisibility);
		else 
			styler.onFeature(overlay, this);
		return overlay;
	}
	
	/** write elements specific to GroundOverlay in KML format */
	@Override public void writeKMLSpecifics(Writer writer){
		try {
			writer.write("<color>"+ColorStyle.colorAsKMLString(mColor)+"</color>\n");
			writer.write("<Icon><href>"+StringEscapeUtils.escapeXml10(mIconHref)+"</href></Icon>\n");
			if (mCoordinates.size() == 2) {
				writer.write("<LatLonBox>");
				GeoPoint pNW = mCoordinates.get(0);
				GeoPoint pSE = mCoordinates.get(1);
				writer.write("<north>" + pNW.getLatitude() + "</north>");
				writer.write("<south>" + pSE.getLatitude() + "</south>");
				writer.write("<east>" + pSE.getLongitude() + "</east>");
				writer.write("<west>" + pNW.getLongitude() + "</west>");
				writer.write("<rotation>" + mRotation + "</rotation>");
				writer.write("</LatLonBox>\n");
			} else {
				writer.write("<gx:LatLonQuad>");
				KmlGeometry.writeKMLCoordinates(writer, mCoordinates);
				writer.write("</gx:LatLonQuad>\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override public JsonObject asGeoJSON(boolean isRoot) {
		//TODO: GroundOverlay is not supported by GeoJSON. Output enclosing polygon with mColor?
		return null;
	}
	
	//Cloneable implementation ------------------------------------

	public KmlGroundOverlay clone(){
		KmlGroundOverlay kmlGroundOverlay = (KmlGroundOverlay)super.clone();
		kmlGroundOverlay.mCoordinates = KmlGeometry.cloneArrayOfGeoPoint(mCoordinates);
		return kmlGroundOverlay;
	}
	
	//Parcelable implementation ------------
	
	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(mIconHref);
		out.writeParcelable(mIcon, flags);
		out.writeInt(mColor);
		out.writeFloat(mRotation);
		out.writeList(mCoordinates);
	}
	
	public static final Creator<KmlGroundOverlay> CREATOR = new Creator<KmlGroundOverlay>() {
		@Override public KmlGroundOverlay createFromParcel(Parcel source) {
			return new KmlGroundOverlay(source);
		}
		@Override public KmlGroundOverlay[] newArray(int size) {
			return new KmlGroundOverlay[size];
		}
	};
	
	public KmlGroundOverlay(Parcel in){
		super(in);
		mIconHref = in.readString();
		mIcon = in.readParcelable(Bitmap.class.getClassLoader());
		mColor = in.readInt();
		mRotation = in.readFloat();
		mCoordinates = in.readArrayList(GeoPoint.class.getClassLoader());
	}

}
