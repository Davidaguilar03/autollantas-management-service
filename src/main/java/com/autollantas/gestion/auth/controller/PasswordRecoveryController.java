package com.autollantas.gestion.auth.controller;

import com.autollantas.gestion.config.model.SystemConfig;
import com.autollantas.gestion.config.service.SystemConfigService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import java.util.Optional;

@Component
public class PasswordRecoveryController {

    @Autowired
    private SystemConfigService configurationService;

    @Autowired
    private ApplicationContext springContext;

    @FXML private StackPane rootPane;
    @FXML private ImageView imgFondoRecuperacion;
    @FXML private VBox recoveryCard;
    @FXML private VBox scrollContent;

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

    private final Map<String, String> questionKeyMap = new HashMap<>();

    @FXML
    public void initialize() {
        imgFondoRecuperacion.fitWidthProperty().bind(rootPane.widthProperty());
        imgFondoRecuperacion.fitHeightProperty().bind(rootPane.heightProperty());

        scrollContent.minHeightProperty().bind(rootPane.heightProperty());

        rootPane.widthProperty().addListener((obs, oldW, newW) -> {
            double cardWidth = Math.max(360, Math.min(440, newW.doubleValue() * 0.36));
            recoveryCard.setMaxWidth(cardWidth);
            recoveryCard.setPrefWidth(cardWidth);
        });

        txtNuevaPass.textProperty().bindBidirectional(txtNuevaPassVisible.textProperty());
        txtConfirmarPass.textProperty().bindBidirectional(txtConfirmarPassVisible.textProperty());

        loadQuestions();

        boxVerificacion.setVisible(true);
        boxVerificacion.setManaged(true);
        boxCambioPass.setVisible(false);
        boxCambioPass.setManaged(false);

        hideError();
        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
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

    private void loadQuestions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        setupQuestion("recovery_pregunta_1", "recovery_respuesta_1", options);
        setupQuestion("recovery_pregunta_2", "recovery_respuesta_2", options);
        setupQuestion("recovery_pregunta_3", "recovery_respuesta_3", options);
        cmbPreguntas.setItems(options);
    }

    private void setupQuestion(String questionKey, String answerKey, ObservableList<String> list) {
        Optional<SystemConfig> conf = configurationService.findByKey(questionKey);
        if (conf.isPresent()) {
            String questionText = conf.get().getValue();
            list.add(questionText);
            questionKeyMap.put(questionText, answerKey);
        }
    }

    @FXML
    private void verificarRespuesta() {
        hideError();
        String selectedQuestion = cmbPreguntas.getValue();
        String userAnswer = txtRespuesta.getText().trim();

        if (selectedQuestion == null || userAnswer.isEmpty()) {
            showError("Selecciona una pregunta y escribe tu respuesta.");
            return;
        }

        String answerKey = questionKeyMap.get(selectedQuestion);
        Optional<SystemConfig> storedAnswer = configurationService.findByKey(answerKey);

        if (storedAnswer.isPresent() &&
                storedAnswer.get().getValue().equalsIgnoreCase(userAnswer)) {
            activatePasswordChangeMode();
        } else {
            showError("La respuesta no coincide con nuestros registros.");
            txtRespuesta.selectAll();
            txtRespuesta.requestFocus();
        }
    }

    private void activatePasswordChangeMode() {
        boxVerificacion.setVisible(false);
        boxVerificacion.setManaged(false);

        boxCambioPass.setVisible(true);
        boxCambioPass.setManaged(true);

        hideError();

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
        hideError();
        String p1 = txtNuevaPass.getText();
        String p2 = txtConfirmarPass.getText();

        if (p1.isEmpty() || p2.isEmpty()) {
            showError("Por favor completa ambos campos.");
            return;
        }

        if (!p1.equals(p2)) {
            showError("Las contraseñas no coinciden.");
            txtConfirmarPass.clear();
            txtConfirmarPass.requestFocus();
            return;
        }

        if (p1.length() < 3) {
            showError("La contraseña es muy corta (mínimo 4 caracteres).");
            return;
        }

        Optional<SystemConfig> configPass = configurationService.findByKey("admin_password");
        if (configPass.isPresent()) {
            SystemConfig c = configPass.get();
            c.setValue(p1);
            configurationService.save(c);
            showSuccessAlert();
        } else {
            showError("Error técnico: Configuración no encontrada.");
        }
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Contraseña Actualizada");
        alert.setContentText("Tu contraseña ha sido cambiada correctamente.");
        alert.showAndWait();
        volverAlLogin();
    }

    private void showError(String message) {
        lblMensajeError.setText(message);
        lblMensajeError.setVisible(true);
        lblMensajeError.setManaged(true);
    }

    private void hideError() {
        lblMensajeError.setVisible(false);
        lblMensajeError.setManaged(false);
    }

    @FXML
    private void volverAlLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/auth/views/Login.fxml"));
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
