package com.autollantas.gestion.reporting.controller;

import com.autollantas.gestion.reporting.model.MovementDto;
import com.autollantas.gestion.reporting.service.DashboardService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.sales.controller.SaleInvoicesController;
import com.autollantas.gestion.purchases.controller.PurchaseInvoicesController;
import com.autollantas.gestion.treasury.controller.OperationalExpensesController;
import com.autollantas.gestion.treasury.controller.OccasionalIncomeController;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import java.util.List;
import java.util.Locale;

@Component
public class DashboardController {

    @Autowired private DashboardService dashboardService;
    @Autowired private ApplicationContext springContext;

    @FXML private ComboBox<String> comboPeriodo;
    @FXML private HBox boxFechasPersonalizadas;
    @FXML private DatePicker dateDesde;
    @FXML private DatePicker dateHasta;
    @FXML private Button btnAplicarFechas;

    @FXML private Label lblIngresos;
    @FXML private Label lblGastos;
    @FXML private Label lblCostos;
    @FXML private Label lblNeto;

    @FXML private Label lblPorCobrar;
    @FXML private Label lblPorPagar;
    @FXML private Label lblSaldo;
    @FXML private Label lblAlertas;
    @FXML private Label lblInfoRegistros;

    @FXML private TableView<MovementDto> tablaMovimientos;
    @FXML private TableColumn<MovementDto, LocalDate> colFecha;
    @FXML private TableColumn<MovementDto, String> colTipo;
    @FXML private TableColumn<MovementDto, String> colConcepto;
    @FXML private TableColumn<MovementDto, String> colCuenta;
    @FXML private TableColumn<MovementDto, Double> colMonto;
    @FXML private Button btnCrearReporte;
    @FXML private ImageView imgReporteAnimada;

    private final ObservableList<MovementDto> masterData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color COLOR_VENTA   = Color.web("#3d8b5e");
    private static final Color COLOR_GASTO   = Color.web("#b85c5c");
    private static final Color COLOR_COSTO   = Color.web("#9a7630");
    private static final Color COLOR_DEFECTO = Color.web("#7a8899");

    private boolean loadingMovements = false;
    private boolean loadingKpis = false;
    private boolean settingDates = false;

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);

        FilteredList<MovementDto> filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<MovementDto> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaMovimientos.comparatorProperty());
        tablaMovimientos.setItems(sortedData);
        tablaMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configureColumns();
        configureRowClick();
        setupFilterUI();

        comboPeriodo.getSelectionModel().select("Este Mes");
        loadGlobalKPIs();
        setupReportButtonAnimation();
    }

    public void refresh() {
        loadGlobalKPIs();
        loadData();
    }

    private void setupReportButtonAnimation() {
        if (btnCrearReporte == null || imgReporteAnimada == null) return;

        TranslateTransition up   = new TranslateTransition(Duration.millis(200), imgReporteAnimada);
        up.setToY(-6);
        TranslateTransition down = new TranslateTransition(Duration.millis(200), imgReporteAnimada);
        down.setToY(0);

        btnCrearReporte.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); imgReporteAnimada.setOpacity(1.0); });
        btnCrearReporte.setOnMouseExited(e ->  { up.stop(); down.playFromStart(); imgReporteAnimada.setOpacity(0.7); });
    }

    /** Carga movimientos Y KPIs de periodo en paralelo. */
    private void loadData() {
        loadMovements();
        loadPeriodKpis();
    }

    private void loadMovements() {
        if (loadingMovements) return;
        LocalDate start = dateDesde.getValue();
        LocalDate end   = dateHasta.getValue();
        if (start == null || end == null) return;

        loadingMovements = true;
        if (lblInfoRegistros != null) lblInfoRegistros.setText("Cargando...");
        setKpiLoadingState(true);

        Task<List<MovementDto>> task = new Task<>() {
            @Override protected List<MovementDto> call() {
                return dashboardService.getMovements(start, end);
            }
        };
        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            updateRecordsInfo();
            loadingMovements = false;
        });
        task.setOnFailed(e -> {
            loadingMovements = false;
            if (lblInfoRegistros != null) lblInfoRegistros.setText("Error al cargar movimientos");
            showError("No se pudieron cargar los movimientos", task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadPeriodKpis() {
        LocalDate start = dateDesde.getValue();
        LocalDate end   = dateHasta.getValue();
        if (start == null || end == null) return;

        setKpiLoadingState(true);

        Task<DashboardService.PeriodKpis> task = new Task<>() {
            @Override protected DashboardService.PeriodKpis call() {
                return dashboardService.getPeriodKpis(start, end);
            }
        };
        task.setOnSucceeded(e -> {
            DashboardService.PeriodKpis kpis = task.getValue();
            updateSmartLabel(lblIngresos, kpis.getTotalIncome(), false);
            updateSmartLabel(lblGastos,   kpis.getTotalExpenses(), false);
            updateSmartLabel(lblCostos,   kpis.getTotalCosts(), false);
            updateSmartLabel(lblNeto,     kpis.getNetResult(), true);
            setKpiLoadingState(false);
        });
        task.setOnFailed(e -> {
            setKpiLoadingState(false);
            showError("No se pudieron cargar los indicadores del periodo", task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadGlobalKPIs() {
        if (loadingKpis) return;
        loadingKpis = true;

        Task<DashboardService.DashboardKpis> task = new Task<>() {
            @Override protected DashboardService.DashboardKpis call() {
                return dashboardService.getGlobalKpis();
            }
        };
        task.setOnSucceeded(e -> {
            DashboardService.DashboardKpis kpis = task.getValue();
            updateSmartLabel(lblPorCobrar, kpis.getTotalReceivable(), false);
            updateSmartLabel(lblPorPagar,  kpis.getTotalPayable(), false);
            updateSmartLabel(lblSaldo,     kpis.getTotalBalance(), false);
            updateAlertLabel(kpis.getAlertCount());
            loadingKpis = false;
        });
        task.setOnFailed(e -> {
            loadingKpis = false;
            showError("No se pudieron cargar los indicadores globales", task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Muestra "—" en los KPI de periodo mientras cargan, para no confundir con cero real. */
    private void setKpiLoadingState(boolean loading) {
        String placeholder = "—";
        if (loading) {
            if (lblIngresos != null) { lblIngresos.setText(placeholder); lblIngresos.setStyle(lblIngresos.getStyle()); }
            if (lblGastos   != null)   lblGastos.setText(placeholder);
            if (lblCostos   != null)   lblCostos.setText(placeholder);
            if (lblNeto     != null)   lblNeto.setText(placeholder);
        }
    }

    private void setupFilterUI() {
        comboPeriodo.getItems().addAll("Hoy", "Ayer", "Esta Semana", "Este Mes", "Mes Anterior", "Este Año", "Personalizado");

        comboPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> handlePeriodChange(newVal));

        // En modo Personalizado la carga se dispara solo con el botón Aplicar
        if (btnAplicarFechas != null) {
            btnAplicarFechas.setOnAction(e -> {
                LocalDate desde = dateDesde.getValue();
                LocalDate hasta = dateHasta.getValue();
                if (desde == null || hasta == null) {
                    ToastNotification.warning(btnAplicarFechas, "Selecciona ambas fechas antes de filtrar");
                    return;
                }
                if (desde.isAfter(hasta)) {
                    ToastNotification.warning(btnAplicarFechas, "La fecha de inicio no puede ser posterior a la fecha final");
                    return;
                }
                loadData();
            });
        }
    }

    private void handlePeriodChange(String selection) {
        boolean isCustom = "Personalizado".equals(selection);
        boxFechasPersonalizadas.setVisible(isCustom);
        boxFechasPersonalizadas.setManaged(isCustom);

        if (!isCustom) {
            settingDates = true;
            calculateAutomaticDates(selection);
            settingDates = false;
            loadData();
        }
    }

    private void calculateAutomaticDates(String period) {
        if (period == null) return;
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        LocalDate end   = today;

        switch (period) {
            case "Ayer":
                start = today.minusDays(1); end = today.minusDays(1); break;
            case "Esta Semana":
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                break;
            case "Este Mes":
                start = today.with(TemporalAdjusters.firstDayOfMonth());
                end   = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Mes Anterior":
                start = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                end   = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Este Año":
                start = today.with(TemporalAdjusters.firstDayOfYear());
                end   = today.with(TemporalAdjusters.lastDayOfYear());
                break;
        }
        dateDesde.setValue(start);
        dateHasta.setValue(end);
    }

    private void updateRecordsInfo() {
        if (lblInfoRegistros == null) return;
        int total = masterData.size();
        if (total >= DashboardService.MAX_MOVEMENTS) {
            lblInfoRegistros.setText("Mostrando los " + DashboardService.MAX_MOVEMENTS + " más recientes");
        } else {
            lblInfoRegistros.setText("Mostrando " + total + " movimientos");
        }
    }

    private void updateAlertLabel(long count) {
        if (lblAlertas == null) return;
        if (count == 0) {
            lblAlertas.setText("Sin alertas");
            lblAlertas.setStyle("-fx-text-fill: #4a9660; -fx-font-size: 18px; -fx-font-weight: bold;");
        } else {
            lblAlertas.setText(count + " en stock bajo");
            lblAlertas.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 18px; -fx-font-weight: bold;");
        }
    }

    /**
     * Actualiza un label con el valor formateado.
     * Si applySign=true y el valor es negativo, pinta el texto en rojo; positivo en verde.
     */
    private void updateSmartLabel(Label label, double value, boolean applySign) {
        if (label == null) return;
        String text = currencyFormat.format(value);
        label.setText(text);

        if (applySign) {
            if (value < 0) {
                label.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            } else {
                label.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        }

        // Ajuste de tamaño de fuente para valores grandes
        if (text.length() > 16) label.setFont(new Font("System Bold", 13));
        else if (text.length() > 12) label.setFont(new Font("System Bold", 17));
        else label.setFont(new Font("System Bold", 22));
    }

    /** Al hacer clic en un movimiento, navega a su módulo correspondiente. */
    private void configureRowClick() {
        tablaMovimientos.setRowFactory(tv -> {
            TableRow<MovementDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    MovementDto mov = row.getItem();
                    navigateToMovement(mov);
                }
            });
            return row;
        });
    }

    private void navigateToMovement(MovementDto mov) {
        if (mov == null || mov.getSourceTable() == null) return;
        MainLayoutController ml = MainLayoutController.getInstance();
        switch (mov.getSourceTable()) {
            case "VENTA"   -> ml.navigateTo("/com/autollantas/gestion/sales/views/SaleInvoices.fxml",
                                 ml.getTpIngresos(), ml.getBtnVentas());
            case "COMPRA"  -> ml.navigateTo("/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml",
                                 ml.getTpEgresos(), ml.getBtnCompras());
            case "GASTO"   -> ml.navigateTo("/com/autollantas/gestion/treasury/views/OperationalExpenses.fxml",
                                 ml.getTpEgresos(), ml.getBtnCostosOperativos());
            case "INGRESO" -> ml.navigateTo("/com/autollantas/gestion/treasury/views/OccasionalIncome.fxml",
                                 ml.getTpIngresos(), ml.getBtnIngresoOcasional());
        }
    }

    private void showError(String message, Throwable cause) {
        Platform.runLater(() ->
            ToastNotification.error(tablaMovimientos, message)
        );
    }

    private void configureColumns() {
        colFecha.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDate()));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(dateFormatter.format(item));
                lbl.setAlignment(Pos.CENTER);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#555555")));
                setGraphic(lbl);
            }
        });

        colTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item.toUpperCase());
                lbl.setAlignment(Pos.CENTER);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                Color base = COLOR_DEFECTO;
                if (item.equalsIgnoreCase("Ingreso") || item.equalsIgnoreCase("Venta")) base = COLOR_VENTA;
                else if (item.equalsIgnoreCase("Gasto"))  base = COLOR_GASTO;
                else if (item.equalsIgnoreCase("Costo"))  base = COLOR_COSTO;
                lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(base));
                setGraphic(lbl);
            }
        });

        colConcepto.setCellValueFactory(cellData -> {
            MovementDto m = cellData.getValue();
            String text = m.getConcept() != null ? m.getConcept()
                        : (m.getSourceTable() != null ? m.getSourceTable() : "");
            return new SimpleStringProperty(text);
        });
        colConcepto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setAlignment(Pos.CENTER_LEFT);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                setGraphic(lbl);
            }
        });

        colCuenta.setCellValueFactory(cellData -> {
            MovementDto m = cellData.getValue();
            String name = m.getAccount() != null ? m.getAccount().getName() : "Caja General";
            return new SimpleStringProperty(name);
        });
        colCuenta.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setAlignment(Pos.CENTER_LEFT);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                setGraphic(lbl);
            }
        });

        colMonto.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colMonto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(currencyFormat.format(item));
                lbl.setAlignment(Pos.CENTER_RIGHT);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setStyle("-fx-font-weight: bold;");
                lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#333333")));
                setGraphic(lbl);
            }
        });
    }

    // ── Acciones rápidas ─────────────────────────────────────────────────────

    @FXML
    void actionNuevaVenta(ActionEvent event) {
        MainLayoutController ml = MainLayoutController.getInstance();
        Object controller = ml.navigateTo("/com/autollantas/gestion/sales/views/SaleInvoices.fxml",
                ml.getTpIngresos(), ml.getBtnVentas());
        if (controller instanceof SaleInvoicesController c) c.openForm();
    }

    @FXML
    void actionNuevaCompra(ActionEvent event) {
        MainLayoutController ml = MainLayoutController.getInstance();
        Object controller = ml.navigateTo("/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml",
                ml.getTpEgresos(), ml.getBtnCompras());
        if (controller instanceof PurchaseInvoicesController c) c.openForm();
    }

    @FXML
    void actionNuevoGasto(ActionEvent event) {
        MainLayoutController ml = MainLayoutController.getInstance();
        Object controller = ml.navigateTo("/com/autollantas/gestion/treasury/views/OperationalExpenses.fxml",
                ml.getTpEgresos(), ml.getBtnCostosOperativos());
        if (controller instanceof OperationalExpensesController c) c.openForm(new OperationalExpense());
    }

    @FXML
    void actionNuevoIngreso(ActionEvent event) {
        MainLayoutController ml = MainLayoutController.getInstance();
        Object controller = ml.navigateTo("/com/autollantas/gestion/treasury/views/OccasionalIncome.fxml",
                ml.getTpIngresos(), ml.getBtnIngresoOcasional());
        if (controller instanceof OccasionalIncomeController c) c.openForm(new OccasionalIncome());
    }

    @FXML
    void verAlertasStock(javafx.scene.input.MouseEvent event) {
        MainLayoutController ml = MainLayoutController.getInstance();
        ml.navigateTo("/com/autollantas/gestion/inventory/views/StockAlerts.fxml",
                ml.getTpInventario(), ml.getBtnAlertas());
    }

    @FXML
    void abrirDialogoReportes(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/autollantas/gestion/reporting/views/ReportGeneration.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage mainStage = (Stage) tablaMovimientos.getScene().getWindow();
            modalStage.initOwner(mainStage);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);
            modalStage.setX(mainStage.getX());
            modalStage.setY(mainStage.getY());
            modalStage.setWidth(mainStage.getWidth());
            modalStage.setHeight(mainStage.getHeight());

            modalStage.showAndWait();
            loadGlobalKPIs();

        } catch (IOException e) {
            e.printStackTrace();
            ToastNotification.error(tablaMovimientos, "No se pudo abrir el módulo de reportes");
        }
    }
}
