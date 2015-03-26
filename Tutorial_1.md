_We assume you already followed [Tutorial\_0](Tutorial_0.md)._

# 1. "Hello, Routing World!" #

First, get a road manager:
```
RoadManager roadManager = new OSRMRoadManager();
```

Set-up your start and end points:
```
ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
waypoints.add(startPoint);
GeoPoint endPoint = new GeoPoint(48.4, -1.9);
waypoints.add(endPoint);
```
And retreive the road between those points:
```
Road road = roadManager.getRoad(waypoints);
```

then, build a Polyline with the route shape:
```
Polyline roadOverlay = RoadManager.buildRoadOverlay(road, this);
```

Add this Polyline to the overlays of your map:
```
map.getOverlays().add(roadOverlay);
```

And then? Refresh the map!
```
map.invalidate();
```

# 2. Playing with the RoadManager #

- Okay, but I wanted this route for bicycles...

- First, change to MapQuestRoadManager, as it supports bicycle routes. Bad news, MapQuest service now requires an API key, so you will also have to register at http://developer.mapquest.com, and obtain this API key.

Then, in the code, just few changes, not too complicated:
```
RoadManager roadManager = new MapQuestRoadManager("_YOUR MAPQUEST API KEY_");
```

Then set appropriate option (before getting the road):
```
roadManager.addRequestOption("routeType=bicycle");
```
(all MapQuest options are described here: http://open.mapquestapi.com/guidance/)

Done.

# 3. Showing the Route steps on the map #

At each road node, we put a Marker. Straightforward:

```
Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
for (int i=0; i<road.mNodes.size(); i++){
	RoadNode node = road.mNodes.get(i);
	Marker nodeMarker = new Marker(map);
	nodeMarker.setPosition(node.mLocation);
	nodeMarker.setIcon(nodeIcon);
	nodeMarker.setTitle("Step "+i);
	map.getOverlays().add(nodeMarker);
}
```

You don't have a marker\_node icon? Shame on you. Pick the OSMNavigator one (res/drawable-nodpi/marker\_node.png).

Here we are! Clicking on a step marker will open its bubble.

- Hey, guy, those bubbles are nice, but useless!

- You are right. So, go to step 4.

# 4. Filling the bubbles #

Set the bubble snippet with the instructions:
```
nodeMarker.setSnippet(node.mInstructions);
```

Set the bubble sub-description with the length and duration of the step:
```
nodeMarker.setSubDescription(Road.getLengthDurationText(node.mLength, node.mDuration));
```

And put an icon showing the maneuver at this step:
```
Drawable icon = getResources().getDrawable(R.drawable.ic_continue);
nodeMarker.setImage(icon);
```

Give it a try, you should get something like that:

<img src='http://osmbonuspack.googlecode.com/svn/BonusPackDownloads/img/tuto_routing.png' height='530'>

And, yes, you are right, maneuver icons are not correct!<br>
<br>
The maneuver id is in node.mManeuverType.<br>
The possible maneuver ids are <a href='http://open.mapquestapi.com/guidance/#maneuvertypes'>here</a>. And the maneuver icons are in OSMNavigator res/drawable-mpi.<br>
It's boring, so I'm going downstair to take a coffee, while you handle those icons properly.<br>
<br>
And see you again on <a href='Tutorial_2.md'>Tutorial_2</a>.