package com.autollantas.gestion.sales.controller;

import com.autollantas.gestion.treasury.model.Collection;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.service.SalesService;
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
public class HistorialRecaudosController {

    @Autowired
    private SalesService salesService;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblCliente;
    @FXML private Label lblTotalOriginal;
    @FXML private Label lblSaldoActual;
    @FXML private Label lblTotalRecaudado;

    @FXML private TableView<Collection> tablaHistorial;
    @FXML private TableColumn<Collection, String> colFecha;
    @FXML private TableColumn<Collection, String> colMetodo;
    @FXML private TableColumn<Collection, String> colCuenta;
    @FXML private TableColumn<Collection, String> colMonto;

    private Sale currentSale;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(cell -> {
            if (cell.getValue().getDate() != null) {
                return new SimpleStringProperty(fechaFormatter.format(cell.getValue().getDate()));
            }
            return new SimpleStringProperty("-");
        });

        colMetodo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getPaymentMethod())
        );

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null) {
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            }
            return new SimpleStringProperty("N/A");
        });

        colMonto.setCellValueFactory(cell ->
                new SimpleStringProperty(currencyFormat.format(cell.getValue().getAmount()))
        );
    }

    public void setSale(Sale sale) {
        this.currentSale = sale;
        if (currentSale == null) return;

        lblNumeroFactura.setText(sale.getInvoiceNumber());
        lblCliente.setText(sale.getCustomer() != null ? sale.getCustomer().getName() : "Sin Cliente");
        lblTotalOriginal.setText(currencyFormat.format(sale.getTotal()));

        loadCollections();
    }

    private void loadCollections() {
        if (currentSale == null) return;

        Platform.runLater(() -> {
            List<Collection> collections = salesService.findCollectionsBySale(currentSale);
            tablaHistorial.setItems(FXCollections.observableArrayList(collections));

            double totalPaid = collections.stream()
                    .mapToDouble(Collection::getAmount)
                    .sum();

            double pendingBalance;

            if ("PAGADA".equalsIgnoreCase(currentSale.getStatus())) {
                pendingBalance = 0.0;
            } else {
                pendingBalance = currentSale.getTotal() - totalPaid;
                if (pendingBalance < 0) pendingBalance = 0;
            }

            lblTotalRecaudado.setText(currencyFormat.format(totalPaid));
            lblSaldoActual.setText(currencyFormat.format(pendingBalance));
        });
    }

    @FXML
    void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
