package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.*;
import com.autollantas.gestion.repository.*;
import javafx.animation.TranslateTransition;
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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
@Component
public class PanelControlController {

    @Autowired private VentaRepository ventaRepo;
    @Autowired private CompraRepository compraRepo;
    @Autowired private GastoOperativoRepository gastoRepo;
    @Autowired private IngresoOcasionalRepository ingresoOcasionalRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private CuentaRepository cuentaRepo;

    @Autowired private ApplicationContext springContext;

    @FXML private ComboBox<String> comboPeriodo;
    @FXML private HBox boxFechasPersonalizadas;
    @FXML private DatePicker dateDesde;
    @FXML private DatePicker dateHasta;

    @FXML private Label lblIngresos;
    @FXML private Label lblGastos;
    @FXML private Label lblCostos;
    @FXML private Label lblNeto;

    @FXML private Label lblPorCobrar;
    @FXML private Label lblPorPagar;
    @FXML private Label lblSaldo;
    @FXML private Label lblAlertas;
    @FXML private Label lblInfoRegistros;

    @FXML private TableView<Movimiento> tablaMovimientos;
    @FXML private TableColumn<Movimiento, LocalDate> colFecha;
    @FXML private TableColumn<Movimiento, String> colTipo;
    @FXML private TableColumn<Movimiento, String> colConcepto;
    @FXML private TableColumn<Movimiento, String> colCuenta;
    @FXML private TableColumn<Movimiento, Double> colMonto;
    @FXML private Button btnCrearReporte;
    @FXML private ImageView imgReporteAnimada;

    private final ObservableList<Movimiento> masterData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");

    @FXML
    public void initialize() {

        FilteredList<Movimiento> filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Movimiento> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaMovimientos.comparatorProperty());
        tablaMovimientos.setItems(sortedData);

        configurarColumnas();
        configurarFiltrosUI();

        comboPeriodo.getSelectionModel().select("Este Mes");
        cargarKPIsGlobales();
        tablaMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurarAnimacionBotonReporte();
    }

    private void configurarAnimacionBotonReporte() {
        if (btnCrearReporte == null || imgReporteAnimada == null) return;

        TranslateTransition subir = new TranslateTransition(Duration.millis(200), imgReporteAnimada);
        subir.setToY(-6);

        TranslateTransition bajar = new TranslateTransition(Duration.millis(200), imgReporteAnimada);
        bajar.setToY(0);

        btnCrearReporte.setOnMouseEntered(event -> {
            bajar.stop();
            subir.playFromStart();
            imgReporteAnimada.setOpacity(1.0);
        });

        btnCrearReporte.setOnMouseExited(event -> {
            subir.stop();
            bajar.playFromStart();
            imgReporteAnimada.setOpacity(0.7);
        });
    }

    private void cargarMovimientosReales() {
        masterData.clear();
        LocalDate inicio = dateDesde.getValue();
        LocalDate fin = dateHasta.getValue();

        List<Movimiento> listaUnificada = new ArrayList<>();

        List<Venta> ventas = ventaRepo.findByFechaVentaBetween(inicio, fin);
        for (Venta v : ventas) {
            Movimiento mov = new Movimiento(
                    v.getFechaVenta(),
                    v.getIdVenta(),
                    "Venta",
                    v.getTotalVenta(),
                    v.getCuenta()
            );
            mov.setTablaOrigenMovimiento("VENTA");
            listaUnificada.add(mov);
        }

        List<Compra> compras = compraRepo.findByFechaCompraBetween(inicio, fin);
        for (Compra c : compras) {
            Movimiento mov = new Movimiento(
                    c.getFechaCompra(),
                    c.getIdCompra(),
                    "Costo",
                    c.getTotalCompra(),
                    c.getCuenta()
            );
            mov.setTablaOrigenMovimiento("COMPRA");
            listaUnificada.add(mov);
        }

        List<GastoOperativo> gastos = gastoRepo.findByFechaGastoBetween(inicio, fin);
        for (GastoOperativo g : gastos) {
            Movimiento mov = new Movimiento(
                    g.getFechaGasto(),
                    g.getIdGasto(),
                    "Gasto",
                    g.getMontoGasto(),
                    g.getCuenta()
            );
            mov.setTablaOrigenMovimiento("GASTO: " + g.getConceptoGasto());
            listaUnificada.add(mov);
        }

        List<IngresoOcasional> ingresos = ingresoOcasionalRepo.findByFechaIngresoBetween(inicio, fin);
        for (IngresoOcasional i : ingresos) {
            Movimiento mov = new Movimiento(
                    i.getFechaIngreso(),
                    i.getIdIngreso(),
                    "Ingreso",
                    i.getMontoIngreso(),
                    i.getCuenta()
            );
            mov.setTablaOrigenMovimiento("OTRO: " + i.getConceptoIngreso());
            listaUnificada.add(mov);
        }

        listaUnificada.sort((m1, m2) -> m2.getFechaMovimiento().compareTo(m1.getFechaMovimiento()));
        masterData.setAll(listaUnificada);

        recalcularTotales();
        actualizarInfoRegistros();
    }

    private void cargarKPIsGlobales() {
        Platform.runLater(() -> {
            double totalPorCobrar = ventaRepo.findAll().stream()
                    .filter(v -> "PENDIENTE".equalsIgnoreCase(v.getEstadoVenta()))
                    .mapToDouble(Venta::getTotalVenta)
                    .sum();

            double totalPorPagar = compraRepo.findAll().stream()
                    .filter(c -> "PENDIENTE".equalsIgnoreCase(c.getEstadoCompra()))
                    .mapToDouble(Compra::getTotalCompra)
                    .sum();

            double saldoTotal = cuentaRepo.findAll().stream()
                    .mapToDouble(c -> c.getSaldoActual() != null ? c.getSaldoActual() : 0.0)
                    .sum();

            long numAlertas = productoRepo.findAll().stream()
                    .filter(p -> p.getCategoria() != null &&
                            p.getCantidad() <= p.getCategoria().getStockMinAmarillo())
                    .count();

            actualizarLabelInteligente(lblPorCobrar, totalPorCobrar);
            actualizarLabelInteligente(lblPorPagar, totalPorPagar);
            actualizarLabelInteligente(lblSaldo, saldoTotal);
            if (lblAlertas != null) lblAlertas.setText(numAlertas + " Productos");
        });
    }

    private void recalcularTotales() {
        double tIngresos = 0;
        double tGastos = 0;
        double tCostos = 0;

        for (Movimiento mov : masterData) {
            String tipo = mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().toLowerCase() : "";
            double monto = mov.getMontoMovimiento() != null ? mov.getMontoMovimiento() : 0.0;

            if (tipo.contains("venta") || tipo.contains("ingreso")) {
                tIngresos += monto;
            } else if (tipo.contains("gasto")) {
                tGastos += monto;
            } else if (tipo.contains("costo") || tipo.contains("compra")) {
                tCostos += monto;
            }
        }

        actualizarLabelInteligente(lblIngresos, tIngresos);
        actualizarLabelInteligente(lblGastos, tGastos);
        actualizarLabelInteligente(lblCostos, tCostos);
        actualizarLabelInteligente(lblNeto, tIngresos - (tGastos + tCostos));
    }

    private void configurarFiltrosUI() {
        comboPeriodo.getItems().addAll("Hoy", "Ayer", "Esta Semana", "Este Mes", "Mes Anterior", "Este Año", "Personalizado");
        dateDesde.valueProperty().addListener((obs, oldVal, newVal) -> { if(newVal != null && dateHasta.getValue() != null) cargarMovimientosReales(); });
        dateHasta.valueProperty().addListener((obs, oldVal, newVal) -> { if(newVal != null && dateDesde.getValue() != null) cargarMovimientosReales(); });
        comboPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> manejarCambioPeriodo(newVal));
    }

    private void manejarCambioPeriodo(String seleccion) {
        boolean esPersonalizado = "Personalizado".equals(seleccion);
        boxFechasPersonalizadas.setVisible(esPersonalizado);
        boxFechasPersonalizadas.setManaged(esPersonalizado);

        if (!esPersonalizado) {
            calcularFechasAutomaticas(seleccion);
            cargarMovimientosReales();
        }
    }

    private void calcularFechasAutomaticas(String periodo) {
        if(periodo == null) return;
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy;
        LocalDate fin = hoy;

        switch (periodo) {
            case "Ayer": inicio = hoy.minusDays(1); fin = hoy.minusDays(1); break;
            case "Esta Semana":
                inicio = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                fin = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                break;
            case "Este Mes":
                inicio = hoy.with(TemporalAdjusters.firstDayOfMonth());
                fin = hoy.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Mes Anterior":
                inicio = hoy.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                fin = hoy.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Este Año":
                inicio = hoy.with(TemporalAdjusters.firstDayOfYear());
                fin = hoy.with(TemporalAdjusters.lastDayOfYear());
                break;
        }
        dateDesde.setValue(inicio);
        dateHasta.setValue(fin);
    }

    private void actualizarInfoRegistros() {
        if (lblInfoRegistros != null) lblInfoRegistros.setText("Mostrando " + masterData.size() + " movimientos");
    }

    private void actualizarLabelInteligente(Label label, double valor) {
        if (label == null) return;
        String texto = currencyFormat.format(valor);
        label.setText(texto);
        if (texto.length() > 16) label.setFont(new Font("System Bold", 14));
        else if (texto.length() > 12) label.setFont(new Font("System Bold", 18));
        else label.setFont(new Font("System Bold", 22));
    }

    private void configurarColumnas() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaMovimiento"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(fechaFormatter.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#555555")));
                    setGraphic(lbl);
                }
            }
        });

        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                    Color baseColor = Color.GRAY;
                    if (item.equalsIgnoreCase("Ingreso") || item.equalsIgnoreCase("Venta")) baseColor = COLOR_VERDE_OSCURO;
                    else if (item.equalsIgnoreCase("Gasto")) baseColor = Color.web("#c0392b");
                    else if (item.equalsIgnoreCase("Costo")) baseColor = Color.web("#f39c12");
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(baseColor));
                    setGraphic(lbl);
                }
            }
        });

        colConcepto.setCellValueFactory(cellData -> {
            Movimiento m = cellData.getValue();
            String desc = m.getTablaOrigenMovimiento() != null ? m.getTablaOrigenMovimiento() : "MOV";
            if (m.getIdOrigenMovimiento() != null) {
                desc += " #" + m.getIdOrigenMovimiento();
            }
            return new SimpleStringProperty(desc);
        });
        colConcepto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(Pos.CENTER_LEFT);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                }
            }
        });

        colCuenta.setCellValueFactory(cellData -> {
            Movimiento m = cellData.getValue();
            if (m.getCuenta() != null) {
                return new SimpleStringProperty(m.getCuenta().getNombreCuenta());
            }
            return new SimpleStringProperty("Caja General");
        });
        colCuenta.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(Pos.CENTER_LEFT);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                }
            }
        });

        colMonto.setCellValueFactory(new PropertyValueFactory<>("montoMovimiento"));
        colMonto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER_RIGHT);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setStyle("-fx-font-weight: bold;");
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#333333")));
                    setGraphic(lbl);
                }
            }
        });
    }

    @FXML
    void actionNuevaVenta(ActionEvent event) {
        Object controller = MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/FacturasVenta.fxml");
        if (controller instanceof FacturasVentaController c) {
            c.abrirModal();
        }
    }

    @FXML
    void actionNuevaCompra(ActionEvent event) {
        MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/FormularioCompra.fxml");
    }

    @FXML
    void actionNuevoGasto(ActionEvent event) {
        Object controller = MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/GastosOperativos.fxml");
        if (controller instanceof GastosOperativosController c) {
            c.abrirFormulario(new GastoOperativo());
        }
    }

    @FXML
    void actionNuevoIngreso(ActionEvent event) {
        Object controller = MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/IngresoOcasional.fxml");
        if (controller instanceof IngresoOcasionalController c) {
            c.abrirFormulario(new IngresoOcasional());
        }
    }

    @FXML
    void verAlertasStock(javafx.scene.input.MouseEvent event) {
        MainLayoutController.getInstance().cargarVista("/com/autollantas/gestion/views/AlertasStock.fxml");
    }

    @FXML
    void onPeriodoChanged(ActionEvent event) {
    }

    @FXML
    void abrirDialogoReportes(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/GeneracionReportes.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaMovimientos.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());

            modalStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo cargar el módulo de reportes");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }
}