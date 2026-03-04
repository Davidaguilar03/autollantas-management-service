package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.repository.CompraRepository;
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
public class PagosController {

    @Autowired private CompraRepository compraRepo;
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

    @FXML private TableView<Compra> tablaPagos;
    @FXML private TableColumn<Compra, String> colNumero;
    @FXML private TableColumn<Compra, String> colProveedor;
    @FXML private TableColumn<Compra, LocalDate> colFechaCreacion;
    @FXML private TableColumn<Compra, LocalDate> colFechaVencimiento;
    @FXML private TableColumn<Compra, String> colFormaPago;
    @FXML private TableColumn<Compra, String> colMedioPago;
    @FXML private TableColumn<Compra, String> colCuenta;
    @FXML private TableColumn<Compra, Double> colTotal;
    @FXML private TableColumn<Compra, Double> colPagado;
    @FXML private TableColumn<Compra, Double> colPorPagar;
    @FXML private TableColumn<Compra, String> colEstado;

    @FXML private Button btnVerDetalle;
    @FXML private Button btnRegistrarPago;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Compra> masterData = FXCollections.observableArrayList();
    private FilteredList<Compra> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Compra> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaPagos.comparatorProperty());
        tablaPagos.setItems(sortedData);

        configurarColumnas();
        configurarFiltrosUI();
        configurarConvertersFecha();
        setupListenersBusqueda();
        setupInteractionListeners();
        cargarDatosDB();

        btnVerDetalle.disableProperty().bind(
                tablaPagos.getSelectionModel().selectedItemProperty().isNull()
        );

        if (btnRegistrarPago != null) {

            tablaPagos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                boolean deshabilitar = true;

                if (newSelection != null) {
                    boolean estaPagada = "PAGADA".equalsIgnoreCase(newSelection.getEstadoCompra());
                    boolean estaAnulada = "ANULADA".equalsIgnoreCase(newSelection.getEstadoCompra());

                    double saldoPendiente = 0.0;

                    if (estaAnulada) {
                        saldoPendiente = 0.0;
                    } else if (newSelection.getSaldoPendiente() != null) {
                        saldoPendiente = newSelection.getSaldoPendiente();
                    } else {
                        saldoPendiente = estaPagada ? 0.0 : newSelection.getTotalCompra();
                    }

                    if (!estaPagada && !estaAnulada && saldoPendiente > 0) {
                        deshabilitar = false;
                    }
                }

                btnRegistrarPago.setDisable(deshabilitar);
            });
        }

        tablaPagos.setOnMouseClicked(event -> {
            javafx.scene.Node nodoClic = event.getPickResult().getIntersectedNode();
            boolean clickEnFilaValida = false;
            while (nodoClic != null && nodoClic != tablaPagos) {
                if (nodoClic instanceof TableRow) {
                    TableRow<?> fila = (TableRow<?>) nodoClic;
                    if (!fila.isEmpty()) clickEnFilaValida = true;
                    break;
                }
                nodoClic = nodoClic.getParent();
            }
            if (!clickEnFilaValida) {
                tablaPagos.getSelectionModel().clearSelection();
            } else if (event.getClickCount() == 2) {
                Compra seleccionada = tablaPagos.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    abrirHistorialPagos(seleccionada);
                }
            }
        });
    }

    private void setupInteractionListeners() {
        tablaPagos.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Compra seleccionada = tablaPagos.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    abrirHistorialPagos(seleccionada);
                    event.consume();
                }
            }
        });
    }

    private void cargarDatosDB() {
        if (compraRepo == null) return;
        Platform.runLater(() -> {
            try {
                List<Compra> lista = compraRepo.findAll();
                masterData.setAll(lista);
                actualizarInfoRegistros();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void btnRegistrarPagoClick(ActionEvent event) {
        Compra seleccion = tablaPagos.getSelectionModel().getSelectedItem();
        if (seleccion == null) return;

        if ("PAGADA".equalsIgnoreCase(seleccion.getEstadoCompra())) {
            mostrarAlerta("Factura Saldada", "Esta factura ya está totalmente pagada.");
            return;
        }

        if ("ANULADA".equalsIgnoreCase(seleccion.getEstadoCompra())) {
            mostrarAlerta("Información", "No se puede registrar un pago para una factura anulada.");
            return;
        }

        abrirFormularioPago(seleccion);
    }

    @FXML
    void btnVerDetalleClick(ActionEvent event) {
        Compra seleccion = tablaPagos.getSelectionModel().getSelectedItem();
        if(seleccion != null) {
            abrirHistorialPagos(seleccion);
        }
    }

    private void abrirFormularioPago(Compra compra) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/FormularioPago.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            FormularioPagoController controller = loader.getController();
            controller.setCompra(compra);

            crearYMostrarModal(root, controller);

            if (controller.isGuardado()) {
                cargarDatosDB();
            }

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la ventana de pagos: " + e.getMessage());
        }
    }

    private void abrirHistorialPagos(Compra compra) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/HistorialPagos.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            HistorialPagosController controller = loader.getController();
            controller.setCompra(compra);

            crearYMostrarModal(root, null);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar el historial: " + e.getMessage());
        }
    }

    private void crearYMostrarModal(Parent root, Object controller) {
        Stage modalStage = new Stage();
        modalStage.initStyle(StageStyle.TRANSPARENT);
        modalStage.initModality(Modality.APPLICATION_MODAL);

        Stage ventanaPrincipal = (Stage) tablaPagos.getScene().getWindow();
        modalStage.initOwner(ventanaPrincipal);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                modalStage.close();
            }
        });

        modalStage.setScene(scene);

        modalStage.setX(ventanaPrincipal.getX());
        modalStage.setY(ventanaPrincipal.getY());
        modalStage.setWidth(ventanaPrincipal.getWidth());
        modalStage.setHeight(ventanaPrincipal.getHeight());

        ventanaPrincipal.widthProperty().addListener((obs, oldV, newV) -> modalStage.setWidth(newV.doubleValue()));
        ventanaPrincipal.heightProperty().addListener((obs, oldV, newV) -> modalStage.setHeight(newV.doubleValue()));
        ventanaPrincipal.xProperty().addListener((obs, oldV, newV) -> modalStage.setX(newV.doubleValue()));
        ventanaPrincipal.yProperty().addListener((obs, oldV, newV) -> modalStage.setY(newV.doubleValue()));

        modalStage.showAndWait();
    }

    private void actualizarInfoRegistros() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configurarColumnas() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroFacturaCompra"));
        estilizarColumnaTexto(colNumero, Pos.CENTER);

        colProveedor.setCellValueFactory(cell -> {
            if (cell.getValue().getProveedor() != null) return new SimpleStringProperty(cell.getValue().getProveedor().getNombreProveedor());
            return new SimpleStringProperty("-");
        });
        estilizarColumnaTexto(colProveedor, Pos.CENTER);

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colFechaCreacion.setCellFactory(col -> crearCeldaFecha());

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimientoCompra"));
        colFechaVencimiento.setCellFactory(col -> crearCeldaFecha());

        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPagoCompra"));
        estilizarColumnaTexto(colFormaPago, Pos.CENTER);

        colMedioPago.setCellValueFactory(new PropertyValueFactory<>("medioPagoCompra"));
        estilizarColumnaTexto(colMedioPago, Pos.CENTER);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null) return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            return new SimpleStringProperty("N/A");
        });
        estilizarColumnaTexto(colCuenta, Pos.CENTER);

        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCompra"));
        colTotal.setCellFactory(col -> crearCeldaMoneda(false));

        colPagado.setCellValueFactory(cell -> {
            Compra c = cell.getValue();
            if ("ANULADA".equalsIgnoreCase(c.getEstadoCompra())) return new SimpleObjectProperty<>(0.0);

            double total = c.getTotalCompra() != null ? c.getTotalCompra() : 0.0;
            double pendiente = (c.getSaldoPendiente() != null) ? c.getSaldoPendiente() : total;

            if ("PAGADA".equalsIgnoreCase(c.getEstadoCompra())) pendiente = 0.0;
            else if (c.getSaldoPendiente() == null) pendiente = total;

            return new SimpleObjectProperty<>(total - pendiente);
        });
        colPagado.setCellFactory(col -> crearCeldaMoneda(false));

        colPorPagar.setCellValueFactory(cell -> {
            Compra c = cell.getValue();
            double pendiente;

            if ("ANULADA".equalsIgnoreCase(c.getEstadoCompra())) pendiente = 0.0;
            else if ("PAGADA".equalsIgnoreCase(c.getEstadoCompra())) pendiente = 0.0;
            else pendiente = (c.getSaldoPendiente() != null) ? c.getSaldoPendiente() : c.getTotalCompra();

            return new SimpleObjectProperty<>(pendiente);
        });
        colPorPagar.setCellFactory(col -> crearCeldaMoneda(true));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoCompra"));
        colEstado.setCellFactory(col -> crearCeldaEstado());
    }


    private void estilizarColumnaTexto(TableColumn<Compra, String> col, Pos pos) {
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

    private TableCell<Compra, LocalDate> crearCeldaFecha() {
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

    private TableCell<Compra, Double> crearCeldaMoneda(boolean usarColorDeuda) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER);

                    lbl.setMaxWidth(Double.MAX_VALUE);

                    Color baseColor = Color.BLACK;
                    if (usarColorDeuda) {
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

    private TableCell<Compra, String> crearCeldaEstado() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setStyle("-fx-font-weight: bold;");

                    Color baseColor;
                    if ("PAGADA".equalsIgnoreCase(item)) {
                        baseColor = ESTADO_PAGADA;
                    } else if ("PENDIENTE".equalsIgnoreCase(item)) {
                        baseColor = ESTADO_PENDIENTE;
                    } else if ("ANULADA".equalsIgnoreCase(item)) {
                        baseColor = ESTADO_ANULADA;
                    } else {
                        baseColor = Color.BLACK;
                    }

                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private void setupListenersBusqueda() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> aplicarFiltros();
        txtNumero.textProperty().addListener(changeListener);
        txtProveedor.textProperty().addListener(changeListener);
        txtTotal.textProperty().addListener(changeListener);
        comboEstado.valueProperty().addListener(changeListener);
        comboFormaPago.valueProperty().addListener(changeListener);
        comboMedioPago.valueProperty().addListener(changeListener);
        dpFechaDesde.valueProperty().addListener(changeListener);
        dpFechaHasta.valueProperty().addListener(changeListener);
        dpVencimientoDesde.valueProperty().addListener(changeListener);
        dpVencimientoHasta.valueProperty().addListener(changeListener);
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(c -> {
            if (!matchTexto(c.getNumeroFacturaCompra(), txtNumero.getText())) return false;
            String nomProv = (c.getProveedor() != null) ? c.getProveedor().getNombreProveedor() : "";
            if (!matchTexto(nomProv, txtProveedor.getText())) return false;
            String totalInput = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!totalInput.isEmpty() && !String.valueOf(c.getTotalCompra().longValue()).startsWith(totalInput)) return false;
            if (!matchCombo(comboEstado, c.getEstadoCompra())) return false;
            if (!matchCombo(comboFormaPago, c.getFormaPagoCompra())) return false;
            if (!matchCombo(comboMedioPago, c.getMedioPagoCompra())) return false;
            if (fueraDeRango(c.getFechaCompra(), dpFechaDesde.getValue(), dpFechaHasta.getValue())) return false;
            if (fueraDeRango(c.getFechaVencimientoCompra(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;
            return true;
        });
        actualizarInfoRegistros();
    }

    private boolean matchTexto(String valor, String filtro) {
        return filtro == null || filtro.isEmpty() || (valor != null && valor.toLowerCase().contains(filtro.toLowerCase()));
    }
    private boolean matchCombo(ComboBox<String> combo, String valor) {
        String sel = combo.getValue();
        return sel == null || sel.equals("Todas") || sel.equals("Todos") || (valor != null && valor.equalsIgnoreCase(sel));
    }
    private boolean fueraDeRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return true;
        if (desde != null && fecha.isBefore(desde)) return true;
        if (hasta != null && fecha.isAfter(hasta)) return true;
        return false;
    }
    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }
    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtNumero.clear(); txtProveedor.clear(); txtTotal.clear();
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.getSelectionModel().selectFirst();
        dpFechaDesde.setValue(null); dpFechaHasta.setValue(null);
        dpVencimientoDesde.setValue(null); dpVencimientoHasta.setValue(null);
        aplicarFiltros();
    }
    private void configurarConvertersFecha() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? dateFormat.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, dateFormat) : null; }
        };
        dpFechaDesde.setConverter(c); dpFechaHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }
    private void configurarFiltrosUI() {
        comboEstado.setItems(FXCollections.observableArrayList("Todos", "PENDIENTE", "PAGADA", "ANULADA"));
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.setItems(FXCollections.observableArrayList("Todas", "Crédito", "Contado"));
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.setItems(FXCollections.observableArrayList("Todos", "Efectivo", "Transferencia", "Cheque", "Tarjeta"));
        comboMedioPago.getSelectionModel().selectFirst();
    }
    private void mostrarAlerta(String titulo, String contenido) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Gestión de Compras");
        a.setHeaderText(titulo);
        a.setContentText(contenido);
        a.showAndWait();
    }
}