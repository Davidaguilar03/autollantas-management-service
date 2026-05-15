package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.service.TreasuryService;
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
public class OccasionalIncomeFormController {

    @Autowired private TreasuryService treasuryService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtConcepto;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private TextField txtMonto;
    @FXML private TextArea txtObservaciones;

    private boolean saved = false;
    private OccasionalIncome currentIncome;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        configureAccountCombo();
        configureAmountInput();

        this.currentIncome = new OccasionalIncome();
        dpFecha.setValue(LocalDate.now());

        Platform.runLater(() -> txtConcepto.requestFocus());
    }

    private void configureAmountInput() {
        txtMonto.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) return;
            String clean = newVal.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) return;
            try {
                long num = Long.parseLong(clean);
                String formatted = currencyFormat.format(num);
                if (!newVal.equals(formatted)) {
                    txtMonto.setText(formatted);
                    Platform.runLater(() -> txtMonto.positionCaret(txtMonto.getText().length()));
                }
            } catch (NumberFormatException ignored) {}
        });
    }

    public void setIncome(OccasionalIncome income) {
        this.currentIncome = income;
        if (income.getId() != null) {
            lblTitulo.setText("Editar Ingreso");
        } else {
            lblTitulo.setText("Registrar Ingreso");
        }

        txtConcepto.setText(income.getConcept() != null ? income.getConcept() : "");
        txtObservaciones.setText(income.getNotes() != null ? income.getNotes() : "");

        if (income.getAmount() != null) {
            txtMonto.setText(currencyFormat.format(income.getAmount()));
        } else {
            txtMonto.setText("");
        }

        if (income.getDate() != null) dpFecha.setValue(income.getDate());
        else dpFecha.setValue(LocalDate.now());

        if (income.getAccount() != null) comboCuenta.setValue(income.getAccount());
    }

    @FXML
    public void guardarIngreso() {
        if (!validateFields()) return;

        try {
            boolean isNew = (currentIncome.getId() == null);

            currentIncome.setConcept(txtConcepto.getText());
            currentIncome.setNotes(txtObservaciones.getText());
            currentIncome.setDate(dpFecha.getValue());
            currentIncome.setAccount(comboCuenta.getValue());

            String amountStr = txtMonto.getText().replaceAll("[^0-9]", "");
            double amount = amountStr.isEmpty() ? 0 : Double.parseDouble(amountStr);
            currentIncome.setAmount(amount);

            treasuryService.saveOccasionalIncome(currentIncome, isNew);
            saved = true;
            cerrarVentana();

        } catch (Exception e) {
            showAlert("Error", "Error al guardar: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        boolean valid = true;
        String errorStyle = "-fx-border-color: #e74c3c;";
        String normalStyle = "";

        txtConcepto.setStyle(normalStyle);
        txtMonto.setStyle(normalStyle);
        comboCuenta.setStyle(normalStyle);

        if (txtConcepto.getText() == null || txtConcepto.getText().trim().isEmpty()) {
            txtConcepto.setStyle(errorStyle); valid = false;
        }
        if (txtMonto.getText() == null || txtMonto.getText().trim().isEmpty()) {
            txtMonto.setStyle(errorStyle); valid = false;
        }
        if (comboCuenta.getValue() == null) {
            comboCuenta.setStyle(errorStyle); valid = false;
        }

        if (!valid) showAlert("Datos incompletos", "Verifique los campos obligatorios");
        return valid;
    }

    private void configureAccountCombo() {
        List<Account> accounts = treasuryService.findAllAccounts();
        comboCuenta.setItems(FXCollections.observableArrayList(accounts));
        comboCuenta.setConverter(new StringConverter<Account>() {
            @Override public String toString(Account a) { return a != null ? a.getName() : ""; }
            @Override public Account fromString(String s) { return null; }
        });
    }

    @FXML
    public void cerrarVentana() {
        if (txtConcepto.getScene() != null) ((Stage) txtConcepto.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }
}
