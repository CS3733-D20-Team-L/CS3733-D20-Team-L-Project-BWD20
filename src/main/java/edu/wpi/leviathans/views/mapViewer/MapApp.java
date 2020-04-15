package edu.wpi.leviathans.views.mapViewer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.wpi.leviathans.modules.DatabaseServiceProvider;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapApp extends Application {

    @Override
    public void init() {
        log.info("Starting Up");
    }

    public Stage pStage;

    Parent root;
    MapViewer controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Injector injector = Guice.createInjector(new DatabaseServiceProvider());
        pStage = primaryStage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MapViewer.fxml"));
        //fxmlLoader.setControllerFactory(injector::getInstance);

        root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        primaryStage.setTitle("Map Viewer");
        primaryStage.setScene(new Scene(root));

        primaryStage.show();

        controller.init();
    }

    @Override
    public void stop() {
        log.info("Shutting Down");
    }
}
