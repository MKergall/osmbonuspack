package com.example.osmbonuspacktuto;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlFolder;
import org.osmdroid.bonuspack.kml.KmlLineString;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlPoint;
import org.osmdroid.bonuspack.kml.KmlPolygon;
import org.osmdroid.bonuspack.kml.KmlTrack;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Marker.OnMarkerDragListener;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the implementation of OSMBonusPack tutorials.
 * Sections of code can be commented/uncommented depending on the progress in the tutorials.
 *
 * @author M.Kergall
 * @see <a href="https://github.com/MKergall/osmbonuspack">OSMBonusPack on GitHub</a>
 */
public class MainActivity extends Activity implements MapEventsReceiver, MapView.OnFirstLayoutListener {

	MapView map;
	KmlDocument mKmlDocument;

	@Override protected void onCreate(Bundle savedInstanceState) {

		//Disable StrictMode.ThreadPolicy to perform network calls in the UI thread.
		//Yes, it's not the good practice, but this is just a tutorial!
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		//Introduction
		super.onCreate(savedInstanceState);
		Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

		setContentView(R.layout.main);
		map = (MapView) findViewById(R.id.map);
		map.setMultiTouchControls(true);
		GeoPoint startPoint = new GeoPoint(48.13, -1.63);
		IMapController mapController = map.getController();
		mapController.setZoom(10.0);
		mapController.setCenter(startPoint);

		//0. Using the Marker overlay
		Marker startMarker = new Marker(map);
		startMarker.setPosition(startPoint);
		startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
		startMarker.setTitle("Start point");
		//startMarker.setIcon(getResources().getDrawable(R.drawable.marker_kml_point).mutate());
		//startMarker.setImage(getResources().getDrawable(R.drawable.ic_launcher));
		//startMarker.setInfoWindow(new MarkerInfoWindow(R.layout.bonuspack_bubble_black, map));
		startMarker.setDraggable(true);
		startMarker.setOnMarkerDragListener(new OnMarkerDragListenerDrawer());
		map.getOverlays().add(startMarker);

		//1. "Hello, Routing World"
		RoadManager roadManager = new OSRMRoadManager(this, "OBP_Tuto/1.0");
		//2. Playing with the RoadManager
		//((OSRMRoadManager)roadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE);
		ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
		waypoints.add(startPoint);
		GeoPoint endPoint = new GeoPoint(48.4, -1.9);
		waypoints.add(endPoint);
		Road road = roadManager.getRoad(waypoints);
		if (road.mStatus != Road.STATUS_OK)
			Toast.makeText(this, "Error when loading the road - status=" + road.mStatus, Toast.LENGTH_SHORT).show();

		Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
		map.getOverlays().add(roadOverlay);

		//3. Showing the Route steps on the map
		FolderOverlay roadMarkers = new FolderOverlay();
		map.getOverlays().add(roadMarkers);
		Drawable nodeIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_node, null);
		for (int i = 0; i < road.mNodes.size(); i++) {
			RoadNode node = road.mNodes.get(i);
			Marker nodeMarker = new Marker(map);
			nodeMarker.setPosition(node.mLocation);
			nodeMarker.setIcon(nodeIcon);
			nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

			//4. Filling the bubbles
			nodeMarker.setTitle("Step " + i);
			nodeMarker.setSnippet(node.mInstructions);
			nodeMarker.setSubDescription(Road.getLengthDurationText(this, node.mLength, node.mDuration));
			Drawable iconContinue = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_continue, null);
			nodeMarker.setImage(iconContinue);
			//4. end

			roadMarkers.add(nodeMarker);
		}

		//5. OpenStreetMap POIs with Nominatim
		NominatimPOIProvider poiProvider = new NominatimPOIProvider("OsmNavigator/1.0");
		ArrayList<POI> pois = poiProvider.getPOICloseTo(startPoint, "cinema", 50, 0.1);
		//or : ArrayList<POI> pois = poiProvider.getPOIAlong(road.getRouteLow(), "fuel", 50, 2.0);

		//6. Wikipedia POIs with GeoNames 
		/*
		GeoNamesPOIProvider poiProvider = new GeoNamesPOIProvider("mkergall");
		//BoundingBox bb = map.getBoundingBox();
		//ArrayList<POI> pois = poiProvider.getPOIInside(bb, 30);
		//=> not possible in onCreate, as map bounding box is not correct until a draw occurs (osmdroid issue). 
		ArrayList<POI> pois = poiProvider.getPOICloseTo(startPoint, 30, 20.0);
		*/

		//8. Quick overview of the Flickr and Picasa POIs */
		/*
		PicasaPOIProvider poiProvider = new PicasaPOIProvider(null);
		BoundingBox bb = BoundingBox.fromGeoPoints(waypoints);
		ArrayList<POI> pois = poiProvider.getPOIInside(bb, 20, null);
		*/

		//FolderOverlay poiMarkers = new FolderOverlay(this);
		//10. Marker Clustering
		RadiusMarkerClusterer poiMarkers = new RadiusMarkerClusterer(this);
		//end of 10.
		//11.1 Customizing the clusters design
		Bitmap clusterIcon = BonusPackHelper.getBitmapFromVectorDrawable(this, R.drawable.marker_poi_cluster);
		poiMarkers.setIcon(clusterIcon);
		poiMarkers.getTextPaint().setTextSize(12 * getResources().getDisplayMetrics().density);
		poiMarkers.mAnchorV = Marker.ANCHOR_BOTTOM;
		poiMarkers.mTextAnchorU = 0.70f;
		poiMarkers.mTextAnchorV = 0.27f;
		//end of 11.1
		map.getOverlays().add(poiMarkers);
		Drawable poiIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_poi_default, null);
		if (pois != null) {
			for (POI poi : pois) {
				Marker poiMarker = new Marker(map);
				poiMarker.setTitle(poi.mType);
				poiMarker.setSnippet(poi.mDescription);
				poiMarker.setPosition(poi.mLocation);
				poiMarker.setIcon(poiIcon);
				poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
				if (poi.mThumbnail != null) {
					poiMarker.setImage(new BitmapDrawable(getResources(), poi.mThumbnail));
				}
				// 7.
				poiMarker.setInfoWindow(new CustomInfoWindow(map));
				poiMarker.setRelatedObject(poi);
				poiMarkers.add(poiMarker);
			}
		}

		//12. Loading KML content
		//String url = "http://mapsengine.google.com/map/kml?forcekml=1&mid=z6IJfj90QEd4.kUUY9FoHFRdE";
		mKmlDocument = new KmlDocument();
		//boolean ok = mKmlDocument.parseKMLUrl(url);

		//Get OpenStreetMap content as KML with Overpass API:
		OverpassAPIProvider overpassProvider = new OverpassAPIProvider();
		BoundingBox oBB = new BoundingBox(startPoint.getLatitude() + 0.25, startPoint.getLongitude() + 0.25,
				startPoint.getLatitude() - 0.25, startPoint.getLongitude() - 0.25);
		String oUrl = overpassProvider.urlForTagSearchKml("highway=speed_camera", oBB, 500, 30);
		boolean ok = overpassProvider.addInKmlFolder(mKmlDocument.mKmlRoot, oUrl);

		if (ok) {
			//13.1 Simple styling
			Drawable defaultMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_kml_point, null);
			Bitmap defaultBitmap = ((BitmapDrawable) defaultMarker).getBitmap();
			Style defaultStyle = new Style(defaultBitmap, 0x901010AA, 3.0f, 0x20AA1010);
			//13.2 Advanced styling with Styler
			KmlFeature.Styler styler = new MyKmlStyler(defaultStyle);

			FolderOverlay kmlOverlay = (FolderOverlay) mKmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, styler, mKmlDocument);
			map.getOverlays().add(kmlOverlay);
			BoundingBox bb = mKmlDocument.mKmlRoot.getBoundingBox();
			if (bb != null) {
				//map.zoomToBoundingBox(bb, false); //=> not working in onCreate - this is a well-known osmdroid issue.
				//Workaround:
				setInitialViewOn(bb);
			}
		} else
			Toast.makeText(this, "Error when loading KML", Toast.LENGTH_SHORT).show();

		//14. Grab overlays in KML structure, save KML document locally
		if (mKmlDocument.mKmlRoot != null) {
			KmlFolder root = mKmlDocument.mKmlRoot;
			root.addOverlay(roadOverlay, mKmlDocument);
			root.addOverlay(roadMarkers, mKmlDocument);
			mKmlDocument.saveAsKML(mKmlDocument.getDefaultPathForAndroid(this, "my_route.kml"));
			//15. Loading and saving of GeoJSON content
			mKmlDocument.saveAsGeoJSON(mKmlDocument.getDefaultPathForAndroid(this,"my_route.json"));
		}

		//16. Handling Map events
		MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);
		map.getOverlays().add(0, mapEventsOverlay); //inserted at the "bottom" of all overlays

		checkPermissions();
	}

	//--- Stuff for setting the mapview on a box at startup:
	BoundingBox mInitialBoundingBox = null;

	void setInitialViewOn(BoundingBox bb) {
		if (map.getScreenRect(null).height() == 0) {
			mInitialBoundingBox = bb;
			map.addOnFirstLayoutListener(this);
		} else
			map.zoomToBoundingBox(bb, false);
	}

	@Override
	public void onFirstLayout(View v, int left, int top, int right, int bottom) {
		if (mInitialBoundingBox != null)
			map.zoomToBoundingBox(mInitialBoundingBox, false);
	}
	//---

	//0. Using the Marker and Polyline overlays - advanced options
	class OnMarkerDragListenerDrawer implements OnMarkerDragListener {
		ArrayList<GeoPoint> mTrace;
		Polyline mPolyline;

		OnMarkerDragListenerDrawer() {
			mTrace = new ArrayList<GeoPoint>(100);
			mPolyline = new Polyline();
			mPolyline.setColor(0xAA0000FF);
			mPolyline.setWidth(2.0f);
			mPolyline.setGeodesic(true);
			map.getOverlays().add(mPolyline);
		}

		@Override public void onMarkerDrag(Marker marker) {
			//mTrace.add(marker.getPosition());
		}

		@Override public void onMarkerDragEnd(Marker marker) {
			mTrace.add(marker.getPosition());
			mPolyline.setPoints(mTrace);
			map.invalidate();
		}

		@Override public void onMarkerDragStart(Marker marker) {
			//mTrace.add(marker.getPosition());
		}
	}

	//7. Customizing the bubble behaviour
	class CustomInfoWindow extends MarkerInfoWindow {
		POI mSelectedPoi;

		public CustomInfoWindow(MapView mapView) {
			super(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
			Button btn = (Button) (mView.findViewById(org.osmdroid.bonuspack.R.id.bubble_moreinfo));
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					if (mSelectedPoi.mUrl != null) {
						Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mSelectedPoi.mUrl));
						view.getContext().startActivity(myIntent);
					} else {
						Toast.makeText(view.getContext(), "Button clicked", Toast.LENGTH_LONG).show();
					}
				}
			});
		}

		@Override
		public void onOpen(Object item) {
			super.onOpen(item);
			mView.findViewById(org.osmdroid.bonuspack.R.id.bubble_moreinfo).setVisibility(View.VISIBLE);
			Marker marker = (Marker) item;
			mSelectedPoi = (POI) marker.getRelatedObject();

			//8. put thumbnail image in bubble, fetching the thumbnail in background:
			if (mSelectedPoi.mThumbnailPath != null) {
				ImageView imageView = (ImageView) mView.findViewById(org.osmdroid.bonuspack.R.id.bubble_image);
				mSelectedPoi.fetchThumbnailOnThread(imageView);
			}
		}
	}

	//11.2 Customizing the clusters design - and beyond
	class CirclesGridMarkerClusterer extends RadiusMarkerClusterer {

		public CirclesGridMarkerClusterer(Context ctx) {
			super(ctx);
		}

		@Override
		public Marker buildClusterMarker(StaticCluster cluster, MapView mapView) {
			Marker m = new Marker(mapView);
			m.setPosition(cluster.getPosition());
			m.setInfoWindow(null);
			m.setAnchor(0.5f, 0.5f);
			int radius = (int) Math.sqrt(cluster.getSize() * 3);
			radius = Math.max(radius, 10);
			radius = Math.min(radius, 30);
			Bitmap finalIcon = Bitmap.createBitmap(radius * 2, radius * 2, mClusterIcon.getConfig());
			Canvas iconCanvas = new Canvas(finalIcon);
			Paint circlePaint = new Paint();
			if (cluster.getSize() < 20)
				circlePaint.setColor(Color.BLUE);
			else
				circlePaint.setColor(Color.RED);
			circlePaint.setAlpha(200);
			iconCanvas.drawCircle(radius, radius, radius, circlePaint);
			String text = "" + cluster.getSize();
			int textHeight = (int) (mTextPaint.descent() + mTextPaint.ascent());
			iconCanvas.drawText(text,
					mTextAnchorU * finalIcon.getWidth(),
					mTextAnchorV * finalIcon.getHeight() - textHeight / 2,
					mTextPaint);
			m.setIcon(new BitmapDrawable(mapView.getContext().getResources(), finalIcon));
			return m;
		}
	}

	//13.2 Loading KML content - Advanced styling with Styler
	class MyKmlStyler implements KmlFeature.Styler {
		Style mDefaultStyle;

		MyKmlStyler(Style defaultStyle) {
			mDefaultStyle = defaultStyle;
		}

		@Override
		public void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString) {
			//Custom styling:
			polyline.setColor(Color.GREEN);
			polyline.setWidth(Math.max(kmlLineString.mCoordinates.size() / 200.0f, 3.0f));
		}

		@Override
		public void onPolygon(Polygon polygon, KmlPlacemark kmlPlacemark, KmlPolygon kmlPolygon) {
			//Keeping default styling:
			kmlPolygon.applyDefaultStyling(polygon, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
		}

		@Override
		public void onTrack(Polyline polyline, KmlPlacemark kmlPlacemark, KmlTrack kmlTrack) {
			//Keeping default styling:
			kmlTrack.applyDefaultStyling(polyline, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
		}

		@Override
		public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {
			//Styling based on ExtendedData properties: 
			if (kmlPlacemark.getExtendedData("maxspeed") != null)
				kmlPlacemark.mStyle = "maxspeed";
			kmlPoint.applyDefaultStyling(marker, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
		}

		@Override
		public void onFeature(Overlay overlay, KmlFeature kmlFeature) {
			//If nothing to do, do nothing. 
		}
	}

	//16. Handling Map events
	@Override
	public boolean singleTapConfirmedHelper(GeoPoint p) {
		Toast.makeText(this, "Tap on (" + p.getLatitude() + "," + p.getLongitude() + ")", Toast.LENGTH_SHORT).show();
		InfoWindow.closeAllInfoWindowsOn(map);
		return true;
	}

	float mGroundOverlayBearing = 0.0f;

	@Override public boolean longPressHelper(GeoPoint p) {
		//Toast.makeText(this, "Long press", Toast.LENGTH_SHORT).show();
		//17. Using Polygon, defined as a circle:
		Polygon circle = new Polygon();
		circle.setPoints(Polygon.pointsAsCircle(p, 10000.0));
		circle.getFillPaint().setColor(0x12121212);
		circle.getOutlinePaint().setColor(Color.RED);
		circle.getOutlinePaint().setStrokeWidth(2);
		map.getOverlays().add(circle);
		circle.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map));
		circle.setTitle("Centered on " + p.getLatitude() + "," + p.getLongitude());

		//18. Using GroundOverlay
		/*
		GroundOverlay myGroundOverlay = new GroundOverlay();
		myGroundOverlay.setPosition(p);
		Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, null);
		myGroundOverlay.setImage(d.mutate());
		myGroundOverlay.setDimensions(2000.0f);
		//myGroundOverlay.setTransparency(0.25f);
		myGroundOverlay.setBearing(mGroundOverlayBearing);
		mGroundOverlayBearing += 20.0f;
		map.getOverlays().add(myGroundOverlay);
		*/

		/*
		Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.bonuspack_bubble_black, null);
		myGroundOverlay.setImage(d.mutate());
		BoundingBox bb = new BoundingBox(p.getLatitude()+map.getLatitudeSpanDouble()/2,
				p.getLongitude()+map.getLongitudeSpanDouble()/2,
				p.getLatitude(),
				p.getLongitude());
		myGroundOverlay.setPositionFromBounds(bb);
		map.getOverlays().add(myGroundOverlay);

		Marker ne = new Marker(map);
		ne.setPosition(new GeoPoint(bb.getLatNorth(), bb.getLonEast()));
		ne.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
		map.getOverlays().add(ne);
		Marker sw = new Marker(map);
		sw.setPosition(new GeoPoint(bb.getLatSouth(), bb.getLonWest()));
		sw.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
		map.getOverlays().add(sw);
		*/

		map.invalidate();
		return true;
	}

	final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

	void checkPermissions() {
		List<String> permissions = new ArrayList<>();
		String message = "Application permissions:";
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
			message += "\nLocation to show user location.";
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			message += "\nStorage access to store map tiles.";
		}
		if (!permissions.isEmpty()) {
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			String[] params = permissions.toArray(new String[permissions.size()]);
			ActivityCompat.requestPermissions(this, params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
		} // else: We already have permissions, so handle as normal
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
				Map<String, Integer> perms = new HashMap<>();
				// Initial
				perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
				// Fill with results
				for (int i = 0; i < permissions.length; i++)
					perms.put(permissions[i], grantResults[i]);
				// Check for WRITE_EXTERNAL_STORAGE
				Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
				if (!storage) {
					// Permission Denied
					Toast.makeText(this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
				} // else: permission was granted, yay!
			}
		}
	}

}
