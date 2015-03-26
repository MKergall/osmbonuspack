(we assume you already followed [Tutorial\_1](Tutorial_1.md) and [Tutorial\_2](Tutorial_2.md))

# 10. Marker Clustering #

Marker Clustering is a now classical technique when you have a lot of markers on a map, to group markers which are close together in a single "cluster" marker, displaying the number of markers it "contains".

This (cool) feature is available in OSMBonusPack with the RadiusMarkerClusterer.

If you already grouped your markers in a FolderOverlay (as we did in chapter 5 with poiMarkers), introducing marker clustering is trivial.
You replace the FolderOverlay by a RadiusMarkerClusterer:
```
RadiusMarkerClusterer poiMarkers = new RadiusMarkerClusterer(this);
```

You also have to set an icon for the clusters. This is the icon to be used when there are many markers in a cluster.
As this icon must be a Bitmap, here is the usual way to get a Bitmap from a Drawable resource:
```
Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
Bitmap clusterIcon = ((BitmapDrawable)clusterIconD).getBitmap();
```

Then you can set this icon to clusters:
```
poiMarkers.setIcon(clusterIcon);
```

The rest of the code: adding the poiMarkers to map overlays, and adding markers to poiMarkers, has already been done in chapter 5, and need no change.

So you can give it a try.

The result when searching for "bakery" with NominatimPOIProvider:

<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/tuto_clustering.png' height='530'>

If you zoom in, each individual marker appears. When you zoom out, they are grouped and displayed with the cluster icon. Fine, isn't it?<br>
<br>
<h1>11. Customizing the clusters design - and beyond</h1>

<h2>11.1 Simple customization</h2>
To customize the design of clusters, the first level is to use the available getters, setters and public attributes.<br>
<br>
Example:<br>
<pre><code>poiMarkers.getTextPaint().setColor(Color.DKGRAY);<br>
poiMarkers.getTextPaint().setTextSize(12.0f);<br>
poiMarkers.mAnchorU = Marker.ANCHOR_RIGHT;<br>
poiMarkers.mAnchorV = Marker.ANCHOR_BOTTOM;<br>
poiMarkers.mTextAnchorV = 0.40f;<br>
</code></pre>

<h2>11.2 Advanced customization</h2>
But you may want to go deeper. There is a lot of excellent ideas for cluster design:<br>
<ul><li>circles with various colors or sizes,<br>
</li><li>set of photos,<br>
</li><li>...</li></ul>

You can implement such designs by sub-classing the RadiusMarkerClusterer.<br>
Then you override #buildClusterMarker(StaticCluster cluster, MapView mapView) method, and you draw your cluster icon as needed.<br>
<br>
To have a good starting point, you can look<br>
<ul><li>at RadiusMarkerClusterer#buildClusterMarker source code<br>
</li><li>or at OSMBonusPackTuto CirclesGridMarkerClusterer, which implements circles with various colors and sizes.</li></ul>

If you want to go further, you can also implement your own clustering algorithm. You "just" have to sub-class the MarkerClusterer, and override the #clusterer(MapView mapView) method.<br>
<br>
<br>
<h1>Next step</h1>
In <a href='Tutorial_4.md'>Tutorial_4</a>, you will learn how to use the KML and GeoJSON toolbox.