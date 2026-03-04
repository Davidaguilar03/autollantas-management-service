package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.Recaudo;
import com.autollantas.gestion.model.Venta;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.RecaudoRepository;
import com.autollantas.gestion.repository.VentaRepository;
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
public class FormularioRecaudoController {

    @Autowired private VentaRepository ventaRepo;
    @Autowired private CuentaRepository cuentaRepo;
    @Autowired private RecaudoRepository recaudoRepo;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblCliente;
    @FXML private Label lblTotalPendiente;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtValor;

    private Venta ventaActual;
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
        comboMetodoPago.getItems().addAll("Efectivo", "Tarjeta Crédito", "Tarjeta Débito", "Transferencia", "Nequi/Daviplata", "Cheque");
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
                } catch (NumberFormatException e) {
                }
            }
        });
    }

    public void setVenta(Venta venta) {
        this.ventaActual = venta;
        if (venta != null) {
            lblNumeroFactura.setText(venta.getNumeroFacturaVenta());
            lblCliente.setText(venta.getCliente() != null ? venta.getCliente().getNombreCliente() : "N/A");

            double saldo = (venta.getSaldoPendiente() != null) ? venta.getSaldoPendiente() : venta.getTotalVenta();

            if ("PAGADA".equalsIgnoreCase(venta.getEstadoVenta())) saldo = 0.0;

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
            double montoAbono = obtenerValorNumerico();
            Cuenta cuentaDestino = comboCuenta.getValue();
            LocalDate fechaPago = dpFechaPago.getValue();
            String metodoPago = comboMetodoPago.getValue();

            double deudaActual = (ventaActual.getSaldoPendiente() != null)
                    ? ventaActual.getSaldoPendiente()
                    : ventaActual.getTotalVenta();

            if (montoAbono > (deudaActual + 1.0)) {
                mostrarAlerta("Monto Excedido", "El abono ($" + decimalFormat.format(montoAbono) +
                        ") supera la deuda actual ($" + decimalFormat.format(deudaActual) + ").");
                return;
            }

            Recaudo nuevoRecaudo = new Recaudo();
            nuevoRecaudo.setVenta(ventaActual);
            nuevoRecaudo.setCuenta(cuentaDestino);
            nuevoRecaudo.setFechaRecaudo(fechaPago);
            nuevoRecaudo.setMetodoPagoRecaudo(metodoPago);
            nuevoRecaudo.setValorRecaudo(montoAbono);

            recaudoRepo.save(nuevoRecaudo);

            double saldoCuenta = cuentaDestino.getSaldoActual() != null ? cuentaDestino.getSaldoActual() : 0.0;
            cuentaDestino.setSaldoActual(saldoCuenta + montoAbono);
            cuentaRepo.save(cuentaDestino);

            double nuevoSaldo = deudaActual - montoAbono;
            if (nuevoSaldo < 0) nuevoSaldo = 0.0;

            ventaActual.setSaldoPendiente(nuevoSaldo);
            ventaActual.setCuenta(cuentaDestino);
            ventaActual.setMedioPagoVenta(metodoPago);

            if (nuevoSaldo <= 0) {
                ventaActual.setEstadoVenta("PAGADA");
            } else {
                ventaActual.setEstadoVenta("PENDIENTE");
            }

            ventaRepo.save(ventaActual);

            guardado = true;

            mostrarExitoPersonalizado("¡Pago Exitoso!",
                    "Se registró el pago de " + currencyFormat.format(montoAbono) +
                            "\nNuevo saldo pendiente: " + currencyFormat.format(nuevoSaldo));

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

        dialogPane.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 14px;" +
                        "-fx-border-color: #2e7d32;" +
                        "-fx-border-width: 2px;"
        );

        dialogPane.lookup(".header-panel").setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-padding: 15px;"
        );

        javafx.scene.Node headerText = dialogPane.lookup(".header-panel .label");
        if (headerText != null) {
            headerText.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 18px;"
            );
        }

        javafx.scene.Node content = dialogPane.lookup(".content");
        if (content != null) {
            content.setStyle("-fx-padding: 20px;");
        }

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Aceptar");
        okButton.setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8px 20px;"
        );

        okButton.setOnMouseEntered(e -> okButton.setStyle(
                "-fx-background-color: #1b5e20;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8px 20px;"
        ));
        okButton.setOnMouseExited(e -> okButton.setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8px 20px;"
        ));

        alert.setGraphic(null);

        alert.showAndWait();
    }
}