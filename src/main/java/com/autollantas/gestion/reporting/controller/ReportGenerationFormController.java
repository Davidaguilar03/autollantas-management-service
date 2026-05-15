package com.autollantas.gestion.reporting.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

@Component
public class ReportGenerationFormController {

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private ComboBox<String> cmbPeriodo;

    @FXML private RadioButton rbPdf;
    @FXML private RadioButton rbExcel;
    @FXML private ToggleGroup grupoFormato;

    @FXML private HBox boxFechas;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    private final List<String> REPORT_TYPES = Arrays.asList(
            "Productos Vendidos",
            "Productos Comprados",
            "Indices Financieros (Panel de Control)",
            "Productos más vendidos",
            "Reporte de Stock Crítico",
            "Facturas Venta",
            "Recaudos",
            "Ingresos Ocasionales",
            "Facturas de Compra",
            "Pagos",
            "Gastos Operativos",
            "Productos (Valoración de Inventario)",
            "Movimientos y transferencias (Balance)"
    );

    @FXML
    public void initialize() {
        loadReportTypes();
        setupPeriods();

        rbPdf.setSelected(true);
        cmbPeriodo.getSelectionModel().select("Este Mes");

        updateDatesForPeriod();
    }

    private void loadReportTypes() {
        cmbTipoReporte.setItems(FXCollections.observableArrayList(REPORT_TYPES));
    }

    private void setupPeriods() {
        cmbPeriodo.setItems(FXCollections.observableArrayList(
                "Hoy", "Ayer", "Esta Semana", "Este Mes",
                "Mes Anterior", "Este Año", "Personalizado"
        ));
        cmbPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> updateDatesForPeriod());
    }

    private void updateDatesForPeriod() {
        String period = cmbPeriodo.getValue();
        if (period == null) return;

        boolean isCustom = "Personalizado".equals(period);
        boxFechas.setVisible(isCustom);
        boxFechas.setManaged(isCustom);

        if (!isCustom) {
            calculateDateRange(period);
        } else {
            if (dpInicio.getValue() == null) dpInicio.setValue(LocalDate.now());
            if (dpFin.getValue() == null) dpFin.setValue(LocalDate.now());
        }
    }

    private void calculateDateRange(String period) {
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

        dpInicio.setValue(start);
        dpFin.setValue(end);
    }

    @FXML
    public void generarReporte() {
        if (validateForm()) {
            String reportType = cmbTipoReporte.getValue();
            String format = rbPdf.isSelected() ? "PDF" : "EXCEL";
            LocalDate from = dpInicio.getValue();
            LocalDate to = dpFin.getValue();

            System.out.println("Generando reporte: " + reportType);
            System.out.println("Formato: " + format);
            System.out.println("Desde: " + from + " Hasta: " + to);

            showSuccessAlert("Reporte en proceso", "El reporte se está generando y se descargará en breve.");
            cerrarVentana();
        }
    }

    private boolean validateForm() {
        if (cmbTipoReporte.getValue() == null) {
            showErrorAlert("Selección requerida", "Por favor selecciona un Tipo de Reporte.");
            cmbTipoReporte.requestFocus();
            return false;
        }
        if (dpInicio.getValue() == null || dpFin.getValue() == null) {
            showErrorAlert("Fechas inválidas", "Por favor verifica el rango de fechas.");
            return false;
        }
        if (dpInicio.getValue().isAfter(dpFin.getValue())) {
            showErrorAlert("Rango inválido", "La fecha de inicio no puede ser posterior a la fecha fin.");
            return false;
        }
        return true;
    }

    @FXML
    public void cerrarVentana() {
        if (cmbTipoReporte.getScene() != null) {
            ((Stage) cmbTipoReporte.getScene().getWindow()).close();
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.show();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.show();
    }
}
