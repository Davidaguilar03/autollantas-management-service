package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.service.PurchasesService;
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
public class PurchaseDetailsController {

    @Autowired
    private PurchasesService purchasesService;

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

    @FXML private TableView<PurchaseDetail> tablaDetalles;
    @FXML private TableColumn<PurchaseDetail, String> colCodigo;
    @FXML private TableColumn<PurchaseDetail, String> colProducto;
    @FXML private TableColumn<PurchaseDetail, Integer> colCantidad;
    @FXML private TableColumn<PurchaseDetail, String> colPrecio;
    @FXML private TableColumn<PurchaseDetail, String> colDescuento;
    @FXML private TableColumn<PurchaseDetail, String> colImpuesto;
    @FXML private TableColumn<PurchaseDetail, String> colSubtotal;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configureTable();
    }

    public void setPurchase(Purchase purchase) {
        if (purchase == null) return;

        lblNumeroFactura.setText(purchase.getInvoiceNumber());
        lblProveedor.setText(purchase.getSupplier() != null ? purchase.getSupplier().getName() : "Proveedor General");
        lblFecha.setText(purchase.getPurchaseDate() != null ? purchase.getPurchaseDate().format(dateFormat) : "-");
        lblCuenta.setText(purchase.getAccount() != null ? purchase.getAccount().getName() : "Sin Asignar");
        lblFormaPago.setText(purchase.getPaymentType());
        lblMedioPago.setText(purchase.getPaymentMethod());

        if (purchase.getDueDate() != null) {
            lblVencimiento.setText(purchase.getDueDate().format(dateFormat));

            if ("Crédito".equalsIgnoreCase(purchase.getPaymentType())
                    && !"PAGADA".equalsIgnoreCase(purchase.getStatus())) {

                long days = ChronoUnit.DAYS.between(LocalDate.now(), purchase.getDueDate());
                lblDiasRestantes.setVisible(true);
                lblDiasRestantes.setManaged(true);

                if (days > 0) {
                    lblDiasRestantes.setText("(Faltan " + days + " días)");
                    lblDiasRestantes.setStyle("-fx-text-fill: #27ae60;");
                } else if (days == 0) {
                    lblDiasRestantes.setText("(Vence HOY)");
                    lblDiasRestantes.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");
                } else {
                    lblDiasRestantes.setText("(Vencida hace " + Math.abs(days) + " días)");
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

        lblEstado.setText(purchase.getStatus());
        lblTotal.setText(currencyFormat.format(purchase.getTotal()));
        Double balance = purchase.getPendingBalance() != null ? purchase.getPendingBalance() : 0.0;
        lblSaldoPendiente.setText(currencyFormat.format(balance));
        txtNotas.setText(purchase.getNotes());

        loadDetails(purchase);
    }

    private void loadDetails(Purchase purchase) {
        List<PurchaseDetail> details = purchasesService.findDetailsByPurchase(purchase);
        tablaDetalles.setItems(FXCollections.observableArrayList(details));
    }

    private void configureTable() {
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduct().getCode()));
        colProducto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduct().getDescription()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrecio.setCellValueFactory(cell -> new SimpleStringProperty(currencyFormat.format(cell.getValue().getUnitPrice())));
        colDescuento.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.0f%%", cell.getValue().getDiscount())));
        colImpuesto.setCellValueFactory(cell -> {
            Double tax = cell.getValue().getTax();
            return new SimpleStringProperty(tax != null ? String.format("%.0f", tax) : "0");
        });
        colSubtotal.setCellValueFactory(cell -> new SimpleStringProperty(currencyFormat.format(cell.getValue().getSubtotal())));
    }

    @FXML
    void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        stage.close();
    }
}
