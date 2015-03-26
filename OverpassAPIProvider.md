

# Introduction #
[Overpass API](http://wiki.openstreetmap.org/wiki/Overpass_API) is a super-powerful search API directly accessing to OpenSteetMap data.

OSMBonusPack OverpassAPIProvider gives you access to this API. It comes with 2 flavors: either using the simple "POI" approach, or using the KML flavor to get full geometry and tags.

OverpassAPIProvider is available from OSMBonusPack v5.0.

# POI approach #

OverpassAPIProvider#urlForPOISearch is a helper method to build a request for a simple tag search. Have a look at [OSM Map Features](http://wiki.openstreetmap.org/wiki/Map_Features) to discover usual OSM tags (cinemas, fuel stations, speed cameras, shops,...).

OverpassAPIProvider#getPOIsFromUrl performs the query, and convert the features found as POI objects.

For each feature, only 1 position is retrieved, not the whole geometry. This approach is simple (homogeneous), and quite efficient (small amount of data).

You can safely get up to 500 features in one request.

Example when querying "amenity=cinema":

<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/osmbonuspackdemo_4_2.png' height='530'>

<h1>KML flavor</h1>
It combines the power of OverpassAPI query language, with the power of KML  representation.<br>
<br>
OverpassAPIProvider#urlForTagSearchKml(String tag, BoundingBoxE6 bb, int limit, int timeout) is a helper method to build a request for a simple tag search.<br>
<br>
OverpassAPIProvider#addInKmlFolder(KmlFolder kmlFolder, String url) performs the query, and convert the result to KML content, that is added in the kmlFolder.<br>
Each OSM element becomes a KML Placemark, with a KML Geometry depending on the OSM type of the element:<br>
<ul><li>OSM node becomes a KML Point<br>
</li><li>OSM open way becomes a KML LineString<br>
</li><li>OSM closed way becomes a KML Polygon<br>
</li><li>OSM relation becomes a KML MultiGeometry</li></ul>

For each element:<br>
<ul><li>The OSM id is copied as the KML Placemark id.<br>
</li><li>All OSM tags (key=value) are copied as KML Extended Data.<br>
</li><li>The "name" tag is copied as the KML Placemark name.</li></ul>


Some use cases:<br>
<ul><li>Get cinemas => urlForTagSearchKml("amenity=cinema", bb, limit, timeout);<br>
</li><li>Get all shops => urlForTagSearchKml("shop", bb, limit, timeout);<br>
</li><li>Get buildings with their shape => urlForTagSearchKml("building", bb, limit, timeout);<br>
</li><li>Get all features having "website" tag => urlForTagSearchKml("website", bb, limit, timeout);</li></ul>

Example when querying "building":<br>
<br>
<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/osmbonuspackdemo_13.png' height='530'>

For more complex searches, have a look at #urlForTagSearchKml, and then you can build your own Overpass API request. You should be familiar with the <a href='http://wiki.openstreetmap.org/wiki/Overpass_API/Overpass_QL'>OverpassQL</a> query syntax.<br>
<br>
To use #addInKmlFolder, your url request must ensure that:<br>
<ul><li>Content is in JSON format => obtained with the "[out:json]" setting<br>
</li><li>ways and relations have the "geometry" element => obtained with the "out geom" setting.