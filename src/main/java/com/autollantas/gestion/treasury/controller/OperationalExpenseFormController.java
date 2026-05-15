package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.OperationalExpense;
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
public class OperationalExpenseFormController {

    @Autowired
    private TreasuryService treasuryService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtConcepto;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private TextField txtMonto;
    @FXML private TextArea txtObservaciones;

    private boolean saved = false;
    private OperationalExpense currentExpense;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        configureAccountCombo();
        configureAmountInput();

        this.currentExpense = new OperationalExpense();
        dpFecha.setValue(LocalDate.now());

        Platform.runLater(() -> txtConcepto.requestFocus());
        applyFocusStyles();
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

    public void setExpense(OperationalExpense expense) {
        this.currentExpense = expense;

        if (expense.getId() != null) {
            lblTitulo.setText("Editar Gasto");
        } else {
            lblTitulo.setText("Registrar Gasto");
        }

        txtConcepto.setText(expense.getConcept() != null ? expense.getConcept() : "");
        txtObservaciones.setText(expense.getNotes() != null ? expense.getNotes() : "");

        if (expense.getAmount() != null) {
            txtMonto.setText(currencyFormat.format(expense.getAmount()));
        } else {
            txtMonto.setText("");
        }

        if (expense.getDate() != null) {
            dpFecha.setValue(expense.getDate());
        } else {
            dpFecha.setValue(LocalDate.now());
        }

        if (expense.getAccount() != null) {
            comboCuenta.setValue(expense.getAccount());
        }
    }

    @FXML
    public void guardarGasto() {
        if (!validateFields()) return;

        try {
            currentExpense.setConcept(txtConcepto.getText());
            currentExpense.setNotes(txtObservaciones.getText());
            currentExpense.setDate(dpFecha.getValue());
            currentExpense.setAccount(comboCuenta.getValue());

            String amountStr = txtMonto.getText().replaceAll("[^0-9]", "");

            if (amountStr.isEmpty()) {
                showAlert("Monto requerido", "Por favor ingresa un valor válido.");
                return;
            }

            double amount = Double.parseDouble(amountStr);
            currentExpense.setAmount(amount);

            treasuryService.saveOperationalExpense(currentExpense);

            saved = true;
            cerrarVentana();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error al guardar", "Ocurrió un error interno: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        boolean valid = true;
        String errorStyle = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
        String normalStyle = "-fx-border-color: #cccccc; -fx-border-radius: 4;";

        txtConcepto.setStyle(normalStyle);
        txtMonto.setStyle(normalStyle);
        comboCuenta.setStyle(normalStyle);
        dpFecha.setStyle(normalStyle);

        if (txtConcepto.getText() == null || txtConcepto.getText().trim().isEmpty()) {
            txtConcepto.setStyle(errorStyle);
            valid = false;
        }

        if (txtMonto.getText() == null || txtMonto.getText().trim().isEmpty()) {
            txtMonto.setStyle(errorStyle);
            valid = false;
        }

        if (comboCuenta.getValue() == null) {
            comboCuenta.setStyle(errorStyle);
            valid = false;
        }

        if (dpFecha.getValue() == null) {
            dpFecha.setStyle(errorStyle);
            valid = false;
        }

        if (!valid) {
            showAlert("Datos Incompletos", "Por favor completa los campos obligatorios.");
        }
        return valid;
    }

    private void applyFocusStyles() {
        String normal = "-fx-background-radius: 4; -fx-border-color: #cccccc; -fx-border-radius: 4;";
        String focused = "-fx-background-radius: 4; -fx-border-color: #13522d; -fx-border-radius: 4; -fx-border-width: 1.5;";

        applyFieldStyle(txtConcepto, normal, focused);
        applyFieldStyle(txtMonto, normal, focused);

        txtObservaciones.setStyle(normal);
        txtObservaciones.focusedProperty().addListener((obs, oldVal, newVal) ->
                txtObservaciones.setStyle(newVal ? focused : normal));

        comboCuenta.setStyle(normal);
        dpFecha.setStyle(normal);
    }

    private void applyFieldStyle(TextField field, String normal, String focused) {
        field.setStyle(normal);
        field.focusedProperty().addListener((obs, oldVal, newVal) ->
                field.setStyle(newVal ? focused : normal));
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
        if (txtConcepto.getScene() != null) {
            ((Stage) txtConcepto.getScene().getWindow()).close();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }
}
