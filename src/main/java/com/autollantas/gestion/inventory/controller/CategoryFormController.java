package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryFormController {

    @Autowired
    private InventoryService inventoryService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private ColorPicker colorPicker;

    private ProductCategory currentCategory;
    private boolean guardado = false;

    @FXML
    public void initialize() {
        guardado = false;
        currentCategory = null;
        Platform.runLater(() -> txtNombre.requestFocus());
    }

    public void setCategory(ProductCategory cat) {
        this.currentCategory = cat;
        lblTitulo.setText("Editar Categoría");
        txtNombre.setText(cat.getName() != null ? cat.getName() : "");
        if (cat.getColor() != null) {
            colorPicker.setValue(Color.web(cat.getColor()));
        }
    }

    @FXML
    public void guardar() {
        if (!validar()) return;

        if (currentCategory != null) {
            CustomDialog.confirm(txtNombre,
                "Guardar cambios",
                "Vas a modificar la categoría \"" + currentCategory.getName() + "\". "
                    + "El nombre y color serán reemplazados. ¿Confirmas?",
                this::doSave,
                null);
        } else {
            doSave();
        }
    }

    private void doSave() {
        try {
            String nombre = txtNombre.getText().trim();
            String colorHex = colorPicker.getValue() != null
                    ? "#" + colorPicker.getValue().toString().substring(2, 8)
                    : "#4db6ac";

            if (currentCategory == null) {
                inventoryService.createCategory(nombre, colorHex);
            } else {
                currentCategory.setName(nombre);
                currentCategory.setColor(colorHex);
                inventoryService.saveCategory(currentCategory);
            }
            guardado = true;
            cerrarVentana();
        } catch (Exception e) {
            ToastNotification.error(txtNombre, "No se pudo guardar la categoría: " + e.getMessage());
        }
    }

    @FXML
    public void cancelar() {
        cerrarVentana();
    }

    private boolean validar() {
        String errorStyle = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
        String normalStyle = "-fx-border-color: #cccccc; -fx-border-radius: 4;";
        txtNombre.setStyle(normalStyle);

        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            txtNombre.setStyle(errorStyle);
            ToastNotification.warning(txtNombre, "El nombre de la categoría es requerido.");
            return false;
        }
        return true;
    }

    private void cerrarVentana() {
        if (txtNombre.getScene() != null) {
            ((Stage) txtNombre.getScene().getWindow()).close();
        }
    }

    public boolean isGuardado() {
        return guardado;
    }
}
