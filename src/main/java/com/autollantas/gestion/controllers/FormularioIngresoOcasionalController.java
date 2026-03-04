package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.IngresoOcasional;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.IngresoOcasionalRepository;
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
public class FormularioIngresoOcasionalController {

    @Autowired private IngresoOcasionalRepository ingresoRepo;
    @Autowired private CuentaRepository cuentaRepo;

    @FXML private Label lblTitulo;
    @FXML private TextField txtConcepto;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private TextField txtMonto;
    @FXML private TextArea txtObservaciones;

    private boolean guardado = false;
    private IngresoOcasional ingresoActual;
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        formatoMoneda.setMaximumFractionDigits(0);
        configurarComboCuentas();
        configurarInputMonto();

        this.ingresoActual = new IngresoOcasional();
        dpFecha.setValue(LocalDate.now());

        Platform.runLater(() -> txtConcepto.requestFocus());
    }

    private void configurarInputMonto() {
        txtMonto.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) return;
            String limpio = newVal.replaceAll("[^0-9]", "");
            if (limpio.isEmpty()) return;
            try {
                long num = Long.parseLong(limpio);
                String form = formatoMoneda.format(num);
                if (!newVal.equals(form)) {
                    txtMonto.setText(form);
                    Platform.runLater(() -> txtMonto.positionCaret(txtMonto.getText().length()));
                }
            } catch (NumberFormatException ignored) {}
        });
    }

    public void setIngreso(IngresoOcasional ingreso) {
        this.ingresoActual = ingreso;
        if (ingreso.getIdIngreso() != null) {
            lblTitulo.setText("Editar Ingreso");
        } else {
            lblTitulo.setText("Registrar Ingreso");
        }

        txtConcepto.setText(ingreso.getConceptoIngreso() != null ? ingreso.getConceptoIngreso() : "");
        txtObservaciones.setText(ingreso.getObservaciones() != null ? ingreso.getObservaciones() : "");

        if (ingreso.getMontoIngreso() != null) {
            txtMonto.setText(formatoMoneda.format(ingreso.getMontoIngreso()));
        } else {
            txtMonto.setText("");
        }

        if (ingreso.getFechaIngreso() != null) dpFecha.setValue(ingreso.getFechaIngreso());
        else dpFecha.setValue(LocalDate.now());

        if (ingreso.getCuenta() != null) comboCuenta.setValue(ingreso.getCuenta());
    }

    @FXML
    public void guardarIngreso() {
        if (!validarCampos()) return;

        try {
            boolean esNuevo = (ingresoActual.getIdIngreso() == null);

            ingresoActual.setConceptoIngreso(txtConcepto.getText());
            ingresoActual.setObservaciones(txtObservaciones.getText());
            ingresoActual.setFechaIngreso(dpFecha.getValue());
            ingresoActual.setCuenta(comboCuenta.getValue());

            String montoStr = txtMonto.getText().replaceAll("[^0-9]", "");
            double monto = montoStr.isEmpty() ? 0 : Double.parseDouble(montoStr);
            ingresoActual.setMontoIngreso(monto);

            if (esNuevo) {
                Cuenta c = comboCuenta.getValue();
                double saldo = c.getSaldoActual() != null ? c.getSaldoActual() : 0.0;
                c.setSaldoActual(saldo + monto);
                cuentaRepo.save(c);
            }

            ingresoRepo.save(ingresoActual);
            guardado = true;
            cerrarVentana();

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al guardar: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        boolean valido = true;
        String errorStyle = "-fx-border-color: #e74c3c;";
        String normalStyle = "";

        txtConcepto.setStyle(normalStyle);
        txtMonto.setStyle(normalStyle);
        comboCuenta.setStyle(normalStyle);

        if (txtConcepto.getText() == null || txtConcepto.getText().trim().isEmpty()) {
            txtConcepto.setStyle(errorStyle); valido = false;
        }
        if (txtMonto.getText() == null || txtMonto.getText().trim().isEmpty()) {
            txtMonto.setStyle(errorStyle); valido = false;
        }
        if (comboCuenta.getValue() == null) {
            comboCuenta.setStyle(errorStyle); valido = false;
        }

        if (!valido) mostrarAlerta("Datos incompletos", "Verifique los campos obligatorios");
        return valido;
    }

    private void configurarComboCuentas() {
        List<Cuenta> cuentas = cuentaRepo.findAll();
        comboCuenta.setItems(FXCollections.observableArrayList(cuentas));
        comboCuenta.setConverter(new StringConverter<Cuenta>() {
            @Override public String toString(Cuenta c) { return c != null ? c.getNombreCuenta() : ""; }
            @Override public Cuenta fromString(String s) { return null; }
        });
    }

    @FXML
    public void cerrarVentana() {
        if (txtConcepto.getScene() != null) ((Stage) txtConcepto.getScene().getWindow()).close();
    }

    public boolean isGuardado() { return guardado; }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.show();
    }
}