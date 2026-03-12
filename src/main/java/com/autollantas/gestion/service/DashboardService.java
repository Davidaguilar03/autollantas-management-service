package com.autollantas.gestion.service;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.GastoOperativo;
import com.autollantas.gestion.model.IngresoOcasional;
import com.autollantas.gestion.model.Movimiento;
import com.autollantas.gestion.model.Venta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final VentasService ventasService;
    private final ComprasService comprasService;
    private final TesoreriaService tesoreriaService;
    private final InventarioService inventarioService;

    public DashboardService(VentasService ventasService,
                            ComprasService comprasService,
                            TesoreriaService tesoreriaService,
                            InventarioService inventarioService) {
        this.ventasService = ventasService;
        this.comprasService = comprasService;
        this.tesoreriaService = tesoreriaService;
        this.inventarioService = inventarioService;
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerMovimientos(LocalDate inicio, LocalDate fin) {
        List<Movimiento> listaUnificada = new ArrayList<>();

        for (Venta venta : ventasService.findVentasByFechaVentaBetween(inicio, fin)) {
            Movimiento movimiento = new Movimiento(
                    venta.getFechaVenta(),
                    venta.getIdVenta(),
                    "Venta",
                    venta.getTotalVenta(),
                    venta.getCuenta()
            );
            movimiento.setTablaOrigenMovimiento("VENTA");
            listaUnificada.add(movimiento);
        }

        for (Compra compra : comprasService.findComprasByFechaCompraBetween(inicio, fin)) {
            Movimiento movimiento = new Movimiento(
                    compra.getFechaCompra(),
                    compra.getIdCompra(),
                    "Costo",
                    compra.getTotalCompra(),
                    compra.getCuenta()
            );
            movimiento.setTablaOrigenMovimiento("COMPRA");
            listaUnificada.add(movimiento);
        }

        for (GastoOperativo gasto : tesoreriaService.findGastosByFechaBetween(inicio, fin)) {
            Movimiento movimiento = new Movimiento(
                    gasto.getFechaGasto(),
                    gasto.getIdGasto(),
                    "Gasto",
                    gasto.getMontoGasto(),
                    gasto.getCuenta()
            );
            movimiento.setTablaOrigenMovimiento("GASTO: " + gasto.getConceptoGasto());
            listaUnificada.add(movimiento);
        }

        for (IngresoOcasional ingreso : tesoreriaService.findIngresosByFechaBetween(inicio, fin)) {
            Movimiento movimiento = new Movimiento(
                    ingreso.getFechaIngreso(),
                    ingreso.getIdIngreso(),
                    "Ingreso",
                    ingreso.getMontoIngreso(),
                    ingreso.getCuenta()
            );
            movimiento.setTablaOrigenMovimiento("OTRO: " + ingreso.getConceptoIngreso());
            listaUnificada.add(movimiento);
        }

        listaUnificada.sort((m1, m2) -> m2.getFechaMovimiento().compareTo(m1.getFechaMovimiento()));
        return listaUnificada;
    }

    @Transactional(readOnly = true)
    public DashboardKpis obtenerKpisGlobales() {
        double totalPorCobrar = ventasService.findAllVentas().stream()
                .filter(v -> "PENDIENTE".equalsIgnoreCase(v.getEstadoVenta()))
                .mapToDouble(Venta::getTotalVenta)
                .sum();

        double totalPorPagar = comprasService.findAllCompras().stream()
                .filter(c -> "PENDIENTE".equalsIgnoreCase(c.getEstadoCompra()))
                .mapToDouble(Compra::getTotalCompra)
                .sum();

        return new DashboardKpis(
                totalPorCobrar,
                totalPorPagar,
                tesoreriaService.getSaldoTotalCuentas(),
                inventarioService.contarAlertasStock()
        );
    }

    public static class DashboardKpis {
        private final double totalPorCobrar;
        private final double totalPorPagar;
        private final double saldoTotal;
        private final long numeroAlertas;

        public DashboardKpis(double totalPorCobrar, double totalPorPagar, double saldoTotal, long numeroAlertas) {
            this.totalPorCobrar = totalPorCobrar;
            this.totalPorPagar = totalPorPagar;
            this.saldoTotal = saldoTotal;
            this.numeroAlertas = numeroAlertas;
        }

        public double getTotalPorCobrar() {
            return totalPorCobrar;
        }

        public double getTotalPorPagar() {
            return totalPorPagar;
        }

        public double getSaldoTotal() {
            return saldoTotal;
        }

        public long getNumeroAlertas() {
            return numeroAlertas;
        }
    }
}

