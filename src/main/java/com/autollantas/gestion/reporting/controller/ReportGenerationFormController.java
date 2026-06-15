package com.autollantas.gestion.reporting.controller;

import com.autollantas.gestion.reporting.service.ReportService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    @Autowired private ReportService reportService;

    private static final List<String> REPORT_TYPES = Arrays.asList(
            "Resumen Financiero",
            "Ventas por Producto",
            "Compras por Producto"
    );

    @FXML
    public void initialize() {
        cmbTipoReporte.setItems(FXCollections.observableArrayList(REPORT_TYPES));
        cmbPeriodo.setItems(FXCollections.observableArrayList(
                "Hoy", "Ayer", "Esta Semana", "Este Mes", "Mes Anterior", "Este Año", "Personalizado"));
        cmbPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> updateDatesForPeriod());
        rbPdf.setSelected(true);
        cmbPeriodo.getSelectionModel().select("Este Mes");
        updateDatesForPeriod();
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
            case "Ayer":
                start = today.minusDays(1);
                end = today.minusDays(1);
                break;
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
            default:
                break;
        }
        dpInicio.setValue(start);
        dpFin.setValue(end);
    }

    @FXML
    public void generarReporte() {
        if (!validateForm()) return;

        String tipo = cmbTipoReporte.getValue();
        boolean isPdf = rbPdf.isSelected();
        LocalDate from = dpInicio.getValue();
        LocalDate to = dpFin.getValue();

        String ext = isPdf ? ".pdf" : ".xlsx";
        String defaultName = tipo.replace(" ", "_") + "_" + from + "_" + to + ext;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar Reporte");
        chooser.setInitialFileName(defaultName);
        if (isPdf) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        }

        String userHome = System.getProperty("user.home");
        File desktop = new File(userHome, "Desktop");
        if (!desktop.exists()) desktop = new File(userHome, "Escritorio");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        Stage stage = (Stage) cmbTipoReporte.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            byte[] bytes = generateBytes(tipo, isPdf, from, to);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
            // Cerrar modal primero, luego mostrar el toast en la ventana principal
            cerrarVentana();
            ToastNotification.success(
                MainLayoutController.getInstance().getContentArea(),
                "Reporte guardado en: " + file.getAbsolutePath()
            );
        } catch (IOException e) {
            ToastNotification.error(cmbTipoReporte, "No se pudo guardar el archivo: " + e.getMessage());
        } catch (Exception e) {
            ToastNotification.error(cmbTipoReporte, "Error al generar el reporte: " + e.getMessage());
        }
    }

    private byte[] generateBytes(String tipo, boolean isPdf, LocalDate from, LocalDate to) {
        if ("Resumen Financiero".equals(tipo)) {
            return isPdf ? reportService.generateResumenFinancieroPDF(from, to)
                         : reportService.generateResumenFinancieroExcel(from, to);
        } else if ("Ventas por Producto".equals(tipo)) {
            return isPdf ? reportService.generateVentasPorProductoPDF(from, to)
                         : reportService.generateVentasPorProductoExcel(from, to);
        } else if ("Compras por Producto".equals(tipo)) {
            return isPdf ? reportService.generateComprasPorProductoPDF(from, to)
                         : reportService.generateComprasPorProductoExcel(from, to);
        }
        throw new IllegalArgumentException("Tipo de reporte desconocido: " + tipo);
    }

    private boolean validateForm() {
        if (cmbTipoReporte.getValue() == null) {
            ToastNotification.warning(cmbTipoReporte, "Selecciona un tipo de reporte antes de continuar");
            cmbTipoReporte.requestFocus();
            return false;
        }
        if (dpInicio.getValue() == null || dpFin.getValue() == null) {
            ToastNotification.warning(cmbTipoReporte, "Verifica que el rango de fechas esté completo");
            return false;
        }
        if (dpInicio.getValue().isAfter(dpFin.getValue())) {
            ToastNotification.warning(cmbTipoReporte, "La fecha de inicio no puede ser posterior a la fecha final");
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

}
