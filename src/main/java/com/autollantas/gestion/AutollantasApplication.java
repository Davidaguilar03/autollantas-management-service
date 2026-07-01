package com.autollantas.gestion;

import com.autollantas.gestion.shared.util.CustomDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

@SpringBootApplication
public class AutollantasApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        springContext = SpringApplication.run(AutollantasApplication.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/auth/views/Login.fxml"));

        fxmlLoader.setControllerFactory(springContext::getBean);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMinWidth(1220);
        stage.setMinHeight(550);

        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/autollantas/gestion/images/logo_amarillo_negro.png")));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar el icono: " + e.getMessage());
        }

        stage.setTitle("Autollantas A&C");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            event.consume();
            CustomDialog.danger(
                stage,
                "Cerrar aplicación",
                "¿Seguro que deseas cerrar Autollantas A&C? Asegúrate de haber guardado cualquier cambio pendiente antes de continuar.",
                () -> {
                    Platform.exit();
                    springContext.close();
                },
                null
            );
        });

        stage.show();
    }

    @Override
    public void stop() {
        if (springContext != null && springContext.isActive()) springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
