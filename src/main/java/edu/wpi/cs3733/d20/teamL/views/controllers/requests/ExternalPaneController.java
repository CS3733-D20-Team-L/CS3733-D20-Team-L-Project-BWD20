package edu.wpi.cs3733.d20.teamL.views.controllers.requests;

import com.google.inject.Inject;
import com.jfoenix.controls.JFXAutoCompletePopup;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTextField;
import edu.wpi.cs3733.d20.teamL.services.db.DBConstants;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseCache;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseService;
import edu.wpi.cs3733.d20.teamL.services.db.SQLEntry;
import edu.wpi.cs3733.d20.teamL.services.users.ILoginManager;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderFactory;
import edu.wpi.cs3733.d20.teamL.util.SearchFields;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


public class ExternalPaneController implements Initializable {

    @FXML
    private ImageView requestReceived;
    @FXML
    private BorderPane borderPane;
    @FXML
    private  StackPane stackPane;


    private SearchFields sf;
    private JFXAutoCompletePopup<String> autoCompletePopup;
    private FXMLLoaderFactory loaderHelper = new FXMLLoaderFactory();
    @FXML
    JFXComboBox transportSelector;
    ObservableList<String> transportOptions = FXCollections.observableArrayList("Taxi", "Bus", "Uber", "Lyft");

    @FXML
    JFXTextField patient, startLoc, endLoc, hour, minutes;
    @FXML
    Label confirmation, timeTxt;
    @Inject
    private IDatabaseService db;
    @Inject
    private IDatabaseCache dbCache;
    @Inject
    private ILoginManager manager;
    @FXML
    JFXDatePicker date;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sf = new SearchFields(dbCache.getNodeCache());
        sf.getFields().add(SearchFields.Field.nodeID);
        sf.populateWithExits();
        autoCompletePopup = new JFXAutoCompletePopup<>();
        autoCompletePopup.getSuggestions().addAll(sf.getSuggestions());

        transportSelector.setItems(transportOptions);

        hour.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (!"0123456789".contains(keyEvent.getCharacter())) {
                keyEvent.consume();
            }
        });

        minutes.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (!"0123456789".contains(keyEvent.getCharacter())) {
                keyEvent.consume();
            }
        });

        borderPane.prefWidthProperty().bind(stackPane.widthProperty());
        borderPane.prefHeightProperty().bind(stackPane.heightProperty());
    }

    @FXML
    private void autocompleteS() {
        sf.applyAutocomplete(startLoc, autoCompletePopup);
    }

    @FXML
    private boolean timeIsValid() {
        if (Integer.parseInt(hour.getText()) < 25 || (Integer.parseInt(minutes.getText()) < 60)) {
            return true;
        }
        return false;
    }

    @FXML
    private void submitClicked() {
        String start = startLoc.getText();
        String end = endLoc.getText();
        String type = (String) transportSelector.getValue();
        String dateNeeded = date.getId();
        String hourNeeded = hour.getText();
        String minNeeded = minutes.getText();
        String patientID = patient.getText();

        String status = "0";
        String dateAndTime = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss").format(new Date());
        String concatenatedNotes = end + dateNeeded + "\n" + hourNeeded + " : " + minNeeded;

        boolean validFields = true;

        if(start == null || start.equals("")) {
            startLoc.setStyle("-fx-prompt-text-fill: RED");
            validFields = false;
        } else startLoc.setStyle("-fx-prompt-text-fill: GRAY");
        if(end == null || end.equals("")) {
            endLoc.setStyle("-fx-prompt-text-fill: RED");
            validFields = false;
        } else endLoc.setStyle("-fx-prompt-text-fill: GRAY");
        if(hourNeeded == null || hourNeeded.equals("") || minNeeded == null || minNeeded.equals("")) {
            timeTxt.setStyle("-fx-text-fill: RED");
            validFields = false;
        } else timeTxt.setStyle("-fx-text-fill: GRAY");
        if(db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.GET_PATIENT_NAME, new ArrayList<>(Collections.singletonList(patientID))))).size() == 0) {
            patient.setStyle("-fx-prompt-text-fill: RED");
            validFields = false;
        } else patient.setStyle("-fx-prompt-text-fill: GRAY");
        if(type == null || type.equals("")) {
            transportSelector.setStyle("-fx-prompt-text-fill: RED");
            validFields = false;
        } else transportSelector.setStyle("-fx-prompt-text-fill: GRAY");

        int rows = 0;
        if(validFields) rows = db.executeUpdate(new SQLEntry(DBConstants.ADD_SERVICE_REQUEST, new ArrayList<>(Arrays.asList(patientID, manager.getCurrentUser().getUsername(), null, start, "External Transportation", type, concatenatedNotes, status, dateAndTime))));


        if (rows == 0) {
            confirmation.setTextFill(Color.RED);
            confirmation.setText("Request failed");
        } else {
            transportSelector.setValue(null);
            date.setId("");
            hour.setText("");
            minutes.setText("");
            patient.setText("");
            confirmation.setText("");
            loaderHelper.showAndFade(requestReceived);
            transportSelector.setValue(null);
        }

        loaderHelper.showAndFade(confirmation);
    }

    @FXML
    private void closeClicked() {loaderHelper.goBack(); }
}




