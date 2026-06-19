package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import com.autollantas.gestion.inventory.model.Product;
import java.util.List;

@SuppressWarnings("ALL")
@Component
public class TaxManagementController implements com.autollantas.gestion.shared.util.ShortcutAware {

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

    @FXML private TableView<CategoryAssignRow> tableCategoryAssignment;
    @FXML private TableColumn<CategoryAssignRow, Boolean> colAssignCheck;
    @FXML private TableColumn<CategoryAssignRow, String> colAssignCatName;
    @FXML private Button btnSelectAll;
    @FXML private Button btnDeselectAll;

    @FXML private ComboBox<ProductCategory> comboCat;
    @FXML private TextField txtMargen;
    @FXML private TableView<ProductCategory> tableCategories;
    @FXML private TableColumn<ProductCategory, String> colCategoriaNombre;
    @FXML private TableColumn<ProductCategory, String> colMargen;

    private final ObservableList<TaxType> taxList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaTaxTypes();
        configurarTablaCategoryAssignment();
        loadTaxTypes();

        tableTaxTypes.getSelectionModel().selectedItemProperty().addListener((obs, old, tax) -> {
            boolean sel = tax != null;
            btnEditTax.setDisable(!sel);
            boolean esIva = sel && Boolean.TRUE.equals(tax.getIsVat());
            btnDeleteTax.setDisable(!sel || esIva);

            if (tax != null) loadCategoryAssignments(tax);
        });

        configurarTablaMargen();
        cargarTablaMargen();
        cargarComboCat();
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

    private void configurarTablaCategoryAssignment() {
        colAssignCheck.setCellValueFactory(c -> c.getValue().assignedProperty());
        colAssignCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colAssignCheck));
        colAssignCatName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tableCategoryAssignment.setEditable(true);
    }

    private void loadTaxTypes() {
        new Thread(() -> {
            List<TaxType> taxes = inventoryService.findAllTaxTypes();
            Platform.runLater(() -> taxList.setAll(taxes));
        }).start();
    }

    private void loadCategoryAssignments(TaxType tax) {
        new Thread(() -> {
            List<ProductCategory> allCats = inventoryService.findAllCategories();
            ObservableList<CategoryAssignRow> rows = FXCollections.observableArrayList();
            for (ProductCategory cat : allCats) {
                boolean has = cat.getTaxTypes().stream()
                        .anyMatch(t -> t.getId().equals(tax.getId()));
                rows.add(new CategoryAssignRow(cat, has));
            }
            Platform.runLater(() -> tableCategoryAssignment.setItems(rows));
        }).start();
    }

    @FXML
    public void onSelectAll(ActionEvent event) {
        tableCategoryAssignment.getItems().forEach(r -> r.setAssigned(true));
    }

    @FXML
    public void onDeselectAll(ActionEvent event) {
        tableCategoryAssignment.getItems().forEach(r -> r.setAssigned(false));
    }

    // --- Atajos de teclado -------------------------------------------------
    @Override public void shortcutNuevo()    { onNewTax(null); }
    @Override public void shortcutEditar()   { onEditTax(null); }
    @Override public void shortcutEliminar() { onDeleteTax(null); }
    @Override public void shortcutRefrescar(){ loadTaxTypes(); }

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
        TaxType tax = tableTaxTypes.getSelectionModel().getSelectedItem();
        if (tax == null) {
            ToastNotification.warning(tableTaxTypes, "Selecciona un impuesto antes de guardar la asignación");
            return;
        }

        ObservableList<CategoryAssignRow> rows = tableCategoryAssignment.getItems();
        if (rows.isEmpty()) {
            ToastNotification.warning(tableTaxTypes, "No hay categorías disponibles");
            return;
        }

        long count = rows.stream().filter(CategoryAssignRow::isAssigned).count();

        CustomDialog.confirm(tableTaxTypes,
            "Guardar asignación de impuestos",
            "Vas a aplicar \"" + tax.getName() + "\" a " + count + " categoría(s). "
                + "Los precios de los productos afectados serán recalculados. ¿Confirmas?",
            () -> new Thread(() -> {
                for (CategoryAssignRow row : rows) {
                    ProductCategory cat = row.getCategory();
                    boolean currentlyHas = cat.getTaxTypes().stream()
                            .anyMatch(t -> t.getId().equals(tax.getId()));
                    if (row.isAssigned() && !currentlyHas) {
                        inventoryService.addTaxToCategory(cat, tax);
                    } else if (!row.isAssigned() && currentlyHas) {
                        inventoryService.removeTaxFromCategory(cat, tax);
                    }
                }
                Platform.runLater(() -> ToastNotification.success(tableTaxTypes,
                        "Asignación de \"" + tax.getName() + "\" guardada correctamente"));
            }).start(),
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

    private void configurarTablaMargen() {
        colCategoriaNombre.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMargen.setCellValueFactory(cell -> {
            Double m = cell.getValue().getTargetMargin();
            return new SimpleStringProperty(m != null ? String.format("%.0f%%", m * 100) : "0%");
        });
        tableCategories.setEditable(false);
    }

    private void cargarComboCat() {
        List<ProductCategory> cats = inventoryService.findAllCategories();
        comboCat.setConverter(new StringConverter<>() {
            @Override public String toString(ProductCategory c) { return c != null ? c.getName() : ""; }
            @Override public ProductCategory fromString(String s) { return null; }
        });
        comboCat.setItems(FXCollections.observableArrayList(cats));
    }

    private void cargarTablaMargen() {
        List<ProductCategory> cats = inventoryService.findAllCategories();
        tableCategories.setItems(FXCollections.observableArrayList(cats));
    }

    @FXML
    public void guardarMargen(ActionEvent event) {
        ProductCategory cat = comboCat.getValue();
        String txt = txtMargen.getText() != null ? txtMargen.getText().trim() : "";
        if (cat == null || txt.isEmpty()) {
            ToastNotification.warning(comboCat, "Selecciona una categoría e ingresa el porcentaje");
            return;
        }
        double margenParsed = 0;
        try {
            margenParsed = Double.parseDouble(txt) / 100.0;
            if (margenParsed < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ToastNotification.warning(txtMargen, "Ingresa un número válido mayor a 0");
            return;
        }
        final double margen = margenParsed;
        CustomDialog.confirm(comboCat,
            "Guardar margen",
            "Vas a asignar " + txt + "% de utilidad a \"" + cat.getName() + "\". ¿Confirmas?",
            () -> new Thread(() -> {
                cat.setTargetMargin(margen);
                inventoryService.saveCategory(cat);
                List<Product> prods = inventoryService.findProductsByCategory(cat);
                prods.forEach(inventoryService::recalculateMinSalePrice);
                Platform.runLater(() -> {
                    comboCat.setValue(null);
                    txtMargen.clear();
                    cargarTablaMargen();
                    ToastNotification.success(comboCat, "Margen guardado correctamente");
                });
            }).start(),
            null);
    }

    static class CategoryAssignRow {
        private final BooleanProperty assigned;
        private final ProductCategory category;

        CategoryAssignRow(ProductCategory cat, boolean assigned) {
            this.category = cat;
            this.assigned = new SimpleBooleanProperty(assigned);
        }

        public BooleanProperty assignedProperty() { return assigned; }
        public boolean isAssigned() { return assigned.get(); }
        public void setAssigned(boolean v) { assigned.set(v); }
        public ProductCategory getCategory() { return category; }
        public String getName() { return category.getName(); }
    }
}
