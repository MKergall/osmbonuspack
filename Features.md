

# Introduction #
osmdroid is an opensource library which provides a simple and efficient way to port an Android application from Google Maps to OpenStreetMap.

osmdroid provides:
  * the map tiles (MapView) and the usual zoom/pan tools (MapController)
  * the standard overlays (ItemizedOverlay, MyLocationOverlay,...)
  * and some additional overlays (MinimapOverlay, PathOverlay,...)

However, nowadays, for modern map-based applications, we want more!
So, here comes OSMBonusPack.


# Routes and Directions #
Probably the most desired feature.
OSMBonusPack provides a set of classes to get routes, with turn-by-turn information and route shape:

[RoadManagers](Tutorial_1.md) retrieve roads from a variety of service providers. Currently supported:
  * **OSRMRoadManager**, using [OSRM](http://project-osrm.org/) service
  * **GraphHopperRoadManager**, using [GraphHopper](http://graphhopper.com/) online service
  * **MapQuestRoadManager** using [MapQuest Open Guidance](http://open.mapquestapi.com/guidance/) service

In all cases, the result is a **Road** object, containing:
  * the road shape, as a list of GeoPoint => RoadManager can build a Polyline overlay from this road shape, ready-to-display.
  * turn-by-turn information, as a list of RoadNode.

[WhichRoutingService](WhichRoutingService.md) page may help you in choosing the routing service corresponding to your needs.

Also provided, very useful route-related utilities:
  * **PolylineEncoder**, a polyline encoder/decoder
  * **DouglasPeuckerReducer**, a polyline reducer based on Douglas-Peucker algorithm.


# Markers with cartoon-bubble #
The [Marker](Tutorial_0.md) overlay mimics as much as possible its Google Maps V2 equivalent.

It supports the now classical "cartoon-bubble" or **InfoWindow**.
Two standard bubble layouts are provided in the demo application, each one with its NinePatch background. You can use your own, and contributors are strongly encouraged to share some nice alternatives...

This **Marker** also supports drag events, transparency, rotation, and so on.

# Polygon, Polyline, GroundOverlay, FolderOverlay #

The [Polygon](Tutorial_5.md) and Polyline overlays mimic as much as possible their Google Maps V2 equivalents.
Both of them also support touch detection and cartoon-bubbles (something not available in Google Maps V2).

The [GroundOverlay](Tutorial_5.md) also mimic its Google Maps V2 equivalents.

The **FolderOverlay** has no Google Maps counterpart. It is a container for other overlays. It can contain any Overlay object - including other FolderOverlays.
It is perfect for managing easily logical groups of overlays - for instance a list of Markers - without adding dedicated lists or HashMaps in your code, as usually needed when using Google Maps V2 API.

# Marker Clustering #
Marker Clustering is the now classical solution to the "Too Many Markers Problem".

[GridMarkerClusterer](Tutorial_3.md) displays all Markers which are close together as a single icon - adapting the clustering to the zoom level.

It has been designed to be as simple to use as possible. Basically, you just have to create a GridMarkerClusterer, add it to your map overlays, and put your Markers inside.

You can easily customize its look&feel (cluster icon, cluster size, text style and positionning,...).


# Points Of Interest - POIs #
[POIProviders](Tutorial_2.md) retrieve POI from a variety of service providers.
Currently supported:
  * **NominatimPOIProvider**, to retrieve OpenStreetMap "features"
  * [OverpassAPIProvider](OverpassAPIProvider.md), an other way to retrieve OpenSteetMap "features"
  * **GeoNamesPOIProvider**, using [GeoNames Wikipedia](http://www.geonames.org/export/wikipedia-webservice.html) service, retrieving Wikipedia geolocalized entries
  * **FlickrPOIProvider** and **PicasaPOIProvider**, using Flickr and Picasa services to search among the billions of Flickr and Picasa photos and retrieve geolocalized photos for an area.

Result is a list of **POI** objects containing the position of the POI, and POI information.

# Geocoding and Reverse Geocoding #
Android provides the [Geocoder](http://developer.android.com/reference/android/location/Geocoder.html) class. BUT... according to Google Maps APIs Terms of Service, it can only be used in relation with a Google map.
(see https://developers.google.com/maps/terms, item (g): No Use of Content without a Google Map)

If you are using osmdroid, you are probably not displaying a Google map.

So, OSMBonusPack provides **GeocoderNominatim** class, which accesses to a Nominatim service:
  * either the standard OpenStreetMap [Nominatim](http://wiki.openstreetmap.org/wiki/Nominatim) service
  * or the Nominatim service hosted by [MapQuest](http://developer.mapquest.com/web/products/open/nominatim/)
  * or any other Nominatim-compliant service (your own installation for example).
Migrating from Android Geocoder is a simple change of class name.

WARNING: The relevance of Nominatim service is far behind Google Geocoding service. Results are sometimes quite surprising, and there is few support for misspelling (e.g. searching for a "Mozar street", you will not find any "Mozart street"). Take this aspect into account before providing a real-life service.

# Map events handling #
[MapEventsOverlay and MapEventsReceiver](Tutorial_5.md) classes provide a simple way to handle single-press and long-press events somewhere on the map.

Example: long-press on the map to define the destination, a new POI,...


# KML and GeoJSON support #
[KmlDocument and KmlFeature](Tutorial_4.md) are the major building blocks to load KML/KMZ and GeoJSON content, to build corresponding overlays, to convert overlays as KML content, and to save KML and GeoJSON documents locally.

Refer to the [KML wiki page](KML.md) and [GeoJSON wiki page](GeoJSON.md) for more details.

You can also look at [OverpassAPIProvider](OverpassAPIProvider.md), a powerful tool to get OSM data as KML content.

# Integrated tools for off-line maps and Cache Management #
CacheManager allows to download tiles on areas, for later off-line usage. This feature can be completely integrated in Android applications (as shown in OSMNavigator).
For end-users, this means:
  * no need to find, install and learn external tools
  * no need to transfer map files from a PC to the device
  * no issue about map file formats

CacheManager also provides various utilities for managing the osmdroid cache: usage, capacity, cleaning.

# Next steps #
  * See HowToInclude the lib in your project
  * Follow the [Tutorials](Tutorial_0.md)
  * Look at (or checkout) [OSMNavigator source](https://code.google.com/p/osmbonuspack/source/browse/#svn%2Ftrunk%2FOSMNavigator) to see how to use these classes in common use cases
  * Download the javadoc to get detailed information on the API.