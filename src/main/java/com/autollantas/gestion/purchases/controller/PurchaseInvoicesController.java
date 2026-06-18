package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    @FXML private TableColumn<Purchase, Double> colIvaFavor;
    @FXML private TableColumn<Purchase, String> colEstado;

    @FXML private Button btnNuevaCompra;
    @FXML private Button btnVerDetalles;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnPapelera;
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
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
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
        actualizarBotonNuevaCompra();
    }

    public void actualizarBotonNuevaCompra() {
        if (btnNuevaCompra == null) return;
        boolean hayCache = MainLayoutController.getInstance().hasCachedPurchaseForm();
        if (hayCache) {
            btnNuevaCompra.setText("Continuar Compra");
            btnNuevaCompra.getStyleClass().removeAll("mini-card-green", "mini-card-btn");
            btnNuevaCompra.getStyleClass().addAll("mini-card-btn", "mini-card-blue");
            if (btnNuevaCompra.getGraphic() instanceof StackPane sp) {
                sp.getStyleClass().removeAll("mini-card-chip-green");
                sp.getStyleClass().add("mini-card-chip-blue");
                sp.getChildren().stream()
                    .filter(n -> n instanceof Label).map(n -> (Label) n)
                    .findFirst().ifPresent(lbl -> lbl.setText("↩"));
            }
        } else {
            btnNuevaCompra.setText("Nueva Compra");
            btnNuevaCompra.getStyleClass().removeAll("mini-card-blue", "mini-card-btn");
            btnNuevaCompra.getStyleClass().addAll("mini-card-btn", "mini-card-green");
            if (btnNuevaCompra.getGraphic() instanceof StackPane sp) {
                sp.getStyleClass().removeAll("mini-card-chip-blue");
                sp.getStyleClass().add("mini-card-chip-green");
                sp.getChildren().stream()
                    .filter(n -> n instanceof Label).map(n -> (Label) n)
                    .findFirst().ifPresent(lbl -> lbl.setText("＋"));
            }
        }
    }

    private void loadDataFromDB() {
        if (purchasesService == null) return;
        Platform.runLater(() -> {
            try {
                List<Purchase> list = purchasesService.findAllPurchases();
                masterData.setAll(list);
                applyFilters();
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

        colIvaFavor.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(purchasesService.calculateIvaFavor(cell.getValue()))
        );
        colIvaFavor.setCellFactory(col -> createCurrencyCell(false));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEstado.setCellFactory(col -> createStatusCell());
    }

    @FXML
    void btnEliminarClick(ActionEvent event) {
        Purchase sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if ("ANULADA".equalsIgnoreCase(sel.getStatus())) {
            ToastNotification.warning(tablaFacturasCompra, "La factura " + sel.getInvoiceNumber() + " ya está anulada");
            return;
        }

        CustomDialog.danger(tablaFacturasCompra,
            "Anular factura " + sel.getInvoiceNumber(),
            "Esta acción es irreversible. Se anulará la compra al proveedor "
                + (sel.getSupplier() != null ? sel.getSupplier().getName() : "")
                + " por " + currencyFormat.format(sel.getTotal())
                + " y las unidades compradas serán descontadas del inventario automáticamente.",
            () -> {
                try {
                    purchasesService.cancelPurchase(sel);
                    tablaFacturasCompra.refresh();
                    applyFilters();
                    ToastNotification.success(tablaFacturasCompra,
                        "Factura " + sel.getInvoiceNumber() + " anulada · stock revertido al inventario");
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastNotification.error(tablaFacturasCompra, "No se pudo anular la factura");
                }
            },
            null);
    }

    @FXML void btnPapeleraClick(ActionEvent event) {
        List<Purchase> anuladas = purchasesService.findAllPurchases().stream()
            .filter(p -> "ANULADA".equalsIgnoreCase(p.getStatus()))
            .toList();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);

        Stage owner = (Stage) tablaFacturasCompra.getScene().getWindow();
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
        Label titulo = new Label("Papelera — Compras Anuladas");
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

        Button btnRecu = new Button("Recuperar Compra");
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
            Label vacio = new Label("No hay compras anuladas en la papelera");
            vacio.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6; -fx-padding: 40;");
            vacio.setAlignment(Pos.CENTER);
            vacio.setMaxWidth(Double.MAX_VALUE);
            botonesBox.getChildren().add(btnCerrar);
            card.getChildren().addAll(header, sep1, vacio, sep2, botonesBox);
        } else {
            TableView<Purchase> tabla = new TableView<>();
            tabla.setStyle("-fx-border-color: #ecf0f1;");
            VBox.setVgrow(tabla, Priority.ALWAYS);

            TableColumn<Purchase, String> colNum = new TableColumn<>("Nro. Factura");
            colNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInvoiceNumber()));
            colNum.setPrefWidth(150);

            TableColumn<Purchase, String> colProv = new TableColumn<>("Proveedor");
            colProv.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSupplier() != null ? c.getValue().getSupplier().getName() : ""));
            colProv.setPrefWidth(220);

            TableColumn<Purchase, String> colFech = new TableColumn<>("Fecha");
            colFech.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPurchaseDate() != null ? c.getValue().getPurchaseDate().toString() : ""));
            colFech.setPrefWidth(110);

            TableColumn<Purchase, String> colTot = new TableColumn<>("Total");
            colTot.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTotal() != null
                    ? "$ " + String.format("%,.0f", c.getValue().getTotal()) : "$ 0"));
            colTot.setPrefWidth(130);
            colTot.setStyle("-fx-alignment: CENTER-RIGHT;");

            TableColumn<Purchase, String> colEst = new TableColumn<>("Estado");
            colEst.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
            colEst.setPrefWidth(100);

            tabla.getColumns().addAll(colNum, colProv, colFech, colTot, colEst);
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tabla.setItems(FXCollections.observableArrayList(anuladas));

            tabla.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                boolean hay = sel != null;
                btnVerDet.setDisable(!hay);
                btnRecu.setDisable(!hay);
            });

            btnVerDet.setOnAction(e -> {
                Purchase sel = tabla.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    modal.close();
                    openDetailsModal(sel);
                }
            });

            btnRecu.setOnAction(e -> {
                Purchase sel = tabla.getSelectionModel().getSelectedItem();
                if (sel == null) return;
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Recuperar Compra");
                confirm.setHeaderText("¿Recuperar la compra " + sel.getInvoiceNumber() + "?");
                confirm.setContentText("La compra volverá a estado PENDIENTE.");
                confirm.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.OK) {
                        purchasesService.restorePurchase(sel);
                        tabla.getItems().remove(sel);
                        loadDataFromDB();
                        ToastNotification.success(tablaFacturasCompra, "Compra recuperada correctamente");
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
            if (ke.getCode() == KeyCode.ESCAPE) modal.close();
        });
        modal.setScene(scene);
        modal.setX(owner.getX());
        modal.setY(owner.getY());
        modal.setWidth(owner.getWidth());
        modal.setHeight(owner.getHeight());
        modal.show();
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
        if (sel == null) {
            ToastNotification.warning(tablaFacturasCompra, "Selecciona una factura para editar");
            return;
        }
        if ("ANULADA".equalsIgnoreCase(sel.getStatus())) {
            ToastNotification.warning(tablaFacturasCompra, "No se puede editar una factura anulada");
            return;
        }
        Object controllerObj = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/purchases/views/PurchaseForm.fxml");
        if (controllerObj instanceof PurchaseFormController) {
            ((PurchaseFormController) controllerObj).setPurchaseForEditing(sel);
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
            ToastNotification.error(tablaFacturasCompra, "No se pudo abrir el detalle de la factura");
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
            if ("ANULADA".equalsIgnoreCase(p.getStatus())) return false;
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
