package edu.wpi.cs3733.d20.teamL.views.controllers.map;

import com.jfoenix.controls.*;
import edu.wpi.cs3733.d20.teamL.entities.*;
import edu.wpi.cs3733.d20.teamL.entities.Edge;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseCache;
import edu.wpi.cs3733.d20.teamL.services.pathfinding.IPathfinderService;
import edu.wpi.cs3733.d20.teamL.services.pathfinding.MapParser;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderHelper;
import edu.wpi.cs3733.d20.teamL.util.io.CSVHelper;
import edu.wpi.cs3733.d20.teamL.util.search.SearchFields;
import edu.wpi.cs3733.d20.teamL.views.components.*;
import edu.wpi.cs3733.d20.teamL.views.controllers.dialogues.DataDialogue;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;

import javafx.geometry.Point2D;


import java.util.*;
import javax.inject.Inject;

import javafx.scene.layout.VBox;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapEditorController {
    @FXML
    JFXTextField startNode, endNode;
    @FXML
    JFXButton pathFind, btnCancel, btnSave, saveToDB, saveToCSV, open, node, saveOptions, loadOptions, pathfindingOptions, floorUp, floorDown;
    @FXML
    JFXToggleNode eraser;
    @FXML
    BorderPane root;
    @FXML
    MapPane map;
    @FXML
    Label nodeIDText,  numberlbl;
    @FXML
    JFXTextField numberText, xCoordText, yCoordText, buildingText, nodeTypeText, shortNameText, longNameText;
    @FXML
    ComboBox nodeTypeValue;
    @FXML
    VBox editor, multiFloorConnection, nodeConnectionsTab, floorSelector;
	@FXML
    JFXNodesList saveNodesList, loadNodesList, pathNodesList;
	@FXML
    Tooltip saveTooltip, loadTooltip, pathfindTooltip;
	@FXML
    ImageView saveOptImg, loadOptionsImage, pathfindImage;

    @Inject
	private IDatabaseCache cache;
    @Inject
    private IPathfinderService pathfinder;

    private Scene scene;
    private FXMLLoaderHelper loaderHelper = new FXMLLoaderHelper();
    private SearchFields sf;
    private JFXAutoCompletePopup<String> autoCompletePopup;
    private boolean eraserBool = false;
    private char pathFindingAlg = 'A';

    private final List<String> types = Arrays.asList("ELEV", "REST", "STAI", "DEPT", "LABS", "INFO", "CONF", "EXIT", "RETL", "SERV");
    private int floor = 2;

    @FXML
    public void initialize() {
        scene = root.getScene();

        coreShortcuts();

        pathFind.setOnAction(event -> {
            Path path = pathfinder.pathfind(map.getBuilding(), sf.getNode(startNode.getText()), sf.getNode(endNode.getText()));
            System.out.println(path.generateTextMessage());

            Iterator<Node> nodeIterator = path.iterator();

            // Loop through each node in the path and select it as well as the edge pointing to the next node
            Node currentNode = nodeIterator.next();
            Node nextNode;
            while (nodeIterator.hasNext()) {
                nextNode = nodeIterator.next();

                NodeGUI nodeGUI = map.getNodeGUI(currentNode);
                EdgeGUI edgeGUI = map.getEdgeGUI(currentNode.getEdge(nextNode));

                map.getSelector().add(nodeGUI);
                map.getSelector().add(edgeGUI);

				currentNode = nextNode;
			}
			// The above loop does not highlight the last node, this does that
			NodeGUI nodeGUI = map.getNodeGUI(currentNode);
			map.getSelector().add(nodeGUI);
			nodeGUI.setHighlighted(true);
		});

        cache.cacheAllFromDB();

        sf = new SearchFields(cache.getNodeCache());
        sf.getFields().add(SearchFields.Field.nodeID);
        sf.getFields().add(SearchFields.Field.longName);
        sf.populateSearchFields();
        autoCompletePopup = new JFXAutoCompletePopup<>();
        autoCompletePopup.getSuggestions().addAll(sf.getSuggestions());

        map.setEditable(true);
        map.init();
        openFromDB();

        map.setZoomLevel(1);

        // Add floor buttons
        for (int i = 1; i <= map.getBuilding().getMaxFloor(); i++) {
            JFXButton newButton = new JFXButton();
            newButton.setButtonType(JFXButton.ButtonType.RAISED);
            newButton.getStylesheets().add("edu/wpi/cs3733/d20/teamL/css/MapStyles.css");
            newButton.setText("" + i);
            newButton.setOnAction(this::changeFloor);
            newButton.getStyleClass().add("floor-buttons");

            floorSelector.getChildren().add(1, newButton);
        }

        //Hides the node editor VBox
        editor.setPrefWidth(0);
        editor.setVisible(false);

        //Hides the edges editor VBox
        nodeConnectionsTab.setPrefWidth(0);
        nodeConnectionsTab.setVisible(false);

        map.recalculatePositions();

        eraser.setDisableAnimation(true);

        //saveNodesList.addAnimatedNode(saveDBButton);
	}

    /**
     * Creates and allows keyboard shortcuts on the map Editor
     */
    private void coreShortcuts() {
        // Instantiate key combinations with ancronyms as naming convention (cv --> Ctrl-V)
        KeyCombination cq = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
        KeyCombination cs = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination css = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        KeyCombination cz = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
        KeyCombination cy = new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN);
        KeyCombination co = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);

    }

    /**
     * @return a map pane that is used for both map editor and map viewer
     */
    public MapPane getMap() {
        return map;
    }

    @FXML
    private void autocompletestart() {
        sf.applyAutocomplete(startNode, autoCompletePopup);
    }

    @FXML
    private void autocompleteend() {
        sf.applyAutocomplete(endNode, autoCompletePopup);
    }

    @FXML
    private void quit() {
        cache.updateDB();
        Platform.exit();
    }

    @FXML
    private void saveToDB() {
        ArrayList<Node> nodes = new ArrayList<>(map.getBuilding().getNodes());
        ArrayList<Edge> blackList = new ArrayList<>();
        ArrayList<Edge> newEdges = new ArrayList<>();

        for (Node node : nodes) {
            for (Edge edge : node.getEdges()) {
                if (!newEdges.contains(edge) && blackList.contains(edge)) newEdges.add(edge);
                if (edge.getDestination().getNeighbors().contains(node))
                    blackList.add(edge.getDestination().getEdge(node));
            }
        }

        cache.cacheNodes(nodes, map.getEditedNodes());
        cache.cacheEdges(newEdges);
        cache.updateDB();

        map.getEditedNodes().clear();
    }

    @FXML
    public void saveToCSV() {
    	DataDialogue data = new DataDialogue();
    	data.setSaving(true);
    	data.showDialogue(pathFind.getScene().getWindow());
    	String nodeFilePath = data.getNodeFile().getAbsolutePath();
		String edgeFilePath = data.getEdgeFile().getAbsolutePath();
		CSVHelper csvHelper = new CSVHelper();
		ArrayList<ArrayList<String>> nodeTable = new ArrayList<>();
		ArrayList<ArrayList<String>> edgeTable = new ArrayList<>();
		ArrayList<Node> nodes = new ArrayList<>(map.getBuilding().getNodes());
		ArrayList<Edge> edges = new ArrayList<>(map.getBuilding().getEdges());
		nodeTable.add(new ArrayList<>(Arrays.asList("nodeID", "xCoord", "yCoord", "floor", "building", "nodeType", "longName", "shortName")));
		for (Node node : nodes) {
			nodeTable.add(node.toArrayList());
		}
		edgeTable.add(new ArrayList<>(Arrays.asList("edgeID", "startNode", "endNode")));
		for (Edge edge : edges) {
			edgeTable.add(edge.toArrayList());
		}
		csvHelper.writeToCSV(nodeFilePath, nodeTable);
		csvHelper.writeToCSV(edgeFilePath, edgeTable);
    }

    @FXML
    public void open() {
        DataDialogue data = new DataDialogue();
        boolean confirmed = data.showDialogue(pathFind.getScene().getWindow());

        if (confirmed) {
            Building newBuilding = MapParser.parseMapToBuilding(data.getNodeFile(), data.getEdgeFile());
            map.setBuilding(newBuilding);
        }
    }

    @FXML
    private void openFromDB() {
        cache.cacheAllFromDB();
        Building newBuilding = new Building("Faulkner");
        newBuilding.addAllNodes(cache.getNodeCache());

        map.setBuilding(newBuilding);
        map.setFloor(2);
    }

    @FXML
    private void insertNode() {
        Node node = new Node(map.getBuilding().getUniqueNodeID(), new Point2D(100,100), map.getFloor(), map.getBuilding().getName());
        map.addNode(node);
    }

    @FXML
    private void handleTools() {
        if (eraser.isSelected()) {
            map.setErasing(true);
        } else {
            map.setErasing(false);
        }
    }

    @FXML
    private void backToMain() {
        try {
			loaderHelper.goBack();
        } catch (Exception ex) {
            log.error("Encountered Exception.", ex);
        }
    }

    @FXML
    private void myCustomAction(MouseEvent event) {
        Node selectedNode = map.getSelectedNode();

        if(selectedNode == null) {
            editor.setPrefWidth(0);
            editor.setVisible(false);
        } else {
            editor.setPrefWidth(200);
            editor.setVisible(true);
            nodeIDText.setText(selectedNode.getID());
            Double x = selectedNode.getPosition().getX();
            Double y = selectedNode.getPosition().getY();
            xCoordText.setText(x.toString());
            yCoordText.setText(y.toString());
            nodeTypeValue.getSelectionModel().select(types.indexOf(selectedNode.getType())+1);
            shortNameText.setText(selectedNode.getShortName());
            longNameText.setText(selectedNode.getLongName());
            nodeTypeChanged();
        }
    }

    @FXML
    private void nodeTypeChanged() {
        int index = nodeTypeValue.getSelectionModel().getSelectedIndex();
        Node selected = map.getSelectedNode();

        if(index == 1) {
            numberlbl.setText("Elevator Number:");
            multiFloorConnection.setVisible(true);
            numberText.setText(selected.getShaft());
        } else if (index == 3) {
            numberlbl.setText("Stairwell Number:");
            multiFloorConnection.setVisible(true);
            numberText.setText(selected.getShaft());
        } else {
            multiFloorConnection.setVisible(false);
            numberText.setText("");
        }
    }

    @FXML
    private void updateNode() {
        Node selectedNode = map.getSelectedNode();
        NodeGUI selectedNodeGUI = map.getNodeGUI(selectedNode);
        Collection<Node> neighbors = selectedNode.getNeighbors();

        selectedNode.setId(nodeIDText.getText());
        double x = Double.parseDouble(xCoordText.getText());
        double y = Double.parseDouble(yCoordText.getText());
        selectedNode.setPosition(new Point2D(x, y));
        selectedNode.setBuilding(map.getBuilding().getName());
        selectedNode.setType(types.get(nodeTypeValue.getSelectionModel().getSelectedIndex()-1));
        selectedNode.setShortName(shortNameText.getText());
        selectedNode.setLongName(longNameText.getText());
        selectedNode.setShaft(numberText.getText());

        selectedNode.setId(map.getBuilding().getUniqueNodeID(selectedNode));
        nodeIDText.setText(selectedNode.getID());

        addMultiFloorEdge(selectedNode);
        map.removeNode(selectedNodeGUI);
        map.addNode(selectedNode);

        for(Node neighbor : neighbors) {
            Edge edge = new Edge(selectedNode, neighbor);
            selectedNode.addEdgeTwoWay(edge);
            map.addEdge(edge);
        }
    }

    /**
     * Adds a node with edges linking to nodes on other floors.
     *
     * @param node The new node to add
     */
    private void addMultiFloorEdge(Node node) {
        if(node.getType().equals("ELEV") || node.getType().equals("STAI")) {
            for(Node adj : map.getBuilding().getNodes()) {
                if(node.getType().equals(adj.getType()) && node.getShaft().equals(adj.getShaft())) {
                    if((node.getFloor() == adj.getFloor() + 1) || (node.getFloor() == adj.getFloor() - 1)) {
                        node.addEdgeTwoWay(new Edge(node, adj));
                    }
                }
            }
        }
    }

    @FXML
    private void changeFloor(ActionEvent event) {
        JFXButton sourceButton = (JFXButton) event.getSource();

        if(event.getSource() == floorUp && map.getFloor() < 5) {
            map.setFloor(map.getFloor() + 1);
            openFromDB();
        } else if (event.getSource() == floorDown && map.getFloor() > 1) {
            map.setFloor(map.getFloor() - 1);
            openFromDB();
        } else if (isNumeric(sourceButton.getText())) {
            map.setFloor(Integer.parseInt(sourceButton.getText()));
        }
    }

    private boolean isNumeric(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    /**
     * Show editConnects tab
     */
    @FXML
    private void editConnections() {
        nodeConnectionsTab.setPrefWidth(200);
        nodeConnectionsTab.setVisible(true);
    }

    /**
     * Hides editConnections tab
     */
    @FXML
    private void saveConnections() {
        nodeConnectionsTab.setPrefWidth(0);
        nodeConnectionsTab.setVisible(false);
    }

    /**
     * toggles mouse between normal mouse and eraser
     */
    @FXML
    private void eraserMouse() {
        if (!eraserBool) {
            Image eraserImage = new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/eraserMouse.png");  //pass in the image path
            map.getScene().setCursor(new ImageCursor(eraserImage));
            eraserBool = true;
        } else if (eraserBool) {
            eraser.getScene().setCursor(Cursor.DEFAULT);
            eraserBool = false;
        }
    }

    @FXML
    private void saveOptionsClicked() {
        //show/hide options image
        if(saveNodesList.isExpanded()) {
            saveTooltip.setText("Click to Close");
            saveOptImg.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/xButton.png", 40, 0, true, false));
        } else {
            saveTooltip.setText("Click to Show Save Options");
            saveOptImg.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/SaveToFile.png"));
        }
    }

    @FXML
    private void loadOptionsClicked() {
        //show/hide options image
        if(loadNodesList.isExpanded()) {
            loadTooltip.setText("Click to Close");
            loadOptionsImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/xButton.png"));
        } else {
            loadTooltip.setText("Click to Show Load Options");
            loadOptionsImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/UploadFromFolder.png"));
        }
    }

    @FXML
    private void pathfindOptionsClicked() {
        //show/hide options image
        if(pathNodesList.isExpanded()) {
            pathfindTooltip.setText("Click to Close");
            pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/xButton.png", 40, 0, true, false));
        } else {
            pathfindTooltip.setText("Switch Pathfinding Algorithm");
            if (pathFindingAlg == 'A') pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/AStar.png"));
            if (pathFindingAlg == 'B') pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/Breath First.png", 60, 0, true, false));
            if (pathFindingAlg == 'D') pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/DepthFirst.png"));
        }
    }

    @FXML
    private void aStarSelected(){
        pathFindingAlg = 'A';
        pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/AStar.png", 40, 0, true, false));
        pathNodesList.animateList(false);
    }

    @FXML
    private void depthSelected(){
        pathFindingAlg = 'D';
        pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/DepthFirst.png", 40, 0, true, false));
        pathNodesList.animateList(false);
    }

    @FXML
    private void breadthSelected(){
        pathFindingAlg = 'B';
        pathfindImage.setImage(new Image("/edu/wpi/cs3733/d20/teamL/assets/map editor/Breath First.png", 60, 0, true, false));
        pathNodesList.animateList(false);
    }


}
