package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.service.TreasuryService;
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
public class TransferFormController {

    @Autowired private TreasuryService treasuryService;

    @FXML private ComboBox<Account> comboOrigen;
    @FXML private ComboBox<Account> comboDestino;
    @FXML private TextField txtMonto;
    @FXML private TextField txtConcepto;

    private boolean saved = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        configureCombos();
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

    private void configureCombos() {
        StringConverter<Account> converter = new StringConverter<>() {
            @Override public String toString(Account a) { return (a == null) ? "" : a.getName(); }
            @Override public Account fromString(String s) { return null; }
        };
        comboOrigen.setConverter(converter);
        comboDestino.setConverter(converter);
    }

    private void loadAccounts() {
        List<Account> accounts = treasuryService.findAllAccounts();
        comboOrigen.getItems().setAll(accounts);
        comboDestino.getItems().setAll(accounts);
    }

    @FXML
    void guardarTransferencia(ActionEvent event) {
        try {
            Account source = comboOrigen.getValue();
            Account destination = comboDestino.getValue();

            if (source == null || destination == null) {
                showAlert("Error", "Seleccione cuenta origen y destino.");
                return;
            }
            if (source.getId().equals(destination.getId())) {
                showAlert("Error", "La cuenta origen y destino no pueden ser la misma.");
                return;
            }

            String amountStr = txtMonto.getText().replaceAll("[^0-9]", "");

            if (amountStr.isEmpty()) {
                showAlert("Error", "Ingrese un monto válido.");
                return;
            }

            Double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
                showAlert("Error", "El monto debe ser mayor a cero.");
                return;
            }

            if (source.getCurrentBalance() < amount) {
                showAlert("Fondos insuficientes", "La cuenta " + source.getName() + " no tiene saldo suficiente.");
                return;
            }

            treasuryService.registerTransfer(source, destination, amount, LocalDate.now());

            saved = true;
            cerrarModal(event);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error al procesar transferencia: " + e.getMessage());
        }
    }

    @FXML
    void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(header);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public boolean isSaved() {
        return saved;
    }
}
