# Introduction #

So now, with OSMBonusPack, you have simple access to 3 routing services:
  * GraphHopper
  * MapQuest
  * OSRM

And you can switch between those routing services, by just calling the right RoadManager.
But choosing the right routing service may not be so simple.
You will find here some elements to help you in this choice.

# Common strengths #
They all provide:
  * Worldwide coverage
  * Waypoints support
  * Route instructions
  * Fresh OSM data

Not surprisingly, they are the 3 routing services chosen by the official [OpenStreetMap web site](https://blog.openstreetmap.org/2015/02/16/routing-on-openstreetmap-org) for providing directions.


# Performance #
On a 350km trip (in France, Rennes-Paris), the duration of an end-to-end route retrieval (including full parsing of server response) is, on an average of 5 requests:
  * With MapQuest Open API: 6 seconds (5s min, 7s max)
  * With Google Directions API: 1.6s (1.5s min, 1.9s max)
  * With OSRM demo service: 0.8s (0.4s min, 1.2s max)
  * With GraphHopper service: not measured yet, but seems similar to OSRM

Yes, OSRM and GraphHopper are really fast!

# OSRM #
Issues:
  * Some start/end points "in the green" will lead to no-route answer
  * The set of available directions is a little bit short. No "Enter/merge", or "Take exit lane on the left/on the right" for instance.
  * In the demo service, no bicycle or pedestrian routing.

OSRM project is quite new and very active, these issues may be fixed soon.

# MapQuest #
Issues:
  * Quite slow compared with others
  * Route is ending "one node too far". This is probably intentional, to give indications if the driver went too far. But not easy to hide.
  * API Key to request (but free, and quite easy to obtain)

# GraphHopper #
The newcomer. At the time of writing (August 2014), it is still in alpha phase, and support in OSMBonusPack has just been added.

Issues:
  * API Key to request to use the public service (not so easy to obtain - need direct contact with the author)
  * The set of available directions is a little bit short.

Pros:
  * Support bicyle and pedestrian modes.
  * Ability to provide altitude