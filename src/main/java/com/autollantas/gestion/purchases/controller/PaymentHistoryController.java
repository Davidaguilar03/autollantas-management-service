package com.autollantas.gestion.purchases.controller;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.treasury.model.Payment;
import com.autollantas.gestion.purchases.service.PurchasesService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class PaymentHistoryController {

    @Autowired private PurchasesService purchasesService;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblProveedor;
    @FXML private Label lblTotalOriginal;
    @FXML private Label lblSaldoActual;
    @FXML private Label lblTotalPagado;

    @FXML private TableView<Payment> tablaHistorial;
    @FXML private TableColumn<Payment, String> colFecha;
    @FXML private TableColumn<Payment, String> colMetodo;
    @FXML private TableColumn<Payment, String> colCuenta;
    @FXML private TableColumn<Payment, String> colMonto;

    private Purchase currentPurchase;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        configureTable();
    }

    private void configureTable() {
        colFecha.setCellValueFactory(cell -> {
            if (cell.getValue().getDate() != null)
                return new SimpleStringProperty(dateFormatter.format(cell.getValue().getDate()));
            return new SimpleStringProperty("-");
        });
        colMetodo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getPaymentMethod()));
        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null)
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            return new SimpleStringProperty("N/A");
        });
        colMonto.setCellValueFactory(cell ->
                new SimpleStringProperty(currencyFormat.format(cell.getValue().getAmount())));
    }

    public void setPurchase(Purchase purchase) {
        this.currentPurchase = purchase;
        if (currentPurchase == null) return;

        lblNumeroFactura.setText(purchase.getInvoiceNumber());
        lblProveedor.setText(purchase.getSupplier() != null ? purchase.getSupplier().getName() : "N/A");
        lblTotalOriginal.setText(currencyFormat.format(purchase.getTotal()));

        loadPayments();
    }

    private void loadPayments() {
        if (currentPurchase == null) return;

        Platform.runLater(() -> {
            List<Payment> payments = purchasesService.findPaymentsByPurchase(currentPurchase);
            tablaHistorial.setItems(FXCollections.observableArrayList(payments));

            double totalPaid = payments.stream().mapToDouble(Payment::getAmount).sum();
            double pendingBalance = currentPurchase.getTotal() - totalPaid;
            if (pendingBalance < 0) pendingBalance = 0;

            lblTotalPagado.setText(currencyFormat.format(totalPaid));
            lblSaldoActual.setText(currencyFormat.format(pendingBalance));
        });
    }

    @FXML void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
