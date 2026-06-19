package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Component
public class ProductsController {

    @Autowired
    private InventoryService inventoryService;
    @Autowired private ApplicationContext springContext;

    @FXML private ComboBox<String> comboCategoria;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtMarca;

    @FXML private TextField txtPrecioCompraMin;
    @FXML private TextField txtPrecioCompraMax;
    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;

    @FXML private TableView<Product> tablaProductos;
    @FXML private TableColumn<Product, String> colCategoria;
    @FXML private TableColumn<Product, String> colCodigo;
    @FXML private TableColumn<Product, String> colDescripcion;

    @FXML private TableColumn<Product, Double> colPurchaseCost;
    @FXML private TableColumn<Product, Double> colTaxAmount;
    @FXML private TableColumn<Product, Double> colMinSalePrice;
    @FXML private TableColumn<Product, Double> colSuggestedPrice;

    @FXML private TableColumn<Product, Integer> colExistencias;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;


    private final ObservableList<Product> masterData = FXCollections.observableArrayList();
    private FilteredList<Product> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Product> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(sortedData);


        configurarColumnas();
        configurarFiltrosUI();

        setupListenersBusqueda();
        setupListenerSeleccion();


        cargarDatosDB();
    }

    private void cargarDatosDB() {
        if (inventoryService == null) return;

        new Thread(() -> {
            List<Product> productos = inventoryService.findAllProducts();

            Platform.runLater(() -> {
                masterData.setAll(productos);
                actualizarInfoRegistros();
                cargarCategoriasDesdeDatos();
            });
        }).start();
    }


    private void cargarCategoriasDesdeDatos() {

        List<String> categoriasUnicas = masterData.stream()
                .filter(p -> p.getCategory() != null)
                .map(p -> obtenerNombreCategoria(p))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        comboCategoria.getItems().clear();
        comboCategoria.getItems().add("Todas");
        comboCategoria.getItems().addAll(categoriasUnicas);
        comboCategoria.getSelectionModel().select("Todas");
    }

    private void actualizarInfoRegistros() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }



    private void configurarColumnas() {

        colCategoria.setCellValueFactory(cell -> {
            String nombreCat = "Sin Categoría";
            if (cell.getValue().getCategory() != null) {
                nombreCat = obtenerNombreCategoria(cell.getValue());
            }
            return new SimpleStringProperty(nombreCat);
        });
        colCategoria.setCellFactory(col -> new TableCell<Product, String>() {
            private final Label chip = new Label();
            {
                chip.setStyle(
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 3 10 3 10;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;"
                );
                chip.setMaxWidth(Double.MAX_VALUE);
                chip.setAlignment(javafx.geometry.Pos.CENTER);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    ProductCategory cat = product != null ? product.getCategory() : null;
                    String color = (cat != null && cat.getColor() != null && !cat.getColor().isEmpty())
                        ? cat.getColor() : "#94a3b8";
                    chip.setText(item);
                    chip.setStyle(
                        "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 3 10 3 10;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
                    );
                    setGraphic(chip);
                    setText(null);
                }
            }
        });


        colCodigo.setCellValueFactory(new PropertyValueFactory<>("code"));
        estilizarColumnaTexto(colCodigo);


        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("description"));
        estilizarColumnaTexto(colDescripcion);


        colPurchaseCost.setCellValueFactory(new PropertyValueFactory<>("purchaseCost"));
        colPurchaseCost.setCellFactory(col -> crearCeldaMoneda());

        colTaxAmount.setCellValueFactory(new PropertyValueFactory<>("taxAmount"));
        colTaxAmount.setCellFactory(col -> crearCeldaMoneda());

        colMinSalePrice.setCellValueFactory(new PropertyValueFactory<>("minSalePrice"));
        colMinSalePrice.setCellFactory(col -> crearCeldaMoneda());

        colSuggestedPrice.setCellValueFactory(new PropertyValueFactory<>("suggestedPrice"));
        colSuggestedPrice.setCellFactory(col -> crearCeldaMoneda());

        colExistencias.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colExistencias.setCellFactory(col -> crearCeldaStock());
    }


    private String obtenerNombreCategoria(Product p) {
        if (p.getCategory() == null) return "N/A";

        return p.getCategory().toString();
    }



    private void estilizarColumnaTexto(TableColumn<Product, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private TableCell<Product, Double> crearCeldaMoneda() {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<Product, Integer> crearCeldaStock() {
        return new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(String.valueOf(item));
                    lbl.setAlignment(Pos.CENTER);


                    if (item <= 5) {
                        lbl.setStyle("-fx-font-weight: bold;");
                        lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.RED));
                    } else {
                        lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    }

                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }


    private void setupListenersBusqueda() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> {
            aplicarFiltros();
            actualizarInfoRegistros();
        };

        txtCodigo.textProperty().addListener(changeListener);
        txtDescripcion.textProperty().addListener(changeListener);
        txtMarca.textProperty().addListener(changeListener);
        comboCategoria.valueProperty().addListener(changeListener);

        txtPrecioCompraMin.textProperty().addListener(changeListener);
        txtPrecioCompraMax.textProperty().addListener(changeListener);
        txtPrecioMin.textProperty().addListener(changeListener);
        txtPrecioMax.textProperty().addListener(changeListener);
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(prod -> {

            if (!matchTexto(prod.getCode(), txtCodigo.getText())) return false;
            if (!matchTexto(prod.getDescription(), txtDescripcion.getText())) return false;


            String marcaFilter = txtMarca.getText();
            if (marcaFilter != null && !marcaFilter.isEmpty()) {
                boolean enDesc = prod.getDescription() != null && prod.getDescription().toLowerCase().contains(marcaFilter.toLowerCase());
                boolean enTipo = prod.getItemType() != null && prod.getItemType().toLowerCase().contains(marcaFilter.toLowerCase());
                if (!enDesc && !enTipo) return false;
            }


            String catSel = comboCategoria.getValue();
            if (catSel != null && !"Todas".equals(catSel)) {
                String catProd = obtenerNombreCategoria(prod);
                if (!catSel.equalsIgnoreCase(catProd)) return false;
            }


            if (!filtrarPorRangoInteligente(prod.getSuggestedPrice(), txtPrecioMin.getText(), txtPrecioMax.getText())) {
                return false;
            }

            if (!filtrarPorRangoInteligente(prod.getPurchaseCost(), txtPrecioCompraMin.getText(), txtPrecioCompraMax.getText())) {
                return false;
            }

            return true;
        });
    }

    private boolean filtrarPorRangoInteligente(Double valorReal, String minStr, String maxStr) {
        boolean sinFiltro = (minStr == null || minStr.isEmpty())
                         && (maxStr == null || maxStr.isEmpty());
        if (sinFiltro) return true;
        if (valorReal == null) return false;

        Double min = parsearValorNumerico(minStr);
        Double max = parsearValorNumerico(maxStr);

        if (min != null && valorReal < min) return false;
        if (max != null && valorReal > max) return false;

        return true;
    }

    private Double parsearValorNumerico(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        try {
            String limpio = texto.replaceAll("[^0-9.]", "");
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean matchTexto(String valorReal, String filtro) {
        return filtro == null || filtro.isEmpty() || (valorReal != null && valorReal.toLowerCase().contains(filtro.toLowerCase()));
    }

    private void configurarFiltrosUI() {

        comboCategoria.getItems().add("Todas");
        comboCategoria.getSelectionModel().select("Todas");
    }

    private void setupListenerSeleccion() {
        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean haySeleccion = (newSelection != null);
            btnEditar.setDisable(!haySeleccion);
            btnEliminar.setDisable(!haySeleccion);
        });
    }


    @FXML void btnCategoriasClick(ActionEvent event) {
        abrirModalConfiguracion("/com/autollantas/gestion/inventory/views/CategoryManagement.fxml", "categorías");
    }


    private void abrirModalConfiguracion(String fxmlPath, String nombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaProductos.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());

            modalStage.showAndWait();
            cargarDatosDB();
        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaProductos, "No se pudo abrir el módulo de " + nombre);
        }
    }

    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtCodigo.clear(); txtDescripcion.clear(); txtMarca.clear();
        txtPrecioCompraMin.clear(); txtPrecioCompraMax.clear();
        txtPrecioMin.clear(); txtPrecioMax.clear();
        comboCategoria.getSelectionModel().select("Todas");
        aplicarFiltros();
    }

    public void abrirModalProduct(Product producto, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/inventory/views/ProductForm.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            ProductFormController controller = loader.getController();
            boolean esEdicion = producto != null;
            if (esEdicion) controller.setProduct(producto);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaProductos.getScene().getWindow();
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
                cargarDatosDB();
                if (esEdicion) {
                    ToastNotification.success(tablaProductos, "Producto \"" + producto.getDescription() + "\" actualizado");
                } else {
                    ToastNotification.success(tablaProductos, "Producto creado correctamente");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaProductos, "No se pudo abrir el formulario de producto");
        }
    }

    @FXML
    void btnNuevoProductoClick(ActionEvent event) {
        abrirModalProduct(null, "Nuevo Product");
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        Product seleccion = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccion != null) {
            abrirModalProduct(seleccion, "Editar Product: " + seleccion.getDescription());
        }
    }

    @FXML void btnEliminarClick(ActionEvent event) {
        Product p = tablaProductos.getSelectionModel().getSelectedItem();
        if (p == null) return;

        CustomDialog.danger(tablaProductos,
            "Eliminar producto",
            "Vas a eliminar permanentemente \"" + p.getDescription() + "\" (código: " + p.getCode() + "). "
                + "Si el producto está asociado a facturas de venta o compra, la operación no podrá completarse. "
                + "Esta acción no se puede deshacer.",
            () -> {
                try {
                    inventoryService.deleteProduct(p);
                    masterData.remove(p);
                    actualizarInfoRegistros();
                    cargarCategoriasDesdeDatos();
                    ToastNotification.success(tablaProductos, "Producto \"" + p.getDescription() + "\" eliminado");
                } catch (Exception e) {
                    ToastNotification.error(tablaProductos, "No se pudo eliminar el producto, puede estar en uso en ventas o compras");
                }
            },
            null);
    }
}