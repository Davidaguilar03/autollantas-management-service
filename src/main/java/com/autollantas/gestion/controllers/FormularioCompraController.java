package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.*;
import com.autollantas.gestion.repository.*;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class FormularioCompraController {

    @Autowired private CompraRepository compraRepo;
    @Autowired private DetalleCompraRepository detalleRepo;
    @Autowired private ProveedorRepository proveedorRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private CuentaRepository cuentaRepo;

    @FXML private VBox rootFormulario;
    @FXML private TextField txtNumeroFactura;

    @FXML private ComboBox<Proveedor> comboProveedor;
    @FXML private TextField txtNit;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtCelular;

    @FXML private DatePicker dpFechaCompra;
    @FXML private DatePicker dpFechaVencimiento;
    @FXML private ComboBox<String> comboFormaPago;
    @FXML private ComboBox<String> comboMedioPago;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private TextArea txtNotas;
    @FXML private Button btnGuardar;

    @FXML private TableView<DetalleCompraRow> tablaDetalles;
    @FXML private TableColumn<DetalleCompraRow, Producto> colCodigo;
    @FXML private TableColumn<DetalleCompraRow, Producto> colDescripcion;
    @FXML private TableColumn<DetalleCompraRow, Integer> colCantidad;
    @FXML private TableColumn<DetalleCompraRow, Double> colPrecio;
    @FXML private TableColumn<DetalleCompraRow, Double> colDescuento;
    @FXML private TableColumn<DetalleCompraRow, Double> colImpuesto;
    @FXML private TableColumn<DetalleCompraRow, String> colSubtotal;
    @FXML private TableColumn<DetalleCompraRow, Void> colAccion;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuentos;
    @FXML private Label lblTotalGeneral;

    private ObservableList<DetalleCompraRow> listaDetalles;
    private ObservableList<Proveedor> todosLosProveedores;
    private ObservableList<Producto> todosLosProductos;
    private ObservableList<Cuenta> todasLasCuentas;

    private Compra compraEnEdicion;
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
        configurarBuscadorProveedor();
        configurarFechasYCombos();

        listaDetalles.addListener((ListChangeListener<DetalleCompraRow>) c -> recalcularTotalesGenerales());

        if (!modoEdicion) {
            agregarLinea();
        }
    }

    private void cargarDatosIniciales() {
        todosLosProveedores = FXCollections.observableArrayList(proveedorRepo.findAll());
        todosLosProductos = FXCollections.observableArrayList(productoRepo.findAll());
        todasLasCuentas = FXCollections.observableArrayList(cuentaRepo.findAll());

        generarSiguienteNumeroFactura();
    }

    private void generarSiguienteNumeroFactura() {
        try {
            List<Compra> compras = compraRepo.findAll();
            long maximo = 0;

            for (Compra c : compras) {
                String numeroFactura = c.getNumeroFacturaCompra();
                if (numeroFactura != null && !numeroFactura.isEmpty()) {
                    String soloNumeros = numeroFactura.replaceAll("\\D+", "");
                    if (!soloNumeros.isEmpty()) {
                        long actual = Long.parseLong(soloNumeros);
                        if (actual > maximo) {
                            maximo = actual;
                        }
                    }
                }
            }

            long siguienteNumero = maximo > 0 ? maximo + 1 : 1;

            txtNumeroFactura.setText(String.format("FAC-%05d", siguienteNumero));

        } catch (Exception e) {
            e.printStackTrace();
            txtNumeroFactura.setText("FAC-00001");
        }
    }

    private void configurarBuscadorProveedor() {
        FilteredList<Proveedor> filtrados = new FilteredList<>(todosLosProveedores, p -> true);
        comboProveedor.setItems(filtrados);

        comboProveedor.setConverter(new StringConverter<>() {
            @Override public String toString(Proveedor p) { return p == null ? "" : p.getNombreProveedor(); }
            @Override public Proveedor fromString(String string) { return comboProveedor.getValue(); }
        });

        comboProveedor.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (comboProveedor.getValue() != null &&
                        comboProveedor.getValue().getNombreProveedor().equals(newText)) {
                    return;
                }

                filtrados.setPredicate(prov -> {
                    if (newText == null || newText.isEmpty()) return true;
                    String lower = newText.toLowerCase();
                    return prov.getNombreProveedor().toLowerCase().contains(lower) ||
                            prov.getNumeroNitProveedor().contains(lower);
                });

                if (!filtrados.isEmpty() && !comboProveedor.isShowing() && comboProveedor.isFocused()) {
                    comboProveedor.show();
                }
            });
        });

        comboProveedor.setOnAction(e -> {
            Proveedor sel = comboProveedor.getSelectionModel().getSelectedItem();
            if (sel != null) {
                txtNit.setText(sel.getNumeroNitProveedor());
                txtCorreo.setText(sel.getCorreoProveedor());
                txtCelular.setText(sel.getCelularProveedor());
                comboProveedor.getEditor().setText(sel.getNombreProveedor());
            }
        });
    }

    private void configurarFechasYCombos() {
        if (!modoEdicion) {
            dpFechaCompra.setValue(LocalDate.now());
            dpFechaVencimiento.setValue(LocalDate.now());
            comboFormaPago.getSelectionModel().select("Contado");
        }

        comboFormaPago.getItems().setAll("Contado", "Crédito");
        comboMedioPago.getItems().addAll("Efectivo", "Transferencia", "Nequi", "Tarjeta");
        comboCuenta.setItems(todasLasCuentas);

        comboCuenta.setConverter(new StringConverter<Cuenta>() {
            @Override public String toString(Cuenta c) { return c == null ? "" : c.getNombreCuenta(); }
            @Override public Cuenta fromString(String string) { return comboCuenta.getValue(); }
        });

        Runnable actualizarFechas = () -> {
            String pago = comboFormaPago.getValue();
            LocalDate creacion = dpFechaCompra.getValue();
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
        dpFechaCompra.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFechas.run());
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
                    double precioBase = cell.getValue().getPrecio();
                    int cant = cell.getValue().getCantidad();
                    return unitario * cant;
                }, cell.getValue().impuestoProperty(), cell.getValue().cantidadProperty(), cell.getValue().precioProperty())
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
                    DetalleCompraRow row = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(row);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    public void setCompraParaEdicion(Compra compra) {
        this.compraEnEdicion = compra;
        this.modoEdicion = true;

        txtNumeroFactura.setText(compra.getNumeroFacturaCompra());
        txtNumeroFactura.setDisable(true);

        comboProveedor.setValue(compra.getProveedor());
        if(compra.getProveedor() != null){
            txtNit.setText(compra.getProveedor().getNumeroNitProveedor());
            txtCorreo.setText(compra.getProveedor().getCorreoProveedor());
            txtCelular.setText(compra.getProveedor().getCelularProveedor());
        }

        comboCuenta.setValue(compra.getCuenta());

        dpFechaCompra.setValue(compra.getFechaCompra());
        dpFechaVencimiento.setValue(compra.getFechaVencimientoCompra());
        comboFormaPago.setValue(compra.getFormaPagoCompra());
        comboMedioPago.setValue(compra.getMedioPagoCompra());

        txtNotas.setText(compra.getNotasCompra());

        List<DetalleCompra> detallesDB = detalleRepo.findByCompra(compra);
        List<DetalleCompraRow> rows = new ArrayList<>();

        for (DetalleCompra d : detallesDB) {
            DetalleCompraRow r = new DetalleCompraRow();
            r.setProducto(d.getProducto());
            r.setCantidad(d.getCantidadCompra());
            r.setPrecio(d.getPrecioCompra());
            r.setDescuento(d.getDescuentoCompra());
            r.setImpuesto(d.getImpuestoCompra());
            rows.add(r);
        }

        listaDetalles.setAll(rows);
        recalcularTotalesGenerales();

        if (btnGuardar != null) {
            btnGuardar.setText("Actualizar Compra");
        }
    }

    private void recalcularTotalesGenerales() {
        double subtotal = 0;
        double descuentos = 0;
        double total = 0;

        for (DetalleCompraRow row : listaDetalles) {
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
        listaDetalles.add(new DetalleCompraRow());
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
            Proveedor proveedor = guardarOObtenerProveedor();

            Compra compra = modoEdicion ? compraEnEdicion : new Compra();

            compra.setNumeroFacturaCompra(txtNumeroFactura.getText());
            compra.setProveedor(proveedor);
            compra.setFechaCompra(dpFechaCompra.getValue());
            compra.setFechaVencimientoCompra(dpFechaVencimiento.getValue());
            compra.setFormaPagoCompra(comboFormaPago.getValue());
            compra.setMedioPagoCompra(comboMedioPago.getValue());
            compra.setCuenta(comboCuenta.getValue());
            compra.setNotasCompra(txtNotas.getText());

            double totalFinal = listaDetalles.stream()
                    .filter(r -> r.getProducto() != null)
                    .mapToDouble(DetalleCompraRow::getTotalLinea).sum();

            compra.setTotalCompra(totalFinal);
            compra.setSaldoPendiente("Crédito".equals(comboFormaPago.getValue()) ? totalFinal : 0.0);
            compra.setEstadoCompra("Crédito".equals(comboFormaPago.getValue()) ? "PENDIENTE" : "PAGADA");

            Compra compraGuardada = compraRepo.save(compra);

            if (modoEdicion) {
                List<DetalleCompra> detallesAntiguos = detalleRepo.findByCompra(compraGuardada);
                for (DetalleCompra oldDet : detallesAntiguos) {
                    Producto p = oldDet.getProducto();
                    if (p != null) {
                        p.setCantidad(p.getCantidad() - oldDet.getCantidadCompra());
                        productoRepo.save(p);
                    }
                }
                detalleRepo.deleteAll(detallesAntiguos);
            }

            for (DetalleCompraRow row : listaDetalles) {
                if (row.getProducto() == null) continue;

                DetalleCompra det = new DetalleCompra();
                det.setCompra(compraGuardada);
                det.setProducto(row.getProducto());
                det.setCantidadCompra(row.getCantidad());
                det.setPrecioCompra(row.getPrecio());
                det.setDescuentoCompra(row.getDescuento());
                det.setImpuestoCompra(row.getImpuesto());
                det.setSubtotalCompra(row.getTotalLinea());
                detalleRepo.save(det);

                Producto p = row.getProducto();
                Optional<Producto> pOpt = productoRepo.findById(p.getIdProducto());
                if(pOpt.isPresent()) {
                    Producto pReal = pOpt.get();
                    pReal.setCantidad(pReal.getCantidad() + row.getCantidad());
                    productoRepo.save(pReal);
                }
            }

            mostrarAlerta("Éxito", "Compra guardada correctamente.");
            navegarHaciaAtras();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error Crítico", "No se pudo guardar la compra: " + e.getMessage());
        }
    }

    private Proveedor guardarOObtenerProveedor() {
        Proveedor seleccionado = comboProveedor.getValue();
        String nombreEscrito = comboProveedor.getEditor().getText();
        String nitEscrito = txtNit.getText();

        if (seleccionado != null && seleccionado.getNombreProveedor().equalsIgnoreCase(nombreEscrito)) {
            seleccionado.setNumeroNitProveedor(nitEscrito);
            seleccionado.setCorreoProveedor(txtCorreo.getText());
            seleccionado.setCelularProveedor(txtCelular.getText());
            return proveedorRepo.save(seleccionado);
        }

        Optional<Proveedor> existente = proveedorRepo.findByNumeroNitProveedor(nitEscrito);
        Proveedor p = existente.orElse(new Proveedor());

        p.setNombreProveedor(nombreEscrito);
        p.setNumeroNitProveedor(nitEscrito);
        p.setCorreoProveedor(txtCorreo.getText());
        p.setCelularProveedor(txtCelular.getText());

        return proveedorRepo.save(p);
    }

    @FXML void btnCancelarClick(ActionEvent event) { navegarHaciaAtras(); }

    private void navegarHaciaAtras() {
        MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/FacturasCompra.fxml");
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

    private class CantidadCell extends TableCell<DetalleCompraRow, Integer> {
        private final HBox container = new HBox();
        private final TextField tfCantidad = new TextField();
        private final Button btnMenos = new Button("-");
        private final Button btnMas = new Button("+");

        public CantidadCell() {
            tfCantidad.setPrefWidth(40);
            tfCantidad.setAlignment(Pos.CENTER);
            String btnStyle = "-fx-background-color: #bdc3c7; -fx-font-weight: bold; -fx-min-width: 25px;";
            btnMenos.setStyle(btnStyle); btnMas.setStyle(btnStyle);
            btnMenos.setFocusTraversable(false); btnMas.setFocusTraversable(false);

            container.getChildren().addAll(btnMenos, tfCantidad, btnMas);
            container.setAlignment(Pos.CENTER);
            container.setSpacing(3);

            btnMenos.setOnAction(e -> modificarCantidad(-1));
            btnMas.setOnAction(e -> modificarCantidad(1));

            tfCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) tfCantidad.setText(newVal.replaceAll("[^\\d]", ""));
            });
            tfCantidad.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) confirmar(); });
            tfCantidad.focusedProperty().addListener((o, old, isFocused) -> { if (!isFocused) confirmar(); });
        }

        private void modificarCantidad(int delta) {
            DetalleCompraRow row = getTableRow().getItem();
            if (row == null) return;
            int nuevoVal = row.getCantidad() + delta;
            if (nuevoVal < 1) return;
            row.setCantidad(nuevoVal);
        }

        private void confirmar() {
            DetalleCompraRow row = getTableRow().getItem();
            if (row == null) return;
            try {
                int val = Integer.parseInt(tfCantidad.getText());
                if (val < 1) val = 1;
                row.setCantidad(val);
                tfCantidad.setText(String.valueOf(val));
            } catch (Exception e) { tfCantidad.setText(String.valueOf(row.getCantidad())); }
        }

        @Override protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                if (!tfCantidad.isFocused()) tfCantidad.setText(String.valueOf(item));
                setGraphic(container);
            }
        }
    }

    private class PrecioCell extends TableCell<DetalleCompraRow, Double> {
        private final TextField textField = new TextField();
        public PrecioCell() {
            textField.setAlignment(Pos.CENTER_RIGHT);
            textField.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
            textField.focusedProperty().addListener((obs, was, isNow) -> {
                if (isNow) {
                    DetalleCompraRow row = getTableRow().getItem();
                    if (row != null) {
                        double val = row.getPrecio();
                        if (val % 1 == 0) textField.setText(String.format("%.0f", val));
                        else textField.setText(String.format("%.2f", val).replace(".", ","));
                        textField.selectAll();
                    }
                } else commitPrecio();
            });
            textField.setOnAction(e -> tablaDetalles.requestFocus());
        }
        private void commitPrecio() {
            DetalleCompraRow row = getTableRow().getItem();
            if (row == null) return;
            String text = textField.getText().replace("$", "").trim();
            if (text.isEmpty() || text.equals("0")) { restaurarPrecioOriginal(row); return; }
            text = text.replace(".", "").replace(",", ".");
            try {
                double val = Double.parseDouble(text);
                if (val <= 0) restaurarPrecioOriginal(row);
                else { row.setPrecio(val); textField.setText(monedaFormat.format(val)); }
            } catch (Exception e) { restaurarPrecioOriginal(row); }
        }
        private void restaurarPrecioOriginal(DetalleCompraRow row) {
            if (row.getProducto() != null) {
                row.setPrecio(row.getProducto().getPrecioBrutoProducto());
                textField.setText(monedaFormat.format(row.getPrecio()));
            } else textField.setText(monedaFormat.format(0));
        }
        @Override protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                if (!textField.isFocused()) textField.setText(monedaFormat.format(item));
                setGraphic(textField);
            }
        }
    }

    private class PercentageCell extends TableCell<DetalleCompraRow, Double> {
        private final TextField textField = new TextField();
        public PercentageCell() {
            textField.setAlignment(Pos.CENTER);
            textField.setStyle("-fx-background-color: transparent;");
            textField.focusedProperty().addListener((o, old, isFocused) -> {
                if (isFocused) {
                    String raw = textField.getText().replace("%", "").trim();
                    textField.setText(raw);
                    textField.selectAll();
                } else commit();
            });
            textField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) { commit(); tablaDetalles.requestFocus(); }});
        }
        private void commit() {
            DetalleCompraRow row = getTableRow().getItem();
            if (row == null) return;
            try {
                String txt = textField.getText().replaceAll("[^0-9.]", "");
                if (txt.isEmpty()) txt = "0";
                double val = Double.parseDouble(txt);
                if (val > 100) val = 100;
                row.setDescuento(val);
                textField.setText(String.format("%.0f%%", val));
            } catch (Exception e) { textField.setText(String.format("%.0f%%", row.getDescuento())); }
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

    private class ProductoComboCell extends TableCell<DetalleCompraRow, Producto> {
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
        }

        private void confirmarSeleccion() {
            if (isUpdating) return;
            Producto p = comboBox.getValue();
            if (p != null) {
                DetalleCompraRow row = getTableRow().getItem();
                if (row != null && row.getProducto() != p) {
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
            if (empty) setGraphic(null);
            else {
                isUpdating = true;
                comboBox.setValue(item);
                isUpdating = false;
                setGraphic(comboBox);
            }
        }
    }
}

class DetalleCompraRow {
    private final ObjectProperty<Producto> producto = new SimpleObjectProperty<>();
    private final DoubleProperty precio = new SimpleDoubleProperty(0.0);
    private final IntegerProperty cantidad = new SimpleIntegerProperty(1);
    private final DoubleProperty descuento = new SimpleDoubleProperty(0.0);
    private final DoubleProperty impuesto = new SimpleDoubleProperty(0.0);

    public DetalleCompraRow() {}

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