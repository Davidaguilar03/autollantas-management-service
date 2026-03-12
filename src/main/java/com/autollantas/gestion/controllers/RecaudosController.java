package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Venta;
import com.autollantas.gestion.service.VentasService;
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
public class RecaudosController {

    @Autowired private VentasService ventasService;
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

    @FXML private TableView<Venta> tablaFacturas;
    @FXML private TableColumn<Venta, String> colNumero;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, LocalDate> colFechaCreacion;
    @FXML private TableColumn<Venta, LocalDate> colFechaVencimiento;
    @FXML private TableColumn<Venta, String> colFormaPago;
    @FXML private TableColumn<Venta, String> colMetodoPago;
    @FXML private TableColumn<Venta, String> colCuenta;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, Double> colPorCobrar;
    @FXML private TableColumn<Venta, String> colEstado;

    @FXML private Button btnRegistrarPago;
    @FXML private Button btnVerDetalle;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Venta> masterData = FXCollections.observableArrayList();
    private FilteredList<Venta> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        this.filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Venta> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaFacturas.comparatorProperty());
        tablaFacturas.setItems(sortedData);

        configurarColumnas();
        configurarFormatosFecha();

        configurarFiltrosUI();
        resetearFiltros();

        configurarListeners();
        setupInteractionListeners();

        cargarDatosDB();
    }

    private void cargarDatosDB() {
        Platform.runLater(() -> {
            List<Venta> lista = ventasService.findAllVentas();
            masterData.setAll(lista);
            aplicarFiltros();
        });
    }

    private void configurarFiltrosUI() {
        comboEstado.getItems().clear();
        comboEstado.getItems().addAll("Todos", "PENDIENTE", "PAGADA", "ANULADA");

        comboFormaPago.getItems().clear();
        comboFormaPago.getItems().addAll("Todas", "Crédito", "Contado");

        comboMedioPago.getItems().clear();
        comboMedioPago.getItems().addAll("Todos", "Efectivo", "Transferencia", "Nequi/Daviplata", "Cheque");
    }

    private void resetearFiltros() {
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
        resetearFiltros();
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        if (filteredData == null) return;

        filteredData.setPredicate(venta -> {
            if (!matchTexto(txtNumero.getText(), venta.getNumeroFacturaVenta())) return false;

            String cliente = (venta.getCliente() != null) ? venta.getCliente().getNombreCliente() : "";
            if (!matchTexto(txtCliente.getText(), cliente)) return false;

            String totalFilter = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!totalFilter.isEmpty()) {
                String totalStr = String.valueOf(venta.getTotalVenta().longValue());
                if (!totalStr.startsWith(totalFilter)) return false;
            }

            if (!matchCombo(comboEstado, venta.getEstadoVenta())) return false;
            if (!matchCombo(comboFormaPago, venta.getFormaPagoVenta())) return false;
            if (!matchCombo(comboMedioPago, venta.getMedioPagoVenta())) return false;

            if (fueraDeRango(venta.getFechaVenta(), dpCreacionDesde.getValue(), dpCreacionHasta.getValue())) return false;
            if (fueraDeRango(venta.getFechaVencimientoVenta(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;

            return true;
        });

        actualizarLabelRegistros();
    }

    private boolean matchTexto(String filtro, String valor) {
        return filtro == null || filtro.isEmpty() || (valor != null && valor.toLowerCase().contains(filtro.toLowerCase()));
    }

    private boolean matchCombo(ComboBox<String> combo, String valorReal) {
        String seleccion = combo.getValue();
        if (seleccion == null || seleccion.isEmpty() || seleccion.startsWith("Tod")) return true;

        return valorReal != null && valorReal.equalsIgnoreCase(seleccion);
    }

    private boolean fueraDeRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return true;
        if (desde != null && fecha.isBefore(desde)) return true;
        if (hasta != null && fecha.isAfter(hasta)) return true;
        return false;
    }

    private void actualizarLabelRegistros() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configurarColumnas() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroFacturaVenta"));
        estilizarColumnaTexto(colNumero);

        colCliente.setCellValueFactory(cell -> {
            if (cell.getValue().getCliente() != null) return new SimpleStringProperty(cell.getValue().getCliente().getNombreCliente());
            return new SimpleStringProperty("N/A");
        });
        estilizarColumnaTexto(colCliente);

        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPagoVenta"));
        estilizarColumnaTexto(colFormaPago);

        colMetodoPago.setCellValueFactory(new PropertyValueFactory<>("medioPagoVenta"));
        estilizarColumnaTexto(colMetodoPago);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null) return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            return new SimpleStringProperty("-");
        });
        estilizarColumnaTexto(colCuenta);

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("fechaVenta"));
        colFechaCreacion.setCellFactory(col -> crearCeldaFecha());

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimientoVenta"));
        colFechaVencimiento.setCellFactory(col -> crearCeldaFecha());

        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));
        colTotal.setCellFactory(col -> crearCeldaMoneda(false));

        colPorCobrar.setCellValueFactory(cell -> {
            Venta v = cell.getValue();
            Double deuda;
            if ("ANULADA".equalsIgnoreCase(v.getEstadoVenta())) {
                deuda = 0.0;
            } else if (v.getSaldoPendiente() != null) {
                deuda = v.getSaldoPendiente();
            } else {
                deuda = "PAGADA".equalsIgnoreCase(v.getEstadoVenta()) ? 0.0 : v.getTotalVenta();
            }
            return new SimpleObjectProperty<>(deuda);
        });
        colPorCobrar.setCellFactory(col -> crearCeldaMoneda(true));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoVenta"));
        colEstado.setCellFactory(col -> crearCeldaEstado());
    }

    private void estilizarColumnaTexto(TableColumn<Venta, String> col) {
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

    private TableCell<Venta, LocalDate> crearCeldaFecha() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(fechaFormatter.format(item));
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<Venta, String> crearCeldaEstado() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
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

    private TableCell<Venta, Double> crearCeldaMoneda(boolean esDeuda) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor = Color.BLACK;
                    if (esDeuda) {
                        baseColor = (item > 0) ? Color.RED : COLOR_VERDE_OSCURO;
                    }
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private void configurarListeners() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> aplicarFiltros();

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
                double deuda = 0.0;

                if ("ANULADA".equalsIgnoreCase(seleccion.getEstadoVenta())) {
                    deuda = 0.0;
                } else if (seleccion.getSaldoPendiente() != null) {
                    deuda = seleccion.getSaldoPendiente();
                } else {
                    deuda = "PAGADA".equalsIgnoreCase(seleccion.getEstadoVenta()) ? 0.0 : seleccion.getTotalVenta();
                }

                puedePagar = (deuda > 0) && !"PAGADA".equalsIgnoreCase(seleccion.getEstadoVenta()) && !"ANULADA".equalsIgnoreCase(seleccion.getEstadoVenta());
            }
            btnRegistrarPago.setDisable(!puedePagar);
        });
    }

    private void setupInteractionListeners() {
        tablaFacturas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Venta seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    abrirHistorial(seleccionada);
                }
            }
        });

        tablaFacturas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Venta seleccionada = tablaFacturas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    abrirHistorial(seleccionada);
                    event.consume();
                }
            }
        });
    }

    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }

    @FXML
    void btnRegistrarPagoClick(ActionEvent event) {
        Venta seleccion = tablaFacturas.getSelectionModel().getSelectedItem();
        if (seleccion == null) return;

        if ("PAGADA".equalsIgnoreCase(seleccion.getEstadoVenta())) {
            mostrarAlerta("Información", "Esta factura ya está pagada.");
            return;
        }

        if ("ANULADA".equalsIgnoreCase(seleccion.getEstadoVenta())) {
            mostrarAlerta("Información", "No se puede registrar un pago para una factura anulada.");
            return;
        }

        abrirModalRecaudo(seleccion);
    }

    private void abrirModalRecaudo(Venta venta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/FormularioRecaudo.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            FormularioRecaudoController controller = loader.getController();
            controller.setVenta(venta);

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

            if (controller.isGuardado()) {
                cargarDatosDB();
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error Crítico", "No se pudo abrir el formulario de pago: " + e.getMessage());
        }
    }

    @FXML
    void btnVerDetalleClick(ActionEvent event) {
        Venta seleccion = tablaFacturas.getSelectionModel().getSelectedItem();
        if(seleccion != null) {
            abrirHistorial(seleccion);
        } else {
            mostrarAlerta("Atención", "Seleccione un registro para ver el historial.");
        }
    }

    private void abrirHistorial(Venta ventaSeleccionada) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/HistorialRecaudos.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            HistorialRecaudosController controller = loader.getController();
            controller.setVenta(ventaSeleccionada);

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
            mostrarAlerta("Error al abrir Historial", "Detalle técnico: " + e.getMessage());
        }
    }

    private void configurarFormatosFecha() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? fechaFormatter.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, fechaFormatter) : null; }
        };
        dpCreacionDesde.setConverter(c); dpCreacionHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestión de Recaudos");
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}