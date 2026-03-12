package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.*;
import com.autollantas.gestion.service.InventarioService;
import com.autollantas.gestion.service.TesoreriaService;
import com.autollantas.gestion.service.VentasService;
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class FormularioVentaController {

    @Autowired private VentasService ventasService;
    @Autowired private InventarioService inventarioService;
    @Autowired private TesoreriaService tesoreriaService;

    @FXML private VBox rootFormulario;
    @FXML private TextField txtNumeroFactura;

    @FXML private ComboBox<Cliente> comboCliente;
    @FXML private ComboBox<String> comboTipoDoc;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtCelular;

    @FXML private DatePicker dpFechaCreacion;
    @FXML private DatePicker dpFechaVencimiento;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private TextArea txtNotas;

    @FXML private TableView<DetalleVentaRow> tablaDetalles;
    @FXML private TableColumn<DetalleVentaRow, Producto> colCodigo;
    @FXML private TableColumn<DetalleVentaRow, Producto> colDescripcion;
    @FXML private TableColumn<DetalleVentaRow, Integer> colCantidad;
    @FXML private TableColumn<DetalleVentaRow, Double> colPrecio;
    @FXML private TableColumn<DetalleVentaRow, Double> colDescuento;
    @FXML private TableColumn<DetalleVentaRow, Double> colImpuesto;
    @FXML private TableColumn<DetalleVentaRow, String> colSubtotal;
    @FXML private TableColumn<DetalleVentaRow, Void> colAccion;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuentos;
    @FXML private Label lblTotalGeneral;

    private ObservableList<DetalleVentaRow> listaDetalles;
    private ObservableList<Cliente> todosLosClientes;
    private ObservableList<Producto> todosLosProductos;
    private ObservableList<Cuenta> todasLasCuentas;

    private Venta ventaEnEdicion;
    private boolean modoEdicion = false;

    private final NumberFormat monedaFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        cargarDatosIniciales();

        listaDetalles = FXCollections.observableArrayList(row -> new javafx.beans.Observable[] {
                row.productoProperty(),
                row.precioProperty(),
                row.cantidadProperty(),
                row.descuentoProperty(),
                row.impuestoProperty()
        });

        configurarTablaAvanzada();
        configurarBuscadorCliente();
        configurarFechasYCombos();

        listaDetalles.addListener((ListChangeListener<DetalleVentaRow>) c -> recalcularTotalesGenerales());

        if (!modoEdicion) {
            agregarLinea();
        }
    }

    private void cargarDatosIniciales() {
        todosLosClientes = FXCollections.observableArrayList(ventasService.findAllClientes());
        List<Producto> productosConStock = inventarioService.findProductosConStock();
        todosLosProductos = FXCollections.observableArrayList(productosConStock);
        todasLasCuentas = FXCollections.observableArrayList(tesoreriaService.findAllCuentas());

        generarSiguienteNumeroFactura();
    }

    private void generarSiguienteNumeroFactura() {
        try {
            txtNumeroFactura.setText(ventasService.generarSiguienteNumeroFactura());
        } catch (Exception e) {
            e.printStackTrace();
            txtNumeroFactura.setText("VEN-00001");
        }
    }

    private void configurarTablaAvanzada() {
        tablaDetalles.setItems(listaDetalles);
        tablaDetalles.setEditable(true);

        colCodigo.setCellValueFactory(cell -> cell.getValue().productoProperty());
        colCodigo.setCellFactory(col -> new ProductoComboCell(todosLosProductos, true));

        colDescripcion.setCellValueFactory(cell -> cell.getValue().productoProperty());
        colDescripcion.setCellFactory(col -> new ProductoComboCell(todosLosProductos, false));

        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty().asObject());
        colCantidad.setCellFactory(col -> new CantidadCell());

        colPrecio.setCellValueFactory(cell -> cell.getValue().precioProperty().asObject());
        colPrecio.setCellFactory(col -> new PrecioCell());

        colDescuento.setCellValueFactory(cell -> cell.getValue().descuentoProperty().asObject());
        colDescuento.setCellFactory(col -> new PercentageCell());

        colImpuesto.setCellValueFactory(cell ->
                Bindings.createObjectBinding(() -> {
                    double unitario = cell.getValue().getImpuesto();
                    int cant = cell.getValue().getCantidad();
                    return unitario * cant;
                }, cell.getValue().impuestoProperty(), cell.getValue().cantidadProperty())
        );
        colImpuesto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(monedaFormat.format(item));
            }
        });

        colSubtotal.setCellValueFactory(cell ->
                Bindings.createStringBinding(
                        () -> monedaFormat.format(cell.getValue().totalBinding().get()),
                        cell.getValue().totalBinding()
                )
        );

        colAccion.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("X");
            {
                btn.setStyle("-fx-text-fill: white; -fx-background-color: #e74c3c; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 3;");
                btn.setPrefSize(25, 25);
                btn.setOnAction(e -> {
                    DetalleVentaRow row = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(row);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    public void setVentaParaEdicion(Venta venta) {
        this.ventaEnEdicion = venta;
        this.modoEdicion = true;

        txtNumeroFactura.setText(venta.getNumeroFacturaVenta());
        txtNumeroFactura.setDisable(true);

        comboCliente.setValue(venta.getCliente());
        if(venta.getCliente() != null){
            txtDocumento.setText(venta.getCliente().getNumeroDocumentoCliente());
            txtCorreo.setText(venta.getCliente().getCorreoCliente());
            txtCelular.setText(venta.getCliente().getCelularCliente());
        }

        comboCuenta.setValue(venta.getCuenta());

        dpFechaCreacion.setValue(venta.getFechaVenta());
        dpFechaVencimiento.setValue(venta.getFechaVencimientoVenta());
        comboFormaPago.setValue(venta.getFormaPagoVenta());
        comboMedioPago.setValue(venta.getMedioPagoVenta());

        txtNotas.setText(venta.getNotasVenta());

        List<DetalleVenta> detallesDB = ventasService.findDetallesByVenta(venta);
        List<DetalleVentaRow> rows = new ArrayList<>();

        for (DetalleVenta d : detallesDB) {
            DetalleVentaRow r = new DetalleVentaRow();
            r.setProducto(d.getProducto());
            r.setCantidad(d.getCantidadVenta());
            r.setPrecio(d.getPrecioVenta());
            r.setDescuento(d.getDescuentoVenta());
            r.setImpuesto(d.getImpuestoVenta());
            rows.add(r);
        }

        listaDetalles.setAll(rows);
        recalcularTotalesGenerales();
    }

    private class CantidadCell extends TableCell<DetalleVentaRow, Integer> {
        private final HBox container = new HBox();
        private final TextField tfCantidad = new TextField();
        private final Button btnMenos = new Button("-");
        private final Button btnMas = new Button("+");

        public CantidadCell() {
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

            btnMenos.setOnAction(e -> modificarCantidad(-1));
            btnMas.setOnAction(e -> modificarCantidad(1));

            tfCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    tfCantidad.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });

            tfCantidad.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) confirmarManual();
            });
            tfCantidad.focusedProperty().addListener((o, old, isFocused) -> {
                if (!isFocused) confirmarManual();
            });
        }

        private void modificarCantidad(int delta) {
            DetalleVentaRow row = getTableRow().getItem();
            if (row == null || row.getProducto() == null) return;

            int nuevoVal = row.getCantidad() + delta;

            if (nuevoVal < 1) return;

            if (nuevoVal > row.getProducto().getCantidad()) {
                mostrarAlerta("Stock Máximo", "Solo hay " + row.getProducto().getCantidad() + " unidades disponibles.");
                return;
            }
            row.setCantidad(nuevoVal);
        }

        private void confirmarManual() {
            DetalleVentaRow row = getTableRow().getItem();
            if (row == null) return;

            if (tfCantidad.getText().isEmpty()) {
                tfCantidad.setText(String.valueOf(row.getCantidad()));
                return;
            }

            try {
                int val = Integer.parseInt(tfCantidad.getText());
                if (val < 1) val = 1;

                if (row.getProducto() != null && val > row.getProducto().getCantidad()) {
                    mostrarAlerta("Stock insuficiente", "Máximo disponible: " + row.getProducto().getCantidad());
                    val = row.getProducto().getCantidad();
                }
                row.setCantidad(val);
                tfCantidad.setText(String.valueOf(val));
            } catch (Exception e) {
                tfCantidad.setText(String.valueOf(row.getCantidad()));
            }
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                if (!tfCantidad.isFocused()) {
                    tfCantidad.setText(String.valueOf(item));
                }
                setGraphic(container);
            }
        }
    }

    private class PrecioCell extends TableCell<DetalleVentaRow, Double> {
        private final TextField textField = new TextField();

        public PrecioCell() {
            textField.setAlignment(Pos.CENTER_RIGHT);
            textField.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused) {
                    DetalleVentaRow row = getTableRow().getItem();
                    if (row != null) {
                        double val = row.getPrecio();
                        if (val % 1 == 0) {
                            textField.setText(String.format("%.0f", val));
                        } else {
                            textField.setText(String.format("%.2f", val).replace(".", ","));
                        }
                        textField.selectAll();
                    }
                } else {
                    commitPrecio();
                }
            });

            textField.setOnAction(e -> tablaDetalles.requestFocus());
        }

        private void commitPrecio() {
            DetalleVentaRow row = getTableRow().getItem();
            if (row == null) return;

            String text = textField.getText();
            if (text == null) text = "";

            text = text.replace("$", "").trim();

            if (text.isEmpty() || text.equals("0")) {
                restaurarPrecioOriginal(row);
                return;
            }

            text = text.replace(".", "");
            text = text.replace(",", ".");

            try {
                double val = Double.parseDouble(text);

                if (val <= 0) {
                    restaurarPrecioOriginal(row);
                } else {
                    row.setPrecio(val);
                    textField.setText(monedaFormat.format(val));
                }
            } catch (NumberFormatException e) {
                restaurarPrecioOriginal(row);
            }
        }

        private void restaurarPrecioOriginal(DetalleVentaRow row) {
            if (row.getProducto() != null) {
                double precioOriginal = row.getProducto().getPrecioBrutoProducto();
                row.setPrecio(precioOriginal);
                textField.setText(monedaFormat.format(precioOriginal));
            } else {
                textField.setText(monedaFormat.format(0));
            }
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                if (!textField.isFocused()) {
                    textField.setText(monedaFormat.format(item));
                }
                setGraphic(textField);
            }
        }
    }

    private class PercentageCell extends TableCell<DetalleVentaRow, Double> {
        private final TextField textField = new TextField();

        public PercentageCell() {
            textField.setAlignment(Pos.CENTER);
            textField.setStyle("-fx-background-color: transparent;");

            textField.focusedProperty().addListener((o, old, isFocused) -> {
                if (isFocused) {
                    String raw = textField.getText().replace("%", "").trim();
                    textField.setText(raw);
                    textField.selectAll();
                } else {
                    commit();
                }
            });
            textField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) { commit(); tablaDetalles.requestFocus(); }});
        }

        private void commit() {
            DetalleVentaRow row = getTableRow().getItem();
            if (row == null) return;
            try {
                String txt = textField.getText().replaceAll("[^0-9.]", "");
                if (txt.isEmpty()) txt = "0";
                double val = Double.parseDouble(txt);
                if (val > 100) val = 100;
                row.setDescuento(val);
                textField.setText(String.format("%.0f%%", val));
            } catch (Exception e) {
                textField.setText(String.format("%.0f%%", row.getDescuento()));
            }
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                if (!textField.isFocused()) textField.setText(String.format("%.0f%%", item));
                setGraphic(textField);
            }
        }
    }

    private class ProductoComboCell extends TableCell<DetalleVentaRow, Producto> {
        private final ComboBox<Producto> comboBox;
        private final FilteredList<Producto> filteredItems;
        private boolean isUpdating = false;

        public ProductoComboCell(ObservableList<Producto> allProductos, boolean porCodigo) {
            this.filteredItems = new FilteredList<>(allProductos, p -> true);
            this.comboBox = new ComboBox<>(filteredItems);
            this.comboBox.setEditable(true);
            this.comboBox.setMaxWidth(Double.MAX_VALUE);
            this.comboBox.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

            this.comboBox.setConverter(new StringConverter<>() {
                @Override public String toString(Producto p) {
                    if (p == null) return "";
                    return porCodigo ? p.getCodigoProducto() : p.getDescripcion();
                }
                @Override public Producto fromString(String s) { return comboBox.getValue(); }
            });

            this.comboBox.getEditor().textProperty().addListener((obs, oldTxt, newTxt) -> {
                if (isUpdating) return;
                Platform.runLater(() -> {
                    if (comboBox.getSelectionModel().getSelectedItem() != null) return;
                    filteredItems.setPredicate(p -> {
                        if (newTxt == null || newTxt.isEmpty()) return true;
                        String lower = newTxt.toLowerCase();
                        return p.getDescripcion().toLowerCase().contains(lower) || p.getCodigoProducto().toLowerCase().contains(lower);
                    });
                    if (!comboBox.isShowing() && !filteredItems.isEmpty() && comboBox.isFocused()) {
                        comboBox.show();
                    }
                });
            });

            this.comboBox.setOnAction(e -> confirmarSeleccion());
            this.comboBox.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) confirmarSeleccion(); });
        }

        private void confirmarSeleccion() {
            if (isUpdating) return;
            Producto p = comboBox.getValue();
            if (p != null) {
                DetalleVentaRow row = getTableRow().getItem();
                if (row != null && row.getProducto() != p) {
                    if (p.getCantidad() < 1) {
                        mostrarAlerta("Sin Stock", "Producto agotado.");
                        Platform.runLater(() -> comboBox.getSelectionModel().clearSelection());
                        return;
                    }
                    row.setProducto(p);
                    row.setPrecio(p.getPrecioBrutoProducto());
                    row.setImpuesto(p.getIvaProducto() != null ? p.getIvaProducto() : 0.0);
                    row.setDescuento(0.0);
                    row.setCantidad(1);
                    tablaDetalles.refresh();
                }
            }
        }

        @Override
        protected void updateItem(Producto item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                isUpdating = true;
                comboBox.setValue(item);
                isUpdating = false;
                setGraphic(comboBox);
            }
        }
    }

    private void configurarBuscadorCliente() {
        FilteredList<Cliente> clientesFiltrados = new FilteredList<>(todosLosClientes, p -> true);
        comboCliente.setItems(clientesFiltrados);
        comboCliente.setConverter(new StringConverter<>() {
            @Override public String toString(Cliente c) { return c == null ? "" : c.getNombreCliente(); }
            @Override public Cliente fromString(String string) { return comboCliente.getValue(); }
        });

        comboCliente.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (comboCliente.getValue() != null && comboCliente.getValue().getNombreCliente().equals(newText)) return;
                clientesFiltrados.setPredicate(cliente -> {
                    if (newText == null || newText.isEmpty()) return true;
                    String lower = newText.toLowerCase();
                    return cliente.getNombreCliente().toLowerCase().contains(lower) ||
                            cliente.getNumeroDocumentoCliente().contains(lower);
                });
                if (!clientesFiltrados.isEmpty() && !comboCliente.isShowing() && comboCliente.isFocused()) {
                    comboCliente.show();
                }
            });
        });

        comboCliente.setOnAction(e -> {
            Cliente seleccionado = comboCliente.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                txtDocumento.setText(seleccionado.getNumeroDocumentoCliente());
                txtCorreo.setText(seleccionado.getCorreoCliente());
                txtCelular.setText(seleccionado.getCelularCliente());
                comboCliente.getEditor().setText(seleccionado.getNombreCliente());
            }
        });
    }

    private void configurarFechasYCombos() {
        dpFechaCreacion.setValue(LocalDate.now());
        dpFechaVencimiento.setValue(LocalDate.now());

        comboFormaPago.getItems().setAll("Contado", "Crédito");
        comboMedioPago.getItems().addAll("Efectivo", "Transferencia", "Nequi", "Tarjeta");
        comboTipoDoc.getItems().addAll("CC", "NIT", "RUT");
        comboCuenta.setItems(todasLasCuentas);

        comboCuenta.setConverter(new StringConverter<Cuenta>() {
            @Override public String toString(Cuenta c) { return c == null ? "" : c.getNombreCuenta(); }
            @Override public Cuenta fromString(String string) { return comboCuenta.getValue(); }
        });

        Runnable actualizarFechas = () -> {
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

        comboFormaPago.setOnAction(e -> actualizarFechas.run());
        dpFechaCreacion.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFechas.run());
    }

    private void recalcularTotalesGenerales() {
        double subtotal = 0;
        double descuentos = 0;
        double total = 0;

        for (DetalleVentaRow row : listaDetalles) {
            if (row.getProducto() != null) {
                double precioTotal = row.getPrecio() * row.getCantidad();
                double descMonto = precioTotal * (row.getDescuento() / 100.0);

                subtotal += precioTotal;
                descuentos += descMonto;
                total += row.getTotalLinea();
            }
        }

        lblSubtotal.setText(monedaFormat.format(subtotal));
        lblDescuentos.setText(monedaFormat.format(descuentos));
        lblTotalGeneral.setText(monedaFormat.format(total));
    }

    @FXML void btnAgregarLineaClick(ActionEvent event) { agregarLinea(); }

    private void agregarLinea() {
        listaDetalles.add(new DetalleVentaRow());
        tablaDetalles.scrollTo(listaDetalles.size() - 1);
    }

    @FXML
    void btnGuardarClick(ActionEvent event) {
        if (listaDetalles.isEmpty() || listaDetalles.stream().noneMatch(r -> r.getProducto() != null)) {
            mostrarAlerta("Error", "Debe agregar al menos un producto válido.");
            return;
        }
        if (comboCuenta.getValue() == null) {
            mostrarAlerta("Error", "Debe seleccionar una Cuenta.");
            return;
        }

        try {
            Cliente cliente = guardarOObtenerCliente();

            Venta venta = modoEdicion ? ventaEnEdicion : new Venta();

            venta.setNumeroFacturaVenta(txtNumeroFactura.getText());
            venta.setCliente(cliente);
            venta.setFechaVenta(dpFechaCreacion.getValue());
            venta.setFechaVencimientoVenta(dpFechaVencimiento.getValue());
            venta.setFormaPagoVenta(comboFormaPago.getValue());
            venta.setMedioPagoVenta(comboMedioPago.getValue());
            venta.setCuenta(comboCuenta.getValue());
            venta.setNotasVenta(txtNotas.getText());

            double totalFinal = listaDetalles.stream()
                    .filter(r -> r.getProducto() != null)
                    .mapToDouble(DetalleVentaRow::getTotalLinea).sum();

            venta.setTotalVenta(totalFinal);

            venta.setSaldoPendiente("Crédito".equals(comboFormaPago.getValue()) ? totalFinal : 0.0);

            venta.setEstadoVenta("Crédito".equals(comboFormaPago.getValue()) ? "PENDIENTE" : "PAGADA");


            List<DetalleVenta> detalles = listaDetalles.stream()
                    .filter(r -> r.getProducto() != null)
                    .map(row -> {
                        DetalleVenta det = new DetalleVenta();
                        det.setProducto(row.getProducto());
                        det.setCantidadVenta(row.getCantidad());
                        det.setPrecioVenta(row.getPrecio());
                        det.setDescuentoVenta(row.getDescuento());
                        det.setImpuestoVenta(row.getImpuesto());
                        det.setSubtotalVenta(row.getTotalLinea());
                        return det;
                    })
                    .toList();

            ventasService.guardarVentaConDetalles(venta, detalles, modoEdicion);

            mostrarAlerta("Éxito", "Venta guardada correctamente.");
            navegarHaciaAtras();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error Crítico", "No se pudo guardar la venta: " + e.getMessage());
        }
    }

    private Cliente guardarOObtenerCliente() {
        Cliente seleccionado = comboCliente.getValue();
        String nombreEscrito = comboCliente.getEditor().getText();
        String docEscrito = txtDocumento.getText();

        return ventasService.guardarOActualizarCliente(
                seleccionado,
                nombreEscrito,
                docEscrito,
                txtCorreo.getText(),
                txtCelular.getText()
        );
    }

    @FXML void btnCancelarClick(ActionEvent event) { navegarHaciaAtras(); }

    private void navegarHaciaAtras() {
        MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/FacturasVenta.fxml");
    }

    private void mostrarAlerta(String titulo, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        });
    }
}

class DetalleVentaRow {
    private final ObjectProperty<Producto> producto = new SimpleObjectProperty<>();
    private final DoubleProperty precio = new SimpleDoubleProperty(0.0);
    private final IntegerProperty cantidad = new SimpleIntegerProperty(1);
    private final DoubleProperty descuento = new SimpleDoubleProperty(0.0);
    private final DoubleProperty impuesto = new SimpleDoubleProperty(0.0);

    public DetalleVentaRow() {}

    public javafx.beans.binding.DoubleBinding totalBinding() {
        return Bindings.createDoubleBinding(() -> {
            double precioTotal = getPrecio() * getCantidad();
            double descMonto = precioTotal * (getDescuento() / 100.0);
            double base = precioTotal - descMonto;
            double impuestoTotal = getImpuesto() * getCantidad();
            return base + impuestoTotal;
        }, precio, cantidad, descuento, impuesto);
    }

    public double getTotalLinea() { return totalBinding().get(); }

    public ObjectProperty<Producto> productoProperty() { return producto; }
    public Producto getProducto() { return producto.get(); }
    public void setProducto(Producto p) { this.producto.set(p); }

    public DoubleProperty precioProperty() { return precio; }
    public double getPrecio() { return precio.get(); }
    public void setPrecio(double d) { this.precio.set(d); }

    public IntegerProperty cantidadProperty() { return cantidad; }
    public int getCantidad() { return cantidad.get(); }
    public void setCantidad(int i) { this.cantidad.set(i); }

    public DoubleProperty descuentoProperty() { return descuento; }
    public double getDescuento() { return descuento.get(); }
    public void setDescuento(double d) { this.descuento.set(d); }

    public DoubleProperty impuestoProperty() { return impuesto; }
    public double getImpuesto() { return impuesto.get(); }
    public void setImpuesto(double d) { this.impuesto.set(d); }
}