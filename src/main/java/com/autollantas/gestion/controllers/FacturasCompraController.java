package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.DetalleCompra;
import com.autollantas.gestion.model.Producto;
import com.autollantas.gestion.service.ComprasService;
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
public class FacturasCompraController {

    @Autowired private ComprasService comprasService;
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

    @FXML private TableView<Compra> tablaFacturasCompra;
    @FXML private TableColumn<Compra, String> colNumero;
    @FXML private TableColumn<Compra, String> colProveedor;
    @FXML private TableColumn<Compra, LocalDate> colFechaCreacion;
    @FXML private TableColumn<Compra, LocalDate> colFechaVencimiento;

    @FXML private TableColumn<Compra, String> colFormaPago;
    @FXML private TableColumn<Compra, String> colMetodoPago;
    @FXML private TableColumn<Compra, String> colCuenta;

    @FXML private TableColumn<Compra, Double> colTotal;
    @FXML private TableColumn<Compra, Double> colPagado;
    @FXML private TableColumn<Compra, Double> colPorPagar;
    @FXML private TableColumn<Compra, String> colEstado;

    @FXML private Button btnVerDetalles;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<Compra> masterData = FXCollections.observableArrayList();
    private FilteredList<Compra> filteredData;

    @SuppressWarnings("deprecation")
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");
    private final Color ESTADO_PAGADA = Color.web("#27ae60");
    private final Color ESTADO_PENDIENTE = Color.web("#f39c12");
    private final Color ESTADO_ANULADA = Color.web("#8b0000");

    @FXML
    public void initialize() {
        System.out.println("--- INICIANDO FACTURAS COMPRA CONTROLLER ---");

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Compra> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaFacturasCompra.comparatorProperty());
        tablaFacturasCompra.setItems(sortedData);

        configurarColumnas();
        configurarFiltrosUI();
        configurarFormatosFecha();
        configurarListeners();
        setupInteractionListeners();

        cargarDatosDB();
    }

    private void cargarDatosDB() {
        if (comprasService == null) return;
        Platform.runLater(() -> {
            try {
                List<Compra> lista = comprasService.findAllCompras();
                masterData.setAll(lista);
                actualizarLabelRegistros();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void actualizarLabelRegistros() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configurarColumnas() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroFacturaCompra"));
        estilizarColumnaTexto(colNumero, Pos.CENTER);

        colProveedor.setCellValueFactory(cell -> {
            if (cell.getValue().getProveedor() != null) {
                return new SimpleStringProperty(cell.getValue().getProveedor().getNombreProveedor());
            }
            return new SimpleStringProperty("Sin Proveedor");
        });
        estilizarColumnaTexto(colProveedor, Pos.CENTER);

        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colFechaCreacion.setCellFactory(col -> crearCeldaFecha());

        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimientoCompra"));
        colFechaVencimiento.setCellFactory(col -> crearCeldaFecha());

        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPagoCompra"));
        estilizarColumnaTexto(colFormaPago, Pos.CENTER);

        colMetodoPago.setCellValueFactory(new PropertyValueFactory<>("medioPagoCompra"));
        estilizarColumnaTexto(colMetodoPago, Pos.CENTER);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null) {
                return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            }
            return new SimpleStringProperty("-");
        });
        estilizarColumnaTexto(colCuenta, Pos.CENTER);

        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCompra"));
        colTotal.setCellFactory(col -> crearCeldaMoneda(false));

        colPagado.setCellValueFactory(cell -> {
            Compra c = cell.getValue();
            double pagado = "PAGADA".equalsIgnoreCase(c.getEstadoCompra()) ? c.getTotalCompra() : 0.0;
            return new SimpleObjectProperty<>(pagado);
        });
        colPagado.setCellFactory(col -> crearCeldaMoneda(false));

        colPorPagar.setCellValueFactory(cell -> {
            Compra c = cell.getValue();
            double pendiente = "PENDIENTE".equalsIgnoreCase(c.getEstadoCompra()) ? c.getTotalCompra() : 0.0;
            return new SimpleObjectProperty<>(pendiente);
        });
        colPorPagar.setCellFactory(col -> crearCeldaMoneda(true));

        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoCompra"));
        colEstado.setCellFactory(col -> crearCeldaEstado());
    }

    @FXML
    void btnEliminarClick(ActionEvent event) {
        Compra sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if(sel != null) {

            if ("ANULADA".equalsIgnoreCase(sel.getEstadoCompra())) {
                mostrarAlerta("Acción no permitida", "Esta factura ya se encuentra anulada.");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Anular Factura");
            alert.setHeaderText("Va a anular la factura " + sel.getNumeroFacturaCompra());
            alert.setContentText("Esta acción revertirá los productos del inventario y cambiará su estado a ANULADA.");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    comprasService.anularCompra(sel);

                    tablaFacturasCompra.refresh();
                    aplicarFiltros();

                    mostrarAlerta("Éxito", "Compra anulada y stock revertido correctamente.");

                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarAlerta("Error", "No se pudo anular: " + e.getMessage());
                }
            }
        }
    }

    private void estilizarColumnaTexto(TableColumn<Compra, String> col, Pos alineacion) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(alineacion);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(alineacion);
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
                    Label lbl = new Label(fechaFormatter.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<Compra, Double> crearCeldaMoneda(boolean esDeuda) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setStyle("-fx-font-weight: bold;");
                    Color baseColor = Color.BLACK;
                    if (esDeuda) baseColor = (item > 0) ? Color.RED : COLOR_VERDE_OSCURO;
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
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

    @FXML
    void btnNuevaFacturaClick(ActionEvent event) {
        abrirModal();
    }

    public void abrirModal(){
        MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/FormularioCompra.fxml");
    }


    @FXML
    void btnEditarClick(ActionEvent event) {
        Compra sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if (sel != null) {
            if ("ANULADA".equalsIgnoreCase(sel.getEstadoCompra())) {
                mostrarAlerta("Acción no permitida", "No se puede editar una factura anulada.");
                return;
            }
            Object controllerObj = MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/FormularioCompra.fxml");
            if (controllerObj instanceof FormularioCompraController) {
                ((FormularioCompraController) controllerObj).setCompraParaEdicion(sel);
            }
        } else {
            mostrarAlerta("Selección requerida", "Seleccione una compra para editar.");
        }
    }

    @FXML
    void btnVerDetallesClick(ActionEvent event) {
        Compra sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
        if (sel != null) {
            abrirModalDetalles(sel);
        }
    }

    private void abrirModalDetalles(Compra compra) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/VerDetallesCompra.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            VerDetallesCompraController controller = loader.getController();
            controller.setCompra(compra);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaFacturasCompra.getScene().getWindow();
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

            modalStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el detalle: " + e.getMessage());
        }
    }

    private void configurarListeners() {
        InvalidationListener listener = obs -> aplicarFiltros();
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

        tablaFacturasCompra.getSelectionModel().selectedItemProperty().addListener((obs, old, nueva) -> {
            boolean haySeleccion = (nueva != null);
            btnVerDetalles.setDisable(!haySeleccion);
            btnEditar.setDisable(!haySeleccion);
            btnEliminar.setDisable(!haySeleccion);
        });
    }

    private void setupInteractionListeners() {
        tablaFacturasCompra.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Compra sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    abrirModalDetalles(sel);
                    event.consume();
                }
            }
        });
        tablaFacturasCompra.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Compra sel = tablaFacturasCompra.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    abrirModalDetalles(sel);
                }
            }
        });
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(c -> {
            String filtroEstado = comboEstado.getValue();
            String estadoReal = c.getEstadoCompra();

            if ("PENDIENTE".equals(filtroEstado)) {
                if (!"PENDIENTE".equalsIgnoreCase(estadoReal)) return false;
            } else if ("PAGADA".equals(filtroEstado)) {
                if (!"PAGADA".equalsIgnoreCase(estadoReal)) return false;
            } else if ("ANULADA".equals(filtroEstado)) {
                if (!"ANULADA".equalsIgnoreCase(estadoReal)) return false;
            } else if ("Todas".equals(filtroEstado) || filtroEstado == null) {
            }

            if (!matchTexto(txtNumero.getText(), c.getNumeroFacturaCompra())) return false;
            String nombreProv = (c.getProveedor() != null) ? c.getProveedor().getNombreProveedor() : "";
            if (!matchTexto(txtProveedor.getText(), nombreProv)) return false;

            String ft = txtTotal.getText().replaceAll("[^0-9]", "");
            if (!ft.isEmpty()) {
                String totalStr = String.valueOf(c.getTotalCompra().longValue());
                if (!totalStr.startsWith(ft)) return false;
            }

            if (!matchCombo(comboFormaPago, c.getFormaPagoCompra())) return false;
            if (!matchCombo(comboMedioPago, c.getMedioPagoCompra())) return false;

            if (fueraDeRango(c.getFechaCompra(), dpCreacionDesde.getValue(), dpCreacionHasta.getValue())) return false;
            if (fueraDeRango(c.getFechaVencimientoCompra(), dpVencimientoDesde.getValue(), dpVencimientoHasta.getValue())) return false;

            return true;
        });
        actualizarLabelRegistros();
    }

    private boolean matchTexto(String filtro, String valor) {
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

    private void configurarFiltrosUI() {
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

    private void configurarFormatosFecha() {
        StringConverter<LocalDate> c = new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? fechaFormatter.format(d) : ""; }
            @Override public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, fechaFormatter) : null; }
        };
        dpCreacionDesde.setConverter(c); dpCreacionHasta.setConverter(c);
        dpVencimientoDesde.setConverter(c); dpVencimientoHasta.setConverter(c);
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gestión de Compras");
            alert.setHeaderText(titulo);
            alert.setContentText(contenido);
            alert.showAndWait();
        });
    }

    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtNumero.clear(); txtProveedor.clear(); txtTotal.clear();
        comboEstado.getSelectionModel().selectFirst();
        comboFormaPago.getSelectionModel().selectFirst();
        comboMedioPago.getSelectionModel().selectFirst();
        dpCreacionDesde.setValue(null); dpCreacionHasta.setValue(null);
        dpVencimientoDesde.setValue(null); dpVencimientoHasta.setValue(null);
        aplicarFiltros();
    }
}