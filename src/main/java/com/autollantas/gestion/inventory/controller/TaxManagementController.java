package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Component
public class TaxManagementController {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ApplicationContext springContext;

    @FXML private TableView<TaxType> tableTaxTypes;
    @FXML private TableColumn<TaxType, String> colTaxName;
    @FXML private TableColumn<TaxType, String> colTaxRate;
    @FXML private TableColumn<TaxType, String> colTaxApplies;
    @FXML private TableColumn<TaxType, String> colTaxDesc;

    @FXML private Button btnEditTax;
    @FXML private Button btnDeleteTax;

    @FXML private ComboBox<ProductCategory> comboCategoryTax;
    @FXML private TableView<TaxTypeRow> tableCategoryTaxes;
    @FXML private TableColumn<TaxTypeRow, Boolean> colCatTaxCheck;
    @FXML private TableColumn<TaxTypeRow, String> colCatTaxName;
    @FXML private TableColumn<TaxTypeRow, String> colCatTaxRate;
    @FXML private TableColumn<TaxTypeRow, String> colCatTaxApplies;

    private final ObservableList<TaxType> taxList = FXCollections.observableArrayList();
    private final ObservableList<TaxTypeRow> categoryTaxRows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaTaxTypes();
        configurarTablaCategoryTaxes();
        loadTaxTypes();
        loadCategories();

        tableTaxTypes.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            boolean sel = nw != null;
            btnEditTax.setDisable(!sel);
            boolean esIva = sel && Boolean.TRUE.equals(nw.getIsVat());
            btnDeleteTax.setDisable(!sel || esIva);
        });

        comboCategoryTax.valueProperty().addListener((obs, old, nw) -> {
            if (nw != null) onCategorySelected();
        });
    }

    private void configurarTablaTaxTypes() {
        colTaxName.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getName() != null ? cell.getValue().getName() : ""));
        colTaxRate.setCellValueFactory(cell -> {
            Double rate = cell.getValue().getRate();
            return new SimpleStringProperty(rate != null ? String.format("%.0f%%", rate * 100) : "0%");
        });
        colTaxApplies.setCellValueFactory(cell -> {
            Boolean t = cell.getValue().getAppliesToTransaction();
            return new SimpleStringProperty(Boolean.TRUE.equals(t) ? "Transacción" : "Producto");
        });
        colTaxDesc.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDescription() != null ? cell.getValue().getDescription() : ""));
        tableTaxTypes.setItems(taxList);
    }

    private void configurarTablaCategoryTaxes() {
        colCatTaxCheck.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colCatTaxCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colCatTaxCheck));
        colCatTaxName.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTaxType().getName() != null ? cell.getValue().getTaxType().getName() : ""));
        colCatTaxRate.setCellValueFactory(cell -> {
            Double rate = cell.getValue().getTaxType().getRate();
            return new SimpleStringProperty(rate != null ? String.format("%.0f%%", rate * 100) : "0%");
        });
        colCatTaxApplies.setCellValueFactory(cell -> {
            Boolean t = cell.getValue().getTaxType().getAppliesToTransaction();
            return new SimpleStringProperty(Boolean.TRUE.equals(t) ? "Transacción" : "Producto");
        });
        tableCategoryTaxes.setItems(categoryTaxRows);
        tableCategoryTaxes.setEditable(true);
    }

    public void loadTaxTypes() {
        new Thread(() -> {
            List<TaxType> taxes = inventoryService.findAllTaxTypes();
            Platform.runLater(() -> taxList.setAll(taxes));
        }).start();
    }

    public void loadCategories() {
        new Thread(() -> {
            List<ProductCategory> cats = inventoryService.findAllCategories();
            Platform.runLater(() -> {
                comboCategoryTax.setConverter(new StringConverter<>() {
                    @Override public String toString(ProductCategory c) { return c != null ? c.getName() : ""; }
                    @Override public ProductCategory fromString(String s) { return null; }
                });
                comboCategoryTax.setItems(FXCollections.observableArrayList(cats));
            });
        }).start();
    }

    public void onCategorySelected() {
        ProductCategory cat = comboCategoryTax.getValue();
        if (cat == null) return;

        new Thread(() -> {
            List<TaxType> allTaxes = inventoryService.findAllTaxTypes();
            List<TaxType> assigned = cat.getTaxTypes();

            Platform.runLater(() -> {
                categoryTaxRows.clear();
                for (TaxType t : allTaxes) {
                    boolean isSelected = assigned.stream().anyMatch(a -> a.getId().equals(t.getId()));
                    categoryTaxRows.add(new TaxTypeRow(t, isSelected));
                }
            });
        }).start();
    }

    @FXML
    public void onNewTax(ActionEvent event) {
        abrirModalTax(null);
    }

    @FXML
    public void onEditTax(ActionEvent event) {
        TaxType sel = tableTaxTypes.getSelectionModel().getSelectedItem();
        if (sel != null) abrirModalTax(sel);
    }

    @FXML
    public void onDeleteTax(ActionEvent event) {
        TaxType sel = tableTaxTypes.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        CustomDialog.danger(tableTaxTypes,
            "Eliminar impuesto",
            "Vas a eliminar el impuesto \"" + sel.getName() + "\" ("
                + (sel.getRate() != null ? String.format("%.0f%%", sel.getRate() * 100) : "0%") + "). "
                + "Si está asignado a alguna categoría, la operación no podrá completarse. "
                + "Esta acción no se puede deshacer.",
            () -> {
                try {
                    inventoryService.deleteTaxType(sel);
                    loadTaxTypes();
                    ToastNotification.success(tableTaxTypes, "Impuesto \"" + sel.getName() + "\" eliminado");
                } catch (Exception e) {
                    ToastNotification.error(tableTaxTypes, "No se pudo eliminar el impuesto, puede estar en uso");
                }
            },
            null);
    }

    @FXML
    public void onSaveAssignment(ActionEvent event) {
        ProductCategory cat = comboCategoryTax.getValue();
        if (cat == null) {
            ToastNotification.warning(comboCategoryTax, "Selecciona una categoría antes de guardar la asignación");
            return;
        }

        long count = categoryTaxRows.stream().filter(TaxTypeRow::isSelected).count();

        CustomDialog.confirm(comboCategoryTax,
            "Guardar asignación de impuestos",
            "Vas a aplicar " + count + " impuesto(s) a la categoría \"" + cat.getName() + "\". "
                + "Los precios mínimo y sugerido de todos los productos de esta categoría serán recalculados automáticamente. ¿Confirmas?",
            () -> {
                List<TaxType> selectedTaxes = categoryTaxRows.stream()
                        .filter(TaxTypeRow::isSelected)
                        .map(TaxTypeRow::getTaxType)
                        .collect(Collectors.toList());

                cat.getTaxTypes().clear();
                cat.getTaxTypes().addAll(selectedTaxes);

                new Thread(() -> {
                    inventoryService.saveCategory(cat);

                    List<Product> products = inventoryService.findProductsByCategory(cat);
                    for (Product p : products) {
                        inventoryService.recalculateMinSalePrice(p);
                    }

                    Platform.runLater(() -> ToastNotification.success(comboCategoryTax,
                            "Impuestos de \"" + cat.getName() + "\" guardados · " + products.size() + " precios recalculados"));
                }).start();
            },
            null);
    }

    private void abrirModalTax(TaxType taxType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/autollantas/gestion/inventory/views/TaxForm.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            TaxFormController controller = loader.getController();
            boolean esEdicion = taxType != null;
            if (esEdicion) controller.setTaxType(taxType);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tableTaxTypes.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());

            modalStage.showAndWait();

            if (controller.isGuardado()) {
                loadTaxTypes();
                if (esEdicion) {
                    ToastNotification.success(tableTaxTypes, "Impuesto \"" + taxType.getName() + "\" actualizado");
                } else {
                    ToastNotification.success(tableTaxTypes, "Impuesto creado correctamente");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tableTaxTypes, "No se pudo abrir el formulario de impuesto");
        }
    }

    @FXML
    public void cerrarVentana() {
        if (tableTaxTypes.getScene() != null) {
            ((Stage) tableTaxTypes.getScene().getWindow()).close();
        }
    }

    public static class TaxTypeRow {
        private final TaxType taxType;
        private final SimpleBooleanProperty selected;

        public TaxTypeRow(TaxType taxType, boolean selected) {
            this.taxType = taxType;
            this.selected = new SimpleBooleanProperty(selected);
        }

        public TaxType getTaxType() { return taxType; }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
    }
}
