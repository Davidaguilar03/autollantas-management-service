package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
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

    private static final String STYLE_ERROR  = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-background-radius: 4;";
    private static final String STYLE_NORMAL = "-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;";

    private Purchase currentPurchase;
    private boolean saved = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        saved = false;
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        dpFechaPago.setValue(LocalDate.now());
        loadCombos();
        setupCurrencyInput();

        comboCuenta.valueProperty().addListener((obs, old, nw) -> { if (nw != null) comboCuenta.setStyle(STYLE_NORMAL); });
        comboCuenta.valueProperty().addListener((obs, old, nw) -> {
            if (nw != null && nw.getName() != null && nw.getName().toLowerCase().contains("caja")) {
                comboMetodoPago.setValue("Efectivo");
            }
        });
        comboMetodoPago.valueProperty().addListener((obs, old, nw) -> { if (nw != null) comboMetodoPago.setStyle(STYLE_NORMAL); });
        txtValor.textProperty().addListener((obs, old, nw) -> { if (getNumericValue() > 0) txtValor.setStyle(STYLE_NORMAL); });
    }

    private void loadCombos() {
        comboCuenta.getItems().addAll(treasuryService.findAllAccounts());
        comboCuenta.setConverter(new StringConverter<>() {
            @Override public String toString(Account a) { return a != null ? a.getName() : ""; }
            @Override public Account fromString(String s) { return null; }
        });
        comboMetodoPago.getItems().addAll("Efectivo", "Transferencia", "Tarjeta");
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

        double amount = getNumericValue();
        Account account = comboCuenta.getValue();
        LocalDate date = dpFechaPago.getValue();
        String method = comboMetodoPago.getValue();

        if (account.getCurrentBalance() < amount) {
            comboCuenta.setStyle(STYLE_ERROR);
            ToastNotification.warning(txtValor, "La cuenta no tiene saldo suficiente para este pago");
            return;
        }

        double currentDebt = (currentPurchase.getPendingBalance() != null)
                ? currentPurchase.getPendingBalance()
                : currentPurchase.getTotal();

        if (amount > (currentDebt + 1.0)) {
            txtValor.setStyle(STYLE_ERROR);
            ToastNotification.warning(txtValor,
                "El pago supera la deuda actual (" + currencyFormat.format(currentDebt) + ")");
            return;
        }

        CustomDialog.confirm(
            txtValor,
            "Confirmar pago a proveedor",
            "Vas a registrar un pago de " + currencyFormat.format(amount)
                + " a la factura " + currentPurchase.getInvoiceNumber()
                + " desde " + account.getName() + ". "
                + (amount >= currentDebt
                    ? "Con este pago la factura quedará completamente saldada."
                    : "Saldo restante: " + currencyFormat.format(currentDebt - amount) + "."),
            () -> {
                try {
                    purchasesService.registerPayment(currentPurchase, account, date, method, amount);
                    saved = true;
                    closeModal();
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastNotification.error(txtValor, "No se pudo registrar el pago");
                }
            },
            null
        );
    }

    private boolean validateForm() {
        boolean valid = true;
        if (comboCuenta.getValue() == null) { comboCuenta.setStyle(STYLE_ERROR); valid = false; }
        if (comboMetodoPago.getValue() == null) { comboMetodoPago.setStyle(STYLE_ERROR); valid = false; }
        if (getNumericValue() <= 0) { txtValor.setStyle(STYLE_ERROR); valid = false; }
        if (!valid) ToastNotification.warning(txtValor, "Completa los campos resaltados");
        return valid;
    }

    public boolean isSaved() { return saved; }

    @FXML void btnCancelarClick(ActionEvent event) { closeModal(); }

    private void closeModal() {
        Stage stage = (Stage) txtValor.getScene().getWindow();
        if (stage != null) stage.close();
    }

}
