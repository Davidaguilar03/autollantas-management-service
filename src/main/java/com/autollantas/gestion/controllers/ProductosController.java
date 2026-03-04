package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Producto;
import com.autollantas.gestion.repository.ProductoRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Component
public class ProductosController {

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired private ApplicationContext springContext;

    @FXML private ComboBox<String> comboCategoria;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtMarca;

    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;
    @FXML private TextField txtStockMin;
    @FXML private TextField txtStockMax;

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colDescripcion;

    @FXML private TableColumn<Producto, Double> colPrecioBruto;
    @FXML private TableColumn<Producto, Double> colIva;
    @FXML private TableColumn<Producto, Double> colPrecioTotal;

    @FXML private TableColumn<Producto, Integer> colExistencias;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;


    private final ObservableList<Producto> masterData = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Producto> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(sortedData);


        configurarColumnas();
        configurarFiltrosUI();

        setupListenersBusqueda();
        setupListenerSeleccion();


        cargarDatosDB();
    }

    private void cargarDatosDB() {
        if (productoRepository == null) return;


        new Thread(() -> {
            List<Producto> productos = productoRepository.findAll();

            Platform.runLater(() -> {
                masterData.setAll(productos);
                actualizarInfoRegistros();
                cargarCategoriasDesdeDatos();
            });
        }).start();
    }


    private void cargarCategoriasDesdeDatos() {

        List<String> categoriasUnicas = masterData.stream()
                .filter(p -> p.getCategoria() != null)
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
            if (cell.getValue().getCategoria() != null) {

                nombreCat = obtenerNombreCategoria(cell.getValue());
            }
            return new SimpleStringProperty(nombreCat);
        });
        estilizarColumnaTexto(colCategoria);


        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoProducto"));
        estilizarColumnaTexto(colCodigo);


        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        estilizarColumnaTexto(colDescripcion);


        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBrutoProducto"));
        colPrecioBruto.setCellFactory(col -> crearCeldaMoneda());


        colIva.setCellValueFactory(new PropertyValueFactory<>("ivaProducto"));
        colIva.setCellFactory(col -> crearCeldaMoneda());


        colPrecioTotal.setCellValueFactory(new PropertyValueFactory<>("precioIvaProducto"));
        colPrecioTotal.setCellFactory(col -> crearCeldaMoneda());


        colExistencias.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colExistencias.setCellFactory(col -> crearCeldaStock());
    }


    private String obtenerNombreCategoria(Producto p) {
        if (p.getCategoria() == null) return "N/A";

        return p.getCategoria().toString();
    }



    private void estilizarColumnaTexto(TableColumn<Producto, String> col) {
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

    private TableCell<Producto, Double> crearCeldaMoneda() {
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

    private TableCell<Producto, Integer> crearCeldaStock() {
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

        txtPrecioMin.textProperty().addListener(changeListener);
        txtPrecioMax.textProperty().addListener(changeListener);
        txtStockMin.textProperty().addListener(changeListener);
        txtStockMax.textProperty().addListener(changeListener);
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(prod -> {

            if (!matchTexto(prod.getCodigoProducto(), txtCodigo.getText())) return false;
            if (!matchTexto(prod.getDescripcion(), txtDescripcion.getText())) return false;


            String marcaFilter = txtMarca.getText();
            if (marcaFilter != null && !marcaFilter.isEmpty()) {
                boolean enDesc = prod.getDescripcion() != null && prod.getDescripcion().toLowerCase().contains(marcaFilter.toLowerCase());
                boolean enTipo = prod.getTipoItem() != null && prod.getTipoItem().toLowerCase().contains(marcaFilter.toLowerCase());
                if (!enDesc && !enTipo) return false;
            }


            String catSel = comboCategoria.getValue();
            if (catSel != null && !"Todas".equals(catSel)) {
                String catProd = obtenerNombreCategoria(prod);
                if (!catSel.equalsIgnoreCase(catProd)) return false;
            }


            if (!filtrarPorRangoInteligente(prod.getPrecioIvaProducto(), txtPrecioMin.getText(), txtPrecioMax.getText())) {
                return false;
            }

            if (!filtrarPorRangoInteligente(prod.getCantidad() != null ? prod.getCantidad().doubleValue() : 0.0, txtStockMin.getText(), txtStockMax.getText())) {
                return false;
            }

            return true;
        });
    }

    private boolean filtrarPorRangoInteligente(Double valorReal, String minStr, String maxStr) {
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


    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtCodigo.clear(); txtDescripcion.clear(); txtMarca.clear();
        txtPrecioMin.clear(); txtPrecioMax.clear();
        txtStockMin.clear(); txtStockMax.clear();
        comboCategoria.getSelectionModel().select("Todas");
        aplicarFiltros();
    }

    public void abrirModalProducto(Producto producto, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/FormularioProducto.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            FormularioProductoController controller = loader.getController();
            if (producto != null) controller.setProducto(producto);

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
                if (producto != null) tablaProductos.getSelectionModel().select(producto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnNuevoProductoClick(ActionEvent event) {
        abrirModalProducto(null, "Nuevo Producto");
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        Producto seleccion = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccion != null) {
            abrirModalProducto(seleccion, "Editar Producto: " + seleccion.getDescripcion());
        } else {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un producto para editar.");
        }
    }

    @FXML void btnEliminarClick(ActionEvent event) {
        Producto p = tablaProductos.getSelectionModel().getSelectedItem();
        if (p != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Eliminar");
            alert.setHeaderText("¿Eliminar producto de la base de datos?");
            alert.setContentText("Va a eliminar permanentemente: " + p.getDescripcion());

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    productoRepository.delete(p);
                    masterData.remove(p);
                    actualizarInfoRegistros();
                    cargarCategoriasDesdeDatos();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo eliminar el producto. Puede estar en uso en ventas o compras.");
                }
            }
        }
    }

    private void mostrarAlerta(Alert.AlertType type, String titulo, String contenido) {
        Alert alert = new Alert(type);
        alert.setTitle("Gestión de Inventario");
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}