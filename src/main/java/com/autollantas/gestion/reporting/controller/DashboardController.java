package com.autollantas.gestion.reporting.controller;

import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.reporting.service.DashboardService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.sales.controller.SaleInvoicesController;
import com.autollantas.gestion.treasury.controller.OperationalExpensesController;
import com.autollantas.gestion.treasury.controller.OccasionalIncomeController;
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
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
@Component
public class DashboardController {

    @Autowired private DashboardService dashboardService;
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

    @FXML private TableView<Movement> tablaMovimientos;
    @FXML private TableColumn<Movement, LocalDate> colFecha;
    @FXML private TableColumn<Movement, String> colTipo;
    @FXML private TableColumn<Movement, String> colConcepto;
    @FXML private TableColumn<Movement, String> colCuenta;
    @FXML private TableColumn<Movement, Double> colMonto;
    @FXML private Button btnCrearReporte;
    @FXML private ImageView imgReporteAnimada;

    private final ObservableList<Movement> masterData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Color COLOR_VERDE_OSCURO = Color.web("#13522d");

    @FXML
    public void initialize() {
        FilteredList<Movement> filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Movement> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaMovimientos.comparatorProperty());
        tablaMovimientos.setItems(sortedData);

        configureColumns();
        setupFilterUI();

        comboPeriodo.getSelectionModel().select("Este Mes");
        loadGlobalKPIs();
        tablaMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupReportButtonAnimation();
    }

    private void setupReportButtonAnimation() {
        if (btnCrearReporte == null || imgReporteAnimada == null) return;

        TranslateTransition up = new TranslateTransition(Duration.millis(200), imgReporteAnimada);
        up.setToY(-6);

        TranslateTransition down = new TranslateTransition(Duration.millis(200), imgReporteAnimada);
        down.setToY(0);

        btnCrearReporte.setOnMouseEntered(event -> {
            down.stop(); up.playFromStart();
            imgReporteAnimada.setOpacity(1.0);
        });
        btnCrearReporte.setOnMouseExited(event -> {
            up.stop(); down.playFromStart();
            imgReporteAnimada.setOpacity(0.7);
        });
    }

    private void loadMovements() {
        masterData.clear();
        LocalDate start = dateDesde.getValue();
        LocalDate end = dateHasta.getValue();

        List<Movement> movements = dashboardService.getMovements(start, end);
        masterData.setAll(movements);

        recalculateTotals();
        updateRecordsInfo();
    }

    private void loadGlobalKPIs() {
        Platform.runLater(() -> {
            DashboardService.DashboardKpis kpis = dashboardService.getGlobalKpis();

            updateSmartLabel(lblPorCobrar, kpis.getTotalReceivable());
            updateSmartLabel(lblPorPagar, kpis.getTotalPayable());
            updateSmartLabel(lblSaldo, kpis.getTotalBalance());
            if (lblAlertas != null) lblAlertas.setText(kpis.getAlertCount() + " Productos");
        });
    }

    private void recalculateTotals() {
        double totalIncome = 0;
        double totalExpenses = 0;
        double totalCosts = 0;

        for (Movement mov : masterData) {
            String type = mov.getType() != null ? mov.getType().toLowerCase() : "";
            double amount = mov.getAmount() != null ? mov.getAmount() : 0.0;

            if (type.contains("venta") || type.contains("ingreso")) {
                totalIncome += amount;
            } else if (type.contains("gasto")) {
                totalExpenses += amount;
            } else if (type.contains("costo") || type.contains("compra")) {
                totalCosts += amount;
            }
        }

        updateSmartLabel(lblIngresos, totalIncome);
        updateSmartLabel(lblGastos, totalExpenses);
        updateSmartLabel(lblCostos, totalCosts);
        updateSmartLabel(lblNeto, totalIncome - (totalExpenses + totalCosts));
    }

    private void setupFilterUI() {
        comboPeriodo.getItems().addAll("Hoy", "Ayer", "Esta Semana", "Este Mes", "Mes Anterior", "Este Año", "Personalizado");
        dateDesde.valueProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null && dateHasta.getValue() != null) loadMovements(); });
        dateHasta.valueProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null && dateDesde.getValue() != null) loadMovements(); });
        comboPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> handlePeriodChange(newVal));
    }

    private void handlePeriodChange(String selection) {
        boolean isCustom = "Personalizado".equals(selection);
        boxFechasPersonalizadas.setVisible(isCustom);
        boxFechasPersonalizadas.setManaged(isCustom);

        if (!isCustom) {
            calculateAutomaticDates(selection);
            loadMovements();
        }
    }

    private void calculateAutomaticDates(String period) {
        if (period == null) return;
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        LocalDate end = today;

        switch (period) {
            case "Ayer": start = today.minusDays(1); end = today.minusDays(1); break;
            case "Esta Semana":
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                break;
            case "Este Mes":
                start = today.with(TemporalAdjusters.firstDayOfMonth());
                end = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Mes Anterior":
                start = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                end = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Este Año":
                start = today.with(TemporalAdjusters.firstDayOfYear());
                end = today.with(TemporalAdjusters.lastDayOfYear());
                break;
        }
        dateDesde.setValue(start);
        dateHasta.setValue(end);
    }

    private void updateRecordsInfo() {
        if (lblInfoRegistros != null) lblInfoRegistros.setText("Mostrando " + masterData.size() + " movimientos");
    }

    private void updateSmartLabel(Label label, double value) {
        if (label == null) return;
        String text = currencyFormat.format(value);
        label.setText(text);
        if (text.length() > 16) label.setFont(new Font("System Bold", 14));
        else if (text.length() > 12) label.setFont(new Font("System Bold", 18));
        else label.setFont(new Font("System Bold", 22));
    }

    private void configureColumns() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("date"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(dateFormatter.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#555555")));
                    setGraphic(lbl);
                }
            }
        });

        colTipo.setCellValueFactory(new PropertyValueFactory<>("type"));
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
            Movement m = cellData.getValue();
            String desc = m.getSourceTable() != null ? m.getSourceTable() : "MOV";
            if (m.getSourceId() != null) desc += " #" + m.getSourceId();
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
            Movement m = cellData.getValue();
            if (m.getAccount() != null) return new SimpleStringProperty(m.getAccount().getName());
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

        colMonto.setCellValueFactory(new PropertyValueFactory<>("amount"));
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
        Object controller = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/sales/views/SaleInvoices.fxml");
        if (controller instanceof SaleInvoicesController c) {
            c.openForm();
        }
    }

    @FXML
    void actionNuevaCompra(ActionEvent event) {
        MainLayoutController.getInstance().loadView("/com/autollantas/gestion/purchases/views/PurchaseForm.fxml");
    }

    @FXML
    void actionNuevoGasto(ActionEvent event) {
        Object controller = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/treasury/views/OperationalExpenses.fxml");
        if (controller instanceof OperationalExpensesController c) {
            c.openForm(new OperationalExpense());
        }
    }

    @FXML
    void actionNuevoIngreso(ActionEvent event) {
        Object controller = MainLayoutController.getInstance().loadView("/com/autollantas/gestion/treasury/views/OccasionalIncome.fxml");
        if (controller instanceof OccasionalIncomeController c) {
            c.openForm(new OccasionalIncome());
        }
    }

    @FXML
    void verAlertasStock(javafx.scene.input.MouseEvent event) {
        MainLayoutController.getInstance().loadView("/com/autollantas/gestion/inventory/views/StockAlerts.fxml");
    }

    @FXML
    void onPeriodoChanged(ActionEvent event) {
    }

    @FXML
    void abrirDialogoReportes(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/reporting/views/ReportGeneration.fxml"));
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
