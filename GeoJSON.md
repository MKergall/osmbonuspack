

# Introduction #
_GeoJSON support is available from OSMBonusPack v4.2.9._

In OSMBonusPack, support for GeoJSON is done through KML objects.

We assume the reader understand both the [GeoJSON concepts](http://geojson.org/geojson-spec.html) and the [KML concepts](https://developers.google.com/kml/documentation/kmlreference).

# Mapping between GeoJSON types and KML classes #

The table below shows the mapping in both ways between GeoJSON types and KML classes:

| **GeoJSON type** | **OSMBonusPack KML class** |
|:-----------------|:---------------------------|
| FeatureCollection | KmlFolder |
| Feature | KmlPlacemark |
| Point | KmlPoint |
| LineString | KmlLineString |
| Polygon | KmlPolygon |
| GeometryCollection | KmlMultiGeometry |

MultiPoint is supported at loading only, and converted to KmlMultiGeometry.

At loading, other GeoJSON types will be ignored.
At saving, other KML classes will be ignored.

GeoJSON spec doesn't support nesting of FeatureCollection.
So, at saving, if your KmlFolder contains recursively other KmlFolder, this hierarchy is flattened: the root KmlFolder is converted in a FeatureCollection, containing all KmlPlacemarks converted as Features.

Nested GeometryCollections are fully supported, as nested KmlMultiGeometry.

# GeoJSON properties, KML attributes and KML ExtendedData #

KML Placemark "id" attribute is handled (at loading and saving) as GeoJSON Feature "id".

KML Placemark "name" attribute is handled (at loading and saving) as the GeoJSON Feature property "name".

Exemple:
```
{ "type": "Feature",
  "id": "NYC",
  "geometry": {"type": "Point", "coordinates": [102.0, 0.5]},
  "properties": {"name": "New-York City"}
}
```

All other GeoJSON properties are loaded as ExtendedData (assuming their key and value can be loaded as String).

All KML Placemark ExtendedData are saved as GeoJSON Feature properties.

KML ExtendedData of other classes are not saved in GeoJSON.

And other KML attributes, like description or style information, are not saved in GeoJSON.

# Coordinates reference system #
Only WGS84 is supported. All longitude and latitude values are assumed to be in decimal degrees.

# Limits #
As for KML, the only technical limits are about memory and time.

For memory, you will find the same limits than [KML](KML.md), related to memory heap on Android.

For time: performance for GeoJSON loading should be significantly slower than for its KML equivalent. There are 2 reasons:
  * The GSON library which is used - even if better than the standard JSON lib included in the Android SDK - is not as efficient as the standard XML SAX parser.
  * Cumulated with the coordinates format of GeoJSON: each position is stored as a JSON array, implying a heavy usage of JSON parsing. In KML, the coordinates of a whole geometry are in a single XML element with a simple formatting, allowing an optimized loading strategy.