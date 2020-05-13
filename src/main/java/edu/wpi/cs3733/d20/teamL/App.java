package edu.wpi.cs3733.d20.teamL;

import java.io.IOException;
import java.util.Timer;

import edu.wpi.cs3733.d20.teamL.util.TimerManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import lombok.extern.slf4j.Slf4j;

import edu.wpi.cs3733.d20.teamL.util.FXMLLoaderFactory;

@Slf4j
public class App extends Application {
	private final FXMLLoaderFactory loaderFactory = new FXMLLoaderFactory();
	private static final TimerManager timerManager = new TimerManager();
	public static Timer idleLogoutTimer;
	public static Timer idleCacheUpdateTimer;
	public static Timer forceCacheUpdateTimer;
	public static Timer screenSaverTimer;
	public static Stage stage;
	public static final double SCREEN_WIDTH = Screen.getPrimary().getVisualBounds().getWidth();
	public static final double SCREEN_HEIGHT = Screen.getPrimary().getVisualBounds().getHeight();
	public static boolean doUpdateCacheOnLoad = true;
	public static boolean allowCacheUpdates = true;
	public static boolean isScreenSaverActive = false;
	public static double UI_SCALE = 1.34 * (SCREEN_WIDTH / 1920);

	public static void startLogoutTimer() {
		if (idleLogoutTimer != null) {
			idleLogoutTimer.cancel();
		}
		idleLogoutTimer = timerManager.startTimer(timerManager::showLogoutDialogueIfNoInput, "showLogoutDialogueIfNoInput");
	}

	public static void startIdleTimer() {
		if (idleCacheUpdateTimer != null) {
			idleCacheUpdateTimer.cancel();
		}
		idleCacheUpdateTimer = timerManager.startTimer(timerManager::updateCacheIfNoInput, "updateCacheIfNoInput");
	}

	public static void startForceUpdateTimer() {
		if (forceCacheUpdateTimer != null) {
			forceCacheUpdateTimer.cancel();
		}
		forceCacheUpdateTimer = timerManager.startTimer(timerManager::forceUpdateCache, "forceUpdateCache");
	}

	public static void startScreenSaverTimer() {
		if (screenSaverTimer != null) {
			screenSaverTimer.cancel();
		}
		screenSaverTimer = timerManager.startTimer(timerManager::showScreensaverIfNoInput, "showScreensaverIfNoInput");
	}

	public static void stopTimers() {
		if (idleLogoutTimer != null) {
			idleLogoutTimer.cancel();
		}
		if (idleCacheUpdateTimer != null) {
			idleCacheUpdateTimer.cancel();
		}
		if (forceCacheUpdateTimer != null) {
			forceCacheUpdateTimer.cancel();
		}
		if (screenSaverTimer != null) {
			screenSaverTimer.cancel();
		}
	}

	@Override
	public void init() {
		log.info("Starting Up");
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		stage = primaryStage;
		Scene homeScene = new Scene(loaderFactory.getFXMLLoader("map_viewer/MapViewer").load());
		stage.setScene(homeScene);
		stage.setMaximized(true);
		stage.setWidth(SCREEN_WIDTH);
		stage.setHeight(SCREEN_HEIGHT);
		stage.setTitle("Team L");
		stage.show();
		timerManager.determineTimeoutPeriods();
		FXMLLoaderFactory.getHistory().push(homeScene);
//		homeScene.addEventHandler(Event.ANY, event -> {
//			startIdleTimer();
//			startForceUpdateTimer();
//			startScreenSaverTimer();
//		});
	}

	@Override
	public void stop() {
		Platform.exit();
		System.exit(0);
	}
}
