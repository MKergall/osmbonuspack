

# Introduction #

OSMBonusPack implements a subset of KML features.

From an end-user point of view:
  * When using Google Maps Engine LITE (the Google Maps Web interface to create personal maps), anything you can create as KML will be supported.
  * But if you created your KML content with advanced tools like Google Earth or ArcGIS, find below the details about what is supported - and what is not.


If there is an unsupported feature that is key for you, don't hesitate to create an Issue in the Issues tab, explaining your need. We will see if we can add it to the roadmap.


# Details #

This explanation assumes you know KML concepts.
See the [KML Reference](https://developers.google.com/kml/documentation/kmlreference) if necessary.

## Supported object types ##

Supported KML object types are listed in the table below, with their related OSMBonusPack class:

| **KML object type** | **OSMBonusPack class** |
|:--------------------|:-----------------------|
| Document | KmlDocument |
| Feature | KmlFeature |
| Folder | KmlFolder |
| NetworkLink | KmlFolder |
| Placemark| KmlPlacemark |
| GroundOverlay| KmlGroundOverlay |
| Geometry| KmlGeometry |
| Point| KmlPoint |
| LineString | KmlLineString |
| Polygon | KmlPolygon |
| MultiGeometry | KmlMultiGeometry |
| Style | Style |
| ColorStyle | ColorStyle |
| PolyStyle| ColorStyle |
| LineStyle | LineStyle |
| IconStyle| IconStyle |
| StyleMap | StyleMap |


Unsupported objects are - normally - just ignored, without raising an error.

## Feature ##
For all supported **Feature**, supported attributes/elements are:
  * id
  * name
  * description - with no warranty about what happens if you put HTML, JavaScript or CSS inside.
  * visibility
  * open
  * styleUrl: references to external files are not handled.
  * ExtendedData: see restrictions below.

## Document ##
Document is loaded and then handled as a KmlFolder.
When saving, the root folder is saved as a Document.

## NetworkLink ##
NetworkLink is handled at loading, but converted to a regular KmlFolder. The content of the related file is completely integrated into the folder hierarchy: the link information is lost, styles of both files are merged.

Supported Link elements:
  * href: must be either a full HTTP address, or a relative path to a local file (ex: "subdir/subfile\_1.kml").

## Geometry ##
For all supported **Geometry**:
  * extrude, altitudeMode and tesselate (everything related to 3D display) are not supported. Altitude is kept as is.


## Polygon ##
  * innerBoundaryIs (holes) are supported from v4.2.6.

## GroundOverlay ##
Supported from v4.2.7.

  * Icon href: either a full HTTP address, or a relative path to a local file
  * LatLonBox is supported. Meridian overlapping is not supported.
  * gx:LatLonQuad is not supported.
  * color: only the alpha value is used, as the GroundOverlay transparency.
  * drawOrder is not supported. Draw order will be the order of elements in the KML file.
  * elements related to altitude are not supported.

## Style ##
Inline styles are supported at loading, and converted to shared styles.

## IconStyle ##
  * href: either a full HTTP address, or a relative path to a local file (ex: "images/img1.png").
  * heading is supported from 4.2.8
  * hotSpot is supported from 4.2.8. Only fraction units are supported. Hotspot is bottom-centered by default.

## PolyStyle ##
  * fill and outline are not supported, and considered as "true".

## LineStyle ##
None of "gx:" elements are supported.

## StyleMap ##
Supported from v4.6


## ExtendedData ##
  * **Data** are supported, with their name and value. displayName is not handled.
  * **SimpleData** are supported, but with no handling of SchemaData, and no control of the Schema: all values are handled as Java Strings. When saving, they will be saved as Data.
  * There is no support for ExtendedData using other namespaces.

## KMZ support ##
Supported from v4.4

Loading of local KMZ files is also supported.
The KMZ file must contain 1 KML file with ".kml" extension. This file will be the "main KML file". It can reference other files (icons, NetworkLinks) inside the KMZ, and external urls (with http/https).

References to local files external to this KMZ file (using the "../" prefix) is not supported.

Saving in KMZ format is not supported yet (note that you can easily create a KMZ file from your set of files, using a Zip utility).

# Limits #

There is no hard-coded limit in the lib, for instance on the number of folders, levels of folders, number of placemarks, number of coordinates, and so on.
So the only technical limit is the memory heap.

Unfortunately, on Android, the memory heap available to an application is ridiculously small: something between 20Mb and 60Mb, depending on the device - when it is usually above 1Gb on a PC.
And as OSMBonusPack handles KML entirely in memory, memory is the real limit.

In practice, you can reasonnably plan to manipulate KML files up to 1Mb (loading, displaying, editing, saving).
But with a 10Mb file, you will certainly encounter crashes due to OutOfMemory exceptions.

Hint: if you can target an SDK >= 11, then you can set android:largeHeap="true" in your manifest. Depending on the device, this may allow to get up to 256Mb of memory heap.