package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.treasury.service.TreasuryService;
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
public class PaymentFormController {

    @Autowired private PurchasesService purchasesService;
    @Autowired private TreasuryService treasuryService;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblProveedor;
    @FXML private Label lblTotalPendiente;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtValor;
    @FXML private Button btnGuardar;

    private Purchase currentPurchase;
    @Getter private boolean saved = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        dpFechaPago.setValue(LocalDate.now());
        loadCombos();
        setupCurrencyInput();
    }

    private void loadCombos() {
        comboCuenta.getItems().addAll(treasuryService.findAllAccounts());
        comboCuenta.setConverter(new StringConverter<>() {
            @Override public String toString(Account a) { return a != null ? a.getName() : ""; }
            @Override public Account fromString(String s) { return null; }
        });
        comboMetodoPago.getItems().addAll("Efectivo", "Transferencia", "Cheque", "Tarjeta", "Nequi/Daviplata");
    }

    private void setupCurrencyInput() {
        txtValor.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().isEmpty()) return change;
            if (!change.getControlNewText().matches("[0-9.,$ ]*")) return null;
            return change;
        }));

        txtValor.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) return;
            if (!newText.equals(oldText)) {
                String clean = newText.replaceAll("[^0-9]", "");
                if (clean.isEmpty()) return;
                try {
                    long parsed = Long.parseLong(clean);
                    String formatted = "$ " + decimalFormat.format(parsed);
                    Platform.runLater(() -> {
                        if (!txtValor.getText().equals(formatted)) {
                            txtValor.setText(formatted);
                            txtValor.positionCaret(formatted.length());
                        }
                    });
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    public void setPurchase(Purchase purchase) {
        this.currentPurchase = purchase;
        if (purchase != null) {
            lblNumeroFactura.setText(purchase.getInvoiceNumber());
            lblProveedor.setText(purchase.getSupplier() != null ? purchase.getSupplier().getName() : "N/A");

            double balance = (purchase.getPendingBalance() != null)
                    ? purchase.getPendingBalance() : purchase.getTotal();
            if ("PAGADA".equalsIgnoreCase(purchase.getStatus())) balance = 0.0;

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
            Account account = comboCuenta.getValue();
            LocalDate date = dpFechaPago.getValue();
            String method = comboMetodoPago.getValue();

            if (account.getCurrentBalance() < amount) {
                showAlert("Fondos Insuficientes", "La cuenta no tiene saldo suficiente para este pago.");
                return;
            }

            double currentDebt = (currentPurchase.getPendingBalance() != null)
                    ? currentPurchase.getPendingBalance()
                    : currentPurchase.getTotal();

            if (amount > (currentDebt + 1.0)) {
                showAlert("Monto Excedido", "El pago supera la deuda actual (" + currencyFormat.format(currentDebt) + ").");
                return;
            }

            purchasesService.registerPayment(currentPurchase, account, date, method, amount);

            double newBalance = currentDebt - amount;
            if (newBalance < 0) newBalance = 0.0;

            saved = true;
            showSuccessAlert("¡Pago Registrado!",
                    "Egreso guardado correctamente.\nNuevo saldo deuda: " + currencyFormat.format(newBalance));
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestión de Pagos");
        alert.setHeaderText(title);
        alert.setContentText(content);
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
