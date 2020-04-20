package edu.wpi.cs3733.d20.teamL.services.graph;

import edu.wpi.cs3733.d20.teamL.entities.Node;
import edu.wpi.cs3733.d20.teamL.entities.Edge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Graph {

    private Map<String, Node> nodes = new ConcurrentHashMap<>();

    /**
     * Gets the collection of Nodes contained in this graph.
     *
     * @return The collection of Nodes in this graph
     */
    public Collection<Node> getNodes() {
        return nodes.values();
    }

    /**
     * Gets a list of all edges between nodes in this Graph
     *
     * @return An ArrayList of Edges
     */
    public List<Edge> getEdges() {
        ArrayList<Edge> edges = new ArrayList<>();
        ArrayList<Edge> blackList = new ArrayList<>();

        for (Node node : getNodes()) {
            for (Edge edge : node.getEdges()) {
                if (!edges.contains(edge) && blackList.contains(edge)) edges.add(edge);
                if (edge.getDestination().getNeighbors().contains(node))
                    blackList.add(edge.getDestination().getEdge(node));
            }
        }

        return edges;
    }

    /**
     * Adds a new node to this graph.
     *
     * @param newNode The new node to add
     */
    public void addNode(Node newNode) {
        String name = newNode.getID();

        nodes.put(name, newNode);
    }

    /**
     * Adds all nodes in the given collection.
     *
     * @param newNodes Collection of nodes to add
     */
    public void addAllNodes(Collection<Node> newNodes) {
        for (Node node : newNodes) {
            addNode(node);
        }
    }

    /**
     * Adds all nodes in the given collection.
     *
     * @param newNodes Collection of nodes to add
     */
    public void addAllNodes(Node ... newNodes) {
        for (Node node : newNodes) {
            addNode(node);
        }
    }

    /**
     * Gets a node within this graph based on its name
     *
     * @param nodeID
     */
    public Node getNode(String nodeID) {
        return nodes.get(nodeID);
    }

    /**
     * Gets a collection of all the edges pointing to a given node.
     *
     * @param node The node the edges all point to
     * @return a collection of edges pointing to the node
     */
    public Collection<Edge> getEdgesPointingTo(Node node) {
        Collection<Edge> pointingEdges = new ArrayList<>();

        for (Edge edge : getEdges()) {
            if (edge.getDestination().equals(node))
                pointingEdges.add(edge);
        }

        return pointingEdges;
    }

    /**
     * Removes a given node.
     *
     * @param node The node to remove
     */
    public void removeNode(Node node) {
        nodes.remove(node.getID());
    }

    /**
     * Removes a Node based on its name.
     *
     * @param name String containing the name of the node to remove
     */
    public void removeNode(String name) {
        nodes.remove(name);
    }

    public String getUniqueNodeID() { // TODO: require the user to put in a nodeID instead of generating it
        String id = "new_node1";
        Integer curr = 1;
        boolean unique = false;
        while(!unique) {
            if(this.getNode(id) == null) unique = true;
            else {
                curr ++;
                id = "new_node" + curr.toString();
            }
        }
        return id;
    }

}
