package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.GastoOperativo;
import com.autollantas.gestion.service.TesoreriaService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Component
public class FormularioGastoOperativoController {

    @Autowired
    private TesoreriaService tesoreriaService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtConcepto;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private TextField txtMonto;
    @FXML private TextArea txtObservaciones;

    private boolean guardado = false;
    private GastoOperativo gastoActual;

    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        formatoMoneda.setMaximumFractionDigits(0);
        configurarComboCuentas();
        configurarInputMonto();

        this.gastoActual = new GastoOperativo();
        dpFecha.setValue(LocalDate.now());

        Platform.runLater(() -> txtConcepto.requestFocus());
        aplicarEstilosFocus();
    }

    private void configurarInputMonto() {
        txtMonto.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            String valorLimpio = newValue.replaceAll("[^0-9]", "");
            if (valorLimpio.isEmpty()) return;

            try {
                long numero = Long.parseLong(valorLimpio);
                String valorFormateado = formatoMoneda.format(numero);
                if (!newValue.equals(valorFormateado)) {
                    txtMonto.setText(valorFormateado);
                    Platform.runLater(() -> txtMonto.positionCaret(txtMonto.getText().length()));
                }
            } catch (NumberFormatException e) {
            }
        });
    }

    public void setGasto(GastoOperativo gasto) {
        this.gastoActual = gasto;

        if (gasto.getIdGasto() != null) {
            lblTitulo.setText("Editar Gasto");
        } else {
            lblTitulo.setText("Registrar Gasto");
        }

        txtConcepto.setText(gasto.getConceptoGasto() != null ? gasto.getConceptoGasto() : "");
        txtObservaciones.setText(gasto.getObservaciones() != null ? gasto.getObservaciones() : "");

        if (gasto.getMontoGasto() != null) {
            txtMonto.setText(formatoMoneda.format(gasto.getMontoGasto()));
        } else {
            txtMonto.setText("");
        }

        if (gasto.getFechaGasto() != null) {
            dpFecha.setValue(gasto.getFechaGasto());
        } else {
            dpFecha.setValue(LocalDate.now());
        }

        if (gasto.getCuenta() != null) {
            comboCuenta.setValue(gasto.getCuenta());
        }
    }

    @FXML
    public void guardarGasto() {
        if (!validarCampos()) return;

        try {
            gastoActual.setConceptoGasto(txtConcepto.getText());
            gastoActual.setObservaciones(txtObservaciones.getText());
            gastoActual.setFechaGasto(dpFecha.getValue());
            gastoActual.setCuenta(comboCuenta.getValue());

            String montoStr = txtMonto.getText().replaceAll("[^0-9]", "");

            if (montoStr.isEmpty()) {
                mostrarAlerta("Monto requerido", "Por favor ingresa un valor válido.");
                return;
            }

            double monto = Double.parseDouble(montoStr);
            gastoActual.setMontoGasto(monto);

            tesoreriaService.saveGastoOperativo(gastoActual);

            guardado = true;
            cerrarVentana();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al guardar", "Ocurrió un error interno: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        boolean valido = true;
        String errorStyle = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
        String normalStyle = "-fx-border-color: #cccccc; -fx-border-radius: 4;";

        txtConcepto.setStyle(normalStyle);
        txtMonto.setStyle(normalStyle);
        comboCuenta.setStyle(normalStyle);
        dpFecha.setStyle(normalStyle);

        if (txtConcepto.getText() == null || txtConcepto.getText().trim().isEmpty()) {
            txtConcepto.setStyle(errorStyle);
            valido = false;
        }

        if (txtMonto.getText() == null || txtMonto.getText().trim().isEmpty()) {
            txtMonto.setStyle(errorStyle);
            valido = false;
        }

        if (comboCuenta.getValue() == null) {
            comboCuenta.setStyle(errorStyle);
            valido = false;
        }

        if (dpFecha.getValue() == null) {
            dpFecha.setStyle(errorStyle);
            valido = false;
        }

        if (!valido) {
            mostrarAlerta("Datos Incompletos", "Por favor completa los campos obligatorios.");
        }
        return valido;
    }

    private void aplicarEstilosFocus() {
        String estiloNormal = "-fx-background-radius: 4; -fx-border-color: #cccccc; -fx-border-radius: 4;";
        String estiloFocus = "-fx-background-radius: 4; -fx-border-color: #13522d; -fx-border-radius: 4; -fx-border-width: 1.5;";

        configurarEstiloCampo(txtConcepto, estiloNormal, estiloFocus);
        configurarEstiloCampo(txtMonto, estiloNormal, estiloFocus);

        txtObservaciones.setStyle(estiloNormal);
        txtObservaciones.focusedProperty().addListener((obs, oldVal, newVal) ->
                txtObservaciones.setStyle(newVal ? estiloFocus : estiloNormal));

        comboCuenta.setStyle(estiloNormal);
        dpFecha.setStyle(estiloNormal);
    }

    private void configurarEstiloCampo(TextField campo, String normal, String focus) {
        campo.setStyle(normal);
        campo.focusedProperty().addListener((obs, oldVal, newVal) ->
                campo.setStyle(newVal ? focus : normal));
    }

    private void configurarComboCuentas() {
        List<Cuenta> cuentas = tesoreriaService.findAllCuentas();
        comboCuenta.setItems(FXCollections.observableArrayList(cuentas));
        comboCuenta.setConverter(new StringConverter<Cuenta>() {
            @Override public String toString(Cuenta c) { return c != null ? c.getNombreCuenta() : ""; }
            @Override public Cuenta fromString(String string) { return null; }
        });
    }

    @FXML
    public void cerrarVentana() {
        if (txtConcepto.getScene() != null) {
            ((Stage) txtConcepto.getScene().getWindow()).close();
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.show();
    }
}