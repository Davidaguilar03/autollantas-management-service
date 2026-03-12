package com.autollantas.gestion.service;

import com.autollantas.gestion.model.Compra;
import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.DetalleCompra;
import com.autollantas.gestion.model.Pago;
import com.autollantas.gestion.model.Producto;
import com.autollantas.gestion.model.Proveedor;
import com.autollantas.gestion.repository.CompraRepository;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.DetalleCompraRepository;
import com.autollantas.gestion.repository.PagoRepository;
import com.autollantas.gestion.repository.ProductoRepository;
import com.autollantas.gestion.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ComprasService {

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final PagoRepository pagoRepository;
    private final CuentaRepository cuentaRepository;

    public ComprasService(CompraRepository compraRepository,
                          DetalleCompraRepository detalleCompraRepository,
                          ProveedorRepository proveedorRepository,
                          ProductoRepository productoRepository,
                          PagoRepository pagoRepository,
                          CuentaRepository cuentaRepository) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.pagoRepository = pagoRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public List<Compra> findAllCompras() {
        return compraRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Compra> findComprasByFechaCompraBetween(LocalDate inicio, LocalDate fin) {
        return compraRepository.findByFechaCompraBetween(inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<DetalleCompra> findDetallesByCompra(Compra compra) {
        return detalleCompraRepository.findByCompra(compra);
    }

    @Transactional(readOnly = true)
    public List<Pago> findPagosByCompra(Compra compra) {
        return pagoRepository.findByCompra(compra);
    }

    @Transactional(readOnly = true)
    public List<Proveedor> findAllProveedores() {
        return proveedorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String generarSiguienteNumeroFactura() {
        long maximo = 0;
        for (Compra compra : compraRepository.findAll()) {
            String numeroFactura = compra.getNumeroFacturaCompra();
            if (numeroFactura == null || numeroFactura.isEmpty()) {
                continue;
            }
            String soloNumeros = numeroFactura.replaceAll("\\D+", "");
            if (soloNumeros.isEmpty()) {
                continue;
            }
            long actual = Long.parseLong(soloNumeros);
            if (actual > maximo) {
                maximo = actual;
            }
        }
        long siguienteNumero = maximo > 0 ? maximo + 1 : 1;
        return String.format("FAC-%05d", siguienteNumero);
    }

    @Transactional
    public Proveedor guardarOActualizarProveedor(Proveedor seleccionado,
                                                 String nombre,
                                                 String nit,
                                                 String correo,
                                                 String celular) {
        if (seleccionado != null && seleccionado.getNombreProveedor() != null
                && seleccionado.getNombreProveedor().equalsIgnoreCase(nombre)) {
            seleccionado.setNumeroNitProveedor(nit);
            seleccionado.setCorreoProveedor(correo);
            seleccionado.setCelularProveedor(celular);
            return proveedorRepository.save(seleccionado);
        }

        Optional<Proveedor> existente = proveedorRepository.findByNumeroNitProveedor(nit);
        Proveedor proveedor = existente.orElse(new Proveedor());
        proveedor.setNombreProveedor(nombre);
        proveedor.setNumeroNitProveedor(nit);
        proveedor.setCorreoProveedor(correo);
        proveedor.setCelularProveedor(celular);
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public Compra guardarCompraConDetalles(Compra compra, List<DetalleCompra> nuevosDetalles, boolean modoEdicion) {
        Compra compraGuardada = compraRepository.save(compra);

        if (modoEdicion) {
            List<DetalleCompra> detallesAntiguos = detalleCompraRepository.findByCompra(compraGuardada);
            for (DetalleCompra detalleAntiguo : detallesAntiguos) {
                Producto producto = detalleAntiguo.getProducto();
                if (producto != null) {
                    producto.setCantidad(producto.getCantidad() - detalleAntiguo.getCantidadCompra());
                    productoRepository.save(producto);
                }
            }
            detalleCompraRepository.deleteAll(detallesAntiguos);
        }

        for (DetalleCompra detalle : nuevosDetalles) {
            detalle.setCompra(compraGuardada);
            detalleCompraRepository.save(detalle);

            Producto producto = detalle.getProducto();
            if (producto == null || producto.getIdProducto() == null) {
                continue;
            }

            productoRepository.findById(producto.getIdProducto()).ifPresent(productoReal -> {
                productoReal.setCantidad(productoReal.getCantidad() + detalle.getCantidadCompra());
                productoRepository.save(productoReal);
            });
        }

        return compraGuardada;
    }

    @Transactional
    public void anularCompra(Compra compra) {
        List<DetalleCompra> detalles = detalleCompraRepository.findByCompra(compra);
        for (DetalleCompra detalle : detalles) {
            Producto producto = detalle.getProducto();
            if (producto != null) {
                producto.setCantidad(producto.getCantidad() - detalle.getCantidadCompra());
                productoRepository.save(producto);
            }
        }
        compra.setEstadoCompra("ANULADA");
        compraRepository.save(compra);
    }

    @Transactional
    public void registrarPago(Compra compra, Cuenta cuentaOrigen, LocalDate fechaPago, String metodoPago, double montoPago) {
        Pago nuevoPago = new Pago();
        nuevoPago.setCompra(compra);
        nuevoPago.setCuenta(cuentaOrigen);
        nuevoPago.setFechaPago(fechaPago);
        nuevoPago.setMetodoPagoPago(metodoPago);
        nuevoPago.setValorPago(montoPago);
        pagoRepository.save(nuevoPago);

        double saldoCuenta = cuentaOrigen.getSaldoActual() != null ? cuentaOrigen.getSaldoActual() : 0.0;
        cuentaOrigen.setSaldoActual(saldoCuenta - montoPago);
        cuentaRepository.save(cuentaOrigen);

        double deudaActual = compra.getSaldoPendiente() != null ? compra.getSaldoPendiente() : compra.getTotalCompra();
        double nuevoSaldo = deudaActual - montoPago;
        if (nuevoSaldo < 0) {
            nuevoSaldo = 0.0;
        }

        compra.setSaldoPendiente(nuevoSaldo);
        compra.setCuenta(cuentaOrigen);
        compra.setMedioPagoCompra(metodoPago);
        compra.setEstadoCompra(nuevoSaldo <= 0 ? "PAGADA" : "PENDIENTE");
        compraRepository.save(compra);
    }
}

