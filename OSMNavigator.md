

# Introduction #

[OSMNavigator](http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/OSMNavigator_v5.1.apk.zip) is a generic map application for Android, based on OSMBonusPack library. It is simple and compact.

It can be downloaded in the [Downloads](http://osmbonuspack.googlecode.com/svn/BonusPackDownloads) section of this site.
We have no plan to make it available through Google Play.

# Features #

## Display an OpenStreetMap map ##
In the option menu "Tile Provider" item, you can choose the tile source between:
  * OSM Standard map,
  * OSM by MapQuest,
  * MapBox Satellite (a wonderful satellite map),
  * MapsForge offline map: see "Working off-line" below.

## GPS location ##
When the GPS is on, a green arrow shows the current location.
When moving, this arrow shows the direction of the move.
With the button in the lower-left corner, you can switch between 2 modes:
  * tracking/driving mode: the map stays centered on the current location, and oriented to the direction of the move
  * static mode: the map remains oriented to the north

## Departure and destination ##
Using the top panel, you can search for a departure address ("My Position" text field) and for a Destination address.

Note that the top panel can be hidden/shown by just tapping on its horizontal separator.

You can also set the departure/destination location by a long-press on the map at the desired position.

Once the departure/destination is set, a marker is displayed.
After a long-press on this marker, you can drag it at another location.


## Routing ##

When a destination is defined, the route is automatically computed.

On the "Road Provider" option in the option menu, you will find the following options:
  * OSRM-Fastest: for car,
  * GraphHopper-Fastest: for car,
  * GraphHopper for bicycle
  * and GraphHopper for pedestrian.

To add a "via-point" (an intermediate point on the route), long-press on the map at the desired position, and choose "Via-point" item.

To remove a point (the departure, the destination, or any via-point), tap on its marker, then in the bubble, tap on the "Delete" icon on the right.

In the option menu, use "Itinerary" item to display the route directions as a list. Tap on a direction to open it on the map.

## Searching for Features ##
In the top panel, use the "Features" text field to search for various map features (fuel, cinema, supermarket, shoe shop, etc, etc...). If you just enter some letters, suggestions will be proposed.

If there is no route defined, features will be searched in the "view", the area of the map currently displayed. If you move the view (by panning/zooming), there is no automatic refresh: tap "Search" again everytime you want to search.

Warning: the search returns a maximum of 100 results. Narrow your view if needed.

<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/osmbonuspackdemo_4_2.png' height='530'><br>

In the options menu, you can use "Features list" item to display all the features as a list.<br>
<br>
<br>
<h2>Searching for Wikipedia entries</h2>
In the Features text field, you can type "wikipedia". This will search for geolocalized Wikipedia entries.<br>
<br>
<br>
<h2>Searching for Pictures</h2>
In the Features text field, you can type "picasa" or "flickr". This will search for geolocalized pictures from these services.<br>
<br>
<h2>KML Toolbox</h2>
In the options menu, the "KML Toolbox" item is the entry point for handling KML layer.<br>
<br>
"Open url..." opens a KML file from its url (KMZ and GeoJSON content are not supported).<br>
<br>
"Open file..." opens a file. The type is defined from the file name extension: .kml for KML, .kmz for KMZ, .json for GeoJSON.<br>
<br>
Files are loaded from the SD card directory, from the "kml" sub-directory. Using the Android file manager, it is also possible to open a file located anywhere on the device, as long as its extension is .kml, .kmz or .json.<br>
<br>
With "OverpassAPI Wizard", you can directly query OpenStreetMap underlying data using OverpassAPI service, and get the resulting OSM features (nodes, ways and relations) displayed as KML features.<br>
The query should be an <a href='http://wiki.openstreetmap.org/wiki/Tags'>OSM tag</a>, in the form "key=value", or just "key".<br>
<br>
Some examples: "shop=bakery", "amenity=cinema", or "building".<br>
<br>
<br>
With "Pick Overlays", all overlays currently displayed are copied as KML features.<br>
<br>
"Edit Features..." opens a simple KML editor, where you can:<br>
<ul><li>Edit the name and description of a Feature<br>
</li><li>Set its visibility<br>
</li><li>Set its style, by choosing one of the Shared Styles of the KML document (see below)<br>
</li><li>For a Folder:<br>
<ul><li>list all its items<br>
</li><li>open an item: tap on it<br>
</li><li>copy/cut an item: long-press on it to open the menu<br>
</li><li>paste item from clipboard: using the option menu<br>
</li><li>add a new folder: using the option menu<br>
</li><li>center the map on the selected item</li></ul></li></ul>

"Edit Shared Styles" lists all Shared Styles of the current KML document. You can add a new style (option menu), delete a style (long-press), and edit a style (tap). When editing a style, you can:<br>
<ul><li>change its "style id"<br>
</li><li>define its icon. 2 methods:<br>
<ul><li>It can be the full url.<br>
</li><li>If the KML document was opened from a local file, it can be the path relative to this local file (e.g. "img/icon.png").<br>
</li></ul></li><li>define the fill & outline colors<br>
</li><li>define the outline width</li></ul>

"Save as..." saves the current KML layer, either as a KML or GeoJSON file, depending on the extension (.kml or .json). Files are saved on the SD card directory, in the "kml" sub-directory.<br>
<br>
"Clear" empties the current KML layer.<br>
<br>
With a long-press directly at the desired location on the map, you can add a KML Point.<br>
Long-press on a KML Point marker to drag it at another location.<br>
<br>
<h2>Working off-line</h2>

The options to display a map when offline are described below.<br>
Note that when offline, you will have no search and no routing.<br>
<br>
<h3>Using MOBAC</h3>
You can create maps using <a href='http://mobac.sourceforge.net'>MOBAC</a>. Most MOBAC formats mentionning "osmdroid" should be supported. Use Osmdroid ZIP format if you don't care.<br>
You can refer to this <a href='http://stackoverflow.com/questions/22862534/download-maps-for-osmdroid/22868462#22868462'>StackOverflow answer</a> for more details.<br>
<br>
<br>
Copy the resulting file on your device, in your SD card directory, inside an "osmdroid" sub-directory (if you already used OSMNavigator before, this "osmdroid" sub-directory already exists).<br>
<br>
All compatible map files found in this directory will be loaded by OSMNavigator when the application starts.<br>
<br>
<h3>Using MapsForge maps</h3>
You can download a MapsForge map on <a href='http://download.mapsforge.org'>MapsForge Download server</a>.<br>
Copy it on your device, in your SD card directory, inside a "mapsforge" sub-directory.<br>
In the "Tile Provider" option item, take care to select MapsForge.<br>
<br>
If you have multiple ".map" files in this directory, the first one (in alphabetical order) will be loaded.<br>
<br>
<br>
<h3>Using the Cache Manager</h3>
In the options menu, you can use the "Cache Manager" option to download in advance the tiles of an area.<br>
<br>
Note that the "depth" is limited to 5 zoom levels, from the current zoom level.<br>
<br>
The Cache Manager downloads the tiles in the standard osmdroid cache. It can be used multiple times, at various positions and zoom levels.