package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("ALL")
@Component
@Scope("prototype")
public class SaleInvoicesController {

    @Autowired private SalesService salesService;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtNumero;
    @FXML private TextField txtCliente;
    @FXML private TextField txtTotal;
    @FXML private ComboBox<String> comboEstado;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;

    @FXML private DatePicker dpCreacionDesde;
    @FXML private DatePicker dpCreacionHasta;
    @FXML private DatePicker dpVencimientoDesde;
    @FXML private DatePicker dpVencimientoHasta;

    @FXML private TableView<Sale> tablaFacturas;
    @FXML private TableColumn<Sale, String> colNumero;
    @FXML private TableColumn<Sale, String> colCliente;
    @FXML private TableColumn<Sale, LocalDate> colFechaCreacion;
    @FXML private TableColumn<Sale, LocalDate> colFechaVencimiento;
    @FXML private TableColumn<Sale, Double> colTotal;
    @FXML private TableColumn<Sale, String> colMetodoPago;
    @FXML private TableColumn<Sale, String> colFormaPago;
    @FXML private TableColumn<Sale, String> colCuenta;
    @FXML private TableColumn<Sale, Double> colPorCobrar;
    @FXML private TableColumn<Sale, String> colEstado;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnVerDetalles;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Sale> masterData = FXCollections.observableArrayList();
    private FilteredList<Sale> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        this.filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Sale> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaFacturas.comparatorProperty());
        tablaFacturas.setItems(sortedData);

        setupColumns();
        setupFilterUI();
        setupDateFormatters();
        setupSearchListeners();
        setupSelectionListeners();

        tablaFacturas.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                Sale seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    openDetailsModal(seleccionada);
                    event.consume();
                }
            }
        });

        tablaFacturas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Sale seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    openDetailsModal(seleccionada);
                }
            }
        });

        loadDataFromDB();
    }

    private void loadDataFromDB() {
        Platform.runLater(() -> {
            try {
                List<Sale> salesDB = salesService.findAllSales();
                masterData.setAll(salesDB);
                updateRecordsLabel();
                applyFilters();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateRecordsLabel() {
        if (lblInfoRegistros != null) {
            int total = masterData.size();
            int mostrados = filteredData.size();
            lblInfoRegistros.setText("Mostrando " + mostrados + " de " + total + " registros");
        }
    }

    @FXML
    void btnNuevaFacturaClick(ActionEvent event) {
        openForm();
    }

    public void openForm() {
        MainLayoutController.getInstance().loadView("/com/autollantas/gestion/sales/views/SaleForm.fxml");
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        Sale seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            showAlert("Ninguna selección", "Por favor selecciona una venta para editar.", Alert.AlertType.WARNING);
            return;
        }

        if ("ANULADA".equalsIgnoreCase(seleccionada.getStatus())) {
            showAlert("Acción no permitida", "No se puede editar una factura que ya ha sido ANULADA.", Alert.AlertType.ERROR);
            return;
        }

        Object controllerObj = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/sales/views/SaleForm.fxml");

        if (controllerObj instanceof SaleFormController) {
            SaleFormController formularioController = (SaleFormController) controllerObj;
            formularioController.setSaleForEditing(seleccionada);
        }
    }

    private void showAlert(String titulo, String contenido, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    @FXML
    void btnAnularClick(ActionEvent event) {
        Sale sel = tablaFacturas.getSelectionModel().getSelectedItem();
        if (sel != null) {
            if ("ANULADA".equalsIgnoreCase(sel.getStatus())) {
                showAlert("Atención", "Esta factura ya se encuentra anulada.", Alert.AlertType.WARNING);
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Anular Venta");
            confirm.setHeaderText("¿Desea anular la factura " + sel.getInvoiceNumber() + "?");
            confirm.setContentText("Esta acción es irreversible y DEVOLVERÁ los productos al inventario.");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    salesService.cancelSale(sel);
                    tablaFacturas.refresh();
                    applyFilters();
                    showAlert("Éxito", "Venta anulada y stock devuelto correctamente.", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "No se pudo anular la venta.", Alert.AlertType.ERROR);
                }
            }
        }
    }

    @FXML
    void btnVerDetallesClick(ActionEvent event) {
        Sale seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;
        openDetailsModal(seleccionada);
    }

    private void openDetailsModal(Sale sale) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/sales/views/SaleDetails.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            SaleDetailsController controller = loader.getController();
            controller.setSale(sale);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaFacturas.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) modalStage.close();
            });

            modalStage.setScene(scene);
            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());
            modalStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el detalle: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupColumns() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        styleTextColumn(colNumero);

        colCliente.setCellValueFactory(cell -> {
            if (cell.getValue().getCustomer() != null)
                return new SimpleStringProperty(cell.getValue().getCustomer().getName());
            return new SimpleStringProperty("Cliente General");
        });
        styleTextColumn(colCliente);

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        colFechaCreacion.setCellFactory(col -> createDateCell(false));

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colFechaVencimiento.setCellFactory(col -> createDateCell(true));

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> createCurrencyCell());

        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        styleTextColumn(colFormaPago);

        colMetodoPago.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        styleTextColumn(colMetodoPago);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null)
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            return new SimpleStringProperty("-");
        });
        styleTextColumn(colCuenta);

        colPorCobrar.setCellValueFactory(cell -> {
            Sale v = cell.getValue();
            double porCobrar = 0.0;
            if ("PENDIENTE".equalsIgnoreCase(v.getStatus()))
                porCobrar = v.getPendingBalance() != null ? v.getPendingBalance() : v.getTotal();
            return new javafx.beans.property.SimpleObjectProperty<>(porCobrar);
        });
        colPorCobrar.setCellFactory(col -> createPendingCell());

        colEstado.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEstado.setCellFactory(col -> createStatusCell());
    }

    private void styleTextColumn(TableColumn<Sale, String> col) {
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
                    setPadding(Insets.EMPTY);
                }
            }
        });
    }

    private TableCell<Sale, LocalDate> createDateCell(boolean validarVencimiento) {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(dateFormat.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    Color baseColor = Color.BLACK;
                    if (validarVencimiento && item.isBefore(LocalDate.now())) {
                        baseColor = Color.RED;
                        lbl.setStyle("-fx-font-weight: bold;");
                    }
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<Sale, Double> createCurrencyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER_RIGHT);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private TableCell<Sale, Double> createPendingCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER_RIGHT);
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor = (item > 0) ? Color.RED : COLOR_VERDE_OSCURO;
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private TableCell<Sale, String> createStatusCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor;
                    if ("PAGADA".equalsIgnoreCase(item)) baseColor = ESTADO_PAGADA;
                    else if ("PENDIENTE".equalsIgnoreCase(item)) baseColor = ESTADO_PENDIENTE;
                    else if ("ANULADA".equalsIgnoreCase(item)) baseColor = ESTADO_ANULADA;
                    else baseColor = Color.BLACK;
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private void setupSearchListeners() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> applyFilters();
        txtNumero.textProperty().addListener(changeListener);
        txtCliente.textProperty().addListener(changeListener);
        txtTotal.textProperty().addListener(changeListener);
        comboEstado.valueProperty().addListener(changeListener);
        comboFormaPago.valueProperty().addListener(changeListener);
        comboMedioPago.valueProperty().addListener(changeListener);
        dpCreacionDesde.valueProperty().addListener(changeListener);
        dpCreacionHasta.valueProperty().addListener(changeListener);
        dpVencimientoDesde.valueProperty().addListener(changeListener);
        dpVencimientoHasta.valueProperty().addListener(changeListener);
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(sale -> {
            String filtroEstado = comboEstado.getValue();
            String saleStatus = sale.getStatus();

            if ("PENDIENTE".equals(filtroEstado)) { if (!"PENDIENTE".equalsIgnoreCase(saleStatus)) return false; }
            else if ("PAGADA".equals(filtroEstado)) { if (!"PAGADA".equalsIgnoreCase(saleStatus)) return false; }
            else if ("ANULADA".equals(filtroEstado)) { if (!"ANULADA".equalsIgnoreCase(saleStatus)) return false; }

            if (!matchTexto(sale.getInvoiceNumber(), txtNumero.getText())) return false;

            String nombreCliente = (sale.getCustomer() != null) ? sale.getCustomer().getName() : "";
            if (!matchTexto(nombreCliente, txtCliente.getText())) return false;

            String totalFilter = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!totalFilter.isEmpty()) {
                if (sale.getTotal() == null) return false;
                if (!String.valueOf(sale.getTotal().longValue()).startsWith(totalFilter)) return false;
            }

            if (!matchCombo(comboFormaPago, sale.getPaymentType())) return false;
            if (!matchCombo(comboMedioPago, sale.getPaymentMethod())) return false;
            if (fueraDeRango(sale.getSaleDate(), dpCreacionDesde.getValue(), dpCreacionHasta.getValue())) return false;
            if (fueraDeRango(sale.getDueDate(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;

            return true;
        });

        updateRecordsLabel();
    }

    private boolean matchTexto(String valorReal, String filtroUsuario) {
        if (filtroUsuario == null || filtroUsuario.isEmpty()) return true;
        if (valorReal == null) return false;
        return valorReal.toLowerCase().contains(filtroUsuario.toLowerCase());
    }

    private boolean matchCombo(ComboBox<String> combo, String valorReal) {
        String seleccion = combo.getValue();
        if (seleccion == null || seleccion.equals("Todos") || seleccion.equals("Todas")) return true;
        return valorReal != null && valorReal.equalsIgnoreCase(seleccion);
    }

    private boolean fueraDeRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return true;
        if (desde != null && fecha.isBefore(desde)) return true;
        if (hasta != null && fecha.isAfter(hasta)) return true;
        return false;
    }

    private void setupDateFormatters() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? dateFormat.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, dateFormat) : null; }
        };
        dpCreacionDesde.setConverter(c); dpCreacionHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }

    private void setupFilterUI() {
        comboEstado.getItems().clear();
        comboEstado.getItems().addAll("Todos", "PAGADA", "PENDIENTE", "ANULADA");
        comboEstado.getSelectionModel().selectFirst();

        comboFormaPago.getItems().clear();
        comboFormaPago.getItems().addAll("Todas", "Contado", "Crédito");
        comboFormaPago.getSelectionModel().selectFirst();

        comboMedioPago.getItems().clear();
        comboMedioPago.getItems().addAll("Todos", "Efectivo", "Transferencia", "Nequi/Daviplata", "Tarjeta");
        comboMedioPago.getSelectionModel().selectFirst();
    }

    private void setupSelectionListeners() {
        btnEditar.setDisable(true); btnEliminar.setDisable(true); btnVerDetalles.setDisable(true);
        tablaFacturas.getSelectionModel().selectedItemProperty().addListener((obs, old, nueva) -> {
            boolean haySeleccion = (nueva != null);
            btnEditar.setDisable(!haySeleccion);
            btnEliminar.setDisable(!haySeleccion);
            btnVerDetalles.setDisable(!haySeleccion);
        });
    }

    @FXML void btnBuscarClick(ActionEvent event) { applyFilters(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtNumero.clear(); txtCliente.clear(); txtTotal.clear();
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.getSelectionModel().selectFirst();
        dpCreacionDesde.setValue(null); dpCreacionHasta.setValue(null);
        dpVencimientoDesde.setValue(null); dpVencimientoHasta.setValue(null);
    }
}
