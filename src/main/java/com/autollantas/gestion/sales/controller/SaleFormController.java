package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.sales.model.Customer;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.model.SaleDetail;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
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
public class SaleFormController {

    @Autowired private SalesService salesService;
    @Autowired private InventoryService inventoryService;
    @Autowired private TreasuryService treasuryService;

    @FXML private VBox rootFormulario;
    @FXML private TextField txtNumeroFactura;

    @FXML private ComboBox<Customer> comboCliente;
    @FXML private ComboBox<String> comboTipoDoc;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtCelular;

    @FXML private DatePicker dpFechaCreacion;
    @FXML private DatePicker dpFechaVencimiento;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private TextArea txtNotas;

    @FXML private TableView<SaleDetailRow> tablaDetalles;
    @FXML private TableColumn<SaleDetailRow, Product> colCodigo;
    @FXML private TableColumn<SaleDetailRow, Product> colDescripcion;
    @FXML private TableColumn<SaleDetailRow, Integer> colCantidad;
    @FXML private TableColumn<SaleDetailRow, Double> colPrecio;
    @FXML private TableColumn<SaleDetailRow, Double> colDescuento;
    @FXML private TableColumn<SaleDetailRow, Double> colImpuesto;
    @FXML private TableColumn<SaleDetailRow, String> colSubtotal;
    @FXML private TableColumn<SaleDetailRow, Void> colAccion;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuentos;
    @FXML private Label lblTotalGeneral;

    private ObservableList<SaleDetailRow> detailRows;
    private ObservableList<Customer> allCustomers;
    private ObservableList<Product> allProducts;
    private ObservableList<Account> allAccounts;

    private Sale saleForEditing;
    private boolean editMode = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        loadInitialData();

        detailRows = FXCollections.observableArrayList(row -> new javafx.beans.Observable[]{
                row.productProperty(),
                row.priceProperty(),
                row.quantityProperty(),
                row.discountProperty(),
                row.taxProperty()
        });

        setupDetailTable();
        setupCustomerSearch();
        setupDatesAndCombos();

        detailRows.addListener((ListChangeListener<SaleDetailRow>) c -> recalculateTotals());

        if (!editMode) {
            addLine();
        }
    }

    private void loadInitialData() {
        allCustomers = FXCollections.observableArrayList(salesService.findAllCustomers());
        allProducts = FXCollections.observableArrayList(inventoryService.findProductsWithStock());
        allAccounts = FXCollections.observableArrayList(treasuryService.findAllAccounts());
        generateNextInvoiceNumber();
    }

    private void generateNextInvoiceNumber() {
        try {
            txtNumeroFactura.setText(salesService.generateNextInvoiceNumber());
        } catch (Exception e) {
            e.printStackTrace();
            txtNumeroFactura.setText("VEN-00001");
        }
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
                    double unitario = cell.getValue().getTax();
                    int cant = cell.getValue().getQuantity();
                    return unitario * cant;
                }, cell.getValue().taxProperty(), cell.getValue().quantityProperty())
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
                btn.setOnAction(e -> {
                    SaleDetailRow row = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(row);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    public void setSaleForEditing(Sale sale) {
        this.saleForEditing = sale;
        this.editMode = true;

        txtNumeroFactura.setText(sale.getInvoiceNumber());
        txtNumeroFactura.setDisable(true);

        comboCliente.setValue(sale.getCustomer());
        if (sale.getCustomer() != null) {
            txtDocumento.setText(sale.getCustomer().getDocumentNumber());
            txtCorreo.setText(sale.getCustomer().getEmail());
            txtCelular.setText(sale.getCustomer().getPhone());
        }

        comboCuenta.setValue(sale.getAccount());
        dpFechaCreacion.setValue(sale.getSaleDate());
        dpFechaVencimiento.setValue(sale.getDueDate());
        comboFormaPago.setValue(sale.getPaymentType());
        comboMedioPago.setValue(sale.getPaymentMethod());
        txtNotas.setText(sale.getNotes());

        List<SaleDetail> detailsDB = salesService.findSaleDetailsBySale(sale);
        List<SaleDetailRow> rows = new ArrayList<>();

        for (SaleDetail d : detailsDB) {
            SaleDetailRow r = new SaleDetailRow();
            r.setProduct(d.getProduct());
            r.setQuantity(d.getQuantity());
            r.setPrice(d.getSalePrice());
            r.setDiscount(d.getDiscount());
            r.setTax(d.getTax());
            rows.add(r);
        }

        detailRows.setAll(rows);
        recalculateTotals();
    }

    private class QuantityCell extends TableCell<SaleDetailRow, Integer> {
        private final HBox container = new HBox();
        private final TextField tfCantidad = new TextField();
        private final Button btnMenos = new Button("-");
        private final Button btnMas = new Button("+");

        public QuantityCell() {
            tfCantidad.setPrefWidth(40);
            tfCantidad.setAlignment(Pos.CENTER);
            tfCantidad.setStyle("-fx-font-weight: bold;");

            String btnStyle = "-fx-background-color: #bdc3c7; -fx-font-weight: bold; -fx-min-width: 25px; -fx-cursor: hand;";
            btnMenos.setStyle(btnStyle);
            btnMas.setStyle(btnStyle);
            btnMenos.setFocusTraversable(false);
            btnMas.setFocusTraversable(false);

            container.getChildren().addAll(btnMenos, tfCantidad, btnMas);
            container.setAlignment(Pos.CENTER);
            container.setSpacing(3);

            btnMenos.setOnAction(e -> adjustQuantity(-1));
            btnMas.setOnAction(e -> adjustQuantity(1));

            tfCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) tfCantidad.setText(newVal.replaceAll("[^\\d]", ""));
            });
            tfCantidad.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) commitEntry(); });
            tfCantidad.focusedProperty().addListener((o, old, isFocused) -> { if (!isFocused) commitEntry(); });
        }

        private void adjustQuantity(int delta) {
            SaleDetailRow row = getTableRow().getItem();
            if (row == null || row.getProduct() == null) return;

            int nuevoVal = row.getQuantity() + delta;
            if (nuevoVal < 1) return;

            if (nuevoVal > row.getProduct().getQuantity()) {
                showAlert("Stock Máximo", "Solo hay " + row.getProduct().getQuantity() + " unidades disponibles.");
                return;
            }
            row.setQuantity(nuevoVal);
        }

        private void commitEntry() {
            SaleDetailRow row = getTableRow().getItem();
            if (row == null) return;

            if (tfCantidad.getText().isEmpty()) {
                tfCantidad.setText(String.valueOf(row.getQuantity()));
                return;
            }
            try {
                int val = Integer.parseInt(tfCantidad.getText());
                if (val < 1) val = 1;
                if (row.getProduct() != null && val > row.getProduct().getQuantity()) {
                    showAlert("Stock insuficiente", "Máximo disponible: " + row.getProduct().getQuantity());
                    val = row.getProduct().getQuantity();
                }
                row.setQuantity(val);
                tfCantidad.setText(String.valueOf(val));
            } catch (Exception e) {
                tfCantidad.setText(String.valueOf(row.getQuantity()));
            }
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); }
            else {
                if (!tfCantidad.isFocused()) tfCantidad.setText(String.valueOf(item));
                setGraphic(container);
            }
        }
    }

    private class PriceCell extends TableCell<SaleDetailRow, Double> {
        private final TextField textField = new TextField();

        public PriceCell() {
            textField.setAlignment(Pos.CENTER_RIGHT);
            textField.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused) {
                    SaleDetailRow row = getTableRow().getItem();
                    if (row != null) {
                        double val = row.getPrice();
                        textField.setText(val % 1 == 0 ? String.format("%.0f", val) : String.format("%.2f", val).replace(".", ","));
                        textField.selectAll();
                    }
                } else {
                    commitPrice();
                }
            });
            textField.setOnAction(e -> tablaDetalles.requestFocus());
        }

        private void commitPrice() {
            SaleDetailRow row = getTableRow().getItem();
            if (row == null) return;

            String text = textField.getText();
            if (text == null) text = "";
            text = text.replace("$", "").trim();

            if (text.isEmpty() || text.equals("0")) { restoreOriginalPrice(row); return; }

            text = text.replace(".", "").replace(",", ".");
            try {
                double val = Double.parseDouble(text);
                if (val <= 0) { restoreOriginalPrice(row); }
                else { row.setPrice(val); textField.setText(currencyFormat.format(val)); }
            } catch (NumberFormatException e) {
                restoreOriginalPrice(row);
            }
        }

        private void restoreOriginalPrice(SaleDetailRow row) {
            if (row.getProduct() != null) {
                double precioOriginal = row.getProduct().getBasePrice();
                row.setPrice(precioOriginal);
                textField.setText(currencyFormat.format(precioOriginal));
            } else {
                textField.setText(currencyFormat.format(0));
            }
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); }
            else {
                if (!textField.isFocused()) textField.setText(currencyFormat.format(item));
                setGraphic(textField);
            }
        }
    }

    private class PercentageCell extends TableCell<SaleDetailRow, Double> {
        private final TextField textField = new TextField();

        public PercentageCell() {
            textField.setAlignment(Pos.CENTER);
            textField.setStyle("-fx-background-color: transparent;");

            textField.focusedProperty().addListener((o, old, isFocused) -> {
                if (isFocused) {
                    textField.setText(textField.getText().replace("%", "").trim());
                    textField.selectAll();
                } else {
                    commit();
                }
            });
            textField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) { commit(); tablaDetalles.requestFocus(); } });
        }

        private void commit() {
            SaleDetailRow row = getTableRow().getItem();
            if (row == null) return;
            try {
                String txt = textField.getText().replaceAll("[^0-9.]", "");
                if (txt.isEmpty()) txt = "0";
                double val = Double.parseDouble(txt);
                if (val > 100) val = 100;
                row.setDiscount(val);
                textField.setText(String.format("%.0f%%", val));
            } catch (Exception e) {
                textField.setText(String.format("%.0f%%", row.getDiscount()));
            }
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); }
            else {
                if (!textField.isFocused()) textField.setText(String.format("%.0f%%", item));
                setGraphic(textField);
            }
        }
    }

    private class ProductComboCell extends TableCell<SaleDetailRow, Product> {
        private final ComboBox<Product> comboBox;
        private final FilteredList<Product> filteredItems;
        private boolean isUpdating = false;

        public ProductComboCell(ObservableList<Product> allProducts, boolean porCodigo) {
            this.filteredItems = new FilteredList<>(allProducts, p -> true);
            this.comboBox = new ComboBox<>(filteredItems);
            this.comboBox.setEditable(true);
            this.comboBox.setMaxWidth(Double.MAX_VALUE);
            this.comboBox.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

            this.comboBox.setConverter(new StringConverter<>() {
                @Override public String toString(Product p) {
                    if (p == null) return "";
                    return porCodigo ? p.getCode() : p.getDescription();
                }
                @Override public Product fromString(String s) { return comboBox.getValue(); }
            });

            this.comboBox.getEditor().textProperty().addListener((obs, oldTxt, newTxt) -> {
                if (isUpdating) return;
                Platform.runLater(() -> {
                    if (comboBox.getSelectionModel().getSelectedItem() != null) return;
                    filteredItems.setPredicate(p -> {
                        if (newTxt == null || newTxt.isEmpty()) return true;
                        String lower = newTxt.toLowerCase();
                        return p.getDescription().toLowerCase().contains(lower) || p.getCode().toLowerCase().contains(lower);
                    });
                    if (!comboBox.isShowing() && !filteredItems.isEmpty() && comboBox.isFocused()) comboBox.show();
                });
            });

            this.comboBox.setOnAction(e -> confirmSelection());
            this.comboBox.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) confirmSelection(); });
        }

        private void confirmSelection() {
            if (isUpdating) return;
            Product p = comboBox.getValue();
            if (p != null) {
                SaleDetailRow row = getTableRow().getItem();
                if (row != null && row.getProduct() != p) {
                    if (p.getQuantity() < 1) {
                        showAlert("Sin Stock", "Producto agotado.");
                        Platform.runLater(() -> comboBox.getSelectionModel().clearSelection());
                        return;
                    }
                    row.setProduct(p);
                    row.setPrice(p.getBasePrice());
                    row.setTax(p.getTaxAmount() != null ? p.getTaxAmount() : 0.0);
                    row.setDiscount(0.0);
                    row.setQuantity(1);
                    tablaDetalles.refresh();
                }
            }
        }

        @Override
        protected void updateItem(Product item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) { setGraphic(null); }
            else {
                isUpdating = true;
                comboBox.setValue(item);
                isUpdating = false;
                setGraphic(comboBox);
            }
        }
    }

    private void setupCustomerSearch() {
        FilteredList<Customer> clientesFiltrados = new FilteredList<>(allCustomers, p -> true);
        comboCliente.setItems(clientesFiltrados);
        comboCliente.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) { return c == null ? "" : c.getName(); }
            @Override public Customer fromString(String string) { return comboCliente.getValue(); }
        });

        comboCliente.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (comboCliente.getValue() != null && comboCliente.getValue().getName().equals(newText)) return;
                clientesFiltrados.setPredicate(cliente -> {
                    if (newText == null || newText.isEmpty()) return true;
                    String lower = newText.toLowerCase();
                    return cliente.getName().toLowerCase().contains(lower) ||
                            cliente.getDocumentNumber().contains(lower);
                });
                if (!clientesFiltrados.isEmpty() && !comboCliente.isShowing() && comboCliente.isFocused()) {
                    comboCliente.show();
                }
            });
        });

        comboCliente.setOnAction(e -> {
            Customer seleccionado = comboCliente.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                txtDocumento.setText(seleccionado.getDocumentNumber());
                txtCorreo.setText(seleccionado.getEmail());
                txtCelular.setText(seleccionado.getPhone());
                comboCliente.getEditor().setText(seleccionado.getName());
            }
        });
    }

    private void setupDatesAndCombos() {
        dpFechaCreacion.setValue(LocalDate.now());
        dpFechaVencimiento.setValue(LocalDate.now());

        comboFormaPago.getItems().setAll("Contado", "Crédito");
        comboMedioPago.getItems().addAll("Efectivo", "Transferencia", "Nequi", "Tarjeta");
        comboTipoDoc.getItems().addAll("CC", "NIT", "RUT");
        comboCuenta.setItems(allAccounts);

        comboCuenta.setConverter(new StringConverter<Account>() {
            @Override public String toString(Account a) { return a == null ? "" : a.getName(); }
            @Override public Account fromString(String string) { return comboCuenta.getValue(); }
        });

        Runnable updateDates = () -> {
            String pago = comboFormaPago.getValue();
            LocalDate creacion = dpFechaCreacion.getValue();
            if (pago == null || creacion == null) return;
            if ("Contado".equals(pago)) {
                dpFechaVencimiento.setValue(creacion);
                dpFechaVencimiento.setDisable(true);
            } else if ("Crédito".equals(pago)) {
                dpFechaVencimiento.setValue(creacion.plusMonths(1));
                dpFechaVencimiento.setDisable(false);
            }
        };

        comboFormaPago.setOnAction(e -> updateDates.run());
        dpFechaCreacion.valueProperty().addListener((obs, oldVal, newVal) -> updateDates.run());
    }

    private void recalculateTotals() {
        double subtotal = 0, descuentos = 0, total = 0;

        for (SaleDetailRow row : detailRows) {
            if (row.getProduct() != null) {
                double precioTotal = row.getPrice() * row.getQuantity();
                double descMonto = precioTotal * (row.getDiscount() / 100.0);
                subtotal += precioTotal;
                descuentos += descMonto;
                total += row.getLineTotal();
            }
        }

        lblSubtotal.setText(currencyFormat.format(subtotal));
        lblDescuentos.setText(currencyFormat.format(descuentos));
        lblTotalGeneral.setText(currencyFormat.format(total));
    }

    @FXML void btnAgregarLineaClick(ActionEvent event) { addLine(); }

    private void addLine() {
        detailRows.add(new SaleDetailRow());
        tablaDetalles.scrollTo(detailRows.size() - 1);
    }

    @FXML
    void btnGuardarClick(ActionEvent event) {
        if (detailRows.isEmpty() || detailRows.stream().noneMatch(r -> r.getProduct() != null)) {
            showAlert("Error", "Debe agregar al menos un producto válido.");
            return;
        }
        if (comboCuenta.getValue() == null) {
            showAlert("Error", "Debe seleccionar una Cuenta.");
            return;
        }

        try {
            Customer customer = getOrSaveCustomer();

            Sale sale = editMode ? saleForEditing : new Sale();
            sale.setInvoiceNumber(txtNumeroFactura.getText());
            sale.setCustomer(customer);
            sale.setSaleDate(dpFechaCreacion.getValue());
            sale.setDueDate(dpFechaVencimiento.getValue());
            sale.setPaymentType(comboFormaPago.getValue());
            sale.setPaymentMethod(comboMedioPago.getValue());
            sale.setAccount(comboCuenta.getValue());
            sale.setNotes(txtNotas.getText());

            double totalFinal = detailRows.stream()
                    .filter(r -> r.getProduct() != null)
                    .mapToDouble(SaleDetailRow::getLineTotal).sum();

            sale.setTotal(totalFinal);
            sale.setPendingBalance("Crédito".equals(comboFormaPago.getValue()) ? totalFinal : 0.0);
            sale.setStatus("Crédito".equals(comboFormaPago.getValue()) ? "PENDIENTE" : "PAGADA");

            List<SaleDetail> details = detailRows.stream()
                    .filter(r -> r.getProduct() != null)
                    .map(row -> {
                        SaleDetail det = new SaleDetail();
                        det.setProduct(row.getProduct());
                        det.setQuantity(row.getQuantity());
                        det.setSalePrice(row.getPrice());
                        det.setDiscount(row.getDiscount());
                        det.setTax(row.getTax());
                        det.setSubtotal(row.getLineTotal());
                        return det;
                    })
                    .toList();

            salesService.saveSaleWithDetails(sale, details, editMode);

            showAlert("Éxito", "Venta guardada correctamente.");
            navigateBack();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error Crítico", "No se pudo guardar la venta: " + e.getMessage());
        }
    }

    private Customer getOrSaveCustomer() {
        Customer selected = comboCliente.getValue();
        String nombreEscrito = comboCliente.getEditor().getText();
        String docEscrito = txtDocumento.getText();

        return salesService.saveOrUpdateCustomer(
                selected,
                nombreEscrito,
                docEscrito,
                txtCorreo.getText(),
                txtCelular.getText()
        );
    }

    @FXML void btnCancelarClick(ActionEvent event) { navigateBack(); }

    private void navigateBack() {
        MainLayoutController.getInstance().loadView("/com/autollantas/gestion/sales/views/SaleInvoices.fxml");
    }

    private void showAlert(String titulo, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        });
    }
}

class SaleDetailRow {
    private final ObjectProperty<Product> product = new SimpleObjectProperty<>();
    private final DoubleProperty price = new SimpleDoubleProperty(0.0);
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);
    private final DoubleProperty discount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tax = new SimpleDoubleProperty(0.0);

    public SaleDetailRow() {}

    public javafx.beans.binding.DoubleBinding totalBinding() {
        return Bindings.createDoubleBinding(() -> {
            double totalPrice = getPrice() * getQuantity();
            double discountAmount = totalPrice * (getDiscount() / 100.0);
            double base = totalPrice - discountAmount;
            double totalTax = getTax() * getQuantity();
            return base + totalTax;
        }, price, quantity, discount, tax);
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
