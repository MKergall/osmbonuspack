package org.osmdroid.bonuspack.clustering;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;

/** 
 * Cluster of Markers. 
 * @author M.Kergall
 */
public class StaticCluster {
	protected final ArrayList<Marker> mItems = new ArrayList<Marker>();
	protected GeoPoint mCenter;
	protected Marker mMarker;
	
	public StaticCluster(GeoPoint center) {
	    mCenter = center;
	}
	
	public void setPosition(GeoPoint center){
		mCenter = center;
	}
	
	public GeoPoint getPosition() {
	    return mCenter;
	}
	
	public int getSize() {
	    return mItems.size();
	}
	
	public Marker getItem(int index) {
	    return mItems.get(index);
	}
	
	public boolean add(Marker t) {
	    return mItems.add(t);
	}
	
	/** set the Marker to be displayed for this cluster */
	public void setMarker(Marker marker){
		mMarker = marker;
	}
	
	/** @return the Marker to be displayed for this cluster */
	public Marker getMarker(){
		return mMarker;
	}

	public BoundingBox getBoundingBox(){
		if (mItems.size()==0)
			return null;
		GeoPoint p = getItem(0).getPosition();
		BoundingBox bb = new BoundingBox(p.getLatitude(), p.getLongitude(), p.getLatitude(), p.getLongitude());
		for (int i=1; i<getSize(); i++) {
			p = getItem(i).getPosition();
            double minLat = Math.min(bb.getLatSouth(), p.getLatitude());
            double minLon = Math.min(bb.getLonWest(), p.getLongitude());
            double maxLat = Math.max(bb.getLatNorth(), p.getLatitude());
            double maxLon = Math.max(bb.getLonEast(), p.getLongitude());
            bb.set(maxLat, maxLon, minLat, minLon);
		}
		return bb;
	}
}
