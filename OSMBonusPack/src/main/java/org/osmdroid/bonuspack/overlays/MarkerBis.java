package org.osmdroid.bonuspack.overlays;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;

import org.osmdroid.util.RectL;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;

public class MarkerBis extends Marker {

    public MarkerBis(MapView mapView) {
        super(mapView);
    }

    @Override public boolean hitTest(final MotionEvent event, final MapView mapView) {
        return mIcon != null && mDisplayed && mOrientedMarkerRect.contains((int) event.getX(), (int) event.getY()); // "!=null": fix for #1078
    }

    @Override
    public void draw(Canvas canvas, Projection pj) {
        if (mIcon == null)
            return;
        if (!isEnabled())
            return;

        pj.toPixels(mPosition, mPositionPixels);

        float rotationOnScreen = (mFlat ? -mBearing : -pj.getOrientation() - mBearing);
        drawAt(canvas, mPositionPixels.x, mPositionPixels.y, rotationOnScreen);
        if (isInfoWindowShown()) {
            //showInfoWindow();
            mInfoWindow.draw();
        }
    }

    private final Rect mOrientedMarkerRect = new Rect();
    private final Rect mRect = new Rect();
    private boolean mDisplayed;

    protected void drawAt(final Canvas pCanvas, final int pX, final int pY, final float pOrientation) {
        final int markerWidth = mIcon.getIntrinsicWidth();
        final int markerHeight = mIcon.getIntrinsicHeight();
        final int offsetX = pX - Math.round(markerWidth * mAnchorU);
        final int offsetY = pY - Math.round(markerHeight * mAnchorV);
        mRect.set(offsetX, offsetY, offsetX + markerWidth, offsetY + markerHeight);
        RectL.getBounds(mRect, pX, pY, pOrientation, mOrientedMarkerRect);
        mDisplayed = Rect.intersects(mOrientedMarkerRect, pCanvas.getClipBounds());
        if (!mDisplayed) { // optimization 1: (much faster, depending on the proportions) don't try to display if the Marker is not visible
            return;
        }
        if (mAlpha == 0) {
            return;
        }
        if (pOrientation != 0) { // optimization 2: don't manipulate the Canvas if not needed (about 25% faster) - step 1/2
            pCanvas.save();
            pCanvas.rotate(pOrientation, pX, pY);
        }
        /*
        if (mIcon instanceof BitmapDrawable) { // optimization 3: (about 15% faster)
            final Paint paint;
            if (mAlpha == 1) {
                paint = null;
            } else {
                if (mPaint == null) {
                    mPaint = new Paint();
                }
                mPaint.setAlpha((int) (mAlpha * 255));
                paint = mPaint;
            }
            pCanvas.drawBitmap(((BitmapDrawable) mIcon).getBitmap(), offsetX, offsetY, paint);
        } else
        */
        {
            mIcon.setAlpha((int) (mAlpha * 255));
            mIcon.setBounds(mRect);
            mIcon.draw(pCanvas);
        }
        if (pOrientation != 0) { // optimization 2: step 2/2
            pCanvas.restore();
        }
    }

}
