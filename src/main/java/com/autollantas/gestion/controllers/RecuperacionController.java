package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Configuracion;
import com.autollantas.gestion.service.ConfiguracionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class RecuperacionController {

    @Autowired
    private ConfiguracionService configuracionService;

    @Autowired
    private ApplicationContext springContext;

    @FXML private StackPane rootPane;
    @FXML private ImageView imgLogo;

    @FXML private VBox boxVerificacion;
    @FXML private ComboBox<String> cmbPreguntas;
    @FXML private TextField txtRespuesta;
    @FXML private Label lblMensajeError;

    @FXML private VBox boxCambioPass;

    @FXML private PasswordField txtNuevaPass;
    @FXML private TextField txtNuevaPassVisible;
    @FXML private Button btnVerPass;

    @FXML private PasswordField txtConfirmarPass;
    @FXML private TextField txtConfirmarPassVisible;
    @FXML private Button btnVerConfirmPass;

    private final Map<String, String> mapaPreguntaClave = new HashMap<>();

    @FXML
    public void initialize() {
        try {
            Image logoImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/autollantas/gestion/images/Logo Negro.png")));
            imgLogo.setImage(logoImg);
        } catch (Exception ignored) { }

        txtNuevaPass.textProperty().bindBidirectional(txtNuevaPassVisible.textProperty());
        txtConfirmarPass.textProperty().bindBidirectional(txtConfirmarPassVisible.textProperty());

        cargarPreguntas();

        boxVerificacion.setVisible(true);
        boxVerificacion.setManaged(true);
        boxCambioPass.setVisible(false);
        boxCambioPass.setManaged(false);

        ocultarError();
        configurarTeclas();
    }

    private void configurarTeclas() {
        rootPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) volverAlLogin();
        });

        txtRespuesta.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) verificarRespuesta();
        });

        txtNuevaPass.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) txtConfirmarPass.requestFocus();
        });
        txtNuevaPassVisible.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) txtConfirmarPass.requestFocus();
        });

        txtConfirmarPass.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) guardarNuevaContrasena();
        });
        txtConfirmarPassVisible.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) guardarNuevaContrasena();
        });
    }

    @FXML
    private void togglePass() {
        boolean visible = txtNuevaPass.isVisible();
        if (visible) {
            txtNuevaPass.setVisible(false);
            txtNuevaPassVisible.setVisible(true);
            btnVerPass.setText("🙈");
        } else {
            txtNuevaPass.setVisible(true);
            txtNuevaPassVisible.setVisible(false);
            btnVerPass.setText("👁");
        }
    }

    @FXML
    private void toggleConfirmPass() {
        boolean visible = txtConfirmarPass.isVisible();
        if (visible) {
            txtConfirmarPass.setVisible(false);
            txtConfirmarPassVisible.setVisible(true);
            btnVerConfirmPass.setText("🙈");
        } else {
            txtConfirmarPass.setVisible(true);
            txtConfirmarPassVisible.setVisible(false);
            btnVerConfirmPass.setText("👁");
        }
    }

    private void cargarPreguntas() {
        ObservableList<String> opciones = FXCollections.observableArrayList();
        configurarPregunta("recovery_pregunta_1", "recovery_respuesta_1", opciones);
        configurarPregunta("recovery_pregunta_2", "recovery_respuesta_2", opciones);
        configurarPregunta("recovery_pregunta_3", "recovery_respuesta_3", opciones);
        cmbPreguntas.setItems(opciones);
    }

    private void configurarPregunta(String clavePregunta, String claveRespuesta, ObservableList<String> lista) {
        Optional<Configuracion> conf = configuracionService.findByClave(clavePregunta);
        if (conf.isPresent()) {
            String textoPregunta = conf.get().getValor();
            lista.add(textoPregunta);
            mapaPreguntaClave.put(textoPregunta, claveRespuesta);
        }
    }

    @FXML
    private void verificarRespuesta() {
        ocultarError();
        String preguntaSeleccionada = cmbPreguntas.getValue();
        String respuestaUsuario = txtRespuesta.getText().trim();

        if (preguntaSeleccionada == null || respuestaUsuario.isEmpty()) {
            mostrarError("Selecciona una pregunta y escribe tu respuesta.");
            return;
        }

        String claveRespuestaBD = mapaPreguntaClave.get(preguntaSeleccionada);
        Optional<Configuracion> configRespuestaReal = configuracionService.findByClave(claveRespuestaBD);

        if (configRespuestaReal.isPresent() &&
                configRespuestaReal.get().getValor().equalsIgnoreCase(respuestaUsuario)) {
            activarModoCambioContrasena();
        } else {
            mostrarError("La respuesta no coincide con nuestros registros.");
            txtRespuesta.selectAll();
            txtRespuesta.requestFocus();
        }
    }

    private void activarModoCambioContrasena() {
        boxVerificacion.setVisible(false);
        boxVerificacion.setManaged(false);

        boxCambioPass.setVisible(true);
        boxCambioPass.setManaged(true);

        ocultarError();

        txtNuevaPass.setVisible(true);
        txtNuevaPassVisible.setVisible(false);
        btnVerPass.setText("👁");

        txtConfirmarPass.setVisible(true);
        txtConfirmarPassVisible.setVisible(false);
        btnVerConfirmPass.setText("👁");

        txtNuevaPass.requestFocus();
    }

    @FXML
    private void guardarNuevaContrasena() {
        ocultarError();
        String p1 = txtNuevaPass.getText();
        String p2 = txtConfirmarPass.getText();

        if (p1.isEmpty() || p2.isEmpty()) {
            mostrarError("Por favor completa ambos campos.");
            return;
        }

        if (!p1.equals(p2)) {
            mostrarError("Las contraseñas no coinciden.");
            txtConfirmarPass.clear();
            txtConfirmarPass.requestFocus();
            return;
        }

        if (p1.length() < 3) {
            mostrarError("La contraseña es muy corta (mínimo 4 caracteres).");
            return;
        }

        Optional<Configuracion> configPass = configuracionService.findByClave("admin_password");
        if (configPass.isPresent()) {
            Configuracion c = configPass.get();
            c.setValor(p1);
            configuracionService.guardar(c);
            mostrarAlertaExito();
        } else {
            mostrarError("Error técnico: Configuración no encontrada.");
        }
    }

    private void mostrarAlertaExito() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Contraseña Actualizada");
        alert.setContentText("Tu contraseña ha sido cambiada correctamente.");
        alert.showAndWait();
        volverAlLogin();
    }

    private void mostrarError(String mensaje) {
        lblMensajeError.setText(mensaje);
        lblMensajeError.setVisible(true);
        lblMensajeError.setManaged(true);
    }

    private void ocultarError() {
        lblMensajeError.setVisible(false);
        lblMensajeError.setManaged(false);
    }

    @FXML
    private void volverAlLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/Login.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}