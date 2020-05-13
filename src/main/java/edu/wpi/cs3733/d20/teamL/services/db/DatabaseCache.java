package edu.wpi.cs3733.d20.teamL.services.db;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point2D;

import lombok.extern.slf4j.Slf4j;

import com.google.inject.Inject;

import edu.wpi.cs3733.d20.teamL.entities.Building;
import edu.wpi.cs3733.d20.teamL.entities.Doctor;
import edu.wpi.cs3733.d20.teamL.entities.Edge;
import edu.wpi.cs3733.d20.teamL.entities.Gift;
import edu.wpi.cs3733.d20.teamL.entities.GiftDeliveryRequest;
import edu.wpi.cs3733.d20.teamL.entities.Graph;
import edu.wpi.cs3733.d20.teamL.entities.Kiosk;
import edu.wpi.cs3733.d20.teamL.entities.Node;
import edu.wpi.cs3733.d20.teamL.entities.Question;
import edu.wpi.cs3733.d20.teamL.entities.ServiceRequest;
import edu.wpi.cs3733.d20.teamL.entities.User;

@Slf4j
public class DatabaseCache implements IDatabaseCache {
    private ArrayList<Node> nodeCache = new ArrayList<>();
    private ArrayList<Edge> edgeCache = new ArrayList<>();
	private final ArrayList<Gift> giftCache = new ArrayList<>();
	private final ArrayList<User> userCache = new ArrayList<>();
	private final ArrayList<Doctor> doctorCache = new ArrayList<>();
	private final ArrayList<Kiosk> kioskCache = new ArrayList<>();
	private final ArrayList<Question> questionCache = new ArrayList<>();
	private final ArrayList<ServiceRequest> requests = new ArrayList<>();
	private final ArrayList<GiftDeliveryRequest> giftRequests = new ArrayList<>();
	private Map<String, Integer> cartCache = new HashMap<>();
    private final ArrayList<Node> addedNodes = new ArrayList<>();
    private final ArrayList<Edge> addedEdges = new ArrayList<>();
    private final ArrayList<Node> deletedNodes = new ArrayList<>();
    private final ArrayList<Edge> deletedEdges = new ArrayList<>();
	private ArrayList<Node> editedNodes = new ArrayList<>();
    private ArrayList<Edge> editedEdges = new ArrayList<>();
    @Inject
    private IDatabaseService db;

    @Override
    public void cacheAllFromDB() {
        cacheNodesFromDB();
        cacheEdgesFromDB();
        cacheGiftsFromDB();
        cacheUsersFromDB();
        cacheDoctorsFromDB();
        cacheQuestionsFromDB();
        cacheKiosksFromDB();
    }

    /**
     * Sets the nodes cache to the given ArrayList of nodes. This will overwrite the cache and eventually overwrite the database, use this when you are sure you want to make changes to the map.
     *
     * @param newNodes A new list of nodes to set the nodes cache to.
     */
    @Override
    public void cacheNodes(ArrayList<Node> newNodes, ArrayList<Node> editedNodes) {
        for (Node node : newNodes) {
            if (!nodeCache.contains(node)) addedNodes.add(node);
        }
        for (Node node : nodeCache) {
            if (!newNodes.contains(node)) deletedNodes.add(node);
        }
        editedNodes.removeIf(deletedNodes::contains);
        this.editedNodes = editedNodes;
        this.nodeCache = newNodes;
    }

    @Override
    public void setEditedNodes(ArrayList<Node> editedNodes) {
        this.editedNodes = editedNodes;
    }

    @Override
    public void setEditedEdges(ArrayList<Edge> editedEdges) {
        this.editedEdges = editedEdges;
    }

    /**
     * Sets the edges cache to the given ArrayList of edges. This will overwrite the cache and eventually overwrite the database, use this when you are sure you want to make changes to the map.
     *
     * @param newEdges A new list of edges to set the edges cache to.
     */
    @Override
    public void cacheEdges(ArrayList<Edge> newEdges, ArrayList<Edge> editedEdges) {
        for (Edge edge : newEdges) {
            if (!edgeCache.contains(edge)) {
                addedEdges.add(edge);
            }
        }
        for (Edge edge : edgeCache) if (!newEdges.contains(edge)) deletedEdges.add(edge);
        this.editedEdges = editedEdges;
        this.edgeCache = newEdges;
    }

    /**
     * Pushes all caches to the database.
     */
    @Override
    public void updateDB() {
        // Concatenate the node and edges lists for one update on the cache
        ArrayList<SQLEntry> updates = new ArrayList<>();

        // Delete edges
        for (Edge edge : deletedEdges) {
            updates.add(new SQLEntry(DBConstants.REMOVE_EDGE, new ArrayList<>(Collections.singletonList(edge.getID()))));
        }
        // Delete nodes
        for (ArrayList<String> currentNode : convertNodesToValuesList(deletedNodes)) {
            updates.add(new SQLEntry(DBConstants.REMOVE_NODE, new ArrayList<>(Collections.singletonList(currentNode.get(0)))));
        }
        // Add nodes
        for (ArrayList<String> nodeInfo : convertNodesToValuesList(addedNodes)) {
            updates.add(new SQLEntry(DBConstants.ADD_NODE, nodeInfo));
        }
        // Add edges
        for (ArrayList<String> edgeInfo : convertEdgesToValuesList(addedEdges)) {
            updates.add(new SQLEntry(DBConstants.ADD_EDGE, edgeInfo));
        }
        // Edit nodes
        for (ArrayList<String> currentNode : convertNodesToValuesList(editedNodes)) {
            String nodeID = currentNode.get(0);
            currentNode.remove(0);
            currentNode.add(nodeID);
            updates.add(new SQLEntry(DBConstants.UPDATE_NODE, currentNode));
        }
        // Edit edges
        for (ArrayList<String> currentEdge : convertEdgesToValuesList(editedEdges)) {
            String edgeID = currentEdge.get(0);
            currentEdge.remove(0);
            currentEdge.add(edgeID);
            updates.add(new SQLEntry(DBConstants.UPDATE_EDGE, currentEdge));
        }

        db.executeUpdates(updates);
        // Clear added, edited, and deleted nodes from cache
        addedNodes.clear();
        addedEdges.clear();
        editedNodes.clear();
        editedEdges.clear();
        deletedEdges.clear();
        deletedNodes.clear();
    }

    /**
     * Converts a list of nodes into a list of all the nodes' fields
     *
     * @param nodes This list of Nodes to be converted
     * @return A list of lists of entries representing individual nodes
     */
    @Override
    public ArrayList<ArrayList<String>> convertNodesToValuesList(List<Node> nodes) {
        ArrayList<ArrayList<String>> valuesList = new ArrayList<>();
        for (Node node : nodes) {
            valuesList.add(node.toArrayList());
        }
        return valuesList;
    }

    /**
     * Converts a list of edges into a list of all the edges' fields
     *
     * @param edges The list of edges to be converted
     * @return A list of lists of entries representing individual edges
     */

    @Override
    public ArrayList<ArrayList<String>> convertEdgesToValuesList(List<Edge> edges) {
        ArrayList<ArrayList<String>> valuesList = new ArrayList<>();

        for (Edge edge : edges) {
            valuesList.add(edge.toArrayList());
        }

        return valuesList;
    }

    /**
     * cacheNode: Populates the node cache with nodes from the Database
     */
    @Override
    public void cacheNodesFromDB() {
        ResultSet resSet = db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_NODES));
        clearNodeCache();
        ArrayList<ArrayList<String>> nodeData = db.getTableFromResultSet(resSet);

        for (ArrayList<String> row : nodeData) {
            nodeCache.add(new Node(row.get(0),
                    new Point2D(Double.parseDouble(row.get(1)), Double.parseDouble(row.get(2))),
                    row.get(3), row.get(4), row.get(5), row.get(6), row.get(7), Integer.parseInt(row.get(8))));
        }
    }

    /**
     * clearNodeCache: Clears the Nodes cache
     */
    @Override
    public void clearNodeCache() {
        nodeCache.clear();
    }

    @Override
    public ArrayList<Node> getNodeCache() {
        return nodeCache;
    }

    public ArrayList<Edge> getEdgeCache() {
        return edgeCache;
    }

    /**
     * cacheEdgesFromDB: Populates the edge cache with edges from the Database
     */
    @Override
    public void cacheEdgesFromDB() {
        ResultSet resSet = db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_EDGES));
        clearEdgeCache();
        ArrayList<ArrayList<String>> edgeData = db.getTableFromResultSet(resSet);

        for (ArrayList<String> row : edgeData) {
            Edge newEdge = new Edge(searchNodeCache(row.get(1)), searchNodeCache(row.get(2)), Integer.parseInt(row.get(3)));
            edgeCache.add(newEdge);
        }
    }

    /**
     * Searches the nodeCache for a node with a particular nodeID.
     *
     * @param nodeID The nodeID to search for.
     * @return The node with the associated nodeID. {null} If the nodeID is not found.
     */
    @Override
    public Node searchNodeCache(String nodeID) {
        for (Node node : nodeCache) {
            if (node.getID().equals(nodeID)) return node;
        }
        throw new RuntimeException("Did not find node " + nodeID);
    }

    /**
     * clearEdgeCache: Clears the Edges cache
     */
    @Override
    public void clearEdgeCache() {
        edgeCache.clear();
    }

    public Building getBuilding(String building) {
        Building newBuilding = new Building(building);
        try {
            newBuilding.addAllNodes(getNodeCache());
        } catch (IllegalArgumentException ex) {
            log.error("Encountered IllegalArgumentException", ex);
        }

        Graph.graphFromCache(newBuilding.getNodes(), getEdgeCache());

        return newBuilding;
    }

    @Override
    public void cacheGiftsFromDB() {
        ArrayList<ArrayList<String>> giftsDB = db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_GIFTS)));
        clearGiftsCache();
        for (ArrayList<String> g : giftsDB) {
            giftCache.add(new Gift(g.get(0), g.get(1), g.get(2), g.get(3), g.get(4), Double.parseDouble(g.get(5))));//Double.parseDouble(g.get(5))
        }
    }

    @Override
    public void updateInventory() {
        ArrayList<SQLEntry> updates = new ArrayList<>();

        for (String giftType : cartCache.keySet()) {
            for (Gift gift : giftCache) {
                if (gift.getSubtype().equals(giftType)) {
                    gift.setInventory(Integer.toString(Integer.parseInt(gift.getInventory()) - cartCache.get(giftType)));
                    ArrayList<String> values = new ArrayList<>();
                    values.add(gift.getInventory());
                    values.add(gift.getID());
                    updates.add(new SQLEntry(DBConstants.UPDATE_GIFT_INVENTORY, values));
                }
            }
        }
        db.executeUpdates(updates);
    }

    @Override
    public void cacheCart(Map<String, Integer> cart) {
        cartCache = cart;
    }

    @Override
    public Map<String, Integer> getCartCache() {
        return cartCache;
    }

    @Override
    public ArrayList<Gift> getGiftCache() {
        return giftCache;
    }

    @Override
    public void clearGiftsCache() {
        giftCache.clear();
        cartCache.clear();
    }

    @Override
    public void clearCartCache() {
        cartCache.clear();
    }

	@Override
	public void cacheUsersFromDB() {
		ArrayList<ArrayList<String>> usersTable = db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_USERS)));
		clearUserCache();
		for (ArrayList<String> row : usersTable) {
			userCache.add(new User(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4), row.get(5), row.get(6)));
		}
	}

	@Override
	public ArrayList<User> getUserCache() {
		return userCache;
	}

	@Override
	public void clearUserCache() {
    	userCache.clear();
	}

	@Override
	public void cacheDoctorsFromDB() {
		ArrayList<ArrayList<String>> doctorsTable = db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_DOCTORS)));
		clearDoctorCache();
		for (ArrayList<String> row : doctorsTable) {
			doctorCache.add(new Doctor(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4), row.get(5)));
		}
	}

	@Override
	public ArrayList<Doctor> getDoctorCache() {
		return doctorCache;
	}

	@Override
	public void clearDoctorCache() {
    	doctorCache.clear();
	}

	@Override
	public void cacheKiosksFromDB() {
		ArrayList<ArrayList<String>> kiosksTable = db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_KIOSK_SETTINGS)));
		clearDoctorCache();
		for (ArrayList<String> row : kiosksTable) {
			kioskCache.add(new Kiosk(row.get(0), row.get(1), Long.parseLong(row.get(2)), Long.parseLong(row.get(3)), Long.parseLong(row.get(4)), Long.parseLong(row.get(5))));
		}
	}

	@Override
	public ArrayList<Kiosk> getKioskCache() {
		return kioskCache;
	}

	@Override
	public void clearKioskCache() {

	}

    @Override
    public void cacheQuestionsFromDB() {
        ArrayList<ArrayList<String>> questionTable = db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_SCREENING_QUESTIONS)));
        for(ArrayList<String> row : questionTable) {
            questionCache.add(new Question(row.get(1), Integer.parseInt(row.get(2)), Integer.parseInt(row.get(3)), Integer.parseInt(row.get(4))));
        }
    }

    @Override
    public ArrayList<Question> getQuestions() {
        return questionCache;
    }

	@Override
	public void cacheRequestsFromDB() {
        ResultSet resSet = db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_SERVICE_REQUESTS));
        clearRequestCache();
        ArrayList<ArrayList<String>> requestData = db.getTableFromResultSet(resSet);

        for (ArrayList<String> row : requestData) {
            requests.add(new ServiceRequest(row.get(0), null, null, null, null, row.get(4), row.get(5), row.get(6), row.get(7), row.get(8), row.get(9)));
        }

        resSet = db.executeQuery(new SQLEntry(DBConstants.SELECT_ALL_GIFT_DELIVERY_REQUESTS));
        requestData = db.getTableFromResultSet(resSet);

        for (ArrayList<String> row : requestData) {
            String patientID = row.get(1);
            String roomID = db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.GET_PATIENT_ROOM, new ArrayList<>(Collections.singletonList(patientID))))).get(0).get(0);
            giftRequests.add(new GiftDeliveryRequest(row.get(0), null, null, roomID, null, null, null, row.get(5), row.get(6), row.get(7), row.get(8), row.get(9)));
        }
    }

    @Override
    public void clearRequestCache() {
        requests.clear();
        giftRequests.clear();
    }

    @Override
    public ArrayList<ServiceRequest> getAllRequests() {
        return requests;
    }

    @Override
    public ArrayList<GiftDeliveryRequest> getAllGiftRequests() {
        return giftRequests;
    }

    @Override
    public ArrayList<ServiceRequest> getAllSpecificRequest(String service) {
        ArrayList<ServiceRequest> reqs = new ArrayList<>();
        for(ServiceRequest req : requests) {
            if(req.getService().equals(service)) {
                reqs.add(req);
            }
        }
        return reqs;
    }
}
