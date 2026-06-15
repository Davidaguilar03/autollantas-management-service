package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@SuppressWarnings("ALL")
@Component
public class CategoryManagementController {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ApplicationContext springContext;

    @FXML private TableView<ProductCategory> tableCategories;
    @FXML private TableColumn<ProductCategory, String> colNombre;
    @FXML private TableColumn<ProductCategory, Integer> colStockAmarillo;
    @FXML private TableColumn<ProductCategory, Integer> colStockRojo;
    @FXML private TableColumn<ProductCategory, Integer> colProductCount;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final ObservableList<ProductCategory> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getName() != null ? cell.getValue().getName() : ""));
        colStockAmarillo.setCellValueFactory(cell -> {
            Integer val = cell.getValue().getYellowStockMin();
            return new SimpleIntegerProperty(val != null ? val : 0).asObject();
        });
        colStockRojo.setCellValueFactory(cell -> {
            Integer val = cell.getValue().getRedStockMin();
            return new SimpleIntegerProperty(val != null ? val : 0).asObject();
        });
        colProductCount.setCellValueFactory(cell -> {
            int count = inventoryService.findProductsByCategory(cell.getValue()).size();
            return new SimpleIntegerProperty(count).asObject();
        });

        tableCategories.setItems(categoryList);

        tableCategories.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            boolean sel = nw != null;
            btnEditar.setDisable(!sel);
            btnEliminar.setDisable(!sel);
        });

        loadCategories();
    }

    private void loadCategories() {
        new Thread(() -> {
            List<ProductCategory> cats = inventoryService.findAllCategories();
            Platform.runLater(() -> categoryList.setAll(cats));
        }).start();
    }

    @FXML
    public void onNueva(ActionEvent event) {
        abrirModalCategory(null);
    }

    @FXML
    public void onEditar(ActionEvent event) {
        ProductCategory sel = tableCategories.getSelectionModel().getSelectedItem();
        if (sel != null) abrirModalCategory(sel);
    }

    @FXML
    public void onEliminar(ActionEvent event) {
        ProductCategory sel = tableCategories.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        CustomDialog.danger(tableCategories,
            "Eliminar categoría",
            "Vas a eliminar la categoría \"" + sel.getName() + "\". "
                + "Si tiene productos asociados, la operación no podrá completarse. "
                + "Esta acción no se puede deshacer.",
            () -> {
                boolean eliminado = inventoryService.deleteCategory(sel);
                if (!eliminado) {
                    ToastNotification.warning(tableCategories, "No se puede eliminar: la categoría tiene productos asociados");
                } else {
                    loadCategories();
                    ToastNotification.success(tableCategories, "Categoría \"" + sel.getName() + "\" eliminada");
                }
            },
            null);
    }

    private void abrirModalCategory(ProductCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/autollantas/gestion/inventory/views/CategoryForm.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            CategoryFormController controller = loader.getController();
            boolean esEdicion = category != null;
            if (esEdicion) controller.setCategory(category);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tableCategories.getScene().getWindow();
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
                loadCategories();
                if (esEdicion) {
                    ToastNotification.success(tableCategories, "Categoría \"" + category.getName() + "\" actualizada");
                } else {
                    ToastNotification.success(tableCategories, "Categoría creada correctamente");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tableCategories, "No se pudo abrir el formulario de categoría");
        }
    }

    @FXML
    public void cerrarVentana() {
        if (tableCategories.getScene() != null) {
            ((Stage) tableCategories.getScene().getWindow()).close();
        }
    }
}