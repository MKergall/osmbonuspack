package org.osmdroid.bonuspack.overlays;

import android.graphics.Canvas;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A Folder overlay implementing Z-Index to all overlays that it contains.
 * Like Google Maps Android API:
 * "An overlay with a larger z-index is drawn over overlays with smaller z-indices.
 * The order of overlays with the same z-index value is arbitrary.
 * The default is 0."
 * Unlike Google Maps Android API, this applies to all overlays, including Markers.
 *
 * TODO - WORK IN PROGRESS.
 *
 * @author M.Kergall
 */

public class FolderZOverlay extends Overlay {
    protected TreeSet<ZOverlay> mList;
    protected String mName, mDescription;

    protected class ZOverlay implements Comparator<ZOverlay> {
        float mZIndex;
        Overlay mOverlay;

        ZOverlay(Overlay o, float zIndex){
            mOverlay = o;
            mZIndex = zIndex;
        }

        @Override public int compare(ZOverlay o1, ZOverlay o2){
            return (int)Math.signum(o1.mZIndex - o2.mZIndex);
        }
    }

    FolderZOverlay(){
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

    //TODO:
    //get highest z-index => getMaxZIndex

    @Override protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow)
            return;

        Iterator<ZOverlay> itr=mList.iterator();
        while(itr.hasNext()){
            ZOverlay item = itr.next();
            Overlay overlay = item.mOverlay;
            if (overlay!=null && overlay.isEnabled()) {
                //TODO overlay.draw(canvas, mapView, false); - IMPOSSIBLE, private method!
            }
        }
    }

    //TODO Implement events
}
