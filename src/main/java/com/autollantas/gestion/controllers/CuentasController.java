package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.Movimiento;
import com.autollantas.gestion.model.Transferencia;
import com.autollantas.gestion.service.TesoreriaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class CuentasController {

    @Autowired private TesoreriaService tesoreriaService;
    @Autowired private ApplicationContext springContext;

    @FXML private Label lblTotalGlobal;

    @FXML private Label lblSaldoCaja;
    @FXML private TableView<MovimientoDTO> tablaMovCaja;
    @FXML private TableColumn<MovimientoDTO, LocalDate> colCajaMovFecha;
    @FXML private TableColumn<MovimientoDTO, String> colCajaMovTipo;
    @FXML private TableColumn<MovimientoDTO, String> colCajaMovConcepto;
    @FXML private TableColumn<MovimientoDTO, Double> colCajaMovMonto;
    @FXML private Label lblInfoMovCaja;

    @FXML private TableView<TransferenciaDTO> tablaTransCaja;
    @FXML private TableColumn<TransferenciaDTO, String> colCajaTransConcepto;
    @FXML private TableColumn<TransferenciaDTO, LocalDate> colCajaTransFecha;
    @FXML private TableColumn<TransferenciaDTO, Double> colCajaTransValor;
    @FXML private TableColumn<TransferenciaDTO, String> colCajaTransOrigen;
    @FXML private TableColumn<TransferenciaDTO, String> colCajaTransDestino;
    @FXML private Label lblInfoTransCaja;

    @FXML private Label lblSaldoBanco;
    @FXML private TableView<MovimientoDTO> tablaMovBanco;
    @FXML private TableColumn<MovimientoDTO, LocalDate> colBancoMovFecha;
    @FXML private TableColumn<MovimientoDTO, String> colBancoMovTipo;
    @FXML private TableColumn<MovimientoDTO, String> colBancoMovConcepto;
    @FXML private TableColumn<MovimientoDTO, Double> colBancoMovMonto;
    @FXML private Label lblInfoMovBanco;

    @FXML private TableView<TransferenciaDTO> tablaTransBanco;
    @FXML private TableColumn<TransferenciaDTO, String> colBancoTransConcepto;
    @FXML private TableColumn<TransferenciaDTO, LocalDate> colBancoTransFecha;
    @FXML private TableColumn<TransferenciaDTO, Double> colBancoTransValor;
    @FXML private TableColumn<TransferenciaDTO, String> colBancoTransOrigen;
    @FXML private TableColumn<TransferenciaDTO, String> colBancoTransDestino;
    @FXML private Label lblInfoTransBanco;

    private final ObservableList<MovimientoDTO> movCajaList = FXCollections.observableArrayList();
    private final ObservableList<TransferenciaDTO> transCajaList = FXCollections.observableArrayList();
    private final ObservableList<MovimientoDTO> movBancoList = FXCollections.observableArrayList();
    private final ObservableList<TransferenciaDTO> transBancoList = FXCollections.observableArrayList();

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Cuenta cuentaCaja;
    private Cuenta cuentaBanco;

    @FXML
    public void initialize() {
        configurarTablas();
        cargarDatosDB();
    }

    private void cargarDatosDB() {
        if (tesoreriaService == null) return;

        new Thread(() -> {
            List<Cuenta> cuentas = tesoreriaService.findAllCuentas();
            cuentaCaja = cuentas.stream().filter(c -> c.getNombreCuenta().toUpperCase().contains("CAJA") || c.getNombreCuenta().toUpperCase().contains("EFECTIVO")).findFirst().orElse(null);
            cuentaBanco = cuentas.stream().filter(c -> c.getNombreCuenta().toUpperCase().contains("BANCO") || c.getNombreCuenta().toUpperCase().contains("COLOMBIA")).findFirst().orElse(null);

            if (cuentaCaja == null && !cuentas.isEmpty()) cuentaCaja = cuentas.get(0);
            if (cuentaBanco == null && cuentas.size() > 1) cuentaBanco = cuentas.get(1);

            Platform.runLater(() -> {
                actualizarSaldosUI();
                cargarTablas();
            });
        }).start();
    }

    private void cargarTablas() {
        if (cuentaCaja != null) {
            cargarMovimientosDeCuenta(cuentaCaja.getIdCuenta(), movCajaList, lblInfoMovCaja);
            cargarTransferenciasDeCuenta(cuentaCaja.getIdCuenta(), transCajaList, lblInfoTransCaja);
        }
        if (cuentaBanco != null) {
            cargarMovimientosDeCuenta(cuentaBanco.getIdCuenta(), movBancoList, lblInfoMovBanco);
            cargarTransferenciasDeCuenta(cuentaBanco.getIdCuenta(), transBancoList, lblInfoTransBanco);
        }
    }

    private void cargarMovimientosDeCuenta(Integer idCuenta, ObservableList<MovimientoDTO> lista, Label lblInfo) {
        lista.clear();
        List<Movimiento> movimientos = tesoreriaService.findMovimientosByCuentaId(idCuenta);
        for (Movimiento m : movimientos) {
            lista.add(new MovimientoDTO(m));
        }
        if (lblInfo != null) lblInfo.setText("Registros: " + lista.size());
    }

    private void cargarTransferenciasDeCuenta(Integer idCuenta, ObservableList<TransferenciaDTO> lista, Label lblInfo) {
        lista.clear();
        List<Transferencia> transferencias = tesoreriaService.findTransferenciasByCuentaId(idCuenta);
        for (Transferencia t : transferencias) {
            lista.add(new TransferenciaDTO(t));
        }
        if (lblInfo != null) lblInfo.setText("Registros: " + lista.size());
    }

    private void actualizarSaldosUI() {
        double saldoCaja = (cuentaCaja != null) ? cuentaCaja.getSaldoActual() : 0.0;
        double saldoBanco = (cuentaBanco != null) ? cuentaBanco.getSaldoActual() : 0.0;
        double total = saldoCaja + saldoBanco;

        if (lblSaldoCaja != null) lblSaldoCaja.setText(currencyFormat.format(saldoCaja));
        if (lblSaldoBanco != null) lblSaldoBanco.setText(currencyFormat.format(saldoBanco));
        if (lblTotalGlobal != null) lblTotalGlobal.setText("Total: " + currencyFormat.format(total));
    }

    private void configurarTablas() {
        tablaMovCaja.setItems(movCajaList);
        colCajaMovFecha.setCellValueFactory(cell -> cell.getValue().fechaProperty());
        colCajaMovTipo.setCellValueFactory(cell -> cell.getValue().tipoProperty());
        colCajaMovConcepto.setCellValueFactory(cell -> cell.getValue().conceptoProperty());
        colCajaMovMonto.setCellValueFactory(cell -> cell.getValue().montoProperty().asObject());
        aplicarFormatoFecha(colCajaMovFecha);
        aplicarFormatoMoneda(colCajaMovMonto);
        formatearColumnaTipo(colCajaMovTipo);

        tablaTransCaja.setItems(transCajaList);
        configurarColumnasTransferencia(colCajaTransConcepto, colCajaTransFecha, colCajaTransValor, colCajaTransOrigen, colCajaTransDestino);

        tablaMovBanco.setItems(movBancoList);
        colBancoMovFecha.setCellValueFactory(cell -> cell.getValue().fechaProperty());
        colBancoMovTipo.setCellValueFactory(cell -> cell.getValue().tipoProperty());
        colBancoMovConcepto.setCellValueFactory(cell -> cell.getValue().conceptoProperty());
        colBancoMovMonto.setCellValueFactory(cell -> cell.getValue().montoProperty().asObject());
        aplicarFormatoFecha(colBancoMovFecha);
        aplicarFormatoMoneda(colBancoMovMonto);
        formatearColumnaTipo(colBancoMovTipo);

        tablaTransBanco.setItems(transBancoList);
        configurarColumnasTransferencia(colBancoTransConcepto, colBancoTransFecha, colBancoTransValor, colBancoTransOrigen, colBancoTransDestino);
    }

    private void formatearColumnaTipo(TableColumn<MovimientoDTO, String> columna) {
        columna.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

                    if (item.equalsIgnoreCase("Ingreso") || item.equalsIgnoreCase("Venta")) {
                        lbl.setTextFill(Color.web("#27ae60"));
                    } else {
                        lbl.setTextFill(Color.web("#c0392b"));
                    }
                    setGraphic(lbl);
                }
            }
        });
    }

    private void configurarColumnasTransferencia(TableColumn<TransferenciaDTO, String> colConcepto,
                                                 TableColumn<TransferenciaDTO, LocalDate> colFecha,
                                                 TableColumn<TransferenciaDTO, Double> colValor,
                                                 TableColumn<TransferenciaDTO, String> colOrigen,
                                                 TableColumn<TransferenciaDTO, String> colDestino) {
        colConcepto.setCellValueFactory(cell -> cell.getValue().conceptoProperty());
        colFecha.setCellValueFactory(cell -> cell.getValue().fechaProperty());
        colValor.setCellValueFactory(cell -> cell.getValue().valorProperty().asObject());
        colOrigen.setCellValueFactory(cell -> cell.getValue().origenProperty());
        colDestino.setCellValueFactory(cell -> cell.getValue().destinoProperty());
        aplicarFormatoFecha(colFecha);
        aplicarFormatoMoneda(colValor);
    }

    @FXML
    void abrirModalTransferencia(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/FormularioTransferencia.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            FormularioTransferenciaController controller = loader.getController();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) lblTotalGlobal.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());

            modalStage.showAndWait();

            if (controller.isGuardado()) {
                cargarDatosDB();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario: " + e.getMessage()).show();
        }
    }

    private <T> void aplicarFormatoFecha(TableColumn<T, LocalDate> columna) {
        columna.setCellFactory(column -> new TableCell<T, LocalDate>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateFormat.format(item));
            }
        });
    }

    private <T> void aplicarFormatoMoneda(TableColumn<T, Double> columna) {
        columna.setCellFactory(column -> new TableCell<T, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : currencyFormat.format(item));
            }
        });
    }

    public static class MovimientoDTO {
        private final SimpleObjectProperty<LocalDate> fecha;
        private final SimpleStringProperty tipo;
        private final SimpleStringProperty concepto;
        private final SimpleDoubleProperty monto;

        public MovimientoDTO(Movimiento m) {
            this.fecha = new SimpleObjectProperty<>(m.getFechaMovimiento());
            this.tipo = new SimpleStringProperty(m.getTipoMovimiento());
            String desc = m.getTablaOrigenMovimiento() != null ? m.getTablaOrigenMovimiento() + " #" + m.getIdOrigenMovimiento() : "General";
            this.concepto = new SimpleStringProperty(desc);
            this.monto = new SimpleDoubleProperty(m.getMontoMovimiento());
        }
        public SimpleObjectProperty<LocalDate> fechaProperty() { return fecha; }
        public SimpleStringProperty tipoProperty() { return tipo; }
        public SimpleStringProperty conceptoProperty() { return concepto; }
        public SimpleDoubleProperty montoProperty() { return monto; }
    }

    public static class TransferenciaDTO {
        private final SimpleStringProperty concepto;
        private final SimpleObjectProperty<LocalDate> fecha;
        private final SimpleDoubleProperty valor;
        private final SimpleStringProperty origen;
        private final SimpleStringProperty destino;

        public TransferenciaDTO(Transferencia t) {
            this.concepto = new SimpleStringProperty("Transferencia ID " + t.getIdTransferencia());
            this.fecha = new SimpleObjectProperty<>(t.getFechaTransferencia());
            this.valor = new SimpleDoubleProperty(t.getMontoTransferencia());
            this.origen = new SimpleStringProperty(t.getCuentaOrigen() != null ? t.getCuentaOrigen().getNombreCuenta() : "Desc.");
            this.destino = new SimpleStringProperty(t.getCuentaDestino() != null ? t.getCuentaDestino().getNombreCuenta() : "Desc.");
        }
        public SimpleStringProperty conceptoProperty() { return concepto; }
        public SimpleObjectProperty<LocalDate> fechaProperty() { return fecha; }
        public SimpleDoubleProperty valorProperty() { return valor; }
        public SimpleStringProperty origenProperty() { return origen; }
        public SimpleStringProperty destinoProperty() { return destino; }
    }
}