// License: GPL. For details, see LICENSE file.
package PerpendicularWayFromNode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;

class PerpendicularWayCreationResult {
    private static class PerpendicularWayData {
        Way reference;
        Node fromNode;
        WaySegment closestSegment;
        EastNorth closestPoint;

        public PerpendicularWayData(
                Way reference, Node fromNode, WaySegment closestSegment, EastNorth closestPoint) {
            this.reference = reference;
            this.fromNode = fromNode;
            this.closestSegment = closestSegment;
            this.closestPoint = closestPoint;
        }
    }

    private PerpendicularWayData data;
    private String errorMessage;
    private boolean isValid;

    public PerpendicularWayCreationResult(
            Way reference, Node fromNode, WaySegment closestSegment, EastNorth closestPoint) {
        this.data = new PerpendicularWayData(reference, fromNode, closestSegment, closestPoint);
        this.isValid = true;
    }

    public PerpendicularWayCreationResult(String errorMessage) {
        this.errorMessage = errorMessage;
        this.isValid = false;
    }

    public boolean isValid() {
        return isValid;
    }

    public void performAction(DataSet ds) {
        performAction(ds, null);
    }

    public void performAction(DataSet ds, Way wayToContinue) {
        if(!isValid) {
            new Notification(this.errorMessage)
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        Node referenceStart = data.closestSegment.getFirstNode();
        Node referenceEnd = data.closestSegment.getSecondNode();

        EastNorth referenceStartEN = referenceStart.getEastNorth();
        EastNorth referenceEndEN = referenceEnd.getEastNorth();

        Collection<Command> commands = new LinkedList<>();

        Node closestNode = new Node(data.closestPoint);
        boolean willAddClosestNode = false;
        if(data.closestPoint.equalsEpsilon(referenceStartEN, 1e-5)) {
            closestNode = referenceStart;
        } else if(data.closestPoint.equalsEpsilon(referenceEndEN, 1e-5)) {
            closestNode = referenceEnd;
        } else {
            commands.add(new AddCommand(ds, closestNode));
            willAddClosestNode = true;
        }

        MapView mapView = MainApplication.getMap().mapView;
        if(!mapView.isActiveLayerDrawable())
        {
            new Notification(tr("The current layer is not drawable."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }
        List<WaySegment> wss = mapView.getNearestWaySegments(
                mapView.getPoint(closestNode), OsmPrimitive::isSelectable);
        insertNodeIntoAllNearbySegments(wss, closestNode, commands);

        Way perpendicularWay = null;
        if(wayToContinue != null) {
            List<Node> wayNodes = wayToContinue.getNodes();
            wayNodes.add(closestNode);
            commands.add(new ChangeNodesCommand(ds, wayToContinue, wayNodes));
        } else {
            perpendicularWay = new Way();
            perpendicularWay.addNode(data.fromNode);
            perpendicularWay.addNode(closestNode);
            commands.add(new AddCommand(ds, perpendicularWay));
        }

        if(willAddClosestNode) {
            List<Node> wayNodes = data.reference.getNodes();
            wayNodes.add(data.closestSegment.getUpperIndex(), closestNode);
            commands.add(new ChangeNodesCommand(ds, data.reference, wayNodes));
        }

        SequenceCommand sequence = new SequenceCommand(tr("Perpendicular Way from Node"), commands);
        UndoRedoHandler.getInstance().add(sequence);

        ds.setSelected((wayToContinue != null) ? wayToContinue : perpendicularWay);
    }

    /**
     * Modified from
     * {@link org.openstreetmap.josm.actions.mapmode.DrawAction#insertNodeIntoAllNearbySegments DrawAction.insertNodeIntoAllNearbySegments}
     */
    private static void insertNodeIntoAllNearbySegments(List<WaySegment> wss, Node n, Collection<Command> cmds) {
        Map<Way, List<Integer>> insertPoints = new HashMap<>();
        for (WaySegment ws : wss) {
            Way way = ws.getWay();
            if(Geometry.getDistanceWayNode(way, n) <= 1e-5) {
                List<Integer> is;
                if (insertPoints.containsKey(way)) {
                    is = insertPoints.get(way);
                } else {
                    is = new ArrayList<>();
                    insertPoints.put(way, is);
                }

                is.add(ws.getLowerIndex());
            }
        }

        for (Map.Entry<Way, List<Integer>> insertPoint : insertPoints.entrySet()) {
            Way w = insertPoint.getKey();
            List<Integer> is = insertPoint.getValue();

            List<Node> modNodes = w.getNodes();
            pruneSuccsAndReverse(is);
            for (int i : is) {
                modNodes.add(i + 1, n);
            }

            cmds.add(new ChangeNodesCommand(insertPoint.getKey(), modNodes));
        }
    }

    /**
     * Copied from
     * {@link org.openstreetmap.josm.actions.mapmode.DrawAction#pruneSuccsAndReverse DrawAction.pruneSuccsAndReverse}
     */
    private static void pruneSuccsAndReverse(List<Integer> is) {
        Set<Integer> is2 = new HashSet<>();
        for (int i : is) {
            if (!is2.contains(i - 1) && !is2.contains(i + 1)) {
                is2.add(i);
            }
        }
        is.clear();
        is.addAll(is2);
        Collections.sort(is);
        Collections.reverse(is);
    }
}
