package org.osmdroid.bonuspack.overlays;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import java.util.ArrayList;

/**
 * Place an image on the map, each corner (4) of the image being associated with a {@link GeoPoint}
 * or only top-left and bottom-right corners
 * @since 6.1.1
 * @author Fabrice Fontaine
 * Triggered by issue 1361 (https://github.com/osmdroid/osmdroid/issues/1361)
 * *****************************************************************************************
 * TODO - TO BE REMOVED BECAUSE REPLACED BY OSMDROID.GROUNDOVERLAY, ONCE IT WILL BE RELEASED
 * *****************************************************************************************
 */
public class GroundOverlay extends Overlay {

	private final Paint mPaint = new Paint();
	private final Matrix mMatrix = new Matrix();

	private float mBearing;
	private float mTransparency;
	private Bitmap mImage;

	private float[] mMatrixSrc;
	private float[] mMatrixDst;

	private GeoPoint mTopLeft;
	private GeoPoint mTopRight;
	private GeoPoint mBottomRight;
	private GeoPoint mBottomLeft;

	public GroundOverlay() {
		super();
		mBearing = 0.0f;
		setTransparency(0.0f);
	}

	public void setImage(final Bitmap pImage){
		mImage = pImage;
		mMatrixSrc = null;
	}

	public Bitmap getImage() {
		return mImage;
	}

	public void setBearing(final float pBearing){
		mBearing = pBearing;
	}

	public float getBearing(){
		return mBearing;
	}

	public void setTransparency(final float pTransparency){
		mTransparency = pTransparency;
		mPaint.setAlpha(255-(int)(mTransparency * 255));
	}

	public float getTransparency(){
		return mTransparency;
	}

	@Override
	public void draw(final Canvas pCanvas, final Projection pProjection) {
		if(mImage == null) {
			return;
		}
		computeMatrix(pProjection);
		pCanvas.drawBitmap(mImage, mMatrix, mPaint);
	}

	public void setPositionFromBounds(BoundingBox bb){
		setPositionFromBounds(
				new GeoPoint(bb.getLatNorth(), bb.getLonWest()),
				new GeoPoint(bb.getLatSouth(), bb.getLonEast()));
	}

	public void setPositionFromBounds(final GeoPoint pTopLeft, final GeoPoint pBottomRight) {
		mMatrix.reset();
		mMatrixSrc = null;
		mMatrixDst = null;
		mTopLeft = new GeoPoint(pTopLeft);
		mTopRight = null;
		mBottomRight = new GeoPoint(pBottomRight);
		mBottomLeft = null;
	}

	public void setPositionFromBounds(final GeoPoint pTopLeft, final GeoPoint pTopRight,
							final GeoPoint pBottomRight, final GeoPoint pBottomLeft) {
		mMatrix.reset();
		mTopLeft = new GeoPoint(pTopLeft);
		mTopRight = new GeoPoint(pTopRight);
		mBottomRight = new GeoPoint(pBottomRight);
		mBottomLeft = new GeoPoint(pBottomLeft);
	}

    /**
     * @return the list of bounds, as:
     *  - top-left, bottom-right, if 2 bounds
     *  - clock-wise from top-left, if 4 bounds
     */
	public ArrayList<GeoPoint> getAllBounds(){
        ArrayList<GeoPoint> bounds = new ArrayList<GeoPoint>(4);
	    if (mTopRight == null) {
            bounds.add(mTopLeft);
            bounds.add(mBottomRight);
        } else {
            bounds.add(mTopLeft);
            bounds.add(mTopRight);
            bounds.add(mBottomRight);
            bounds.add(mBottomLeft);
        }
        return bounds;
	}

	// TODO check if performance-wise it would make sense to use the mMatrix.setPolyToPoly option
	// TODO even for the 2 corner case
	private void computeMatrix(final Projection pProjection) {
		if (mTopRight == null) { // only 2 corners
			final long x0 = pProjection.getLongPixelXFromLongitude(mTopLeft.getLongitude());
			final long y0 = pProjection.getLongPixelYFromLatitude(mTopLeft.getLatitude());
			final long x1 = pProjection.getLongPixelXFromLongitude(mBottomRight.getLongitude());
			final long y1 = pProjection.getLongPixelYFromLatitude(mBottomRight.getLatitude());
			final float widthOnTheMap = x1 - x0;
			final float heightOnTheMap = y1 - y0;
			final float scaleX = widthOnTheMap / mImage.getWidth();
			final float scaleY = heightOnTheMap / mImage.getHeight();
			mMatrix.setScale(scaleX, scaleY);
			mMatrix.postTranslate(x0, y0);
			return;
		}
		// 4 corners
		if (mMatrixSrc == null) {
			mMatrixSrc = new float[8];
			final int width = mImage.getWidth();
			final int height = mImage.getHeight();
			mMatrixSrc[0] = 0;
			mMatrixSrc[1] = 0;
			mMatrixSrc[2] = width;
			mMatrixSrc[3] = 0;
			mMatrixSrc[4] = width;
			mMatrixSrc[5] = height;
			mMatrixSrc[6] = 0;
			mMatrixSrc[7] = height;
		}
		if (mMatrixDst == null) {
			mMatrixDst = new float[8];
		}
		final long topLeftCornerX = pProjection.getLongPixelXFromLongitude(mTopLeft.getLongitude());
		final long topLeftCornerY = pProjection.getLongPixelYFromLatitude(mTopLeft.getLatitude());
		final long topRightCornerX = pProjection.getLongPixelXFromLongitude(mTopRight.getLongitude());
		final long topRightCornerY = pProjection.getLongPixelYFromLatitude(mTopRight.getLatitude());
		final long bottomRightCornerX = pProjection.getLongPixelXFromLongitude(mBottomRight.getLongitude());
		final long bottomRightCornerY = pProjection.getLongPixelYFromLatitude(mBottomRight.getLatitude());
		final long bottomLeftCornerX = pProjection.getLongPixelXFromLongitude(mBottomLeft.getLongitude());
		final long bottomLeftCornerY = pProjection.getLongPixelYFromLatitude(mBottomLeft.getLatitude());
		mMatrixDst[0] = (float) topLeftCornerX;
		mMatrixDst[1] = (float) topLeftCornerY;
		mMatrixDst[2] = (float) topRightCornerX;
		mMatrixDst[3] = (float) topRightCornerY;
		mMatrixDst[4] = (float) bottomRightCornerX;
		mMatrixDst[5] = (float) bottomRightCornerY;
		mMatrixDst[6] = (float) bottomLeftCornerX;
		mMatrixDst[7] = (float) bottomLeftCornerY;

		mMatrix.setPolyToPoly(mMatrixSrc, 0, mMatrixDst, 0, 4);
	}
}