package org.osmdroid.bonuspack.overlays;

import android.content.Context;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MarkerWithStatus extends Marker {

    public MarkerWithStatus(MapView mapView) {
        super(mapView);
    }

    public MarkerWithStatus(MapView mapView, Context resourceProxy) {
        super(mapView, resourceProxy);
    }

    public void setFocused(boolean mFocused) {
        if (mIcon == null) return;
        if (mFocused) {
            mIcon.setState(new int[]{android.R.attr.state_focused});
        } else {
            mIcon.setState(new int[]{android.R.attr.state_single});
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
        setFocused(false);
        return super.onSingleTapUp(e, mapView);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event, MapView mapView) {
        boolean touched = hitTest(event, mapView);
        setFocused(touched);
        return super.onSingleTapConfirmed(event, mapView);
    }

}
