package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.service.PurchasesService;
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
import javafx.geometry.Insets;
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

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
@Component
public class PaymentsController {

    @Autowired private PurchasesService purchasesService;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtNumero;
    @FXML private TextField txtProveedor;
    @FXML private TextField txtTotal;

    @FXML private ComboBox<String> comboEstado;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;

    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;
    @FXML private DatePicker dpVencimientoDesde;
    @FXML private DatePicker dpVencimientoHasta;

    @FXML private TableView<Purchase> tablaPagos;
    @FXML private TableColumn<Purchase, String> colNumero;
    @FXML private TableColumn<Purchase, String> colProveedor;
    @FXML private TableColumn<Purchase, LocalDate> colFechaCreacion;
    @FXML private TableColumn<Purchase, LocalDate> colFechaVencimiento;
    @FXML private TableColumn<Purchase, String> colFormaPago;
    @FXML private TableColumn<Purchase, String> colMedioPago;
    @FXML private TableColumn<Purchase, String> colCuenta;
    @FXML private TableColumn<Purchase, Double> colTotal;
    @FXML private TableColumn<Purchase, Double> colPagado;
    @FXML private TableColumn<Purchase, Double> colPorPagar;
    @FXML private TableColumn<Purchase, String> colEstado;

    @FXML private Button btnVerDetalle;
    @FXML private Button btnRegistrarPago;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Purchase> masterData = FXCollections.observableArrayList();
    private FilteredList<Purchase> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Purchase> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaPagos.comparatorProperty());
        tablaPagos.setItems(sortedData);

        configureColumns();
        setupFilterUI();
        setupDateConverters();
        setupSearchListeners();
        setupTableInteractions();
        loadDataFromDB();

        btnVerDetalle.disableProperty().bind(
                tablaPagos.getSelectionModel().selectedItemProperty().isNull()
        );

        if (btnRegistrarPago != null) {
            tablaPagos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                boolean disable = true;
                if (newSel != null) {
                    boolean paid = "PAGADA".equalsIgnoreCase(newSel.getStatus());
                    boolean cancelled = "ANULADA".equalsIgnoreCase(newSel.getStatus());
                    double pendingBalance = 0.0;
                    if (cancelled) pendingBalance = 0.0;
                    else if (newSel.getPendingBalance() != null) pendingBalance = newSel.getPendingBalance();
                    else pendingBalance = paid ? 0.0 : newSel.getTotal();

                    if (!paid && !cancelled && pendingBalance > 0) disable = false;
                }
                btnRegistrarPago.setDisable(disable);
            });
        }

        tablaPagos.setOnMouseClicked(event -> {
            javafx.scene.Node clickedNode = event.getPickResult().getIntersectedNode();
            boolean validRow = false;
            while (clickedNode != null && clickedNode != tablaPagos) {
                if (clickedNode instanceof TableRow) {
                    TableRow<?> row = (TableRow<?>) clickedNode;
                    if (!row.isEmpty()) validRow = true;
                    break;
                }
                clickedNode = clickedNode.getParent();
            }
            if (!validRow) {
                tablaPagos.getSelectionModel().clearSelection();
            } else if (event.getClickCount() == 2) {
                Purchase selected = tablaPagos.getSelectionModel().getSelectedItem();
                if (selected != null) openPaymentHistory(selected);
            }
        });
    }

    private void setupTableInteractions() {
        tablaPagos.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Purchase selected = tablaPagos.getSelectionModel().getSelectedItem();
                if (selected != null) { openPaymentHistory(selected); event.consume(); }
            }
        });
    }

    private void loadDataFromDB() {
        if (purchasesService == null) return;
        Platform.runLater(() -> {
            try {
                List<Purchase> list = purchasesService.findAllPurchases();
                masterData.setAll(list);
                updateRecordsInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void btnRegistrarPagoClick(ActionEvent event) {
        Purchase selected = tablaPagos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if ("PAGADA".equalsIgnoreCase(selected.getStatus())) {
            showAlert("Factura Saldada", "Esta factura ya está totalmente pagada.");
            return;
        }
        if ("ANULADA".equalsIgnoreCase(selected.getStatus())) {
            showAlert("Información", "No se puede registrar un pago para una factura anulada.");
            return;
        }

        openPaymentForm(selected);
    }

    @FXML
    void btnVerDetalleClick(ActionEvent event) {
        Purchase selected = tablaPagos.getSelectionModel().getSelectedItem();
        if (selected != null) openPaymentHistory(selected);
    }

    private void openPaymentForm(Purchase purchase) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/purchases/views/PaymentForm.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            PaymentFormController controller = loader.getController();
            controller.setPurchase(purchase);

            showModal(root, controller);

            if (controller.isSaved()) loadDataFromDB();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo cargar la ventana de pagos: " + e.getMessage());
        }
    }

    private void openPaymentHistory(Purchase purchase) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/purchases/views/PaymentHistory.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            PaymentHistoryController controller = loader.getController();
            controller.setPurchase(purchase);

            showModal(root, null);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo cargar el historial: " + e.getMessage());
        }
    }

    private void showModal(Parent root, Object controller) {
        Stage modalStage = new Stage();
        modalStage.initStyle(StageStyle.TRANSPARENT);
        modalStage.initModality(Modality.APPLICATION_MODAL);

        Stage mainStage = (Stage) tablaPagos.getScene().getWindow();
        modalStage.initOwner(mainStage);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ESCAPE) modalStage.close(); });

        modalStage.setScene(scene);
        modalStage.setX(mainStage.getX());
        modalStage.setY(mainStage.getY());
        modalStage.setWidth(mainStage.getWidth());
        modalStage.setHeight(mainStage.getHeight());

        mainStage.widthProperty().addListener((obs, oldV, newV) -> modalStage.setWidth(newV.doubleValue()));
        mainStage.heightProperty().addListener((obs, oldV, newV) -> modalStage.setHeight(newV.doubleValue()));
        mainStage.xProperty().addListener((obs, oldV, newV) -> modalStage.setX(newV.doubleValue()));
        mainStage.yProperty().addListener((obs, oldV, newV) -> modalStage.setY(newV.doubleValue()));

        modalStage.showAndWait();
    }

    private void updateRecordsInfo() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configureColumns() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        styleTextColumn(colNumero, Pos.CENTER);

        colProveedor.setCellValueFactory(cell -> {
            if (cell.getValue().getSupplier() != null)
                return new SimpleStringProperty(cell.getValue().getSupplier().getName());
            return new SimpleStringProperty("-");
        });
        styleTextColumn(colProveedor, Pos.CENTER);

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        colFechaCreacion.setCellFactory(col -> createDateCell());

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colFechaVencimiento.setCellFactory(col -> createDateCell());

        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        styleTextColumn(colFormaPago, Pos.CENTER);

        colMedioPago.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        styleTextColumn(colMedioPago, Pos.CENTER);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null)
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            return new SimpleStringProperty("N/A");
        });
        styleTextColumn(colCuenta, Pos.CENTER);

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> createCurrencyCell(false));

        colPagado.setCellValueFactory(cell -> {
            Purchase p = cell.getValue();
            if ("ANULADA".equalsIgnoreCase(p.getStatus())) return new SimpleObjectProperty<>(0.0);
            double total = p.getTotal() != null ? p.getTotal() : 0.0;
            double pending = (p.getPendingBalance() != null) ? p.getPendingBalance() : total;
            if ("PAGADA".equalsIgnoreCase(p.getStatus())) pending = 0.0;
            else if (p.getPendingBalance() == null) pending = total;
            return new SimpleObjectProperty<>(total - pending);
        });
        colPagado.setCellFactory(col -> createCurrencyCell(false));

        colPorPagar.setCellValueFactory(cell -> {
            Purchase p = cell.getValue();
            double pending;
            if ("ANULADA".equalsIgnoreCase(p.getStatus())) pending = 0.0;
            else if ("PAGADA".equalsIgnoreCase(p.getStatus())) pending = 0.0;
            else pending = (p.getPendingBalance() != null) ? p.getPendingBalance() : p.getTotal();
            return new SimpleObjectProperty<>(pending);
        });
        colPorPagar.setCellFactory(col -> createCurrencyCell(true));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEstado.setCellFactory(col -> createStatusCell());
    }

    private void styleTextColumn(TableColumn<Purchase, String> col, Pos pos) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(pos);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(pos);
                    setPadding(Insets.EMPTY);
                }
            }
        });
    }

    private TableCell<Purchase, LocalDate> createDateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(dateFormat.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<Purchase, Double> createCurrencyCell(boolean useDebtColor) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    Color baseColor = Color.BLACK;
                    if (useDebtColor) {
                        lbl.setStyle("-fx-font-weight: bold;");
                        baseColor = (item > 0) ? Color.RED : COLOR_VERDE_OSCURO;
                    }
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                    setPadding(Insets.EMPTY);
                }
            }
        };
    }

    private TableCell<Purchase, String> createStatusCell() {
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
        javafx.beans.value.ChangeListener<Object> listener = (obs, oldVal, newVal) -> applyFilters();
        txtNumero.textProperty().addListener(listener);
        txtProveedor.textProperty().addListener(listener);
        txtTotal.textProperty().addListener(listener);
        comboEstado.valueProperty().addListener(listener);
        comboFormaPago.valueProperty().addListener(listener);
        comboMedioPago.valueProperty().addListener(listener);
        dpFechaDesde.valueProperty().addListener(listener);
        dpFechaHasta.valueProperty().addListener(listener);
        dpVencimientoDesde.valueProperty().addListener(listener);
        dpVencimientoHasta.valueProperty().addListener(listener);
    }

    private void applyFilters() {
        filteredData.setPredicate(p -> {
            if (!matchText(p.getInvoiceNumber(), txtNumero.getText())) return false;
            String supplierName = (p.getSupplier() != null) ? p.getSupplier().getName() : "";
            if (!matchText(supplierName, txtProveedor.getText())) return false;
            String totalInput = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!totalInput.isEmpty() && !String.valueOf(p.getTotal().longValue()).startsWith(totalInput)) return false;
            if (!matchCombo(comboEstado, p.getStatus())) return false;
            if (!matchCombo(comboFormaPago, p.getPaymentType())) return false;
            if (!matchCombo(comboMedioPago, p.getPaymentMethod())) return false;
            if (outOfRange(p.getPurchaseDate(), dpFechaDesde.getValue(), dpFechaHasta.getValue())) return false;
            if (outOfRange(p.getDueDate(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;
            return true;
        });
        updateRecordsInfo();
    }

    private boolean matchText(String value, String filter) {
        return filter == null || filter.isEmpty() || (value != null && value.toLowerCase().contains(filter.toLowerCase()));
    }

    private boolean matchCombo(ComboBox<String> combo, String value) {
        String sel = combo.getValue();
        return sel == null || sel.equals("Todas") || sel.equals("Todos") || (value != null && value.equalsIgnoreCase(sel));
    }

    private boolean outOfRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return true;
        if (from != null && date.isBefore(from)) return true;
        if (to != null && date.isAfter(to)) return true;
        return false;
    }

    @FXML void btnBuscarClick(ActionEvent event) { applyFilters(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtNumero.clear(); txtProveedor.clear(); txtTotal.clear();
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.getSelectionModel().selectFirst();
        dpFechaDesde.setValue(null); dpFechaHasta.setValue(null);
        dpVencimientoDesde.setValue(null); dpVencimientoHasta.setValue(null);
        applyFilters();
    }

    private void setupDateConverters() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? dateFormat.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, dateFormat) : null; }
        };
        dpFechaDesde.setConverter(c); dpFechaHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }

    private void setupFilterUI() {
        comboEstado.setItems(FXCollections.observableArrayList("Todos", "PENDIENTE", "PAGADA", "ANULADA"));
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.setItems(FXCollections.observableArrayList("Todas", "Crédito", "Contado"));
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.setItems(FXCollections.observableArrayList("Todos", "Efectivo", "Transferencia", "Cheque", "Tarjeta"));
        comboMedioPago.getSelectionModel().selectFirst();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Gestión de Compras");
        a.setHeaderText(title);
        a.setContentText(content);
        a.showAndWait();
    }
}
