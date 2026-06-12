package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryFormController {

    @Autowired
    private InventoryService inventoryService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtStockAmarillo;
    @FXML private TextField txtStockRojo;

    private ProductCategory currentCategory;
    private boolean guardado = false;

    @FXML
    public void initialize() {
        currentCategory = null;
        Platform.runLater(() -> txtNombre.requestFocus());
    }

    public void setCategory(ProductCategory cat) {
        this.currentCategory = cat;
        lblTitulo.setText("Editar Categoría");
        txtNombre.setText(cat.getName() != null ? cat.getName() : "");
        txtStockAmarillo.setText(cat.getYellowStockMin() != null ? String.valueOf(cat.getYellowStockMin()) : "");
        txtStockRojo.setText(cat.getRedStockMin() != null ? String.valueOf(cat.getRedStockMin()) : "");
    }

    @FXML
    public void guardar() {
        if (!validar()) return;
        try {
            String nombre = txtNombre.getText().trim();
            int amarillo = Integer.parseInt(txtStockAmarillo.getText().trim());
            int rojo = Integer.parseInt(txtStockRojo.getText().trim());

            if (currentCategory == null) {
                inventoryService.createCategory(nombre, amarillo, rojo);
            } else {
                currentCategory.setName(nombre);
                currentCategory.setYellowStockMin(amarillo);
                currentCategory.setRedStockMin(rojo);
                inventoryService.saveCategory(currentCategory);
            }
            guardado = true;
            cerrarVentana();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo guardar: " + e.getMessage());
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
        txtStockAmarillo.setStyle(normalStyle);
        txtStockRojo.setStyle(normalStyle);

        boolean valido = true;

        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            txtNombre.setStyle(errorStyle);
            valido = false;
        }

        try {
            int a = Integer.parseInt(txtStockAmarillo.getText().trim());
            if (a < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            txtStockAmarillo.setStyle(errorStyle);
            valido = false;
        }

        try {
            int r = Integer.parseInt(txtStockRojo.getText().trim());
            if (r < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            txtStockRojo.setStyle(errorStyle);
            valido = false;
        }

        if (!valido) mostrarAlerta("Datos Incompletos", "Nombre requerido. Stocks deben ser números mayores o iguales a 0.");
        return valido;
    }

    private void cerrarVentana() {
        if (txtNombre.getScene() != null) {
            ((Stage) txtNombre.getScene().getWindow()).close();
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
