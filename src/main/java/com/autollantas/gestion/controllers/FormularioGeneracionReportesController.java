package com.autollantas.gestion.controllers;

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
public class FormularioGeneracionReportesController {

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private ComboBox<String> cmbPeriodo;

    @FXML private RadioButton rbPdf;
    @FXML private RadioButton rbExcel;
    @FXML private ToggleGroup grupoFormato;

    @FXML private HBox boxFechas;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    private final List<String> LISTA_REPORTES = Arrays.asList(
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
        cargarTiposReporte();
        configurarPeriodos();

        rbPdf.setSelected(true);
        cmbPeriodo.getSelectionModel().select("Este Mes");

        actualizarFechasPorPeriodo();
    }

    private void cargarTiposReporte() {
        cmbTipoReporte.setItems(FXCollections.observableArrayList(LISTA_REPORTES));
    }

    private void configurarPeriodos() {
        cmbPeriodo.setItems(FXCollections.observableArrayList(
                "Hoy", "Ayer", "Esta Semana", "Este Mes",
                "Mes Anterior", "Este Año", "Personalizado"
        ));

        cmbPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFechasPorPeriodo());
    }

    private void actualizarFechasPorPeriodo() {
        String periodo = cmbPeriodo.getValue();
        if (periodo == null) return;

        boolean esPersonalizado = "Personalizado".equals(periodo);

        boxFechas.setVisible(esPersonalizado);
        boxFechas.setManaged(esPersonalizado);

        if (!esPersonalizado) {
            calcularRangoFechas(periodo);
        } else {
            if (dpInicio.getValue() == null) dpInicio.setValue(LocalDate.now());
            if (dpFin.getValue() == null) dpFin.setValue(LocalDate.now());
        }
    }

    private void calcularRangoFechas(String periodo) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy;
        LocalDate fin = hoy;

        switch (periodo) {
            case "Ayer":
                inicio = hoy.minusDays(1);
                fin = hoy.minusDays(1);
                break;
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

        dpInicio.setValue(inicio);
        dpFin.setValue(fin);
    }

    @FXML
    public void generarReporte() {
        if (validarFormulario()) {
            String tipoReporte = cmbTipoReporte.getValue();
            String formato = rbPdf.isSelected() ? "PDF" : "EXCEL";
            LocalDate desde = dpInicio.getValue();
            LocalDate hasta = dpFin.getValue();

            System.out.println("Generando reporte: " + tipoReporte);
            System.out.println("Formato: " + formato);
            System.out.println("Desde: " + desde + " Hasta: " + hasta);

            mostrarAlertaExito("Reporte en proceso", "El reporte se está generando y se descargará en breve.");
            cerrarVentana();
        }
    }

    private boolean validarFormulario() {
        if (cmbTipoReporte.getValue() == null) {
            mostrarAlertaError("Selección requerida", "Por favor selecciona un Tipo de Reporte.");
            cmbTipoReporte.requestFocus();
            return false;
        }
        if (dpInicio.getValue() == null || dpFin.getValue() == null) {
            mostrarAlertaError("Fechas inválidas", "Por favor verifica el rango de fechas.");
            return false;
        }
        if (dpInicio.getValue().isAfter(dpFin.getValue())) {
            mostrarAlertaError("Rango inválido", "La fecha de inicio no puede ser posterior a la fecha fin.");
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

    private void mostrarAlertaError(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.show();
    }

    private void mostrarAlertaExito(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.show();
    }
}