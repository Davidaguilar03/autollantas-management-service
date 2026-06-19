package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

@SuppressWarnings("ALL")
@Component
@Scope("prototype")
public class SaleInvoicesController {

    @Autowired private SalesService salesService;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtNumero;
    @FXML private TextField txtCliente;
    @FXML private TextField txtTotalMin;
    @FXML private TextField txtTotalMax;
    @FXML private ComboBox<String> comboEstado;
    @FXML private ComboBox<String> comboFormaPago;

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
    @FXML private TableColumn<Sale, Double> colDiferenciaIva;
    @FXML private TableColumn<Sale, Double> colUtilidad;
    @FXML private TableColumn<Sale, String> colEstado;

    @FXML private Button btnNuevaFactura;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnPapelera;
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
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
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
        actualizarBotonNuevaFactura();
    }

    public void actualizarBotonNuevaFactura() {
        if (btnNuevaFactura == null) return;
        boolean hayCache = MainLayoutController.getInstance().hasCachedSaleForm();
        if (hayCache) {
            btnNuevaFactura.setText("Continuar Factura");
            btnNuevaFactura.getStyleClass().removeAll("mini-card-green", "mini-card-btn");
            btnNuevaFactura.getStyleClass().addAll("mini-card-btn", "mini-card-blue");
            if (btnNuevaFactura.getGraphic() instanceof StackPane sp) {
                sp.getStyleClass().removeAll("mini-card-chip-green");
                sp.getStyleClass().add("mini-card-chip-blue");
                sp.getChildren().stream()
                    .filter(n -> n instanceof Label).map(n -> (Label) n)
                    .findFirst().ifPresent(lbl -> lbl.setText("↩"));
            }
        } else {
            btnNuevaFactura.setText("Nueva Factura");
            btnNuevaFactura.getStyleClass().removeAll("mini-card-blue", "mini-card-btn");
            btnNuevaFactura.getStyleClass().addAll("mini-card-btn", "mini-card-green");
            if (btnNuevaFactura.getGraphic() instanceof StackPane sp) {
                sp.getStyleClass().removeAll("mini-card-chip-blue");
                sp.getStyleClass().add("mini-card-chip-green");
                sp.getChildren().stream()
                    .filter(n -> n instanceof Label).map(n -> (Label) n)
                    .findFirst().ifPresent(lbl -> lbl.setText("＋"));
            }
        }
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
            ToastNotification.warning(tablaFacturas, "Selecciona una factura para editar");
            return;
        }

        if ("ANULADA".equalsIgnoreCase(seleccionada.getStatus())) {
            ToastNotification.error(tablaFacturas, "No se puede editar una factura anulada");
            return;
        }

        Object controllerObj = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/sales/views/SaleForm.fxml");
        if (controllerObj instanceof SaleFormController formularioController) {
            formularioController.setSaleForEditing(seleccionada);
        }
    }

    @FXML
    void btnAnularClick(ActionEvent event) {
        Sale sel = tablaFacturas.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if ("ANULADA".equalsIgnoreCase(sel.getStatus())) {
            ToastNotification.warning(tablaFacturas, "Esta factura ya se encuentra anulada");
            return;
        }

        CustomDialog.danger(
            tablaFacturas,
            "Anular factura " + sel.getInvoiceNumber(),
            "Esta acción es irreversible. Se anulará la venta y las unidades vendidas serán devueltas al inventario automáticamente.",
            () -> {
                try {
                    salesService.cancelSale(sel);
                    tablaFacturas.refresh();
                    applyFilters();
                    ToastNotification.success(tablaFacturas, "Factura " + sel.getInvoiceNumber() + " anulada · stock devuelto al inventario");
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastNotification.error(tablaFacturas, "No se pudo anular la factura " + sel.getInvoiceNumber());
                }
            },
            null
        );
    }

    @FXML void btnPapeleraClick(ActionEvent event) {
        List<Sale> anuladas = salesService.findAllSales().stream()
            .filter(s -> "ANULADA".equalsIgnoreCase(s.getStatus()))
            .toList();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);

        Stage owner = (Stage) tablaFacturas.getScene().getWindow();
        modal.initOwner(owner);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 10);");
        card.setPrefWidth(1000);
        card.setPrefHeight(750);
        card.setMaxWidth(1000);
        card.setMaxHeight(750);
        card.setPadding(new Insets(20, 15, 15, 20));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("Papelera — Facturas Anuladas");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);
        Label contadorLbl = new Label(anuladas.size() + " registros");
        contadorLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        header.getChildren().addAll(titulo, spacerH, contadorLbl);

        Separator sep1 = new Separator();
        sep1.setPadding(new Insets(8, 0, 8, 0));

        Button btnVerDet = new Button("Ver Detalles");
        btnVerDet.setStyle(
            "-fx-background-color: #4db6ac; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-padding: 10 30 10 30; -fx-background-radius: 4; -fx-cursor: hand;");
        btnVerDet.setDisable(true);

        Button btnRecu = new Button("Recuperar Factura");
        btnRecu.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-padding: 10 30 10 30; -fx-background-radius: 4; -fx-cursor: hand;");
        btnRecu.setDisable(true);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle(
            "-fx-background-color: #34495e; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-padding: 10 30 10 30; -fx-background-radius: 4; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> modal.close());

        HBox botonesBox = new HBox(16);
        botonesBox.setAlignment(Pos.CENTER);
        botonesBox.setPadding(new Insets(10, 0, 0, 0));
        botonesBox.setPrefHeight(50);

        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(8, 0, 8, 0));

        if (anuladas.isEmpty()) {
            Label vacio = new Label("No hay facturas anuladas en la papelera");
            vacio.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6; -fx-padding: 40;");
            vacio.setAlignment(Pos.CENTER);
            vacio.setMaxWidth(Double.MAX_VALUE);
            botonesBox.getChildren().add(btnCerrar);
            card.getChildren().addAll(header, sep1, vacio, sep2, botonesBox);
        } else {
            TableView<Sale> tabla = new TableView<>();
            tabla.setStyle("-fx-border-color: #ecf0f1;");
            VBox.setVgrow(tabla, Priority.ALWAYS);

            TableColumn<Sale, String> colNum = new TableColumn<>("Nro. Factura");
            colNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInvoiceNumber()));
            colNum.setPrefWidth(150);

            TableColumn<Sale, String> colCli = new TableColumn<>("Cliente");
            colCli.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomer() != null ? c.getValue().getCustomer().getName() : ""));
            colCli.setPrefWidth(220);

            TableColumn<Sale, String> colFech = new TableColumn<>("Fecha");
            colFech.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSaleDate() != null ? c.getValue().getSaleDate().toString() : ""));
            colFech.setPrefWidth(110);

            TableColumn<Sale, String> colTot = new TableColumn<>("Total");
            colTot.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTotal() != null
                    ? "$ " + String.format("%,.0f", c.getValue().getTotal()) : "$ 0"));
            colTot.setPrefWidth(130);
            colTot.setStyle("-fx-alignment: CENTER-RIGHT;");

            TableColumn<Sale, String> colEst = new TableColumn<>("Estado");
            colEst.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
            colEst.setPrefWidth(100);

            tabla.getColumns().addAll(colNum, colCli, colFech, colTot, colEst);
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tabla.setItems(FXCollections.observableArrayList(anuladas));

            tabla.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                boolean hay = sel != null;
                btnVerDet.setDisable(!hay);
                btnRecu.setDisable(!hay);
            });

            btnVerDet.setOnAction(e -> {
                Sale sel = tabla.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    modal.close();
                    openDetailsModal(sel);
                }
            });

            btnRecu.setOnAction(e -> {
                Sale sel = tabla.getSelectionModel().getSelectedItem();
                if (sel == null) return;
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Recuperar Factura");
                confirm.setHeaderText("¿Recuperar la factura " + sel.getInvoiceNumber() + "?");
                confirm.setContentText("La factura volverá a estado PENDIENTE.");
                confirm.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.OK) {
                        salesService.restoreSale(sel);
                        tabla.getItems().remove(sel);
                        loadDataFromDB();
                        ToastNotification.success(tablaFacturas, "Factura recuperada correctamente");
                    }
                });
            });

            botonesBox.getChildren().addAll(btnVerDet, btnRecu, btnCerrar);
            card.getChildren().addAll(header, sep1, tabla, sep2, botonesBox);
        }

        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(card);

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(null);
        scene.setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.ESCAPE) modal.close();
        });
        modal.setScene(scene);
        modal.setX(owner.getX());
        modal.setY(owner.getY());
        modal.setWidth(owner.getWidth());
        modal.setHeight(owner.getHeight());
        modal.show();
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
            ToastNotification.error(tablaFacturas, "No se pudo abrir el detalle de la factura");
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

        colDiferenciaIva.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(salesService.calculateDiferenciaIva(cell.getValue()))
        );
        colDiferenciaIva.setCellFactory(col -> createSignedCurrencyCell());

        colUtilidad.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(salesService.calculateUtilidad(cell.getValue()))
        );
        colUtilidad.setCellFactory(col -> createSignedCurrencyCell());

        colEstado.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEstado.setCellFactory(col -> createStatusCell());
    }

    private TableCell<Sale, Double> createSignedCurrencyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER_RIGHT);
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor = item >= 0 ? COLOR_VERDE_OSCURO : Color.RED;
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
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
        txtTotalMin.textProperty().addListener(changeListener);
        txtTotalMax.textProperty().addListener(changeListener);
        comboEstado.valueProperty().addListener(changeListener);
        comboFormaPago.valueProperty().addListener(changeListener);
        dpCreacionDesde.valueProperty().addListener(changeListener);
        dpCreacionHasta.valueProperty().addListener(changeListener);
        dpVencimientoDesde.valueProperty().addListener(changeListener);
        dpVencimientoHasta.valueProperty().addListener(changeListener);
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(sale -> {
            if ("ANULADA".equalsIgnoreCase(sale.getStatus())) return false;
            String filtroEstado = comboEstado.getValue();
            String saleStatus = sale.getStatus();

            if ("PENDIENTE".equals(filtroEstado)) { if (!"PENDIENTE".equalsIgnoreCase(saleStatus)) return false; }
            else if ("PAGADA".equals(filtroEstado)) { if (!"PAGADA".equalsIgnoreCase(saleStatus)) return false; }
            else if ("ANULADA".equals(filtroEstado)) { if (!"ANULADA".equalsIgnoreCase(saleStatus)) return false; }

            if (!matchTexto(sale.getInvoiceNumber(), txtNumero.getText())) return false;

            String nombreCliente = (sale.getCustomer() != null) ? sale.getCustomer().getName() : "";
            if (!matchTexto(nombreCliente, txtCliente.getText())) return false;

            String minStr = txtTotalMin.getText().replaceAll("[^0-9]", "");
            String maxStr = txtTotalMax.getText().replaceAll("[^0-9]", "");
            if (!minStr.isEmpty() || !maxStr.isEmpty()) {
                if (sale.getTotal() == null) return false;
                long total = sale.getTotal().longValue();
                if (!minStr.isEmpty() && total < Long.parseLong(minStr)) return false;
                if (!maxStr.isEmpty() && total > Long.parseLong(maxStr)) return false;
            }

            if (!matchCombo(comboFormaPago, sale.getPaymentType())) return false;
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
        txtNumero.clear(); txtCliente.clear(); txtTotalMin.clear(); txtTotalMax.clear();
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.getSelectionModel().selectFirst();
        dpCreacionDesde.setValue(null); dpCreacionHasta.setValue(null);
        dpVencimientoDesde.setValue(null); dpVencimientoHasta.setValue(null);
    }
}
