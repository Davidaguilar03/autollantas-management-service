package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.Pago;
import com.autollantas.gestion.repository.CompraRepository;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.PagoRepository;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class FormularioPagoController {

    @Autowired private CompraRepository compraRepo;
    @Autowired private CuentaRepository cuentaRepo;
    @Autowired private PagoRepository pagoRepo;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblProveedor;
    @FXML private Label lblTotalPendiente;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtValor;
    @FXML private Button btnGuardar;

    private Compra compraActual;
    @Getter private boolean guardado = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        dpFechaPago.setValue(LocalDate.now());
        cargarCombos();
        configurarInputMoneda();
    }

    private void cargarCombos() {
        comboCuenta.getItems().addAll(cuentaRepo.findAll());
        comboCuenta.setConverter(new StringConverter<>() {
            @Override public String toString(Cuenta c) { return c != null ? c.getNombreCuenta() : ""; }
            @Override public Cuenta fromString(String s) { return null; }
        });
        comboMetodoPago.getItems().addAll("Efectivo", "Transferencia", "Cheque", "Tarjeta", "Nequi/Daviplata");
    }

    private void configurarInputMoneda() {
        txtValor.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().isEmpty()) return change;
            if (!change.getControlNewText().matches("[0-9.,$ ]*")) return null;
            return change;
        }));

        txtValor.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) return;
            if (!newText.equals(oldText)) {
                String cleanString = newText.replaceAll("[^0-9]", "");
                if (cleanString.isEmpty()) return;
                try {
                    long parsed = Long.parseLong(cleanString);
                    String formatted = "$ " + decimalFormat.format(parsed);
                    Platform.runLater(() -> {
                        if (!txtValor.getText().equals(formatted)) {
                            txtValor.setText(formatted);
                            txtValor.positionCaret(formatted.length());
                        }
                    });
                } catch (NumberFormatException e) { }
            }
        });
    }

    public void setCompra(Compra compra) {
        this.compraActual = compra;
        if (compra != null) {
            lblNumeroFactura.setText(compra.getNumeroFacturaCompra());
            lblProveedor.setText(compra.getProveedor() != null ? compra.getProveedor().getNombreProveedor() : "N/A");

            double saldo = (compra.getSaldoPendiente() != null) ? compra.getSaldoPendiente() : compra.getTotalCompra();
            if ("PAGADA".equalsIgnoreCase(compra.getEstadoCompra())) saldo = 0.0;

            lblTotalPendiente.setText(currencyFormat.format(saldo));
            txtValor.setText(String.valueOf((long) saldo));
        }
    }

    private double obtenerValorNumerico() {
        String text = txtValor.getText().replaceAll("[^0-9]", "");
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    @FXML
    void btnGuardarClick(ActionEvent event) {
        if (!validarFormulario()) return;

        try {
            double montoPago = obtenerValorNumerico();
            Cuenta cuentaOrigen = comboCuenta.getValue();
            LocalDate fecha = dpFechaPago.getValue();
            String metodo = comboMetodoPago.getValue();

            if (cuentaOrigen.getSaldoActual() < montoPago) {
                mostrarAlerta("Fondos Insuficientes", "La cuenta no tiene saldo suficiente para este pago.");
                return;
            }

            double deudaActual = (compraActual.getSaldoPendiente() != null)
                    ? compraActual.getSaldoPendiente()
                    : compraActual.getTotalCompra();

            if (montoPago > (deudaActual + 1.0)) {
                mostrarAlerta("Monto Excedido", "El pago supera la deuda actual (" + currencyFormat.format(deudaActual) + ").");
                return;
            }

            Pago nuevoPago = new Pago();
            nuevoPago.setCompra(compraActual);
            nuevoPago.setCuenta(cuentaOrigen);
            nuevoPago.setFechaPago(fecha);
            nuevoPago.setMetodoPagoPago(metodo);
            nuevoPago.setValorPago(montoPago);

            pagoRepo.save(nuevoPago);

            double saldoCuenta = cuentaOrigen.getSaldoActual() != null ? cuentaOrigen.getSaldoActual() : 0.0;
            cuentaOrigen.setSaldoActual(saldoCuenta - montoPago);
            cuentaRepo.save(cuentaOrigen);

            double nuevoSaldo = deudaActual - montoPago;
            if (nuevoSaldo < 0) nuevoSaldo = 0.0;

            compraActual.setSaldoPendiente(nuevoSaldo);
            compraActual.setCuenta(cuentaOrigen);
            compraActual.setMedioPagoCompra(metodo);

            if (nuevoSaldo <= 0) {
                compraActual.setEstadoCompra("PAGADA");
            } else {
                compraActual.setEstadoCompra("PENDIENTE");
            }

            compraRepo.save(compraActual);

            guardado = true;
            mostrarExitoPersonalizado("¡Pago Registrado!",
                    "Egreso guardado correctamente.\nNuevo saldo deuda: " + currencyFormat.format(nuevoSaldo));

            cerrarModal();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo registrar: " + e.getMessage());
        }
    }

    private boolean validarFormulario() {
        if (comboCuenta.getValue() == null || comboMetodoPago.getValue() == null) {
            mostrarAlerta("Datos incompletos", "Seleccione cuenta y método de pago.");
            return false;
        }
        if (obtenerValorNumerico() <= 0) {
            mostrarAlerta("Valor inválido", "El monto debe ser mayor a 0.");
            return false;
        }
        return true;
    }

    @FXML void btnCancelarClick(ActionEvent event) { cerrarModal(); }

    private void cerrarModal() {
        Stage stage = (Stage) txtValor.getScene().getWindow();
        if (stage != null) stage.close();
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void mostrarExitoPersonalizado(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestión de Pagos");
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.initStyle(StageStyle.UTILITY);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #c0392b; -fx-border-width: 2px;");

        dialogPane.lookup(".header-panel").setStyle("-fx-background-color: #c0392b; -fx-padding: 15px;");
        javafx.scene.Node headerText = dialogPane.lookup(".header-panel .label");
        if (headerText != null) headerText.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Aceptar");
        okButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px; -fx-cursor: hand;");
        alert.setGraphic(null);
        alert.showAndWait();
    }
}