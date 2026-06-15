package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
@Component
public class CollectionsController {

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
    @FXML private TableColumn<Sale, String> colFormaPago;
    @FXML private TableColumn<Sale, String> colMetodoPago;
    @FXML private TableColumn<Sale, String> colCuenta;
    @FXML private TableColumn<Sale, Double> colTotal;
    @FXML private TableColumn<Sale, Double> colPorCobrar;
    @FXML private TableColumn<Sale, String> colEstado;

    @FXML private Button btnRegistrarPago;
    @FXML private Button btnVerDetalle;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Sale> masterData = FXCollections.observableArrayList();
    private FilteredList<Sale> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        this.filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Sale> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaFacturas.comparatorProperty());
        tablaFacturas.setItems(sortedData);

        setupColumns();
        configurarFormatosFecha();
        setupFilterUI();
        resetFilters();
        setupListeners();
        setupInteractionListeners();
        loadDataFromDB();
    }

    private void loadDataFromDB() {
        Platform.runLater(() -> {
            List<Sale> lista = salesService.findAllSales().stream()
                    .filter(s -> s.getPendingBalance() != null && s.getPendingBalance() > 0)
                    .toList();
            masterData.setAll(lista);
            applyFilters();
        });
    }

    private void setupFilterUI() {
        comboEstado.getItems().clear();
        comboEstado.getItems().addAll("Todos", "PENDIENTE", "PAGADA", "ANULADA");

        comboFormaPago.getItems().clear();
        comboFormaPago.getItems().addAll("Todas", "Crédito", "Contado");

        comboMedioPago.getItems().clear();
        comboMedioPago.getItems().addAll("Todos", "Efectivo", "Transferencia", "Nequi/Daviplata", "Cheque");
    }

    private void resetFilters() {
        txtNumero.clear();
        txtCliente.clear();
        txtTotal.clear();
        comboEstado.getSelectionModel().select(0);
        comboFormaPago.getSelectionModel().select(0);
        comboMedioPago.getSelectionModel().select(0);
        dpCreacionDesde.setValue(null);
        dpCreacionHasta.setValue(null);
        dpVencimientoDesde.setValue(null);
        dpVencimientoHasta.setValue(null);
    }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        resetFilters();
        applyFilters();
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(sale -> {
            if (!matchText(txtNumero.getText(), sale.getInvoiceNumber())) return false;

            String cliente = (sale.getCustomer() != null) ? sale.getCustomer().getName() : "";
            if (!matchText(txtCliente.getText(), cliente)) return false;

            String totalFilter = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!totalFilter.isEmpty()) {
                String totalStr = String.valueOf(sale.getTotal().longValue());
                if (!totalStr.startsWith(totalFilter)) return false;
            }

            if (!matchCombo(comboEstado, sale.getStatus())) return false;
            if (!matchCombo(comboFormaPago, sale.getPaymentType())) return false;
            if (!matchCombo(comboMedioPago, sale.getPaymentMethod())) return false;
            if (outOfRange(sale.getSaleDate(), dpCreacionDesde.getValue(), dpCreacionHasta.getValue())) return false;
            if (outOfRange(sale.getDueDate(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;

            return true;
        });

        updateRecordsLabel();
    }

    private boolean matchText(String filtro, String valor) {
        return filtro == null || filtro.isEmpty() || (valor != null && valor.toLowerCase().contains(filtro.toLowerCase()));
    }

    private boolean matchCombo(ComboBox<String> combo, String valorReal) {
        String seleccion = combo.getValue();
        if (seleccion == null || seleccion.isEmpty() || seleccion.startsWith("Tod")) return true;
        return valorReal != null && valorReal.equalsIgnoreCase(seleccion);
    }

    private boolean outOfRange(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return true;
        if (desde != null && fecha.isBefore(desde)) return true;
        if (hasta != null && fecha.isAfter(hasta)) return true;
        return false;
    }

    private void updateRecordsLabel() {
        if (lblInfoRegistros != null)
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
    }

    private void setupColumns() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        styleTextColumn(colNumero);

        colCliente.setCellValueFactory(cell -> {
            if (cell.getValue().getCustomer() != null)
                return new SimpleStringProperty(cell.getValue().getCustomer().getName());
            return new SimpleStringProperty("N/A");
        });
        styleTextColumn(colCliente);

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

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        colFechaCreacion.setCellFactory(col -> createDateCell());

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colFechaVencimiento.setCellFactory(col -> createDateCell());

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> createCurrencyCell(false));

        colPorCobrar.setCellValueFactory(cell -> {
            Sale v = cell.getValue();
            Double deuda;
            if ("ANULADA".equalsIgnoreCase(v.getStatus())) {
                deuda = 0.0;
            } else if (v.getPendingBalance() != null) {
                deuda = v.getPendingBalance();
            } else {
                deuda = "PAGADA".equalsIgnoreCase(v.getStatus()) ? 0.0 : v.getTotal();
            }
            return new SimpleObjectProperty<>(deuda);
        });
        colPorCobrar.setCellFactory(col -> createCurrencyCell(true));

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
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private TableCell<Sale, LocalDate> createDateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(dateFormatter.format(item));
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
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

    private TableCell<Sale, Double> createCurrencyCell(boolean esDeuda) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor = Color.BLACK;
                    if (esDeuda) baseColor = (item > 0) ? Color.RED : COLOR_VERDE_OSCURO;
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private void setupListeners() {
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

        btnRegistrarPago.setDisable(true);
        btnVerDetalle.setDisable(true);

        tablaFacturas.getSelectionModel().selectedItemProperty().addListener((obs, old, seleccion) -> {
            boolean haySeleccion = (seleccion != null);
            btnVerDetalle.setDisable(!haySeleccion);

            boolean puedePagar = false;
            if (haySeleccion) {
                double deuda;
                if ("ANULADA".equalsIgnoreCase(seleccion.getStatus())) {
                    deuda = 0.0;
                } else if (seleccion.getPendingBalance() != null) {
                    deuda = seleccion.getPendingBalance();
                } else {
                    deuda = "PAGADA".equalsIgnoreCase(seleccion.getStatus()) ? 0.0 : seleccion.getTotal();
                }
                puedePagar = (deuda > 0)
                        && !"PAGADA".equalsIgnoreCase(seleccion.getStatus())
                        && !"ANULADA".equalsIgnoreCase(seleccion.getStatus());
            }
            btnRegistrarPago.setDisable(!puedePagar);
        });
    }

    private void setupInteractionListeners() {
        tablaFacturas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Sale seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) openHistory(seleccionada);
            }
        });

        tablaFacturas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Sale seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) { openHistory(seleccionada); event.consume(); }
            }
        });
    }

    @FXML void btnBuscarClick(ActionEvent event) { applyFilters(); }

    @FXML
    void btnRegistrarPagoClick(ActionEvent event) {
        Sale seleccion = tablaFacturas.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            ToastNotification.warning(tablaFacturas, "Selecciona una factura para registrar el pago");
            return;
        }
        if ("PAGADA".equalsIgnoreCase(seleccion.getStatus())) {
            ToastNotification.warning(tablaFacturas, "Esta factura ya está completamente pagada");
            return;
        }
        if ("ANULADA".equalsIgnoreCase(seleccion.getStatus())) {
            ToastNotification.warning(tablaFacturas, "No se puede registrar un pago en una factura anulada");
            return;
        }
        openCollectionModal(seleccion);
    }

    private void openCollectionModal(Sale sale) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/sales/views/CollectionForm.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            CollectionFormController controller = loader.getController();
            controller.setSale(sale);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaFacturas.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);
            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());
            ventanaPrincipal.widthProperty().addListener((obs, oldV, newV) -> modalStage.setWidth(newV.doubleValue()));
            ventanaPrincipal.heightProperty().addListener((obs, oldV, newV) -> modalStage.setHeight(newV.doubleValue()));

            modalStage.showAndWait();

            if (controller.isSaved()) {
                String numFactura = sale.getInvoiceNumber();
                loadDataFromDB();
                ToastNotification.success(
                    MainLayoutController.getInstance().getContentArea(),
                    "Pago registrado en factura " + numFactura + " correctamente"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaFacturas, "No se pudo abrir el formulario de pago");
        }
    }

    @FXML
    void btnVerDetalleClick(ActionEvent event) {
        Sale seleccion = tablaFacturas.getSelectionModel().getSelectedItem();
        if (seleccion != null) openHistory(seleccion);
        else ToastNotification.warning(tablaFacturas, "Selecciona una factura para ver el historial");
    }

    private void openHistory(Sale sale) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/sales/views/CollectionHistory.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            CollectionHistoryController controller = loader.getController();
            controller.setSale(sale);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaFacturas.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);
            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());
            ventanaPrincipal.widthProperty().addListener((obs, oldV, newV) -> modalStage.setWidth(newV.doubleValue()));
            ventanaPrincipal.heightProperty().addListener((obs, oldV, newV) -> modalStage.setHeight(newV.doubleValue()));

            modalStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaFacturas, "No se pudo abrir el historial de cobros");
        }
    }

    private void configurarFormatosFecha() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? dateFormatter.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, dateFormatter) : null; }
        };
        dpCreacionDesde.setConverter(c); dpCreacionHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }

}
