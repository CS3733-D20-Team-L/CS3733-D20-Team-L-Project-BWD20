package edu.wpi.cs3733.d20.teamL.entities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javafx.geometry.Point2D;

public class Path implements Iterable<Node> {
    private ArrayList<String> message = new ArrayList<>();
    private ArrayList<ArrayList<Node>> subpaths = new ArrayList<>();
    private List<Node> pathNodes = new LinkedList<>();
    private int length = 0;

    public List<Node> getPathNodes() {
        return pathNodes;
    }

    public int getLength() {
        return length;
    }

    /**
     * Gets the sum of the path lengths
     *
     * @return
     */
    int recalculateLength() {
        int retLength = 0;

        Iterator<Node> iterator = pathNodes.iterator();

        Node current = iterator.next();

        while (iterator.hasNext()) {
            Node next = iterator.next();
            retLength += current.getEdge(next).getLength();
            current = next;
        }

        length = retLength;

        return length;
    }

    /**
     * Adds a node to the end of the path.
     *
     * @param node The new node to add
     */
    public void add(Node node) {
        Edge lastEdge = null;
        if (pathNodes.size() > 0) {
            Node lastNode = pathNodes.get(pathNodes.size() - 1);
            lastEdge = lastNode.getEdge(node);
            if (lastEdge != null)
                length += lastEdge.getLength();
            else
                throw new IllegalArgumentException("Tried to add " + node.getID() + " to the path, but there was no edge between " + lastNode.getID() + " and " + node.getID());
        }

        pathNodes.add(node);
    }

    /**
     * Adds a node to the beginning of the path.
     *
     * @param node The new node to add
     */
    public void addFirst(Node node) {
        Edge firstEdge = null;
        if (pathNodes.size() > 0) firstEdge = node.getEdge(pathNodes.get(0));

        if (pathNodes.size() == 0 || firstEdge != null) {
            pathNodes.add(node);
            length += firstEdge.getLength();
        } else {
            throw new IllegalArgumentException(
                    "Given node does not have an edge pointing to the previous first node");
        }
    }

    /**
     * Adds each node in order to the path.
     *
     * @param nodes An ordered list of nodes to add
     */
    public void addAll(List<Node> nodes) {
        for (Node node : nodes) {
            add(node);
        }
    }

    public Iterator<Node> iterator() {
        return pathNodes.iterator();
    }

    /**
     * Converts an ordered list of nodes to a path. Ensure that there is an actual path of edges
     * between the nodes.
     *
     * @param list A list of nodes
     * @return A path containing all the nodes
     */
    public static Path listToPath(List<Node> list) {
        Path newPath = new Path();
        newPath.addAll(list);

        return newPath;
    }

    public String generateRobotPath() {
        StringBuilder robotPath = new StringBuilder("#");

        Point2D start, end;
        Node prev, curr, next;
        double angle;
        String sign;

        for (int i = 1; i < pathNodes.size() - 1; i++) {
            prev = pathNodes.get(i - 1);
            curr = pathNodes.get(i);
            next = pathNodes.get(i + 1);

            start = delta(prev, curr);
            end = delta(curr, next);
            angle = start.angle(end);

            robotPath.append("2 ");
            int length = ((int)curr.getEdge(next).getLength()) * 10;
            robotPath.append(length + ",");

            if (angle > 10) {
                sign = determineDirection(start, end);

                if(sign.equals("left")) robotPath.append("1,");
                else robotPath.append("3,");

            }
        }

        robotPath.append("#");

        return robotPath.toString();
    }

    public int getPathTime(String transportation) {
        int time = 0;
        for(int i = 0; i < pathNodes.size() - 1; i++) {
            Edge edge = pathNodes.get(i).getEdge(pathNodes.get(i+1));
            if(!edge.getSource().getBuilding().equals(edge.getDestination().getBuilding())) {
                if(transportation.equals("driving")) time += 10000;
                else if (transportation.equals("walking")) time += 100000;
            } else if (edge.getSource().getFloor() != edge.getDestination().getFloor()) {
                if(edge.getSource().getType().equals("ELEV")) time += 15 / .338;
                else time += 11 / .338;
            } else {
                time += edge.getLength();
            }
        }
        return (int) Math.round(time * .338 / 60);
    }

    public void generateTextMessage() {
        ArrayList<Node> subpath = new ArrayList<>();
        Point2D start, end;
        Node prev, curr, next;
        Node goal = pathNodes.get(pathNodes.size() - 1);
        double angle;
        String sign;
        String lastRoom = null;
        int lefts = 0;
        int rights = 0;
        boolean foundAdjRoom;
        boolean lastStatement = true;

        subpath.add(pathNodes.get(0));

        int pathTime = getPathTime("walking");
        if (pathTime == 0) message.add("Estimated Path Time: less than a minute.");
        else message.add("Estimated Path Time: " + getPathTime("walking") + " minutes.");
        subpaths.add(subpath);
        for (int i = 1; i < pathNodes.size() - 1; i++) {
            prev = pathNodes.get(i - 1);
            curr = pathNodes.get(i);
            next = pathNodes.get(i + 1);

            start = delta(prev, curr);
            end = delta(curr, next);
            angle = start.angle(end);

            subpath.add(curr);

            if (curr.getType().equals("ELEV") && next.getType().equals("ELEV")) {
                message.add("Take the elevator to floor " + next.getFloorAsString() + ".");

                lastRoom = null;
                rights = 0;
                lefts = 0;
                subpaths.add(addSubPath(subpath));
                subpath.clear();
            } else if (curr.getType().equals("STAI") && next.getType().equals("STAI")) {
                message.add("Take the stairs to floor " + next.getFloorAsString() + ".");

                lastRoom = null;
                rights = 0;
                lefts = 0;
                subpaths.add(addSubPath(subpath));
                subpath.clear();
            }  else if(!curr.getBuilding().equals(next.getBuilding())) {
                message.add("Navigate from " + curr.getBuilding() + " to " + next.getBuilding() + ".");

                lastRoom = null;
                rights = 0;
                lefts = 0;
                subpath.add(next);
                subpaths.add(addSubPath(subpath));
                subpath.clear();
            }
            else {
                if (angle > 10) {
                    StringBuilder builder = new StringBuilder();
                    sign = determineDirection(start, end);

                    if (lastRoom != null) {
                        builder.append("After you pass the " + parseLongName(lastRoom) + ", take ");
                    } else builder.append("Continue forward and take ");

                    if (lefts > 0 && sign.equals("left")) {
                        builder.append("the " + enumCounter(lefts + 1) + " left");
                    } else if (rights > 0 && sign.equals("right")) {
                        builder.append("the " + enumCounter(rights + 1) + " right");
                    } else {
                        builder.append("the next " + turnAmount(angle) + sign);
                    }

                    if (next.equals(goal)) {
                        builder.append(" to the " + parseLongName(goal.getLongName()));
                        lastStatement = false;
                    }
                    builder.append(".");

                    lastRoom = null;
                    rights = 0;
                    lefts = 0;

                    subpath.add(next);
                    message.add(builder.toString());
                    subpaths.add(addSubPath(subpath));
                    subpath.clear();

                } else {
                    if (!curr.getType().equals("HALL")) {
                        lefts = 0;
                        rights = 0;

                        subpath.add(next);
                        message.add("Cut straight through the " + curr.getLongName() + ".");
                        subpaths.add(addSubPath(subpath));
                        subpath.clear();
                    } else {
                        foundAdjRoom = false;
                        for (Node adj : curr.getNeighbors()) {
                            if (!adj.equals(prev) && !adj.equals(next)) {
                                if (adj.getType().equals("HALL")) {
                                    Point2D adjEnd = delta(curr, adj);
                                    String adjSign = determineDirection(start, adjEnd);
                                    if (adjSign.equals("right")) rights++;
                                    else lefts++;
                                } else {
                                    lastRoom = adj.getLongName();
                                    foundAdjRoom = true;
                                }
                            }
                        }
                        if (foundAdjRoom) {
                            lefts = 0;
                            rights = 0;
                        }
                    }
                }
            }
        }

        if (lastStatement) {
            message.add("Continue straight until your destination at " + goal.getLongName() + ".");
            subpath.add(pathNodes.get(pathNodes.size() - 1));
            subpaths.add(addSubPath(subpath));
            subpath.clear();
        }
    }

    private ArrayList<Node> addSubPath(ArrayList<Node> nodes) {
        ArrayList<Node> retPath = new ArrayList<>();

        for (Node node : nodes)
            retPath.add(node);

        return retPath;
    }

    private Point2D delta(Node curr, Node next) {
        return new Point2D(next.getPosition().getX() - curr.getPosition().getX(), next.getPosition().getY() - curr.getPosition().getY());
    }

    private String determineDirection(Point2D start, Point2D end) {
        double dir = -start.getX() * end.getY() + start.getY() * end.getX();
        if (dir > 0) return "left";
        return "right";
    }

    private String enumCounter(int num) {
        switch (num) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return num + "th";
        }
    }

    private String turnAmount(double angle) {
        if (angle < 45) return "slight ";
        else if (angle > 95) return "sharp ";
        else return "";
    }

    private String parseLongName(String name) {
        for (int i = 0; i < name.length() - 1; i++) {
            if (checkNumber(name.substring(i, i + 1))) {
                return name.substring(0, i - 1).toLowerCase();
            }
        }

        return name;
    }

    private boolean checkNumber(String s) {
        try {
            double test = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public ArrayList<String> getMessage() {
        return message;
    }

    public ArrayList<ArrayList<Node>> getSubpaths() {
        return subpaths;
    }
}