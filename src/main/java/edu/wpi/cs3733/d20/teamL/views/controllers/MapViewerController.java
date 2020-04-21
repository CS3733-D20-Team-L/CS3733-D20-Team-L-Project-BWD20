package edu.wpi.cs3733.d20.teamL.views.controllers;

import com.jfoenix.controls.JFXAutoCompletePopup;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import edu.wpi.cs3733.d20.teamL.entities.Node;
import edu.wpi.cs3733.d20.teamL.services.db.DBCache;
import edu.wpi.cs3733.d20.teamL.services.graph.MapParser;
import edu.wpi.cs3733.d20.teamL.services.graph.Path;
import edu.wpi.cs3733.d20.teamL.services.graph.PathFinder;
import edu.wpi.cs3733.d20.teamL.services.navSearch.SearchFields;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderHelper;
import edu.wpi.cs3733.d20.teamL.util.io.SMSSender;
import edu.wpi.cs3733.d20.teamL.views.components.EdgeGUI;
import edu.wpi.cs3733.d20.teamL.views.components.MapPane;
import edu.wpi.cs3733.d20.teamL.views.components.NodeGUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Iterator;

@Slf4j
public class MapViewerController {
    @FXML
    MapPane map;

    @FXML
    JFXTextField startingPoint, destination;

    @FXML
    VBox instructions;

    @FXML
    JFXButton btnTextMe;

    @Inject
    private DBCache dbCache;

    private SearchFields sf;
    private JFXAutoCompletePopup<String> autoCompletePopup;
    private FXMLLoaderHelper loaderHelper = new FXMLLoaderHelper();
    private String directions;

    @FXML
    public void initialize() {
        dbCache.cacheAllFromDB();

        map.setEditable(false);

        map.setGraph(MapParser.getGraphFromCache(dbCache.getNodeCache()));

        map.setZoomLevel(1);
        map.init();
        map.getScroller().setVvalue(0.5);
        map.getScroller().setHvalue(0.5);

        sf = new SearchFields(dbCache.getNodeCache());
        sf.populateSearchFields();
        autoCompletePopup = new JFXAutoCompletePopup<>();
        autoCompletePopup.getSuggestions().addAll(sf.getSuggestions());
    }

    @FXML
    private void startingPointAutocomplete() {
        autocomplete(startingPoint);
    }

    @FXML
    private void destinationAutocomplete() {
        autocomplete(destination);
    }

    private void autocomplete(JFXTextField field) {
        autoCompletePopup.setSelectionHandler(event -> field.setText(event.getObject()));
        field.textProperty().addListener(observable -> {
            autoCompletePopup.filter(string ->
                    string.toLowerCase().contains(field.getText().toLowerCase()));
            if (autoCompletePopup.getFilteredSuggestions().isEmpty() ||
                    field.getText().isEmpty()) {
                autoCompletePopup.hide();
            } else {
                autoCompletePopup.show(field);
            }
        });
    }

    public void setStartingPoint(String startingPoint) {
        this.startingPoint.setText(startingPoint);
    }

    public void setDestination(String destination) {
        this.destination.setText(destination);
    }

    @FXML
    public void navigate() {
        Node startNode = sf.getNode(startingPoint.getText());
        Node destNode = sf.getNode(destination.getText());

        if (startNode != null && destNode != null) {
        	directions = highlightSourceToDestination(startNode, destNode);
            Label directionsLabel = new Label();
            directionsLabel.setText(directions);
            directionsLabel.setTextFill(Color.WHITE);
            directionsLabel.setWrapText(true);

            instructions.getChildren().clear();
            instructions.getChildren().add(directionsLabel);
            instructions.setVisible(true);
			btnTextMe.setText("Text Me Directions");
			btnTextMe.setDisable(false);
            btnTextMe.setVisible(true);
        }
    }

    @FXML
    private void backToMain() {
        try {
            Stage stage = (Stage) startingPoint.getScene().getWindow();
            Parent newRoot = loaderHelper.getFXMLLoader("Home").load();
            Scene newScene = new Scene(newRoot);
            stage.setScene(newScene);
            stage.show();
        } catch (Exception ex) {
            log.error("Encountered Exception.", ex);
        }
    }

    private String highlightSourceToDestination(Node source, Node destination) {
        map.getSelector().clear();

        Path path = PathFinder.aStarPathFind(map.getGraph(), source, destination);
        Iterator<Node> nodeIterator = path.iterator();

        // Loop through each node in the path and select it as well as the edge pointing to the next node
        Node currentNode = nodeIterator.next();
        Node nextNode;

        while (nodeIterator.hasNext()) {
            nextNode = nodeIterator.next();
            NodeGUI nodeGUI = map.getNodeGUI(currentNode);
            EdgeGUI edgeGUI = map.getEdgeGUI(currentNode.getEdge(nextNode));

            //map.getSelector().add(nodeGUI);
            map.getSelector().add(edgeGUI);

            currentNode = nextNode;
        }

        // The above loop does not highlight the last node, this does that
        NodeGUI nodeGUI = map.getNodeGUI(currentNode);
        map.getSelector().add(nodeGUI);
        nodeGUI.setHighlighted(true);

        return path.generateTextMessage();
    }

	public void handleButtonAction(ActionEvent event) {
		if (event.getSource() == btnTextMe) {
			SMSSender sender = new SMSSender();
			// Temporarily hard-coded as Luke's phone number
			sender.sendMessage(directions, "2073186779");
			btnTextMe.setText("Sent!");
			btnTextMe.setDisable(true);
		}
	}

    public MapPane getMap() {
        return map;
    }
}