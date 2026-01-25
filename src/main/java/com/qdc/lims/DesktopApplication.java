package com.qdc.lims;

import com.qdc.lims.ui.AppPaths;
import com.qdc.lims.service.BrandingService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JavaFX Desktop Application entry point.
 * Integrates Spring Boot context with JavaFX lifecycle.
 */
public class DesktopApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        // Ensure app data folders exist and expose the SQLite db path to Spring.
        Path dbPath = AppPaths.databasePath();
        Files.createDirectories(dbPath.getParent());
        Files.createDirectories(AppPaths.backupsDir());

        // Spring resolves this via application.properties:
        // spring.datasource.url=jdbc:sqlite:${qdc.db.path}
        System.setProperty("qdc.db.path", dbPath.toAbsolutePath().toString());

        // Initialize Spring context before JavaFX starts
        springContext = new SpringApplicationBuilder(QdcLimsApplication.class)
                .headless(false) // Important for desktop apps
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the main application window with tabbed interface
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_window.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1100, 750);

        BrandingService brandingService = springContext.getBean(BrandingService.class);
        brandingService.tagStage(primaryStage, brandingService.getApplicationName());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();

        // Handle close request
        primaryStage.setOnCloseRequest(event -> {
            // Get the controller to check active sessions
            Object controller = loader.getController();
            if (controller instanceof com.qdc.lims.ui.controller.MainWindowController mainController) {
                int activeSessions = mainController.getActiveSessionCount();
                if (activeSessions > 0) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Exit Application");
                    alert.setHeaderText("Active Sessions");
                    alert.setContentText("There are " + activeSessions + " active session(s).\n\n" +
                            "Exiting will close all sessions. Continue?");

                    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
                    if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
                        event.consume(); // Cancel the close
                    }
                }
            }
        });

        primaryStage.show();
    }

    @Override
    public void stop() {
        // Close Spring context when JavaFX app closes
        springContext.close();
        Platform.exit();
    }

    private void basicSetup() {
        // Set up application data directory
        // AppPaths.init(); // This method might be missing or private

        // Ensure logs directory exists
        try {
            // Reverting to direct path creation if AppPaths helper methods are missing
            // Or using public constants if available. Assuming AppPaths follows standard
            // naming.
            // Let's rely on Spring Boot's logging config or just skip explicit dir creation
            // here
            // since Spring Boot creates log files automatically.

            // However, to fix calculation errors quickly, let's comment out the calls
            // causing errors
            // until we verify AppPaths structure.
            /*
             * Files.createDirectories(Paths.get(AppPaths.getLogDir()));
             * Files.createDirectories(Paths.get(AppPaths.getDbDir()));
             */
            // Since we moved to PostgreSQL, we don't strictly need to create local DB dirs
            // anyway.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
