package edu.wpi.cs3733.d20.teamL.views.controllers.requests;

import com.google.inject.Inject;
import com.jfoenix.controls.*;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;


import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ITPaneController implements Initializable {

    @FXML
    private ImageView requestReceived;
    @FXML
    private BorderPane borderPane;
    @FXML
    private StackPane stackPane;

    ObservableList<String> options = FXCollections.observableArrayList("General Help", "Data Backup", "Hardware/Software Issues", "Cyber Attacks");

    private FXMLLoaderFactory loaderHelper = new FXMLLoaderFactory();
    private JFXAutoCompletePopup<String> autoCompletePopup = new JFXAutoCompletePopup<>();

    @Inject
    private IDatabaseService db;
    @Inject
    private IDatabaseCache dbCache;
    @Inject
    private ILoginManager loginManager;
    @FXML
    private Label confirmation;
    @FXML
    private JFXTextField locationText;
    @FXML
    private JFXTextArea notesText;
    @FXML
    private JFXComboBox<String> typeBox;

	private SearchFields searchFields;
    @FXML
    public void initialize(URL location, ResourceBundle resources) {
		searchFields = new SearchFields(dbCache.getNodeCache());
		searchFields.getFields().add(SearchFields.Field.nodeID);
		searchFields.populateSearchFields();
		autoCompletePopup.getSuggestions().addAll(searchFields.getSuggestions());

        typeBox.setPromptText("Request Type:");
        typeBox.setItems(options);
        borderPane.prefWidthProperty().bind(stackPane.widthProperty());
        borderPane.prefHeightProperty().bind(stackPane.heightProperty());
    }

    /**
     * Does autocomplete text for the destination for the service to go to
     *
     */
    @FXML
    private void autoComplete() {
        searchFields.applyAutocomplete(locationText, autoCompletePopup);
    }

    /**
     * When clicked, the UI will either show that the request is made.
     * Or that the request had failed.
     *
     */
    @FXML
    private void submitClicked() {
        String userName = loginManager.getCurrentUser().getUsername();
        String location = locationText.getText() != null ? searchFields.getNode(locationText.getText()).getID() : null;
        String type = typeBox.getValue();
        String notes = notesText.getText();

        String status = "0";
        String dateAndTime = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss").format(new Date());

        boolean validFields = true;

        if (location == null || location.length() == 0) {
            locationText.setStyle("-fx-prompt-text-fill: RED");
            validFields = false;
        } else locationText.setStyle("-fx-prompt-text-fill: GRAY");
        if (type == null || type.length() == 0) {
            typeBox.setStyle("-fx-prompt-text-fill: RED");
            validFields = false;
        } else typeBox.setStyle("-fx-text-fill: GRAY");

        int rows = 0;
        if(validFields) rows = db.executeUpdate(new SQLEntry(DBConstants.ADD_SERVICE_REQUEST,
                new ArrayList<>(Arrays.asList(null, userName, null, location, "IT", type, notes, status, dateAndTime))));

        if (rows == 0) {
            confirmation.setTextFill(Color.RED);
            confirmation.setText("Submission failed");
        } else {
            locationText.setText("");
            typeBox.setValue(null);
            notesText.setText("");
            loaderHelper.showAndFade(requestReceived);
            confirmation.setText("");
        }
        loaderHelper.showAndFade(confirmation);
    }
}
