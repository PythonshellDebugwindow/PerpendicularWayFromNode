// License: GPL. For details, see LICENSE file.
package PerpendicularWayFromNode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Shortcut;

class PerpendicularWayFromNodeAction extends JosmAction {
    PerpendicularWayFromNodeAction() {
        super(tr("Perpendicular Way from Node"), "perpendicularwayfromnode",
                tr("Create a perpendicular way from a starting node."),
                Shortcut.registerShortcut("perpendicularwayfromnode:action",
                        tr("More tools: {0}", tr("Perpendicular Way from Node")),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if(ds == null) {
            return;
        }

        Collection<OsmPrimitive> selection = ds.getSelected();

        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<Way> ways = new ArrayList<>();

        for(OsmPrimitive primitive : selection) {
            if(primitive instanceof Node) {
                nodes.add((Node) primitive);
            } else if(primitive instanceof Way) {
                ways.add((Way) primitive);
            } else {
                showSelectionMessage();
                return;
            }
        }

        if(nodes.size() == 1 && ways.size() == 1) {
            Node fromNode = nodes.get(0);
            Way reference = ways.get(0);

            PerpendicularWayCreationResult result = getPerpendicularWayFromNodeResult(fromNode, reference);
            result.performAction(ds);
        } else if(nodes.size() == 0 && ways.size() == 2) {
            Way way1 = ways.get(0);
            Way way2 = ways.get(1);

            PerpendicularWayCreationResult first = getPerpendicularWayFromNodeResult(way1, way2),
                    second = getPerpendicularWayFromNodeResult(way2, way1);

            if(first.isValid() && second.isValid()) {
                new Notification(
                        tr("The current selection is ambiguous. Try selecting a node and a way instead."))
                        .setIcon(JOptionPane.INFORMATION_MESSAGE)
                        .setDuration(Notification.TIME_SHORT)
                        .show();
            } else if(first.isValid()) {
                first.performAction(ds, way1);
            } else if(second.isValid()) {
                second.performAction(ds, way2);
            } else {
                new Notification(
                        tr("A perpendicular way could not be created from the selected elements."))
                        .setIcon(JOptionPane.INFORMATION_MESSAGE)
                        .setDuration(Notification.TIME_SHORT)
                        .show();
            }
        } else {
            showSelectionMessage();
        }
    }

    private static PerpendicularWayCreationResult getPerpendicularWayFromNodeResult(
            Way toContinue, Way reference) {
        if(toContinue.isClosed()) {
            return new PerpendicularWayCreationResult(tr("Cannot extend a closed way."));
        }
        return getPerpendicularWayFromNodeResult(toContinue.lastNode(), reference);
    }

    private static PerpendicularWayCreationResult getPerpendicularWayFromNodeResult(
            Node fromNode, Way reference) {
        WaySegment closestSegment = Geometry.getClosestWaySegment(reference, fromNode);
        if(closestSegment == null) {
            return new PerpendicularWayCreationResult(
                    tr("A perpendicular way could not be created from the selected elements."));
        }

        Node referenceStart = closestSegment.getFirstNode();
        Node referenceEnd = closestSegment.getSecondNode();

        EastNorth referenceStartEN = referenceStart.getEastNorth();
        EastNorth referenceEndEN = referenceEnd.getEastNorth();
        EastNorth fromEN = fromNode.getEastNorth();

        EastNorth closestPoint = Geometry.closestPointToLine(referenceStartEN, referenceEndEN, fromEN);

        Node closestNode = new Node(closestPoint);
        if(Geometry.getDistanceWayNode(closestSegment.toWay(), closestNode) > 1e-5) {
            return new PerpendicularWayCreationResult(
                    tr("A perpendicular way could not be created from the selected elements."));
        } else if(Geometry.getDistance(fromNode, closestNode) < 1e-5) {
            return new PerpendicularWayCreationResult(
                    tr("The way resulting from this action would be of length zero. " +
                            "Please use a different selection."));
        }

        return new PerpendicularWayCreationResult(reference, fromNode, closestSegment, closestPoint);
    }

    private static void showSelectionMessage() {
        new Notification(
                tr("Please select:\n" +
                        "* One node and one way; or\n" +
                        "* Two ways."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_SHORT)
                .show();
    }
}
