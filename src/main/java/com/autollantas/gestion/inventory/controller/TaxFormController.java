package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaxFormController {

    @Autowired
    private InventoryService inventoryService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtName;
    @FXML private TextField txtRate;
    @FXML private TextField txtDescription;
    @FXML private RadioButton rbProduct;
    @FXML private RadioButton rbTransaction;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private TaxType currentTaxType;
    private boolean guardado = false;

    @FXML
    public void initialize() {
        guardado = false;
        rbProduct.setSelected(true);
        currentTaxType = new TaxType();
        Platform.runLater(() -> txtName.requestFocus());
    }

    public void setTaxType(TaxType tax) {
        this.currentTaxType = tax;
        lblTitulo.setText("Editar Impuesto");
        txtName.setText(tax.getName() != null ? tax.getName() : "");
        txtRate.setText(tax.getRate() != null ? String.valueOf(tax.getRate() * 100) : "");
        txtDescription.setText(tax.getDescription() != null ? tax.getDescription() : "");
        if (Boolean.TRUE.equals(tax.getAppliesToTransaction())) {
            rbTransaction.setSelected(true);
        } else {
            rbProduct.setSelected(true);
        }
    }

    @FXML
    public void guardar() {
        if (!validar()) return;

        if (currentTaxType.getId() != null) {
            CustomDialog.confirm(txtName,
                "Guardar cambios",
                "Vas a modificar el impuesto \"" + currentTaxType.getName() + "\". "
                    + "El cambio afectará todas las categorías que lo tengan asignado y sus precios podrían recalcularse. ¿Confirmas?",
                this::doSave,
                null);
        } else {
            doSave();
        }
    }

    private void doSave() {
        try {
            currentTaxType.setName(txtName.getText().trim());
            currentTaxType.setRate(Double.parseDouble(txtRate.getText().trim().replace(",", ".")) / 100.0);
            currentTaxType.setDescription(txtDescription.getText().trim());
            currentTaxType.setAppliesToTransaction(rbTransaction.isSelected());
            inventoryService.saveTaxType(currentTaxType);
            guardado = true;
            cerrarVentana();
        } catch (Exception e) {
            ToastNotification.error(txtName, "No se pudo guardar el impuesto: " + e.getMessage());
        }
    }

    @FXML
    public void cancelar() {
        cerrarVentana();
    }

    private boolean validar() {
        String errorStyle = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
        String normalStyle = "-fx-border-color: #cccccc; -fx-border-radius: 4;";
        txtName.setStyle(normalStyle);
        txtRate.setStyle(normalStyle);

        boolean valido = true;

        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            txtName.setStyle(errorStyle);
            valido = false;
        }

        String rateStr = txtRate.getText() != null ? txtRate.getText().trim().replace(",", ".") : "";
        try {
            double rate = Double.parseDouble(rateStr);
            if (rate < 0 || rate > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            txtRate.setStyle(errorStyle);
            valido = false;
        }

        if (!valido) ToastNotification.warning(txtName, "Nombre requerido. La tasa debe ser un número entre 0 y 100");
        return valido;
    }

    private void cerrarVentana() {
        if (txtName.getScene() != null) {
            ((Stage) txtName.getScene().getWindow()).close();
        }
    }

    public boolean isGuardado() {
        return guardado;
    }
}
