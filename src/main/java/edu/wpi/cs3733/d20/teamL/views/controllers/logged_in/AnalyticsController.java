package edu.wpi.cs3733.d20.teamL.views.controllers.logged_in;


import com.jfoenix.controls.JFXAutoCompletePopup;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import edu.wpi.cs3733.d20.teamL.App;
import edu.wpi.cs3733.d20.teamL.entities.*;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseCache;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderFactory;
import edu.wpi.cs3733.d20.teamL.util.search.SearchFields;
import edu.wpi.cs3733.d20.teamL.views.components.EdgeGUI;
import edu.wpi.cs3733.d20.teamL.views.components.MapPane;
import edu.wpi.cs3733.d20.teamL.views.components.NodeGUI;
import edu.wpi.cs3733.d20.teamL.views.controllers.map.MapViewerController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.scene.chart.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AnalyticsController implements Initializable {

    @FXML
    MapPane map;
    @FXML
    private JFXComboBox<String> timeBox, buildingChooser, heatBox;
    @FXML
    private BarChart<String, Number> ServiceReqHisto;
    @FXML
    private PieChart servicePieChart;
    @FXML
    VBox floorSelector;
    @FXML
    JFXButton floorUp, floorDown;

    private FXMLLoaderFactory loaderFactory = new FXMLLoaderFactory();
    private ObservableList<String> timeOptions = FXCollections.observableArrayList("Any time", "Past hour", "Past 24 hours", "Past month", "Past year");
    private ObservableList<ServiceRequest> requests;
    private ObservableList<GiftDeliveryRequest> giftRequests;
    ObservableList<String> heatOptions = FXCollections.observableArrayList("Pathfinding", "Gift Delivery", "Security", "Maintenance", "Internal Transportation",
            "External Transportation", "Medicine", "Sanitation", "IT", "Interpreter", "Reflection Room", "On-Call Bed", "All Service Request Locations");
    private Map<String, Integer> freq = new ConcurrentHashMap<>();

    private String defaultBuilding = "Faulkner";
    private int defaultFloor = 2;
    public static final String MAIN = "Main";

    @Inject
    private IDatabaseCache cache;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (App.doUpdateCacheOnLoad) {
            cache.cacheAllFromDB();
            App.doUpdateCacheOnLoad = false;
        }

        cache.cacheRequestsFromDB();

        requests = FXCollections.observableArrayList(cache.getAllRequests());
        giftRequests = FXCollections.observableArrayList(cache.getAllGiftRequests());
        intiFreq();
        updateFreq();

        heatBox.setItems(heatOptions);
        timeBox.setItems(timeOptions);
        setServiceReqHisto();
        handleAllServiceReq();

        map.setEditable(false);
        map.setHighLightColor(Color.GOLD);

        // Import all the nodes from the cache and set the current building to Faulkner
        String startB = "Faulkner";
        Building faulkner = cache.getBuilding("Faulkner");
        Building main = cache.getBuilding(MAIN);

        if (!faulkner.getNodes().isEmpty()) map.setBuilding(faulkner);
        if (!main.getNodes().isEmpty()) map.getBuildings().add(main);
        buildingChooser.getItems().addAll("Faulkner", MAIN);
        buildingChooser.getSelectionModel().select(startB);

        // Add floor buttons
        generateFloorButtons();

        setFloor(2);

        map.setZoomLevel(0.25 * App.UI_SCALE);
        map.init();

        burnAllNodes(map.getCurrentFloor().getNodes());
        burnAllEdges(map.getEdges());
    }

    private void intiFreq() {
        List<String> requests = Arrays.asList("Gift Delivery", "Security", "Maintenance", "Internal Transportation", "External Transportation", "IT", "Interpreter", "Reflection Room","On-Call Bed");

        for(String req : requests) {
            freq.put(req, 0);
        }
    }

    private void updateFreq() {
        for(ServiceRequest request : requests) {
            if (!freq.containsKey(request.getService())) {
                freq.put(request.getService(), 1);
            } else {
                freq.replace(request.getService(), freq.get(request.getService()) + 1);
            }
        }
        freq.replace("Gift Delivery", giftRequests.size());
    }

    private ArrayList<ServiceRequest> getRequestsByDate(ArrayList<ServiceRequest> reqs, String date) {
        ArrayList<ServiceRequest> timeMask = new ArrayList<>();
        switch(date) {
            case "Any time":
                break;
            case "Past hour":
                break;
            case "Past 24 hours":
                break;
            case "Past month":
                break;
            case "Past year":
                break;
        }
        return timeMask;
    }

    @FXML
    void handleCancel() {
        try {
            loaderFactory.goBack();
        } catch (Exception ex) {
            log.error("Encountered Exception.", ex);
        }
    }

    @FXML
    public void handleLive() {
    }

    @FXML
    public void handleRefresh() {
    }

    public void setServiceReqHisto() {

        XYChart.Series set = new XYChart.Series<>();
        set.setName("Type of Request");

        for (String type : freq.keySet()) {
            set.getData().add(new XYChart.Data(type, freq.get(type)));
        }

        ServiceReqHisto.getData().addAll(set);

    }

    private Map<String, Integer> typeFreq (ArrayList<ServiceRequest> reqs) {
        Map<String, Integer> frequencies = new ConcurrentHashMap<>();
        for(ServiceRequest request : reqs) {
            if(!frequencies.containsKey(request.getType())) {
                frequencies.put(request.getType(), 1);
            } else {
                frequencies.replace(request.getType(), frequencies.get(request.getType())+1);
            }
        }
        return frequencies;
    }

    private Map<String, Integer> totalGiftFreq (ArrayList<GiftDeliveryRequest> reqs) {
        Map<String, Integer> frequencies = new ConcurrentHashMap<>();
        for(GiftDeliveryRequest request : reqs) {
            Map<String, Integer> oneGift = oneGiftFreq(request.getGifts());
            for(String gift : oneGift.keySet()) {
                if (!frequencies.containsKey(gift)) {
                    frequencies.put(gift, oneGift.get(gift));
                } else {
                    frequencies.replace(gift, frequencies.get(gift) + oneGift.get(gift));
                }
            }
        }
        return frequencies;
    }

    private Map<String, Integer> oneGiftFreq (String gifts) {
        Map<String, Integer> freqs = new ConcurrentHashMap<>();

        while(gifts.length() > 1) {
            int freq = Integer.parseInt(gifts.substring(1, gifts.indexOf("x")));
            gifts = gifts.substring(gifts.indexOf("x") + 3);
            String gift;
            if(gifts.contains(",")) {
                gift = gifts.substring(0, gifts.indexOf(","));
                gifts = gifts.substring(gifts.indexOf(",") + 2);
            } else {
                gift = gifts.substring(0, gifts.indexOf("."));
                gifts = gifts.substring(gifts.indexOf("."));
            }
            freqs.put(gift, freq);
        }
        return freqs;
    }

    public void handleAllServiceReq() {
        ObservableList<PieChart.Data> allData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            allData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("Service Request Pie Chart");
        servicePieChart.setData(allData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleGiftPieChart() {
        ArrayList<GiftDeliveryRequest> giftRequests = cache.getAllGiftRequests();
        Map<String, Integer> freq = totalGiftFreq(giftRequests);

        ObservableList<PieChart.Data> giftData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            giftData.add(new PieChart.Data(type, freq.get(type)));
        }


        servicePieChart.setTitle("Gift Delivery Pie Chart");
        servicePieChart.setData(giftData);
        servicePieChart.setStartAngle(90);
    }
    @FXML
    void handleSanitationPieChart() {
        ArrayList<ServiceRequest> sanitation = cache.getAllSpecificRequest("Sanitation");
        Map<String, Integer> freq = typeFreq(sanitation);

        ObservableList<PieChart.Data> sanitationData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            sanitationData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("Sanitation Pie Chart");
        servicePieChart.setData(sanitationData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleSecurityPieChart() {

        ArrayList<ServiceRequest> security = cache.getAllSpecificRequest("Security");
        Map<String, Integer> freq = typeFreq(security);

        ObservableList<PieChart.Data> securityData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            securityData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("Security Pie Chart");
        servicePieChart.setData(securityData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleMaintenancePieChart() {

        ArrayList<ServiceRequest> maintenance = cache.getAllSpecificRequest("Maintenance");
        Map<String, Integer> freq = typeFreq(maintenance);

        ObservableList<PieChart.Data> maintenanceData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            maintenanceData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("Maintenance Pie Chart");
        servicePieChart.setData(maintenanceData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleMedicationPieChart() {

        //TODO: Add actual data
        ObservableList<PieChart.Data> medicationData = FXCollections.observableArrayList(
                new PieChart.Data("Ibuprofen", 10),
                new PieChart.Data("Advil", 10),
                new PieChart.Data("Pain Killers", 10),
                new PieChart.Data("Other", 10));

        servicePieChart.setTitle("Medication Pie Chart");
        servicePieChart.setData(medicationData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleOnCallPieChart() {

        //TODO: Add actual data
        ObservableList<PieChart.Data> onCallData = FXCollections.observableArrayList(
                new PieChart.Data("Bed 1", 10),
                new PieChart.Data("Bed 2", 10),
                new PieChart.Data("Bed 3", 10),
                new PieChart.Data("Bed 4", 10),
                new PieChart.Data("Bed 5", 10),
                new PieChart.Data("Bed 6", 10),
                new PieChart.Data("Bed 7", 10));

        servicePieChart.setTitle("On-Call Bed Pie Chart");
        servicePieChart.setData(onCallData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleReflectionPieChart() {

        //TODO: Add actual data
        ObservableList<PieChart.Data> reflectionData = FXCollections.observableArrayList(
                new PieChart.Data("Floor 1", 10),
                new PieChart.Data("Floor 3", 10),
                new PieChart.Data("Floor 4", 10));

        servicePieChart.setTitle("Reflection Room Pie Chart");
        servicePieChart.setData(reflectionData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleITPieChart() {

        ArrayList<ServiceRequest> it = cache.getAllSpecificRequest("IT");
        Map<String, Integer> freq = typeFreq(it);

        ObservableList<PieChart.Data> ITData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            ITData.add(new PieChart.Data(type, freq.get(type)));
        }
        servicePieChart.setTitle("IT Pie Chart");
        servicePieChart.setData(ITData);
        servicePieChart.setStartAngle(90);

    }

    @FXML
    void handleInternalPieChart() {

        ArrayList<ServiceRequest> internal = cache.getAllSpecificRequest("Internal Transportation");
        Map<String, Integer> freq = typeFreq(internal);

        ObservableList<PieChart.Data> internalData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            internalData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("Internal Transportation Pie Chart");
        servicePieChart.setData(internalData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleExternalPieChart() {

        ArrayList<ServiceRequest> external = cache.getAllSpecificRequest("External Transportation");
        Map<String, Integer> freq = typeFreq(external);

        ObservableList<PieChart.Data> externalData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            externalData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("External Transportation Pie Chart");
        servicePieChart.setData(externalData);
        servicePieChart.setStartAngle(90);
    }

    @FXML
    void handleInterpreterPieChart() {

        ArrayList<ServiceRequest> interpreter = cache.getAllSpecificRequest("Interpreter");
        Map<String, Integer> freq = typeFreq(interpreter);

        ObservableList<PieChart.Data> interpreterData = FXCollections.observableArrayList();
        for (String type : freq.keySet()) {
            interpreterData.add(new PieChart.Data(type, freq.get(type)));
        }

        servicePieChart.setTitle("Interpreter Pie Chart");
        servicePieChart.setData(interpreterData);
        servicePieChart.setStartAngle(90);
    }


    // -------------------------- HEAT MAP CODE ------------------------- //

    @FXML
    private void switchBuilding() {
        String selected = buildingChooser.getSelectionModel().getSelectedItem();

        Building newBuilding = cache.getBuilding(selected);
        map.setBuilding(newBuilding);

        int prevFloor = map.getFloor();
        generateFloorButtons();
        setFloor(Math.max(map.getBuilding().getMinFloor(), Math.min(prevFloor, map.getBuilding().getMaxFloor())));
        map.setZoomLevel(.25 * App.UI_SCALE);
        //if (!path.getPathNodes().isEmpty()) highLightPath();
    }

    @FXML
    private void switchHeatMap(ActionEvent event) {

    }

    private void generateFloorButtons() {
        map.generateFloorButtons(floorSelector, this::handleFloor);
    }

    @FXML
    public void handleFloor(ActionEvent event) {
        JFXButton sourceButton = (JFXButton) event.getSource();

        if (event.getSource() == floorUp && map.getFloor() < 5) {
            setFloor(map.getFloor() + 1);
        } else if (event.getSource() == floorDown) {
            setFloor(map.getFloor() - 1);
        } else {
            setFloor(Node.floorStringToInt(sourceButton.getText()));
        }
    }

    public void setFloor(int newFloor) {
        map.setFloor(Math.max(map.getBuilding().getMinFloor(), Math.min(newFloor, map.getBuilding().getMaxFloor())));

        for (javafx.scene.Node node : floorSelector.getChildren()) {
            JFXButton floorButton = (JFXButton) node;
            if (!floorButton.getText().equals(Node.floorIntToString(map.getFloor()))) {
                if (floorButton.getStyleClass().contains("selected-floor")) {
                    floorButton.getStyleClass().clear();
                    floorButton.getStyleClass().add("button");
                    floorButton.getStyleClass().add("floor-buttons");
                }
            } else {
                if (!floorButton.getStyleClass().contains("selected-floor"))
                    floorButton.getStyleClass().add("selected-floor");
            }
        }
        burnAllNodes(map.getCurrentFloor().getNodes());
        burnAllEdges(map.getEdges());
    }

    @FXML
    private void zoomIn() {
        map.setZoomLevelToPosition(map.getZoomLevel() * 1.2, new Point2D(map.getBody().getWidth() / 2, map.getBody().getHeight() / 2));
    }

    @FXML
    private void zoomOut() {
        map.setZoomLevelToPosition(map.getZoomLevel() * 0.8, new Point2D(map.getBody().getWidth() / 2, map.getBody().getHeight() / 2));
    }

    private void burnAllNodes(Collection<Node> nodes) {
        for(Node node : nodes) {
            NodeGUI nodeGUI =  map.getNodeGUI(node);
            if(node.getFreq() > 0) {
                nodeGUI.setVisible(true);
                //nodeGUI.setHighlightColor(Color.RED);
                nodeGUI.setGradientNode();
                nodeGUI.setHighlightThickness(node.getFreq());
                nodeGUI.setHighlighted(true);
            } else {
                nodeGUI.setVisible(false);
            }
        }
    }

    private void burnAllEdges(Collection<EdgeGUI> edges) {
        for(EdgeGUI edgeGUI : edges) {
            if(edgeGUI.getEdge().getFreq() > 0) {
                if(edgeGUI != null) {
                    edgeGUI.setHighlightColor(Color.RED);
                    edgeGUI.setHighlightThickness(edgeGUI.getEdge().getFreq());
                    edgeGUI.setHighlighted(true);
                }
            }
        }
    }
}