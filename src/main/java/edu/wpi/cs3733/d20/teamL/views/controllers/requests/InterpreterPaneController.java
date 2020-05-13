package edu.wpi.cs3733.d20.teamL.views.controllers.requests;

import com.google.inject.Inject;
import com.jfoenix.controls.JFXAutoCompletePopup;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import edu.wpi.cs3733.d20.teamL.services.db.DBConstants;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseCache;
import edu.wpi.cs3733.d20.teamL.services.db.IDatabaseService;
import edu.wpi.cs3733.d20.teamL.services.db.SQLEntry;
import edu.wpi.cs3733.d20.teamL.services.users.ILoginManager;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderFactory;
import edu.wpi.cs3733.d20.teamL.util.SearchFields;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class InterpreterPaneController implements Initializable {
    public JFXButton btnSpanish;
    public JFXButton btnFrench;
    public JFXButton btnItalian;
    public JFXButton btnASL;
    public JFXButton btnChinese;
    public StackPane stackPane;
    public BorderPane borderPane;
    public ImageView requestReceived;
    private final FXMLLoaderFactory loaderFactory = new FXMLLoaderFactory();
    private SearchFields sf;
    private JFXAutoCompletePopup<String> autoCompletePopup;
    @Inject
    private IDatabaseService db;
    @Inject
    private IDatabaseCache dbCache;
    @Inject
    private ILoginManager loginManager;
    @FXML
    private Label confirmation, interpType, languageTxt;
    @FXML
    private JFXButton btnBack, btnSubmit;
    @FXML
    private JFXTextField patientIDText, roomNumText, patientFN;
    @FXML
    private JFXTextArea additionalText;

    @FXML
    public void initialize(URL location, ResourceBundle resources) {

        sf = new SearchFields(dbCache.getNodeCache());
        sf.getFields().add(SearchFields.Field.nodeID);
//        sf.getFields().add(SearchFields.Field.shortName);
        sf.populateSearchFields();
        autoCompletePopup = new JFXAutoCompletePopup<>();
        autoCompletePopup.getSuggestions().addAll(sf.getSuggestions());
        borderPane.prefWidthProperty().bind(stackPane.widthProperty());
        borderPane.prefHeightProperty().bind(stackPane.heightProperty());
    }

    /**
     * shows autocomplete options when searching for a room
     */
    @FXML
    private void autocomplete() {
        sf.applyAutocomplete(roomNumText, autoCompletePopup);
    }

    /**
     * handles buttons "cancel" and "submit" when clicked in a interpreter service request
     * @param e tracks when button is pressed
     * @throws IOException
     */
    @FXML
    public void handleButtonAction(ActionEvent e) throws IOException {
        //goes back to staff View
        if (e.getSource() == btnBack) {
            loaderFactory.goBack();

            //sumbits request
        } else if (e.getSource() == btnSubmit){
            String interpreterType = interpType.getText();
            String patientID = patientIDText.getText();
            String roomNumber = roomNumText.getText() != null ? sf.getNode(roomNumText.getText()).getID() : null;
            String additionalInfo = additionalText.getText();
            String firstName = patientFN.getText();

            // Status codes-- 0: pending, 1: approved, 2: assigned, 3: denied
            String status = "0";
            String dateAndTime = new SimpleDateFormat("M/dd/yy | h:mm aa").format(new Date());
            String user = loginManager.getCurrentUser().getUsername();
            // Adds request info to database
            //patient_id, request_username, assignee_username, location, service, type, notes, status, date_and_time

            String concatenatedNotes = additionalInfo;

            boolean validFields = true;

            if(interpreterType == null || interpreterType.length() == 0) {
                languageTxt.setStyle("-fx-text-fill: RED");
                validFields = false;
            } else interpType.setStyle("-fx-text-fill: GRAY");
            if(db.getTableFromResultSet(db.executeQuery(new SQLEntry(DBConstants.GET_PATIENT_NAME, new ArrayList<>(Collections.singletonList(patientID))))).size() == 0) {
                log.info("Did not find patient");
                patientIDText.setStyle("-fx-prompt-text-fill: RED");
                validFields = false;
            } else patientIDText.setStyle("-fx-text-fill: GRAY");
            if(roomNumber == null || roomNumber.length() == 0) {
                roomNumText.setStyle("-fx-prompt-text-fill: RED");
                validFields = false;
            } else roomNumText.setStyle("-fx-text-fill: GRAY");

            int rows = 0;
            if(validFields) rows = db.executeUpdate((new SQLEntry(DBConstants.ADD_SERVICE_REQUEST,
                    new ArrayList<>(Arrays.asList(patientID, user, null, roomNumber, "Interpreter", interpreterType, concatenatedNotes, status, dateAndTime)))));

            if(rows == 0) {
                confirmation.setTextFill(Color.RED);
                confirmation.setText("Submission failed");
            } else {
                confirmation.setTextFill(Color.WHITE);
                confirmation.setText("");

                interpType.setText("");
                patientIDText.setText("");
                patientFN.setText("");
                roomNumText.setText("");
                additionalText.setText("");

                loaderFactory.showAndFade(requestReceived);
            }
            confirmation.setVisible(true);
            loaderFactory.showAndFade(confirmation);
        }

    }
    @FXML
    private void autoFillLanguage(ActionEvent e) {
        if(e.getSource() == btnASL){
            interpType.setText("ASL");
        }else if(e.getSource() == btnChinese){
            interpType.setText("Chinese");
        }else if(e.getSource() == btnFrench){
            interpType.setText("French");
        }else if(e.getSource() == btnItalian){
            interpType.setText("Italian");
        }else{
            interpType.setText("Spanish");
        }
    }

}
