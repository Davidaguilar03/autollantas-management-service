package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.Transferencia;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.TransferenciaRepository;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Component
public class FormularioTransferenciaController {

    @Autowired private CuentaRepository cuentaRepository;
    @Autowired private TransferenciaRepository transferenciaRepository;

    @FXML private ComboBox<Cuenta> comboOrigen;
    @FXML private ComboBox<Cuenta> comboDestino;
    @FXML private TextField txtMonto;
    @FXML private TextField txtConcepto;

    private boolean guardado = false;

    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        formatoMoneda.setMaximumFractionDigits(0);
        configurarCombos();
        cargarCuentas();
        configurarInputMonto();
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

    private void configurarCombos() {
        StringConverter<Cuenta> converter = new StringConverter<>() {
            @Override public String toString(Cuenta c) { return (c == null) ? "" : c.getNombreCuenta(); }
            @Override public Cuenta fromString(String s) { return null; }
        };
        comboOrigen.setConverter(converter);
        comboDestino.setConverter(converter);
    }

    private void cargarCuentas() {
        List<Cuenta> cuentas = cuentaRepository.findAll();
        comboOrigen.getItems().setAll(cuentas);
        comboDestino.getItems().setAll(cuentas);
    }

    @FXML
    void guardarTransferencia(ActionEvent event) {
        try {
            Cuenta origen = comboOrigen.getValue();
            Cuenta destino = comboDestino.getValue();

            if (origen == null || destino == null) {
                mostrarAlerta("Error", "Seleccione cuenta origen y destino.");
                return;
            }
            if (origen.getIdCuenta().equals(destino.getIdCuenta())) {
                mostrarAlerta("Error", "La cuenta origen y destino no pueden ser la misma.");
                return;
            }

            String montoStr = txtMonto.getText().replaceAll("[^0-9]", "");

            if (montoStr.isEmpty()) {
                mostrarAlerta("Error", "Ingrese un monto válido.");
                return;
            }

            Double monto = Double.parseDouble(montoStr);

            if (monto <= 0) {
                mostrarAlerta("Error", "El monto debe ser mayor a cero.");
                return;
            }

            if (origen.getSaldoActual() < monto) {
                mostrarAlerta("Fondos insuficientes", "La cuenta " + origen.getNombreCuenta() + " no tiene saldo suficiente.");
                return;
            }

            origen.setSaldoActual(origen.getSaldoActual() - monto);
            destino.setSaldoActual(destino.getSaldoActual() + monto);

            Transferencia t = new Transferencia();
            t.setFechaTransferencia(LocalDate.now());
            t.setMontoTransferencia(monto);
            t.setCuentaOrigen(origen);
            t.setCuentaDestino(destino);

            cuentaRepository.save(origen);
            cuentaRepository.save(destino);
            transferenciaRepository.save(t);

            guardado = true;
            cerrarModal(event);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "Error al procesar transferencia: " + e.getMessage());
        }
    }

    @FXML
    void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    public boolean isGuardado() {
        return guardado;
    }
}