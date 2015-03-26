Note: sources of these tutorials can be found in the [OSMBonusPackTuto](http://code.google.com/p/osmbonuspack/source/browse/#svn%2Ftrunk%2FOSMBonusPackTuto) project. But of course and as usual, we strongly recommend that you follow the tutorials, and build the code yourself.


# Important note about network calls #
For clarity and simplicity, in these tutorials, we do all API calls in the main thread.

Normally, for network calls, this is not recommended at all: we should use threads and asynchronous tasks.
Even worst, since Honeycomb SDK (3.0), it is not allowed to make a network call in the main thread (thanks to the "StrictMode.ThreadPolicy" default settings, a NetworkOnMainThreadException exception will be raised).

So:
  1. For these tutorials, target an SDK earlier than 3.0
  1. Once you have played with the tutos and want to work on your real app, have a look at the OSMNavigator source code, where network calls are done in async tasks.


# Introduction #
To set-up your project, see [HowToInclude](HowToInclude.md) wiki page.

Then, let's start with a super-simple Android application using osmdroid, and displaying a map in an activity. Typically, that:

```
public class MainActivity extends Activity {

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
	map.setBuiltInZoomControls(true);
	map.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(48.13, -1.63);
        IMapController mapController = map.getController();
	mapController.setZoom(9);
        mapController.setCenter(startPoint);
    }
}
```

And its "main.xml" layout, with the map:
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools"
        android:orientation="vertical" 
        android:layout_width="fill_parent"
        android:layout_height="fill_parent">
        
        <org.osmdroid.views.MapView android:id="@+id/map"
                android:layout_width="fill_parent" 
                android:layout_height="fill_parent" />
                
</LinearLayout>
```

# 0. Using the Marker overlay #
As we will make a large usage of OSMBonusPack Marker in those tutorials, let's start with a quick presentation.

First of all, you MUST put in your project res directory all resources needed for the default InfoWindow or "bubble":
  * in your res/layout: bonuspack\_bubble.xml
  * in res/drawable-mpi:
    * bonuspack\_bubble.9.png
    * moreinfo\_arrow.png
    * moreinfo\_arrow\_pressed.png
  * in res/drawable: btn\_moreinfo.xml
You will find all those resources in [OSMNavigator res directory](http://code.google.com/p/osmbonuspack/source/browse/#svn%2Ftrunk%2FOSMNavigator%2Fres). Just copy/paste them in your own project.

Now, back to the code. We create a Marker, we screw it at the start point, we set its "anchor" at bottom-center, and we add this Marker to the map's overlays:
```
Marker startMarker = new Marker(map);
startMarker.setPosition(startPoint);
startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
map.getOverlays().add(startMarker);
```

You will certainly notice that 2 Marker classes are available: one in osmdroid, and one in OSMBonusPack. Of course, you need to import the OSMBonusPack one:
```
import org.osmdroid.bonuspack.overlays.Marker;
```

And we refresh the map. Yes, we will not repeat it everytime, but in order to see your changes, you need to refresh the map - this way:
```
map.invalidate();
```

Give it a try. You should see the default osmdroid marker icon. Clicking on it opens an empty cartoon-bubble.

Now we can improve a little bit, changing the icon, and setting a title to be shown in the bubble:
```
startMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
startMarker.setTitle("Start point");
```

- And then?

- Refresh the map!

- Okay, you got it. Now, let's start with really new stuff: routes and directions, in [Tutorial\_1](Tutorial_1.md)