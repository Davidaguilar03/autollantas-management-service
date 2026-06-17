package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.purchases.model.Supplier;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import com.autollantas.gestion.treasury.service.TreasuryService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Scope("prototype")
public class PurchaseFormController {

    @Autowired private PurchasesService purchasesService;
    @Autowired private InventoryService inventoryService;
    @Autowired private TreasuryService treasuryService;

    @FXML private VBox rootFormulario;
    @FXML private TextField txtNumeroFactura;

    @FXML private ComboBox<Supplier> comboProveedor;
    @FXML private TextField txtNit;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtCelular;

    @FXML private DatePicker dpFechaCompra;
    @FXML private DatePicker dpFechaVencimiento;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private TextArea txtNotas;
    @FXML private Button btnGuardar;

    @FXML private TableView<PurchaseDetailRow> tablaDetalles;
    @FXML private TableColumn<PurchaseDetailRow, Product> colCodigo;
    @FXML private TableColumn<PurchaseDetailRow, Product> colDescripcion;
    @FXML private TableColumn<PurchaseDetailRow, Integer> colCantidad;
    @FXML private TableColumn<PurchaseDetailRow, Double> colPrecio;
    @FXML private TableColumn<PurchaseDetailRow, Double> colDescuento;
    @FXML private TableColumn<PurchaseDetailRow, Double> colImpuesto;
    @FXML private TableColumn<PurchaseDetailRow, String> colSubtotal;
    @FXML private TableColumn<PurchaseDetailRow, Void> colAccion;

    @FXML private Label lblSubtotal;
    @FXML private Label lblIvaFavorTotal;
    @FXML private Label lblDescuentos;
    @FXML private Label lblTotalGeneral;

    private ObservableList<PurchaseDetailRow> detailRows;
    private ObservableList<Supplier> allSuppliers;
    private ObservableList<Product> allProducts;
    private ObservableList<Account> allAccounts;

    private static final String STYLE_ERROR  = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
    private static final String STYLE_NORMAL = "-fx-border-color: transparent; -fx-border-width: 0;";

    private Purchase purchaseInEditing;
    private boolean editMode = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        loadInitialData();

        detailRows = FXCollections.observableArrayList(row -> new javafx.beans.Observable[]{
                row.productProperty(),
                row.priceProperty(),
                row.quantityProperty(),
                row.discountProperty(),
                row.taxProperty()
        });

        setupDetailTable();
        setupSupplierSearch();
        setupDatesAndCombos();

        detailRows.addListener((ListChangeListener<PurchaseDetailRow>) c -> {
            recalculateTotals();
            if (detailRows.stream().anyMatch(r -> r.getProduct() != null)) tablaDetalles.setStyle(STYLE_NORMAL);
        });
        comboCuenta.valueProperty().addListener((obs, old, nw) -> { if (nw != null) comboCuenta.setStyle(STYLE_NORMAL); });

        if (!editMode) addLine();
    }

    private void loadInitialData() {
        allSuppliers = FXCollections.observableArrayList(purchasesService.findAllSuppliers());
        allProducts = FXCollections.observableArrayList(inventoryService.findAllProducts());
        allAccounts = FXCollections.observableArrayList(treasuryService.findAllAccounts());

        loadNextInvoiceNumber();
    }

    private void loadNextInvoiceNumber() {
        try {
            txtNumeroFactura.setText(purchasesService.generateNextInvoiceNumber());
        } catch (Exception e) {
            e.printStackTrace();
            txtNumeroFactura.setText("FAC-00001");
        }
    }

    private void setupSupplierSearch() {
        FilteredList<Supplier> filtered = new FilteredList<>(allSuppliers, p -> true);
        comboProveedor.setItems(filtered);

        comboProveedor.setConverter(new StringConverter<>() {
            @Override public String toString(Supplier s) { return s == null ? "" : s.getName(); }
            @Override public Supplier fromString(String string) { return comboProveedor.getValue(); }
        });

        comboProveedor.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (comboProveedor.getValue() != null &&
                        comboProveedor.getValue().getName().equals(newText)) return;

                filtered.setPredicate(s -> {
                    if (newText == null || newText.isEmpty()) return true;
                    String lower = newText.toLowerCase();
                    return s.getName().toLowerCase().contains(lower) ||
                            s.getNitNumber().contains(lower);
                });

                if (!filtered.isEmpty() && !comboProveedor.isShowing() && comboProveedor.isFocused()) {
                    comboProveedor.show();
                }
            });
        });

        comboProveedor.setOnAction(e -> {
            Supplier sel = comboProveedor.getSelectionModel().getSelectedItem();
            if (sel != null) {
                txtNit.setText(sel.getNitNumber());
                txtCorreo.setText(sel.getEmail());
                txtCelular.setText(sel.getPhone());
                comboProveedor.getEditor().setText(sel.getName());
            }
        });
    }

    private void setupDatesAndCombos() {
        if (!editMode) {
            dpFechaCompra.setValue(LocalDate.now());
            dpFechaVencimiento.setValue(LocalDate.now());
            comboFormaPago.getSelectionModel().select("Contado");
        }

        comboFormaPago.getItems().setAll("Contado", "Crédito");
        comboMedioPago.getItems().addAll("Efectivo", "Transferencia", "Nequi", "Tarjeta");
        comboCuenta.setItems(allAccounts);

        comboCuenta.setConverter(new StringConverter<Account>() {
            @Override public String toString(Account a) { return a == null ? "" : a.getName(); }
            @Override public Account fromString(String string) { return comboCuenta.getValue(); }
        });
        comboCuenta.valueProperty().addListener((obs, old, nw) -> Platform.runLater(() ->
                comboCuenta.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 4; -fx-background-radius: 4;")));

        Runnable updateDates = () -> {
            String type = comboFormaPago.getValue();
            LocalDate creation = dpFechaCompra.getValue();
            if (type == null || creation == null) return;
            if ("Contado".equals(type)) {
                dpFechaVencimiento.setValue(creation);
                dpFechaVencimiento.setDisable(true);
            } else if ("Crédito".equals(type)) {
                dpFechaVencimiento.setValue(creation.plusMonths(1));
                dpFechaVencimiento.setDisable(false);
            }
            boolean esCredito = "Crédito".equals(type);
            comboCuenta.setDisable(esCredito);
            comboMedioPago.setDisable(esCredito);
            if (esCredito) {
                comboCuenta.setValue(null);
                comboMedioPago.setValue(null);
            }
            comboCuenta.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 4; -fx-background-radius: 4;");
            comboMedioPago.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 4; -fx-background-radius: 4;");
        };

        comboFormaPago.setOnAction(e -> updateDates.run());
        dpFechaCompra.valueProperty().addListener((obs, oldVal, newVal) -> updateDates.run());
    }

    private void setupDetailTable() {
        tablaDetalles.setItems(detailRows);
        tablaDetalles.setEditable(true);

        colCodigo.setCellValueFactory(cell -> cell.getValue().productProperty());
        colCodigo.setCellFactory(col -> new ProductComboCell(allProducts, true));

        colDescripcion.setCellValueFactory(cell -> cell.getValue().productProperty());
        colDescripcion.setCellFactory(col -> new ProductComboCell(allProducts, false));

        colCantidad.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        colCantidad.setCellFactory(col -> new QuantityCell());

        colPrecio.setCellValueFactory(cell -> cell.getValue().priceProperty().asObject());
        colPrecio.setCellFactory(col -> new PriceCell());

        colDescuento.setCellValueFactory(cell -> cell.getValue().discountProperty().asObject());
        colDescuento.setCellFactory(col -> new PercentageCell());

        colImpuesto.setCellValueFactory(cell ->
                Bindings.createObjectBinding(() -> {
                    PurchaseDetailRow row = cell.getValue();
                    return row.getPrice() * getIvaRate(row.getProduct()) * row.getQuantity();
                }, cell.getValue().productProperty(), cell.getValue().priceProperty(), cell.getValue().quantityProperty())
        );
        colImpuesto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(currencyFormat.format(item));
            }
        });

        colSubtotal.setCellValueFactory(cell ->
                Bindings.createStringBinding(
                        () -> currencyFormat.format(cell.getValue().totalBinding().get()),
                        cell.getValue().totalBinding()
                )
        );

        colAccion.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("X");
            {
                btn.setStyle("-fx-text-fill: white; -fx-background-color: #e74c3c; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 3;");
                btn.setPrefSize(25, 25);
                btn.setOnAction(e -> getTableView().getItems().remove(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    public void setPurchaseForEditing(Purchase purchase) {
        this.purchaseInEditing = purchase;
        this.editMode = true;

        txtNumeroFactura.setText(purchase.getInvoiceNumber());
        txtNumeroFactura.setDisable(true);

        comboProveedor.setValue(purchase.getSupplier());
        if (purchase.getSupplier() != null) {
            txtNit.setText(purchase.getSupplier().getNitNumber());
            txtCorreo.setText(purchase.getSupplier().getEmail());
            txtCelular.setText(purchase.getSupplier().getPhone());
        }

        comboCuenta.setValue(purchase.getAccount());
        dpFechaCompra.setValue(purchase.getPurchaseDate());
        dpFechaVencimiento.setValue(purchase.getDueDate());
        comboFormaPago.setValue(purchase.getPaymentType());
        comboMedioPago.setValue(purchase.getPaymentMethod());
        txtNotas.setText(purchase.getNotes());

        List<PurchaseDetail> detailsFromDB = purchasesService.findDetailsByPurchase(purchase);
        List<PurchaseDetailRow> rows = new ArrayList<>();

        for (PurchaseDetail d : detailsFromDB) {
            PurchaseDetailRow r = new PurchaseDetailRow();
            r.setProduct(d.getProduct());
            r.setQuantity(d.getQuantity());
            r.setPrice(d.getUnitPrice());
            r.setDiscount(d.getDiscount());
            r.setTax(d.getTax());
            rows.add(r);
        }

        detailRows.setAll(rows);
        recalculateTotals();

        boolean esCredito = "Crédito".equals(comboFormaPago.getValue());
        comboCuenta.setDisable(esCredito);
        comboMedioPago.setDisable(esCredito);

        if (btnGuardar != null) btnGuardar.setText("Actualizar Compra");
    }

    private void recalculateTotals() {
        double subtotal = 0;
        double discountTotal = 0;

        for (PurchaseDetailRow row : detailRows) {
            if (row.getProduct() != null) {
                double lineBase = row.getPrice() * row.getQuantity();
                subtotal += lineBase;
                discountTotal += lineBase * (row.getDiscount() / 100.0);
            }
        }

        double ivaTotal = subtotal * 0.19;
        double total = subtotal - discountTotal + ivaTotal;

        lblSubtotal.setText(currencyFormat.format(subtotal));
        lblIvaFavorTotal.setText(currencyFormat.format(ivaTotal));
        lblDescuentos.setText(currencyFormat.format(discountTotal));
        lblTotalGeneral.setText(currencyFormat.format(total));
    }

    private double getIvaRate(Product p) {
        return 0.19;
    }

    @FXML void btnAgregarLineaClick(ActionEvent event) { addLine(); }

    private void addLine() {
        detailRows.add(new PurchaseDetailRow());
        tablaDetalles.scrollTo(detailRows.size() - 1);
    }

    @FXML
    void btnGuardarClick(ActionEvent event) {
        boolean valid = true;
        if (detailRows.isEmpty() || detailRows.stream().noneMatch(r -> r.getProduct() != null)) {
            tablaDetalles.setStyle(STYLE_ERROR);
            valid = false;
        }
        boolean esCredito = "Crédito".equals(comboFormaPago.getValue());
        if (!esCredito && comboCuenta.getValue() == null) {
            comboCuenta.setStyle(STYLE_ERROR);
            valid = false;
        }
        if (!valid) {
            ToastNotification.warning(tablaDetalles, "Completa los campos resaltados");
            return;
        }

        if (editMode) {
            CustomDialog.confirm(rootFormulario,
                "Guardar cambios",
                "Vas a sobreescribir la factura de compra " + txtNumeroFactura.getText()
                    + ". Los datos anteriores serán reemplazados, incluyendo los productos y cantidades. ¿Confirmas?",
                this::doSave,
                null);
        } else {
            doSave();
        }
    }

    private void doSave() {
        try {
            Supplier supplier = getOrSaveSupplier();
            Purchase purchase = editMode ? purchaseInEditing : new Purchase();

            purchase.setInvoiceNumber(txtNumeroFactura.getText());
            purchase.setSupplier(supplier);
            purchase.setPurchaseDate(dpFechaCompra.getValue());
            purchase.setDueDate(dpFechaVencimiento.getValue());
            purchase.setPaymentType(comboFormaPago.getValue());
            purchase.setPaymentMethod(comboMedioPago.getValue());
            purchase.setAccount(comboCuenta.getValue());
            purchase.setNotes(txtNotas.getText());

            double finalTotal = detailRows.stream()
                    .filter(r -> r.getProduct() != null)
                    .mapToDouble(PurchaseDetailRow::getLineTotal).sum();

            purchase.setTotal(finalTotal);
            purchase.setPendingBalance("Crédito".equals(comboFormaPago.getValue()) ? finalTotal : 0.0);
            purchase.setStatus("Crédito".equals(comboFormaPago.getValue()) ? "PENDIENTE" : "PAGADA");

            List<PurchaseDetail> details = detailRows.stream()
                    .filter(r -> r.getProduct() != null)
                    .map(row -> {
                        PurchaseDetail det = new PurchaseDetail();
                        det.setProduct(row.getProduct());
                        det.setQuantity(row.getQuantity());
                        det.setUnitPrice(row.getPrice());
                        det.setDiscount(row.getDiscount());
                        det.setTax(row.getTax());
                        det.setSubtotal(row.getLineTotal());
                        return det;
                    })
                    .toList();

            purchasesService.savePurchaseWithDetails(purchase, details, editMode);

            String numFactura = purchase.getInvoiceNumber();
            boolean fueEdicion = editMode;
            navigateBack();
            ToastNotification.success(
                MainLayoutController.getInstance().getContentArea(),
                fueEdicion ? "Factura " + numFactura + " actualizada correctamente"
                           : "Factura " + numFactura + " registrada correctamente"
            );

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaDetalles, "No se pudo guardar la compra");
        }
    }

    private Supplier getOrSaveSupplier() {
        Supplier selected = comboProveedor.getValue();
        String nameTyped = comboProveedor.getEditor().getText();
        String nitTyped = txtNit.getText();

        return purchasesService.saveOrUpdateSupplier(
                selected, nameTyped, nitTyped,
                txtCorreo.getText(), txtCelular.getText()
        );
    }

    @FXML void btnCancelarClick(ActionEvent event) { navigateBack(); }

    private void navigateBack() {
        MainLayoutController.getInstance().loadView("/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml");
    }

    private class QuantityCell extends TableCell<PurchaseDetailRow, Integer> {
        private final HBox container = new HBox();
        private final TextField tfQuantity = new TextField();
        private final Button btnMinus = new Button("-");
        private final Button btnPlus = new Button("+");

        public QuantityCell() {
            tfQuantity.setPrefWidth(40);
            tfQuantity.setAlignment(Pos.CENTER);
            String btnStyle = "-fx-background-color: #bdc3c7; -fx-font-weight: bold; -fx-min-width: 25px;";
            btnMinus.setStyle(btnStyle); btnPlus.setStyle(btnStyle);
            btnMinus.setFocusTraversable(false); btnPlus.setFocusTraversable(false);

            container.getChildren().addAll(btnMinus, tfQuantity, btnPlus);
            container.setAlignment(Pos.CENTER);
            container.setSpacing(3);

            btnMinus.setOnAction(e -> adjustQuantity(-1));
            btnPlus.setOnAction(e -> adjustQuantity(1));

            tfQuantity.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) tfQuantity.setText(newVal.replaceAll("[^\\d]", ""));
            });
            tfQuantity.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) commit(); });
            tfQuantity.focusedProperty().addListener((o, old, focused) -> { if (!focused) commit(); });
        }

        private void adjustQuantity(int delta) {
            PurchaseDetailRow row = getTableRow().getItem();
            if (row == null) return;
            int newVal = row.getQuantity() + delta;
            if (newVal < 1) return;
            row.setQuantity(newVal);
        }

        private void commit() {
            PurchaseDetailRow row = getTableRow().getItem();
            if (row == null) return;
            try {
                int val = Integer.parseInt(tfQuantity.getText());
                if (val < 1) val = 1;
                row.setQuantity(val);
                tfQuantity.setText(String.valueOf(val));
            } catch (Exception e) { tfQuantity.setText(String.valueOf(row.getQuantity())); }
        }

        @Override protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                if (!tfQuantity.isFocused()) tfQuantity.setText(String.valueOf(item));
                setGraphic(container);
            }
        }
    }

    private class PriceCell extends TableCell<PurchaseDetailRow, Double> {
        private final TextField textField = new TextField();

        public PriceCell() {
            textField.setAlignment(Pos.CENTER);
            textField.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
            textField.focusedProperty().addListener((obs, was, isNow) -> {
                if (isNow) {
                    PurchaseDetailRow row = getTableRow().getItem();
                    if (row != null) {
                        double val = row.getPrice();
                        textField.setText(val % 1 == 0 ? String.format("%.0f", val) : String.format("%.2f", val).replace(".", ","));
                        textField.selectAll();
                    }
                } else commitPrice();
            });
            textField.setOnAction(e -> tablaDetalles.requestFocus());
        }

        private void commitPrice() {
            PurchaseDetailRow row = getTableRow().getItem();
            if (row == null) return;
            String text = textField.getText().replace("$", "").trim();
            if (text.isEmpty() || text.equals("0")) { restoreOriginalPrice(row); return; }
            text = text.replace(".", "").replace(",", ".");
            try {
                double val = Double.parseDouble(text);
                if (val <= 0) restoreOriginalPrice(row);
                else {
                    row.setPrice(val);
                    row.setTax(val * getIvaRate(row.getProduct()));
                    textField.setText(currencyFormat.format(val));
                }
            } catch (Exception e) { restoreOriginalPrice(row); }
        }

        private void restoreOriginalPrice(PurchaseDetailRow row) {
            if (row.getProduct() != null) {
                double cost = row.getProduct().getPurchaseCost() != null ? row.getProduct().getPurchaseCost() : 0.0;
                row.setPrice(cost);
                row.setTax(cost * getIvaRate(row.getProduct()));
                textField.setText(currencyFormat.format(cost));
            } else textField.setText(currencyFormat.format(0));
        }

        @Override protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                setAlignment(Pos.CENTER);
                if (!textField.isFocused()) textField.setText(currencyFormat.format(item));
                setGraphic(textField);
            }
        }
    }

    private class PercentageCell extends TableCell<PurchaseDetailRow, Double> {
        private final TextField textField = new TextField();

        public PercentageCell() {
            textField.setAlignment(Pos.CENTER);
            textField.setStyle("-fx-background-color: transparent;");
            textField.focusedProperty().addListener((o, old, focused) -> {
                if (focused) {
                    String raw = textField.getText().replace("%", "").trim();
                    textField.setText(raw);
                    textField.selectAll();
                } else commit();
            });
            textField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) { commit(); tablaDetalles.requestFocus(); } });
        }

        private void commit() {
            PurchaseDetailRow row = getTableRow().getItem();
            if (row == null) return;
            try {
                String txt = textField.getText().replaceAll("[^0-9.]", "");
                if (txt.isEmpty()) txt = "0";
                double val = Double.parseDouble(txt);
                if (val > 100) val = 100;
                row.setDiscount(val);
                textField.setText(String.format("%.0f%%", val));
            } catch (Exception e) { textField.setText(String.format("%.0f%%", row.getDiscount())); }
        }

        @Override protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                if (!textField.isFocused()) textField.setText(String.format("%.0f%%", item));
                setGraphic(textField);
            }
        }
    }

    private class ProductComboCell extends TableCell<PurchaseDetailRow, Product> {
        private final ComboBox<Product> comboBox;
        private final FilteredList<Product> filteredItems;
        private boolean updating = false;

        public ProductComboCell(ObservableList<Product> products, boolean byCode) {
            this.filteredItems = new FilteredList<>(products, p -> true);
            this.comboBox = new ComboBox<>(filteredItems);
            this.comboBox.setEditable(true);
            this.comboBox.setMaxWidth(Double.MAX_VALUE);
            this.comboBox.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

            this.comboBox.setConverter(new StringConverter<>() {
                @Override public String toString(Product p) {
                    if (p == null) return "";
                    return byCode ? p.getCode() : p.getDescription();
                }
                @Override public Product fromString(String s) { return comboBox.getValue(); }
            });

            this.comboBox.getEditor().textProperty().addListener((obs, oldTxt, newTxt) -> {
                if (updating) return;
                Platform.runLater(() -> {
                    if (comboBox.getSelectionModel().getSelectedItem() != null) return;
                    filteredItems.setPredicate(p -> {
                        if (newTxt == null || newTxt.isEmpty()) return true;
                        String lower = newTxt.toLowerCase();
                        return p.getDescription().toLowerCase().contains(lower) || p.getCode().toLowerCase().contains(lower);
                    });
                    if (!comboBox.isShowing() && !filteredItems.isEmpty() && comboBox.isFocused()) {
                        comboBox.show();
                    }
                });
            });

            this.comboBox.setOnAction(e -> confirmSelection());
        }

        private void confirmSelection() {
            if (updating) return;
            Product p = comboBox.getValue();
            if (p != null) {
                PurchaseDetailRow row = getTableRow().getItem();
                if (row != null && row.getProduct() != p) {
                    double unitPrice = p.getPurchaseCost() != null ? p.getPurchaseCost() : 0.0;
                    row.setProduct(p);
                    row.setPrice(unitPrice);
                    row.setTax(unitPrice * getIvaRate(p));
                    row.setDiscount(0.0);
                    row.setQuantity(1);
                    tablaDetalles.refresh();
                }
            }
        }

        @Override
        protected void updateItem(Product item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) setGraphic(null);
            else {
                updating = true;
                comboBox.setValue(item);
                updating = false;
                setGraphic(comboBox);
            }
        }
    }
}

class PurchaseDetailRow {
    private final ObjectProperty<Product> product = new SimpleObjectProperty<>();
    private final DoubleProperty price = new SimpleDoubleProperty(0.0);
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);
    private final DoubleProperty discount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tax = new SimpleDoubleProperty(0.0);

    public PurchaseDetailRow() {}

    public javafx.beans.binding.DoubleBinding totalBinding() {
        return Bindings.createDoubleBinding(() -> {
            double lineBase = getPrice() * getQuantity();
            double discAmount = lineBase * (getDiscount() / 100.0);
            double iva = lineBase * 0.19;
            return lineBase - discAmount + iva;
        }, price, quantity, discount);
    }

    public double getLineTotal() { return totalBinding().get(); }

    public ObjectProperty<Product> productProperty() { return product; }
    public Product getProduct() { return product.get(); }
    public void setProduct(Product p) { this.product.set(p); }

    public DoubleProperty priceProperty() { return price; }
    public double getPrice() { return price.get(); }
    public void setPrice(double d) { this.price.set(d); }

    public IntegerProperty quantityProperty() { return quantity; }
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int i) { this.quantity.set(i); }

    public DoubleProperty discountProperty() { return discount; }
    public double getDiscount() { return discount.get(); }
    public void setDiscount(double d) { this.discount.set(d); }

    public DoubleProperty taxProperty() { return tax; }
    public double getTax() { return tax.get(); }
    public void setTax(double d) { this.tax.set(d); }
}
