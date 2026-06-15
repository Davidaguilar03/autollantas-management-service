package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.shared.util.ToastNotification;
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

    private static final String STYLE_ERROR  = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
    private static final String STYLE_NORMAL = "-fx-border-color: transparent; -fx-border-width: 0;";

    private boolean saved = false;
    private OperationalExpense currentExpense;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        saved = false;
        currencyFormat.setMaximumFractionDigits(0);
        configureAccountCombo();
        configureAmountInput();

        this.currentExpense = new OperationalExpense();
        dpFecha.setValue(LocalDate.now());

        txtConcepto.textProperty().addListener((obs, old, nw) -> { if (nw != null && !nw.trim().isEmpty()) txtConcepto.setStyle(STYLE_NORMAL); });
        txtMonto.textProperty().addListener((obs, old, nw) -> { if (nw != null && !nw.replaceAll("[^0-9]", "").isEmpty()) txtMonto.setStyle(STYLE_NORMAL); });
        comboCuenta.valueProperty().addListener((obs, old, nw) -> { if (nw != null) comboCuenta.setStyle(STYLE_NORMAL); });
        dpFecha.valueProperty().addListener((obs, old, nw) -> { if (nw != null) dpFecha.setStyle(STYLE_NORMAL); });

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
                txtMonto.setStyle(STYLE_ERROR);
                ToastNotification.warning(txtConcepto, "Completa los campos resaltados");
                return;
            }

            double amount = Double.parseDouble(amountStr);
            currentExpense.setAmount(amount);

            treasuryService.saveOperationalExpense(currentExpense);

            saved = true;
            cerrarVentana();

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(txtConcepto, "Error al guardar el gasto");
        }
    }

    private boolean validateFields() {
        boolean valid = true;

        txtConcepto.setStyle(STYLE_NORMAL);
        txtMonto.setStyle(STYLE_NORMAL);
        comboCuenta.setStyle(STYLE_NORMAL);
        dpFecha.setStyle(STYLE_NORMAL);

        if (txtConcepto.getText() == null || txtConcepto.getText().trim().isEmpty()) {
            txtConcepto.setStyle(STYLE_ERROR); valid = false;
        }
        if (txtMonto.getText() == null || txtMonto.getText().replaceAll("[^0-9]", "").isEmpty()) {
            txtMonto.setStyle(STYLE_ERROR); valid = false;
        }
        if (comboCuenta.getValue() == null) {
            comboCuenta.setStyle(STYLE_ERROR); valid = false;
        }
        if (dpFecha.getValue() == null) {
            dpFecha.setStyle(STYLE_ERROR); valid = false;
        }

        if (!valid) ToastNotification.warning(txtConcepto, "Completa los campos resaltados");
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

}
