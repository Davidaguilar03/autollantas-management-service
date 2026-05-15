package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
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

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("CallToPrintStackTrace")
@Component
public class PurchaseInvoicesController {

    @Autowired private PurchasesService purchasesService;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtNumero;
    @FXML private TextField txtProveedor;
    @FXML private TextField txtTotal;

    @FXML private ComboBox<String> comboEstado;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;

    @FXML private DatePicker dpCreacionDesde;
    @FXML private DatePicker dpCreacionHasta;
    @FXML private DatePicker dpVencimientoDesde;
    @FXML private DatePicker dpVencimientoHasta;

    @FXML private TableView<Purchase> tablaFacturasCompra;
    @FXML private TableColumn<Purchase, String> colNumero;
    @FXML private TableColumn<Purchase, String> colProveedor;
    @FXML private TableColumn<Purchase, LocalDate> colFechaCreacion;
    @FXML private TableColumn<Purchase, LocalDate> colFechaVencimiento;

    @FXML private TableColumn<Purchase, String> colFormaPago;
    @FXML private TableColumn<Purchase, String> colMetodoPago;
    @FXML private TableColumn<Purchase, String> colCuenta;

    @FXML private TableColumn<Purchase, Double> colTotal;
    @FXML private TableColumn<Purchase, Double> colPagado;
    @FXML private TableColumn<Purchase, Double> colPorPagar;
    @FXML private TableColumn<Purchase, String> colEstado;

    @FXML private Button btnVerDetalles;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Purchase> masterData = FXCollections.observableArrayList();
    private FilteredList<Purchase> filteredData;

    @SuppressWarnings("deprecation")
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Purchase> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaFacturasCompra.comparatorProperty());
        tablaFacturasCompra.setItems(sortedData);

        configureColumns();
        setupFilterUI();
        setupDateFormatters();
        configureListeners();
        setupTableInteractions();
        loadDataFromDB();
    }

    private void loadDataFromDB() {
        if (purchasesService == null) return;
        Platform.runLater(() -> {
            try {
                List<Purchase> list = purchasesService.findAllPurchases();
                masterData.setAll(list);
                updateRecordsLabel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateRecordsLabel() {
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
            return new SimpleStringProperty("Sin Proveedor");
        });
        styleTextColumn(colProveedor, Pos.CENTER);

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        colFechaCreacion.setCellFactory(col -> createDateCell());

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colFechaVencimiento.setCellFactory(col -> createDateCell());

        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        styleTextColumn(colFormaPago, Pos.CENTER);

        colMetodoPago.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        styleTextColumn(colMetodoPago, Pos.CENTER);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null)
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            return new SimpleStringProperty("-");
        });
        styleTextColumn(colCuenta, Pos.CENTER);

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> createCurrencyCell(false));

        colPagado.setCellValueFactory(cell -> {
            Purchase p = cell.getValue();
            double paid = "PAGADA".equalsIgnoreCase(p.getStatus()) ? p.getTotal() : 0.0;
            return new SimpleObjectProperty<>(paid);
        });
        colPagado.setCellFactory(col -> createCurrencyCell(false));

        colPorPagar.setCellValueFactory(cell -> {
            Purchase p = cell.getValue();
            double pending = "PENDIENTE".equalsIgnoreCase(p.getStatus()) ? p.getTotal() : 0.0;
            return new SimpleObjectProperty<>(pending);
        });
        colPorPagar.setCellFactory(col -> createCurrencyCell(true));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEstado.setCellFactory(col -> createStatusCell());
    }

    @FXML
    void btnEliminarClick(ActionEvent event) {
        Purchase sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if (sel != null) {
            if ("ANULADA".equalsIgnoreCase(sel.getStatus())) {
                showAlert("Acción no permitida", "Esta factura ya se encuentra anulada.");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Anular Factura");
            alert.setHeaderText("Va a anular la factura " + sel.getInvoiceNumber());
            alert.setContentText("Esta acción revertirá los productos del inventario y cambiará su estado a ANULADA.");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    purchasesService.cancelPurchase(sel);
                    tablaFacturasCompra.refresh();
                    applyFilters();
                    showAlert("Éxito", "Compra anulada y stock revertido correctamente.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "No se pudo anular: " + e.getMessage());
                }
            }
        }
    }

    private void styleTextColumn(TableColumn<Purchase, String> col, Pos alignment) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(alignment);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(alignment);
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
                    Label lbl = new Label(dateFormatter.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<Purchase, Double> createCurrencyCell(boolean isDebt) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor = Color.BLACK;
                    if (isDebt) baseColor = (item > 0) ? Color.RED : COLOR_VERDE_OSCURO;
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
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

    @FXML
    void btnNuevaFacturaClick(ActionEvent event) {
        openForm();
    }

    public void openForm() {
        MainLayoutController.getInstance().loadView("/com/autollantas/gestion/purchases/views/PurchaseForm.fxml");
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        Purchase sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if (sel != null) {
            if ("ANULADA".equalsIgnoreCase(sel.getStatus())) {
                showAlert("Acción no permitida", "No se puede editar una factura anulada.");
                return;
            }
            Object controllerObj = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/purchases/views/PurchaseForm.fxml");
            if (controllerObj instanceof PurchaseFormController) {
                ((PurchaseFormController) controllerObj).setPurchaseForEditing(sel);
            }
        } else {
            showAlert("Selección requerida", "Seleccione una compra para editar.");
        }
    }

    @FXML
    void btnVerDetallesClick(ActionEvent event) {
        Purchase sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if (sel != null) openDetailsModal(sel);
    }

    private void openDetailsModal(Purchase purchase) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/purchases/views/PurchaseDetails.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            PurchaseDetailsController controller = loader.getController();
            controller.setPurchase(purchase);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage mainStage = (Stage) tablaFacturasCompra.getScene().getWindow();
            modalStage.initOwner(mainStage);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ESCAPE) modalStage.close(); });

            modalStage.setScene(scene);
            modalStage.setX(mainStage.getX());
            modalStage.setY(mainStage.getY());
            modalStage.setWidth(mainStage.getWidth());
            modalStage.setHeight(mainStage.getHeight());
            modalStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el detalle: " + e.getMessage());
        }
    }

    private void configureListeners() {
        InvalidationListener listener = obs -> applyFilters();
        txtNumero.textProperty().addListener(listener);
        txtProveedor.textProperty().addListener(listener);
        txtTotal.textProperty().addListener(listener);
        comboEstado.valueProperty().addListener(listener);
        comboFormaPago.valueProperty().addListener(listener);
        comboMedioPago.valueProperty().addListener(listener);
        dpCreacionDesde.valueProperty().addListener(listener);
        dpCreacionHasta.valueProperty().addListener(listener);
        dpVencimientoDesde.valueProperty().addListener(listener);
        dpVencimientoHasta.valueProperty().addListener(listener);

        btnVerDetalles.setDisable(true);
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        tablaFacturasCompra.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = (selected != null);
            btnVerDetalles.setDisable(!hasSelection);
            btnEditar.setDisable(!hasSelection);
            btnEliminar.setDisable(!hasSelection);
        });
    }

    private void setupTableInteractions() {
        tablaFacturasCompra.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Purchase sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
                if (sel != null) { openDetailsModal(sel); event.consume(); }
            }
        });
        tablaFacturasCompra.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Purchase sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
                if (sel != null) openDetailsModal(sel);
            }
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(p -> {
            String statusFilter = comboEstado.getValue();
            String actualStatus = p.getStatus();

            if ("PENDIENTE".equals(statusFilter)) {
                if (!"PENDIENTE".equalsIgnoreCase(actualStatus)) return false;
            } else if ("PAGADA".equals(statusFilter)) {
                if (!"PAGADA".equalsIgnoreCase(actualStatus)) return false;
            } else if ("ANULADA".equals(statusFilter)) {
                if (!"ANULADA".equalsIgnoreCase(actualStatus)) return false;
            }

            if (!matchText(txtNumero.getText(), p.getInvoiceNumber())) return false;
            String supplierName = (p.getSupplier() != null) ? p.getSupplier().getName() : "";
            if (!matchText(txtProveedor.getText(), supplierName)) return false;

            String totalFilter = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!totalFilter.isEmpty()) {
                String totalStr = String.valueOf(p.getTotal().longValue());
                if (!totalStr.startsWith(totalFilter)) return false;
            }

            if (!matchCombo(comboFormaPago, p.getPaymentType())) return false;
            if (!matchCombo(comboMedioPago, p.getPaymentMethod())) return false;
            if (outOfRange(p.getPurchaseDate(), dpCreacionDesde.getValue(), dpCreacionHasta.getValue())) return false;
            if (outOfRange(p.getDueDate(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;

            return true;
        });
        updateRecordsLabel();
    }

    private boolean matchText(String filter, String value) {
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

    private void setupFilterUI() {
        comboEstado.getItems().clear();
        comboEstado.getItems().addAll("Todas", "PENDIENTE", "PAGADA", "ANULADA");
        comboEstado.getSelectionModel().selectFirst();

        comboFormaPago.getItems().clear();
        comboFormaPago.getItems().addAll("Todas", "Contado", "Crédito");
        comboFormaPago.getSelectionModel().selectFirst();

        comboMedioPago.getItems().clear();
        comboMedioPago.getItems().addAll("Todos", "Efectivo", "Transferencia", "Tarjeta");
        comboMedioPago.getSelectionModel().selectFirst();
    }

    private void setupDateFormatters() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? dateFormatter.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, dateFormatter) : null; }
        };
        dpCreacionDesde.setConverter(c); dpCreacionHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gestión de Compras");
            alert.setHeaderText(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    @FXML void btnBuscarClick(ActionEvent event) { applyFilters(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtNumero.clear(); txtProveedor.clear(); txtTotal.clear();
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.getSelectionModel().selectFirst();
        dpCreacionDesde.setValue(null); dpCreacionHasta.setValue(null);
        dpVencimientoDesde.setValue(null); dpVencimientoHasta.setValue(null);
        applyFilters();
    }
}
