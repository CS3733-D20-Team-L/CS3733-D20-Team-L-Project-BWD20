package edu.wpi.cs3733.d20.teamL.views.controllers.requests;

import com.google.inject.Inject;
import com.jfoenix.controls.JFXAutoCompletePopup;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import edu.wpi.cs3733.d20.teamL.services.db.DBConstants;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseCache;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseService;
import edu.wpi.cs3733.d20.teamL.services.db.SQLEntry;
import edu.wpi.cs3733.d20.teamL.services.users.ILoginManager;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderHelper;
import edu.wpi.cs3733.d20.teamL.util.search.SearchFields;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;

public class MaintenancePaneController implements Initializable {
    @FXML
    private ImageView requestReceived;
    @FXML
    private BorderPane borderPane;
    @FXML
    private StackPane stackPane;

    @FXML
    private JFXComboBox urgency, type;
    @FXML
    private JFXTextField location;
    @FXML
    private JFXTextArea description;
    @FXML
    private Label error;

    private JFXAutoCompletePopup<String> autoCompletePopup = new JFXAutoCompletePopup<>();

    private FXMLLoaderHelper loaderHelper = new FXMLLoaderHelper();
    private SearchFields searchFields;

    @Inject
    private IDatabaseService dbService;
    @Inject
    private IDatabaseCache dbCache;
    @Inject
    private ILoginManager loginManager;

    private String loggedInUsername;

    @FXML
    public void initialize(URL location, ResourceBundle resources) {

        // Setup autocomplete
        searchFields = new SearchFields(dbCache.getNodeCache());
        searchFields.getFields().add(SearchFields.Field.longName);
        searchFields.getFields().add(SearchFields.Field.shortName);
        searchFields.populateSearchFields();
        autoCompletePopup.getSuggestions().addAll(searchFields.getSuggestions());
        borderPane.prefWidthProperty().bind(stackPane.widthProperty());
        borderPane.prefHeightProperty().bind(stackPane.heightProperty());

        urgency.getItems().addAll(
                "Minor",
                "Within the day",
                "Urgent"
        );

        type.getItems().addAll(
                "Plumbing",
                "Medical Equipment",
                "Electrical",
                "IT",
                "Other"
        );
    }

    @FXML
    private void submit() {
        if (fieldsFilled()) {
            String notes = urgency.getSelectionModel().getSelectedItem().toString() + "|" + description.getText();
            String dateAndTime = new SimpleDateFormat("M/dd/yy | h:mm aa").format(new Date());

            ArrayList<String> params = new ArrayList<>(Arrays.asList(null, loginManager.getCurrentUser().getUsername(), null, searchFields.getNode(location.getText()).getID(),
                    "maintenance", type.getSelectionModel().getSelectedItem().toString(), notes, "0", dateAndTime));
            int rows = dbService.executeUpdate(new SQLEntry(DBConstants.ADD_SERVICE_REQUEST, params));

            if (rows == 0) {
                error.setText("Submission failed");
                error.setTextFill(Color.RED);
                loaderHelper.showAndFade(error);
            } else {
                location.setText("");
                description.setText("");
                loaderHelper.showAndFade(requestReceived);
            }
        } else {
            error.setText("Please fill in all fields");
            error.setTextFill(Color.RED);
            loaderHelper.showAndFade(error);
        }
    }

    @FXML
    private void back() {
        loaderHelper.goBack();
    }

    @FXML
    private void autocomplete() {
        searchFields.applyAutocomplete(location, autoCompletePopup);
    }

    /**
     * Checks if all the fields of the form are filled.
     *
     * @return {true} If and only if all fields are filled {false} Otherwise
     */
    private boolean fieldsFilled() {
        return location.getText().length() > 0 && description.getText().length() > 0 && !urgency.getSelectionModel().isEmpty() && !type.getSelectionModel().isEmpty();
    }
}