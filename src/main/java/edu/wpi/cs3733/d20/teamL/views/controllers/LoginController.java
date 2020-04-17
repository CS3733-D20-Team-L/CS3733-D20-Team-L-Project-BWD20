package edu.wpi.cs3733.d20.teamL.views.controllers;
//
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML
    JFXPasswordField passwordText;
    @FXML
    JFXTextField usernameText;
    @FXML
    Button btnEnter;
    @FXML
    JFXButton btnBack;
    @FXML
    AnchorPane root;
    //private Scene scene;
    // @FXML AnchorPane body;

//    private void handleButtonAction(ActionEvent e) throws IOException {
////        Stage stage;
////        Parent root;
////
////
////       if(e.getSource() == btnBack){
////
////        }
////    }



    @FXML
    private String handleLogin(ActionEvent e) throws IOException {
        String user = usernameText.getText();
        String password = passwordText.getText();

        if (user == "Doctor" && password == "Doctor") {
            return "Doctor";
        } else if (user == "Nurse" && password == "Nurse") {
            return "Nurse";
        } else if (user == "Admin" && password == "Admin") {
            return "Admin";
        } else {
            return "Incorrect username or password";
        }
    }
}
