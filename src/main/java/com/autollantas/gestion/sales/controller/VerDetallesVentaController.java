package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.model.SaleDetail;
import com.autollantas.gestion.sales.service.SalesService;
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
    private SalesService salesService;

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

    @FXML private TableView<SaleDetail> tablaDetalles;
    @FXML private TableColumn<SaleDetail, String> colCodigo;
    @FXML private TableColumn<SaleDetail, String> colProducto;
    @FXML private TableColumn<SaleDetail, Integer> colCantidad;
    @FXML private TableColumn<SaleDetail, String> colPrecio;
    @FXML private TableColumn<SaleDetail, String> colDescuento;
    @FXML private TableColumn<SaleDetail, String> colImpuesto;
    @FXML private TableColumn<SaleDetail, String> colSubtotal;

    private final NumberFormat monedaFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
    }

    public void setSale(Sale sale) {
        if (sale == null) return;

        lblNumeroFactura.setText(sale.getInvoiceNumber());
        lblCliente.setText(sale.getCustomer() != null ? sale.getCustomer().getName() : "Cliente General");
        lblFecha.setText(sale.getSaleDate() != null ? sale.getSaleDate().format(fechaFormat) : "-");
        lblCuenta.setText(sale.getAccount() != null ? sale.getAccount().getName() : "Sin Asignar");
        lblFormaPago.setText(sale.getPaymentType());
        lblMedioPago.setText(sale.getPaymentMethod());

        if (sale.getDueDate() != null) {
            lblVencimiento.setText(sale.getDueDate().format(fechaFormat));

            if ("Crédito".equalsIgnoreCase(sale.getPaymentType())
                    && !"PAGADA".equalsIgnoreCase(sale.getStatus())
                    && !"ANULADA".equalsIgnoreCase(sale.getStatus())) {

                long dias = ChronoUnit.DAYS.between(LocalDate.now(), sale.getDueDate());

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

        lblEstado.setText(sale.getStatus());
        switch (sale.getStatus()) {
            case "PAGADA" -> lblEstado.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 14px;");
            case "ANULADA" -> lblEstado.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
            case "PENDIENTE" -> lblEstado.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 14px;");
            default -> lblEstado.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");
        }

        lblTotal.setText(monedaFormat.format(sale.getTotal()));
        Double balance = sale.getPendingBalance() != null ? sale.getPendingBalance() : 0.0;
        lblSaldoPendiente.setText(monedaFormat.format(balance));

        if (balance > 0) lblSaldoPendiente.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        else lblSaldoPendiente.setStyle("-fx-text-fill: green;");

        String notes = sale.getNotes();
        txtNotas.setText((notes == null || notes.trim().isEmpty()) ? "Sin observaciones." : notes);

        loadDetails(sale);
    }

    private void loadDetails(Sale sale) {
        List<SaleDetail> details = salesService.findSaleDetailsBySale(sale);
        tablaDetalles.setItems(FXCollections.observableArrayList(details));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduct().getCode()));
        colProducto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduct().getDescription()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrecio.setCellValueFactory(cell -> new SimpleStringProperty(monedaFormat.format(cell.getValue().getSalePrice())));
        colDescuento.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.0f%%", cell.getValue().getDiscount())));
        colImpuesto.setCellValueFactory(cell -> {
            Double tax = cell.getValue().getTax();
            return new SimpleStringProperty(tax != null ? String.format("%.0f", tax) : "0");
        });
        colSubtotal.setCellValueFactory(cell -> new SimpleStringProperty(monedaFormat.format(cell.getValue().getSubtotal())));
    }

    @FXML
    void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        stage.close();
    }
}
