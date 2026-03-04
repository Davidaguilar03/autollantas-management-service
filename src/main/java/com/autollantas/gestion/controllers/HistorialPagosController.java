package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.Pago;
import com.autollantas.gestion.repository.PagoRepository;
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
public class HistorialPagosController {

    @Autowired private PagoRepository pagoRepo;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblProveedor;
    @FXML private Label lblTotalOriginal;
    @FXML private Label lblSaldoActual;
    @FXML private Label lblTotalPagado;

    @FXML private TableView<Pago> tablaHistorial;
    @FXML private TableColumn<Pago, String> colFecha;
    @FXML private TableColumn<Pago, String> colMetodo;
    @FXML private TableColumn<Pago, String> colCuenta;
    @FXML private TableColumn<Pago, String> colMonto;

    private Compra compraActual;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(cell -> {
            if (cell.getValue().getFechaPago() != null) {
                return new SimpleStringProperty(fechaFormatter.format(cell.getValue().getFechaPago()));
            }
            return new SimpleStringProperty("-");
        });

        colMetodo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMetodoPagoPago())
        );

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null) {
                return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            }
            return new SimpleStringProperty("N/A");
        });

        colMonto.setCellValueFactory(cell ->
                new SimpleStringProperty(currencyFormat.format(cell.getValue().getValorPago()))
        );
    }

    public void setCompra(Compra compra) {
        this.compraActual = compra;
        if (compraActual == null) return;

        lblNumeroFactura.setText(compra.getNumeroFacturaCompra());
        lblProveedor.setText(compra.getProveedor() != null ? compra.getProveedor().getNombreProveedor() : "N/A");
        lblTotalOriginal.setText(currencyFormat.format(compra.getTotalCompra()));

        cargarPagos();
    }

    private void cargarPagos() {
        if (compraActual == null) return;

        Platform.runLater(() -> {
            List<Pago> listaPagos = pagoRepo.findByCompra(compraActual);
            tablaHistorial.setItems(FXCollections.observableArrayList(listaPagos));

            double totalPagado = listaPagos.stream().mapToDouble(Pago::getValorPago).sum();

            double saldoPendiente = compraActual.getTotalCompra() - totalPagado;
            if (saldoPendiente < 0) saldoPendiente = 0;

            lblTotalPagado.setText(currencyFormat.format(totalPagado));
            lblSaldoActual.setText(currencyFormat.format(saldoPendiente));
        });
    }

    @FXML void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        if (stage != null) stage.close();
    }
}