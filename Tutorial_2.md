(we assume you already followed [Tutorial\_1](Tutorial_1.md))

# 5. OpenStreetMap POIs with Nominatim #
"Points Of Interest" (POIs in short) are a very common need in map applications.

Example: I would like to see where are the cinemas (or restaurants, or hotels, or fuel stations, or whatever...) close to my position.
OpenStreetMap contains a lot of POIs, and Nominatim service allows to search for them.

Now, we instantiate a Nominatim POI Provider and do the request:

```
NominatimPOIProvider poiProvider = new NominatimPOIProvider();
ArrayList<POI> pois = poiProvider.getPOICloseTo(startPoint, "cinema", 50, 0.1);
```

Nominatim searches in OpenStreetMap features using specific keywords: the "Special Phrases" (http://wiki.openstreetmap.org/wiki/Nominatim/Special_Phrases).
Take care that, either because some features are rarely described in OSM, or because of Nominatim itself, there are keywords that return few or no results.

You can refer to  http://code.google.com/p/osmbonuspack/source/browse/trunk/OSMNavigator/res/values/poi_tags.xml to get a sub-list of features that are really - really - available through Nominatim.


OK, back to the code. We can build the POI markers, and put them on the map.

Let us improve the coding a little bit, grouping all those POI markers in a FolderOverlay.
This is not mandatory, but organizing the markers is very helpful in real applications.

We create the FolderOverlay and put it on the map this way:
```
FolderOverlay poiMarkers = new FolderOverlay(this);
map.getOverlays().add(poiMarkers);
```

Then we can create the markers, and put them in the FolderOverlay:
```
Drawable poiIcon = getResources().getDrawable(R.drawable.marker_poi_default);
for (POI poi:pois){
	Marker poiMarker = new Marker(map);
	poiMarker.setTitle(poi.mType);
	poiMarker.setSnippet(poi.mDescription);
	poiMarker.setPosition(poi.mLocation);
	poiMarker.setIcon(poiIcon);
	if (poi.mThumbnail != null){
		poiItem.setImage(new BitmapDrawable(poi.mThumbnail));
	}
	poiMarkers.add(poiMarker);
}
```

Okay, you can test it. Clicking on a POI marker will open its bubble and display the cinema name and address.

Nominatim also allows to search POIs along a route.
So, using the road you built in [Tutorial\_1](Tutorial_1.md), you can also get your POIs this way:
```
ArrayList<POI> pois = poiProvider.getPOIAlong(road.getRouteLow(), "fuel", 50, 2.0);
```

Note the call to road.getRouteLow(), which provides a reduced version of the road path. This is better for everybody: for URL length, for workload on Nominatim servers, and for responsiveness.

# 6. Wikipedia POIs with GeoNames #

There is a POI Provider based on GeoNames service, which is able to get Wikipedia entries close to a position, or inside a bounding box.

This is cool, but to use it you MUST 1) create a GeoNames account, and 2) activate the free services on it.
You can do that from here: http://www.geonames.org/login

Once your account is activated, just change the poiProvider calls this way:
```
GeoNamesPOIProvider poiProvider = new GeoNamesPOIProvider("YOUR_GEONAMES_ACCOUNT");
ArrayList<POI> pois = poiProvider.getPOIInside(map.getBoundingBox(), 30);
```

And you can test!


# 7. Customizing the bubble behaviour #
Wikipedia POIs usually contain a URL to the Wikipedia page.
It would be nice to give access to this page from the POI bubble.

We will do that by customizing the bubble, adding a "more info" button which will open a web view on the Wikipedia URL.

Ready? Let's go!

First, create a new class CustomInfoWindow which inherits from MarkerInfoWindow - the default InfoWindow for Markers.
```
public class CustomInfoWindow extends MarkerInfoWindow {
	public CustomInfoWindow(MapView mapView) {
		super(R.layout.bonuspack_bubble, mapView);
	}
}
```

Then, the "more info" button. In fact, our default bubble layouts already have a hidden "more info" button. So we just have to get it.
In the constructor:
```
Button btn = (Button)(mView.findViewById(R.id.bubble_moreinfo));
```

Of course, you are free to define your own layout, and put as many buttons you want, with the look&feel you want. We will see how to do that in chapter 9.

For now, set a listener doing something on this button:
```
btn.setOnClickListener(new View.OnClickListener() {
	public void onClick(View view) {
		Toast.makeText(view.getContext(), "Button clicked", Toast.LENGTH_LONG).show();
	}
});
```

If needed, you can override the onOpen and onClose methods.
onOpen is called when tapping a Marker, just before showing the bubble.
In our case, as our "more info" button is hidden in our layout, we have to show it:
```
@Override public void onOpen(Object item){
	super.onOpen(item);
	mView.findViewById(R.id.bubble_moreinfo).setVisibility(View.VISIBLE);
}
```

The CustomInfoWindow class is ready, we can set it in each POI Marker:
```
poiMarker.setInfoWindow(new CustomInfoWindow(map));
```

OK, ready, you can test it.

The next step is to open an activity on the POI, instead of this useless Toast.
For that, we would like to access POI information in the CustomInfoWindow.
There is a simple solution: each time you create a POI Marker, give it a reference to the corresponding POI object:
```
poiMarker.setRelatedObject(poi);
```

Define "POI mSelectedPoi" as a member of your CustomInfoWindow.
Then, in CustomInfoWindow.onOpen(item), you can access to the reference and update the selectedPoi:
```
Marker marker = (Marker)item;
mSelectedPoi = (POI)marker.getRelatedObject();
```

mSelectedPoi.mUrl contains the Wikipedia URL.
Now, you can modify the button listener to open a web view on this URL.
For instance this way:
```
btn.setOnClickListener(new View.OnClickListener() {
	public void onClick(View view) {
		if (mSelectedPoi.mUrl != null){
			Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mSelectedPoi.mUrl));
			view.getContext().startActivity(myIntent);
		}
	}
});
```

# 8. Quick overview of the Flickr and Picasa POIs #
There is a POI provider for retrieving geolocalized Flickr photos, and another one for Picasa photos.
The API are very similar to GeoNamesPOIProvider.
Let's start with Flickr.
First, you will need to obtain your own API key from Flickr - this is easy.

Then just change the poiProvider calls this way:

```
FlickrPOIProvider poiProvider = new FlickrPOIProvider("YOUR_FLICKR_API_KEY");
BoundingBoxE6 bb = map.getBoundingBox();
ArrayList<POI> pois = poiProvider.getPOIInside(bb, 20);
```

And test!

You should have the markers, a "more info" button to go to the Flickr page, but no thumbnail... This is sad.
This is because we cannot load all thumbnails at once: this would take too long for the user. So the idea is to postpone thumbnail loading when the user opens the bubble.

In addition, we will do that in an async task, to avoid blocking the user interface. Luckily, POI objects have a method to retrieve their thumbnail image in an async task.

Open again your CustomInfoWindow.onOpen method.

We first have to retrieve the target image view in which the image will be displayed:
```
ImageView imageView = (ImageView)mView.findViewById(R.id.bubble_image);
```

And then we call the async image fetching:
```
mSelectedPoi.fetchThumbnailOnThread(imageView);
```

Much better, isn't it?

# 9. Creating your own bubble layout #
Two standard layouts are provided and can be found in OSMNavigator/res/layout directory:
  * bonuspack\_bubble.xml, which is the default layout
  * bonuspack\_bubble\_black.xml

You can build and use your own bubble layouts, but some components are mandatory if you want to use them with a MarkerInfoWindow:
  * a TextView with id: bubble\_title
  * a TextView with id: bubble\_description
  * a TextView with id: bubble\_subdescription
  * an ImageView with id: bubble\_image
Take care to respect the exact spelling of these ids.
If you don't need these components, each one can be in status hidden or gone.

To discover other features, look at [Tutorial\_3](Tutorial_3.md).