package com.autollantas.gestion.auth.controller;

import com.autollantas.gestion.config.model.SystemConfig;
import com.autollantas.gestion.config.service.SystemConfigService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@SuppressWarnings("ALL")
@Component
public class LoginController {

    @Autowired
    private SystemConfigService configurationService;

    @Autowired
    private ApplicationContext springContext;

    @FXML private PasswordField txtPassHidden;
    @FXML private TextField txtPassVisible;
    @FXML private Button btnVerPass;
    @FXML private StackPane panelFondo;
    @FXML private ImageView imgFondo;
    @FXML private VBox loginCard;
    @FXML private VBox formBox;

    private boolean isProcesandoLogin = false;

    @FXML
    public void initialize() {
        txtPassHidden.textProperty().bindBidirectional(txtPassVisible.textProperty());

        // Imagen de fondo siempre cubre toda la pantalla
        imgFondo.fitWidthProperty().bind(panelFondo.widthProperty());
        imgFondo.fitHeightProperty().bind(panelFondo.heightProperty());

        // Tarjeta centrada responsive: entre 360px y 440px de ancho
        panelFondo.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            double cardWidth = Math.max(360, Math.min(440, w * 0.36));
            loginCard.setMaxWidth(cardWidth);
            loginCard.setPrefWidth(cardWidth);
        });

        txtPassHidden.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                intentarLogin();
            }
        });

        txtPassVisible.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                intentarLogin();
            }
        });
    }

    @FXML
    private void togglePass() {
        boolean visible = txtPassHidden.isVisible();
        txtPassHidden.setVisible(!visible);
        txtPassVisible.setVisible(visible);
        btnVerPass.setText(visible ? "🙈" : "👁");
    }

    private void intentarLogin() {
        if (isProcesandoLogin) return;
        handleLogin();
    }

    @FXML
    private void handleLogin() {
        isProcesandoLogin = true;
        if (panelFondo.getScene() != null) panelFondo.getScene().setCursor(Cursor.WAIT);

        try {
            String passwordIngresada = txtPassHidden.getText().trim();

            if (passwordIngresada.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Por favor ingresa la contraseña.");
                return;
            }

            Optional<SystemConfig> configOpt = configurationService.findByKey("admin_password");

            if (configOpt.isPresent()) {
                String passwordRealBD = configOpt.get().getValue();
                if (passwordRealBD != null && passwordRealBD.equals(passwordIngresada)) {
                    cargarMainLayout();
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR, "Acceso Denegado", "Contraseña incorrecta.");
                }
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de Sistema",
                        "No se encontró la configuración de seguridad en la base de datos.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error inesperado.");
        } finally {
            isProcesandoLogin = false;
            if (panelFondo.getScene() != null) panelFondo.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    private void cargarMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/autollantas/gestion/shared/views/MainLayout.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) panelFondo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error Crítico",
                    "No se pudo cargar el sistema: " + e.getMessage());
        }
    }

    @FXML
    private void irARecuperacion() {
        if (isProcesandoLogin) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/autollantas/gestion/auth/views/PasswordRecovery.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) panelFondo.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo cargar la pantalla de recuperación.");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String contenido) {
        if (panelFondo.getScene() != null) panelFondo.getScene().setCursor(Cursor.DEFAULT);
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
