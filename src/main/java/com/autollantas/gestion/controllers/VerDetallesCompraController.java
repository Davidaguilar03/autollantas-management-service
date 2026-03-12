package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.DetalleCompra;
import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.service.ComprasService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Component
@Scope("prototype")
public class VerDetallesCompraController {

    @Autowired
    private ComprasService comprasService;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblProveedor;
    @FXML private Label lblFecha;
    @FXML private Label lblVencimiento;
    @FXML private Label lblDiasRestantes;
    @FXML private Label lblCuenta;
    @FXML private Label lblFormaPago;
    @FXML private Label lblMedioPago;
    @FXML private Label lblEstado;

    @FXML private Label lblTotal;
    @FXML private Label lblSaldoPendiente;
    @FXML private TextArea txtNotas;

    @FXML private TableView<DetalleCompra> tablaDetalles;
    @FXML private TableColumn<DetalleCompra, String> colCodigo;
    @FXML private TableColumn<DetalleCompra, String> colProducto;
    @FXML private TableColumn<DetalleCompra, Integer> colCantidad;
    @FXML private TableColumn<DetalleCompra, String> colPrecio;
    @FXML private TableColumn<DetalleCompra, String> colDescuento;
    @FXML private TableColumn<DetalleCompra, String> colImpuesto;
    @FXML private TableColumn<DetalleCompra, String> colSubtotal;

    private final NumberFormat monedaFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
    }

    public void setCompra(Compra compra) {
        if (compra == null) return;

        lblNumeroFactura.setText(compra.getNumeroFacturaCompra());
        lblProveedor.setText(compra.getProveedor() != null ? compra.getProveedor().getNombreProveedor() : "Proveedor General");
        lblFecha.setText(compra.getFechaCompra() != null ? compra.getFechaCompra().format(fechaFormat) : "-");
        lblCuenta.setText(compra.getCuenta() != null ? compra.getCuenta().getNombreCuenta() : "Sin Asignar");
        lblFormaPago.setText(compra.getFormaPagoCompra());
        lblMedioPago.setText(compra.getMedioPagoCompra());

        if (compra.getFechaVencimientoCompra() != null) {
            lblVencimiento.setText(compra.getFechaVencimientoCompra().format(fechaFormat));

            if ("Crédito".equalsIgnoreCase(compra.getFormaPagoCompra())
                    && !"PAGADA".equalsIgnoreCase(compra.getEstadoCompra())) {

                long dias = ChronoUnit.DAYS.between(LocalDate.now(), compra.getFechaVencimientoCompra());

                lblDiasRestantes.setVisible(true);
                lblDiasRestantes.setManaged(true);

                if (dias > 0) {
                    lblDiasRestantes.setText("(Faltan " + dias + " días)");
                    lblDiasRestantes.setStyle("-fx-text-fill: #27ae60;");
                } else if (dias == 0) {
                    lblDiasRestantes.setText("(Vence HOY)");
                    lblDiasRestantes.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");
                } else {
                    lblDiasRestantes.setText("(Vencida hace " + Math.abs(dias) + " días)");
                    lblDiasRestantes.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                }
            } else {
                lblDiasRestantes.setVisible(false);
                lblDiasRestantes.setManaged(false);
            }
        } else {
            lblVencimiento.setText("-");
            lblDiasRestantes.setVisible(false);
        }

        lblEstado.setText(compra.getEstadoCompra());

        lblTotal.setText(monedaFormat.format(compra.getTotalCompra()));
        Double saldo = compra.getSaldoPendiente() != null ? compra.getSaldoPendiente() : 0.0;
        lblSaldoPendiente.setText(monedaFormat.format(saldo));

        txtNotas.setText(compra.getNotasCompra());

        cargarDetalles(compra);
    }

    private void cargarDetalles(Compra compra) {
        List<DetalleCompra> detalles = comprasService.findDetallesByCompra(compra);
        tablaDetalles.setItems(FXCollections.observableArrayList(detalles));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getCodigoProducto()));
        colProducto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getDescripcion()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadCompra"));
        colPrecio.setCellValueFactory(cell -> new SimpleStringProperty(monedaFormat.format(cell.getValue().getPrecioCompra())));
        colDescuento.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.0f%%", cell.getValue().getDescuentoCompra())));
        colImpuesto.setCellValueFactory(cell -> {
            Double imp = cell.getValue().getImpuestoCompra();
            return new SimpleStringProperty(imp != null ? String.format("%.0f", imp) : "0");
        });
        colSubtotal.setCellValueFactory(cell -> new SimpleStringProperty(monedaFormat.format(cell.getValue().getSubtotalCompra())));
    }

    @FXML
    void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        stage.close();
    }
}