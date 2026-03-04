package com.autollantas.gestion;

import javafx.application.Application;
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/Login.fxml"));

        fxmlLoader.setControllerFactory(springContext::getBean);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMinWidth(1220);
        stage.setMinHeight(600);

        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/autollantas/gestion/images/Logo Amarillo Negro.png")));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar el icono: " + e.getMessage());
        }

        stage.setTitle("Autollantas A&C");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
