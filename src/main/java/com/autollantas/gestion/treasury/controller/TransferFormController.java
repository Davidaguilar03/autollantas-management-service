package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.ToastNotification;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.service.TreasuryService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
public class TransferFormController {

    @Autowired private TreasuryService treasuryService;

    @FXML private ComboBox<Account> comboOrigen;
    @FXML private Label             lblDestino;
    @FXML private TextField txtMonto;
    @FXML private TextField txtConcepto;

    private boolean saved = false;
    private List<Account> allAccounts;
    private Account destinationAccount;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        configureOrigen();
        loadAccounts();
        configureAmountInput();
    }

    private void configureAmountInput() {
        txtMonto.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String clean = newValue.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) return;
            try {
                long number = Long.parseLong(clean);
                String formatted = currencyFormat.format(number);
                if (!newValue.equals(formatted)) {
                    txtMonto.setText(formatted);
                    Platform.runLater(() -> txtMonto.positionCaret(txtMonto.getText().length()));
                }
            } catch (NumberFormatException e) {
            }
        });
    }

    private void configureOrigen() {
        StringConverter<Account> converter = new StringConverter<>() {
            @Override public String toString(Account a) { return (a == null) ? "" : a.getName(); }
            @Override public Account fromString(String s) { return null; }
        };
        comboOrigen.setConverter(converter);
    }

    private void loadAccounts() {
        allAccounts = treasuryService.findAllAccounts();
        comboOrigen.getItems().setAll(allAccounts);
    }

    @FXML
    void onOrigenChanged(ActionEvent event) {
        Account origen = comboOrigen.getValue();
        if (origen == null) {
            lblDestino.setText("—");
            destinationAccount = null;
            return;
        }
        destinationAccount = allAccounts.stream()
                .filter(a -> !a.getId().equals(origen.getId()))
                .findFirst()
                .orElse(null);
        lblDestino.setText(destinationAccount != null ? destinationAccount.getName() : "—");
    }

    @FXML
    void guardarTransferencia(ActionEvent event) {
        try {
            Account source = comboOrigen.getValue();

            if (source == null || destinationAccount == null) {
                ToastNotification.warning(comboOrigen, "Selecciona la cuenta origen antes de continuar");
                return;
            }

            String amountStr = txtMonto.getText().replaceAll("[^0-9]", "");

            if (amountStr.isEmpty()) {
                ToastNotification.warning(comboOrigen, "Ingresa un monto válido para la transferencia");
                return;
            }

            Double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
                ToastNotification.warning(comboOrigen, "El monto debe ser mayor a cero");
                return;
            }

            if (source.getCurrentBalance() < amount) {
                ToastNotification.warning(comboOrigen,
                        "Fondos insuficientes en " + source.getName());
                return;
            }

            String concept = (txtConcepto.getText() != null && !txtConcepto.getText().isBlank())
                    ? txtConcepto.getText().trim() : "Transferencia";
            treasuryService.registerTransfer(source, destinationAccount, amount, LocalDate.now(), concept);

            saved = true;
            cerrarModal(event);
            ToastNotification.success(
                MainLayoutController.getInstance().getContentArea(),
                "Transferencia de " + source.getName() + " a " + destinationAccount.getName() + " registrada"
            );

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(comboOrigen, "Error al procesar la transferencia: " + e.getMessage());
        }
    }

    @FXML
    void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }
}
