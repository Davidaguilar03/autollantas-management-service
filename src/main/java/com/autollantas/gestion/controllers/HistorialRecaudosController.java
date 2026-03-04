package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Recaudo;
import com.autollantas.gestion.model.Venta;
import com.autollantas.gestion.repository.RecaudoRepository;
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
    private RecaudoRepository recaudoRepo;

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblCliente;
    @FXML private Label lblTotalOriginal;
    @FXML private Label lblSaldoActual;
    @FXML private Label lblTotalRecaudado;

    @FXML private TableView<Recaudo> tablaHistorial;
    @FXML private TableColumn<Recaudo, String> colFecha;
    @FXML private TableColumn<Recaudo, String> colMetodo;
    @FXML private TableColumn<Recaudo, String> colCuenta;
    @FXML private TableColumn<Recaudo, String> colMonto;

    private Venta ventaActual;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(cell -> {
            if (cell.getValue().getFechaRecaudo() != null) {
                return new SimpleStringProperty(fechaFormatter.format(cell.getValue().getFechaRecaudo()));
            }
            return new SimpleStringProperty("-");
        });

        colMetodo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMetodoPagoRecaudo())
        );

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null) {
                return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            }
            return new SimpleStringProperty("N/A");
        });

        colMonto.setCellValueFactory(cell ->
                new SimpleStringProperty(currencyFormat.format(cell.getValue().getValorRecaudo()))
        );
    }

    public void setVenta(Venta venta) {
        this.ventaActual = venta;
        if (ventaActual == null) return;

        lblNumeroFactura.setText(venta.getNumeroFacturaVenta());
        lblCliente.setText(venta.getCliente() != null ? venta.getCliente().getNombreCliente() : "Sin Cliente");
        lblTotalOriginal.setText(currencyFormat.format(venta.getTotalVenta()));

        cargarPagosFrescos();
    }

    private void cargarPagosFrescos() {
        if (ventaActual == null) return;

        Platform.runLater(() -> {
            List<Recaudo> listaPagos = recaudoRepo.findByVenta(ventaActual);
            tablaHistorial.setItems(FXCollections.observableArrayList(listaPagos));

            double totalPagado = listaPagos.stream()
                    .mapToDouble(Recaudo::getValorRecaudo)
                    .sum();

            double saldoPendiente;

            if ("PAGADA".equalsIgnoreCase(ventaActual.getEstadoVenta())) {
                saldoPendiente = 0.0;
            } else {
                saldoPendiente = ventaActual.getTotalVenta() - totalPagado;
                if (saldoPendiente < 0) saldoPendiente = 0;
            }

            lblTotalRecaudado.setText(currencyFormat.format(totalPagado));
            lblSaldoActual.setText(currencyFormat.format(saldoPendiente));
        });
    }

    @FXML
    void btnCerrarClick() {
        Stage stage = (Stage) lblNumeroFactura.getScene().getWindow();
        if (stage != null) stage.close();
    }
}