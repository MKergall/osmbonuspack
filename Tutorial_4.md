(we assume you already followed [Tutorial\_1](Tutorial_1.md) and [Tutorial\_2](Tutorial_2.md)])

# 12. Loading KML content #

[KML](http://en.wikipedia.org/wiki/Keyhole_Markup_Language) is a de-facto standard for handling overlays on maps, from a simple list of markers, to complex structures.

Both Google Maps and Bing Maps allow people to create their "personal maps" by putting markers, lines and polygons. Such personal maps can be exported as KML content.
More advanced tools can be used to create and display KML content, like Google Earth or ArcGIS Explorer.

OSMBonusPack provides the basic toolbox to handle KML content: read (from a url or a local file), save locally, display, and edit.

Let see it in action. I have a Url to a KML file, and I want to show it on my OSM map.

(Parenthesis - If you don't have such Url, pick one of those:
  * Paris Tour: http://mapsengine.google.com/map/kml?mid=z6IJfj90QEd4.kUUY9FoHFRdE
  * Chicago Transit map (a sample from Google Maps JavaScript API v3 doc): http://gmaps-samples.googlecode.com/svn/trunk/ggeoxml/cta.kml
  * a message in the desert: http://mapsengine.google.com/map/kml?mid=z6IJfj90QEd4.kcfEKhi8r5LQ
  * Or a guided visit of the "Puy du Fou Grand Parc" - a famous Theme Park in France: http://mapsengine.google.com/map/kml?mid=z6IJfj90QEd4.kABxpiwxshvA
End of parenthesis)

First of all, you need a KmlDocument:
```
KmlDocument kmlDocument = new KmlDocument();
```

Then you can use it to read and parse your KML content:
```
kmlDocument.parseUrl(url);
```

This KmlDocument now contains the whole KML Document.
In particular, it contains a KmlFolder: mKmlRoot, which is the entry point to the KML hierarchy.
Don't hesitate to have a look at [KML](KML.md) Wiki page: you will need to understand the KML classes if you want to go further and manipulate your KML structure.

But for now, we mainly wish to see the graphics on the screen.

So we build the overlays:
```
FolderOverlay kmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(this, map, null, null, kmlDocument);
```

Yes, this is doing the job. This FolderOverlay will be the container for all overlays that will be found in the KML structure:
  * KML Point, handled as an OSMBonusPack Marker
  * KML LineString, handled as OSMBonusPack Polyline
  * KML Polygon, handled as OSMBonusPack Polygon
  * and KML Folder, handled as OSMBonusPack FolderOverlay, allowing to keep the hierarchical organisation of the KML structure.

Now, add this folder overlay on the map:
```
map.getOverlays().add(kmlOverlay);
```

And don't forget to redraw your map view:
```
map.invalidate();
```

Now give this a try.

You see nothing? Try to move your map at the right place. Or set automatically the view on the whole stuff, using its bounding box:
```
map.zoomToBoundingBox(kmlDocument.mKmlRoot.getBoundingBox());
```

The result when loading "Paris Tour" URL:

<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/tuto_kml.png'>

<h1>13. Styling overlays</h1>
In chapter 12, you used KML samples including styles, and the overlays were reflecting these styles.<br>
This is not always the case. You may have content without style. Or you may want to get rid of defined styles and apply your own strategy.<br>
<br>
Example: YOURS service produces routes in KML format.<br>
Here is a<br>
<a href='http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&flat=52.215676&flon=5.963946&tlat=52.2573&tlon=6.1799'>http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&amp;flat=52.215676&amp;flon=5.963946&amp;tlat=52.2573&amp;tlon=6.1799</a>

This KML contains no style at all, and all "visible" elements are set to false.<br>
<br>
So, what can we do?<br>
<br>
<h2>13.1 Default style</h2>
First level of design customization is to specify a default style.<br>
<br>
You can use the simplified Style constructor where you just specify<br>
<ul><li>a default Marker icon,<br>
</li><li>a default line color and line width,<br>
</li><li>and a default fill color</li></ul>

<pre><code>Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_kml_point);<br>
Bitmap defaultBitmap = ((BitmapDrawable)defaultMarker).getBitmap();<br>
Style defaultStyle = new Style(defaultBitmap, 0x901010AA, 3.0f, 0x20AA1010);<br>
</code></pre>

Then you specify this default style when building the overlays:<br>
<pre><code>FolderOverlay kmlOverlay = (FolderOverlay)mKmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, null, mKmlDocument);<br>
</code></pre>

Everytime a Placemark has no style defined, the default style will be used to build its Overlay.<br>
<br>
<br>
<h2>13.2 Advanced styling with the Styler</h2>
<i>Available from OSMBonusPack v4.3</i>

If you need more advanced mechanisms, then you can specify a "Styler".<br>
This is an interface providing methods to be applied to each kind of KML Feature or Geometry during the Overlays building.<br>
<br>
First step is to implement your Styler:<br>
<pre><code>class MyKmlStyler implements KmlFeature.Styler {<br>
}<br>
</code></pre>

Then you write the methods.<br>
Here is a LineString styler setting the width of the line according to its number of points:<br>
<pre><code>@Override public void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString){<br>
  polyline.setWidth(Math.max(kmlLineString.mCoordinates.size()/200.0f, 3.0f));<br>
  polyline.setColor(Color.GREEN);<br>
}<br>
</code></pre>

If you have nothing to do, do nothing:<br>
<pre><code>@Override public void onFeature(Overlay overlay, KmlFeature kmlFeature){}<br>
</code></pre>

Now you can use this styler when building overlays, this way:<br>
<br>
<pre><code>KmlFeature.Styler styler = new MyKmlStyler();<br>
FolderOverlay kmlOverlay = (FolderOverlay)mKmlDocument.mKmlRoot.buildOverlay(map, null, styler, mKmlDocument);<br>
</code></pre>

It's important to understand the styling strategy:<br>
<ol><li>Is there is a styler, it is called, and NO other styling is applied.<br>
</li><li>Else, if there is a style defined in the KML Feature, this style is applied.<br>
</li><li>Else, if there is a default style specified, it is applied.<br>
</li><li>Else, hard-coded default styling (quite ugly) is applied.</li></ol>

It may happen that you would like to both use a Styler, AND still have access to standard styling. In this case, in the Styler methods, you can still call #applyDefaultStyling, which is available in most KML classes.<br>
<br>
In our example, if we have no particular style specificities for Points, we can define onPoint this way:<br>
<pre><code>@Override public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {<br>
  kmlPoint.applyDefaultStyling(marker, mDefaultStyle, kmlPlacemark, mKmlDocument, map);<br>
}<br>
</code></pre>
As you will notice, you will have to set some objects as class members (the default style, the KmlDocument).<br>
<br>
<h2>13.3 Setting-up KML Styles programmatically</h2>
Note that you can also create Styles programmatically inside the KML structure:<br>
<pre><code>Style panda_area = new Style(pandaBitmap, 0x901010AA, 3.0f, 0x2010AA10);<br>
mKmlDocument.putStyle("panda_area", panda_area);<br>
</code></pre>

And you can assign a Style to KML features. For instance this way:<br>
<pre><code>@Override public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {<br>
  if ("panda_area".equals(kmlPlacemark.getExtendedData("category")))<br>
    kmlPlacemark.mStyle = "panda_area";<br>
  kmlPoint.applyDefaultStyling(marker, mDefaultStyle, kmlPlacemark, mKmlDocument, map);<br>
}<br>
</code></pre>

<h1>14. Grab overlays in KML structure, save KML locally</h1>
You can "grab" an overlay and add it in an existing KML folder.<br>
For example, we can grab the route shape overlay we built in chapter 1:<br>
<pre><code>kmlDocument.mKmlRoot.addOverlay(roadOverlay, kmlDocument);<br>
</code></pre>

This is working for Polyline overlays, but also for Markers, FolderOverlays and MarkerClusterers. If you put your road node markers in a roadNodes FolderOverlay (as we did for POI Markers in chapter 5), you can grab all of them once:<br>
<pre><code>kmlDocument.mKmlRoot.addOverlay(roadNodes, kmlDocument);<br>
</code></pre>

And we can save the final result locally, in a KML file:<br>
<pre><code>File localFile = kmlDocument.getDefaultPathForAndroid("my_route.kml");<br>
kmlDocument.saveAsKML(localFile);<br>
</code></pre>

The default path for KML files is in the external storage, in a "kml" directory.<br>
Now, you have a "my_route.kml" file, containing the whole KML content loaded from a url in chapter 12, plus the route shape and route nodes of chapter 1.<br>
<br>
<h1>15. Loading and saving of GeoJSON content</h1>

<i>Available from OSMBonusPack v4.2.9</i>

<a href='http://en.wikipedia.org/wiki/GeoJSON'>GeoJSON</a> is more or less the JSON equivalent to KML.<br>
Very easy to use in a HTML/JavaScript context, it rapidly reached a large audience.<br>
<br>
All KML objects support loading of GeoJSON content, and saving locally in GeoJSON format.<br>
<br>
Let save in GeoJSON the KML structure built in chapters 12 and 13:<br>
<pre><code>File localFile = kmlDocument.getDefaultPathForAndroid("my_route.json");<br>
kmlDocument.saveAsGeoJSON(localFile);<br>
</code></pre>

You can refer to <a href='GeoJSON.md'>GeoJSON</a> Wiki page for more details.<br>
<br>
Note that GeoJSON format doesn't provide styling attributes. So the chapter 13 above about styling will be very useful if you want to display GeoJSON content.<br>
<br>
<h1>Next step</h1>
See you soon on <a href='Tutorial_5.md'>Tutorial_5</a>, to discover Map events handling, Polygons and GroundOverlays.