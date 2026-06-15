package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
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
            ToastNotification.warning(comboCat, "Selecciona una categoría antes de continuar");
            return;
        }
        String txt = txtMargen.getText() != null ? txtMargen.getText().trim().replace(",", ".") : "";
        if (txt.isEmpty()) {
            ToastNotification.warning(txtMargen, "Ingresa el porcentaje de utilidad");
            return;
        }
        double margen;
        try {
            margen = Double.parseDouble(txt) / 100.0;
        } catch (NumberFormatException e) {
            ToastNotification.warning(txtMargen, "El porcentaje debe ser un número válido");
            return;
        }

        final double margenFinal = margen;
        CustomDialog.confirm(comboCat,
            "Guardar utilidad",
            "Vas a establecer una utilidad del " + txt + "% para la categoría \"" + cat.getName() + "\". "
                + "El precio sugerido de todos los productos de esta categoría será recalculado automáticamente. ¿Confirmas?",
            () -> {
                cat.setTargetMargin(margenFinal);
                inventoryService.saveCategory(cat);
                inventoryService.findProductsByCategory(cat)
                        .forEach(p -> inventoryService.recalculateMinSalePrice(p));
                ToastNotification.success(comboCat, "Utilidad de \"" + cat.getName() + "\" actualizada y precios recalculados");
                txtMargen.clear();
                comboCat.setValue(null);
                cargarTabla();
            },
            null);
    }

    @FXML
    public void cerrarVentana() {
        if (tableCategories.getScene() != null) {
            ((Stage) tableCategories.getScene().getWindow()).close();
        }
    }
}
