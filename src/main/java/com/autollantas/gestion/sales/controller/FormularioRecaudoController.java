package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.sales.service.SalesService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class FormularioRecaudoController {

    @Autowired private SalesService salesService;
    @Autowired private TreasuryService treasuryService;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblCliente;
    @FXML private Label lblTotalPendiente;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtValor;

    private Sale currentSale;
    private boolean saved = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        dpFechaPago.setValue(LocalDate.now());
        loadCombos();
        configureAmountInput();
    }

    private void loadCombos() {
        comboCuenta.getItems().addAll(treasuryService.findAllAccounts());
        comboCuenta.setConverter(new StringConverter<>() {
            @Override public String toString(Account a) { return a != null ? a.getName() : ""; }
            @Override public Account fromString(String s) { return null; }
        });
        comboMetodoPago.getItems().addAll("Efectivo", "Tarjeta Crédito", "Tarjeta Débito", "Transferencia", "Nequi/Daviplata", "Cheque");
    }

    private void configureAmountInput() {
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

    public void setSale(Sale sale) {
        this.currentSale = sale;
        if (sale != null) {
            lblNumeroFactura.setText(sale.getInvoiceNumber());
            lblCliente.setText(sale.getCustomer() != null ? sale.getCustomer().getName() : "N/A");

            double balance = (sale.getPendingBalance() != null) ? sale.getPendingBalance() : sale.getTotal();

            if ("PAGADA".equalsIgnoreCase(sale.getStatus())) balance = 0.0;

            lblTotalPendiente.setText(currencyFormat.format(balance));
            txtValor.setText(String.valueOf((long) balance));
        }
    }

    private double getNumericValue() {
        String text = txtValor.getText().replaceAll("[^0-9]", "");
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    @FXML
    void btnGuardarClick(ActionEvent event) {
        if (!validateForm()) return;

        try {
            double amount = getNumericValue();
            Account destinationAccount = comboCuenta.getValue();
            LocalDate paymentDate = dpFechaPago.getValue();
            String paymentMethod = comboMetodoPago.getValue();

            double currentDebt = (currentSale.getPendingBalance() != null)
                    ? currentSale.getPendingBalance()
                    : currentSale.getTotal();

            if (amount > (currentDebt + 1.0)) {
                showAlert("Monto Excedido", "El abono ($" + decimalFormat.format(amount) +
                        ") supera la deuda actual ($" + decimalFormat.format(currentDebt) + ").");
                return;
            }

            salesService.registerCollection(currentSale, destinationAccount, paymentDate, paymentMethod, amount);

            double newBalance = currentDebt - amount;
            if (newBalance < 0) newBalance = 0.0;

            saved = true;

            showSuccessDialog("¡Pago Exitoso!",
                    "Se registró el pago de " + currencyFormat.format(amount) +
                            "\nNuevo saldo pendiente: " + currencyFormat.format(newBalance));

            closeModal();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo registrar: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        if (comboCuenta.getValue() == null || comboMetodoPago.getValue() == null) {
            showAlert("Datos incompletos", "Seleccione cuenta y método de pago.");
            return false;
        }
        if (getNumericValue() <= 0) {
            showAlert("Valor inválido", "El monto debe ser mayor a 0.");
            return false;
        }
        return true;
    }

    @FXML void btnCancelarClick(ActionEvent event) { closeModal(); }

    private void closeModal() {
        Stage stage = (Stage) txtValor.getScene().getWindow();
        if (stage != null) stage.close();
    }

    public boolean isSaved() { return saved; }

    private void showAlert(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void showSuccessDialog(String titulo, String contenido) {
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
        if (content != null) content.setStyle("-fx-padding: 20px;");

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
