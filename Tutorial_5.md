# 16. Handling Map events #

It is quite usual to want to react to events done directly on the map - not on any particular marker or overlay.

Examples:
  * On a tap, close all open bubbles (usual Google Maps behaviour).
  * On a long press, put a marker at the position the user clicked.

You can do that using the duo MapEventsOverlay/MapEventsReceiver.

1) MapEventsOverlay is an overlay that will catch all events that are not catched by other usual overlays.

You give it the current context (this), and a MapEventsReceiver - let's just put "this" for now.

```
MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
```

Then you add it to your map. But you have to put it at the "bottom" of all overlays ("behind" other overlays), so that other overlays react normally to their events. You can add it this way:
```
map.getOverlays().add(0, mapEventsOverlay);
```

2) MapEventsReceiver is the object that will handle the map events. This is an interface, that you have to implement and pass to the MapEventsOverlay.
A smart solution is to simply add this interface to your Activity implementations:
```
public class MainActivity extends Activity implements MapEventsReceiver
```

Then you override the abstract methods somewhere in your Activity class. There are 2 methods.
One method is the handler of single tap confirmed events:
```
@Override public boolean singleTapConfirmedHelper(GeoPoint p) {
	Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
	return true;
}
```

The other one is the handler of long press events:
```
@Override public boolean longPressHelper(GeoPoint p) {
	//DO NOTHING FOR NOW:
	return false;
}
```

_Other events are not covered, as they already have a role in default osmdroid usage. If you have a specific need, you can ask by raising an issue._

As for other Android event handlers, if you want to "consume" the event, you return true, and if you don't, you return false.

For each method, the GeoPoint parameter is the position of the event on the map. A quick test:
```
Toast.makeText(this, "Tap on ("+p.getLatitude()+","+p.getLongitude()+")", Toast.LENGTH_SHORT).show();
```

Fine, now we can use that to close of all opened bubbles with a tap. Okay?
```
InfoWindow.closeAllInfoWindowsOn(map);
```

# 17. Using Polygon #
Use case: every time the user long-press on the map, we want to "drop" a circle centered on the long-press position.

We will use the Polygon overlay, which has a helper method to be defined as a circle.

In the longPressHelper method (seen in chapter 16), we create the Polygon, from the event position, and a radius of 2 km:
```
Polygon circle = new Polygon(this);
circle.setPoints(Polygon.pointsAsCircle(p, 2000.0));
```
And we adjust some design aspects:
```
circle.setFillColor(0x12121212);
circle.setStrokeColor(Color.RED);
circle.setStrokeWidth(2);
```

Of course, we have to add it to the map overlays:
```
map.getOverlays().add(circle);
```

And then? Yes, refresh the map!
```
map.invalidate();
```

And as Polygon supports bubbles, let's add one:
```
circle.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
circle.setTitle("Centered on "+p.getLatitude()+","+p.getLongitude());
```


# 18. Using GroundOverlay #

If you don't know what is a GroundOverlay, have a look at [Google Maps API](http://developer.android.com/reference/com/google/android/gms/maps/model/GroundOverlay.html). This is also similar to the [Leaflet ImageOverlay](http://leafletjs.com/reference.html#imageoverlay).

Use case: every time the user do a long press, we want to also "drop" a GroundOverlay at the event position.

We add it to the longPressHelper method used in chapter 17:

```
GroundOverlay myGroundOverlay = new GroundOverlay(this);
myGroundOverlay.setPosition(p);
myGroundOverlay.setImage(getResources().getDrawable(R.drawable.ic_launcher).mutate());
myGroundOverlay.setDimensions(2000.0f);
map.getOverlays().add(myGroundOverlay);
```

The result:

<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/tuto_polygon_groundoverlay.png' width='300'>

<h1>Conclusion</h1>
Well, "That's all Folks!"<br>
<br>
If you want to go further, have a look on OSMNavigator source code, and on the javadoc. You will discover some useful stuff like <b>GeocoderNominatim</b>, Caching features, and more...