package org.osmdroid.bonuspack.overlays;

import android.graphics.Canvas;

import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A Folder overlay implementing 2 advanced features:
 *
 * 1) Z-Index to all overlays that it contains.
 * Like Google Maps Android API:
 * "An overlay with a larger z-index is drawn over overlays with smaller z-indices.
 * The order of overlays with the same z-index value is arbitrary.
 * The default is 0."
 * Unlike Google Maps Android API, this applies to all overlays, including Markers.
 *
 * 2) Drawing optimization based on Bounding Box culling.
 *
 * TODO - DO NOT USE YET. WORK IN PROGRESS.
 *
 * @author M.Kergall
 */

public class FolderZOverlay extends Overlay {
    protected TreeSet<ZOverlay> mList;
    protected String mName, mDescription;

    protected class ZOverlay implements Comparator<ZOverlay> {
        float mZIndex;
        BoundingBox mBoundingBox;
        boolean mBoundingBoxSet;
        Overlay mOverlay;

        public ZOverlay(Overlay o, float zIndex){
            mOverlay = o;
            mZIndex = zIndex;
            mBoundingBoxSet = false;
        }

        @Override public int compare(ZOverlay o1, ZOverlay o2){
            return (int)Math.signum(o1.mZIndex - o2.mZIndex);
        }

        public void setBoundingBox(BoundingBox bb){
            mBoundingBox = bb.clone();
            mBoundingBoxSet = true;
        }

        public void unsetBoundingBox(){
            mBoundingBox = null;
            mBoundingBoxSet = false;
        }

        /**
         * @param mapBB bounding box of the map view
         * @param mapOrientation orientation of the map view
         * @return true if the overlay should be drawn.
         */
        public boolean shouldBeDrawn(BoundingBox mapBB, float mapOrientation){
            if (!mBoundingBoxSet)
                return true;
            if (mBoundingBox == null)
                //null bounding box means overlay is empty, so nothing to draw:
                return false;
            if (mapOrientation != 0.0f)
                //TODO - handle map rotation...
                return true;
            if (mBoundingBox.getLatSouth() > mapBB.getLatNorth()
                || mBoundingBox.getLatNorth() < mapBB.getLatSouth()
                || mBoundingBox.getLonWest() > mapBB.getLonEast()
                || mBoundingBox.getLonEast() < mapBB.getLonWest())
                //completely outside the map view:
                return false;
            return true;
        }
    }

    public FolderZOverlay(){
        super();
        mList = new TreeSet<>();
        mName = "";
        mDescription = "";
    }

    public void setName(String name){
        mName = name;
    }

    public String getName(){
        return mName;
    }

    public void setDescription(String description){
        mDescription = description;
    }

    public String getDescription(){
        return mDescription;
    }

    public boolean add(Overlay item, float zIndex){
        return mList.add(new ZOverlay(item, zIndex));
    }

    public boolean add(Overlay item){
        return add(item, 0);
    }

    protected ZOverlay get(Overlay overlay){
        Iterator<ZOverlay> itr = mList.iterator();
        while (itr.hasNext()) {
            ZOverlay item = itr.next();
            if (item.mOverlay == overlay) {
                mList.remove(item);
                return item;
            }
        }
        return null;
    }

    public boolean remove(Overlay overlay) {
        ZOverlay item = get(overlay);
        if (item != null) {
            mList.remove(item);
            return true;
        }
        else
            return false;
   }

    /**
     * Change the Z-Index of an overlay.
     * @param overlay overlay to change
     * @param zIndex new Z-Index to set
     */
    public void setZIndex(Overlay overlay, float zIndex){
        ZOverlay item = get(overlay);
        if (item == null)
            return;
        mList.remove(item);
        item.mZIndex = zIndex; //TODO Check if removal/addition is really necessary.
        mList.add(item);
    }

    /**
     * Define the bounding box of this overlay.
     * This may dramatically increase drawing performance when the overlay is completely outside the current view.
     * @param overlay
     * @param bb the bounding box of this overlay.
     */
    public void setBoundingBox(Overlay overlay, BoundingBox bb){
        ZOverlay item = get(overlay);
        if (item == null)
            return;
        item.setBoundingBox(bb);
    }

    public void unsetBoundingBox(Overlay overlay){
        ZOverlay item = get(overlay);
        if (item == null)
            return;
        item.unsetBoundingBox();
    }

    //TODO:
    //get highest z-index => getMaxZIndex

    @Override public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow)
            return;

        Iterator<ZOverlay> itr=mList.iterator();
        while(itr.hasNext()){
            ZOverlay item = itr.next();
            Overlay overlay = item.mOverlay;
            if (overlay!=null && overlay.isEnabled()) {
                if (item.shouldBeDrawn(mapView.getBoundingBox(), mapView.getMapOrientation())) {
                    overlay.draw(canvas, mapView, false);
                }
            }
        }
    }

    //TODO Implement events
}
