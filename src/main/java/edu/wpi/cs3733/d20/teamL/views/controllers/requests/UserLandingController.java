package edu.wpi.cs3733.d20.teamL.views.controllers.requests;

import com.jfoenix.controls.JFXButton;
//import edu.wpi.cs3733.c20.teamR.AppointmentRequest;
//import edu.wpi.cs3733.d20.teamE.onCallBeds;
import edu.wpi.cs3733.d20.teamL.services.users.ILoginManager;
import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderFactory;
//import edu.wpi.cs3733.d20.teamM.AudioVisualRequest;
//import edu.wpi.cs3733.d20.teamM.ServiceException;
//import edu.wpi.cs3733.d20.teamP.APIController;
//import flowerapi.FlowerAPI;
//import javafx.application.Platform;
import edu.wpi.cs3733.d20.teamL.util.TimerManager;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;

@Slf4j
public class UserLandingController {

    public ImageView btnClose;
    public JFXButton btnAddPatient;

    private FXMLLoaderFactory loaderHelper = new FXMLLoaderFactory();
    private static final TimerManager timerManager = new TimerManager();

    @FXML
    private Label timeLabel, requestLabel, userLabel;
    @FXML
    private Pane servicePane;
    @FXML
    private JFXButton btnGift, btnSecurity, btnSanitation, btnMaintenance, btnIT, btnInternal, btnExternal, btnInterpreter,
            btnMedication, btnReflectionRoom, btnOnCallBed, btnLaunchAPI, btnConferenceRoom;
    @Inject
    ILoginManager login;

    @FXML
    public void initialize() throws IOException {

        timerManager.startTimer(() -> timerManager.updateTime(timeLabel), 0, 1000);
        userLabel.setText("Hello, " + login.getCurrentUser().getFName());

        launchDefaultPane();

        if (login.getCurrentUser().getAcctType().equals("1")) {
            btnAddPatient.setVisible(true);
        }
    }

    @FXML
    public void launchGiftAPI() throws IOException/*, edu.wpi.cs3733.d20.teamP.ServiceException*/ {
       //APIController.run(0, 0, 1280, 720, "edu/wpi/cs3733/d20/teamL/css/GlobalStyleSheet.css", new String(), new String());
    }

    @FXML
    public void launchMaintenanceAPI() throws IOException/*, ServiceException*/ {
        //AudioVisualRequest.run(0, 0, 1280, 720, "edu/wpi/cs3733/d20/teamL/css/GlobalStyleSheet.css", new String(), new String());

    }


    @FXML
    public void launchExternalAPI() throws IOException/*, flowerapi.ServiceException*/ {
		//FlowerAPI.run(0, 0, 1280, 720, "edu/wpi/cs3733/d20/teamL/css/GlobalStyleSheet.css", new String(), new String());
    }

    @FXML
    public void launchDefaultPane() throws IOException{
        resetAndLoadPane("DefaultPane", " ");
        requestLabel.setVisible(false);
        btnClose.setVisible(false);
    }

    @FXML
    public void launchSecurityPane() throws IOException{
        resetAndLoadPane("SecurityPane", "Security Request");
        btnSecurity.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchSanitationPane() throws IOException {
        resetAndLoadPane("SanitationPane", "Sanitation Request");
        btnSanitation.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchGiftPane() throws IOException {
        resetAndLoadPane("GiftCartPane", "Gift Request");
        btnGift.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchMaintenancePane() throws IOException {
        resetAndLoadPane("MaintenancePane", "Maintenance Request");
        btnMaintenance.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchMedicationPane() throws IOException {
        resetAndLoadPane("MedicationPane", "Medication Request");
        btnMedication.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchInternalPane() throws IOException {
        resetAndLoadPane("InternalPane", "Internal Transport");
        btnInternal.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchExternalPane() throws IOException {
        resetAndLoadPane("ExternalPane", "External Transport");
        btnExternal.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchITPane() throws IOException {
        resetAndLoadPane("ITPane", "IT Request");
        btnIT.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    public void launchInterpreterPane() throws IOException {
        resetAndLoadPane("InterpreterPane", "Interpreter Request");
        btnInterpreter.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    private void launchReflectionRoomPane() throws IOException {
        resetAndLoadPane("ReflectionRoomPane", "Reserve Reflection Room");
        btnReflectionRoom.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    private void launchOnCallBedPane() throws IOException {
        resetAndLoadPane("OnCallBedPane", "Reserve On-Call Bed");
        btnOnCallBed.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    private void launchAddPatient() throws IOException {
        resetAndLoadPane("AddPatient", "Add Patient");
        btnAddPatient.setStyle("-fx-background-color: #DCDCDC");
    }

    @FXML
    private void launchConferenceRoom() throws IOException {
        resetAndLoadPane("ConferenceRoomPane", "Reserve Conference Room");
        btnConferenceRoom.setStyle("-fx-background-color: #DCDCDC");
    }

    private void resetAndLoadPane(String regionName, String labelText) throws IOException {
        JFXButton[] allButtons = new JFXButton[] {
        		btnGift, btnSecurity, btnSanitation, btnMaintenance, btnIT, btnInternal, btnExternal, btnInterpreter, btnMedication, btnOnCallBed, btnReflectionRoom, btnAddPatient, btnConferenceRoom
        };
        for (JFXButton currButton : allButtons) {
            currButton.setStyle("-fx-background-color: white;");
        }

        try {
            servicePane.getChildren().clear();
            Region n = loaderHelper.getFXMLLoader("requests/" + regionName).load();

            servicePane.getChildren().add(n);
            n.prefWidthProperty().bind(servicePane.widthProperty());
            n.prefHeightProperty().bind(servicePane.heightProperty());
        } catch (IOException ex) {
            log.error("Encountered IOException", ex);
            launchDefaultPane();
        }

        requestLabel.setText(labelText);
        requestLabel.setVisible(true);

        btnClose.setVisible(true);
    }

    @FXML
    public void launchNotifPage() {
        try {
            Parent root = loaderHelper.getFXMLLoader("staff/NotificationsPage").load();
            loaderHelper.setupScene(new Scene(root));
        } catch (IOException ex) {
            log.error("Encountered IOException", ex);
        }
    }

    public void logoutBtn() {
        login.logOut(true);
        try {
            Parent root = loaderHelper.getFXMLLoader("map_viewer/MapViewer").load();
            loaderHelper.setupScene(new Scene(root));
        } catch (IOException ex) {
            log.error("Encountered IOException", ex);
        }
    }

}


