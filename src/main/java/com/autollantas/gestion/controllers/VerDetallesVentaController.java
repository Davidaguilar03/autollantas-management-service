package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.DetalleVenta;
import com.autollantas.gestion.model.Venta;
import com.autollantas.gestion.repository.DetalleVentaRepository;
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
public class VerDetallesVentaController {

    @Autowired
    private DetalleVentaRepository detalleRepo;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblCliente;
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

    @FXML private TableView<DetalleVenta> tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String> colCodigo;
    @FXML private TableColumn<DetalleVenta, String> colProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colPrecio;
    @FXML private TableColumn<DetalleVenta, String> colDescuento;
    @FXML private TableColumn<DetalleVenta, String> colImpuesto;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;

    private final NumberFormat monedaFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
    }

    public void setVenta(Venta venta) {
        if (venta == null) return;

        lblNumeroFactura.setText(venta.getNumeroFacturaVenta());
        lblCliente.setText(venta.getCliente() != null ? venta.getCliente().getNombreCliente() : "Cliente General");
        lblFecha.setText(venta.getFechaVenta() != null ? venta.getFechaVenta().format(fechaFormat) : "-");
        lblCuenta.setText(venta.getCuenta() != null ? venta.getCuenta().getNombreCuenta() : "Sin Asignar");
        lblFormaPago.setText(venta.getFormaPagoVenta());
        lblMedioPago.setText(venta.getMedioPagoVenta());

        if (venta.getFechaVencimientoVenta() != null) {
            lblVencimiento.setText(venta.getFechaVencimientoVenta().format(fechaFormat));

            if ("Crédito".equalsIgnoreCase(venta.getFormaPagoVenta())
                    && !"PAGADA".equalsIgnoreCase(venta.getEstadoVenta())
                    && !"ANULADA".equalsIgnoreCase(venta.getEstadoVenta())) {

                long dias = ChronoUnit.DAYS.between(LocalDate.now(), venta.getFechaVencimientoVenta());

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
            lblDiasRestantes.setManaged(false);
        }

        lblEstado.setText(venta.getEstadoVenta());
        switch (venta.getEstadoVenta()) {
            case "PAGADA" -> lblEstado.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 14px;");
            case "ANULADA" -> lblEstado.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
            case "PENDIENTE" -> lblEstado.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 14px;");
            default -> lblEstado.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");
        }

        lblTotal.setText(monedaFormat.format(venta.getTotalVenta()));
        Double saldo = venta.getSaldoPendiente() != null ? venta.getSaldoPendiente() : 0.0;
        lblSaldoPendiente.setText(monedaFormat.format(saldo));

        if (saldo > 0) lblSaldoPendiente.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        else lblSaldoPendiente.setStyle("-fx-text-fill: green;");

        String notas = venta.getNotasVenta();
        txtNotas.setText((notas == null || notas.trim().isEmpty()) ? "Sin observaciones." : notas);

        cargarDetalles(venta);
    }

    private void cargarDetalles(Venta venta) {
        List<DetalleVenta> detalles = detalleRepo.findByVenta(venta);
        tablaDetalles.setItems(FXCollections.observableArrayList(detalles));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getCodigoProducto()));
        colProducto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getDescripcion()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadVenta"));
        colPrecio.setCellValueFactory(cell -> new SimpleStringProperty(monedaFormat.format(cell.getValue().getPrecioVenta())));
        colDescuento.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.0f%%", cell.getValue().getDescuentoVenta())));
        colImpuesto.setCellValueFactory(cell -> {
            Double imp = cell.getValue().getImpuestoVenta();
            return new SimpleStringProperty(imp != null ? String.format("%.0f", imp) : "0");
        });
        colSubtotal.setCellValueFactory(cell -> new SimpleStringProperty(monedaFormat.format(cell.getValue().getSubtotalVenta())));
    }

    @FXML
    void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        stage.close();
    }
}