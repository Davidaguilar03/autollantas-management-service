package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMarginsController {

    @Autowired private InventoryService inventoryService;

    @FXML private ComboBox<ProductCategory> comboCat;
    @FXML private TextField txtMargen;
    @FXML private TableView<ProductCategory> tableCategories;
    @FXML private TableColumn<ProductCategory, String> colCategoriaNombre;
    @FXML private TableColumn<ProductCategory, String> colMargen;

    private final ObservableList<ProductCategory> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        List<ProductCategory> cats = inventoryService.findAllCategories();
        comboCat.setItems(FXCollections.observableArrayList(cats));
        comboCat.setConverter(new StringConverter<>() {
            @Override public String toString(ProductCategory c) { return c != null ? c.getName() : ""; }
            @Override public ProductCategory fromString(String s) { return null; }
        });

        colCategoriaNombre.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMargen.setCellValueFactory(cell -> {
            Double m = cell.getValue().getTargetMargin();
            return new SimpleStringProperty(m != null ? String.format("%.0f%%", m * 100) : "0%");
        });

        tableCategories.setItems(data);
        cargarTabla();
    }

    private void cargarTabla() {
        data.setAll(inventoryService.findAllCategories());
    }

    @FXML
    public void guardar() {
        ProductCategory cat = comboCat.getValue();
        if (cat == null) {
            mostrarAlerta("Selecciona una categoría.");
            return;
        }
        String txt = txtMargen.getText() != null ? txtMargen.getText().trim().replace(",", ".") : "";
        if (txt.isEmpty()) {
            mostrarAlerta("Ingresa el porcentaje de utilidad.");
            return;
        }
        double margen;
        try {
            margen = Double.parseDouble(txt) / 100.0;
        } catch (NumberFormatException e) {
            mostrarAlerta("El porcentaje debe ser un número válido.");
            return;
        }

        cat.setTargetMargin(margen);
        inventoryService.saveCategory(cat);
        inventoryService.findProductsByCategory(cat)
                .forEach(p -> inventoryService.recalculateMinSalePrice(p));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Utilidad guardada");
        alert.setContentText("Margen actualizado para '" + cat.getName() + "' y precios recalculados.");
        alert.showAndWait();

        txtMargen.clear();
        comboCat.setValue(null);
        cargarTabla();
    }

    @FXML
    public void cerrarVentana() {
        if (tableCategories.getScene() != null) {
            ((Stage) tableCategories.getScene().getWindow()).close();
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Datos incompletos");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
