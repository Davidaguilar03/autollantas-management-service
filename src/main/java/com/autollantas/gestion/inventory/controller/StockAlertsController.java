package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class StockAlertsController {

    @Autowired
    private InventoryService inventoryService;

    @FXML private Label lblKpiCritico;
    @FXML private Label lblKpiAdvertencia;
    @FXML private Label lblKpiSaludable;

    @FXML private ComboBox<String> comboFiltroCategoria;
    @FXML private ComboBox<String> comboFiltroEstado;
    @FXML private TextField txtBuscarProducto;

    @FXML private ProgressIndicator loaderScroll;
    @FXML private Label lblInfoScroll;

    @FXML private TableView<ProductAlertDTO> tablaAlertas;
    @FXML private TableColumn<ProductAlertDTO, String> colCodigo;
    @FXML private TableColumn<ProductAlertDTO, String> colProducto;
    @FXML private TableColumn<ProductAlertDTO, String> colCategoria;
    @FXML private TableColumn<ProductAlertDTO, Integer> colStockActual;
    @FXML private TableColumn<ProductAlertDTO, AlertSeverity> colEstadoVisual;

    @FXML private TableView<CategoryConfigModel> tablaConfiguracion;
    @FXML private TableColumn<CategoryConfigModel, String> colConfCategoria;
    @FXML private TableColumn<CategoryConfigModel, Integer> colConfAmarillo;
    @FXML private TableColumn<CategoryConfigModel, Integer> colConfRojo;

    private final ObservableList<ProductAlertDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<ProductAlertDTO> filteredData;

    private final ObservableList<CategoryConfigModel> configList = FXCollections.observableArrayList();
    private final Map<String, CategoryConfigModel> configMap = new HashMap<>();

    @FXML
    public void initialize() {
        configurarTablaAlertas();
        configurarTablaConfiguracion();

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<ProductAlertDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaAlertas.comparatorProperty());
        tablaAlertas.setItems(sortedData);

        setupListenersFiltros();

        cargarDatosDB();
    }

    private void cargarDatosDB() {
        if (inventoryService == null) return;
        if (loaderScroll != null) loaderScroll.setVisible(true);

        new Thread(() -> {
            List<Product> productosDB = inventoryService.findAllProducts();
            List<ProductAlertDTO> dtos = new ArrayList<>();
            Set<String> categoriasEncontradas = new HashSet<>();

            for (Product p : productosDB) {
                ProductAlertDTO dto = new ProductAlertDTO(p);
                dtos.add(dto);
                categoriasEncontradas.add(dto.getCategoria());
            }

            Platform.runLater(() -> {
                inicializarConfigMap(categoriasEncontradas);

                dtos.forEach(this::analizarProducto);

                masterData.setAll(dtos);
                actualizarKPIs();
                actualizarComboCategorias(categoriasEncontradas);

                aplicarFiltros();

                if (loaderScroll != null)
                    loaderScroll.setVisible(false);
            });
        }).start();
    }

    private void inicializarConfigMap(Set<String> categorias) {
        if (configList.isEmpty()) {
            configList.add(new CategoryConfigModel("General (Default)", 10, 5));
        }

        configMap.clear();
        for (CategoryConfigModel c : configList) {
            configMap.put(c.getCategoriaNombre(), c);
        }

        for (String cat : categorias) {
            if (!configMap.containsKey(cat)) {
                CategoryConfigModel nuevaConf = new CategoryConfigModel(cat, 5, 2);
                configList.add(nuevaConf);
                configMap.put(cat, nuevaConf);
            }
        }
    }

    private void analizarProducto(ProductAlertDTO prod) {
        CategoryConfigModel conf = configMap.get(prod.getCategoria());
        if (conf == null) conf = configList.stream().filter(c -> c.getCategoriaNombre().startsWith("General")).findFirst().orElse(configList.getFirst());

        int stock = prod.getStock();
        int rojo = conf.getUmbralRojo();
        int amarillo = conf.getUmbralAmarillo();

        if (stock <= rojo) {
            prod.setSeverity(AlertSeverity.CRITICAL);
            prod.setMensaje("Crítico (≤ " + rojo + ")");
        } else if (stock <= amarillo) {
            prod.setSeverity(AlertSeverity.WARNING);
            prod.setMensaje("Bajo (≤ " + amarillo + ")");
        } else {
            prod.setSeverity(AlertSeverity.OK);
            prod.setMensaje("Normal");
        }
    }

    private void actualizarKPIs() {
        long criticos = masterData.stream().filter(p -> p.getSeverity() == AlertSeverity.CRITICAL).count();
        long advertencias = masterData.stream().filter(p -> p.getSeverity() == AlertSeverity.WARNING).count();
        long ok = masterData.stream().filter(p -> p.getSeverity() == AlertSeverity.OK).count();

        if (lblKpiCritico != null) lblKpiCritico.setText(String.valueOf(criticos));
        if (lblKpiAdvertencia != null) lblKpiAdvertencia.setText(String.valueOf(advertencias));
        if (lblKpiSaludable != null) lblKpiSaludable.setText(String.valueOf(ok));
    }

    private void configurarTablaAlertas() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colStockActual.setCellValueFactory(new PropertyValueFactory<>("stock"));

        colEstadoVisual.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colEstadoVisual.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setMaxWidth(Double.MAX_VALUE);
                badge.setAlignment(Pos.CENTER);
                badge.setStyle("-fx-padding: 3 10; -fx-background-radius: 10;");
            }
            @Override protected void updateItem(AlertSeverity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                switch (item) {
                    case CRITICAL:
                        badge.setText("CRÍTICO");
                        badge.setStyle("-fx-background-color: #fadbd8; -fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-background-radius: 12;");
                        break;
                    case WARNING:
                        badge.setText("BAJO");
                        badge.setStyle("-fx-background-color: #fdebd0; -fx-text-fill: #d35400; -fx-font-weight: bold; -fx-background-radius: 12;");
                        break;
                    case OK:
                        badge.setText("OK");
                        badge.setStyle("-fx-background-color: #d5f5e3; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-background-radius: 12;");
                        break;
                }
                setGraphic(badge);
            }
        });
    }

    private void configurarTablaConfiguracion() {
        colConfCategoria.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));

        javafx.util.Callback<TableColumn<CategoryConfigModel, Integer>, TableCell<CategoryConfigModel, Integer>> cellFactory
                = p -> new SmartIntegerCell();

        colConfAmarillo.setCellValueFactory(new PropertyValueFactory<>("umbralAmarillo"));
        colConfAmarillo.setCellFactory(cellFactory);
        colConfAmarillo.setOnEditCommit(e -> e.getRowValue().setUmbralAmarillo(e.getNewValue()));

        colConfRojo.setCellValueFactory(new PropertyValueFactory<>("umbralRojo"));
        colConfRojo.setCellFactory(cellFactory);
        colConfRojo.setOnEditCommit(e -> e.getRowValue().setUmbralRojo(e.getNewValue()));

        tablaConfiguracion.setItems(configList);
    }

    private void actualizarComboCategorias(Set<String> categorias) {
        comboFiltroCategoria.getItems().clear();
        comboFiltroCategoria.getItems().add("Todas");
        List<String> sorted = new ArrayList<>(categorias);
        Collections.sort(sorted);
        comboFiltroCategoria.getItems().addAll(sorted);
        comboFiltroCategoria.getSelectionModel().selectFirst();
    }

    private void setupListenersFiltros() {
        comboFiltroEstado.getItems().addAll("Todos", "CRÍTICO", "BAJO", "OK");
        comboFiltroEstado.getSelectionModel().selectFirst();

        comboFiltroCategoria.valueProperty().addListener((o, old, val) -> aplicarFiltros());
        comboFiltroEstado.valueProperty().addListener((o, old, val) -> aplicarFiltros());
        txtBuscarProducto.textProperty().addListener((o, old, val) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(p -> {
            String term = txtBuscarProducto.getText() != null ? txtBuscarProducto.getText().toLowerCase() : "";
            String cat = comboFiltroCategoria.getValue();
            String estado = comboFiltroEstado.getValue();

            boolean matchTexto = term.isEmpty()
                    || (p.getNombre() != null && p.getNombre().toLowerCase().contains(term))
                    || (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(term));

            boolean matchCat = cat == null || "Todas".equals(cat) || p.getCategoria().equals(cat);

            boolean matchEstado = true;
            if (estado != null && !"Todos".equals(estado)) {
                if ("CRÍTICO".equals(estado) && p.getSeverity() != AlertSeverity.CRITICAL) {
                    matchEstado = false;
                } else if ("BAJO".equals(estado) && p.getSeverity() != AlertSeverity.WARNING) {
                    matchEstado = false;
                } else if ("OK".equals(estado) && p.getSeverity() != AlertSeverity.OK) {
                    matchEstado = false;
                }
            }

            return matchTexto && matchCat && matchEstado;
        });

        if (lblInfoScroll != null) {
            lblInfoScroll.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    @FXML
    void btnGuardarConfiguracionClick(ActionEvent event) {
        tablaConfiguracion.edit(-1, null);

        CustomDialog.confirm(tablaAlertas,
            "Aplicar umbrales de stock",
            "Vas a reclasificar todos los productos según los nuevos umbrales configurados por categoría. "
                + "Los estados crítico, bajo y normal se recalcularán para toda la tabla. ¿Confirmas?",
            () -> {
                configMap.clear();
                for (CategoryConfigModel c : configList) configMap.put(c.getCategoriaNombre(), c);

                masterData.forEach(this::analizarProducto);
                tablaAlertas.refresh();
                actualizarKPIs();
                aplicarFiltros();

                long criticos = masterData.stream().filter(p -> p.getSeverity() == AlertSeverity.CRITICAL).count();
                long advertencias = masterData.stream().filter(p -> p.getSeverity() == AlertSeverity.WARNING).count();

                if (criticos > 0 || advertencias > 0) {
                    ToastNotification.warning(tablaAlertas,
                        "Umbrales aplicados: " + criticos + " crítico(s), " + advertencias + " bajo(s)");
                } else {
                    ToastNotification.success(tablaAlertas,
                        "Umbrales aplicados · todos los productos en estado normal");
                }
            },
            null);
    }

    @FXML void btnRefreshClick(ActionEvent event) {
        cargarDatosDB();
        ToastNotification.success(tablaAlertas, "Datos de stock actualizados");
    }

    public enum AlertSeverity { CRITICAL, WARNING, OK }

    @Data
    public static class ProductAlertDTO {
        private final Product originalProduct;
        private final String codigo;
        private final String nombre;
        private final String categoria;
        private final Integer stock;
        private AlertSeverity severity;
        private String mensaje;

        public ProductAlertDTO(Product p) {
            this.originalProduct = p;
            this.codigo = p.getCode();
            this.nombre = p.getDescription();
            this.stock = p.getQuantity() != null ? p.getQuantity() : 0;
            this.categoria = (p.getCategory() != null) ? p.getCategory().toString() : "Sin Categoría";
            this.severity = AlertSeverity.OK;
            this.mensaje = "";
        }
    }

    public static class CategoryConfigModel {
        private final StringProperty categoriaNombre;
        private final IntegerProperty umbralAmarillo;
        private final IntegerProperty umbralRojo;

        public CategoryConfigModel(String nombre, int amarillo, int rojo) {
            this.categoriaNombre = new SimpleStringProperty(nombre);
            this.umbralAmarillo = new SimpleIntegerProperty(amarillo);
            this.umbralRojo = new SimpleIntegerProperty(rojo);
        }

        public String getCategoriaNombre() { return categoriaNombre.get(); }
        public void setCategoriaNombre(String v) { categoriaNombre.set(v); }

        public Integer getUmbralAmarillo() { return umbralAmarillo.get(); }
        public void setUmbralAmarillo(Integer v) { umbralAmarillo.set(v); }

        public Integer getUmbralRojo() { return umbralRojo.get(); }
        public void setUmbralRojo(Integer v) { umbralRojo.set(v); }
    }

    static class SmartIntegerCell extends TableCell<CategoryConfigModel, Integer> {
        private TextField textField;

        @Override public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
            }
        }
        @Override public void cancelEdit() {
            super.cancelEdit();
            setText(getItem().toString());
            setGraphic(null);
        }
        @Override public void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) { setText(null); setGraphic(null); }
            else {
                if (isEditing()) {
                    if (textField != null) textField.setText(getString());
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }
        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) textField.setText(newVal.replaceAll("[^\\d]", ""));
            });

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) commitEdit(Integer.valueOf(textField.getText().isEmpty() ? "0" : textField.getText()));
            });
            textField.setOnAction(e -> commitEdit(Integer.valueOf(textField.getText().isEmpty() ? "0" : textField.getText())));
        }
        private String getString() { return getItem() == null ? "" : getItem().toString(); }
    }
}