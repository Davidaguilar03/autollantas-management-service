package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class CollectionFormController {

    @Autowired private SalesService salesService;
    @Autowired private TreasuryService treasuryService;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblCliente;
    @FXML private Label lblTotalPendiente;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtValor;

    private static final String STYLE_ERROR  = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-background-radius: 4;";
    private static final String STYLE_NORMAL = "-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;";

    private Sale currentSale;
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
        configureAmountInput();

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

        double amount = getNumericValue();
        double currentDebt = (currentSale.getPendingBalance() != null)
                ? currentSale.getPendingBalance()
                : currentSale.getTotal();

        if (amount > (currentDebt + 1.0)) {
            txtValor.setStyle(STYLE_ERROR);
            ToastNotification.warning(txtValor,
                "El abono ($" + decimalFormat.format(amount) +
                ") supera la deuda ($" + decimalFormat.format(currentDebt) + ")");
            return;
        }

        CustomDialog.confirm(
            txtValor,
            "Confirmar pago",
            "Vas a registrar un abono de " + currencyFormat.format(amount) +
            " a la factura " + currentSale.getInvoiceNumber() + ". " +
            (amount >= currentDebt
                ? "Con este pago la factura quedará completamente saldada."
                : "Saldo restante: " + currencyFormat.format(currentDebt - amount) + "."),
            () -> {
                try {
                    salesService.registerCollection(currentSale, comboCuenta.getValue(),
                            dpFechaPago.getValue(), comboMetodoPago.getValue(), amount);
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

    @FXML void btnCancelarClick(ActionEvent event) { closeModal(); }

    private void closeModal() {
        Stage stage = (Stage) txtValor.getScene().getWindow();
        if (stage != null) stage.close();
    }

    public boolean isSaved() { return saved; }

}
