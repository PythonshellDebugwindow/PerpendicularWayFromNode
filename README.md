# PerpendicularWayFromNode

PerpendicularWayFromNode is a [JOSM](https://josm.openstreetmap.de/) plugin
which allows you to easily create a perpendicular way from a starting node.

## Installation

PerpendicularWayFromNode can be installed via the Plugins tab in JOSM's
Preferences menu.

## Usage

The Perpendicular Way from Node action can be found in the More Tools menu.
When this action is invoked, the selection must consist of one node and one way
or of two ways.

### One node and one way

If one node and one way are selected, the plugin will attempt to create a new
way perpendicular to the selected way, with one end at the selected node and
the other end on the selected way. If the selected way has more than two nodes,
then only the segment which is closest to the selected node will be considered
when creating the new way.

### Two ways

If two ways are selected, the plugin will extend one of the existing ways
instead of creating a new one. If both of the selected ways can be extended
to form a valid perpendicular way, the selection is ambiguous, so no extension
will take place; to avoid ambiguity, instead invoke the action with one node
and one way selected, then manually join the created way to the existing way.

## License
GPL v2 or later.
