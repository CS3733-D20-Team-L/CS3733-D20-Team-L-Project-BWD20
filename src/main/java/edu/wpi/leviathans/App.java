package edu.wpi.leviathans;

import java.io.IOException;

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
public class App extends Application {

    @Override
    public void init() {
        log.info("Starting Up");
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
		Injector injector = Guice.createInjector(new DatabaseServiceProvider());

        Parent root = FXMLLoader.load(getClass().getResource("views/pathfinder.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        log.info("Shutting Down");
    }
}